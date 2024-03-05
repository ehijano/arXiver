package com.example.arXiver_module.daily_feed;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.arXiver_module.ActivityAdapterListener;
import com.example.arXiver_module.browse.BrowseActivity;
import com.example.arXiver_module.browse.BrowseSwipeToDeleteCallBack;
import com.example.arXiver_module.R;
import com.example.arXiver_module.arxiv.ArXivPaper;
import com.example.arXiver_module.arxiv.ArXivScanner;
import com.example.arXiver_module.task_util.BaseTask;
import com.example.arXiver_module.task_util.TaskRunner;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class DayActivity extends BrowseActivity {
    DayRecyclerViewAdapter dayRecyclerViewAdapter;

    RecyclerView recyclerView;
    String category;
    SwipeRefreshLayout daySwipeRefreshLayout;

    ActivityAdapterListener listener;
    FloatingActionButton shareFloatingActionButton;
    FloatingActionButton deleteFloatingActionButton;
    FloatingActionButton selectAllFloatingActionButton;
    FloatingActionButton folderFloatingActionButton;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        // Obtaining category
        category = getIntent().getStringExtra("category");

        this.setTitle(getResources().getString(R.string.app_name)+" - "+category);

        // listener buttons
        shareFloatingActionButton = findViewById(R.id.shareFloatingActionButton);
        folderFloatingActionButton = findViewById(R.id.folderFloatingActionButton);
        deleteFloatingActionButton = findViewById(R.id.deleteFloatingActionButton);
        selectAllFloatingActionButton = findViewById(R.id.selectAllFloatingActionButton);
        listener = new ActivityAdapterListener(this,
                new FloatingActionButton[]{shareFloatingActionButton,deleteFloatingActionButton, selectAllFloatingActionButton, folderFloatingActionButton},
                false);

        // SwipeRefreshLayout to refresh page
        daySwipeRefreshLayout = findViewById(R.id.browseSwipeRefreshLayout);
        daySwipeRefreshLayout.setOnRefreshListener(() -> new TaskRunner().executeAsync(new downloadFeedTask(this, category)));
        daySwipeRefreshLayout.setRefreshing(true);

        // Creating the RecyclerViews
        recyclerView = findViewById(R.id.browseRecyclerView);

        // Pass to the adapter
        try {
            dayRecyclerViewAdapter = new DayRecyclerViewAdapter(this,listener, category);
            new TaskRunner().executeAsync(new downloadFeedTask(this, category));

        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        recyclerView.setAdapter(dayRecyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Decorations
        //DayItemDecoration decoration = new DayItemDecoration(getResources().getInteger(R.integer.item_margin));
        //recyclerView.addItemDecoration(decoration);

        // Item touch helper
        ItemTouchHelper dayItemTouchHelper = new ItemTouchHelper(new BrowseSwipeToDeleteCallBack(
                dayRecyclerViewAdapter
        , ResourcesCompat.getDrawable(getResources(),R.drawable.ic_baseline_delete_24, null)
        , ResourcesCompat.getDrawable(getResources(),R.drawable.ic_baseline_archive_24, null))
        );
        dayItemTouchHelper.attachToRecyclerView(recyclerView);

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

        View.OnClickListener folderOnClickListener = view -> {
            this.showFolderManager(listener.selectedPapers);
            // do not reset selection.
        };
        folderFloatingActionButton.setOnClickListener(folderOnClickListener);

        View.OnClickListener deleteOnClickListener = view -> dayRecyclerViewAdapter.deleteItems(listener.selectedPapers,view);
        deleteFloatingActionButton.setOnClickListener(deleteOnClickListener);

        View.OnClickListener selectAllOnClickListener = view -> dayRecyclerViewAdapter.selectAllItems();
        selectAllFloatingActionButton.setOnClickListener(selectAllOnClickListener);

        // tip
        showTip(TIP_DAY);
    }

    public void updatePapers(ArrayList<ArXivPaper> papers){
        dayRecyclerViewAdapter.refresh(papers);
        daySwipeRefreshLayout.setRefreshing(false);
        recyclerView.smoothScrollToPosition(0);
    }

    private static class downloadFeedTask extends BaseTask<ArrayList<ArXivPaper>> {

        final String category;
        final DayActivity context;

        public downloadFeedTask(DayActivity context,String category) {
            this.category = category;
            this.context = context;
        }

        @Override
        public ArrayList<ArXivPaper> call() {
            return ArXivScanner.extractPapersRSS(category);
        }


        @Override
        public void setDataAfterLoading(ArrayList<ArXivPaper> result) {
            if(result!=null) {
                context.updatePapers(result);
                if (result.size()==0){
                    Toast.makeText(context,context.getResources().getString(R.string.no_arxiv),Toast.LENGTH_SHORT).show();
                }
            }else{
                context.updatePapers(new ArrayList<>());
                Toast.makeText(context,context.getResources().getString(R.string.no_internet),Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        if (listener.isSelectMode){
            dayRecyclerViewAdapter.resetSelection();
        }else{
            super.onBackPressed();
        }
    }

}