package com.example.teachertrackfinalnagidnaugotnako;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText editTextIdNumber, editTextPassword;
    private Button loginButton;
    private TextView signupText, forgotPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_login);
            Log.d(TAG, "Layout loaded successfully");

            editTextIdNumber = findViewById(R.id.editTextIdNumber);
            editTextPassword = findViewById(R.id.editTextPassword);
            loginButton = findViewById(R.id.loginButton);
            signupText = findViewById(R.id.signupLink);
            forgotPasswordText = findViewById(R.id.forgotPassword);

            loginButton.setOnClickListener(v -> handleLogin());

            signupText.setOnClickListener(v -> {
                Intent intent = new Intent(login.this, signup.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                finish();
            });

            forgotPasswordText.setOnClickListener(v -> {
                Intent intent = new Intent(login.this, forgotpassword.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error initializing login: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleLogin() {
        String idNumber = editTextIdNumber.getText() != null ? editTextIdNumber.getText().toString().trim() : "";
        String password = editTextPassword.getText() != null ? editTextPassword.getText().toString().trim() : "";

        // Validate ID number format
        if (idNumber.isEmpty() || password.isEmpty()) {
            showEmptyFieldsDialog();
            return;
        }

        // Validate ID number is 10 digits
        if (!idNumber.matches("^\\d{10}$")) {
            showInvalidIdFormatDialog();
            return;
        }

        showLoadingDialog();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check teachers collection
        DocumentReference teacherRef = db.collection("teachers").document(idNumber);
        teacherRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String storedPassword = document.getString("password");
                if (storedPassword != null && storedPassword.equals(password)) {
                    String userType = document.getString("userType");
                    String username = document.getString("username");

                    // Handle missing username
                    if (username == null || username.isEmpty()) {
                        username = "Teacher"; // Default name
                    }

                    // Handle missing userType
                    if (userType == null || userType.isEmpty()) {
                        userType = "teacher"; // Default type
                    }

                    showLoginSuccessDialog(userType, idNumber, username);
                } else {
                    showInvalidCredentialsDialog();
                }
            } else {
                // Check checkers collection
                DocumentReference checkerRef = db.collection("checkers").document(idNumber);
                checkerRef.get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String storedPassword = doc.getString("password");
                        if (storedPassword != null && storedPassword.equals(password)) {
                            String userType = doc.getString("userType");
                            String username = doc.getString("username");

                            // Handle missing username
                            if (username == null || username.isEmpty()) {
                                username = "Checker"; // Default name
                            }

                            // Handle missing userType
                            if (userType == null || userType.isEmpty()) {
                                userType = "checker"; // Default type
                            }

                            showLoginSuccessDialog(userType, idNumber, username);
                        } else {
                            showInvalidCredentialsDialog();
                        }
                    } else {
                        showUserNotFoundDialog();
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching checker profile", e);
                    showNetworkErrorDialog("Error loading user profile. Please check your connection.");
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching teacher profile", e);
            showNetworkErrorDialog("Error connecting to server. Please try again.");
        });
    }

    private void showLoadingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Logging In");
        builder.setMessage("Please wait while we verify your credentials...");
        builder.setCancelable(false);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Auto-dismiss after 5 seconds if still loading (safety measure)
        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                showNetworkErrorDialog("Login is taking longer than expected. Please check your connection.");
            }
        }, 5000);
    }

    private void showEmptyFieldsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Missing Information");
        builder.setMessage("Please enter both ID Number and Password to continue.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Focus on the first empty field
            if (editTextIdNumber.getText().toString().trim().isEmpty()) {
                editTextIdNumber.requestFocus();
            } else {
                editTextPassword.requestFocus();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void showInvalidIdFormatDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Invalid ID Format");
        builder.setMessage("ID Number must be exactly 10 digits. Please check your ID and try again.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            editTextIdNumber.setText("");
            editTextIdNumber.requestFocus();
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void showInvalidCredentialsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Invalid Credentials");
        builder.setMessage("The ID Number or Password you entered is incorrect.\n\nPlease check your credentials and try again.");
        builder.setPositiveButton("Try Again", (dialog, which) -> {
            editTextPassword.setText("");
            editTextPassword.requestFocus();
        });
        builder.setNegativeButton("Forgot Password?", (dialog, which) -> {
            Intent intent = new Intent(login.this, forgotpassword.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void showUserNotFoundDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Account Not Found");
        builder.setMessage("No account found with this ID Number.\n\nIf you don't have an account, please sign up first.");
        builder.setPositiveButton("Sign Up", (dialog, which) -> {
            Intent intent = new Intent(login.this, signup.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            finish();
        });
        builder.setNegativeButton("Try Again", (dialog, which) -> {
            editTextIdNumber.setText("");
            editTextPassword.setText("");
            editTextIdNumber.requestFocus();
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void showNetworkErrorDialog(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Connection Error");
        builder.setMessage(message);
        builder.setPositiveButton("Retry", (dialog, which) -> {
            handleLogin(); // Retry login
        });
        builder.setNegativeButton("Cancel", null);
        builder.setCancelable(true);
        builder.show();
    }

    private void showLoginSuccessDialog(String userType, String idNumber, String username) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Login Successful ✓");

        String welcomeMessage;
        if ("checker".equalsIgnoreCase(userType)) {
            welcomeMessage = "Welcome, Checker " + (username != null ? username : "") + "!";
        } else {
            welcomeMessage = "Welcome, Teacher " + (username != null ? username : "") + "!";
        }

        builder.setMessage(welcomeMessage + "\n\nRedirecting to your dashboard...");
        builder.setCancelable(false);

        // Auto-proceed after 2 seconds
        android.app.AlertDialog dialog = builder.show();

        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                proceedToDashboard(userType, idNumber, username);
            }
        }, 2000);
    }

    private void proceedToDashboard(String userType, String idNumber, String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("user_id", idNumber);
        editor.putString("user_type", userType);
        editor.putString("user_name", username);
        editor.apply();

        Intent intent;
        if ("checker".equalsIgnoreCase(userType)) {
            intent = new Intent(login.this, dashboardchecker.class);
        } else {
            intent = new Intent(login.this, dashboardteacher.class);
        }

        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        finish();
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }
}