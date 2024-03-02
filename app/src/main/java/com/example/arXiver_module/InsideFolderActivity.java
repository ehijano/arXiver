package com.example.arXiver_module;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.arXiver_module.folder_system.Folder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class InsideFolderActivity extends BrowseActivity {

    SavedRecyclerViewAdapter browseFolderRecyclerViewAdapter;
    RecyclerView recyclerView;
    ActivityAdapterListener listener;
    FloatingActionButton shareFloatingActionButton;
    FloatingActionButton deleteFloatingActionButton;
    FloatingActionButton selectAllFloatingActionButton;
    FloatingActionButton folderFloatingActionButton;

    Folder folder;

    SwipeRefreshLayout browseSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve folder name
        folder = new Folder( getIntent().getStringExtra("folderName") );
        if(folder.folderName.equals(getResources().getString(R.string.saved_papers)))
        {
            folder.setFolderPrefs(SAVED_PREFS);
        }

        setContentView(R.layout.activity_browse);

        this.setTitle(getResources().getString(R.string.app_name)+" - "+ folder.folderName);

        // FABs
        shareFloatingActionButton = findViewById(R.id.shareFloatingActionButton);
        folderFloatingActionButton = findViewById(R.id.folderFloatingActionButton);
        deleteFloatingActionButton = findViewById(R.id.deleteFloatingActionButton);
        selectAllFloatingActionButton = findViewById(R.id.selectAllFloatingActionButton);
        listener = new ActivityAdapterListener(this,
                new FloatingActionButton[] {shareFloatingActionButton,deleteFloatingActionButton, selectAllFloatingActionButton, folderFloatingActionButton}
                , false);

        // Refresh layout
        browseSwipeRefreshLayout = findViewById(R.id.browseSwipeRefreshLayout);
        browseSwipeRefreshLayout.setRefreshing(false);
        browseSwipeRefreshLayout.setOnRefreshListener( () -> browseSwipeRefreshLayout.setRefreshing(false) );

        // Creating the RecyclerView
        recyclerView = findViewById(R.id.browseRecyclerView);

        // Pass to the adapter
        browseFolderRecyclerViewAdapter = new SavedRecyclerViewAdapter(this, listener, folder.folderPrefs);

        recyclerView.setAdapter(browseFolderRecyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // On-clicks of the floating action buttons
        View.OnClickListener shareOnClickListener = view -> {
            String shareText = listener.generateMessage();
            if(!shareText.isEmpty()){
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        };
        shareFloatingActionButton.setOnClickListener(shareOnClickListener);

        View.OnClickListener deleteOnClickListener = view -> browseFolderRecyclerViewAdapter.deleteItems(listener.selectedPapers,view);
        deleteFloatingActionButton.setOnClickListener(deleteOnClickListener);

        View.OnClickListener folderOnClickListener = view -> {
            this.showFolderManager(listener.selectedPapers);
            // do not reset selection.
        };
        folderFloatingActionButton.setOnClickListener(folderOnClickListener);

        View.OnClickListener selectAllOnClickListener = view -> browseFolderRecyclerViewAdapter.selectAllItems();
        selectAllFloatingActionButton.setOnClickListener(selectAllOnClickListener);

        // Tip
        if(folder.folderName.equals(getResources().getString(R.string.saved_papers))){
            showTip(TIP_SAVED);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (listener.isSelectMode){
            browseFolderRecyclerViewAdapter.resetSelection();
        }else{
            super.onBackPressed();
        }
    }
}
