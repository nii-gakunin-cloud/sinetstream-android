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
import org.json.JSONObject;

import java.util.HashMap;

import javax.net.ssl.SSLContext;

import jp.ad.sinet.stream.android.config.remote.configclient.constants.CryptoTypes;
import jp.ad.sinet.stream.android.config.remote.configclient.net.HttpsGetTask;

public class GetSecret {
    private final String TAG = GetSecret.class.getSimpleName();

    private final Context mContext;
    private final String mServerUrl;
    private final String mFingerPrint;
    private final GetSecretListener mListener;

    private SSLContext mSslContext = null;

    private boolean mUseOaepHashSha1 = true;

    public GetSecret(@NonNull Context context,
                     @NonNull String serverUrl,
                     @Nullable String fingerPrint,
                     @NonNull GetSecretListener listener) {
        this.mContext = context;
        this.mServerUrl = serverUrl;
        this.mFingerPrint = fingerPrint;
        this.mListener = listener;
    }

    public void setSslContext(@NonNull SSLContext sslContext) {
        /*
         * Optional settings:
         * Set if client-certificate or self-signed server certificate is used.
         */
        mSslContext = sslContext;
    }

    public void run(@NonNull String secretId, @NonNull String authToken) {
        String urlString = mServerUrl + "/api/v1/secrets/" + secretId;

        /* Optionally, we can specify the KEM (Key Encryption Method) parameter */
        if (mUseOaepHashSha1) {
            urlString += "?kem=0x" + CryptoTypes.PK_RSA_OAEP_SHA1;
        }

        HttpsGetTask httpsGetTask =
                new HttpsGetTask(mContext,
                        urlString,
                        new HttpsGetTask.HttpsGetTaskListener() {
                            @Override
                            public void onDownloadFinished(@NonNull JSONObject responseData) {
                                mListener.onSecretInfo(responseData);
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
        if (mFingerPrint != null) {
            headers.put("SINETStream-config-publickey", mFingerPrint);
        }
        httpsGetTask.setExtraHeaders(headers);

        /* Set SSLContext option */
        if (mSslContext != null) {
            httpsGetTask.setSslContext(mSslContext);
        }

        /* Relay developer option */
        httpsGetTask.enableDebug(mDebugEnabled);

        httpsGetTask.execute();
    }

    public void setUseOaepHashSha1(boolean enabled) {
        mUseOaepHashSha1 = enabled;
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface GetSecretListener {
        void onSecretInfo(@NonNull JSONObject secret);
        void onError(@NonNull String description);
    }
}
