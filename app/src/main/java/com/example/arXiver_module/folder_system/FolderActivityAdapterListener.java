package com.example.arXiver_module.folder_system;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;

import com.example.arXiver_module.InsideFolderActivity;
import com.example.arXiver_module.ParentActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class FolderActivityAdapterListener {

    final Activity activity;
    final FloatingActionButton folderButton;

    public FolderActivityAdapterListener(Activity activity, FloatingActionButton folderButton){
        this.activity = activity;
        this.folderButton = folderButton;
    }

    public void openFolder(Folder folder){
        if(!folder.folderName.isEmpty()) {
            Intent openFolderIntent = new Intent(activity, InsideFolderActivity.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            openFolderIntent.putExtra("folderName", folder.folderName);
            activity.startActivity(openFolderIntent);
            // no finish, we want to go back to folders when pressed back
        }
    }

    public void updateOrder(ArrayList<Folder> folders){
        SharedPreferences sharedPreferences= activity.getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int order = 0;
        for(Folder folder:folders){
            editor.putInt(folder.folderName,order);
            order++;
        }
        editor.apply();
    }

    public void onMove(boolean b){
        if(b){
            folderButton.setVisibility(View.GONE);
        }else{
            folderButton.setVisibility(View.VISIBLE);
        }
    }
}