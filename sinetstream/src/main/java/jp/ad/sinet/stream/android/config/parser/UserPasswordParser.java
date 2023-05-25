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
import jp.ad.sinet.stream.android.mqtt.MqttAsyncMessageIOKt;

public class UserPasswordParser extends BaseParser {

    /* Entry point */
    public void parse(@NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        parseUserPassword(configParameters);
    }

    private void parseUserPassword(@NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = MqttAsyncMessageIOKt.KEY_MQTT_USER_PW;  /* Optional */
        Map<String,Object> parent = parseMap(configParameters, key, false);
        if (parent != null) {
            /*
             * Both UserName and Password must be set, if we use
             * basic user authentication.
             */
            parseUserName(parent);
            parsePassword(parent);
        }
    }

    private String mUserName = null;
    private void parseUserName(@NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = MqttAsyncMessageIOKt.KEY_MQTT_USERNAME; /* Mandatory */
        mUserName = parseAlphaNumeric(configParameters, key, true);
    }

    @Nullable
    public final String getUserName() {
        return mUserName;
    }

    private String mPassword = null;
    private void parsePassword(@NonNull Map<String,Object> configParameters)
            throws InvalidConfigurationException {
        String key = MqttAsyncMessageIOKt.KEY_MQTT_PASSWORD; /* Mandatory */
        mPassword = parseAlphaNumeric(configParameters, key, true);
    }

    @Nullable
    public final String getPassword() {
        return mPassword;
    }
}
