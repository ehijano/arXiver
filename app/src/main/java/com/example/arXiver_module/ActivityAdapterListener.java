package com.example.arXiver_module;

import android.app.Activity;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ActivityAdapterListener {
    boolean isSelectMode;

    public final FloatingActionButton[] allFABs;

    ArrayList<ArXivPaper> selectedPapers = new ArrayList<>();
    final Activity activity;

    public ActivityAdapterListener(Activity activity, FloatingActionButton[] allFABs,  boolean isSelectMode){
        this.activity = activity;

        this.allFABs = allFABs;

        this.isSelectMode = isSelectMode;
    }

    public void update(boolean b, ArrayList<ArXivPaper> papers){
        this.isSelectMode = b;
        this.selectedPapers = papers;

        for(FloatingActionButton eachFAB: allFABs){
            eachFAB.setVisibility(b ? View.VISIBLE : View.GONE);
        }
    }

    public String generateMessage(){
        String message;
        ArXivScanner scanner = new ArXivScanner();

        StringBuilder messageBuilder = new StringBuilder();
        for (ArXivPaper paper : selectedPapers) {
            messageBuilder.append(scanner.paperMessage(paper)).append("\n __________ \n");
        }
        message = messageBuilder.toString();

        if(!message.isEmpty()){
            message = activity.getResources().getString(R.string.message_head)+ "\n __________ \n" + message;
        }

        return message;

    }
}
