package com.example.activities;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.adapters.NotesAdapter;
import com.example.database.DatabaseHelper;
import com.example.databinding.ActivityMainBinding;
import com.example.interfaces.OnNoteClickListener;
import com.example.models.Note;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.platform.MaterialSharedAxis;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnNoteClickListener {

    private ActivityMainBinding binding;
    private boolean isDarkMode = false;
    private DatabaseHelper databaseHelper;
    private NotesAdapter notesAdapter;
    private ObjectAnimator fabPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        
        String langCode = prefs.getString("language", java.util.Locale.getDefault().getLanguage());
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        isDarkMode = prefs.getBoolean("is_dark_mode", (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        getWindow().setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        getWindow().setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        
        databaseHelper = new DatabaseHelper(this);
        setupRecyclerView();
        setupSearch();

        binding.fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditorActivity.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        });
        
        setupFabPulseAnimation();
    }

    private void setupFabPulseAnimation() {
        fabPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                binding.fabAddNote,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f, 1.0f)
        );
        fabPulseAnimator.setDuration(1500);
        fabPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
    }

    private void setupSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    loadNotes();
                } else {
                    searchNotes(s.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.searchInput.getText() != null && !binding.searchInput.getText().toString().trim().isEmpty()) {
            searchNotes(binding.searchInput.getText().toString().trim());
        } else {
            loadNotes();
        }
    }

    private void setupRecyclerView() {
        notesAdapter = new NotesAdapter();
        notesAdapter.setListener(this);
        binding.recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewNotes.setAdapter(notesAdapter);
        
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                Note noteToDelete = notesAdapter.getNoteAt(position);
                deleteNoteWithUndo(noteToDelete, position);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewNotes);
    }

    private void loadNotes() {
        List<Note> notes = databaseHelper.getAllNotes();
        notesAdapter.setNotes(notes);
        checkNotesEmptyState(notes.isEmpty());
    }
    
    private void searchNotes(String query) {
        List<Note> notes = databaseHelper.searchNotes(query);
        notesAdapter.setNotes(notes);
        checkNotesEmptyState(notes.isEmpty());
    }

    private void checkNotesEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.recyclerViewNotes.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            if (!fabPulseAnimator.isRunning()) {
                fabPulseAnimator.start();
            }
        } else {
            binding.recyclerViewNotes.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
            if (fabPulseAnimator.isRunning()) {
                fabPulseAnimator.cancel();
                binding.fabAddNote.setScaleX(1.0f);
                binding.fabAddNote.setScaleY(1.0f);
            }
        }
    }

    @Override
    public void onNoteClick(Note note, View sharedView) {
        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("NOTE_ID", note.getId());
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                this, sharedView, "shared_element_container");
        startActivity(intent, options.toBundle());
    }

    @Override
    public void onNoteLongClick(Note note) {
        showBottomSheet(note);
    }
    
    private void showBottomSheet(Note note) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_note_options, null);
        
        TextView bsNoteTitle = bottomSheetView.findViewById(R.id.bsNoteTitle);
        bsNoteTitle.setText(note.getTitle());
        
        TextView textPin = bottomSheetView.findViewById(R.id.bsTextPin);
        ImageView iconPin = bottomSheetView.findViewById(R.id.bsIconPin);
        
        if (note.getIsPinned() == 1) {
            textPin.setText(R.string.action_unpin);
            iconPin.setImageResource(R.drawable.ic_unpin);
        } else {
            textPin.setText(R.string.action_pin);
            iconPin.setImageResource(R.drawable.ic_pin);
        }

        bottomSheetView.findViewById(R.id.bsActionEdit).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            onNoteClick(note, binding.recyclerViewNotes); // Fallback shared view
        });

        bottomSheetView.findViewById(R.id.bsActionPin).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            boolean isNowPinned = note.getIsPinned() == 0;
            databaseHelper.pinNote(note.getId(), isNowPinned);
            loadNotes();
        });
        
        bottomSheetView.findViewById(R.id.bsActionShare).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, note.getTitle() + "\n\n" + note.getContent());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.action_share)));
        });

        bottomSheetView.findViewById(R.id.bsActionDelete).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showDeleteConfirmation(note);
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }
    
    private void showDeleteConfirmation(Note note) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                deleteNoteWithUndo(note, -1);
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }
    
    private void deleteNoteWithUndo(Note note, int position) {
        databaseHelper.deleteNote(note.getId());
        loadNotes();
        
        Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.msg_note_deleted, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.action_undo, v -> {
            databaseHelper.insertNote(note);
            loadNotes();
        });
        snackbar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_dark_mode) {
            toggleDarkMode();
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply();
        
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
