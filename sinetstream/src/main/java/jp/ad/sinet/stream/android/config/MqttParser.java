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

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.Map;
import java.util.Properties;

import jp.ad.sinet.stream.android.api.Consistency;
import jp.ad.sinet.stream.android.api.InvalidConfigurationException;
import jp.ad.sinet.stream.android.mqtt.MqttAsyncMessageIOKt;

public class MqttParser extends BaseParser {

    /* Entry point */
    public void parse(@NonNull Map<String,Object> myParams) {
        parseCleanSession(myParams);
        parseMqttVersion(myParams);
        parseTransport(myParams);
        parseQos(myParams);
        parseRetain(myParams);
        parseMaxInflightMessagesSet(myParams);
        parseLwtSet(myParams);
        parseMqttConnect(myParams);
        parseMaxReconnectDelay(myParams);
    }

    private Boolean mCleanSession = null;
    private void parseCleanSession(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "clean_session"; /* Optional */
        mCleanSession = super.parseBoolean(myParams, key, false);
    }

    @Nullable
    public final Boolean getCleanSession() {
        return mCleanSession;
    }

    private Integer mMqttVersion = null;
    private void parseMqttVersion(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "protocol"; /* Optional */
        String parsedValue = super.parseString(myParams, key, false);
        if (parsedValue != null) {
            switch (parsedValue) {
                case "MQTTv31":
                    mMqttVersion = MqttConnectOptions.MQTT_VERSION_3_1;
                    break;
                case "MQTTv311":
                    mMqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1;
                    break;
                case "DEFAULT":
                    mMqttVersion = MqttConnectOptions.MQTT_VERSION_DEFAULT;
                    break;
                default:
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Unknown value", null);
            }
        }
    }

    @Nullable
    public final Integer getMqttVersion() {
        return mMqttVersion;
    }

    private String mTransport = null;
    private void parseTransport(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "transport"; /* Optional */
        String parsedValue = super.parseString(myParams, key, false);
        if (parsedValue != null) {
            switch (parsedValue) {
                case "tcp":
                    mTransport = parsedValue;
                    break;
                case "websocket":
                case "websockets":
                    mTransport = parsedValue;

                    /* Check WebSocket-specific options */
                    parseWebSocketOptionsSet(myParams);
                    break;
                default:
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Unknown value", null);
            }
        }
    }

    @Nullable
    public final String getTransport() {
        return mTransport;
    }

    //private int mQos = (Consistency.AT_LEAST_ONCE).getQos();
    private Integer mQos = null;
    private void parseQos(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "qos"; /* Optional */
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue instanceof Integer) {
            Integer probe = (Integer) parsedValue;
            if (!probe.equals(Consistency.AT_MOST_ONCE.getQos())
                    && !probe.equals(Consistency.AT_LEAST_ONCE.getQos())
                    && !probe.equals(Consistency.EXACTLY_ONCE.getQos())) {
                throw new InvalidConfigurationException(
                        key + "(" + parsedValue + "): Out of range", null);
            }
            mQos = probe;
        }
    }

    @Nullable
    public Integer getQos() {
        return mQos;
    }

    private Boolean mRetain = null;
    private void parseRetain(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "retain"; /* Optional */
        mRetain = super.parseBoolean(myParams, key, false);
    }

    @Nullable
    public final Boolean getRetain() {
        return mRetain;
    }

    private Boolean mMaxInflightMessagesSet = null;
    public void parseMaxInflightMessagesSet(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "max_inflight_messages_set"; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mMaxInflightMessagesSet = true;
            parseInflight(parent);
        }
    }

    @Nullable
    public final Boolean hasMaxInflightMessagesSet() {
        return mMaxInflightMessagesSet;
    }

    //private int mInflight = MqttConnectOptions.MAX_INFLIGHT_DEFAULT;
    private Integer mInflight = null;
    private void parseInflight(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "inflight"; /* Optional */
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue != null) {
            /* Non-empty value has set */
            if (parsedValue instanceof Integer) {
                int ival = (int) parsedValue;
                if (ival < 0) {
                    throw new InvalidConfigurationException(
                            key + ": Out of range (" + parsedValue + ")", null);
                }
                mInflight = ival;
            } else {
                throw new InvalidConfigurationException(
                        key + ": Not an Integer (" + parsedValue + ")", null);
            }
        }
    }

    @Nullable
    public final Integer getInflight() {
        return mInflight;
    }

    /*
     * WebSocket part
     */
    private Boolean mWebsocketOptionsSet = null;
    public void parseWebSocketOptionsSet(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "ws_set_options"; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mWebsocketOptionsSet = true;
            parseWebSocketCustomHeaders(parent);
        }
    }

    @Nullable
    public final Boolean hasWebsocketOptionsSet() {
        return mWebsocketOptionsSet;
    }

    private Properties mWebSocketCustomHeaders = null;
    private void parseWebSocketCustomHeaders(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "headers"; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mWebSocketCustomHeaders = new Properties();
            for (Map.Entry<String,Object> entry: parent.entrySet()) {
                mWebSocketCustomHeaders.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Nullable
    public final Properties getWebSocketCustomHeaders() {
        return mWebSocketCustomHeaders;
    }

    /*
     * LWT (Last Will and Testament) part
     */
    private Boolean mLwtSet = null;
    public void parseLwtSet(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "will_set"; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mLwtSet = true;

            /*
             * Following parameters use the identical key with previous ones,
             * such like "topic", "qos" or "retain".
             * To avoid ambiguity, these parameters must be set all at once.
             */
            parseLwtTopic(parent);
            parseLwtPayload(parent);
            parseLwtQos(parent);
            parseLwtRetain(parent);
        }
    }

    @Nullable
    public final Boolean hasLwtSet() {
        return mLwtSet;
    }

    private String mLwtTopic = null;
    private void parseLwtTopic(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "topic"; /* Mandatory */
        mLwtTopic = super.parseString(myParams, key, true);
    }

    @Nullable
    public final String getLwtTopic() {
        return mLwtTopic;
    }

    private String mLwtPayload = null;
    private void parseLwtPayload(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "payload"; /* Mandatory */
        mLwtPayload = super.parseString(myParams, key, true);
    }

    @Nullable
    public final String getLwtPayload() {
        return mLwtPayload;
    }

    private Integer mLwtQos = null;
    private void parseLwtQos(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "qos"; /* Mandatory */
        Number parsedValue = super.parseNumber(myParams, key, true);
        if (parsedValue instanceof Integer) {
            Integer probe = (Integer) parsedValue;
            if (!probe.equals(Consistency.AT_MOST_ONCE.getQos())
                    && !probe.equals(Consistency.AT_LEAST_ONCE.getQos())
                    && !probe.equals(Consistency.EXACTLY_ONCE.getQos())) {
                throw new InvalidConfigurationException(
                        key + "(" + parsedValue + "): Out of range", null);
            }
            mLwtQos = probe;
        }
    }

    @Nullable
    public Integer getLwtQos() {
        return mLwtQos;
    }

    private Boolean mLwtRetain = null;
    private void parseLwtRetain(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "retain"; /* Mandatory */
        mLwtRetain = super.parseBoolean(myParams, key, true);
    }

    @Nullable
    public final Boolean getLwtRetain() {
        return mLwtRetain;
    }

    /*
     * Connect part
     */
    private Boolean mConnectParameters = null;
    public void parseMqttConnect(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = MqttAsyncMessageIOKt.KEY_MQTT_CONNECT; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mConnectParameters = true;

            parseKeepAliveInterval(parent);
            parseAutomaticReconnect(parent);
            parseConnectionTimeout(parent);
        }
    }

    @Nullable
    public final Boolean hasConnectParameters() {
        return mConnectParameters;
    }

    //private int mKeepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
    private Integer mKeepAliveInterval = null;
    private void parseKeepAliveInterval(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "keepalive"; /* Optional */
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue != null) {
            /* Non-empty value has set */
            if (parsedValue instanceof Integer) {
                int intValue = (int) parsedValue;
                if (intValue < 0) {
                    throw new InvalidConfigurationException(
                            key + ": Out of range (" + parsedValue + ")", null);
                }
                mKeepAliveInterval = intValue;
            } else {
                throw new InvalidConfigurationException(
                        key + ": Not an Integer (" + parsedValue + ")", null);
            }
        }
    }

    @Nullable
    public final Integer getKeepAliveInterval() {
        return mKeepAliveInterval;
    }

    private Boolean mAutomaticReconnect = null;
    private void parseAutomaticReconnect(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "automatic_reconnect"; /* Optional */
        mAutomaticReconnect = super.parseBoolean(myParams, key, false);
    }

    @Nullable
    public final Boolean getAutomaticReconnect() {
        return mAutomaticReconnect;
    }

    //private int mConnectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
    private Integer mConnectionTimeout = null;
    private void parseConnectionTimeout(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "connection_timeout"; /* Optional */
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue != null) {
            /* Non-empty value has set */
            if (parsedValue instanceof Integer) {
                int intValue = (int) parsedValue;
                if (intValue < 0) {
                    throw new InvalidConfigurationException(
                            key + ": Out of range (" + parsedValue + ")", null);
                }
                mConnectionTimeout = intValue;
            } else {
                throw new InvalidConfigurationException(
                        key + ": Not an Integer (" + parsedValue + ")", null);
            }
        }
    }

    @Nullable
    public final Integer getConnectionTimeout() {
        return mConnectionTimeout;
    }

    /*
     * Reconnect part
     */
    private Boolean mReconnectParameters = null;
    public void parseMqttReconnect(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "reconnect_delay_set"; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mReconnectParameters = true;

            parseKeepAliveInterval(parent);
            parseAutomaticReconnect(parent);
            parseConnectionTimeout(parent);
        }
    }

    @Nullable
    public final Boolean hasReconnectParameters() {
        return mReconnectParameters;
    }

    private Integer mMaxReconnectDelay = null;
    private void parseMaxReconnectDelay(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "max_delay";
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue != null) {
            /* Non-empty value has set */
            if (parsedValue instanceof Integer) {
                int intValue = (int) parsedValue;
                if (intValue < 0) {
                    throw new InvalidConfigurationException(
                            key + ": Out of range (" + parsedValue + ")", null);
                }
                mMaxReconnectDelay = intValue * 1000; /* seconds -> milliseconds */
            } else {
                throw new InvalidConfigurationException(
                        key + ": Not an Integer (" + parsedValue + ")", null);
            }
        }
    }

    @Nullable
    public final Integer getMaxReconnectDelay() {
        return mMaxReconnectDelay;
    }
}
