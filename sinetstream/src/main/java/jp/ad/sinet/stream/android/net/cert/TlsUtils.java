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

package jp.ad.sinet.stream.android.net.cert;

import android.content.Context;

import androidx.annotation.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class TlsUtils {
    private final String mServerCertificate;
    private final String mClientCertificate;
    private final char[] mClientPassword;

    public TlsUtils(@Nullable String serverCertificate,
                    @Nullable String clientCertificate,
                    @Nullable char[] clientPassword) {
        this.mServerCertificate = serverCertificate;
        this.mClientCertificate = clientCertificate;
        this.mClientPassword = clientPassword;
    }

    public SSLSocketFactory buildSslSocketFactory(Context context) {
        /*
         * Get SSLContext which holds both TrustStore (for CA certificate)
         * and KeyStore (for Client certificate).
         */
        PrivateCertHandler privateCertHandler = new PrivateCertHandler(context);
        SSLContext sslContext = privateCertHandler.getSSLContext(
                mServerCertificate, mClientCertificate, mClientPassword);

        return sslContext.getSocketFactory();
    }
}
