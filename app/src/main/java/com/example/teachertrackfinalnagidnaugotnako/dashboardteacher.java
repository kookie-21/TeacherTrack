package com.example.teachertrackfinalnagidnaugotnako;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

public class dashboardteacher extends AppCompatActivity {

    private TextView teacherName, teacherId;
    private MaterialButton logoutButton, exitButton;
    private ImageView qrCodeImage;
    private SharedPreferences sharedPreferences;
    private FirebaseFirestore firestore;
    private ListenerRegistration realtimeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboardteacher);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);

        initializeViews();
        checkProfileCompletion();
        setupLogoutButton();
        setupExitButton();
        setupMenuButton();
    }

    private void initializeViews() {
        teacherName = findViewById(R.id.teacherName);
        teacherId = findViewById(R.id.teacherId);
        logoutButton = findViewById(R.id.logoutButton);
        exitButton = findViewById(R.id.exitButton);
        qrCodeImage = findViewById(R.id.qrCodeImage);
    }

    private void checkProfileCompletion() {
        String userId = sharedPreferences.getString("user_id", "").trim();
        if (userId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        DocumentReference docRef = firestore.collection("teachers").document(userId);
        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                Boolean isComplete = document.getBoolean("isProfileComplete");
                String username = document.getString("username");
                String password = document.getString("password");

                if (isComplete == null || !isComplete) {
                    Intent intent = new Intent(dashboardteacher.this, teachprofile.class);
                    intent.putExtra("user_id", userId);
                    intent.putExtra("username", username);
                    intent.putExtra("password", password);
                    intent.putExtra("isFirstLogin", true);
                    startActivity(intent);
                    finish();
                } else {
                    setupRealtimeListener();
                }
            } else {
                Toast.makeText(this, "Teacher profile not found", Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }
        }).addOnFailureListener(e -> {
            Log.e("Dashboard", "Error checking profile completion", e);
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRealtimeListener() {
        String userId = sharedPreferences.getString("user_id", "").trim();
        DocumentReference docRef = firestore.collection("teachers").document(userId);
        realtimeListener = docRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                Log.e("Dashboard", "Real-time listener error", error);
                return;
            }

            String updatedName = documentSnapshot.getString("username");
            String updatedId = documentSnapshot.getString("idNumber");

            if (updatedName != null && !updatedName.equals(teacherName.getText().toString())) {
                teacherName.setText(updatedName);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("user_name", updatedName);
                editor.apply();
            }

            if (updatedId != null && !updatedId.equals(teacherId.getText().toString())) {
                teacherId.setText(updatedId);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("user_id", updatedId);
                editor.apply();
                generateQRCode(updatedId);
            }
        });
    }

    private void generateQRCode(String userId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(userId, BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImage.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR Code", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    private void showLogoutConfirmationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Confirm Logout");
        builder.setMessage("Are you sure you want to logout from your account?\n\nYou will need to login again to access your dashboard.");
        builder.setPositiveButton("Yes, Logout", (dialog, which) -> {
            performLogout();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled, do nothing
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void performLogout() {
        if (realtimeListener != null) {
            realtimeListener.remove();
        }

        // Show logout progress
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Logging Out");
        builder.setMessage("Please wait while we sign you out...");
        builder.setCancelable(false);
        android.app.AlertDialog progressDialog = builder.create();
        progressDialog.show();

        // Clear shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Dismiss progress and show success message
        new android.os.Handler().postDelayed(() -> {
            progressDialog.dismiss();
            showLogoutSuccessDialog();
        }, 1000);
    }

    private void showLogoutSuccessDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Logged Out Successfully");
        builder.setMessage("You have been successfully logged out. Redirecting to login screen...");
        builder.setCancelable(false);

        android.app.AlertDialog dialog = builder.show();

        // Auto-redirect after 2 seconds
        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            redirectToLogin();
        }, 2000);
    }

    private void setupExitButton() {
        exitButton.setOnClickListener(v -> {
            showExitConfirmationDialog();
        });
    }

    private void showExitConfirmationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Exit Application");
        builder.setMessage("Are you sure you want to exit the app?\n\nAny unsaved data will be lost.");
        builder.setPositiveButton("Yes, Exit", (dialog, which) -> {
            performExit();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled, do nothing
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void performExit() {
        if (realtimeListener != null) {
            realtimeListener.remove();
        }

        // Show exit progress
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Exiting App");
        builder.setMessage("Closing application...");
        builder.setCancelable(false);
        android.app.AlertDialog progressDialog = builder.create();
        progressDialog.show();

        // Clear session data
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Dismiss progress and exit
        new android.os.Handler().postDelayed(() -> {
            progressDialog.dismiss();
            finishAffinity(); // Closes all activities
            System.exit(0);   // Exits the app process
        }, 1000);
    }

    private void setupMenuButton() {
        ImageView menuButton = findViewById(R.id.profilebutton);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(dashboardteacher.this, teachprofile.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Cannot open profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(dashboardteacher.this, login.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        showExitConfirmationDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeListener != null) {
            realtimeListener.remove();
        }
    }
}