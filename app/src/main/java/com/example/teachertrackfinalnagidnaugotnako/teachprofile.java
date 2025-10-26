package com.example.teachertrackfinalnagidnaugotnako;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class teachprofile extends AppCompatActivity {

    private TextInputEditText editTeacherName, editTeacherPassword, editIdNumber, editTeacherBirthdate;
    private MaterialButton saveButton;
    private ImageView backButtonEdit;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore firestore;

    private String originalName = "", originalPassword = "", originalIdNumber = "", originalBirthdate = "";
    private boolean isFirstLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachprofile);

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);

        initializeViews();
        loadCurrentData();
        setupBackButton();
        setupSaveButton();
        setupBackPressHandler();
    }

    private void initializeViews() {
        backButtonEdit = findViewById(R.id.backButtonEdit);
        editTeacherName = findViewById(R.id.editTeacherName);
        editTeacherPassword = findViewById(R.id.editTeacherPassword);
        editIdNumber = findViewById(R.id.editIdNumber);
        editTeacherBirthdate = findViewById(R.id.editTeacherBirthdate);
        saveButton = findViewById(R.id.saveButton);
    }

    private void loadCurrentData() {
        String userId = getIntent().getStringExtra("user_id");
        isFirstLogin = getIntent().getBooleanExtra("isFirstLogin", false);

        if (userId == null || userId.trim().isEmpty()) {
            userId = sharedPreferences.getString("user_id", "");
        }

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        DocumentReference docRef = firestore.collection("teachers").document(userId);
        String finalUserId = userId;
        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                originalName = document.getString("username") != null ? document.getString("username") : "";
                originalPassword = document.getString("password") != null ? document.getString("password") : "";
                originalIdNumber = document.getString("idNumber") != null ? document.getString("idNumber") : finalUserId;
                originalBirthdate = document.getString("birthdate") != null ? document.getString("birthdate") : "";

                editTeacherName.setText(originalName);
                editTeacherPassword.setText(originalPassword);
                editIdNumber.setText(originalIdNumber);
                editTeacherBirthdate.setText(originalBirthdate);

                editTeacherName.setSelection(originalName.length());

                if (isFirstLogin) {
                    Toast.makeText(this, "Please fill up the necessary information", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Teacher profile not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("teachprofile", "Error fetching teacher", e);
            Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            String updatedName = editTeacherName.getText().toString().trim();
            String updatedPassword = editTeacherPassword.getText().toString().trim();
            String updatedIdNumber = editIdNumber.getText().toString().trim();

            if (updatedName.isEmpty()) {
                editTeacherName.setError("Name is required");
                if (isFirstLogin) {
                    Toast.makeText(this, "Please fill up the text fields to complete your profile.", Toast.LENGTH_LONG).show();
                }
                return;
            }

            if (updatedPassword.isEmpty()) {
                editTeacherPassword.setError("Password is required");
                return;
            }

            if (updatedIdNumber.isEmpty()) {
                editIdNumber.setError("ID Number is required");
                return;
            }

            // Validate password strength
            if (!isValidPassword(updatedPassword)) {
                editTeacherPassword.setError("Password must be 8 characters with at least one capital letter and combination of letters and numbers");
                return;
            }

            if (updatedName.equals(originalName) &&
                    updatedPassword.equals(originalPassword) &&
                    updatedIdNumber.equals(originalIdNumber)) {
                Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show();
                return;
            }

            showSaveConfirmationDialog(updatedName, updatedPassword, updatedIdNumber);
        });
    }

    private boolean isValidPassword(String password) {
        // Password must be exactly 8 characters, contain at least one capital letter,
        // and be a combination of letters and numbers
        if (password.length() != 8) {
            return false;
        }

        // Check for at least one capital letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }

        // Check for at least one letter (can be upper or lower case)
        if (!password.matches(".*[a-zA-Z].*")) {
            return false;
        }

        // Check for at least one digit
        if (!password.matches(".*[0-9].*")) {
            return false;
        }

        // Check that it only contains letters and numbers (no special characters)
        return password.matches("^[a-zA-Z0-9]{8}$");
    }

    private void showSaveConfirmationDialog(String updatedName, String updatedPassword, String updatedIdNumber) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Confirm Changes");

        // Build confirmation message based on changes
        StringBuilder message = new StringBuilder("Are you sure you want to save the following changes?\n\n");

        if (!updatedName.equals(originalName)) {
            message.append("• Name: ").append(originalName).append(" → ").append(updatedName).append("\n");
        }

        if (!updatedPassword.equals(originalPassword)) {
            message.append("• Password: Updated\n");
        }

        if (!updatedIdNumber.equals(originalIdNumber)) {
            message.append("• ID Number: ").append(originalIdNumber).append(" → ").append(updatedIdNumber).append("\n");
        }

        message.append("\nThis action cannot be undone.");

        builder.setMessage(message.toString());
        builder.setPositiveButton("Yes, Save Changes", (dialog, which) -> {
            checkIdAvailabilityAndUpdate(updatedName, updatedPassword, updatedIdNumber);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled, do nothing
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void checkIdAvailabilityAndUpdate(String updatedName, String updatedPassword, String updatedIdNumber) {
        String currentUserId = sharedPreferences.getString("user_id", "");

        if (updatedIdNumber.equals(currentUserId)) {
            showUpdatingDialog();
            updateTeacherInFirebase(updatedName, updatedPassword, updatedIdNumber, false);
        } else {
            showCheckingIdDialog();
            firestore.collection("logins").document(updatedIdNumber).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    editIdNumber.setError("ID Number already in use");
                    Toast.makeText(this, "ID already exists. Choose a different one.", Toast.LENGTH_SHORT).show();
                } else {
                    showUpdatingDialog();
                    updateTeacherInFirebase(updatedName, updatedPassword, updatedIdNumber, true);
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error checking ID availability", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showCheckingIdDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Checking ID Availability");
        builder.setMessage("Please wait while we verify the ID Number...");
        builder.setCancelable(false);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Auto-dismiss after 3 seconds (safety measure)
        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 3000);
    }

    private void showUpdatingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Updating Profile");
        builder.setMessage("Please wait while we save your changes...");
        builder.setCancelable(false);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Auto-dismiss after 5 seconds (safety measure)
        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                showErrorDialog("Update is taking longer than expected. Please check your connection.");
            }
        }, 5000);
    }

    private void updateTeacherInFirebase(String updatedName, String updatedPassword, String updatedIdNumber, boolean idChanged) {
        String currentUserId = sharedPreferences.getString("user_id", "");

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", updatedName);
        userData.put("password", updatedPassword);
        userData.put("idNumber", updatedIdNumber);
        userData.put("userType", "teacher");
        userData.put("isProfileComplete", true);
        userData.put("birthdate", originalBirthdate); // ✅ Preserve birthdate

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (idChanged) {
            db.collection("teachers").document(currentUserId).delete();
            db.collection("logins").document(currentUserId).delete();

            db.collection("teachers").document(updatedIdNumber).set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> loginData = new HashMap<>();
                        loginData.put("idNumber", updatedIdNumber);
                        loginData.put("password", updatedPassword);

                        db.collection("logins").document(updatedIdNumber).set(loginData)
                                .addOnSuccessListener(aVoid2 -> {
                                    showSuccessDialog();
                                    updateSessionAndRedirect(updatedIdNumber, updatedName, updatedPassword);
                                })
                                .addOnFailureListener(e -> {
                                    showErrorDialog("Failed to save login credentials. Please try again.");
                                });
                    })
                    .addOnFailureListener(e -> {
                        showErrorDialog("Failed to save teacher profile. Please try again.");
                    });
        } else {
            db.collection("teachers").document(currentUserId).set(userData)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("logins").document(currentUserId)
                                .update("idNumber", updatedIdNumber, "password", updatedPassword)
                                .addOnSuccessListener(aVoid2 -> {
                                    showSuccessDialog();
                                    updateSessionAndRedirect(updatedIdNumber, updatedName, updatedPassword);
                                })
                                .addOnFailureListener(e -> {
                                    showErrorDialog("Failed to update login credentials. Please try again.");
                                });
                    })
                    .addOnFailureListener(e -> {
                        showErrorDialog("Failed to update teacher profile. Please try again.");
                    });
        }
    }

    private void showSuccessDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Profile Updated Successfully ✓");
        builder.setMessage("Your profile has been updated successfully! Redirecting to dashboard...");
        builder.setCancelable(false);

        android.app.AlertDialog dialog = builder.show();

        // Auto-dismiss after 2 seconds and proceed
        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 2000);
    }

    private void showErrorDialog(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Update Failed");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void updateSessionAndRedirect(String newId, String name, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", newId);
        editor.putString("user_name", name);
        editor.putString("user_password", password);
        editor.apply();

        Intent intent = new Intent(teachprofile.this, dashboardteacher.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupBackButton() {
        backButtonEdit.setOnClickListener(v -> {
            if (hasUnsavedChanges()) {
                showUnsavedChangesDialog();
            } else {
                navigateBack();
            }
        });
    }

    private boolean hasUnsavedChanges() {
        String currentName = editTeacherName.getText().toString().trim();
        String currentPassword = editTeacherPassword.getText().toString().trim();
        String currentIdNumber = editIdNumber.getText().toString().trim();

        return !currentName.equals(originalName) ||
                !currentPassword.equals(originalPassword) ||
                !currentIdNumber.equals(originalIdNumber);
    }

    private void showUnsavedChangesDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Unsaved Changes");
        builder.setMessage("You have unsaved changes. Are you sure you want to discard them?");
        builder.setPositiveButton("Discard", (dialog, which) -> {
            navigateBack();
        });
        builder.setNegativeButton("Keep Editing", null);
        builder.setCancelable(true);
        builder.show();
    }

    private void navigateBack() {
        setResult(RESULT_CANCELED);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    showUnsavedChangesDialog();
                } else {
                    navigateBack();
                }
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(teachprofile.this, login.class);
        startActivity(intent);
        finish();
    }
}