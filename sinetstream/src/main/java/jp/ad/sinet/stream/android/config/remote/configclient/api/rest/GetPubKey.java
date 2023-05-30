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
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.SSLContext;

import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;
import jp.ad.sinet.stream.android.config.remote.configclient.model.RemotePubKey;
import jp.ad.sinet.stream.android.config.remote.configclient.net.HttpsGetTask;

public class GetPubKey {
    private final String TAG = GetPubKey.class.getSimpleName();

    private final Context mContext;
    private final String mServerUrl;
    private final GetPubKeyListener mListener;

    private SSLContext mSslContext = null;

    public GetPubKey(@NonNull Context context,
                     @NonNull String serverUrl,
                     @NonNull GetPubKeyListener listener) {
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

        if (mDebugEnabled) {
            Log.d(TAG, "URL[" + urlString + "]");
        }
        HttpsGetTask httpsGetTask =
                new HttpsGetTask(
                        mContext,
                        urlString,
                        new HttpsGetTask.HttpsGetTaskListener() {
                            @Override
                            public void onDownloadFinished(
                                    @NonNull JSONObject responseData) {
                                String jsonData;
                                try {
                                    jsonData = responseData.toString(4);
                                } catch (JSONException e) {
                                    Log.w(TAG, "onDownloadFinished: " + e.getMessage());
                                    jsonData = responseData.toString();
                                }
                                if (mDebugEnabled) {
                                    Log.d(TAG, "onDownloadFinished: " + jsonData);
                                }
                                parsePubKeyResponse(responseData);
                            }

                            @Override
                            public void onDownloadFinished(
                                    @NonNull JSONArray responseData) {
                                if (mDebugEnabled) {
                                    Log.d(TAG, "onDownLoadFinished(Array)\n" + responseData);
                                }
                                parsePubKeyResponse(responseData);
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

    private void parsePubKeyResponse(@NonNull JSONObject jsonObject) {
        RemotePubKey remotePubKey = parseJsonObject(jsonObject);
        if (remotePubKey != null) {
            mListener.onRemotePubKey(remotePubKey);
        }
    }

    private void parsePubKeyResponse(@NonNull JSONArray jsonArray) {
        ArrayList<RemotePubKey> arrayList = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject;
            try {
                jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                mListener.onError(TAG + ": Invalid JSONArray: " + e.getMessage());
                return;
            }

            RemotePubKey remotePubKey = parseJsonObject(jsonObject);
            if (remotePubKey != null) {
                arrayList.add(remotePubKey);
            }
        }

        mListener.onRemotePubKeys(arrayList.toArray(new RemotePubKey[0]));
    }

    @Nullable
    private RemotePubKey parseJsonObject(@NonNull JSONObject jsonObject) {
        final String[] keys = {
                JsonTags.KEY_PUBKEY_ID,
                JsonTags.KEY_PUBKEY_FINGERPRINT,
                //JsonTags.KEY_PUBKEY_COMMENT,
                JsonTags.KEY_PUBKEY_DEFAULT_KEY,
                JsonTags.KEY_PUBKEY_CREATED_AT,
        };
        for (String key : keys) {
            if (!jsonObject.has(key)) {
                mListener.onError(TAG + ": JSONObject: No mapping for \"" + key + "\"?");
                return null;
            }
        }

        try {
            int id = jsonObject.getInt(JsonTags.KEY_PUBKEY_ID);
            String fingerPrint = jsonObject.getString(JsonTags.KEY_PUBKEY_FINGERPRINT);

            String comment = "";
            if (jsonObject.has(JsonTags.KEY_PUBKEY_COMMENT)) {
                comment = jsonObject.getString(JsonTags.KEY_PUBKEY_COMMENT);
            }

            boolean isDefaultKey = jsonObject.getBoolean(JsonTags.KEY_PUBKEY_DEFAULT_KEY);
            String createdAt = jsonObject.getString(JsonTags.KEY_PUBKEY_CREATED_AT);

            return new RemotePubKey(id, fingerPrint, comment, isDefaultKey, createdAt);
        } catch (JSONException e) {
            mListener.onError(TAG + ": Invalid JSONObject: " + e.getMessage());
            return null;
        }
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface GetPubKeyListener {
        void onRemotePubKey(@NonNull RemotePubKey remotePubKey);
        void onRemotePubKeys(@NonNull RemotePubKey[] remotePubKeys);
        void onError(@NonNull String description);
    }
}
