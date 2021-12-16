/*
 * Copyright (C) 2020 National Institute of Informatics
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

package jp.ad.sinet.stream.android.config;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import jp.ad.sinet.stream.android.AndroidConfigLoader;
import jp.ad.sinet.stream.android.api.InvalidConfigurationException;

public class TlsParser extends BaseParser {

    /* Entry point */
    public void parse(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        parseTls(myParams);
        parseExtraTls(myParams);
    }

    private Boolean mTlsEnabled = null;
    private void parseTls(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = AndroidConfigLoader.KEY_TLS; /* Optional */
        if (myParams.get(key) != null) {
            Object obj = myParams.get(key);
            /*
             * Keyword tls can have several forms.
             * In Android version, we accept boolean cases, and don't
             * interpret in other forms except tls is enabled.
             */
            if (obj instanceof Boolean) {
                /* We accept boolean format */
                mTlsEnabled = (boolean)obj;
            } else if (obj instanceof Map){
                /* We also accept Map format */
                mTlsEnabled = true;

                @SuppressWarnings("unchecked")
                Map<String,Object> parent = (Map<String,Object>) obj;
                parseTlsParameters(parent);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            }
            mTlsEnabled = false;
        }
    }

    @Nullable
    public Boolean isTlsEnabled() {
        return mTlsEnabled;
    }

    private void parseTlsParameters(@NonNull Map<String,Object> myParams) {
        /* OBSOLETED
        setSelfSignedCertificateFile(myParams);
        setClientCertificateFile(myParams);
        setClientCertificatePassword(myParams);
        setHttpsHostnameVerificationEnabled(myParams);

        if (mClientCertificateFile != null
                && mClientCertificatePassword == null) {
            throw new InvalidConfigurationException(
                    "Missing password for the TLS client certificate", null
            );
        }
         */
        parseProtocol(myParams);
        setSelfSignedServerCertificateEnabled(myParams);
        setClientCertificateEnabled(myParams);
        setHttpsHostnameVerificationEnabled(myParams);
    }

    private String mProtocol = null;
    private void parseProtocol(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "protocol"; /* Optional */
        String parsedValue = super.parseString(myParams, key, false);
        if (parsedValue != null) {
            switch (parsedValue) {
                case "TLSv1":
                case "TLSv1.1":
                case "TLSv1.2":
                    mProtocol = parsedValue;
                    break;
                case "TLSv1.3":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mProtocol = parsedValue;
                    } else {
                        throw new InvalidConfigurationException(
                                key + "(" + parsedValue +
                                        "): Not supported on this Android version", null);
                    }
                    break;
                default:
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Unknown value", null);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mProtocol = "TLSv1.3";
            } else {
                mProtocol = "TLSv1.2";
            }
        }
    }

    public final String getProtocol() {
        return mProtocol;
    }

    private String mSelfSignedCertificateFile = null; /* PEM format file (xxx.crt) */
    private void setSelfSignedCertificateFile(
            @NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "ca_certs"; /* Optional */
        mSelfSignedCertificateFile
                = super.parseString(myParams, key, false);
    }

    @Nullable
    public final String getSelfSignedCertificateFile() {
        return mSelfSignedCertificateFile;
    }

    private String mClientCertificateFile = null;  /* PKCS#12/PFX format file (xxx.pfx) */
    private void setClientCertificateFile(
            @NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "certfile"; /* Optional */
        mClientCertificateFile
                = super.parseString(myParams, key, false);
    }

    @Nullable
    public final String getClientCertificateFile() {
        return mClientCertificateFile;
    }

    private String mClientCertificatePassword = null;
    private void setClientCertificatePassword(
            @NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "keyfilePassword"; /* Optional */
        mClientCertificatePassword
                = super.parseString(myParams, key, false);
    }

    @Nullable
    public final String getClientCertificatePassword() {
        return mClientCertificatePassword;
    }

    private Boolean mSelfSignedServerCertificateEnabled = null;
    private void setSelfSignedServerCertificateEnabled(
            @NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = YamlTags.KEY_SERVER_CERTS; /* Optional */
        mSelfSignedServerCertificateEnabled =
                super.parseBoolean(myParams, key, false);
    }

    @Nullable
    public Boolean getSelfSignedServerCertificateEnabled() {
        return mSelfSignedServerCertificateEnabled;
    }

    private Boolean mClientCertificateEnabled = null;
    private void setClientCertificateEnabled(
            @NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = YamlTags.KEY_CLIENT_CERTS; /* Optional */
        mClientCertificateEnabled =
                super.parseBoolean(myParams, key, false);
    }

    @Nullable
    public Boolean getClientCertificateEnabled() {
        return mClientCertificateEnabled;
    }

    private Boolean mHttpsHostnameVerificationEnabled = null;
    private void setHttpsHostnameVerificationEnabled(
            @NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "check_hostname"; /* Optional */
        mHttpsHostnameVerificationEnabled =
                super.parseBoolean(myParams, key, false);
    }

    @Nullable
    public Boolean getHttpsHostnameVerificationEnabled() {
        return mHttpsHostnameVerificationEnabled;
    }

    private void parseExtraTls(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        setClientCertificateAlias(myParams);
    }

    private String mClientCertificateAlias = null;
    private void setClientCertificateAlias(
            @NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = YamlTags.KEY_EXTRA_ALIAS; /* Optional */
        mClientCertificateAlias
                = super.parseString(myParams, key, false);
    }

    @Nullable
    public final String getClientCertificateAlias() {
        return mClientCertificateAlias;
    }
}
