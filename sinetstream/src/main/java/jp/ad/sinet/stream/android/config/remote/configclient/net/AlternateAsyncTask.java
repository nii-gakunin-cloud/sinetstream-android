/*
 * Copyright (c) 2022 National Institute of Informatics
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package jp.ad.sinet.stream.android.config.remote.configclient.net;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

abstract class AlternateAsyncTask<T> {
    private boolean mIsCanceled = false;

    private class AsyncRunnable implements Runnable {
        private T result;
        final Handler handler = new Handler(Looper.getMainLooper());

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            onPreExecute();
            result = doInBackground();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (mIsCanceled) {
                        onCancelled();
                    } else {
                        onPostExecute(result);
                    }
                }
            });
        }
    }

    private Future<?> mFuture = null;
    public void execute() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        mFuture = executorService.submit(new AsyncRunnable());
    }

    protected void onPreExecute() {
        /* stub */
    }

    protected T doInBackground() {
        /* stub */
        return null;
    }

    protected void onPostExecute(@NonNull T result) {
        /* stub */
    }

    protected void onCancelled() {
        /* stub */
    }

    protected boolean cancel(boolean mayInterruptIfRunning) {
        if (mFuture != null) {
            if (mFuture.isDone()) {
                return true;
            } else {
                mIsCanceled = true;
                return mFuture.cancel(mayInterruptIfRunning);
            }
        } else {
            return false;
        }
    }

    protected boolean isCancelled() {
        if (mFuture != null) {
            return mFuture.isCancelled();
        } else {
            return false;
        }
    }
}
