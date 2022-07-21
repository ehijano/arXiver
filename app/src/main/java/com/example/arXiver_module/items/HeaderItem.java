package com.example.arXiver_module.items;

public class HeaderItem extends GeneralItem {

    public final String header;
    public HeaderItem(String header){
        this.header = header;
    }

    @Override
    public int getType() {
        return 2;
    }
}
