package com.example.teachertrackfinalnagidnaugotnako;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class signup extends AppCompatActivity {

    private TextInputEditText editTextIdNumber, editTextPassword, editTextBirthdate;
    private AutoCompleteTextView autoCompleteUserType;
    private MaterialButton signupButton;
    private Calendar calendar;

    private final String[] USER_TYPES = {"Teacher", "Checker"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        setupUserTypeDropdown();
        setupBirthdatePicker();
        setupSignupButton();

        TextView loginLink = findViewById(R.id.login);
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(signup.this, login.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            finish();
        });
    }

    private void initializeViews() {
        editTextIdNumber = findViewById(R.id.editTextIdNumber);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextBirthdate = findViewById(R.id.editTextBirthdate);
        autoCompleteUserType = findViewById(R.id.autoCompleteUserType);
        signupButton = findViewById(R.id.signupButton);
        calendar = Calendar.getInstance();
    }

    private void setupUserTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                USER_TYPES
        );
        autoCompleteUserType.setAdapter(adapter);
        autoCompleteUserType.setThreshold(1);
        autoCompleteUserType.setOnClickListener(v -> autoCompleteUserType.showDropDown());
        autoCompleteUserType.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) autoCompleteUserType.showDropDown();
        });
    }

    private void setupBirthdatePicker() {
        editTextBirthdate.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateBirthdateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateBirthdateDisplay() {
        String dateFormat = "MM/dd/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
        editTextBirthdate.setText(sdf.format(calendar.getTime()));
    }

    private void setupSignupButton() {
        signupButton.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        String idNumber = editTextIdNumber.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String userType = autoCompleteUserType.getText().toString().trim().toLowerCase();
        String birthdate = editTextBirthdate.getText().toString().trim();

        if (validateInputs(idNumber, password, userType, birthdate)) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("logins").document(idNumber).get().addOnSuccessListener(document -> {
                if (document.exists()) {
                    Toast.makeText(signup.this, "ID already registered. Please use a different one.", Toast.LENGTH_SHORT).show();
                } else {
                    Map<String, Object> loginData = new HashMap<>();
                    loginData.put("idNumber", idNumber);
                    loginData.put("password", password);

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("idNumber", idNumber);
                    userData.put("password", password);
                    userData.put("userType", userType);
                    userData.put("birthdate", birthdate);

                    db.collection("logins").document(idNumber).set(loginData)
                            .addOnSuccessListener(aVoid -> {
                                String targetCollection = userType.equals("checker") ? "checkers" : "teachers";

                                db.collection(targetCollection).document(idNumber).set(userData)
                                        .addOnSuccessListener(aVoid2 -> {
                                            Toast.makeText(signup.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            signupButton.setText("Continue to Login");
                                            signupButton.setOnClickListener(v -> {
                                                Intent intent = new Intent(signup.this, login.class);
                                                startActivity(intent);
                                                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                                finish();
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(signup.this, "Error saving user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(signup.this, "Error saving login credentials: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }
            });
        }
    }

    private boolean validateInputs(String idNumber, String password, String userType, String birthdate) {
        // Validate ID Number (must be exactly 10 digits)
        if (idNumber.isEmpty()) {
            editTextIdNumber.setError("ID Number is required");
            return false;
        }

        if (!isValidIdNumber(idNumber)) {
            editTextIdNumber.setError("ID Number must be exactly 10 digits");
            return false;
        }

        // Validate Password
        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            return false;
        }

        if (!isValidPassword(password)) {
            editTextPassword.setError("Password must be 8 characters with at least one capital letter and combination of letters and numbers");
            return false;
        }

        // Validate User Type
        if (userType.isEmpty()) {
            autoCompleteUserType.setError("User type is required");
            return false;
        }

        if (!isValidUserType(userType)) {
            autoCompleteUserType.setError("Please select Teacher or Checker");
            return false;
        }

        // Validate Birthdate
        if (birthdate.isEmpty()) {
            editTextBirthdate.setError("Birthdate is required");
            return false;
        }

        return true;
    }

    private boolean isValidIdNumber(String idNumber) {
        // Check if ID number is exactly 10 digits
        return Pattern.matches("^\\d{10}$", idNumber);
    }

    private boolean isValidPassword(String password) {
        // Password must be exactly 8 characters, contain at least one capital letter,
        // and be a combination of letters and numbers
        if (password.length() != 8) {
            return false;
        }

        // Check for at least one capital letter
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            return false;
        }

        // Check for at least one letter (can be upper or lower case)
        if (!Pattern.compile("[a-zA-Z]").matcher(password).find()) {
            return false;
        }

        // Check for at least one digit
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            return false;
        }

        // Check that it only contains letters and numbers (no special characters)
        return Pattern.matches("^[a-zA-Z0-9]{8}$", password);
    }

    private boolean isValidUserType(String userType) {
        for (String type : USER_TYPES) {
            if (type.equalsIgnoreCase(userType)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }
}