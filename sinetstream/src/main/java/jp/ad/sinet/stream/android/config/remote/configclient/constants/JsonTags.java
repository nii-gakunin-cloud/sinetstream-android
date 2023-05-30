/*
 * Copyright (C) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.android.config.remote.configclient.constants;

public class JsonTags {
    /* Config server access information, which has downloaded outside of REST-API */
    public final static String KEY_CONFIG_SERVER = "config-server";
    public final static String KEY_CONFIG_SERVER_ADDRESS = "address";
    public final static String KEY_CONFIG_SERVER_USER = "user";
    public final static String KEY_CONFIG_SERVER_SECRET_KEY = "secret-key";
    public final static String KEY_CONFIG_SERVER_EXPIRATION_DATE = "expiration-date";

    /* REST-API: AuthToken request */
    public final static String KEY_AUTH_STRATEGY = "strategy";
    public final static String KEY_AUTH_USER = "user";
    public final static String KEY_AUTH_SECRET_KEY = "secret-key";

    /* REST-API: AuthToken response */
    public final static String KEY_AUTH_ACCESS_TOKEN = "accessToken";
    public final static String KEY_AUTH_AUTHENTICATION = "authentication";
    //public final static String KEY_AUTH_USER = "user";

    /* REST-API: Data stream response */
    public final static String KEY_NAME = "name";
    public final static String KEY_CONFIG = "config";
    public final static String KEY_ATTACHMENTS = "attachments";
    public final static String KEY_SECRETS = "secrets";

    /* REST-API: SINETStream configuration converted in JSON format */
    public final static String KEY_CONFIG_HEADER = "header";
    public final static String KEY_CONFIG_HEADER_VERSION = "version";
    public final static String KEY_CONFIG_CONFIG = "config";

    /* REST-API: Attachments */
    public final static String KEY_ATTACHMENT_TARGET = "target";
    public final static String KEY_ATTACHMENT_VALUE = "value";
    public final static String KEY_ATTACHMENT_TLS_CA_CERTS_DATA = "tls.ca_certs_data";
    public final static String KEY_ATTACHMENT_TLS_CERTFILE_DATA = "tls.certfile_data";

    /* REST-API: Secrets */
    public final static String KEY_SECRET_ID = "id";
    public final static String KEY_SECRET_IDS = "ids";
    public final static String KEY_SECRET_VERSION = "version";
    public final static String KEY_SECRET_FINGERPRINT = "fingerprint";
    public final static String KEY_SECRET_TARGET = KEY_ATTACHMENT_TARGET;
    public final static String KEY_SECRET_VALUE = KEY_ATTACHMENT_VALUE;

    /* REST-API: PubKey request */
    public final static String KEY_PUBKEY_PUBLIC_KEY = "publicKey";
    public final static String KEY_PUBKEY_COMMENT = "comment";
    public final static String KEY_PUBKEY_DEFAULT_KEY = "defaultKey";

    /* REST-API: PubKey response */
    public final static String KEY_PUBKEY_ID = "id";
    public final static String KEY_PUBKEY_FINGERPRINT = KEY_SECRET_FINGERPRINT;
    //public final static String KEY_PUBKEY_COMMENT = "comment";
    //public final static String KEY_PUBKEY_DEFAULT_KEY = "defaultKey";
    public final static String KEY_PUBKEY_CREATED_AT = "createdAt";
    public final static String KEY_PUBKEY_UPDATED_AT = "updatedAt";
}
