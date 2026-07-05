package com.example.activities;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.example.R;
import com.example.database.DatabaseHelper;
import com.example.databinding.ActivityEditorBinding;
import com.example.models.Note;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.platform.MaterialContainerTransform;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import com.amrdeveloper.codeview.CodeView;
import com.example.utils.SyntaxManager;
import java.util.Date;

public class EditorActivity extends AppCompatActivity {

    private ActivityEditorBinding binding;
    private DatabaseHelper databaseHelper;
    private Note currentNote;
    private long noteId = -1;
    private boolean isViewMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        String langCode = prefs.getString("language", java.util.Locale.getDefault().getLanguage());
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Material Container Transform for shared element
        findViewById(android.R.id.content).setTransitionName("shared_element_container");
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.addTarget(android.R.id.content);
        transform.setDuration(300);
        getWindow().setSharedElementEnterTransition(transform);
        getWindow().setSharedElementReturnTransition(transform);

        getWindow().setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        getWindow().setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        super.onCreate(savedInstanceState);
        binding = ActivityEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("NOTE_ID")) {
            noteId = getIntent().getLongExtra("NOTE_ID", -1);
            if (noteId != -1) {
                currentNote = databaseHelper.getNote(noteId);
            }
        }

        setupToolbar();
        setupLanguageDropdown();
        setupCharacterCounter();
        setupButtons();
        
        SyntaxManager.applySyntax(this, binding.inputNoteContent);
        
        if (currentNote != null) {
            isViewMode = true;
            populateFields();
        } else {
            isViewMode = false;
            binding.inputNoteTitle.requestFocus();
        }
        updateUIForMode();
    }

    private void populateFields() {
        binding.toolbarEditor.setTitle(currentNote.getTitle());
        binding.inputNoteTitle.setText(currentNote.getTitle());
        binding.inputNoteContent.setText(currentNote.getContent());
        binding.inputLanguage.setText(currentNote.getLanguage(), false);
        binding.inputTags.setText(currentNote.getTags());
        
        CharSequence dateString = DateFormat.format("MMM dd, yyyy HH:mm", new Date(currentNote.getUpdatedAt()));
        binding.textLastEdited.setText(getString(R.string.last_edited_placeholder).replace("Just now", dateString));
    }

    private void updateUIForMode() {
        if (isViewMode) {
            binding.viewNoteTitle.setVisibility(View.VISIBLE);
            binding.viewChipsLayout.setVisibility(View.VISIBLE);
            
            binding.layoutNoteTitle.setVisibility(View.GONE);
            binding.layoutLanguage.setVisibility(View.GONE);
            binding.layoutTags.setVisibility(View.GONE);
            binding.layoutNoteContent.setHintEnabled(false);
            binding.layoutNoteContent.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_NONE);
            
            binding.btnCancel.setVisibility(View.GONE);
            binding.btnSaveNote.setVisibility(View.GONE);
            
            binding.inputNoteContent.setFocusable(false);
            binding.inputNoteContent.setFocusableInTouchMode(false);
            binding.inputNoteContent.setCursorVisible(false);
            
            if (currentNote != null) {
                binding.viewNoteTitle.setText(currentNote.getTitle());
                binding.viewLanguageChip.setText(currentNote.getLanguage());
                binding.viewTagsChip.setText(currentNote.getTags());
                
                if (currentNote.getTags() == null || currentNote.getTags().isEmpty()) {
                    binding.viewTagsChip.setVisibility(View.GONE);
                } else {
                    binding.viewTagsChip.setVisibility(View.VISIBLE);
                }
                
                if (currentNote.getLanguage() == null || currentNote.getLanguage().isEmpty()) {
                    binding.viewLanguageChip.setVisibility(View.GONE);
                } else {
                    binding.viewLanguageChip.setVisibility(View.VISIBLE);
                }
            }
        } else {
            binding.toolbarEditor.setTitle(currentNote != null ? getString(R.string.edit_note) : getString(R.string.new_note));
            binding.viewNoteTitle.setVisibility(View.GONE);
            binding.viewChipsLayout.setVisibility(View.GONE);
            
            binding.layoutNoteTitle.setVisibility(View.VISIBLE);
            binding.layoutLanguage.setVisibility(View.VISIBLE);
            binding.layoutTags.setVisibility(View.VISIBLE);
            binding.layoutNoteContent.setHintEnabled(true);
            binding.layoutNoteContent.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
            
            binding.btnCancel.setVisibility(View.VISIBLE);
            binding.btnSaveNote.setVisibility(View.VISIBLE);
            
            binding.inputNoteContent.setFocusable(true);
            binding.inputNoteContent.setFocusableInTouchMode(true);
            binding.inputNoteContent.setCursorVisible(true);
        }
        invalidateOptionsMenu();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarEditor);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbarEditor.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        binding.toolbarEditor.setNavigationOnClickListener(v -> closeEditor());
    }

    private void setupLanguageDropdown() {
        String[] languages = getResources().getStringArray(R.array.programming_languages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        binding.inputLanguage.setAdapter(adapter);
    }

    private void setupCharacterCounter() {
        binding.inputNoteContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s != null ? s.length() : 0;
                binding.textCharacterCount.setText(length + " " + getString(R.string.character_count_placeholder).replaceAll("\\d+", "").trim());
            }
        });
        
        // Initial count
        int initialLength = binding.inputNoteContent.getText() != null ? binding.inputNoteContent.getText().length() : 0;
        binding.textCharacterCount.setText(initialLength + " " + getString(R.string.character_count_placeholder).replaceAll("\\d+", "").trim());
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> closeEditor());
        binding.btnSaveNote.setOnClickListener(v -> saveNote());
        binding.btnCopyCode.setOnClickListener(v -> copyCodeToClipboard());
    }

    private void copyCodeToClipboard() {
        String code = binding.inputNoteContent.getText() != null ? binding.inputNoteContent.getText().toString() : "";
        if (!code.isEmpty()) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Code snippet", code);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(binding.getRoot(), R.string.msg_code_copied, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void closeEditor() {
        boolean isKeyboardVisible = false;
        androidx.core.view.WindowInsetsCompat insets = androidx.core.view.ViewCompat.getRootWindowInsets(binding.getRoot());
        if (insets != null && insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())) {
            isKeyboardVisible = true;
        }

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        if (isKeyboardVisible) {
            binding.getRoot().postDelayed(this::finishAfterTransition, 150);
        } else {
            finishAfterTransition();
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void saveNote() {
        String title = binding.inputNoteTitle.getText() != null ? binding.inputNoteTitle.getText().toString().trim() : "";
        String content = binding.inputNoteContent.getText() != null ? binding.inputNoteContent.getText().toString().trim() : "";
        String language = binding.inputLanguage.getText() != null ? binding.inputLanguage.getText().toString().trim() : "";
        String tags = binding.inputTags.getText() != null ? binding.inputTags.getText().toString().trim() : "";
        
        if (title.isEmpty()) {
            binding.layoutNoteTitle.setError(getString(R.string.error_title_empty));
            return;
        } else {
            binding.layoutNoteTitle.setError(null);
        }

        long currentTime = System.currentTimeMillis();
        
        if (currentNote != null) {
            currentNote.setTitle(title);
            currentNote.setContent(content);
            currentNote.setLanguage(language);
            currentNote.setTags(tags);
            currentNote.setUpdatedAt(currentTime);
            
            int rows = databaseHelper.updateNote(currentNote);
            if (rows > 0) {
                closeEditor();
            } else {
                Snackbar.make(binding.getRoot(), R.string.msg_error_updating_note, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Note note = new Note(0, title, content, language, tags, 0, currentTime, currentTime);
            long id = databaseHelper.insertNote(note);
            if (id != -1) {
                closeEditor();
            } else {
                Snackbar.make(binding.getRoot(), R.string.msg_error_saving_note, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemSave = menu.findItem(R.id.action_save);
        MenuItem itemEdit = menu.findItem(R.id.action_edit);
        
        if (itemSave != null && itemEdit != null) {
            itemSave.setVisible(!isViewMode);
            itemEdit.setVisible(isViewMode);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveNote();
            return true;
        } else if (item.getItemId() == R.id.action_edit) {
            isViewMode = false;
            updateUIForMode();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            closeEditor();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
