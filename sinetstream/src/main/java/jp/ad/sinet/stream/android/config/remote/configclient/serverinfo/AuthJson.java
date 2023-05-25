/*
 * Copyright (c) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.android.config.remote.configclient.serverinfo;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;

public class AuthJson {
    private final String TAG = AuthJson.class.getSimpleName();

    public void parseJsonData(@NonNull String jsonString,
                              @NonNull AuthJsonListener listener) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            listener.onError(TAG + ": " + e.getMessage());
            return;
        }
        parseAuthJson(jsonObject, listener);
    }

    private void parseAuthJson(@NonNull JSONObject jsonObject,
                               @NonNull AuthJsonListener listener) {
        final String key = JsonTags.KEY_CONFIG_SERVER;
        JSONObject configServer;
        try {
            configServer = jsonObject.getJSONObject(key);
        } catch (JSONException e) {
            listener.onError(TAG + ": " + e.getMessage());
            return;
        }
        parseConfigServer(configServer, listener);
    }

    private void parseConfigServer(@NonNull JSONObject jsonObject,
                                   @NonNull AuthJsonListener listener) {
        final String[] keys = {
                JsonTags.KEY_CONFIG_SERVER_ADDRESS,
                JsonTags.KEY_CONFIG_SERVER_USER,
                JsonTags.KEY_CONFIG_SERVER_SECRET_KEY,
                JsonTags.KEY_CONFIG_SERVER_EXPIRATION_DATE
        };
        /* Scan all keys to check if something is missing */
        HashMap<String,String> map = new HashMap<>();
        for (String key : keys) {
            try {
                String strVal = jsonObject.getString(key);
                map.put(key, strVal);
            } catch (JSONException e) {
                listener.onError(TAG + ": " + e.getMessage());
                return;
            }
        }

        String vServerUrl = map.get(keys[0]); // address
        String vAccount = map.get(keys[1]); // user
        String vSecretKey = map.get(keys[2]); // secret-key
        String vExpirationDate = map.get(keys[3]); // expiration-date

        if (vServerUrl != null && vAccount != null && vSecretKey != null &&
                vExpirationDate != null) {
            /* Sample expiration-date: "2022-03-14T08:20:54.671482697Z" */
            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            Date expirationDate;
            try {
                expirationDate = simpleDateFormat.parse(vExpirationDate);
            } catch (ParseException e) {
                listener.onError(TAG +
                        ": " + keys[3] + ": " + e.getMessage());
                return;
            }
            if (expirationDate == null) {
                listener.onError(TAG +
                        ": " + keys[3] + ": " + "Cannot convert Date to String");
                return;
            }

            Date currentDate = new Date(System.currentTimeMillis());
            if (currentDate.before(expirationDate)) {
                listener.onParsed(vServerUrl, vAccount, vSecretKey, expirationDate);
            } else {
                Log.w(TAG, "Account(" + vAccount + "): " +
                        "SecretKey(" + vSecretKey + ") has expired\n" +
                        "on " + expirationDate);
                listener.onExpired();
            }
        } else {
            /* This case should never happen */
            listener.onError(TAG + ": Incomplete configuration?");
        }
    }

    public interface AuthJsonListener {
        void onParsed(@NonNull String serverUrl,
                      @NonNull String account,
                      @NonNull String secretKey,
                      @NonNull Date expirationDate);

        void onExpired();

        void onError(@NonNull String description);
    }
}
