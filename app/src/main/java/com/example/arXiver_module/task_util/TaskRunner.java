package com.example.arXiver_module.task_util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskRunner {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newCachedThreadPool();

    public <Res> void executeAsync(CustomCallable<Res> callable) {
        try {
            executor.execute(new RunnableTask<>(handler, callable));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class RunnableTask<Res> implements Runnable{
        private final Handler handler;
        private final CustomCallable<Res> callable;

        public RunnableTask(Handler handler, CustomCallable<Res> callable) {
            this.handler = handler;
            this.callable = callable;
        }

        @Override
        public void run() {
            try {
                final Res result = callable.call();
                handler.post(new RunnableTaskForHandler<>(callable, result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class RunnableTaskForHandler<Res> implements Runnable{

        private final CustomCallable<Res> callable;
        private final Res result;

        public RunnableTaskForHandler(CustomCallable<Res> callable, Res result) {
            this.callable = callable;
            this.result = result;
        }

        @Override
        public void run() {
            callable.setDataAfterLoading(result);
        }
    }
}

