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

package jp.ad.sinet.stream.android.config.parser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import jp.ad.sinet.stream.android.api.InvalidConfigurationException;

public class MqttTlsParser extends BaseParser {

    /* Entry point */
    public void parse(@NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        /* OBSOLETED
        parseMqttTlsSet(configParameters);
        parseMqttTlsInsecureSet(configParameters);
         */
    }

    private Boolean mMqttTlsSet = null;
    private void parseMqttTlsSet(@NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = "tls_set"; /* Optional */
        Map<String,Object> parent = super.parseMap(configParameters, key, false);
        if (parent != null) {
            mMqttTlsSet = true;

            setSelfSignedCertificateFile(parent);
            setClientCertificateFile(parent);
            setClientCertificatePassword(parent);
            setHttpsHostnameVerificationEnabled(parent);

            if (mClientCertificateFile != null
                    && mClientCertificatePassword == null) {
                throw new InvalidConfigurationException(
                        "Missing password for the TLS client certificate", null
                );
            }
        }
    }

    @Nullable
    public final Boolean hasMqttTlsSet() {
        return mMqttTlsSet;
    }

    private String mSelfSignedCertificateFile = null; /* PEM format file (xxx.crt) */
    private void setSelfSignedCertificateFile(
            @NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = "ca_certs"; /* Optional */
        mSelfSignedCertificateFile
                = super.parseString(configParameters, key, false);
    }

    @Nullable
    public final String getSelfSignedCertificateFile() {
        return mSelfSignedCertificateFile;
    }

    private String mClientCertificateFile = null;  /* PKCS#12/PFX format file (xxx.pfx) */
    private void setClientCertificateFile(
            @NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = "certfile"; /* Optional */
        mClientCertificateFile
                = super.parseString(configParameters, key, false);
    }

    @Nullable
    public final String getClientCertificateFile() {
        return mClientCertificateFile;
    }

    private String mClientCertificatePassword = null;
    private void setClientCertificatePassword(
            @NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = "keyfilePassword"; /* Optional */
        mClientCertificatePassword
                = super.parseString(configParameters, key, false);
    }

    @Nullable
    public final String getClientCertificatePassword() {
        return mClientCertificatePassword;
    }

    private Boolean mMqttTlsInsecureSet = null;
    private void parseMqttTlsInsecureSet(@NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = "tls_insecure_set"; /* Optional */
        Map<String,Object> parent = super.parseMap(configParameters, key, false);
        if (parent != null) {
            mMqttTlsInsecureSet = true;
            setHttpsHostnameVerificationEnabled(parent);
        }
    }

    @Nullable
    public final Boolean hasMqttTlsInsecureSet() {
        return mMqttTlsInsecureSet;
    }

    private Boolean mHttpsHostnameVerificationEnabled = null;
    private void setHttpsHostnameVerificationEnabled(
            @NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = "value"; /* Optional */
        mHttpsHostnameVerificationEnabled =
                super.parseBoolean(configParameters, key, false);
    }

    @Nullable
    public Boolean getHttpsHostnameVerificationEnabled() {
        return mHttpsHostnameVerificationEnabled;
    }
}
