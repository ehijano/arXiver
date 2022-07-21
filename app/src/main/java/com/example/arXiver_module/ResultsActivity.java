package com.example.arXiver_module;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.arXiver_module.task_util.BaseTask;
import com.example.arXiver_module.task_util.TaskRunner;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class ResultsActivity extends BrowseActivity {
    ResultsRecyclerViewAdapter resultsRecyclerViewAdapter;
    RecyclerView resultsRecyclerView;
    Button nextButton;
    Button backButton;
    SwipeRefreshLayout resultsSwipeRefreshLayout;
    NestedScrollView resultsNestedScrollView;

    ActivityAdapterListener listener;
    FloatingActionButton shareFloatingActionButton;
    FloatingActionButton selectAllFloatingActionButton;
    FloatingActionButton folderFloatingActionButton;

    int page = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        // Obtaining query
        String query = getIntent().getStringExtra("QUERY");

        // FABs
        shareFloatingActionButton = findViewById(R.id.shareFloatingActionButton);
        selectAllFloatingActionButton = findViewById(R.id.selectAllFloatingActionButton);
        folderFloatingActionButton = findViewById(R.id.folderFloatingActionButton);
        // Sending share button twice here. No need for the delete one.
        listener = new ActivityAdapterListener(this,
                new FloatingActionButton[]{shareFloatingActionButton, selectAllFloatingActionButton, folderFloatingActionButton}
                , false);

        // Refresh layout
        resultsSwipeRefreshLayout = findViewById(R.id.resultsSwipeRefreshLayout);
        resultsSwipeRefreshLayout.setRefreshing(true);
        resultsSwipeRefreshLayout.setOnRefreshListener( () -> resultsSwipeRefreshLayout.setRefreshing(false) );

        // Nested view
        resultsNestedScrollView = findViewById(R.id.resultsNestedScrollView);

        // Creating the RecyclerViews
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);

        // Pass to the adapter
        try {
            resultsRecyclerViewAdapter = new ResultsRecyclerViewAdapter(this, listener);
            new TaskRunner().executeAsync(new ResultsActivity.downloadSearchTask(this, page, query));
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        resultsRecyclerView.setAdapter(resultsRecyclerViewAdapter);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Decorations
        //DayItemDecoration decoration = new DayItemDecoration(getResources().getInteger(R.integer.item_margin));
        //resultsRecyclerView.addItemDecoration(decoration);

        // Item touch helper
        ItemTouchHelper resultsItemTouchHelper = new ItemTouchHelper(new BrowseSwipeToDeleteCallBack(
                resultsRecyclerViewAdapter
                , ResourcesCompat.getDrawable(getResources(),R.drawable.ic_baseline_visibility_off_24, null)
                , ResourcesCompat.getDrawable(getResources(),R.drawable.ic_baseline_archive_24, null)
        ));
        resultsItemTouchHelper.attachToRecyclerView(resultsRecyclerView);

        // Initial position of scrolls
        resultsRecyclerView.smoothScrollToPosition(0);

        // Buttons
        nextButton = findViewById(R.id.nextButtonS);
        View.OnClickListener nextListener = view -> {
            if (resultsRecyclerViewAdapter.numberFound == resultsRecyclerViewAdapter.MAX_RESULTS) {
                // next page
                page += 1;
                // update
                resultsSwipeRefreshLayout.setRefreshing(true);
                new TaskRunner().executeAsync(new ResultsActivity.downloadSearchTask(this, page, query));
            }
        };
        nextButton.setOnClickListener(nextListener);

        backButton = findViewById(R.id.backButtonS);
        View.OnClickListener backListener = view -> {
            if(page>0) {
                // next page
                page -= 1;
                // update
                resultsSwipeRefreshLayout.setRefreshing(true);
                new TaskRunner().executeAsync(new ResultsActivity.downloadSearchTask(this, page, query));
            }
        };
        backButton.setOnClickListener(backListener);


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

        View.OnClickListener selectAllOnClickListener = view -> resultsRecyclerViewAdapter.selectAllItems();
        selectAllFloatingActionButton.setOnClickListener(selectAllOnClickListener);
    }


    public void updatePapers(ArrayList<ArXivPaper> papers){
        resultsRecyclerViewAdapter.updatePage(page);
        resultsRecyclerViewAdapter.refresh(papers);
        resultsSwipeRefreshLayout.setRefreshing(false);
        // reset scrolls
        resultsRecyclerView.smoothScrollToPosition(0);
        resultsNestedScrollView.smoothScrollTo(0,0);
    }

    private static class downloadSearchTask extends BaseTask<ArrayList<ArXivPaper>> {

        final String fullQuery;
        final ResultsActivity context;

        public downloadSearchTask(ResultsActivity context, int page, String query) {
            int MAX_RESULTS = 10;
            fullQuery = query + "&start=" + page * MAX_RESULTS + "&max_results=" + MAX_RESULTS;
            this.context = context;
        }

        @Override
        public ArrayList<ArXivPaper> call() {
            return ArXivScanner.extractPapersSearch(fullQuery);
        }

        @Override
        public void setDataAfterLoading(ArrayList<ArXivPaper> result) {
            context.updatePapers(result);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (listener.isSelectMode){
            resultsRecyclerViewAdapter.resetSelection();
        }else{
            super.onBackPressed();
        }
    }

}