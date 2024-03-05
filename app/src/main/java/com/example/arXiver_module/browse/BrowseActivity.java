package com.example.arXiver_module.browse;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.arXiver_module.ParentActivity;
import com.example.arXiver_module.R;
import com.example.arXiver_module.arxiv.ArXivPaper;
import com.example.arXiver_module.arxiv.ArXivScanner;
import com.example.arXiver_module.folder_system.Folder;
import com.example.arXiver_module.folder_system.FolderManagerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class BrowseActivity extends ParentActivity implements FolderManagerDialog.FolderManagerListener {

    @Override
    public void addPapersToFolder(Folder folder, ArrayList<ArXivPaper> papers){
        Date dateObj = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateSaved = simpleDateFormat.format(dateObj);
        ArXivScanner arXivScanner = new ArXivScanner();

        if(folder.folderName.equals(getResources().getString(R.string.saved_papers))){
            SharedPreferences sharedPreferences = getSharedPreferences(ParentActivity.SAVED_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            for(ArXivPaper paper: papers) {
                paper.setDateSaved(dateSaved);
                editor.putString(paper.id, arXivScanner.compress(paper));
                editor.apply();
            }
        }else {
            SharedPreferences sharedPreferences = getSharedPreferences(folder.folderPrefs, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();


            for(ArXivPaper paper:papers) {
                paper.setDateSaved(dateSaved);
                editor.putString(paper.id, arXivScanner.compress(paper));
                editor.apply();
            }
        }
    }

    @Override
    public void addFolder(Folder folder){
        SharedPreferences sharedPreferences= getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int order = sharedPreferences.getAll().size();
        editor.putInt(folder.folderName,order);
        editor.apply();
    }


    @Override
    public void deleteFolder(Folder folder){
        SharedPreferences sharedPreferences= getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int order = (int) sharedPreferences.getAll().get(folder.folderName);

        // Any folder with higher order must come down in order by 1.
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for(Map.Entry<String,?> entry: allEntries.entrySet()){
            if ((int)entry.getValue() > order){
                editor.putInt(entry.getKey(),(int)entry.getValue()-1);
            }
        }
        editor.remove(folder.folderName);
        editor.apply();
    }

    @Override
    public ArrayList<String> getAllFolderNames(){
        SharedPreferences sharedPreferences= getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        Map<String,?> allFolderEntries = sharedPreferences.getAll();
        return sortMap(allFolderEntries);
    }

    public void showFolderManager(ArrayList<ArXivPaper> paperList){
        FolderManagerDialog folderManagerDialog = new FolderManagerDialog(paperList);
        folderManagerDialog.setCancelable(true);
        folderManagerDialog.show(getSupportFragmentManager(),"FOLDER DIALOG");
    }
}
