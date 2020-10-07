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

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class KeyStoreUtil {
    private final static String TAG = KeyStoreUtil.class.getSimpleName();

    /*
     * [cf]
     * https://developer.android.com/training/articles/security-ssl.html?hl=ja
     * https://www.jssec.org/
     */

    public static KeyStore getEmptyKeyStore() {
        /* Android stores CA certificates in Bouncy Castle format */
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("BKS");
        } catch (KeyStoreException e) {
            Log.w(TAG, "KeyStore.getInstance(BKS): " + e.getMessage());
        }
        if (ks != null) {
            try {
                ks.load(null);
            } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
                Log.w(TAG, "KeyStore(BKS).load(null): " + e.getMessage());
            }
        }
        return ks;
    }

    static KeyStore getBksKeyStore() {
        KeyStore keyStore = null;
        try {
            /* Android stores the CA certificates in Bouncy Castle format */
            keyStore = KeyStore.getInstance("BKS");
        } catch (KeyStoreException e) {
            Log.w(TAG, "KeyStore.getInstance(BKS): " + e.getMessage());
        }
        if (keyStore != null) {
            try {
                keyStore.load(null);
            } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
                Log.w(TAG, "KeyStore(BKS).load(null): " + e.getMessage());
            }
        }
        return keyStore;
    }

    static KeyStore getPkcs12KeyStore() {
        KeyStore keyStore = null;
        try {
            /* Android stores the CA certificates in Bouncy Castle format */
            keyStore = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            Log.w(TAG, "KeyStore.getInstance(PKCS12): " + e.getMessage());
        }
        return keyStore;
    }

    static void loadX509Certificate(KeyStore keyStore, InputStream inputStream) {
        CertificateFactory factory;
        try {
            factory = CertificateFactory.getInstance("X509");
            if (factory != null) {
                X509Certificate x509 = null;
                try {
                    x509 = (X509Certificate) factory.generateCertificate(inputStream);
                } catch (CertificateException e) {
                    Log.w(TAG, "X509Certificate.generateCertificate(): " + e.getMessage());
                }
                if (x509 != null) {
                    String alias = x509.getSubjectDN().getName();
                    try {
                        keyStore.setCertificateEntry(alias, x509);
                    } catch (KeyStoreException e) {
                        Log.w(TAG, "KeyStore.setCertificateEntry(alias,x509): " + e.getMessage());
                    }
                }
            }
        } catch (CertificateException e) {
            Log.w(TAG, "CertificateFactory.getInstance(X509): " + e.getMessage());
        }
    }
}
