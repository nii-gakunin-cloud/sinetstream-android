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

import jp.ad.sinet.stream.android.api.Consistency;
import jp.ad.sinet.stream.android.api.InvalidConfigurationException;
import jp.ad.sinet.stream.android.api.ValueType;

public class ConfigParser {
    private final CommonParser mCommonParser = new CommonParser();
    private final ApiParser mApiParser = new ApiParser();
    private final UserPasswordParser mUserPasswordParser = new UserPasswordParser();
    private final TlsParser mTlsParser = new TlsParser();
    private final MqttParser mMqttParser = new MqttParser();
    private final MqttTlsParser mMqttTlsParser = new MqttTlsParser();
    private final CryptoParser mCryptoParser = new CryptoParser();

    /*
     * Link to sub parsers for referring parsed values.
     */
    public final CommonParser getCommonParser() {
        return mCommonParser;
    }

    public final ApiParser getApiParser() {
        return mApiParser;
    }

    public final UserPasswordParser getUserPasswordParser() {
        return mUserPasswordParser;
    }

    public final TlsParser getTlsParser() {
        return mTlsParser;
    }

    public final MqttParser getMqttParser() {
        return mMqttParser;
    }

    public final MqttTlsParser getMqttTlsParser() {
        return mMqttTlsParser;
    }

    public final CryptoParser getCryptoParser() {
        return mCryptoParser;
    }

    /*
     * Global control parameters
     */
    private boolean mMqttEnabled = false;
    private boolean mTlsEnabled = false;
    private boolean mCryptoEnabled = false;

    public final boolean getTlsEnabled() {
        return mTlsEnabled;
    }

    public final boolean getCryptoEnabled() {
        return mCryptoEnabled;
    }

    /*
     * Entry point of the parser tree
     */
    public void parse(@NonNull Map<String,Object> configParameters,
                      @Nullable Map<String,Object> configAttachments)
            throws InvalidConfigurationException {
        /* Common */
        mCommonParser.parse(configParameters);
        String type = mCommonParser.getMessagingSystemType(); // NB: might be null
        if (type != null && type.equalsIgnoreCase("mqtt")) {
            mMqttEnabled = true;
        }

        /* Api */
        mApiParser.parse(configParameters);

        /* UserName & Password */
        mUserPasswordParser.parse(configParameters);

        /* System-wide TLS parameters */
        mTlsParser.parse(configParameters, configAttachments);
        Boolean tlsEnabled = mTlsParser.isTlsEnabled(); // NB: might be null
        if (tlsEnabled != null) {
            mTlsEnabled = Boolean.TRUE.equals(tlsEnabled);
        }

        /* MQTT parameters */
        if (mMqttEnabled) {
            mMqttParser.parse(configParameters);

            if (mTlsEnabled) {
                /* MQTT-specific TLS parameters */
                mMqttTlsParser.parse(configParameters);
            }
        }

        /* Crypto */
        if (hasDataEncryption()) {
            mCryptoParser.parse(configParameters);
            mCryptoEnabled = hasCrypto();

            if (!mCryptoEnabled) {
                throw new InvalidConfigurationException(
                        "DataEncryption has set, but missing crypto", null);
            }
        }
    }

    @Nullable
    public String getClientId() {
        return mApiParser.getClientId();
    }

    public String[] getTopics() {
        String[] topics = null;
        if (mApiParser.getTopics() != null) {
            topics = mApiParser.getTopics();
        } else if (mApiParser.getTopic() != null) {
            topics = new String[]{mApiParser.getTopic()};
        }
        return topics;
    }

    public int getQos() {
        int qos = (Consistency.AT_LEAST_ONCE).getQos();
        Integer parsedValue;
        if ((parsedValue = mApiParser.getConsistency()) != null) {
            qos = parsedValue;
        }
        if (mMqttEnabled) {
            if ((parsedValue = mMqttParser.getQos()) != null) {
                qos = parsedValue;
            }
        }
        return qos;
    }

    public ValueType getValueType() {
        ValueType valueType = ValueType.BYTE_ARRAY;
        ValueType probe = mApiParser.getValueType();
        if (probe != null) {
            valueType = probe;
        }
        return valueType;
    }

    public String getSSLContextProtocol() {
        return mTlsParser.getProtocol();
    }

    public boolean useTlsServerCertificate() {
        boolean enabled = false;
        Boolean parsedValue;
        if ((parsedValue =
                mTlsParser.getSelfSignedServerCertificateEnabled()) != null) {
            enabled = parsedValue;
        }
        return enabled;
    }

    @Nullable
    public String getClientCertificateAlias() {
        return mTlsParser.getClientCertificateAlias(); // NB: might be null
    }

    public boolean getRetain() {
        boolean retain = false;
        Boolean parsedValue;
        if ((parsedValue = mMqttParser.getRetain()) != null) {
            retain = parsedValue;
        }
        return retain;
    }

    public boolean mqttDebugEnabled() {
        boolean enabled = false;
        Boolean parsedValue;
        if ((parsedValue = mMqttParser.getMqttDebug()) != null) {
            enabled = parsedValue;
        }
        return enabled;
    }

    private boolean hasDataEncryption() {
        boolean enabled = false;
        Boolean parsedValue;
        if ((parsedValue = mApiParser.getDataEncryption()) != null) {
            enabled = parsedValue;
        }
        return enabled;
    }

    private boolean hasCrypto() {
        boolean enabled = false;
        Boolean parsedValue;
        if ((parsedValue = mCryptoParser.hasCrypto()) != null) {
            enabled = parsedValue;
        }
        return enabled;
    }

    public String getCryptoPassword() {
        String parsedValue = mCryptoParser.getPassword();
        return ((parsedValue != null) ? parsedValue : "");
    }

    public String getCryptoAlgorithm() {
        String parsedValue = mCryptoParser.getAlgorithm();
        return ((parsedValue != null) ? parsedValue : "");
    }

    public String getFeedbackMode() {
        String parsedValue = mCryptoParser.getMode();
        return ((parsedValue != null) ? parsedValue : "");
    }

    public String getPaddingScheme() {
        String parsedValue = mCryptoParser.getPadding();
        return ((parsedValue != null) ? parsedValue : "");
    }

    public int getKeyLength() {
        Integer parsedValue = mCryptoParser.getKeyLength();
        return ((parsedValue != null) ? parsedValue : 0);
    }

    public String getKeyDerivationAlgorithm() {
        String parsedValue = mCryptoParser.getKeyDerivationAlgorithm();
        return ((parsedValue != null) ? parsedValue : "");
    }

    public int getSaltBytes() {
        Integer parsedValue = mCryptoParser.getSaltBytes();
        return ((parsedValue != null) ? parsedValue : 0);
    }

    public int getIterationCount() {
        Integer parsedValue = mCryptoParser.getIteration();
        return ((parsedValue != null) ? parsedValue : 0);
    }

    public boolean getCryptoDebugEnabled() {
        Boolean parsedValue = mCryptoParser.getCryptoDebug();
        return ((parsedValue != null) ? parsedValue : false);
    }
}
