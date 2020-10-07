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

package jp.ad.sinet.stream.android.net;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import jp.ad.sinet.stream.android.api.InvalidConfigurationException;
import jp.ad.sinet.stream.android.config.CommonParser;
import jp.ad.sinet.stream.android.config.ConfigParser;
import jp.ad.sinet.stream.android.config.MqttParser;

public class UriBuilder {
    private final ConfigParser mConfigParser;
    private final CommonParser mCommonParser;
    private final MqttParser mMqttParser;

    public UriBuilder(@NonNull ConfigParser configParser) {
        this.mConfigParser = configParser;
        this.mCommonParser = configParser.getCommonParser();
        this.mMqttParser = configParser.getMqttParser();
    }

    @NonNull
    public String[] buildBrokerUris() {
        String[] uriArray = {};
        String uriScheme = getUriScheme();
        String[] Brokers = mCommonParser.getBrokers();

        if (Brokers != null) {
            ArrayList<String> arrayList = new ArrayList<>();
            for (String broker : Brokers) {
                String uri = uriScheme + "://" + broker;
                arrayList.add(uri);
            }
            uriArray = arrayList.toArray(new String[0]);
        }
        return uriArray;
    }

    private String getUriScheme() {
        String scheme;
        boolean tlsEnabled = mConfigParser.getTlsEnabled();
        String transport = mMqttParser.getTransport();
        if (transport != null) {
            /*
             * RFC3986, 3.1:
             * Although schemes are case-insensitive, the canonical form is
             * lowercase and documents that specify schemes must do so with
             * lowercase letters.
             */
            if (tlsEnabled) {
                if (transport.equalsIgnoreCase("websocket") ||
                        transport.equalsIgnoreCase("websockets")) {
                    /* Encrypted WebSocket connection */
                    scheme = "wss";
                } else if (transport.equalsIgnoreCase("tcp")) {
                    /*
                     * This is not an IANA-registered URI scheme, but
                     * the Paho MQTT Client treats "ssl" as a connection
                     * with SSL/TLS.
                     */
                    scheme = "ssl";
                } else {
                    throw new InvalidConfigurationException(
                            "Unknown transport(" + transport + ")", null
                    );
                }
            } else {
                if (transport.equalsIgnoreCase("websocket") ||
                        transport.equalsIgnoreCase("websockets")) {
                    /* WebSocket connection */
                    scheme = "ws";
                } else if (transport.equalsIgnoreCase("tcp")) {
                    /*
                     * This is not an IANA-registered URI scheme, but
                     * the Paho MQTT Client treats "tcp" as a plain
                     * TCP connection.
                     */
                    scheme = "tcp";
                }  else {
                    throw new InvalidConfigurationException(
                            "Unknown transport(" + transport + ")", null
                    );
                }
            }
        } else {
            /*
             * Keyword transport has omitted.
             * As a fallback, treat as either "tcp" or "ssl".
             */
            if (tlsEnabled) {
                scheme = "ssl";
            } else {
                scheme = "tcp";
            }
        }
        return scheme;
    }
}
