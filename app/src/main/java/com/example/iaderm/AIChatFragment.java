package com.example.iaderm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIChatFragment extends Fragment {

    private TextInputEditText etMessage;
    private FloatingActionButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollChat;

    private String userMedicalProfileJSON = "";
    private int recentAnalysisScore = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // API CONFIGURATION (Actualizado a Gemini 3.0 Flash)
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.0-flash:generateContent?key=" + API_KEY;
    
    // Conversation History
    private final JSONArray conversationHistory = new JSONArray();
    private boolean isFirstMessage = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_chat, container, false);

        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        chatContainer = view.findViewById(R.id.chatContainer);
        scrollChat = view.findViewById(R.id.scrollChat);

        loadUserContext();

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageToGemini(message);
            }
        });
        
        if (isFirstMessage) {
            showBotMessage("Hola, soy tu asistente dermatológico (Gemini 3.0). He analizado tu perfil clínico y los resultados de tu escaneo. ¿En qué puedo ayudarte?");
            isFirstMessage = false;
        }

        return view;
    }

    private void loadUserContext() {
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("iaderm_prefs", Context.MODE_PRIVATE);
            userMedicalProfileJSON = prefs.getString("user_medical_profile", "Sin información");
            recentAnalysisScore = prefs.getInt("latest_score", 0);
        }
    }

    private void sendMessageToGemini(String userText) {
        showUserMessage(userText);
        etMessage.setText("");

        try {
            JSONObject parts = new JSONObject();
            parts.put("text", userText);
            
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            userContent.put("parts", new JSONArray().put(parts));
            
            conversationHistory.put(userContent);
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", conversationHistory);
            
            String sysText = "Eres un asistente de dermatología extremadamente experto y compasivo, basado en el modelo Gemini 3.0 de Google. NO ERES MÉDICO. " +
                             "El usuario tiene un perfil clínico que debes considerar: " + userMedicalProfileJSON + ". " +
                             "Tu objetivo es explicar, educar y empatizar. " +
                             "REGLA CRÍTICA INQUEBRANTABLE: SIEMPRE recomienda consultar a un dermatólogo y BAJO NINGUNA CIRCUNSTANCIA recetes medicamentos.";
                             
            JSONObject sysParts = new JSONObject();
            sysParts.put("text", sysText);
            
            JSONObject systemInstruction = new JSONObject();
            systemInstruction.put("role", "system");
            systemInstruction.put("parts", new JSONArray().put(sysParts));
            
            requestBody.put("systemInstruction", systemInstruction);

            executor.execute(() -> {
                try {
                    URL url = new URL(GEMINI_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    os.write(requestBody.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String botReply = jsonResponse.getJSONArray("candidates")
                                                      .getJSONObject(0)
                                                      .getJSONObject("content")
                                                      .getJSONArray("parts")
                                                      .getJSONObject(0)
                                                      .getString("text");

                        JSONObject botParts = new JSONObject();
                        botParts.put("text", botReply);
                        
                        JSONObject botContent = new JSONObject();
                        botContent.put("role", "model");
                        botContent.put("parts", new JSONArray().put(botParts));
                        conversationHistory.put(botContent);

                        handler.post(() -> showBotMessage(botReply));
                    } else {
                        handler.post(() -> showBotMessage("Lo siento, los servidores están saturados o no encuentro el modelo (" + responseCode + ")"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(() -> showBotMessage("Error de conexión a internet."));
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error preparando el mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserMessage(String text) {
        addMessageBubble(text, true);
    }

    private void showBotMessage(String text) {
        addMessageBubble(text, false);
    }

    private void addMessageBubble(String text, boolean isUser) {
        if (getContext() == null) return;
        
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(32, 24, 32, 24);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 16, 16, 16);

        if (isUser) {
            params.gravity = Gravity.END;
            tv.setBackgroundResource(R.drawable.bg_indicator_badge);
            tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary, null)));
            tv.setTextColor(getResources().getColor(android.R.color.white, null));
            tv.setElevation(4f);
        } else {
            params.gravity = Gravity.START;
            tv.setBackgroundResource(R.drawable.bg_indicator_badge);
            tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.surface_variant, null)));
            tv.setTextColor(getResources().getColor(R.color.on_surface_variant, null));
            tv.setElevation(2f);
        }
        
        tv.setLayoutParams(params);
        chatContainer.addView(tv);
        
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }
}
