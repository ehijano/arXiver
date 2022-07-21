package com.example.arXiver_module.items;

public class DateItem extends GeneralItem {
    public final String date;
    public DateItem(String date){
        this.date = date;
    }

    @Override
    public int getType() {
        return TYPE_DATE;
    }
}
