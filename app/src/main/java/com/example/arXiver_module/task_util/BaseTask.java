package com.example.arXiver_module.task_util;

public abstract class BaseTask<R> implements CustomCallable<R> {

    @Override
    public void setDataAfterLoading(R result) {

    }

    @Override
    public R call() {
        return null;
    }
}