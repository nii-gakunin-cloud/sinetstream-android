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

import jp.ad.sinet.stream.android.api.InvalidConfigurationException;

public class CommonParser extends BaseParser {

    /* Entry point */
    public void parse(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        parseMessagingSystemType(myParams);
        parseBrokers(myParams);
    }

    private String mType = null;
    private void parseMessagingSystemType(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "type"; /* Mandatory */
        String parsedValue = super.parseString(myParams, key, true);
        if (parsedValue != null && !parsedValue.equalsIgnoreCase("mqtt")) {
            throw new InvalidConfigurationException(
                    key + "(" + parsedValue + "): Unsupported value", null);
        }
        mType = parsedValue;
    }

    @Nullable
    public final String getMessagingSystemType() {
        return mType;
    }

    private String[] mBrokers = null;
    private void parseBrokers(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "brokers"; /* Mandatory */
        String[] array = super.parseStringList(myParams, key, true);
        if (array != null) {
            for (int i = 0, n = array.length; i < n; i++) {
                String parsedValue = array[i];
                if (parsedValue.length() <= 0 || parsedValue.contains("://")) {
                    throw new InvalidConfigurationException(
                            key + "[" + (i+1) + '/' + n + "](" +
                                    ((parsedValue.length() > 0) ? parsedValue : "\"\"") +
                                    "): Must be \"<host>[:<port>]\" format", null);
                }
            }
            mBrokers = array;
        }
    }

    @Nullable
    public final String[] getBrokers() {
        return mBrokers;
    }
}
