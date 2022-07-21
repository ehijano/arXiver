package com.example.arXiver_module;

public class Folder {

    final String folderName;
    String folderPrefs;
    public static String savedFolderName = "";
    public static String SAVED_PREFS = "";

    public Folder(String folderName){
        this.folderName = folderName;
        if(!folderName.equals(savedFolderName)){
            this.folderPrefs = folderName+"_Prefs";
        }else{
            this.folderPrefs = SAVED_PREFS;
        }
    }

    public void setSavedFolderData(String s, String p){
        savedFolderName = s;
        SAVED_PREFS = p;
    }

    public void setFolderPrefs(String PREFS){
        folderPrefs = PREFS;
    }
}
