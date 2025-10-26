package com.example.teachertrackfinalnagidnaugotnako;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private AlertDialog noInternetDialog;
    private Handler handler = new Handler();
    private boolean isCheckingConnection = false;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);

        // Initial delay to simulate loading
        handler.postDelayed(this::checkConnectionAndProceed, 3000);
    }

    private void checkConnectionAndProceed() {
        if (isConnectedToInternet()) {
            checkAutoLogin();
        } else {
            showNoInternetDialog();
            startConnectionWatcher();
        }
    }

    private void checkAutoLogin() {
        String userId = sharedPreferences.getString("user_id", "");
        String userType = sharedPreferences.getString("user_type", "");
        boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);

        Log.d("AutoLogin", "User ID: " + userId + ", User Type: " + userType + ", Is Logged In: " + isLoggedIn);

        // Check if user is already logged in and has valid credentials
        if (isLoggedIn && !userId.isEmpty() && !userType.isEmpty()) {
            // User is already logged in, redirect to appropriate dashboard
            redirectToDashboard(userType);
        } else {
            // No saved login or invalid credentials, go to login screen
            goToLogin();
        }
    }

    private void redirectToDashboard(String userType) {
        Intent intent;

        switch (userType.toLowerCase()) {
            case "teacher":
                intent = new Intent(MainActivity.this, dashboardteacher.class);
                Log.d("AutoLogin", "Redirecting to Teacher Dashboard");
                break;
            case "checker":
                intent = new Intent(MainActivity.this, dashboardchecker.class);
                Log.d("AutoLogin", "Redirecting to Checker Dashboard");
                break;
            default:
                // Fallback to login if user type is unknown
                Log.d("AutoLogin", "Unknown user type, redirecting to login");
                intent = new Intent(MainActivity.this, login.class);
                break;
        }

        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        finish();
    }

    private void startConnectionWatcher() {
        if (isCheckingConnection) return; // prevent multiple loops
        isCheckingConnection = true;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnectedToInternet()) {
                    if (noInternetDialog != null && noInternetDialog.isShowing()) {
                        noInternetDialog.dismiss();
                    }
                    checkAutoLogin(); // Check auto login when connection is restored
                } else {
                    handler.postDelayed(this, 3000);
                }
            }
        }, 3000);
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("⚠️ No Internet Connection")
                .setMessage("Please connect to Wi-Fi or mobile data to continue.")
                .setCancelable(false)
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (isConnectedToInternet()) {
                        dialog.dismiss();
                        checkAutoLogin();
                    } else {
                        dialog.dismiss();
                        // Wait 10 seconds before showing the dialog again
                        handler.postDelayed(() -> {
                            if (!isConnectedToInternet()) {
                                showNoInternetDialog();
                            }
                        }, 10000);
                    }
                })
                .setNegativeButton("Exit App", (dialog, which) -> {
                    dialog.dismiss();
                    finishAffinity();
                });

        noInternetDialog = builder.create();
        noInternetDialog.show();
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, login.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        finish();
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}