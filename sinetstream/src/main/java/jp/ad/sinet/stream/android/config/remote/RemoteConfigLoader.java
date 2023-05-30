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

package jp.ad.sinet.stream.android.config.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Map;

import jp.ad.sinet.stream.android.config.remote.configclient.api.ConfigClient;
import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;

public class RemoteConfigLoader {
    private final String TAG = RemoteConfigLoader.class.getSimpleName();

    private final String mServerUrl;
    private final String mAccount;
    private final String mSecretKey;

    private String mPredefinedDataStream = null;
    private String mPredefinedServiceName = null;

    private RemoteConfigListener mListener;

    /**
     * Constructs a RemoteConfigLoader instance.
     *
     * @param serverUrl The URL of the configuration server.
     * @param account The login account for the configuration server.
     * @param secretKey The API key published by the configuration server.
     */
    public RemoteConfigLoader(@NonNull String serverUrl,
                              @NonNull String account,
                              @NonNull String secretKey) {
        this.mServerUrl = serverUrl;
        this.mAccount = account;
        this.mSecretKey = secretKey;
    }

    /**
     * Sets optional parameters to be used in the REST-API requests.
     *
     * @param dataStream The name of a set of SINETStream configuration
     *                   which has defined on the configuration server.
     * @param serviceName A dataStream consists from a single service
     *                    or multiple services. This parameter specifies
     *                    which service to take in the dataStream.
     */
    public void setPredefinedParameters(
            @Nullable String dataStream,
            @Nullable String serviceName) {
        mPredefinedDataStream = dataStream;
        mPredefinedServiceName = serviceName;
    }

    /**
     * Downloads the set of configuration settings just for peek the contents.
     * <p>
     *     This method meant to be used for downloading the configuration parameters
     *     in raw format, so that the caller can check the specified configuration
     *     settings on the server side.
     * </p>
     *
     * @param context The {@link Context} object which hosts dialogs.
     * @param dialogLabel An optional label to be shown on each dialogs.
     * @param listener An implementation of {@link RemoteConfigPeekListener}.
     */
    public void peek(@NonNull Context context,
                     @Nullable String dialogLabel,
                     @NonNull RemoteConfigPeekListener listener) {
        ConfigClient configClient =
                new ConfigClient(context,
                        dialogLabel,
                        new ConfigClient.ConfigClientListener() {
                            @Override
                            public void onConfigInfo(
                                    @NonNull String name,
                                    @NonNull String service,
                                    @Nullable JSONObject header,
                                    @NonNull JSONObject config,
                                    @NonNull JSONArray attachments,
                                    @Nullable JSONArray secrets) {
                                /* Return raw data as is */
                                listener.onRawData(
                                        name, service, header, config, attachments, secrets);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                /* Relay error info to the listener */
                                listener.onError(description);
                            }
                        });

        /* Relay developer option */
        configClient.enableDebug(mDebugEnabled);

        if (mPredefinedDataStream != null && mPredefinedServiceName != null) {
            configClient.setPredefinedParameters(
                    mPredefinedDataStream, mPredefinedServiceName);
        }
        configClient.getRemoteConfig(mServerUrl, mAccount, mSecretKey);
    }

    /**
     * Downloads the set of configuration settings in key-value format.
     *
     * @param context The {@link Context} object which hosts dialogs.
     * @param dialogLabel An optional label to be shown on each dialogs.
     * @param listener An implementation of {@link RemoteConfigListener}.
     */
    public void load(@NonNull Context context,
                     @Nullable String dialogLabel,
                     @NonNull RemoteConfigListener listener) {
        mListener = listener;

        ConfigClient configClient =
                new ConfigClient(context,
                        dialogLabel,
                        new ConfigClient.ConfigClientListener() {
                            @Override
                            public void onConfigInfo(
                                    @NonNull String name,
                                    @NonNull String service,
                                    @Nullable JSONObject header,
                                    @NonNull JSONObject config,
                                    @NonNull JSONArray attachments,
                                    @Nullable JSONArray secrets) {
                                processConfigInfo(
                                        name, service, header, config, attachments, secrets);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                /* Relay error info to the listener */
                                mListener.onError(description);
                            }
                        });

        /* Relay developer option */
        configClient.enableDebug(mDebugEnabled);

        if (mPredefinedDataStream != null && mPredefinedServiceName != null) {
            configClient.setPredefinedParameters(
                    mPredefinedDataStream, mPredefinedServiceName);
        }
        configClient.getRemoteConfig(mServerUrl, mAccount, mSecretKey);
    }

    private void processConfigInfo(@NonNull String name,
                                   @NonNull String service,
                                   @Nullable JSONObject header,
                                   @NonNull JSONObject config,
                                   @NonNull JSONArray attachments,
                                   @Nullable JSONArray secrets) {
        if (mDebugEnabled) {
            Log.d(TAG, "CONFIG: " + name + "\n" + "SERVICE: " + service);

            Log.d(TAG, "--HEADER--");
            if (header != null) {
                try {
                    Log.d(TAG, "header: " + header.toString(4));
                } catch (JSONException e) {
                    mListener.onError(TAG + ": Invalid Header: " + e.getMessage());
                    return;
                }
            }

            Log.d(TAG, "--CONFIG--");
            Log.d(TAG, "name: " + name);
            try {
                Log.d(TAG, "config: " + config.toString(4));
            } catch (JSONException e) {
                mListener.onError(TAG + ": Invalid config: " + e.getMessage());
                return;
            }
        }

        boolean hasValidAttachments = false;
        String serverCertificate = null;
        String clientCertificate = null;

        for (int i = 0, n = attachments.length(); i < n; i++) {
            String encodedStr, decodedStr;
            JSONObject elem;
            try {
                elem = attachments.getJSONObject(i);
            } catch (JSONException e) {
                mListener.onError(TAG + ": Invalid attachment: " + e.getMessage());
                return;
            }

            /* Both "target" and "value" must coexist in an "elem". */
            if (elem.has(JsonTags.KEY_ATTACHMENT_VALUE)) {
                try {
                    encodedStr = elem.getString(JsonTags.KEY_ATTACHMENT_VALUE);
                } catch (JSONException e) {
                    mListener.onError(TAG + ": Invalid attachment: " + e.getMessage());
                    return;
                }
            } else {
                mListener.onError(TAG + ": Invalid attachment: " +
                        "tag(" + JsonTags.KEY_ATTACHMENT_VALUE + ") is missing");
                return;
            }

            if (elem.has(JsonTags.KEY_ATTACHMENT_TARGET)) {
                String target;
                try {
                    target = elem.getString(JsonTags.KEY_ATTACHMENT_TARGET);
                } catch (JSONException e) {
                    mListener.onError(TAG + ": Invalid attachment: " + e.getMessage());
                    return;
                }

                if (target.endsWith(JsonTags.KEY_ATTACHMENT_TLS_CA_CERTS_DATA)) {
                    serverCertificate = encodedStr;
                    hasValidAttachments = true;
                } else if (target.endsWith(JsonTags.KEY_ATTACHMENT_TLS_CERTFILE_DATA)) {
                    clientCertificate = encodedStr;
                    hasValidAttachments = true;
                } else {
                    Log.w(TAG, TAG + ": Unknown target(" + target + ")?");
                }
            } else {
                mListener.onError(TAG + ": Invalid attachment: " +
                        "tag(" + JsonTags.KEY_ATTACHMENT_TARGET + ") is missing");
                return;
            }
        }

        if (secrets != null) {
            for (int i = 0, n = secrets.length(); i < n; i++) {
                try {
                    JSONObject elem = secrets.getJSONObject(i);
                    if (mDebugEnabled) {
                        Log.d(TAG, "secrets[" + (i + 1) + "/" + n + "]: " +
                                elem.toString());
                    }
                } catch (JSONException e) {
                    mListener.onError(TAG + ": Invalid secrets: " + e.getMessage());
                    return;
                }
            }
        }

        /* Map basic configuration parameters */
        Map<String,Object> configParameters;
        try {
            Type type = new TypeToken<ArrayMap<String,Object>>(){}.getType();
            Gson gson = new Gson();
            configParameters = gson.fromJson(config.toString(), type);
        } catch (JsonParseException e) {
            mListener.onError(TAG + ": Invalid JSON Syntax: " + e.getMessage());
            return;
        }

        /* Map optional configuration attachments */
        Map<String,Object> configAttachments = null;
        if (hasValidAttachments) {
            configAttachments = new ArrayMap<>();
            if (serverCertificate != null) {
                configAttachments.put(
                        JsonTags.KEY_ATTACHMENT_TLS_CA_CERTS_DATA, serverCertificate);
            }
            if (clientCertificate != null) {
                configAttachments.put(
                        JsonTags.KEY_ATTACHMENT_TLS_CERTFILE_DATA, clientCertificate);
            }
        }

        mListener.onLoaded(configParameters, configAttachments);
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface RemoteConfigListener {
        void onLoaded(@NonNull Map<String,Object> parameters,
                      @Nullable Map<String,Object> attachments);
        void onError(@NonNull String description);
    }

    public interface RemoteConfigPeekListener {
        void onRawData(@NonNull String name,
                       @NonNull String service,
                       @Nullable JSONObject header,
                       @NonNull JSONObject config,
                       @NonNull JSONArray attachments,
                       @Nullable JSONArray secrets);
        void onError(@NonNull String description);
    }
}
