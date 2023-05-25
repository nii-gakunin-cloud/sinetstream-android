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

package jp.ad.sinet.stream.android.config.remote.configclient.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.AddPubKey;
import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.DelPubKey;
import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.GetAuthToken;
import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.GetPubKey;
import jp.ad.sinet.stream.android.config.remote.configclient.model.RemotePubKey;

public class PubKeyClient {
    private final String TAG = PubKeyClient.class.getSimpleName();

    private final Context mContext;
    private final PubKeyClientListener mListener;

    private String mConfigServerUrl;

    public PubKeyClient(
            @NonNull Context context,
            @NonNull PubKeyClientListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    public void getAccessToken(
            @NonNull String configServerUrl,
            @NonNull String user,
            @NonNull String secretKey) {
        mConfigServerUrl = configServerUrl;

        GetAuthToken getAuthToken =
                new GetAuthToken(
                        mContext,
                        mConfigServerUrl,
                        new GetAuthToken.GetAuthTokenListener() {
                            @Override
                            public void onAuthToken(
                                    @NonNull String accessToken,
                                    @NonNull JSONObject authentication,
                                    @NonNull JSONObject userInfo) {
                                if (mDebugEnabled) {
                                    Log.d(TAG, "accessToken: " + accessToken);
                                }
                                mListener.onAccessToken(accessToken);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                Log.e(TAG, "Cannot get AuthToken");
                                /* Relay error info to the listener */
                                mListener.onError(description);
                            }
                        });

        /* Relay developer option */
        getAuthToken.enableDebug(mDebugEnabled);

        getAuthToken.run(user, secretKey);
    }

    public void getRemotePubKeyList(@NonNull String accessToken) {
        getRemotePubKey(null, accessToken);
    }

    public void getRemotePubKey(@Nullable Integer id, @NonNull String accessToken) {
        GetPubKey getPubKey =
                new GetPubKey(
                        mContext,
                        mConfigServerUrl,
                        new GetPubKey.GetPubKeyListener() {
                            @Override
                            public void onRemotePubKey(@NonNull RemotePubKey remotePubKey) {
                                if (mDebugEnabled) {
                                    Log.d(TAG, "GetPubKey.onRemotePubKey");
                                }
                                mListener.onRemotePubKey(remotePubKey);
                            }

                            @Override
                            public void onRemotePubKeys(@NonNull RemotePubKey[] remotePubKeys) {
                                if (mDebugEnabled) {
                                    Log.d(TAG, "getRemotePubKeyList.onRemotePubKeys");
                                }
                                mListener.onRemotePubKeys(remotePubKeys);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                /* Relay error info to the listener */
                                mListener.onError(description);
                            }
                        });

        /* Relay developer option */
        getPubKey.enableDebug(mDebugEnabled);

        getPubKey.run(id, accessToken);
    }

    public void addRemotePubKey(@NonNull String base64EncodedPublicKey,
                                @Nullable String comment,
                                boolean isDefaultKey,
                                @NonNull String accessToken) {
        AddPubKey addPubKey = new AddPubKey(
                mContext,
                mConfigServerUrl,
                new AddPubKey.AddPubKeyListener() {
                    @Override
                    public void onPubKey(@NonNull RemotePubKey remotePubKey) {
                        if (mDebugEnabled) {
                            Log.d(TAG, "addRemotePubKey.onPubKey: " + remotePubKey);
                        }

                        /* Get the latest list */
                        getRemotePubKeyList(accessToken);
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        /* Relay error info to the listener */
                        mListener.onError(description);
                    }
                });

        /* Relay developer option */
        addPubKey.enableDebug(mDebugEnabled);

        addPubKey.run(base64EncodedPublicKey, comment, isDefaultKey, accessToken);
    }

    public void delRemotePubKey(@Nullable Integer id,
                                @NonNull String accessToken) {
        DelPubKey delPubKey = new DelPubKey(
                mContext,
                mConfigServerUrl,
                new DelPubKey.DelPubKeyListener() {
                    @Override
                    public void onDeleteFinished() {
                        if (mDebugEnabled) {
                            Log.d(TAG, "DelPubKey.onDeleteFinished");
                        }

                        /* Get the latest list */
                        getRemotePubKeyList(accessToken);
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        /* Relay error info to the listener */
                        mListener.onError(description);
                    }
                });

        /* Relay developer option */
        delPubKey.enableDebug(mDebugEnabled);

        delPubKey.run(id, accessToken);
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface PubKeyClientListener {
        void onAccessToken(@NonNull String accessToken);
        void onRemotePubKey(@NonNull RemotePubKey remotePubKey);
        void onRemotePubKeys(@NonNull RemotePubKey[] remotePubKeys);
        void onError(@NonNull String description);
    }
}
