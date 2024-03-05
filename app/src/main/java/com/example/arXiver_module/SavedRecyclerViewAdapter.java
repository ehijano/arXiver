package com.example.arXiver_module;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arXiver_module.arxiv.ArXivPaper;
import com.example.arXiver_module.browse.BrowseRecyclerViewAdapter;
import com.example.arXiver_module.items.GeneralItem;
import com.example.arXiver_module.task_util.BaseTask;
import com.example.arXiver_module.task_util.TaskRunner;

import java.util.ArrayList;

public class SavedRecyclerViewAdapter extends BrowseRecyclerViewAdapter {

    private final String PREFS;
    public SavedRecyclerViewAdapter(Context context, ActivityAdapterListener listener, String PREFS){
        super(context,listener);
        this.PREFS = PREFS;
        consolidatedItems = getConsolidated();
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder,position);
        if(viewHolder.getItemViewType()== GeneralItem.TYPE_PAPER) {
            PaperRecyclerViewHolder holder = (PaperRecyclerViewHolder) viewHolder;
            ArXivPaper paper = (ArXivPaper) consolidatedItems.get(position);
            // saveButton -> arxiv page button
            holder.saveButton.setText(context.getResources().getString(R.string.arXiv_page));
            View.OnClickListener saveListener = view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(paper.pdfURL.replace(".pdf","").replace("/pdf/","/abs/")));
                context.startActivity(intent);
                resetSelection();
            };
            holder.saveButton.setOnClickListener(saveListener);
            // deleteButton -> un-save Button
            View.OnClickListener deleteListener = view -> {
                ArrayList<ArXivPaper> thisPaper = new ArrayList<>();
                thisPaper.add(paper);
                deleteItems(thisPaper, view);
                resetSelection();
            };
            holder.deleteButton.setOnClickListener(deleteListener);
            // readButton -> read from local?
            View.OnClickListener readListener = view -> {
                boolean reading = false;
                if(paper.downloadID != null){
                    DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri pdfUri = downloadManager.getUriForDownloadedFile(paper.downloadID);
                    if(pdfUri != null){
                        // read from downloads
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        String mime = context.getContentResolver().getType(pdfUri);
                        intent.setDataAndType(pdfUri, mime);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            context.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(context, context.getResources().getString(R.string.no_app_to_view_pdf), Toast.LENGTH_LONG).show();
                        }
                        // don't try to go online.
                        reading = true;
                    }
                }
                if (!reading){
                    Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
                    pdfIntent.setDataAndType(Uri.parse(paper.pdfURL), "application/pdf");
                    pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        context.startActivity(pdfIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(context, context.getResources().getString(R.string.no_app_to_view_pdf), Toast.LENGTH_LONG).show();
                    }
                }
                resetSelection();
            };
            holder.readButton.setOnClickListener(readListener);

            // offline switch
            holder.offlineSwitch.setOnCheckedChangeListener(null);
            // There is a download?
            holder.offlineSwitch.setChecked(paper.downloadID != null);
            holder.offlineSwitch.setVisibility(View.VISIBLE);


            holder.offlineSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if((ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) ) {
                    // App does not have permissions. Ask for them and revert the click
                    ((ParentActivity) context).requestWritePermission();
                    holder.offlineSwitch.setChecked(!isChecked);
                }else {
                    // App has permissions. Perform action.
                    if (isChecked) {
                        new TaskRunner().executeAsync(new downloadPDFTask(context, paper));
                    } else {
                        if (paper.downloadID != null) {// This should always be true, as we are turning off the switch, which means its on, which means the paper is downloaded
                            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                            if (downloadManager.getUriForDownloadedFile(paper.downloadID) != null) {
                                // delete paper pdf
                                downloadManager.remove(paper.downloadID);
                                // inform class that its not downloaded
                                paper.setDownloadID(null);
                                // inform preferences about deletion
                                ((ParentActivity) context).updateDownload(paper.id, null);
                                Toast.makeText(context, context.getResources().getString(R.string.file_deleted), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });

            // Download completion receiver
            context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        }
    }

    private static class downloadPDFTask extends BaseTask<Long> {

        final ArXivPaper paper;
        final Context context;

        public downloadPDFTask(Context context, ArXivPaper paper) {
            this.paper = paper;
            this.context = context;
        }

        @Override
        public Long call() {
            DownloadManager downloadmanager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(paper.pdfURL);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(paper.id+".pdf");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, paper.id+".pdf");
            return downloadmanager.enqueue(request);
        }

        @Override
        public void setDataAfterLoading(Long result) {
            // paper has been created
            // inform class about download
            paper.setDownloadID(result);
            // Update shared preferences
            ParentActivity parentActivity = (ParentActivity) context;
            parentActivity.updateDownload(paper.id, result);
            Toast.makeText(context, context.getResources().getString(R.string.file_downloading), Toast.LENGTH_SHORT).show();
        }
    }

    final BroadcastReceiver onComplete=new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, context.getResources().getString(R.string.file_downloaded), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public ArrayList<GeneralItem> getConsolidated() {
        return arXivScanner.getDateConsolidatedPapers(context,PREFS,DOWNLOAD_PREFS);
    }

    @Override
    public void deletePaperPrefs(ArXivPaper paper){
        // Saving the fact that paper has been deleted
        SharedPreferences sharedPreferences = context.getSharedPreferences(DELETED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(paper.id,"R.I.P.");
        editor.apply();
        // Actually deleting the entry from phone memory
        sharedPreferences = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.remove(paper.id);
        editor.apply();
        // Also deleting pdf if it exists
        if(paper.downloadID != null){
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if(downloadManager.getUriForDownloadedFile(paper.downloadID) != null){
                // delete paper pdf
                downloadManager.remove(paper.downloadID);
                // inform class that its not downloaded
                paper.setDownloadID(null);
                // inform preferences about deletion
                ((ParentActivity) context).updateDownload(paper.id,null);
            }
        }
    }

    @Override
    public void unDeletePaperPrefs(ArXivPaper paper){
        // remove from deleted prefs
        SharedPreferences sharedPreferences = context.getSharedPreferences(DELETED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(paper.id);
        editor.apply();

        // Adding back to memory
        sharedPreferences = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString(paper.id,arXivScanner.compress(paper));
        editor.apply();
    }

    @Override
    public void swipeLeft(int position, View itemView) {

    }

    @Override
    public void swipeRight(int position, View itemView) {

    }

}
