package com.example.arXiver_module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.snackbar.Snackbar;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static final int UPDATE_REQUEST_CODE = 666;
    public static final String DARK_PREFS = "themePrefs";
    private AppUpdateManager appUpdateManager;
    private ProgressBar downloadProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        Objects.requireNonNull(getSupportActionBar()).hide();

        // progress bar
        downloadProgressBar = findViewById(R.id.downloadProgressBar);

        // Dark mode
        SharedPreferences darkPreferences = getSharedPreferences(DARK_PREFS, Context.MODE_PRIVATE);
        // If a preference exists, respect it. Otherwise obey phone
        if(darkPreferences.contains("DarkMode")){// We have selected a preference and it should be respected
            if (darkPreferences.getBoolean("DarkMode",false)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }

        // update?
        checkForUpdates();


    }

    public void startApp(){
        Handler handler = new Handler();
        Runnable runnable = () -> {
            Intent mainIntent = new Intent(getApplicationContext(), HomeActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            finish();
        };
        handler.postDelayed(runnable, 1000);
    }


    public void checkForUpdates(){
        appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if( (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ) {
                requestUpdate(appUpdateInfo);
            }else{
                startApp();
            }
        });

        appUpdateManager.registerListener(installStateUpdatedListener);
    }

    private final InstallStateUpdatedListener installStateUpdatedListener = new InstallStateUpdatedListener() {
        @Override
        public void onStateUpdate(@NonNull InstallState state) {
            if(state.installStatus() == InstallStatus.DOWNLOADED){
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content)
                        , getResources().getString(R.string.update_downloaded),
                        Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction(getResources().getString(R.string.install),
                        view -> appUpdateManager.completeUpdate());
                snackbar.show();
            }if(state.installStatus()==InstallStatus.DOWNLOADING){
                long totalBytes = state.totalBytesToDownload();
                long currentBytes = state.bytesDownloaded();
                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressBar.setProgress((int) (100* currentBytes/totalBytes));
            }
        }

    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == UPDATE_REQUEST_CODE && resultCode != Activity.RESULT_OK){
            Toast.makeText(this,getResources().getString(R.string.update_cancelled),Toast.LENGTH_SHORT).show();
            startApp();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        if(appUpdateManager != null) appUpdateManager.unregisterListener(installStateUpdatedListener);
        super.onStop();
    }

    private void requestUpdate(AppUpdateInfo appUpdateInfo){
        try {
            appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                    AppUpdateType.FLEXIBLE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    UPDATE_REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            startApp();
        }
    }


}