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

import java.util.ArrayList;
import java.util.Map;

import jp.ad.sinet.stream.android.api.Consistency;
import jp.ad.sinet.stream.android.api.InvalidConfigurationException;
import jp.ad.sinet.stream.android.api.ValueType;

/**
 * This is a collection of base parsers to be used as building block
 * for each user-specific parser functions.
 */
abstract class BaseParser {

    @Nullable
    public Boolean parseBoolean(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        Boolean booleanValue = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof Boolean) {
                booleanValue = (Boolean) obj;
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a boolean?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return booleanValue;
    }

    @Nullable
    public Number parseNumber(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        Number numberValue = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof Number) {
                numberValue = (Number) obj;
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a Number?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return numberValue;
    }

    @Nullable
    public String parseString(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        String stringValue = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof String) {
                stringValue = (String) obj;
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a String?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return stringValue;
    }

    @Nullable
    public String parseAlphaNumeric(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        String stringValue = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof String) {
                stringValue = (String) obj;
            } else if (obj instanceof Number) {
                stringValue = String.valueOf(obj);
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a String/Number?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return stringValue;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public String[] parseStringList(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        String[] strArray = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof String) {
                String probe = (String) obj;
                if (probe.contains(",")) {
                    ArrayList<String> wkList = new ArrayList<>();
                    String[] wkArray = probe.split(",", 0);
                    for (String s : wkArray) {
                        /* Remove white spaces, if any */
                        wkList.add(s.trim());
                    }
                    strArray = wkList.toArray(new String[0]);
                } else {
                    strArray = new String[]{probe.trim()};
                }
            } else if (obj instanceof ArrayList) {
                ArrayList<String> arrayList = (ArrayList<String>) obj;

                /*
                 * NB:
                 * Here we want a String array which have converted from
                 * comma-separated list.
                 * However, given ArrayList is formed in a single String.
                 *   arrayList[0] = "aaa,bbb,ccc"
                 * We need to manually split it like below.
                 *   arrayList[0] = "aaa"
                 *   arrayList[1] = "bbb"
                 *   arrayList[2] = "ccc"
                 */
                ArrayList<String> wkList = new ArrayList<>();
                for (int i = 0, n = arrayList.size(); i < n; i++) {
                    String probe = arrayList.get(i);
                    if (probe.contains(",")) {
                        String[] wkArray = probe.split(",", 0);
                        for (String s : wkArray) {
                            /* Remove white spaces, if any */
                            wkList.add(s.trim());
                        }
                    } else {
                        wkList.add(probe.trim());
                    }
                }
                strArray = wkList.toArray(new String[0]);
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a String[]?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return strArray;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public Map<String,Object> parseMap(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        Map<String,Object> mapValue = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof Map) {
                mapValue = (Map<String, Object>) obj;
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a Map?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return mapValue;
    }

    @Nullable
    public Integer parseConsistency(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        //int qos = (Consistency.AT_LEAST_ONCE).getQos(); // default
        Integer qos = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof Consistency) {
                Consistency probe = (Consistency) obj;
                qos = probe.getQos();
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a Consistency member?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return qos;
    }

    @Nullable
    public ValueType parseValueType(
            @NonNull Map<String,Object> myParams,
            @NonNull String key,
            boolean mandatory)
            throws InvalidConfigurationException {
        //ValueType valueType = ValueType.BYTE_ARRAY; // default
        ValueType valueType = null;
        Object obj;
        if ((obj = myParams.get(key)) != null) {
            if (obj instanceof ValueType) {
                valueType = (ValueType) obj;
            } else {
                throw new InvalidConfigurationException(
                        key + "(" + obj + "): Not a ValueType member?", null);
            }
        } else {
            if (myParams.containsKey(key)) {
                throw new InvalidConfigurationException(
                        key + ": Empty value?", null);
            } else if (mandatory) {
                throw new InvalidConfigurationException(
                        "Mandatory parameter '" + key + "' is missing.", null);
            }
        }
        return valueType;
    }
}
