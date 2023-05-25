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
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;

import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;
import jp.ad.sinet.stream.android.config.remote.configclient.net.HttpsPostTask;

public class GetAuthToken {
    private final String TAG = GetAuthToken.class.getSimpleName();

    private final Context mContext;
    private final String mServerUrl;
    private final GetAuthTokenListener mListener;

    private SSLContext mSslContext = null;

    public GetAuthToken(
            @NonNull Context context,
            @NonNull String serverUrl,
            @NonNull GetAuthTokenListener listener) {
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

    public void run(@NonNull String user, @NonNull String secretKey) {
        final String urlString = mServerUrl + "/api/v1/authentication";

        JSONObject requestData = buildJsonObject(user, secretKey);
        if (requestData == null) {
            return;
        }

        HttpsPostTask httpsPostTask =
                new HttpsPostTask(
                        mContext,
                        urlString,
                        requestData,
                        new HttpsPostTask.HttpsPostTaskListener() {
                            @Override
                            public void onUploadFinished(
                                    @NonNull JSONObject responseData) {
                                //Log.d(TAG, "onUploadFinished: " + responseData);
                                parseJsonObject(responseData);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                /* Relay error info to the listener */
                                mListener.onError(description);
                            }
                        });

        /* Set SSLContext option */
        if (mSslContext != null) {
            httpsPostTask.setSslContext(mSslContext);
        }

        /* Relay developer option */
        httpsPostTask.enableDebug(mDebugEnabled);

        httpsPostTask.execute();
    }

    @Nullable
    private JSONObject buildJsonObject(@NonNull String user, @NonNull String secretKey) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JsonTags.KEY_AUTH_STRATEGY, "api-access");
            jsonObject.put(JsonTags.KEY_AUTH_USER, user);
            jsonObject.put(JsonTags.KEY_AUTH_SECRET_KEY, secretKey);
        } catch (JSONException e) {
            mListener.onError("JSONObject.put: " + e.getMessage());
            return null;
        }
        return jsonObject;
    }

    private void parseJsonObject(@NonNull JSONObject jsonObject) {
        final String[] keys = {
                JsonTags.KEY_AUTH_ACCESS_TOKEN,
                JsonTags.KEY_AUTH_AUTHENTICATION,
                JsonTags.KEY_AUTH_USER,
        };
        for (String key : keys) {
            if (!jsonObject.has(key)) {
                mListener.onError(TAG + ": JSONObject: No mapping for \"" + key + "\"?");
                return;
            }
        }

        try {
            String accessToken = jsonObject.getString(JsonTags.KEY_AUTH_ACCESS_TOKEN);
            JSONObject authentication = jsonObject.getJSONObject(JsonTags.KEY_AUTH_AUTHENTICATION);
            JSONObject userInfo = jsonObject.getJSONObject(JsonTags.KEY_AUTH_USER);

            mListener.onAuthToken(accessToken, authentication, userInfo);
        } catch (JSONException e) {
            mListener.onError("JSONObject(user): " + e.getMessage());
        }
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface GetAuthTokenListener {
        void onAuthToken(@NonNull String accessToken,
                         @NonNull JSONObject authentication,
                         @NonNull JSONObject userInfo);

        void onError(@NonNull String description);
    }
}
