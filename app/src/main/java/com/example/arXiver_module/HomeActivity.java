package com.example.arXiver_module;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;

import com.example.arXiver_module.daily_feed.DayActivity;
import com.example.arXiver_module.folder_system.Folder;
import com.example.arXiver_module.folder_system.FolderActivity;
import com.example.arXiver_module.search.SearchActivity;
import com.example.arXiver_module.settings.SettingsActivity;
import com.example.arXiver_module.task_util.BaseTask;
import com.example.arXiver_module.task_util.TaskRunner;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeActivity extends ParentActivity implements FeedManagerDialog.FeedManagerListener {

    CountDownTimer timer;
    private long timeInMillis;
    private ConstraintLayout constraintLayout;
    private Animation rotateOpen;
    private Animation rotateClose;
    private FloatingActionButton feedButton;
    private boolean feedClicked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Have we downloaded taxonomy?
        String TAX_PREFS = "taxPrefs";
        int knownCategories = getSharedPreferences(TAX_PREFS, Context.MODE_PRIVATE)
                .getAll()
                .size();
        if (knownCategories==0){
            new TaskRunner().executeAsync(new HomeActivity.downloadTaxonomyTask(this));
        }

        // Set up saved folder data
        Folder auxFolder = new Folder("");
        auxFolder.setSavedFolderData(getResources().getString(R.string.saved_papers),SAVED_PREFS);

        // scroll view
        constraintLayout = findViewById(R.id.mainConstraintLayout);

        // Add to feed stuff
        rotateOpen = AnimationUtils.loadAnimation(this,R.anim.totate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this,R.anim.totate_close_anim);
        feedButton = findViewById(R.id.feedButton);
        View.OnClickListener feedListener = view -> onFeedButtonClicked();
        feedButton.setOnClickListener(feedListener);

        TextView timerTextView = findViewById(R.id.timerTextField);
        timerTextView.setGravity(Gravity.CENTER);

        // Timer - updated every second (1000 millis)
        Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTimeInMillis = currentDate.getTimeInMillis();
        long publishingTimeInMillis = getPublishingTime().getTimeInMillis();
        timeInMillis = publishingTimeInMillis-currentTimeInMillis;
        timer = new CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long l) {
                timeInMillis = l;
                updateTimer(timerTextView);
            }

            @Override
            public void onFinish() {
                timer.cancel();
                timerReady(timerTextView);
            }
        }.start();

        // Saved Button
        Button savedButton = findViewById(R.id.savedButton);
        View.OnClickListener savedListener = view -> {
            Intent openFolderIntent = new Intent(getApplicationContext(), InsideFolderActivity.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            openFolderIntent.putExtra("folderName", getResources().getString(R.string.saved_papers));
            startActivity(openFolderIntent);
        };
        savedButton.setOnClickListener(savedListener);

        // Search
        Button searchButton = findViewById(R.id.searchButtonMain);
        View.OnClickListener searchListener = view -> {
            Intent searchIntent = new Intent(getApplicationContext(), SearchActivity.class);
            startActivity(searchIntent);
        };
        searchButton.setOnClickListener(searchListener);

        // Folders Button
        Button foldersButton = findViewById(R.id.foldersButton);
        View.OnClickListener foldersListener = view -> {
            Intent folderIntent = new Intent(getApplicationContext(), FolderActivity.class);
            folderIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(folderIntent);
        };
        foldersButton.setOnClickListener(foldersListener);

        // Settings Button
        Button settingsButton = findViewById(R.id.settingsButton);
        View.OnClickListener settingsListener = view -> {
            Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(settingsIntent);
        };
        settingsButton.setOnClickListener(settingsListener);

        // feed
        updateFeed();

        // initialize folder system
        initializeFolders();

        // tip
        showTip(TIP_MAIN);
    }

    private void initializeFolders(){
        SharedPreferences sharedPreferences= getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        if (sharedPreferences.getAll().size()<1) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getResources().getString(R.string.saved_papers), 0);
            editor.apply();
        }
    }

    private void onFeedButtonClicked(){
        feedClicked = !feedClicked;
        setAnimation();
        showFeedManager();
    }

    private void showFeedManager(){
        if(feedClicked) {
            FeedManagerDialog feedManagerDialog = new FeedManagerDialog();
            feedManagerDialog.setCancelable(false);
            feedManagerDialog.show(getSupportFragmentManager(),"FEED DIALOG");
        }
    }

    private void setAnimation(){
        if (feedClicked){
            feedButton.startAnimation(rotateOpen);
        }else{
            feedButton.startAnimation(rotateClose);
        }
    }

    private void updateTimer(TextView timerTextView){
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis);
        // why Locale.US?
        String timeHourMinuteSecond = String.format(Locale.US,"%02d:%02d:%02d", hours,
                minutes - TimeUnit.HOURS.toMinutes(hours),
                seconds - TimeUnit.MINUTES.toSeconds(minutes));

        timerTextView.setText(String.format("%s: %s", getString(R.string.next_arxiv_update), timeHourMinuteSecond));
    }

    private void timerReady(TextView timerTextView){
        timerTextView.setText(getResources().getString(R.string.arxiv_ready));
    }

    private Calendar getPublishingTime(){
        Calendar publishingTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        int dow = publishingTime.get (Calendar.DAY_OF_WEEK);

        publishingTime.set(Calendar.HOUR_OF_DAY, 0);
        publishingTime.set(Calendar.MINUTE, 0);
        publishingTime.set(Calendar.SECOND, 0);
        publishingTime.set(Calendar.MILLISECOND, 0);

        if ((dow >= Calendar.SUNDAY) && (dow < Calendar.FRIDAY)){
            publishingTime.add(Calendar.HOUR_OF_DAY,24);
        }else if (dow == Calendar.SATURDAY){
            publishingTime.add(Calendar.HOUR_OF_DAY,24*2);
        }else{// Friday
            publishingTime.add(Calendar.HOUR_OF_DAY,24*3);
        }

        return publishingTime;
    }

    public void updateFeed(){
        SharedPreferences sharedPreferences = getSharedPreferences(FEED_PREFS, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        TextView addTipTextView = findViewById(R.id.addTipTextView);
        if (allEntries.size()==0){
            addTipTextView.setVisibility(View.VISIBLE);
        }else {
            addTipTextView.setVisibility(View.GONE);
        }

        TableLayout feedTable = findViewById(R.id.feedTableLayout);
        // clear table
        feedTable.removeAllViews();
        // create one row per feed
        ArrayList<String> allEntriesOrdered = sortMap(allEntries);
        for (String category : allEntriesOrdered) {

            TableRow tr = new TableRow(this);
            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT,1f));
            tr.setGravity(Gravity.CENTER);
            tr.setPadding(0,0,0,5);

            Button b = new Button(this);
            b.setTextColor(fetchTextColorPrimary());
            b.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.border_button_plain,getTheme()));
            b.setText(category);
            b.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT,1f));
            View.OnClickListener rssFeedListener = view -> {
                Intent dayIntent = new Intent(getApplicationContext(), DayActivity.class);
                dayIntent.putExtra("category",category);
                startActivity(dayIntent);
            };
            b.setOnClickListener(rssFeedListener);

            ImageButton del = new ImageButton(this);
            del.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.light_red)));
            del.setBackgroundResource(android.R.drawable.ic_menu_delete);
            TableRow.LayoutParams params = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
            );
            del.setLayoutParams(params);

            View.OnClickListener deleteFeedListener = view -> deleteFromFeed(category);
            del.setOnClickListener(deleteFeedListener);

            tr.addView(del);
            tr.addView(b);

            feedTable.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));
        }

    }

    public void updateTaxPrefs(Map<String, String> categoryDictionary){
        String TAX_PREFS = "taxPrefs";

        SharedPreferences sharedPreferences = getSharedPreferences(TAX_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String category : categoryDictionary.keySet()) {
            editor.putString(category, categoryDictionary.get(category));
        }
        editor.apply();
    }

    private void deleteFromFeed(String category){
        SharedPreferences sharedPreferences = getSharedPreferences(FEED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int order = (int) sharedPreferences.getAll().get(category);

        // Any category with higher order must come down in order by 1.
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for(Map.Entry<String,?> entry: allEntries.entrySet()){
            if ((int)entry.getValue() > order){
                editor.putInt(entry.getKey(),(int)entry.getValue()-1);
            }
        }
        // The deleted category must go
        editor.remove(category);

        editor.apply();

        View.OnClickListener undoListener = view -> addCategory(category);

        Snackbar.make(constraintLayout, getResources().getString(R.string.feed_deleted), Snackbar.LENGTH_LONG)
                .setAction(getResources().getString(R.string.snack_bar_undo), undoListener)
                .show();

        updateFeed();
    }

    @Override
    public String[] getAllCategories(){
        return super.getAllCategoriesMemory();
    }


    private static class downloadTaxonomyTask extends BaseTask<Map<String, String>> {

        final HomeActivity context;

        public downloadTaxonomyTask(HomeActivity context) {
            this.context = context;
        }

        @Override
        public Map<String, String> call() {
            String targetURL = "https://arxiv.org/category_taxonomy";

            org.jsoup.nodes.Document doc = null;
            try {
                doc = Jsoup.connect(targetURL).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements categoryInfo = Objects.requireNonNull(doc).getElementsByTag("h4");
            Map<String, String> categoryDictionary = new HashMap<>();
            for(int i=1; i<categoryInfo.size(); i++) {
                org.jsoup.nodes.Element item = categoryInfo.get(i);
                String content = item.text();
                Pattern pattern = Pattern.compile("(.*?) \\((.*?)\\)") ;
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String category = matcher.group(1);
                    String description = matcher.group(2);
                    categoryDictionary.put(category, description);
                }
            }

            return categoryDictionary;
        }

        @Override
        public void setDataAfterLoading(Map<String, String> categoryDictionary) {
            context.updateTaxPrefs(categoryDictionary);
        }
    }

    @Override
    public void dismiss() {
        feedClicked = !feedClicked;
        setAnimation();
    }

    @Override
    public void addCategory(String category) {
        SharedPreferences sharedPreferences = getSharedPreferences(FEED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int order = sharedPreferences.getAll().size();
        editor.putInt(category,order);
        //editor.putString(category,category);
        editor.apply();
        updateFeed();
    }

    @Override
    public void parentFinish(){
        // do not exit activity
    }

}