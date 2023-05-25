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
import android.util.Log;

import androidx.annotation.NonNull;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.Base64;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import jp.ad.sinet.stream.android.config.parser.ConfigParser;
import jp.ad.sinet.stream.android.config.parser.MqttParser;
import jp.ad.sinet.stream.android.config.parser.MqttTlsParser;
import jp.ad.sinet.stream.android.config.parser.TlsParser;
import jp.ad.sinet.stream.android.config.parser.UserPasswordParser;
import jp.ad.sinet.stream.android.net.cert.KeyChainParser;
import jp.ad.sinet.stream.android.net.cert.PrivateCertHandler;

/*
 * This class allocates a MqttConnectOptions and sets up its contents
 * according to the SINETStream configuration file.
 *
 * https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html
 */
public class MqttConnectOptionBuilder {
    private final String TAG = MqttConnectOptionBuilder.class.getSimpleName();

    private final Context mContext;
    private final ConfigParser mConfigParser;
    private final String[] mServerUris;
    private final UserPasswordParser mUserPasswordParser;
    private final MqttParser mMqttParser;
    private final TlsParser mTlsParser;
    private final MqttTlsParser mMqttTlsParser;
    private Thread mThread = null;

    private final MqttConnectOptionBuilderListener mListener;

    public MqttConnectOptionBuilder(
            @NonNull Context context,
            @NonNull MqttConnectOptionBuilderListener listener,
            @NonNull ConfigParser configParser,
            @NonNull String[] serverUris) {
        this.mContext = context;
        this.mListener = listener;
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
    public void buildMqttConnectOptions() {
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
            } else {
                mListener.onFinished(mqttConnectOptions);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            mListener.onError("MqttConnectOptions: " + e.getMessage());
        }
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
        setHttpsHostnameVerificationEnabled(mqttConnectOptions);
        if (mTlsParser.isBuildSslContextByDataSets()) {
            setSslSocketFromDataSets(mqttConnectOptions);

            /* OK, all setup has done */
            mListener.onFinished(mqttConnectOptions);
        } else {
            setSslSocketFromKeyChain(mqttConnectOptions);
        }
    }

    /* OBSOLETED
    private void setSslSocketFromFile(MqttConnectOptions mqttConnectOptions) {
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
     */

    private void setSslSocketFromDataSets(MqttConnectOptions mqttConnectOptions) {
        String probe;
        String serverCertificate = null;
        byte[] clientCertificate = null;
        char[] clientPassword = null;

        probe = mTlsParser.getSelfSignedCertificateData();
        if (probe != null) {
            serverCertificate = new String(Base64.getDecoder().decode(probe));
        }
        probe = mTlsParser.getClientCertificateData();
        if (probe != null) {
            clientCertificate = Base64.getDecoder().decode(probe);

            probe = mTlsParser.getClientCertificatePassword();
            if (probe != null) {
                clientPassword = probe.toCharArray();
            }
        }

        if (serverCertificate != null || clientCertificate != null) {
            if (mDebugEnabled) {
                Log.d(TAG, "Going to build SSLContext from datasets");
            }
            PrivateCertHandler handler = new PrivateCertHandler(mContext);

            String protocolVersion = mConfigParser.getSSLContextProtocol();
            handler.setProtocolVersion(protocolVersion);

            SSLContext sslContext = handler.buildSslContextFromDataSets(
                    serverCertificate, clientCertificate, clientPassword);

            if (sslContext != null) {
                mqttConnectOptions.setSocketFactory(sslContext.getSocketFactory());
            }
        }
    }

    private void setSslSocketFromKeyChain(MqttConnectOptions mqttConnectOptions) {
        String protocol =
                mConfigParser.getSSLContextProtocol();
        String alias =
                mConfigParser.getClientCertificateAlias(); // can be null
        boolean serverCert =
                mConfigParser.useTlsServerCertificate();

        if (mThread != null) {
            if (mThread.isAlive()) {
                Log.w(TAG, "KeyChainParser thread is still running!");
            }
        } else {
            if (mDebugEnabled) {
                Log.d(TAG, "Going to run KeyChainParser thread");
            }
            mThread = new Thread(new KeyChainParser(
                    mContext,
                    new KeyChainParser.KeyChainParserListener() {
                        @Override
                        public void onError(@NonNull String description) {
                            Log.e(TAG, "KeyChainParser: " + description);
                            mListener.onError(description);
                        }

                        @Override
                        public void onParsed(@NonNull SSLContext sslContext) {
                            if (mDebugEnabled) {
                                Log.d(TAG, "onParsed: " + sslContext);
                            }
                            try {
                                SSLSocketFactory sslSocketFactory =
                                        sslContext.getSocketFactory();
                                mqttConnectOptions.setSocketFactory(sslSocketFactory);

                                /* OK, all setup has done */
                                mListener.onFinished(mqttConnectOptions);
                            } catch (IllegalStateException e) {
                                mListener.onError("SSLContext.getSocketFactory(): " + e);
                            }
                        }
                    },
                    protocol,
                    alias,
                    serverCert
            ));
            mThread.start();
        }
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

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface MqttConnectOptionBuilderListener {
        void onError(@NonNull String description);
        void onFinished(@NonNull MqttConnectOptions mqttConnectOptions);
    }
}
