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
}
