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

package jp.ad.sinet.stream.android.config.remote.configclient.api;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.GetAuthToken;
import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.GetConfig;
import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.GetConfigList;
import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;
import jp.ad.sinet.stream.android.config.remote.configclient.util.DialogUtil;

public class ConfigClient {
    private final String TAG = ConfigClient.class.getSimpleName();

    private final Context mContext;
    private final String mDialogLabel;
    private final ConfigClientListener mListener;
    private String mConfigServerUrl;
    private String mAccessToken = null;
    private int chosenItemIndex;

    private String mPredefinedDataStream = null;
    private String mPredefinedServiceName = null;

    public ConfigClient(
            @NonNull Context context,
            @Nullable String dialogLabel,
            @NonNull ConfigClientListener listener) {
        this.mContext = context;
        this.mDialogLabel = dialogLabel;
        this.mListener = listener;
    }

    public void setPredefinedParameters(
            @Nullable String dataStream,
            @Nullable String serviceName) {
        mPredefinedDataStream = dataStream;
        mPredefinedServiceName = serviceName;
    }

    public void getRemoteConfig(
            @NonNull String configServerUrl,
            @NonNull String user,
            @NonNull String secretKey) {
        mConfigServerUrl = configServerUrl;
        execGetAuthToken(user, secretKey);
    }

    private void execGetAuthToken(@NonNull String user,
                                  @NonNull String secretKey) {
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
                                //Log.d(TAG, "accessToken: " + accessToken);
                                mAccessToken = accessToken;
                                execGetConfigList();
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                Log.e(TAG, "Cannot get AuthToken");
                                mListener.onError(description);
                            }
                        });

        /* Relay developer option */
        getAuthToken.enableDebug(mDebugEnabled);

        getAuthToken.run(user, secretKey);
    }

    private void execGetConfigList() {
        GetConfigList getConfigList =
                new GetConfigList(
                        mContext,
                        mConfigServerUrl,
                        new GetConfigList.GetConfigListListener() {
                            @Override
                            public void onDataStreamNames(
                                    @NonNull String[] dataStreamNames) {
                                handleDataStreamNames(dataStreamNames);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                Log.e(TAG, "Cannot get data stream names");
                                mListener.onError(description);
                            }
                        });

        /* Relay developer option */
        getConfigList.enableDebug(mDebugEnabled);

        getConfigList.run(mAccessToken);
    }

    private void handleDataStreamNames(@NonNull String[] dataStreamNames) {
        if (mDebugEnabled) {
            for (int i = 0, n = dataStreamNames.length; i < n; i++) {
                String name = dataStreamNames[i];
                Log.d(TAG, "DataStream[" + (i + 1) + "/" + n + "]: " + name);
            }
        }

        if (mPredefinedDataStream != null) {
            for (String name : dataStreamNames) {
                if (name.contentEquals(mPredefinedDataStream)) {
                    execGetConfig(name);
                    return;
                }
            }
            Log.w(TAG, "Predefined DataStream not found: " + mPredefinedDataStream);
            /* Try with user intervention via dialog... */
        }

        if (dataStreamNames.length > 1) {
            chosenItemIndex = 0;
            String title = ((mDialogLabel != null) ?
                    ("[" + mDialogLabel + "] ") : "") +
                    "Config: DataStreamNames";
            DialogUtil dialogUtil = new DialogUtil(mContext);
            dialogUtil.showSingleChoiceDialog(
                    title,
                    dataStreamNames,
                    chosenItemIndex,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            chosenItemIndex = which;
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String dataStreamName = dataStreamNames[chosenItemIndex];
                            execGetConfig(dataStreamName);
                        }
                    });
        } else {
            String dataStreamName = dataStreamNames[0];
            execGetConfig(dataStreamName);
        }
    }

    private void execGetConfig(@NonNull String dataStreamName) {
        GetConfig getConfig =
                new GetConfig(
                        mContext,
                        mConfigServerUrl,
                        new GetConfig.GetConfigListener() {
                            @Override
                            public void onConfigInfo(
                                    @NonNull String name,
                                    @NonNull JSONObject config1,
                                    @NonNull JSONArray attachments,
                                    @NonNull JSONArray secrets) {
                                JSONObject header = null;
                                JSONObject config2 = config1;

                                /*
                                 * Depending on the configuration format version,
                                 * given "config" may have sub structure with optional "header"
                                 * and mandatory "config" as follows.
                                 *
                                 * < Config version 2+ >
                                 * "config": { "header": { ... }, "config": { ... } }
                                 *
                                 * < Config version 1 >
                                 * "config": { ... }
                                 */
                                if (config1.has(JsonTags.KEY_CONFIG_HEADER)) {
                                    /* Config version 2+ */
                                    header = extractSubHeader(name, config1);
                                    if (header == null) {
                                        return;
                                    }
                                    config2 = extractSubConfig(name, config1);
                                    if (config2 == null) {
                                        return;
                                    }
                                }

                                /* This "config" object may have multiple services */
                                handleConfigInfo(
                                        name, header, config2, attachments, secrets);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                Log.e(TAG, "Cannot get config info");
                                mListener.onError(description);
                            }
                        });

        /* Relay developer option */
        getConfig.enableDebug(mDebugEnabled);

        getConfig.run(dataStreamName, mAccessToken);
    }

    @Nullable
    private JSONObject extractSubHeader(@NonNull String name,
                                        @NonNull JSONObject parent) {
        JSONObject header = null;
        String key = JsonTags.KEY_CONFIG_HEADER;
        if (parent.has(key)) {
            try {
                header = parent.getJSONObject(key);
            } catch (JSONException e) {
                mListener.onError(TAG + ": Invalid Config(" + name + "):\n" +
                        ": " + key + ": " + e.getMessage());
                return null;
            }

            String version;
            key = JsonTags.KEY_CONFIG_HEADER_VERSION;
            if (header.has(key)) {
                try {
                    version = header.getString(key);
                } catch (JSONException e) {
                    mListener.onError(TAG + ": Invalid Config(" + name + "):\n" +
                            "config.version: " + e.getMessage());
                    return null;
                }

                if (! version.equals("2")) {
                    mListener.onError(TAG + ": Invalid Config(" + name + "):\n" +
                            ": Unknown config.version: " + version);
                    return null;
                }
            } else {
                mListener.onError(TAG + ": Invalid Config(" + name + "):\n" +
                        ": config.version is missing.");
                return null;
            }
        }
        return header;
    }

    @Nullable
    private JSONObject extractSubConfig(@NonNull String name,
                                        @NonNull JSONObject parent) {
        JSONObject config;
        String key = JsonTags.KEY_CONFIG_CONFIG;
        if (parent.has(key)) {
            try {
                config = parent.getJSONObject(key);
            } catch (JSONException e) {
                mListener.onError(TAG + ": Invalid Config(" + name + "):\n" +
                        "config.config: " + e.getMessage());
                return null;
            }
        } else {
            mListener.onError(TAG + ": Invalid Config(" + name + "):\n" +
                    "config.config not found.");
            return null;
        }
        return config;
    }

    private boolean embedCertificate(@NonNull String target,
                                     @NonNull byte[] certificate,
                                     @NonNull JSONArray attachments) {
        /*
         * Here is a tricky part.
         *
         * If the SSL/TLS certificate has registered on the configuration server
         * as an "non-encrypted" data, it comes as an element of the attachments.
         * Now that the given certificate has extracted from secrets, we embed it
         * to the attachments so that the certificate to be processed in
         * common way.
         */
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(JsonTags.KEY_ATTACHMENT_TARGET, target);
            jsonObject.put(JsonTags.KEY_ATTACHMENT_VALUE,
                    Base64.encodeToString(certificate, Base64.NO_WRAP));
        } catch (JSONException e) {
            mListener.onError(TAG + ": JSONObject.put(): " + e.getMessage());
            return false;
        }
        attachments.put(jsonObject);

        return true; /* Success */
    }

    private void handleConfigInfo(@NonNull String name,
                                  @Nullable JSONObject header,
                                  @NonNull JSONObject config,
                                  @NonNull JSONArray attachments,
                                  @Nullable JSONArray secrets) {
        /* Preprocess secrets, if any */
        if (secrets != null && secrets.length() > 0) {
            SecretHandler secretHandler =
                    new SecretHandler(
                            mContext,
                            mConfigServerUrl,
                            mAccessToken,
                            new SecretHandler.SecretHandlerListener() {
                                @Override
                                public void onReady(@NonNull SecretHandler secretHandler) {
                                    secretHandler.handleConfigSecrets(secrets);
                                }

                                @Override
                                public void onClientCertificate(@NonNull String target,
                                                                @NonNull byte[] certificate) {
                                    /*
                                     * Now that the SSL/TLS client certificate has extracted
                                     * from secrets, embed it in the attachments.
                                     */
                                    if (!embedCertificate(target, certificate, attachments)) {
                                        return;
                                    }

                                    /* Recursive call! */
                                    handleConfigInfo(
                                            name,
                                            header,
                                            config,
                                            attachments,
                                            null /* SENTINEL */
                                    );
                                }

                                @Override
                                public void onError(
                                        @NonNull String description) {
                                    /* Relay error info to the listener */
                                    mListener.onError(description);
                                }
                            });

            /* Relay developer option */
            secretHandler.enableDebug(mDebugEnabled);

            secretHandler.run(mDialogLabel);
            return;
        }

        /*
         * Top layer of the "config" object is reserved for service names.
         * If the given data stream has multiple services, ask user
         * to choose the target service.
         */
        Iterator<String> keys = config.keys();
        List<String> nameList = new ArrayList<>();

        while (keys.hasNext()) {
            String key = keys.next();
            nameList.add(key);
        }
        if (nameList.size() > 1) {
            if (mPredefinedServiceName != null) {
                for (String service : nameList) {
                    if (service.contentEquals(mPredefinedServiceName)) {
                        /* Service matched, notify now */
                        notifyConfigInfo(name, service,
                                header, config, attachments, secrets);
                        return;
                    }
                }
                Log.w(TAG, "Predefined ServiceName not found: " + mPredefinedServiceName);
                /* Try with user intervention via dialog... */
            }

            String[] services = nameList.toArray(new String[0]);
            chosenItemIndex = 0;
            String title = ((mDialogLabel != null) ?
                    ("[" + mDialogLabel + "] ") : "") +
                    "Config(" + name + "): Services";

            DialogUtil dialogUtil = new DialogUtil(mContext);
            dialogUtil.showSingleChoiceDialog(
                    title,
                    services,
                    chosenItemIndex,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            chosenItemIndex = which;
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String service = services[chosenItemIndex];
                            notifyConfigInfo(name, service,
                                    header, config, attachments, secrets);
                        }
                    });
        } else {
            /* Single service, notify now */
            String service = nameList.get(0);
            notifyConfigInfo(name, service,
                    header, config, attachments, secrets);
        }
    }

    private void notifyConfigInfo(@NonNull String name,
                                  @NonNull String service,
                                  @Nullable JSONObject header,
                                  @NonNull JSONObject config,
                                  @NonNull JSONArray attachments,
                                  @Nullable JSONArray secrets) {
        try {
            JSONObject config2 = config.getJSONObject(service);
            mListener.onConfigInfo(
                    name, service,
                    header, config2, attachments, secrets);
        } catch (JSONException e) {
            String errmsg = TAG + ":\n";
            errmsg += "Config(" + name + "):\n";
            errmsg += "Service(" + service + "):\n";
            errmsg += e.getMessage();

            mListener.onError(errmsg);
        }
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface ConfigClientListener {
        void onConfigInfo(@NonNull String name,
                          @NonNull String service,
                          @Nullable JSONObject header,
                          @NonNull JSONObject config,
                          @NonNull JSONArray attachments,
                          @Nullable JSONArray secrets);

        void onError(@NonNull String description);
    }
}
