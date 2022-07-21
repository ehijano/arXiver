package com.example.arXiver_module.items;

public abstract class GeneralItem {

    public static final int TYPE_DATE = 0;
    public static final int TYPE_PAPER = 1;
    public static final int TYPE_HEADER = 2;
    public static final int TYPE_RESULTS = 3;

    abstract public int getType();
}
