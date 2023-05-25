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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class PrivateCertHandler {
    private final String TAG = PrivateCertHandler.class.getSimpleName();

    private final Context mContext;
    private String mProtocolVersion = "TLSv1.2";

    /*
     * [cf]
     * https://developer.android.com/training/articles/security-ssl.html?hl=ja
     */

    public PrivateCertHandler(@NonNull Context context) {
        this.mContext = context;
    }

    public void setProtocolVersion(@NonNull String protocol) {
        mProtocolVersion = protocol;
    }

    public SSLContext buildSslContextFromDataSets(
            @Nullable String serverCertificate,
            @Nullable byte[] clientCertificate,
            @Nullable char[] clientPassword) {
        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;

        KeyStore trustStore = loadTrustStoreFromData(serverCertificate);
        if (trustStore != null) {
            tmf = setupTrustManagerFactory(trustStore);
        }
        KeyStore keyStore = loadKeyStoreFromData(clientCertificate, clientPassword);
        if (keyStore != null) {
            kmf = setupKeyManagerFactory(keyStore, clientPassword);
        }
        return setupSslContext(tmf, kmf);
    }

    public SSLContext buildSslContextFromLocalFiles(
            @Nullable String serverCertificate,
            @Nullable String clientCertificate,
            @Nullable char[] clientPassword) {
        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;

        KeyStore trustStore = loadTrustStoreFromFile(serverCertificate);
        if (trustStore != null) {
            tmf = setupTrustManagerFactory(trustStore);
        }
        KeyStore keyStore = loadKeyStoreFromFile(clientCertificate, clientPassword);
        if (keyStore != null) {
            kmf = setupKeyManagerFactory(keyStore, clientPassword);
        }

        return setupSslContext(tmf, kmf);
    }

    @Nullable
    private KeyStore loadTrustStoreFromData(@Nullable String serverCertificate) {
        KeyStore keyStore = null;
        if (serverCertificate != null) {
            keyStore = KeyStoreUtil.getBksKeyStore();
            if (keyStore != null) {
                // Load CAs from an InputStream
                InputStream inputStream = new ByteArrayInputStream(
                        serverCertificate.getBytes(StandardCharsets.UTF_8));

                // Create a KeyStore containing our trusted CAs
                KeyStoreUtil.loadX509Certificate(keyStore, inputStream);
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "InputStream: " + e.getMessage());
                }
            }
        }
        return keyStore;
    }

    @Nullable
    private KeyStore loadTrustStoreFromFile(@Nullable String serverCertificate) {
        KeyStore keyStore = null;
        if (serverCertificate != null) {
            keyStore = KeyStoreUtil.getBksKeyStore();
            if (keyStore != null) {
                // Load CAs from an InputStream
                InputStream inputStream = null;
                try {
                    inputStream = mContext.getResources().getAssets().open(serverCertificate);
                } catch (IOException e) {
                    Log.w(TAG, "Cannot open private certificate: " + e.getMessage());
                }

                if (inputStream != null) {
                    // Create a KeyStore containing our trusted CAs
                    KeyStoreUtil.loadX509Certificate(keyStore, inputStream);
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, "InputStream: " + e.getMessage());
                    }
                }
            }
        }
        return keyStore;
    }

    @Nullable
    private TrustManagerFactory setupTrustManagerFactory(@Nullable KeyStore keyStore) {
        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = null;
        try {
            tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "TrustManagerFactory.getInstance: " + e.getMessage());
        }
        if (tmf != null) {
            try {
                tmf.init(keyStore);
            } catch (KeyStoreException e) {
                Log.w(TAG, "TrustManagerFactory.init(KeyStore): " + e.getMessage());
            }
        }
        return tmf;
    }

    @Nullable
    private KeyStore loadKeyStoreFromData(
            @Nullable byte[] clientCertificate,
            @Nullable char[] clientPassword) {
        KeyStore keyStore = null;
        if (clientCertificate != null && clientPassword != null) {
            keyStore = KeyStoreUtil.getPkcs12KeyStore();
            if (keyStore != null) {
                // Load certificate from an InputStream
                InputStream inputStream = new ByteArrayInputStream(clientCertificate);

                // Create a KeyStore containing our trusted CAs
                try {
                    keyStore.load(inputStream, clientPassword);
                } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
                    Log.w(TAG, "KeyStore.load(): " + e.getMessage());
                }

                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "InputStream: " + e.getMessage());
                }
            }
        }
        return keyStore;
    }

    @Nullable
    private KeyStore loadKeyStoreFromFile(
            @Nullable String clientCertificate,
            @Nullable char[] clientPassword) {
        KeyStore keyStore = null;
        if (clientCertificate != null && clientPassword != null) {
            keyStore = KeyStoreUtil.getPkcs12KeyStore();
            if (keyStore != null) {
                // Load CAs from an InputStream
                InputStream inputStream = null;
                try {
                    inputStream = mContext.getResources().getAssets().open(clientCertificate);
                } catch (IOException e) {
                    Log.w(TAG, "Cannot open private certificate: " + e.getMessage());
                }

                if (inputStream != null) {
                    // Create a KeyStore containing our trusted CAs
                    try {
                        keyStore.load(inputStream, clientPassword);
                    } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
                        Log.w(TAG, "KeyStore.load(): " + e.getMessage());
                    }

                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, "InputStream: " + e.getMessage());
                    }
                }
            }
        }
        return keyStore;
    }

    @Nullable
    private KeyManagerFactory setupKeyManagerFactory(
            @Nullable KeyStore keyStore, @Nullable char[] clientPassword) {
        KeyManagerFactory kmf = null;
        try {
            kmf = KeyManagerFactory.getInstance("X509");
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "KeyManagerFactory.getInstance(X509): " + e.getMessage());
        }

        if (kmf != null) {
            try {
                kmf.init(keyStore, clientPassword);
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                Log.w(TAG, "KeyManagerFactory.init(KeyStore): " + e.getMessage());
            }
        }
        return kmf;
    }

    @Nullable
    private SSLContext setupSslContext(
            @Nullable TrustManagerFactory tmf, @Nullable KeyManagerFactory kmf) {
        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance(mProtocolVersion);
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "SSLContext.getInstance(" + mProtocolVersion + "): " + e.getMessage());
        }
        if (sslContext != null) {
            try {
                sslContext.init(
                        (kmf != null) ? kmf.getKeyManagers() : null,
                        (tmf != null) ? tmf.getTrustManagers() : null,
                        new SecureRandom());
            } catch (KeyManagementException e) {
                Log.w(TAG, "SSLContext.init(): " + e.getMessage());
            }
        }
        return sslContext;
    }
}
