package com.example.arXiver_module.items;

public class ResultsItem extends GeneralItem {
    public final String header;
    public ResultsItem(String header){
        this.header = header;
    }

    @Override
    public int getType() {
        return TYPE_RESULTS;
    }
}

