package com.example.iaderm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

public class QuestionnaireActivity extends AppCompatActivity {

    private RadioGroup rgDuration, rgSymptoms, rgTriggers, rgTreatments, rgSkinType, rgAllergies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        
        rgDuration = findViewById(R.id.rgDuration);
        rgSymptoms = findViewById(R.id.rgSymptoms);
        rgTriggers = findViewById(R.id.rgTriggers);
        rgTreatments = findViewById(R.id.rgTreatments);
        rgSkinType = findViewById(R.id.rgSkinType);
        rgAllergies = findViewById(R.id.rgAllergies);

        MaterialButton btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        try {
            JSONObject profileJson = new JSONObject();
            profileJson.put("duration", getSelectedText(rgDuration, "No especificado"));
            profileJson.put("symptoms", getSelectedText(rgSymptoms, "No especificado"));
            profileJson.put("triggers", getSelectedText(rgTriggers, "No especificado"));
            profileJson.put("treatments", getSelectedText(rgTreatments, "No especificado"));
            profileJson.put("skin_type", getSelectedText(rgSkinType, "No especificado"));
            profileJson.put("allergies", getSelectedText(rgAllergies, "No especificado"));

            SharedPreferences prefs = getSharedPreferences("iaderm_prefs", MODE_PRIVATE);
            prefs.edit()
                 .putString("user_medical_profile", profileJson.toString())
                 .putBoolean("has_completed_questionnaire", true)
                 .apply();

            Toast.makeText(this, "Perfil clínico guardado correctamente.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar el perfil", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSelectedText(RadioGroup group, String defaultText) {
        int selectedId = group.getCheckedRadioButtonId();
        if (selectedId != -1) {
            android.widget.RadioButton rb = findViewById(selectedId);
            return rb.getText().toString();
        }
        return defaultText;
    }
}
