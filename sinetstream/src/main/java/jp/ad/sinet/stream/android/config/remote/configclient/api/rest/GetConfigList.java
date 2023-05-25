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

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.SSLContext;

import jp.ad.sinet.stream.android.config.remote.configclient.net.HttpsGetTask;

public class GetConfigList {
    private final String TAG = GetConfigList.class.getSimpleName();

    private final Context mContext;
    private final String mServerUrl;
    private final GetConfigListListener mListener;

    private SSLContext mSslContext = null;

    public GetConfigList(
            @NonNull Context context,
            @NonNull String serverUrl,
            @NonNull GetConfigListListener listener) {
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

    public void run(@NonNull String accessToken) {
        final String urlString = mServerUrl + "/api/v1/configs";

        HttpsGetTask httpsGetTask =
                new HttpsGetTask(
                        mContext,
                        urlString,
                        new HttpsGetTask.HttpsGetTaskListener() {
                            @Override
                            public void onDownloadFinished(
                                    @NonNull JSONObject responseData) {
                                /* This case does not exist */
                            }

                            @Override
                            public void onDownloadFinished(
                                    @NonNull JSONArray responseData) {
                                parseJsonArray(responseData);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                /* Relay error info to the listener */
                                mListener.onError(description);
                            }
                        });

        /* Set extra header option */
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + accessToken); /* rfc6750 */
        httpsGetTask.setExtraHeaders(headers);

        /* Set SSLContext option */
        if (mSslContext != null) {
            httpsGetTask.setSslContext(mSslContext);
        }

        /* Relay developer option */
        httpsGetTask.enableDebug(mDebugEnabled);

        httpsGetTask.execute();
    }

    private void parseJsonArray(@NonNull JSONArray jsonArray) {
        List<String> nameList = new ArrayList<>();
        for (int i = 0, n = jsonArray.length(); i < n; i++) {
            try {
                String name = jsonArray.getString(i);
                nameList.add(name);
            } catch (JSONException e) {
                mListener.onError("JSONArray: " + e.getMessage());
                return;
            }
        }

        String[] strArray = nameList.toArray(new String[0]);
        mListener.onDataStreamNames(strArray);
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface GetConfigListListener {
        void onDataStreamNames(@NonNull String[] dataStreamNames);
        void onError(@NonNull String description);
    }
}
