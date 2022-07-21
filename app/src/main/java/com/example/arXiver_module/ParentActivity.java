package com.example.arXiver_module;

import static com.example.arXiver_module.R.id;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class ParentActivity extends AppCompatActivity {

    public static final String FEED_PREFS = "feedPrefs";
    public static final String FONT_PREFS = "fontPrefs";
    private static final String TAX_PREFS = "taxPrefs";
    public static final String DARK_PREFS = "themePrefs";
    public static final String DOWNLOAD_PREFS = "downloadPrefs";
    public static final String TIP_PREFS = "tipPrefs";
    public static final String FOLDER_PREFS = "folderPrefs";
    public static final String SAVED_PREFS = "savedPrefs";

    private final int INTERNET_PERMISSION_CODE = 1;
    private final int WRITE_PERMISSION_CODE = 3;

    public static final int TIP_MAIN = 1;
    public static final int TIP_SAVED = 2;
    public static final int TIP_DAY = 3;
    public static final int TIP_FOLDER = 4;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.top_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == id.arxivItem){
            Intent mainIntent = new Intent(getApplicationContext(), HomeActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(mainIntent);
            parentFinish();
            return true;
        }else if(item.getItemId() == id.savedItem){
            Intent openFolderIntent = new Intent(getApplicationContext(), InsideFolderActivity.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            openFolderIntent.putExtra("folderName", getResources().getString(R.string.saved_papers));
            startActivity(openFolderIntent);
            parentFinish();
            return true;
        }else if(item.getItemId() == id.foldersItem){
            Intent folderIntent = new Intent(getApplicationContext(),FolderActivity.class);
            folderIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(folderIntent);
            parentFinish();
            return true;
        }else if (item.getItemId() == id.searchItem){
            Intent searchIntent = new Intent(getApplicationContext(),SearchActivity.class);
            startActivity(searchIntent);
            parentFinish();
            return true;
        }else if(item.getItemId() ==  id.settingsItem){
            Intent settingsIntent = new Intent(getApplicationContext(),SettingsActivity.class);
            startActivity(settingsIntent);
            parentFinish();
            return true;
        }else if(item.getItemId() ==  id.helpItem){
            Intent helpIntent = new Intent(getApplicationContext(),HelpActivity.class);
            helpIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(helpIntent);
            parentFinish();
            return true;
        }else if(item.getItemId() ==  id.exitItem){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
            parentFinish();
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ActionBar
        ActionBar actionBar;
        actionBar = getSupportActionBar();
        ColorDrawable colorDrawable = new ColorDrawable(getResources().getColor(R.color.light_red));
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.mipmap.ic_launcher);
        }

        // Font size
        SharedPreferences fontPreferences = getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE);
        float scale = fontPreferences.getFloat("FONT_SIZE", 1.0f);
        adjustFontScale( getResources().getConfiguration(), scale);

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

        if(ContextCompat.checkSelfPermission(ParentActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestInternetPermission();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == INTERNET_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getResources().getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
            }
        }
        else if(requestCode == WRITE_PERMISSION_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getResources().getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void requestInternetPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.INTERNET)){
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.permission_needed))
                    .setMessage(getResources().getString(R.string.why_permission))
                    .setPositiveButton("ok", (dialogInterface, i) -> ActivityCompat.requestPermissions(ParentActivity.this,new String[]{Manifest.permission.INTERNET}, INTERNET_PERMISSION_CODE))
                    .setNegativeButton("cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                    .create().show();
        }else{
            Log.d("permTag","request:");
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.INTERNET}, INTERNET_PERMISSION_CODE);
        }


    }

    public void requestWritePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.permission_needed))
                    .setMessage(getResources().getString(R.string.why_permission_write))
                    .setPositiveButton("ok", (dialogInterface, i) -> ActivityCompat.requestPermissions(ParentActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_CODE))
                    .setNegativeButton("cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                    .create().show();
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_CODE);
        }
    }

    public void showTip(int TIP){
        SharedPreferences sharedPreferences = getSharedPreferences(TIP_PREFS, Context.MODE_PRIVATE);

        if(!sharedPreferences.contains(String.valueOf(TIP))){
            // never show tip again
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(String.valueOf(TIP),true);
            editor.apply();

            TipDialog tipDialog;
            switch(TIP) {
                case TIP_MAIN:
                    tipDialog = new TipDialog(getResources().getString(R.string.tip_main_1)
                        , ResourcesCompat.getDrawable(getResources(), R.drawable.tip1, getTheme()), 200,
                        getResources().getString(R.string.tip_main_2));
                    break;
                case TIP_SAVED:
                    tipDialog = new TipDialog(getResources().getString(R.string.tip_saved_1)
                            , ResourcesCompat.getDrawable(getResources(), R.drawable.tip_save1, getTheme()), 400,
                            getResources().getString(R.string.tip_saved_2));
                    break;
                case TIP_DAY:
                    tipDialog = new TipDialog(getResources().getString(R.string.tip_day_1)
                            , ResourcesCompat.getDrawable(getResources(), R.drawable.tip_feed_1, getTheme()), 400,
                            getResources().getString(R.string.tip_day_2));
                    break;
                case TIP_FOLDER:
                    tipDialog = new TipDialog(getResources().getString(R.string.tip_folder_1)
                            , ResourcesCompat.getDrawable(getResources(), R.drawable.tipfolder, getTheme()), 400,
                            getResources().getString(R.string.tip_folder_2));
                      break;
                default:
                    throw new IllegalStateException("Unexpected value: " + TIP);
            }
            tipDialog.show(getSupportFragmentManager(), String.valueOf(TIP));
        }
    }

    public void updateDownload(String id, Long l){
        SharedPreferences downloadPreferences = getSharedPreferences(DOWNLOAD_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = downloadPreferences.edit();
        if (l!=null) {
            editor.putLong(id, l);
        }else{
            editor.remove(id);
        }
        editor.apply();
    }

    public int fetchTextColorPrimary() {
        TypedValue typedValue = new TypedValue();
        TypedArray arr = obtainStyledAttributes(typedValue.data, new int[] { android.R.attr.textColorPrimary });
        int color = arr.getColor(0, 0);
        arr.recycle();
        return color;
    }

    public void parentFinish(){
        finish();
    }

    public String[] getAllCategoriesMemory(){
        SharedPreferences sharedPreferences = getSharedPreferences(TAX_PREFS, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        String[] allCategories = new String[allEntries.size()];
        int i = 0;
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            allCategories[i] = entry.getKey();
            i ++;
        }

        return allCategories;
    }

    public  void adjustFontScale(Configuration configuration, float scale) {
        configuration.fontScale = scale;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);

        // Save preferences
        SharedPreferences sharedPreferences = getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("FONT_SIZE",scale);
        editor.apply();
    }

    public ArrayList<String> sortMap(Map<String,?> map){
        ArrayList<Map.Entry<String, ?>> entries = new ArrayList<>(map.entrySet());

        Comparator<Map.Entry<String,?>> entryComparator = (e1, e2) -> {
                Integer order1 = (int) e1.getValue();
                Integer order2 = (int) e2.getValue();
                return order1.compareTo(order2);
        };
        Collections.sort(entries,entryComparator);

        ArrayList<String> result = new ArrayList<>();
        for(Map.Entry<String,?> entry: entries){
            result.add(entry.getKey());
        }

        return result;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        recreate();
    }

}
