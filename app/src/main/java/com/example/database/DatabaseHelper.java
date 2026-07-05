package com.example.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.models.Note;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "codenotes.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NOTES = "notes";
    
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_LANGUAGE = "language";
    private static final String COLUMN_TAGS = "tags";
    private static final String COLUMN_IS_PINNED = "isPinned";
    private static final String COLUMN_CREATED_AT = "createdAt";
    private static final String COLUMN_UPDATED_AT = "updatedAt";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NOTES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_CONTENT + " TEXT NOT NULL, " +
                COLUMN_LANGUAGE + " TEXT, " +
                COLUMN_TAGS + " TEXT, " +
                COLUMN_IS_PINNED + " INTEGER DEFAULT 0, " +
                COLUMN_CREATED_AT + " INTEGER, " +
                COLUMN_UPDATED_AT + " INTEGER" +
                ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    public long insertNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_LANGUAGE, note.getLanguage());
        values.put(COLUMN_TAGS, note.getTags());
        values.put(COLUMN_IS_PINNED, note.getIsPinned());
        values.put(COLUMN_CREATED_AT, note.getCreatedAt());
        values.put(COLUMN_UPDATED_AT, note.getUpdatedAt());

        long id = -1;
        try {
            id = db.insertOrThrow(TABLE_NOTES, null, values);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error inserting note", e);
        } finally {
            db.close();
        }
        return id;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_CONTENT, note.getContent());
        values.put(COLUMN_LANGUAGE, note.getLanguage());
        values.put(COLUMN_TAGS, note.getTags());
        values.put(COLUMN_IS_PINNED, note.getIsPinned());
        values.put(COLUMN_CREATED_AT, note.getCreatedAt());
        values.put(COLUMN_UPDATED_AT, note.getUpdatedAt());

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_NOTES, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(note.getId())});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error updating note", e);
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public int deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(TABLE_NOTES, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting note", e);
        } finally {
            db.close();
        }
        return rowsDeleted;
    }

    public Note getNote(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Note note = null;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, null, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                note = cursorToNote(cursor);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting note", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return note;
    }

    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NOTES + " ORDER BY " +
                COLUMN_IS_PINNED + " DESC, " + COLUMN_UPDATED_AT + " DESC";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notes.add(cursorToNote(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting all notes", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notes;
    }

    public List<Note> searchNotes(String queryText) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String wildcardQuery = "%" + queryText + "%";
        String selection = COLUMN_TITLE + " LIKE ? OR " +
                COLUMN_CONTENT + " LIKE ? OR " +
                COLUMN_TAGS + " LIKE ? OR " +
                COLUMN_LANGUAGE + " LIKE ?";
        String[] selectionArgs = new String[]{wildcardQuery, wildcardQuery, wildcardQuery, wildcardQuery};
        String orderBy = COLUMN_IS_PINNED + " DESC, " + COLUMN_UPDATED_AT + " DESC";
        
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, null, selection, selectionArgs, null, null, orderBy);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notes.add(cursorToNote(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error searching notes", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notes;
    }

    public List<Note> getPinnedNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, null, COLUMN_IS_PINNED + " = ?",
                    new String[]{"1"}, null, null, COLUMN_UPDATED_AT + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notes.add(cursorToNote(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting pinned notes", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notes;
    }

    public int pinNote(long id, boolean pinned) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_PINNED, pinned ? 1 : 0);
        
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_NOTES, values, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error pinning note", e);
        } finally {
            db.close();
        }
        return rowsAffected;
    }

    public List<Note> getNotesByLanguage(String language) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String orderBy = COLUMN_IS_PINNED + " DESC, " + COLUMN_UPDATED_AT + " DESC";
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTES, null, COLUMN_LANGUAGE + " = ?",
                    new String[]{language}, null, null, orderBy);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    notes.add(cursorToNote(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting notes by language", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return notes;
    }

    public int countNotes() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTES, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error counting notes", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return count;
    }

    public int deleteAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(TABLE_NOTES, null, null);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting all notes", e);
        } finally {
            db.close();
        }
        return rowsDeleted;
    }

    private Note cursorToNote(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
        note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)));
        note.setLanguage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LANGUAGE)));
        note.setTags(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAGS)));
        note.setIsPinned(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PINNED)));
        note.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
        note.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
        return note;
    }
}
