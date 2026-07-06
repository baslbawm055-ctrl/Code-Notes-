package com.example.activities;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.R;
import com.example.database.DatabaseHelper;
import com.example.databinding.ActivitySettingsBinding;
import com.example.models.Note;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_EXPORT = 1001;
    private static final int REQUEST_CODE_IMPORT = 1002;

    private ActivitySettingsBinding binding;
    private DatabaseHelper databaseHelper;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        
        // Ensure correct language layout direction and translations on startup
        String langCode = prefs.getString("language", Locale.getDefault().getLanguage());
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Setup ultra-snappy and polished Material3 Transitions (200ms)
        MaterialSharedAxis enterTransition = new MaterialSharedAxis(MaterialSharedAxis.X, true);
        enterTransition.setDuration(200);
        getWindow().setEnterTransition(enterTransition);

        MaterialSharedAxis returnTransition = new MaterialSharedAxis(MaterialSharedAxis.X, false);
        returnTransition.setDuration(200);
        getWindow().setReturnTransition(returnTransition);

        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);

        setSupportActionBar(binding.toolbarSettings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbarSettings.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        binding.toolbarSettings.setNavigationOnClickListener(v -> finishAfterTransition());

        updateSettingsUIState();
        setupListeners();
    }

    private void updateSettingsUIState() {
        // Theme selection indicator
        boolean isDark = prefs.getBoolean("is_dark_mode", false);
        if (isDark) {
            binding.imgCheckDark.setVisibility(View.VISIBLE);
            binding.imgCheckLight.setVisibility(View.GONE);
        } else {
            binding.imgCheckDark.setVisibility(View.GONE);
            binding.imgCheckLight.setVisibility(View.VISIBLE);
        }

        // Language selection indicator
        String lang = prefs.getString("language", Locale.getDefault().getLanguage());
        if ("ar".equals(lang)) {
            binding.imgCheckArabic.setVisibility(View.VISIBLE);
            binding.imgCheckEnglish.setVisibility(View.GONE);
        } else {
            binding.imgCheckArabic.setVisibility(View.GONE);
            binding.imgCheckEnglish.setVisibility(View.VISIBLE);
        }

        // Font Size indicator
        float fontSize = prefs.getFloat("font_size", 16f);
        String sizeName;
        if (fontSize == 14f) {
            sizeName = getString("ar".equals(lang) ? R.string.settings_arabic : R.string.settings_english).equals("العربية") ? "صغير (14sp)" : "Small (14sp)";
        } else if (fontSize == 20f) {
            sizeName = getString("ar".equals(lang) ? R.string.settings_arabic : R.string.settings_english).equals("العربية") ? "كبير (20sp)" : "Large (20sp)";
        } else if (fontSize == 24f) {
            sizeName = getString("ar".equals(lang) ? R.string.settings_arabic : R.string.settings_english).equals("العربية") ? "كبير جداً (24sp)" : "Extra Large (24sp)";
        } else {
            sizeName = getString("ar".equals(lang) ? R.string.settings_arabic : R.string.settings_english).equals("العربية") ? "متوسط (16sp)" : "Medium (16sp)";
        }
        binding.txtCurrentFontSize.setText(getString(R.string.settings_font_size_sub) + " (" + sizeName + ")");
    }

    private void setupListeners() {
        // Toggle Dark Mode
        binding.layoutDarkMode.setOnClickListener(v -> {
            prefs.edit().putBoolean("is_dark_mode", true).apply();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            updateSettingsUIState();
        });

        // Toggle Light Mode
        binding.layoutLightMode.setOnClickListener(v -> {
            prefs.edit().putBoolean("is_dark_mode", false).apply();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            updateSettingsUIState();
        });

        // Language: English
        binding.layoutEnglish.setOnClickListener(v -> {
            if (!"en".equals(prefs.getString("language", ""))) {
                changeLanguage("en");
            }
        });

        // Language: Arabic
        binding.layoutArabic.setOnClickListener(v -> {
            if (!"ar".equals(prefs.getString("language", ""))) {
                changeLanguage("ar");
            }
        });

        // Font Size Dialog Picker
        binding.layoutFontSize.setOnClickListener(v -> showFontSizeDialog());

        // Quick Backup
        binding.layoutBackup.setOnClickListener(v -> performQuickBackup());

        // Quick Restore
        binding.layoutRestore.setOnClickListener(v -> performQuickRestore());

        // Custom Export Notes (SAF File Picker)
        binding.layoutExport.setOnClickListener(v -> launchExportFilePicker());

        // Custom Import Notes (SAF File Picker)
        binding.layoutImport.setOnClickListener(v -> launchImportFilePicker());

        // About dialog
        binding.layoutAbout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_about_title)
                    .setMessage(R.string.dialog_about_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });

        // Privacy Policy dialog
        binding.layoutPrivacy.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_privacy_title)
                    .setMessage(R.string.dialog_privacy_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private void changeLanguage(String langCode) {
        prefs.edit().putString("language", langCode).apply();

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Restart MainActivity to reinitialize all resource languages & layouts
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void showFontSizeDialog() {
        String[] fontSizes = {"Small (14sp)", "Medium (16sp)", "Large (20sp)", "Extra Large (24sp)"};
        float[] fontSizeValues = {14f, 16f, 20f, 24f};

        float currentSize = prefs.getFloat("font_size", 16f);
        int checkedItem = 1; // Medium default
        for (int i = 0; i < fontSizeValues.length; i++) {
            if (fontSizeValues[i] == currentSize) {
                checkedItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_font_size)
                .setSingleChoiceItems(fontSizes, checkedItem, (dialog, which) -> {
                    prefs.edit().putFloat("font_size", fontSizeValues[which]).apply();
                    updateSettingsUIState();
                    dialog.dismiss();
                    Snackbar.make(binding.getRoot(), R.string.msg_note_updated, Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performQuickBackup() {
        try {
            List<Note> notes = databaseHelper.getAllNotes();
            String json = serializeNotes(notes);
            if (json == null) {
                showSnackbar(R.string.msg_backup_failed);
                return;
            }

            File backupFile = new File(getExternalFilesDir(null), "codenotes_backup.json");
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(json.getBytes());
            fos.close();

            showSnackbar(R.string.msg_backup_success);
        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar(R.string.msg_backup_failed);
        }
    }

    private void performQuickRestore() {
        try {
            File backupFile = new File(getExternalFilesDir(null), "codenotes_backup.json");
            if (!backupFile.exists()) {
                showSnackbar(R.string.msg_no_backup_found);
                return;
            }

            FileInputStream fis = new FileInputStream(backupFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            fis.close();

            List<Note> restoredNotes = deserializeNotes(sb.toString());
            if (restoredNotes == null || restoredNotes.isEmpty()) {
                showSnackbar(R.string.msg_restore_failed);
                return;
            }

            // Restore/merge: delete and re-insert or insert unique.
            // For simple and safe action, let's delete all and re-insert to avoid duplicate list
            databaseHelper.deleteAllNotes();
            for (Note note : restoredNotes) {
                databaseHelper.insertNote(note);
            }

            showSnackbar(R.string.msg_restore_success);
        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar(R.string.msg_restore_failed);
        }
    }

    private void launchExportFilePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "codenotes_export.json");
        startActivityForResult(intent, REQUEST_CODE_EXPORT);
    }

    private void launchImportFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_CODE_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQUEST_CODE_EXPORT) {
            try {
                List<Note> notes = databaseHelper.getAllNotes();
                String json = serializeNotes(notes);
                if (json != null) {
                    writeTextToUri(uri, json);
                    showSnackbar(R.string.msg_export_success);
                } else {
                    showSnackbar(R.string.msg_backup_failed);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showSnackbar(R.string.msg_backup_failed);
            }
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            try {
                String jsonContent = readTextFromUri(uri);
                List<Note> importedNotes = deserializeNotes(jsonContent);
                if (importedNotes != null && !importedNotes.isEmpty()) {
                    databaseHelper.deleteAllNotes();
                    for (Note note : importedNotes) {
                        databaseHelper.insertNote(note);
                    }
                    showSnackbar(R.string.msg_import_success);
                } else {
                    showSnackbar(R.string.msg_import_failed);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showSnackbar(R.string.msg_import_failed);
            }
        }
    }

    private void writeTextToUri(Uri uri, String text) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            if (pfd != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                fileOutputStream.write(text.getBytes());
                fileOutputStream.close();
                pfd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readTextFromUri(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                reader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private String serializeNotes(List<Note> notes) {
        try {
            JSONArray array = new JSONArray();
            for (Note note : notes) {
                JSONObject obj = new JSONObject();
                obj.put("title", note.getTitle());
                obj.put("content", note.getContent());
                obj.put("language", note.getLanguage());
                obj.put("tags", note.getTags());
                obj.put("isPinned", note.getIsPinned());
                obj.put("createdAt", note.getCreatedAt());
                obj.put("updatedAt", note.getUpdatedAt());
                array.put(obj);
            }
            return array.toString(4);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Note> deserializeNotes(String jsonString) {
        List<Note> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Note note = new Note();
                note.setTitle(obj.optString("title", ""));
                note.setContent(obj.optString("content", ""));
                note.setLanguage(obj.optString("language", ""));
                note.setTags(obj.optString("tags", ""));
                note.setIsPinned(obj.optInt("isPinned", 0));
                note.setCreatedAt(obj.optLong("createdAt", System.currentTimeMillis()));
                note.setUpdatedAt(obj.optLong("updatedAt", System.currentTimeMillis()));
                list.add(note);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    private void showSnackbar(int messageResId) {
        Snackbar.make(binding.getRoot(), messageResId, Snackbar.LENGTH_LONG).show();
    }
}
