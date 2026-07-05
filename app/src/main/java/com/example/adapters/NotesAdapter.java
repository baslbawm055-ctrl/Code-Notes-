package com.example.adapters;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.databinding.ItemNoteBinding;
import com.example.interfaces.OnNoteClickListener;
import com.example.models.Note;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes = new ArrayList<>();
    private OnNoteClickListener listener;
    private int lastPosition = -1;

    public void setListener(OnNoteClickListener listener) {
        this.listener = listener;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }
    
    public Note getNoteAt(int position) {
        return notes.get(position);
    }
    
    public void removeNoteAt(int position) {
        notes.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(notes.get(position), listener);
        setAnimation(holder.itemView, position);
    }
    
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = android.view.animation.AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoteBinding binding;

        public NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Note note, OnNoteClickListener listener) {
            binding.textTitle.setText(note.getTitle());
            binding.textLanguage.setText(note.getLanguage());
            
            if (note.getIsPinned() == 1) {
                binding.iconPinned.setVisibility(View.VISIBLE);
            } else {
                binding.iconPinned.setVisibility(View.GONE);
            }
            
            CharSequence dateString = DateFormat.format("MMM dd, yyyy", new Date(note.getUpdatedAt()));
            binding.textDate.setText(dateString);
            
            binding.cardNote.setTransitionName("note_transition_" + note.getId());
            
            if (listener != null) {
                binding.getRoot().setOnClickListener(v -> listener.onNoteClick(note, binding.cardNote));
                binding.getRoot().setOnLongClickListener(v -> {
                    listener.onNoteLongClick(note);
                    return true;
                });
            }
        }
    }
}
