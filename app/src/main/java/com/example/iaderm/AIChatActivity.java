package com.example.iaderm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class AIChatActivity extends AppCompatActivity {

    private TextInputEditText etMessage;
    private FloatingActionButton btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollChat;

    private String userMedicalProfileJSON = "";
    private int recentAnalysisScore = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // API CONFIGURATION
    private static final String API_KEY = "AIzaSyDiBo_m0jzejO5OmZ3DjRuti8zOZWFZ3Dc";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
    
    // Conversation History
    private final JSONArray conversationHistory = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        chatContainer = findViewById(R.id.chatContainer);
        scrollChat = findViewById(R.id.scrollChat);

        loadUserContext();

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageToGemini(message);
            }
        });
        
        showBotMessage("Hola, soy tu asistente dermatológico con IA. He revisado tu perfil clínico y los resultados de tu escaneo visual. ¿En qué puedo ayudarte?");
    }

    private void loadUserContext() {
        SharedPreferences prefs = getSharedPreferences("iaderm_prefs", MODE_PRIVATE);
        userMedicalProfileJSON = prefs.getString("user_medical_profile", "Sin información");
        recentAnalysisScore = getIntent().getIntExtra(AppNavigator.EXTRA_SCORE, 0);
    }

    private void sendMessageToGemini(String userText) {
        showUserMessage(userText);
        etMessage.setText("");

        try {
            // Append user message to history
            JSONObject parts = new JSONObject();
            parts.put("text", userText);
            
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            userContent.put("parts", new JSONArray().put(parts));
            
            conversationHistory.put(userContent);
            
            // Build full payload
            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", conversationHistory);
            
            // System instructions (Persona)
            String sysText = "Eres un asistente de dermatología compasivo y experto. NO ERES MÉDICO. " +
                             "El usuario tiene una compatibilidad del " + recentAnalysisScore + "% con Rosácea. " +
                             "Su perfil clínico es: " + userMedicalProfileJSON + ". " +
                             "Tu objetivo es explicar la afección, dar consejos básicos y empatizar. " +
                             "SIEMPRE, de manera innegociable, recomienda consultar a un dermatólogo real y NUNCA recetes medicamentos.";
                             
            JSONObject sysParts = new JSONObject();
            sysParts.put("text", sysText);
            
            JSONObject systemInstruction = new JSONObject();
            systemInstruction.put("role", "system"); // or just direct depending on api version, but standard is system
            systemInstruction.put("parts", new JSONArray().put(sysParts));
            
            requestBody.put("systemInstruction", systemInstruction);

            // Make HTTP Request in background
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

                        // Parse response
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String botReply = jsonResponse.getJSONArray("candidates")
                                                      .getJSONObject(0)
                                                      .getJSONObject("content")
                                                      .getJSONArray("parts")
                                                      .getJSONObject(0)
                                                      .getString("text");

                        // Append bot reply to history
                        JSONObject botParts = new JSONObject();
                        botParts.put("text", botReply);
                        
                        JSONObject botContent = new JSONObject();
                        botContent.put("role", "model");
                        botContent.put("parts", new JSONArray().put(botParts));
                        conversationHistory.put(botContent);

                        // Update UI
                        handler.post(() -> showBotMessage(botReply));
                    } else {
                        handler.post(() -> showBotMessage("Error de conexión con la IA (" + responseCode + ")"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(() -> showBotMessage("Lo siento, hubo un error de red."));
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error preparando mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    private void showUserMessage(String text) {
        addMessageBubble(text, true);
    }

    private void showBotMessage(String text) {
        addMessageBubble(text, false);
    }

    private void addMessageBubble(String text, boolean isUser) {
        TextView tv = new TextView(this);
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
            tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.primary)));
            tv.setTextColor(getColor(android.R.color.white));
            tv.setElevation(4f);
        } else {
            params.gravity = Gravity.START;
            tv.setBackgroundResource(R.drawable.bg_indicator_badge); // Reusing bubble shape
            tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.surface_variant)));
            tv.setTextColor(getColor(R.color.on_surface_variant));
            tv.setElevation(2f);
        }
        
        tv.setLayoutParams(params);
        chatContainer.addView(tv);
        
        // Scroll to bottom
        scrollChat.post(() -> scrollChat.fullScroll(android.view.View.FOCUS_DOWN));
    }
}
