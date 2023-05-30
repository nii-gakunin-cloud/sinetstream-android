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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import javax.net.ssl.SSLContext;

import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;
import jp.ad.sinet.stream.android.config.remote.configclient.model.RemotePubKey;
import jp.ad.sinet.stream.android.config.remote.configclient.net.HttpsPostTask;

public class AddPubKey {
    private final String TAG = AddPubKey.class.getSimpleName();

    private final Context mContext;
    private final String mServerUrl;
    private final AddPubKeyListener mListener;

    private SSLContext mSslContext = null;

    public AddPubKey(@NonNull Context context,
                     @NonNull String serverUrl,
                     @NonNull AddPubKeyListener listener) {
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

    public void run(@NonNull String base64EncodedPublicKey,
                    @Nullable String comment,
                    boolean isDefaultKey,
                    @NonNull String authToken) {
        final String urlString = mServerUrl + "/api/v1/public-keys";

        JSONObject requestData = buildJsonObject(
                base64EncodedPublicKey, comment, isDefaultKey);
        if (requestData == null) {
            return;
        }

        HttpsPostTask httpsPostTask =
                new HttpsPostTask(mContext,
                        urlString,
                        requestData,
                        new HttpsPostTask.HttpsPostTaskListener() {
                            @Override
                            public void onUploadFinished(@NonNull JSONObject responseData) {
                                RemotePubKey remotePubKey = parseJsonObject(responseData);
                                if (remotePubKey != null) {
                                    mListener.onPubKey(remotePubKey);
                                }
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
        httpsPostTask.setExtraHeaders(headers);

        /* Set SSLContext option */
        if (mSslContext != null) {
            httpsPostTask.setSslContext(mSslContext);
        }

        /* Relay developer option */
        httpsPostTask.enableDebug(mDebugEnabled);

        httpsPostTask.execute();
    }

    @Nullable
    private JSONObject buildJsonObject(
            @NonNull String base64EncodedPublicKey,
            @Nullable String comment,
            boolean isDefaultKey) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JsonTags.KEY_PUBKEY_PUBLIC_KEY, base64EncodedPublicKey);
            if (comment != null) {
                jsonObject.put(JsonTags.KEY_PUBKEY_COMMENT, comment);
            }
            jsonObject.put(JsonTags.KEY_PUBKEY_DEFAULT_KEY, isDefaultKey);
        } catch (JSONException e) {
            mListener.onError("JSONObject.put: " + e.getMessage());
            return null;
        }
        return jsonObject;
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

    public interface AddPubKeyListener {
        void onPubKey(@NonNull RemotePubKey remotePubKey);
        void onError(@NonNull String description);
    }
}
