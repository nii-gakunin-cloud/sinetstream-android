/*
 * Copyright (C) 2022 National Institute of Informatics
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.net.ssl.SSLContext;

import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;
import jp.ad.sinet.stream.android.config.remote.configclient.net.HttpsGetTask;

public class GetConfig {
    private final String TAG = GetConfig.class.getSimpleName();

    private final Context mContext;
    private final String mServerUrl;
    private final GetConfigListener mListener;

    private SSLContext mSslContext = null;

    public GetConfig(
            @NonNull Context context,
            @NonNull String serverUrl,
            @NonNull GetConfigListener listener) {
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

    public void run(@NonNull String dataStream, @NonNull String authToken) {
        final String urlString = mServerUrl + "/api/v1/configs/" + dataStream;

        HttpsGetTask httpsGetTask =
                new HttpsGetTask(
                        mContext,
                        urlString,
                        new HttpsGetTask.HttpsGetTaskListener() {
                            @Override
                            public void onDownloadFinished(
                                    @NonNull JSONObject responseData) {
                                //Log.d(TAG, "onDownloadFinished: " + responseData);
                                parseJsonObject(responseData);
                            }

                            @Override
                            public void onDownloadFinished(
                                    @NonNull JSONArray responseData) {
                                /* This case does not exist */
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
        httpsGetTask.setExtraHeaders(headers);

        /* Set SSLContext option */
        if (mSslContext != null) {
            httpsGetTask.setSslContext(mSslContext);
        }

        /* Relay developer option */
        httpsGetTask.enableDebug(mDebugEnabled);

        httpsGetTask.execute();
    }

    private void parseJsonObject(@NonNull JSONObject jsonObject) {
        final String[] keys = {
                JsonTags.KEY_NAME,
                JsonTags.KEY_CONFIG,
                JsonTags.KEY_ATTACHMENTS,
                JsonTags.KEY_SECRETS,
        };
        for (String key : keys) {
            if (!jsonObject.has(key)) {
                mListener.onError(TAG + ": JSONObject: No mapping for \"" + key + "\"?");
                return;
            }
        }

        try {
            String name = jsonObject.getString(JsonTags.KEY_NAME);
            JSONObject config = jsonObject.getJSONObject(JsonTags.KEY_CONFIG);
            JSONArray attachments = jsonObject.getJSONArray(JsonTags.KEY_ATTACHMENTS);
            JSONArray secrets = jsonObject.getJSONArray(JsonTags.KEY_SECRETS);

            mListener.onConfigInfo(name, config, attachments, secrets);
        } catch (JSONException e) {
            mListener.onError(TAG + ": Invalid JSONObject: " + e.getMessage());
        }
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface GetConfigListener {
        void onConfigInfo(@NonNull String name,
                          @NonNull JSONObject config,
                          @NonNull JSONArray attachments,
                          @NonNull JSONArray secrets);

        void onError(@NonNull String description);
    }
}
