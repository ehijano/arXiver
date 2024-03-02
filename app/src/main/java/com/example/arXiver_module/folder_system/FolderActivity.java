package com.example.arXiver_module.folder_system;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arXiver_module.ArXivPaper;
import com.example.arXiver_module.ParentActivity;
import com.example.arXiver_module.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class FolderActivity extends ParentActivity implements FolderManagerDialog.FolderManagerListener{

    private Animation rotateOpen;
    private Animation rotateClose;
    private FloatingActionButton folderButton;
    private boolean folderClicked = false;
    private FolderRecyclerViewAdapter folderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folders);

        // Add folders stuff
        rotateOpen = AnimationUtils.loadAnimation(this,R.anim.totate_open_anim);
        rotateClose = AnimationUtils.loadAnimation(this,R.anim.totate_close_anim);
        folderButton = findViewById(R.id.folderButton);
        View.OnClickListener feedListener = view -> onFolderButtonClicked();
        folderButton.setOnClickListener(feedListener);

        RecyclerView folderRecyclerView = findViewById(R.id.folderRecyclerView);
        folderRecyclerView.setHasFixedSize(true);
        folderRecyclerView.setLayoutManager(
                new GridLayoutManager(this,3)
        );
        ArrayList<Folder> folders = getAllFolders();

        FolderActivityAdapterListener listener = new FolderActivityAdapterListener(this, folderButton);
        folderAdapter = new FolderRecyclerViewAdapter(this, folders, listener);

        folderRecyclerView.setAdapter(folderAdapter);

        FolderSimpleCallBack folderSimpleCallBack = new FolderSimpleCallBack(folderAdapter, listener);

        ItemTouchHelper folderItemTouchHelper = new ItemTouchHelper(folderSimpleCallBack);
        folderItemTouchHelper.attachToRecyclerView(folderRecyclerView);

        // tip
        showTip(TIP_FOLDER);
    }

    public void onFolderButtonClicked(){
        folderClicked = !folderClicked;
        setAnimation();
        showFeedManager();
    }

    private void showFeedManager(){
        if(folderClicked) {
            showFolderManager();
        }
    }

    public void showFolderManager(){
        FolderManagerDialog folderManagerDialog = new FolderManagerDialog(null);
        folderManagerDialog.setCancelable(true);
        folderManagerDialog.show(getSupportFragmentManager(),"FOLDER DIALOG");
    }

    private void setAnimation(){
        if (folderClicked){
            folderButton.startAnimation(rotateOpen);
        }else{
            folderButton.startAnimation(rotateClose);
        }
    }


    public ArrayList<Folder> getAllFolders(){
        SharedPreferences sharedPreferences= getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        Map<String,?> allFolderEntries = sharedPreferences.getAll();

        ArrayList<Map.Entry<String, ?>> entries = new ArrayList<>(allFolderEntries.entrySet());

        Comparator<Map.Entry<String,?>> entryComparator = (e1, e2) -> {
            Integer order1 = (int) e1.getValue();
            Integer order2 = (int) e2.getValue();
            return order1.compareTo(order2);
        };
        Collections.sort(entries,entryComparator);

        ArrayList<Folder> result = new ArrayList<>();
        for(Map.Entry<String,?> entry: entries){
            Folder folder = new Folder(entry.getKey());
            result.add(folder);
        }

        return result;
    }


    @Override
    public void addFolder(Folder folder) {
        SharedPreferences sharedPreferences= getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int order = sharedPreferences.getAll().size();
        editor.putInt(folder.folderName,order);
        editor.apply();

        folderAdapter.updateFolders(getAllFolders());
    }

    @Override
    public void addPapersToFolder(Folder folder, ArrayList<ArXivPaper> papers) {

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

        folderAdapter.updateFolders(getAllFolders());
    }

    @Override
    public ArrayList<String> getAllFolderNames(){
        SharedPreferences sharedPreferences= getSharedPreferences(ParentActivity.FOLDER_PREFS, Context.MODE_PRIVATE);
        Map<String,?> allFolderEntries = sharedPreferences.getAll();
        return sortMap(allFolderEntries);
    }
}
