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

package jp.ad.sinet.stream.android.mqtt;

import android.content.Context;

import androidx.annotation.NonNull;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

import jp.ad.sinet.stream.android.api.InvalidConfigurationException;
import jp.ad.sinet.stream.android.config.ConfigParser;
import jp.ad.sinet.stream.android.config.MqttParser;
import jp.ad.sinet.stream.android.config.MqttTlsParser;
import jp.ad.sinet.stream.android.config.TlsParser;
import jp.ad.sinet.stream.android.config.UserPasswordParser;
import jp.ad.sinet.stream.android.net.cert.TlsUtils;

/*
 * This class allocates a MqttConnectOptions and sets up its contents
 * according to the SINETStream configuration file.
 *
 * https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html
 */
public class MqttConnectOptionBuilder {
    private final Context mContext;
    private final ConfigParser mConfigParser;
    private final String[] mServerUris;
    private final UserPasswordParser mUserPasswordParser;
    private final MqttParser mMqttParser;
    private final TlsParser mTlsParser;
    private final MqttTlsParser mMqttTlsParser;

    public MqttConnectOptionBuilder(
            Context context,
            @NonNull ConfigParser configParser,
            @NonNull String[] serverUris) {
        assert(context != null);

        this.mContext = context;
        this.mConfigParser = configParser;
        this.mServerUris = serverUris;

        this.mUserPasswordParser = configParser.getUserPasswordParser();
        this.mMqttParser = configParser.getMqttParser();
        this.mTlsParser = configParser.getTlsParser();
        this.mMqttTlsParser = configParser.getMqttTlsParser();
    }

    /*
     * Entry point
     */
    public MqttConnectOptions buildMqttConnectOptions()
            throws InvalidConfigurationException {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        try {
            setServerUris(mqttConnectOptions);
            setUserPassword(mqttConnectOptions);
            setMqttParams(mqttConnectOptions);
            setLwtParams(mqttConnectOptions);

            if (mConfigParser.getTlsEnabled()) {
                /*
                 * We put higher precedence on Mqtt-specific TLS parameters than
                 * system-wide TLS parameters.
                 */
                setTlsParams(mqttConnectOptions);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidConfigurationException(e.getMessage(), e);
        }
        return mqttConnectOptions;
    }

    private void setServerUris(MqttConnectOptions mqttConnectOptions) {
        /*
         * If we are going to connect to the single server, then set its
         * URI as the 2nd argument of the MqttAndroidClient constructor.
         * Instead, if we target multiple Servers at the same time, set
         * the array of Server URIs as a MqttConnectOptions parameter.
         */
        if (mServerUris.length > 1) {
            mqttConnectOptions.setServerURIs(mServerUris);
        }
    }

    private void setUserPassword(MqttConnectOptions mqttConnectOptions) {
        String userName = mUserPasswordParser.getUserName();
        String password = mUserPasswordParser.getPassword();

        if (userName != null && password != null) {
            mqttConnectOptions.setUserName(userName);
            mqttConnectOptions.setPassword(password.toCharArray());
        }
    }

    private void setMqttParams(MqttConnectOptions mqttConnectOptions) {
        Boolean cleanSession = mMqttParser.getCleanSession();
        if (cleanSession != null) {
            mqttConnectOptions.setCleanSession(
                    Boolean.TRUE.equals(cleanSession)
            );
        }

        Integer inflight = mMqttParser.getInflight();
        if (inflight != null) {
            mqttConnectOptions.setMaxInflight(inflight);
        }

        Properties customWebSocketHeaders = mMqttParser.getWebSocketCustomHeaders();
        if (customWebSocketHeaders != null) {
            mqttConnectOptions.setCustomWebSocketHeaders(customWebSocketHeaders);
        }

        Integer keepAliveInterval = mMqttParser.getKeepAliveInterval();
        if (keepAliveInterval != null) {
            mqttConnectOptions.setKeepAliveInterval(keepAliveInterval);
        }

        Integer mqttVersion = mMqttParser.getMqttVersion();
        if (mqttVersion != null) {
            mqttConnectOptions.setMqttVersion(mqttVersion);
        }

        Boolean automaticReconnect = mMqttParser.getAutomaticReconnect();
        if (automaticReconnect != null) {
            mqttConnectOptions.setAutomaticReconnect(
                    Boolean.TRUE.equals(automaticReconnect)
            );
        }

        Integer connectionTimeout = mMqttParser.getConnectionTimeout();
        if (connectionTimeout != null) {
            mqttConnectOptions.setConnectionTimeout(connectionTimeout);
        }

        Integer maxReconnectDelay = mMqttParser.getMaxReconnectDelay();
        if (maxReconnectDelay != null) {
            mqttConnectOptions.setMaxReconnectDelay(maxReconnectDelay);
        }
    }

    private void setLwtParams(MqttConnectOptions mqttConnectOption) {
        Boolean mLwtSet = mMqttParser.hasLwtSet();
        if (Boolean.TRUE.equals(mLwtSet)) {
            String topic = mMqttParser.getLwtTopic();
            String payload = mMqttParser.getLwtPayload();
            Integer qos = mMqttParser.getLwtQos();
            Boolean retain = mMqttParser.getLwtRetain();

            if (topic != null && payload != null && qos != null && retain != null) {
                mqttConnectOption.setWill(topic, payload.getBytes(), qos, retain);
            }
        }
    }

    private void setTlsParams(MqttConnectOptions mqttConnectOptions) {
        setSslSocket(mqttConnectOptions);
        setHttpsHostnameVerificationEnabled(mqttConnectOptions);
    }

    private void setSslSocket(MqttConnectOptions mqttConnectOptions) {
        String selfSignedCertificate = null;
        String clientCertificate = null;
        String clientCertificatePassword = null;

        if (mMqttTlsParser.getSelfSignedCertificateFile() != null ||
                mMqttTlsParser.getClientCertificateFile() != null) {
            selfSignedCertificate =
                    mMqttTlsParser.getSelfSignedCertificateFile();
            clientCertificate =
                    mMqttTlsParser.getClientCertificateFile();
            clientCertificatePassword =
                    mMqttTlsParser.getClientCertificatePassword();
        } else
        if (mTlsParser.getSelfSignedCertificateFile() != null ||
                mTlsParser.getClientCertificateFile() != null) {
            selfSignedCertificate =
                    mTlsParser.getSelfSignedCertificateFile();
            clientCertificate =
                    mTlsParser.getClientCertificateFile();
            clientCertificatePassword =
                    mTlsParser.getClientCertificatePassword();
        }

        TlsUtils tlsUtils = new TlsUtils(
                selfSignedCertificate,
                clientCertificate,
                (clientCertificatePassword != null) ?
                        clientCertificatePassword.toCharArray() : null
        );

        SSLSocketFactory sslSocketFactory =
                tlsUtils.buildSslSocketFactory(mContext);

        mqttConnectOptions.setSocketFactory(sslSocketFactory);
    }

    private void setHttpsHostnameVerificationEnabled(
            MqttConnectOptions mqttConnectOptions) {
        Boolean httpsHostnameVerificationEnabled;
        if (((httpsHostnameVerificationEnabled =
                mMqttTlsParser.getHttpsHostnameVerificationEnabled()) != null) ||
                ((httpsHostnameVerificationEnabled =
                        mTlsParser.getHttpsHostnameVerificationEnabled()) != null)) {
            mqttConnectOptions.setHttpsHostnameVerificationEnabled(
                    Boolean.TRUE.equals(httpsHostnameVerificationEnabled)
            );
        }
    }
}
