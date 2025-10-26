package com.example.teachertrackfinalnagidnaugotnako;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class checkerprof extends AppCompatActivity {

    private TextInputEditText editCheckerName, editIdNumber, editTeacherPassword, editTeacherBirthdate;
    private MaterialButton saveButton;
    private ImageView backButtonEdit;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private String currentUserId;
    private boolean isFirstLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkerprof);

        // FIXED: Remove the ViewCompat.setOnApplyWindowInsetsListener since there's no R.id.main
        // Or if you want to keep the edge-to-edge, use the correct root view ID
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("user_id", "");

        // Check if this is first login
        isFirstLogin = getIntent().getBooleanExtra("isFirstLogin", false);

        initializeViews();
        setupBackButton();
        loadCurrentProfileData();
        setupSaveButton();
    }

    private void initializeViews() {
        editCheckerName = findViewById(R.id.editCheckerName);
        editIdNumber = findViewById(R.id.editIdNumber);
        editTeacherPassword = findViewById(R.id.editTeacherPassword);
        editTeacherBirthdate = findViewById(R.id.editTeacherBirthdate);
        saveButton = findViewById(R.id.saveButton);
        backButtonEdit = findViewById(R.id.backButtonEdit);

        // Set current date as birthdate (non-editable)
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        editTeacherBirthdate.setText(currentDate);

        // Make birthdate non-editable
        editTeacherBirthdate.setEnabled(false);
        editTeacherBirthdate.setFocusable(false);
    }

    private void setupBackButton() {
        backButtonEdit.setOnClickListener(v -> {
            if (isFirstLogin) {
                // If first login, don't allow going back without completing profile
                Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
            } else {
                finish();
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            }
        });
    }

    private void loadCurrentProfileData() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DocumentReference docRef = firestore.collection("checkers").document(currentUserId);
        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String username = document.getString("username");
                String idNumber = document.getString("idNumber");
                String password = document.getString("password");
                String birthdate = document.getString("birthdate");

                if (username != null) editCheckerName.setText(username);
                if (idNumber != null) editIdNumber.setText(idNumber);
                if (password != null) editTeacherPassword.setText(password);
                if (birthdate != null) editTeacherBirthdate.setText(birthdate);
            } else {
                // If document doesn't exist, create a new one with default values
                createNewCheckerProfile();
            }
        }).addOnFailureListener(e -> {
            Log.e("CheckerProf", "Error loading profile data", e);
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void createNewCheckerProfile() {
        // Create a new profile with default values
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> newProfile = new HashMap<>();
        newProfile.put("username", currentUserId); // Use user ID as default name
        newProfile.put("idNumber", currentUserId);
        newProfile.put("password", "default123"); // Default password
        newProfile.put("birthdate", currentDate);
        newProfile.put("isProfileComplete", false);
        newProfile.put("userType", "checker");

        firestore.collection("checkers").document(currentUserId)
                .set(newProfile)
                .addOnSuccessListener(aVoid -> {
                    // Set the default values in the form
                    editCheckerName.setText(currentUserId);
                    editIdNumber.setText(currentUserId);
                    editTeacherPassword.setText("default123");
                    editTeacherBirthdate.setText(currentDate);
                    Toast.makeText(this, "New profile created. Please update your information.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("CheckerProf", "Error creating new profile", e);
                    Toast.makeText(this, "Error creating profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void saveProfileChanges() {
        String checkerName = editCheckerName.getText() != null ? editCheckerName.getText().toString().trim() : "";
        String idNumber = editIdNumber.getText() != null ? editIdNumber.getText().toString().trim() : "";
        String password = editTeacherPassword.getText() != null ? editTeacherPassword.getText().toString().trim() : "";
        String birthdate = editTeacherBirthdate.getText() != null ? editTeacherBirthdate.getText().toString().trim() : "";

        // Validation
        if (checkerName.isEmpty() || idNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firestore
        DocumentReference docRef = firestore.collection("checkers").document(currentUserId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", checkerName);
        updates.put("idNumber", idNumber);
        updates.put("password", password);
        updates.put("birthdate", birthdate);
        updates.put("isProfileComplete", true);
        updates.put("userType", "checker");

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Update SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_name", checkerName);
                    editor.apply();

                    if (isFirstLogin) {
                        // If first login, go back to dashboard
                        Intent intent = new Intent(checkerprof.this, dashboardchecker.class);
                        startActivity(intent);
                        finish();
                        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    } else {
                        // If editing existing profile, just go back
                        finish();
                        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CheckerProf", "Error updating profile", e);
                    Toast.makeText(this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (isFirstLogin) {
            // If first login, don't allow going back without completing profile
            Toast.makeText(this, "Please complete your profile first", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        }
    }
}