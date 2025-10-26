package com.example.teachertrackfinalnagidnaugotnako;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class files extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 100;
    private TextView selectedFileName;
    private Uri selectedFileUri;
    private FirebaseFirestore firestore;
    private LinearLayout uploadedFilesContainer;
    private List<UploadedFile> uploadedFiles = new ArrayList<>();
    private String currentSelectedFileId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_files);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firestore = FirebaseFirestore.getInstance();
        selectedFileName = findViewById(R.id.selectedFileName);
        uploadedFilesContainer = findViewById(R.id.uploadedFilesContainer);

        findViewById(R.id.uploadButton).setVisibility(View.GONE);

        // Load previously uploaded files
        loadUploadedFiles();

        findViewById(R.id.pickButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            });
            startActivityForResult(intent, PICK_FILE_REQUEST);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        });

        findViewById(R.id.uploadButton).setOnClickListener(v -> {
            if (selectedFileUri != null) {
                // Check for duplicate before uploading
                checkForDuplicateAndUpload();
            } else {
                Toast.makeText(this, "Please select a file first!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.backButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        });
    }

    private void loadUploadedFiles() {
        firestore.collection("uploaded_files")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    uploadedFiles.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UploadedFile file = new UploadedFile();
                        file.id = document.getId();
                        file.fileName = document.getString("fileName");
                        file.uploadDate = document.getString("uploadDate");
                        file.scheduleCount = document.getLong("scheduleCount");
                        uploadedFiles.add(file);
                    }
                    displayUploadedFiles();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading uploaded files", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayUploadedFiles() {
        if (uploadedFilesContainer == null) return;

        uploadedFilesContainer.removeAllViews();

        if (uploadedFiles.isEmpty()) {
            TextView noFilesText = new TextView(this);
            noFilesText.setText("No files uploaded yet");
            noFilesText.setTextSize(16f);
            noFilesText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            noFilesText.setPadding(16, 32, 16, 16);
            noFilesText.setGravity(android.view.Gravity.CENTER);
            uploadedFilesContainer.addView(noFilesText);
            return;
        }

        TextView filesTitle = new TextView(this);
        filesTitle.setText("Uploaded Files:");
        filesTitle.setTextSize(18f);
        filesTitle.setTextColor(ContextCompat.getColor(this, R.color.blue));
        filesTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        filesTitle.setPadding(16, 20, 16, 16);
        uploadedFilesContainer.addView(filesTitle);

        for (UploadedFile file : uploadedFiles) {
            CardView fileCard = createFileCard(file);
            uploadedFilesContainer.addView(fileCard);
        }
    }

    private CardView createFileCard(UploadedFile file) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 16);
        card.setLayoutParams(cardParams);
        card.setRadius(16f); // This is fine for CardView
        card.setCardElevation(4f);

        // Highlight if selected
        if (file.id.equals(currentSelectedFileId)) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.lightblue));
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        }

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(16, 16, 16, 16);

        // File name
        TextView fileNameText = new TextView(this);
        fileNameText.setText(file.fileName);
        fileNameText.setTextSize(16f);
        fileNameText.setTypeface(null, android.graphics.Typeface.BOLD);
        fileNameText.setTextColor(ContextCompat.getColor(this, R.color.blue));
        fileNameText.setPadding(0, 0, 0, 8);
        innerLayout.addView(fileNameText);

        // Upload date and schedule count
        TextView fileInfoText = new TextView(this);
        fileInfoText.setText("Uploaded: " + file.uploadDate + " | Schedules: " + file.scheduleCount);
        fileInfoText.setTextSize(12f);
        fileInfoText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        fileInfoText.setPadding(0, 0, 0, 12);
        innerLayout.addView(fileInfoText);

        // Buttons layout
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Select Button
        MaterialButton selectButton = new MaterialButton(this);
        selectButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));
        selectButton.setText("Select");
        selectButton.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
        selectButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        selectButton.setCornerRadius(8); // Fixed: changed from 8f to 8
        selectButton.setOnClickListener(v -> {
            currentSelectedFileId = file.id;
            saveSelectedFile(file.id); // Save to SharedPreferences
            displayUploadedFiles(); // Refresh to show selection
            Toast.makeText(this, "Selected: " + file.fileName, Toast.LENGTH_SHORT).show();
        });

        // Delete Button
        MaterialButton deleteButton = new MaterialButton(this);
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));
        deleteButton.setText("Delete");
        deleteButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        deleteButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        deleteButton.setCornerRadius(8); // Fixed: changed from 8f to 8
        deleteButton.setOnClickListener(v -> {
            showDeleteConfirmation(file);
        });

        // Add margin between buttons
        LinearLayout.LayoutParams buttonParams = (LinearLayout.LayoutParams) selectButton.getLayoutParams();
        buttonParams.setMargins(0, 0, 8, 0);
        selectButton.setLayoutParams(buttonParams);

        buttonsLayout.addView(selectButton);
        buttonsLayout.addView(deleteButton);
        innerLayout.addView(buttonsLayout);

        card.addView(innerLayout);
        return card;
    }

    private void saveSelectedFile(String fileId) {
        SharedPreferences sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selected_file_id", fileId);
        editor.apply();
        Toast.makeText(this, "File selected successfully", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmation(UploadedFile file) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete '" + file.fileName + "'? This will also remove all associated schedules.")
                .setPositiveButton("Delete", (dialog, which) -> deleteFile(file))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFile(UploadedFile file) {
        // First delete all schedules associated with this file
        firestore.collection("schedules")
                .whereEqualTo("sourceFileId", file.id)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete();
                    }

                    // Then delete the file record
                    firestore.collection("uploaded_files").document(file.id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "File deleted successfully", Toast.LENGTH_SHORT).show();

                                // Clear selection if deleted file was selected
                                if (file.id.equals(currentSelectedFileId)) {
                                    currentSelectedFileId = null;
                                    clearSelectedFile();
                                }

                                loadUploadedFiles(); // Refresh the list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting file", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting schedules", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearSelectedFile() {
        SharedPreferences sharedPreferences = getSharedPreferences("TeacherTrackPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("selected_file_id");
        editor.apply();
    }

    private void checkForDuplicateAndUpload() {
        if (selectedFileUri == null) return;

        String fileName = getFileNameFromUri(selectedFileUri);

        // Check if file with same name already exists
        firestore.collection("uploaded_files")
                .whereEqualTo("fileName", fileName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Duplicate found
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Duplicate File")
                                .setMessage("A file with the name '" + fileName + "' already exists. Do you want to overwrite it?")
                                .setPositiveButton("Overwrite", (dialog, which) -> {
                                    // Delete existing file and upload new one
                                    String existingFileId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                    deleteFileAndUpload(fileName, existingFileId);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        // No duplicate, proceed with upload
                        readExcelFile(selectedFileUri, fileName);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking for duplicates", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFileAndUpload(String fileName, String existingFileId) {
        // Delete existing file and its schedules
        UploadedFile tempFile = new UploadedFile();
        tempFile.id = existingFileId;
        tempFile.fileName = fileName;
        deleteFile(tempFile);

        // Then upload new file
        readExcelFile(selectedFileUri, fileName);
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }
        return fileName != null ? fileName : "unknown_file";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                String fileName = getFileNameFromUri(selectedFileUri);
                selectedFileName.setText("Selected File: " + fileName);
                selectedFileName.setVisibility(View.VISIBLE);
                findViewById(R.id.uploadButton).setVisibility(View.VISIBLE);
            }
        }
    }

    private void readExcelFile(Uri uri, String fileName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                Workbook workbook = WorkbookFactory.create(inputStream);

                if (workbook.getNumberOfSheets() == 0) {
                    Toast.makeText(this, "Excel file has no sheets", Toast.LENGTH_LONG).show();
                    workbook.close();
                    inputStream.close();
                    return;
                }

                Sheet sheet = workbook.getSheetAt(0);

                if (sheet.getPhysicalNumberOfRows() <= 1) {
                    Toast.makeText(this, "No data found in the sheet", Toast.LENGTH_LONG).show();
                    workbook.close();
                    inputStream.close();
                    return;
                }

                // First, create uploaded file record
                createUploadedFileRecord(fileName, sheet, uri);

                workbook.close();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading Excel file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createUploadedFileRecord(String fileName, Sheet sheet, Uri uri) {
        String uploadDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> fileRecord = new HashMap<>();
        fileRecord.put("fileName", fileName);
        fileRecord.put("uploadDate", uploadDate);
        fileRecord.put("scheduleCount", 0); // Will be updated after processing

        firestore.collection("uploaded_files")
                .add(fileRecord)
                .addOnSuccessListener(documentReference -> {
                    String fileId = documentReference.getId();
                    int processedRows = processSheet(sheet, fileId);

                    // Update the schedule count
                    documentReference.update("scheduleCount", processedRows);

                    Toast.makeText(this, "Successfully uploaded " + processedRows + " schedules", Toast.LENGTH_LONG).show();

                    // Auto-select the newly uploaded file
                    currentSelectedFileId = fileId;
                    saveSelectedFile(fileId);

                    // Refresh the files list
                    loadUploadedFiles();

                    // Navigate back to dashboard
                    Intent intent = new Intent(files.this, dashboardchecker.class);
                    intent.putExtra("data_imported", true);
                    intent.putExtra("imported_rows", processedRows);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving file record", Toast.LENGTH_LONG).show();
                });
    }

    private int processSheet(Sheet sheet, String fileId) {
        int processedCount = 0;
        DataFormatter formatter = new DataFormatter();

        // Get header row to understand column structure
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            Log.d("ExcelImport", "Header row detected with " + headerRow.getPhysicalNumberOfCells() + " columns");
            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                String header = formatter.formatCellValue(headerRow.getCell(i));
                Log.d("ExcelImport", "Column " + i + ": " + header);
            }
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            // Read data by columns - adjust indexes based on your Excel structure
            String teacher = getCellValue(row, 0);
            String room = getCellValue(row, 1);
            String startTime = formatTime(getCellValue(row, 2));
            String endTime = formatTime(getCellValue(row, 3));
            String courseNumber = getCellValue(row, 4);
            String schoolYear = getCellValue(row, 5);
            String schoolTerm = getCellValue(row, 6);
            String description = getCellValue(row, 7);

            // Enhanced validation
            if (teacher.isEmpty() || room.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                Log.w("ExcelImport", "Skipping row " + (i+1) + " due to missing required fields");
                Log.w("ExcelImport", "Teacher: '" + teacher + "', Room: '" + room +
                        "', Start: '" + startTime + "', End: '" + endTime + "'");
                continue;
            }

            if (startTime.equals(endTime)) {
                Log.w("ExcelImport", "Skipping row " + (i+1) + " - start and end times are the same");
                continue;
            }

            saveRoomAndTerm(room, schoolTerm);
            saveSchedule(teacher, startTime, endTime, courseNumber, schoolYear, description, room, schoolTerm, fileId);
            processedCount++;

            Log.d("ExcelImport", "Processed row " + (i+1) + ": " + teacher + " - " + description);
        }
        return processedCount;
    }

    private void saveRoomAndTerm(String room, String schoolTerm) {
        // Save room if it doesn't exist
        if (!room.isEmpty()) {
            firestore.collection("rooms")
                    .whereEqualTo("room", room)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            Map<String, Object> roomData = new HashMap<>();
                            roomData.put("room", room);
                            firestore.collection("rooms").add(roomData);
                            Log.d("Firestore", "Saved new room: " + room);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error checking room: " + e.getMessage());
                    });
        }

        // Save term if it doesn't exist
        if (!schoolTerm.isEmpty()) {
            firestore.collection("terms")
                    .whereEqualTo("schoolTerm", schoolTerm)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            Map<String, Object> termData = new HashMap<>();
                            termData.put("schoolTerm", schoolTerm);
                            firestore.collection("terms").add(termData);
                            Log.d("Firestore", "Saved new term: " + schoolTerm);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error checking term: " + e.getMessage());
                    });
        }
    }

    private void saveSchedule(String teacher, String startTime, String endTime, String courseNumber,
                              String schoolYear, String description, String room, String schoolTerm, String fileId) {
        Map<String, Object> schedule = new HashMap<>();
        schedule.put("teacher", teacher);
        schedule.put("startTime", startTime);
        schedule.put("endTime", endTime);
        schedule.put("courseNumber", courseNumber);
        schedule.put("schoolYear", schoolYear);
        schedule.put("subjectDescription", description);
        schedule.put("room", room);
        schedule.put("schoolTerm", schoolTerm);
        schedule.put("sourceFileId", fileId); // Link to uploaded file

        firestore.collection("schedules")
                .add(schedule)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Schedule saved with ID: " + documentReference.getId() +
                            " for teacher: " + teacher + " from file: " + fileId);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error saving schedule for " + teacher + ": " + e.getMessage());
                });
    }

    private String getCellValue(Row row, int index) {
        try {
            DataFormatter formatter = new DataFormatter();
            return formatter.formatCellValue(row.getCell(index)).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String formatTime(String time) {
        if (time.isEmpty()) return "";

        // If already has AM/PM, return as is
        if (time.toLowerCase().contains("am") || time.toLowerCase().contains("pm")) {
            return time;
        }

        try {
            String[] parts = time.split(":|\\.");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

            String period = (hour < 12) ? "AM" : "PM";

            // Convert to 12-hour format
            if (hour == 0) {
                hour = 12; // 12 AM
            } else if (hour > 12) {
                hour = hour - 12;
            }

            return String.format("%d:%02d %s", hour, minute, period);
        } catch (Exception e) {
            Log.w("TimeFormat", "Failed to format time: " + time);
            return time; // Return original if formatting fails
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    // Data model for uploaded files
    private static class UploadedFile {
        String id;
        String fileName;
        String uploadDate;
        Long scheduleCount;
    }
}