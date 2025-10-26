package com.example.teachertrackfinalnagidnaugotnako;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.DatePicker;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class forgotpassword extends AppCompatActivity {

    private TextInputEditText editIdNumber, editBirthdate, editNewPassword, editConfirmPassword;
    private TextInputLayout newPasswordLayout, confirmPasswordLayout;
    private MaterialButton findAccountButton, resetPasswordButton, backButton;
    private LinearLayout passwordFieldsLayout;
    private FirebaseFirestore firestore;

    private String verifiedIdNumber;
    private String verifiedUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);

        firestore = FirebaseFirestore.getInstance();
        initializeViews();
        setupListeners();

        // Initially hide password fields
        hidePasswordFields();
    }

    private void initializeViews() {
        editIdNumber = findViewById(R.id.editIdNumber);
        editBirthdate = findViewById(R.id.editBirthdate);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        findAccountButton = findViewById(R.id.findAccountButton);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        backButton = findViewById(R.id.backButton);

        passwordFieldsLayout = findViewById(R.id.passwordFieldsLayout);
    }

    private void setupListeners() {
        editBirthdate.setOnClickListener(v -> showDatePicker());
        findAccountButton.setOnClickListener(v -> attemptFindAccount());
        resetPasswordButton.setOnClickListener(v -> attemptResetPassword());
        backButton.setOnClickListener(v -> goBackToLogin());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d",
                            selectedMonth + 1, selectedDay, selectedYear);
                    editBirthdate.setText(formattedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void attemptFindAccount() {
        String id = editIdNumber.getText().toString().trim();
        String birthdate = editBirthdate.getText().toString().trim();

        if (id.isEmpty() || birthdate.isEmpty()) {
            Toast.makeText(this, "Please enter ID Number and Birthdate", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate ID number format (10 digits)
        if (!id.matches("^\\d{10}$")) {
            editIdNumber.setError("ID Number must be exactly 10 digits");
            return;
        }

        showSearchingDialog();
        findAccountInCollection("teachers", id, birthdate);
    }

    private void showSearchingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Searching for Account");
        builder.setMessage("Please wait while we verify your account...");
        builder.setCancelable(false);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Auto-dismiss after 3 seconds if still searching
        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 3000);
    }

    private void findAccountInCollection(String collection, String id, String birthdate) {
        DocumentReference docRef = firestore.collection(collection).document(id);
        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String storedBirthdate = document.getString("birthdate");
                if (storedBirthdate != null && storedBirthdate.equals(birthdate)) {
                    // Account found and verified
                    verifiedIdNumber = id;
                    verifiedUserType = collection;
                    showAccountFoundConfirmation();
                } else {
                    if (collection.equals("teachers")) {
                        findAccountInCollection("checkers", id, birthdate);
                    } else {
                        showAccountNotFoundDialog();
                    }
                }
            } else {
                if (collection.equals("teachers")) {
                    findAccountInCollection("checkers", id, birthdate);
                } else {
                    showAccountNotFoundDialog();
                }
            }
        }).addOnFailureListener(e -> {
            showErrorDialog("Error verifying account. Please check your connection and try again.");
        });
    }

    private void showAccountFoundConfirmation() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Account Found ✓");
        builder.setMessage("Your account has been successfully verified! You can now set your new password.");
        builder.setPositiveButton("Continue", (dialog, which) -> {
            showPasswordFields();
            Toast.makeText(this, "Account verified! Set your new password", Toast.LENGTH_SHORT).show();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void showAccountNotFoundDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Account Not Found");
        builder.setMessage("We couldn't find an account matching the provided ID Number and Birthdate.\n\nPlease check your information and try again.");
        builder.setPositiveButton("Try Again", (dialog, which) -> {
            // Clear fields for retry
            editIdNumber.setText("");
            editBirthdate.setText("");
            editIdNumber.requestFocus();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User can choose to go back
        });
        builder.show();
    }

    private void showErrorDialog(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void attemptResetPassword() {
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in both password fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            editConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Validate password strength
        if (!isValidPassword(newPassword)) {
            editNewPassword.setError("Password must be 8 characters with at least one capital letter and combination of letters and numbers");
            return;
        }

        showResetConfirmationDialog(newPassword);
    }

    private void showResetConfirmationDialog(String newPassword) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Confirm Password Reset");
        builder.setMessage("Are you sure you want to reset your password? This action cannot be undone.");
        builder.setPositiveButton("Yes, Reset Password", (dialog, which) -> {
            resetPassword(verifiedIdNumber, newPassword, verifiedUserType);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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

    private void resetPassword(String id, String newPassword, String userType) {
        String collection = userType.equals("teachers") ? "teachers" : "checkers";

        // Show loading dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Resetting Password");
        builder.setMessage("Please wait while we update your password...");
        builder.setCancelable(false);
        android.app.AlertDialog loadingDialog = builder.create();
        loadingDialog.show();

        DocumentReference docRef = firestore.collection(collection).document(id);
        docRef.update("password", newPassword)
                .addOnSuccessListener(aVoid -> {
                    // Also update in logins collection
                    firestore.collection("logins").document(id).update("password", newPassword)
                            .addOnSuccessListener(aVoid2 -> {
                                loadingDialog.dismiss();
                                showResetSuccessDialog();
                            })
                            .addOnFailureListener(e -> {
                                loadingDialog.dismiss();
                                showErrorDialog("Password was reset but there was an issue updating login credentials. Please try logging in.");
                            });
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    showErrorDialog("Error resetting password. Please try again.");
                });
    }

    private void showResetSuccessDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Password Reset Successful ✓");
        builder.setMessage("Your password has been successfully reset! You can now log in with your new password.");
        builder.setPositiveButton("Go to Login", (dialog, which) -> {
            Intent intent = new Intent(forgotpassword.this, login.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void hidePasswordFields() {
        if (passwordFieldsLayout != null) {
            passwordFieldsLayout.setVisibility(View.GONE);
        }
        resetPasswordButton.setVisibility(View.GONE);
        findAccountButton.setVisibility(View.VISIBLE);
    }

    private void showPasswordFields() {
        if (passwordFieldsLayout != null) {
            passwordFieldsLayout.setVisibility(View.VISIBLE);
        }
        resetPasswordButton.setVisibility(View.VISIBLE);
        findAccountButton.setVisibility(View.GONE);

        // Clear any previous password entries
        editNewPassword.setText("");
        editConfirmPassword.setText("");

        // Focus on new password field
        editNewPassword.requestFocus();
    }

    private void goBackToLogin() {
        Intent intent = new Intent(forgotpassword.this, login.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        finish();
    }

    @Override
    public void onBackPressed() {
        goBackToLogin();
    }
}