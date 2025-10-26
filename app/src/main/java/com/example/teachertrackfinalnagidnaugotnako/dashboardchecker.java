package com.example.teachertrackfinalnagidnaugotnako;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class dashboardchecker extends AppCompatActivity {

    private static final String TAG = "DashboardChecker";

    private TextView checkerNameTextView, checkerIdTextView;
    private LinearLayout scheduleContainer;
    private MaterialButton logoutButton, exitButton;
    private ImageView checkerImage;

    private SharedPreferences sharedPreferences;
    private String currentUserId, currentUserType;
    private FirebaseFirestore firestore;

    private List<Schedule> schedules = new ArrayList<>();
    private Schedule currentSelectedSchedule;
    private String currentFileId = null;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openScanner();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }
                String scannedData = result.getContents();
                String numbersOnly = extractNumbersOnly(scannedData);

                if (currentSelectedSchedule != null) {
                    markAttendance(currentSelectedSchedule, numbersOnly);
                } else {
                    Toast.makeText(this, "No schedule selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboardchecker);

        Log.d(TAG, "DashboardChecker onCreate started");

        try {
            sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);
            currentUserId = sharedPreferences.getString("user_id", "");
            currentUserType = sharedPreferences.getString("user_type", "");

            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                redirectToLogin();
                return;
            }

            if (!"checker".equals(currentUserType)) {
                Toast.makeText(this, "Access denied. Checker privileges required.", Toast.LENGTH_SHORT).show();
                redirectToLogin();
                return;
            }

            firestore = FirebaseFirestore.getInstance();

            initializeViews();
            setCheckerInfo();
            setupBottomButtons();
            setupProfileImageClickListener();

            // Check if profile is complete for first-time users
            checkProfileCompletion();

            checkForSelectedFile();
            checkIfFilesExist(); // Check if any files exist first
            loadScheduleData();

            MaterialButton openFilesButton = findViewById(R.id.openFilesButton);
            if (openFilesButton != null) {
                openFilesButton.setOnClickListener(v -> {
                    Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout);
                    v.startAnimation(fadeOut);

                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            startActivity(new Intent(dashboardchecker.this, files.class));
                            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                });
            }

            Log.d(TAG, "DashboardChecker onCreate completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "DashboardChecker onResume");
        try {
            checkForSelectedFile();
            checkIfFilesExist();
            loadScheduleData();
            loadCheckerProfileData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }

    // NEW METHOD: Check if any files exist in uploaded_files collection
    private void checkIfFilesExist() {
        Log.d(TAG, "Checking if files exist");
        try {
            firestore.collection("uploaded_files")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        boolean hasFiles = !queryDocumentSnapshots.isEmpty();
                        Log.d(TAG, "Files exist: " + hasFiles);
                        updateScheduleContainerVisibility(hasFiles);

                        if (!hasFiles) {
                            if (scheduleContainer != null) {
                                scheduleContainer.setVisibility(View.GONE);
                            }
                        } else {
                            if (scheduleContainer != null) {
                                scheduleContainer.setVisibility(View.VISIBLE);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking files existence", e);
                        updateScheduleContainerVisibility(false);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in checkIfFilesExist: " + e.getMessage(), e);
        }
    }

    // NEW METHOD: Update schedule container visibility based on files existence
    private void updateScheduleContainerVisibility(boolean hasFiles) {
        try {
            if (scheduleContainer != null) {
                if (hasFiles) {
                    scheduleContainer.setVisibility(View.VISIBLE);
                } else {
                    scheduleContainer.setVisibility(View.VISIBLE); // Always visible to show empty state
                    currentSelectedSchedule = null;
                    displayEmptyState();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in updateScheduleContainerVisibility: " + e.getMessage(), e);
        }
    }

    private void displayEmptyState() {
        if (scheduleContainer == null) return;

        try {
            scheduleContainer.removeAllViews();

            TextView emptyMessage = new TextView(this);
            emptyMessage.setText("No Excel files uploaded yet.\n\nPlease upload an Excel file to start scanning schedules.");
            emptyMessage.setTextSize(16f);
            emptyMessage.setTextColor(ContextCompat.getColor(this, R.color.blue));
            emptyMessage.setPadding(16, 32, 16, 32);
            emptyMessage.setGravity(android.view.Gravity.CENTER);
            emptyMessage.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            emptyMessage.setLineSpacing(1.2f, 1.2f);
            emptyMessage.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners));

            scheduleContainer.addView(emptyMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying empty state: " + e.getMessage(), e);
        }
    }

    private void checkProfileCompletion() {
        try {
            firestore.collection("checkers").document(currentUserId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Boolean isProfileComplete = document.getBoolean("isProfileComplete");
                            if (isProfileComplete == null || !isProfileComplete) {
                                Intent intent = new Intent(dashboardchecker.this, checkerprof.class);
                                intent.putExtra("isFirstLogin", true);
                                startActivity(intent);
                                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                            } else {
                                loadCheckerProfileData();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking profile completion", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in checkProfileCompletion: " + e.getMessage(), e);
        }
    }

    private void loadCheckerProfileData() {
        try {
            firestore.collection("checkers").document(currentUserId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String username = document.getString("username");
                            String idNumber = document.getString("idNumber");

                            if (checkerNameTextView != null && username != null && !username.isEmpty()) {
                                checkerNameTextView.setText("Checker: " + username);
                            }
                            if (checkerIdTextView != null && idNumber != null && !idNumber.isEmpty()) {
                                checkerIdTextView.setText("ID: " + idNumber);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading checker profile", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in loadCheckerProfileData: " + e.getMessage(), e);
        }
    }

    private void initializeViews() {
        try {
            checkerNameTextView = findViewById(R.id.checkerName);
            checkerIdTextView = findViewById(R.id.checkerId);
            scheduleContainer = findViewById(R.id.scheduleContainer);
            logoutButton = findViewById(R.id.logoutButton);
            exitButton = findViewById(R.id.exitButton);
            checkerImage = findViewById(R.id.checkerImage);

            Log.d(TAG, "Views initialized - checkerName: " + (checkerNameTextView != null) +
                    ", scheduleContainer: " + (scheduleContainer != null));
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
        }
    }

    private void setupProfileImageClickListener() {
        try {
            if (checkerImage != null) {
                checkerImage.setOnClickListener(v -> {
                    Intent intent = new Intent(dashboardchecker.this, checkerprof.class);
                    intent.putExtra("isFirstLogin", false);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                });

                checkerImage.setClickable(true);
                checkerImage.setFocusable(true);
                checkerImage.setBackground(ContextCompat.getDrawable(this, R.drawable.ripple_effect));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up profile image click: " + e.getMessage(), e);
        }
    }

    private void setCheckerInfo() {
        try {
            if (checkerNameTextView != null) checkerNameTextView.setText("Checker: " + currentUserId);
            if (checkerIdTextView != null) checkerIdTextView.setText("ID: " + currentUserId);
        } catch (Exception e) {
            Log.e(TAG, "Error setting checker info: " + e.getMessage(), e);
        }
    }

    private void checkForSelectedFile() {
        try {
            currentFileId = sharedPreferences.getString("selected_file_id", null);
            Log.d(TAG, "Selected file ID: " + currentFileId);
        } catch (Exception e) {
            Log.e(TAG, "Error checking selected file: " + e.getMessage(), e);
        }
    }

    // TIME FILTERING METHODS - WITH BETTER ERROR HANDLING
    private String getCurrentTime() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date());
        } catch (Exception e) {
            Log.e(TAG, "Error getting current time: " + e.getMessage(), e);
            return "00:00"; // Default fallback
        }
    }

    private boolean isTimeInRange(String currentTime, String startTime, String endTime) {
        try {
            if (currentTime == null || startTime == null || endTime == null) {
                Log.w(TAG, "Null time values detected - current: " + currentTime +
                        ", start: " + startTime + ", end: " + endTime);
                return false;
            }

            int current = parseTime(currentTime);
            int start = parseTime(startTime);
            int end = parseTime(endTime);

            boolean inRange = current >= start && current <= end;
            Log.d(TAG, String.format("Time check - Current: %s(%d), Start: %s(%d), End: %s(%d), InRange: %b",
                    currentTime, current, startTime, start, endTime, end, inRange));

            return inRange;
        } catch (Exception e) {
            Log.e(TAG, "Error in time range check: " + e.getMessage(), e);
            return false;
        }
    }

    private int parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            Log.w(TAG, "parseTime called with null or empty string");
            return 0;
        }

        try {
            // Remove any AM/PM and trim whitespace
            String cleanTime = timeString.replace("AM", "").replace("PM", "").replace("am", "").replace("pm", "").trim();
            Log.d(TAG, "Cleaned time string: '" + cleanTime + "'");

            String[] parts = cleanTime.split(":");
            if (parts.length >= 2) {
                int hours = Integer.parseInt(parts[0].trim());
                int minutes = Integer.parseInt(parts[1].trim());

                // Handle 12-hour format
                if (timeString.toLowerCase().contains("pm") && hours < 12) {
                    hours += 12;
                } else if (timeString.toLowerCase().contains("am") && hours == 12) {
                    hours = 0;
                }

                int totalMinutes = hours * 60 + minutes;
                Log.d(TAG, "Parsed time: " + timeString + " -> " + hours + ":" + minutes + " = " + totalMinutes + " minutes");
                return totalMinutes;
            } else {
                Log.w(TAG, "Invalid time format: " + timeString);
                return 0;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Number format error parsing time: " + timeString, e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time: " + timeString, e);
            return 0;
        }
    }

    private List<Schedule> filterSchedulesByCurrentTime(List<Schedule> allSchedules) {
        String currentTime = getCurrentTime();
        List<Schedule> filteredSchedules = new ArrayList<>();

        if (allSchedules == null) {
            Log.w(TAG, "filterSchedulesByCurrentTime: allSchedules is null");
            return filteredSchedules;
        }

        for (Schedule schedule : allSchedules) {
            try {
                if (schedule != null && schedule.startTime != null && schedule.endTime != null) {
                    if (isTimeInRange(currentTime, schedule.startTime, schedule.endTime)) {
                        filteredSchedules.add(schedule);
                    }
                } else {
                    Log.w(TAG, "Invalid schedule data - schedule: " + schedule +
                            ", startTime: " + (schedule != null ? schedule.startTime : "null") +
                            ", endTime: " + (schedule != null ? schedule.endTime : "null"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error filtering schedule: " + e.getMessage(), e);
            }
        }

        Log.d(TAG, "Time filtering - Current: " + currentTime +
                ", Total: " + allSchedules.size() +
                ", Filtered: " + filteredSchedules.size());

        return filteredSchedules;
    }

    private void loadScheduleData() {
        Log.d(TAG, "Loading schedule data...");
        try {
            firestore.collection("uploaded_files")
                    .get()
                    .addOnSuccessListener(uploadedFilesSnapshot -> {
                        if (uploadedFilesSnapshot.isEmpty()) {
                            schedules.clear();
                            displayAllSchedules(schedules);
                            return;
                        }

                        if (currentFileId != null) {
                            firestore.collection("schedules")
                                    .whereEqualTo("sourceFileId", currentFileId)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        processSchedules(queryDocumentSnapshots);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading schedule data for file: " + e.getMessage(), e);
                                        Toast.makeText(this, "Error loading schedule data", Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            firestore.collection("schedules")
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        processSchedules(queryDocumentSnapshots);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading all schedule data: " + e.getMessage(), e);
                                        Toast.makeText(this, "Error loading schedule data", Toast.LENGTH_LONG).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking uploaded files: " + e.getMessage(), e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in loadScheduleData: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading schedules", Toast.LENGTH_LONG).show();
        }
    }

    private void processSchedules(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        try {
            if (queryDocumentSnapshots.isEmpty()) {
                Log.d(TAG, "No schedule data found");
                if (currentFileId != null) {
                    Toast.makeText(this, "No schedule data found for selected file.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "No schedule data found.", Toast.LENGTH_LONG).show();
                }
                schedules.clear();
                displayAllSchedules(schedules);
                return;
            }

            schedules.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                try {
                    Schedule schedule = new Schedule();
                    schedule.id = document.getId();
                    schedule.teacher = document.getString("teacher");
                    schedule.room = document.getString("room");
                    schedule.startTime = document.getString("startTime");
                    schedule.endTime = document.getString("endTime");
                    schedule.courseNumber = document.getString("courseNumber");
                    schedule.schoolYear = document.getString("schoolYear");
                    schedule.schoolTerm = document.getString("schoolTerm");
                    schedule.subjectDescription = document.getString("subjectDescription");
                    schedule.sourceFileId = document.getString("sourceFileId");
                    schedule.isPresent = false;
                    schedules.add(schedule);

                    Log.d(TAG, "Loaded schedule: " + schedule.teacher + " - " + schedule.startTime + " to " + schedule.endTime);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing schedule document: " + e.getMessage(), e);
                }
            }

            checkAttendanceStatus();

            String fileInfo = currentFileId != null ? " from selected file" : "";
            Toast.makeText(this, "Loaded " + schedules.size() + " schedules" + fileInfo, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Exception in processSchedules: " + e.getMessage(), e);
            Toast.makeText(this, "Error processing schedules", Toast.LENGTH_LONG).show();
        }
    }

    private void checkAttendanceStatus() {
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Log.d(TAG, "Checking attendance for date: " + today);

            firestore.collection("attendance")
                    .whereEqualTo("date", today)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                String scheduleId = document.getString("scheduleId");
                                for (Schedule schedule : schedules) {
                                    if (schedule.id.equals(scheduleId)) {
                                        schedule.isPresent = true;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing attendance document: " + e.getMessage(), e);
                            }
                        }
                        // Apply time filtering after checking attendance
                        List<Schedule> filteredSchedules = filterSchedulesByCurrentTime(schedules);
                        displayAllSchedules(filteredSchedules);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking attendance: " + e.getMessage(), e);
                        // Apply time filtering even if attendance check fails
                        List<Schedule> filteredSchedules = filterSchedulesByCurrentTime(schedules);
                        displayAllSchedules(filteredSchedules);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in checkAttendanceStatus: " + e.getMessage(), e);
            List<Schedule> filteredSchedules = filterSchedulesByCurrentTime(schedules);
            displayAllSchedules(filteredSchedules);
        }
    }

    private void displayAllSchedules(List<Schedule> schedules) {
        if (scheduleContainer == null) {
            Log.e(TAG, "scheduleContainer is null in displayAllSchedules");
            return;
        }

        try {
            scheduleContainer.removeAllViews();

            if (schedules == null || schedules.isEmpty()) {
                // Show empty state message
                TextView emptyMessage = new TextView(this);
                emptyMessage.setText("No ongoing classes found for current time.\n\nPlease check back during class hours or upload an Excel file with schedules.");
                emptyMessage.setTextSize(16f);
                emptyMessage.setTextColor(ContextCompat.getColor(this, R.color.blue));
                emptyMessage.setPadding(16, 32, 16, 32);
                emptyMessage.setGravity(android.view.Gravity.CENTER);
                emptyMessage.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                emptyMessage.setLineSpacing(1.2f, 1.2f);
                emptyMessage.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners));
                scheduleContainer.addView(emptyMessage);
                return;
            }

            // Show current time info
            String currentTime = getCurrentTime();
            TextView timeInfoText = new TextView(this);
            timeInfoText.setText("Current Time: " + currentTime + " - Showing ongoing classes");
            timeInfoText.setTextSize(14f);
            timeInfoText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            timeInfoText.setPadding(16, 8, 16, 16);
            timeInfoText.setGravity(android.view.Gravity.CENTER);
            timeInfoText.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            scheduleContainer.addView(timeInfoText);

            // Show file selection info if applicable
            if (currentFileId != null) {
                TextView fileInfoText = new TextView(this);
                fileInfoText.setText("Showing schedules from selected file");
                fileInfoText.setTextSize(12f);
                fileInfoText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                fileInfoText.setPadding(16, 4, 16, 8);
                fileInfoText.setGravity(android.view.Gravity.CENTER);
                fileInfoText.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
                scheduleContainer.addView(fileInfoText);
            }

            for (int i = 0; i < schedules.size(); i++) {
                try {
                    Schedule schedule = schedules.get(i);
                    displayScheduleCard(scheduleContainer, schedule, i + 1);
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying schedule card " + i + ": " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in displayAllSchedules: " + e.getMessage(), e);
            Toast.makeText(this, "Error displaying schedules", Toast.LENGTH_LONG).show();
        }
    }

    private void displayScheduleCard(LinearLayout rootLayout, Schedule schedule, int index) {
        try {
            // Create schedule card
            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 8, 0, 16);
            card.setLayoutParams(cardParams);
            card.setRadius(16f);
            card.setCardElevation(4f);

            // Set card color based on attendance status
            if (schedule.isPresent) {
                card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.skyblue));
            }

            // Make card clickable only if not present
            if (!schedule.isPresent) {
                card.setClickable(true);
                card.setFocusable(true);
                card.setForeground(ContextCompat.getDrawable(this, R.drawable.ripple_effect));
                card.setOnClickListener(v -> {
                    currentSelectedSchedule = schedule;
                    checkCameraPermissionAndOpenScanner();
                });
            } else {
                card.setClickable(false);
                card.setAlpha(0.7f);
            }

            // Create inner layout
            LinearLayout innerLayout = new LinearLayout(this);
            innerLayout.setOrientation(LinearLayout.VERTICAL);
            innerLayout.setPadding(16, 16, 16, 16);

            // ROOM AS HEADER - Very prominent
            if (schedule.room != null && !schedule.room.isEmpty()) {
                TextView roomHeader = new TextView(this);
                roomHeader.setText("🏢 ROOM " + schedule.room);
                roomHeader.setTextSize(18f);
                roomHeader.setTypeface(null, Typeface.BOLD);
                roomHeader.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                roomHeader.setPadding(0, 0, 0, 12);
                roomHeader.setGravity(android.view.Gravity.CENTER);

                // Add some styling to make it stand out
                roomHeader.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_blue));
                roomHeader.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corners));
                roomHeader.setPadding(12, 8, 12, 8);

                innerLayout.addView(roomHeader);
            }

            // Add schedule number and status below room
            TextView scheduleHeader = new TextView(this);
            String status = schedule.isPresent ? "✅ PRESENT" : "📱 CLICK TO SCAN";
            scheduleHeader.setText("Schedule #" + index + " - " + status);
            scheduleHeader.setTextSize(14f);
            scheduleHeader.setTypeface(null, Typeface.BOLD);
            scheduleHeader.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            scheduleHeader.setPadding(0, 0, 0, 8);
            innerLayout.addView(scheduleHeader);

            // Add teacher info
            if (schedule.teacher != null && !schedule.teacher.isEmpty()) {
                TextView teacherText = new TextView(this);
                teacherText.setText("👤 Teacher: " + schedule.teacher);
                teacherText.setTextSize(14f);
                teacherText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                teacherText.setPadding(0, 2, 0, 2);
                innerLayout.addView(teacherText);
            }

            // Add time info
            if (schedule.startTime != null && schedule.endTime != null) {
                TextView timeText = new TextView(this);
                timeText.setText("🕐 Time: " + schedule.startTime + " - " + schedule.endTime);
                timeText.setTextSize(14f);
                timeText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                timeText.setPadding(0, 2, 0, 2);
                innerLayout.addView(timeText);
            }

            // Add course number
            if (schedule.courseNumber != null && !schedule.courseNumber.isEmpty()) {
                TextView cnText = new TextView(this);
                cnText.setText("📚 Course: " + schedule.courseNumber);
                cnText.setTextSize(14f);
                cnText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                cnText.setPadding(0, 2, 0, 2);
                innerLayout.addView(cnText);
            }

            // Add term and school year
            LinearLayout termSyLayout = new LinearLayout(this);
            termSyLayout.setOrientation(LinearLayout.HORIZONTAL);
            termSyLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            if (schedule.schoolTerm != null && !schedule.schoolTerm.isEmpty()) {
                TextView termText = new TextView(this);
                termText.setText("Term: " + schedule.schoolTerm);
                termText.setTextSize(14f);
                termText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                termText.setPadding(0, 2, 16, 2);
                termSyLayout.addView(termText);
            }

            if (schedule.schoolYear != null && !schedule.schoolYear.isEmpty()) {
                TextView syText = new TextView(this);
                syText.setText("S.Y. " + schedule.schoolYear);
                syText.setTextSize(14f);
                syText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                syText.setPadding(0, 2, 0, 2);
                termSyLayout.addView(syText);
            }

            if (termSyLayout.getChildCount() > 0) {
                innerLayout.addView(termSyLayout);
            }

            // Add subject description
            if (schedule.subjectDescription != null && !schedule.subjectDescription.isEmpty()) {
                TextView subjectText = new TextView(this);
                subjectText.setText("📖 " + schedule.subjectDescription);
                subjectText.setTextSize(14f);
                subjectText.setTypeface(null, Typeface.BOLD);
                subjectText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                subjectText.setPadding(0, 8, 0, 0);
                innerLayout.addView(subjectText);
            }

            card.addView(innerLayout);
            rootLayout.addView(card);

        } catch (Exception e) {
            Log.e(TAG, "Error in displayScheduleCard: " + e.getMessage(), e);
        }
    }

    private void markAttendance(Schedule schedule, String scannedTeacherId) {
        try {
            if (schedule.teacher != null && schedule.teacher.contains(scannedTeacherId)) {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                Map<String, Object> attendance = new HashMap<>();
                attendance.put("scheduleId", schedule.id);
                attendance.put("teacher", schedule.teacher);
                attendance.put("subject", schedule.subjectDescription);
                attendance.put("room", schedule.room);
                attendance.put("time", schedule.startTime + " - " + schedule.endTime);
                attendance.put("date", currentDate);
                attendance.put("timestamp", currentTime);
                attendance.put("scannedBy", currentUserId);
                attendance.put("teacherId", scannedTeacherId);

                firestore.collection("attendance")
                        .add(attendance)
                        .addOnSuccessListener(documentReference -> {
                            schedule.isPresent = true;
                            List<Schedule> filteredSchedules = filterSchedulesByCurrentTime(schedules);
                            displayAllSchedules(filteredSchedules);
                            Toast.makeText(this, "Attendance marked for " + schedule.teacher, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Attendance marked for schedule: " + schedule.id);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to mark attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error marking attendance: " + e.getMessage());
                        });
            } else {
                Toast.makeText(this, "Teacher ID doesn't match schedule!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in markAttendance: " + e.getMessage(), e);
            Toast.makeText(this, "Error marking attendance", Toast.LENGTH_LONG).show();
        }
    }

    private void setupBottomButtons() {
        try {
            if (logoutButton != null) {
                logoutButton.setOnClickListener(v -> {
                    animateBounce(logoutButton);
                    showLogoutConfirmation();
                });
            }

            if (exitButton != null) {
                exitButton.setOnClickListener(v -> {
                    animateBounce(exitButton);
                    showExitConfirmation();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom buttons: " + e.getMessage(), e);
        }
    }

    private void showLogoutConfirmation() {
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> performLogout())
                    .setNegativeButton("No", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing logout confirmation: " + e.getMessage(), e);
        }
    }

    private void showExitConfirmation() {
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit the application?")
                    .setPositiveButton("Yes", (dialog, which) -> exitApp())
                    .setNegativeButton("No", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing exit confirmation: " + e.getMessage(), e);
        }
    }

    private void performLogout() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            redirectToLogin();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error performing logout: " + e.getMessage(), e);
        }
    }

    private void exitApp() {
        try {
            finishAffinity();
            System.exit(0);
        } catch (Exception e) {
            Log.e(TAG, "Error exiting app: " + e.getMessage(), e);
        }
    }

    private void checkCameraPermissionAndOpenScanner() {
        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openScanner();
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking camera permission: " + e.getMessage(), e);
        }
    }

    private void openScanner() {
        try {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan Teacher QR Code");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE, ScanOptions.CODE_128);
            barcodeLauncher.launch(options);
        } catch (Exception e) {
            Log.e(TAG, "Error opening scanner: " + e.getMessage(), e);
        }
    }

    private void animateBounce(View view) {
        try {
            Animation bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce);
            if (view != null) view.startAnimation(bounceAnimation);
        } catch (Exception e) {
            Log.e(TAG, "Error animating bounce: " + e.getMessage(), e);
        }
    }

    private void redirectToLogin() {
        try {
            startActivity(new Intent(this, login.class));
            finish();
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to login: " + e.getMessage(), e);
        }
    }

    private String extractNumbersOnly(String input) {
        try {
            return input.replaceAll("[^0-9]", "");
        } catch (Exception e) {
            Log.e(TAG, "Error extracting numbers: " + e.getMessage(), e);
            return "";
        }
    }

    // Schedule data model class
    private static class Schedule {
        String id;
        String teacher;
        String room;
        String startTime;
        String endTime;
        String courseNumber;
        String schoolYear;
        String schoolTerm;
        String subjectDescription;
        String sourceFileId;
        boolean isPresent;
    }
}