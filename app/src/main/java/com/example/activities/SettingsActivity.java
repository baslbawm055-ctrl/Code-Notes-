package com.example.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.R;
import com.example.databinding.ActivitySettingsBinding;
import com.google.android.material.transition.platform.MaterialSharedAxis;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        String langCode = prefs.getString("language", java.util.Locale.getDefault().getLanguage());
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        getWindow().setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        getWindow().setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));

        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarSettings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbarSettings.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        }
        binding.toolbarSettings.setNavigationOnClickListener(v -> finishAfterTransition());

        setupListeners();
    }

    private void setupListeners() {
        binding.layoutDarkMode.setOnClickListener(v -> {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_dark_mode", true).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        });

        binding.layoutLightMode.setOnClickListener(v -> {
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_dark_mode", false).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.layoutEnglish.setOnClickListener(v -> {
            changeLanguage("en");
        });

        binding.layoutArabic.setOnClickListener(v -> {
            changeLanguage("ar");
        });
    }

    private void changeLanguage(String langCode) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putString("language", langCode).apply();

        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
