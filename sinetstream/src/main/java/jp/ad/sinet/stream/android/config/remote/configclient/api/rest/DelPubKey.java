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

package jp.ad.sinet.stream.android.config.remote.configclient.api.rest;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.net.ssl.SSLContext;

import jp.ad.sinet.stream.android.config.remote.configclient.net.HttpsDeleteTask;

public class DelPubKey {
    private final String TAG = DelPubKey.class.getSimpleName();

    private final Context mContext;
    private final String mServerUrl;
    private final DelPubKeyListener mListener;

    private SSLContext mSslContext = null;

    public DelPubKey(@NonNull Context context,
                     @NonNull String serverUrl,
                     @NonNull DelPubKeyListener listener) {
        this.mContext = context;
        this.mServerUrl = serverUrl;
        this.mListener = listener;
    }

    public void setSslContext(@NonNull SSLContext sslContext) {
        /*
         * Optional settings:
         * Set if client-certificate or self-signed server certificate is used.
         */
        mSslContext = sslContext;
    }

    public void run(@Nullable Integer id,
                    @NonNull String authToken) {
        /* If given "id" is null, ask server to get all public keys */
        final String urlString = mServerUrl + "/api/v1/public-keys/" +
                (id != null ? id.intValue() : "");

        HttpsDeleteTask httpsDeleteTask = new HttpsDeleteTask(mContext,
                urlString,
                new HttpsDeleteTask.HttpsDeleteTaskListener() {
                    @Override
                    public void onDeleteFinished(@NonNull JSONObject responseData) {
                        String jsonData;
                        try {
                            jsonData = responseData.toString(4);
                        } catch (JSONException e) {
                            Log.w(TAG, "onDeleteFinished: " + e.getMessage());
                            jsonData = responseData.toString();
                        }
                        if (mDebugEnabled) {
                            Log.d(TAG, "onDeleteFinished: " + jsonData);
                        }
                        mListener.onDeleteFinished();
                    }

                    @Override
                    public void onDeleteFinished(@NonNull JSONArray responseData) {
                        String jsonData;
                        try {
                            jsonData = responseData.toString(4);
                        } catch (JSONException e) {
                            Log.w(TAG, "onDeleteFinished: " + e.getMessage());
                            jsonData = responseData.toString();
                        }
                        if (mDebugEnabled) {
                            Log.d(TAG, "onDeleteFinished: " + jsonData);
                        }
                        mListener.onDeleteFinished();
                    }

                    @Override
                    public void onDeleteFinished() {
                        if (mDebugEnabled) {
                            Log.d(TAG, "onDeleteFinished: Empty data");
                        }
                        mListener.onDeleteFinished();
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        /* Relay error info to the listener */
                        mListener.onError(description);
                    }
                });

        /* Set extra header option */
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + authToken); /* rfc6750 */
        httpsDeleteTask.setExtraHeaders(headers);

        /* Set SSLContext option */
        if (mSslContext != null) {
            httpsDeleteTask.setSslContext(mSslContext);
        }

        /* Relay developer option */
        httpsDeleteTask.enableDebug(mDebugEnabled);

        httpsDeleteTask.execute();
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface DelPubKeyListener {
        void onDeleteFinished();
        void onError(@NonNull String description);
    }
}
