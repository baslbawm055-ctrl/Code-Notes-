package com.example.interfaces;

import android.view.View;
import com.example.models.Note;

public interface OnNoteClickListener {
    void onNoteClick(Note note, View sharedView);
    void onNoteLongClick(Note note);
}
