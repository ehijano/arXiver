package com.example.arXiver_module.settings;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.appcompat.widget.SwitchCompat;

import com.example.arXiver_module.ParentActivity;
import com.example.arXiver_module.R;


public class SettingsActivity extends ParentActivity {

    public static final String SAVED_PREFS = "savedPrefs";
    public static final String DELETED_PREFS = "deletedPrefs";
    SeekBar fontSeekBar;
    SwitchCompat darkModeSwitch;
    float scale = 1.0F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);



        // Buttons
        Button clearButton = findViewById(R.id.clearButton);

        View.OnClickListener testListener = view -> {
            // Reset locally stored papers and deleted database
            SharedPreferences sharedPreferences = getSharedPreferences(SAVED_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            sharedPreferences = getSharedPreferences(DELETED_PREFS, MODE_PRIVATE);
            editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            // it is done
            clearButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.light_green)));
        };
        clearButton.setOnClickListener(testListener);

        Button clearDeletedButton = findViewById(R.id.clearDayButton);

        View.OnClickListener clearDeletedListener = view -> {
            SharedPreferences sharedPreferences = getSharedPreferences(DELETED_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            // it is done
            clearDeletedButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.light_green)));
        };
        clearDeletedButton.setOnClickListener(clearDeletedListener);

        fontSeekBar = findViewById(R.id.fontSeekBar);

        SharedPreferences sharedPreferences = getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE);
        float preferredScale = sharedPreferences.getFloat("FONT_SIZE", 1.0f);
        int progress = (int) (100*preferredScale - 50) ;
        fontSeekBar.setProgress(progress);

        fontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                scale = (float)  ((i+50) / 100.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                adjustFontScale( getResources().getConfiguration(), scale);

                finish();
                startActivity(getIntent());
            }
        });

        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        SharedPreferences darkPreferences = getSharedPreferences(DARK_PREFS, Context.MODE_PRIVATE);
        if(darkPreferences.contains("DarkMode")){// We have selected a preference and it should be respected
            darkModeSwitch.setChecked(darkPreferences.getBoolean("DarkMode",false));
        } else { // show switch activated if the app is on night theme automatically
            darkModeSwitch.setChecked(isNightMode());
        }

        darkModeSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            SharedPreferences.Editor editor = darkPreferences.edit();
            editor.putBoolean("DarkMode",isChecked);
            editor.apply();
            // Recreate activity. ParentActivity will update the theme.
            finish();
            startActivity(getIntent());
        });


    }

    private boolean isNightMode(){
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
            case Configuration.UI_MODE_NIGHT_NO:
            default:
                return false;
        }
    }

}
