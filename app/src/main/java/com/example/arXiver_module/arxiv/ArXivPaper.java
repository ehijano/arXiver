package com.example.arXiver_module.arxiv;


import com.example.arXiver_module.items.GeneralItem;

public class ArXivPaper extends GeneralItem {

    public final String title;
    public final String id;
    public final String[] authors;
    public final String[] categories;
    public final String pdfURL;
    public final String publishedDate;
    public final String abs;
    public final String updatedDate;

    public boolean isExpanded;
    public final String announceType;
    public String dateSaved = "";
    public Long downloadID;
    public boolean isSelected = false;


    public ArXivPaper(String title, String id, String[] authors, String[] categories, String pdfURL, String publishedDate, String updatedDate, String abs, String announceType) {
        this.title = title;
        this.id = id;
        this.authors = authors;
        this.pdfURL = pdfURL;
        this.publishedDate = publishedDate;
        this.updatedDate = updatedDate;
        this.abs = abs;
        this.categories = categories;
        this.isExpanded = false;
        this.announceType = announceType;

    }

    public void setDownloadID(Long l){
        downloadID = l;
    }

    public void setExpanded(boolean b){
        isExpanded = b;
    }

    public void setSelected(boolean b){
        isSelected = b;
    }

    public void setDateSaved(String date){
        dateSaved = date;
    }

    @Override
    public int getType() {
        return TYPE_PAPER;
    }
}
