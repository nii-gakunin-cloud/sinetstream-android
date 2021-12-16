/*
 * Copyright (c) 2021 National Institute of Informatics
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

import android.app.Activity;
import android.content.Context;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class KeyChainParser implements Runnable {
    private final String TAG = KeyChainParser.class.getSimpleName();

    private final Context mContext;
    private final KeyChainParserListener mListener;
    private final String mProtocol;
    private final String mAlias;
    private final boolean mHasServerCert;

    private boolean mIsError = false;

    public KeyChainParser(
            Context context,
            KeyChainParserListener listener,
            String protocol, String alias, boolean hasServerCert) {
        this.mContext = context;
        this.mListener = listener;
        this.mProtocol = protocol;
        this.mAlias = alias;
        this.mHasServerCert = hasServerCert;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        buildSslContext(mProtocol, mAlias, mHasServerCert);
    }

    /**
     * buildSslContext -- Build a {@link SSLContext} object for SSL/TLS connection.
     * <p>
     *     This function is supposed to build a SSLContext using the alias for the
     *     keychain and certificate pair, as the result of
     *     {@link KeyChainHandler#checkCertificate(Activity, KeyChainHandler.KeyChainListener)}.
     * </p>
     * @param protocol -- SSL/TLS version such like "TLSv1.2".
     * @param alias -- The alias for the private key and certificate pair. It can
     *              be {@code null} if client certificate is not used.
     * @param hasServerCert -- {@code true} if self-signed certificate is used.
     */
    private void buildSslContext(
            @NonNull String protocol, @Nullable String alias, boolean hasServerCert) {
        SSLContext sslContext;

        KeyManager[] keyManagers = null;
        if (alias != null) {
            /* We use client certificates */
            keyManagers = extractKeyManagers(alias);
            if (mIsError) {
                return;
            }
        }

        TrustManager[] trustManagers = null;
        if (hasServerCert) {
            /* We use self-signed certificates */
            trustManagers = extractTrustManagers();
            if (mIsError) {
                return;
            }
        }

        try {
            sslContext = SSLContext.getInstance(protocol);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SSLContext.getInstance: " + e.getMessage());
            mListener.onError(e.toString());
            return;
        }

        try {
            sslContext.init(
                    keyManagers,
                    trustManagers,
                    new SecureRandom());
        } catch (KeyManagementException e) {
            Log.e(TAG, "SSLContext.init(): " + e.getMessage());
            mListener.onError(e.toString());
            return;
        }
        mListener.onParsed(sslContext);
    }

    @Nullable
    private TrustManager[] extractTrustManagers() {
        KeyStore ksSrc, ksDst;
        try {
            ksSrc = KeyStore.getInstance("AndroidCAStore");
            ksDst = KeyStore.getInstance("BKS");
        } catch (KeyStoreException e) {
            mListener.onError("KeyStore.getInstance(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        try {
            ksSrc.load(null);
            ksDst.load(null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            mListener.onError("KeyStore.load(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        Enumeration<String> aliases;
        try {
            aliases = ksSrc.aliases();
        } catch (KeyStoreException e) {
            mListener.onError("KeyStore.aliases(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        boolean setUserCertificates = false;
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            /*
             * The KeyStore for "AndroidCAStore" contains certificates
             * both for system preinstalled ones (alias is described
             * like "system:xxx"), and user installed ones (alias is
             * described like "user:xxx").
             * Here we choose user installed ones.
             */
            if (!alias.startsWith("user:")) {
                continue;
            }

            X509Certificate cert;
            try {
                cert = (X509Certificate) ksSrc.getCertificate(alias);
            } catch (KeyStoreException e) {
                mListener.onError("KeyStore.getCertificate(" + alias + "): "
                        + e.getMessage());
                mIsError = true;
                return null;
            }

            String distinguishedName = cert.getSubjectX500Principal().getName();
            Log.d(TAG, alias + "-> DN: " + distinguishedName);

            try {
                cert.checkValidity();
            } catch (CertificateExpiredException
                    | CertificateNotYetValidException e) {
                mListener.onError("KeyStore.checkValidity(): "
                        + distinguishedName
                        + e.getMessage());
                mIsError = true;
                return null;
            }

            try {
                ksDst.setCertificateEntry(alias, cert);
            } catch (KeyStoreException e) {
                mListener.onError("KeyStore.setCertificateEntry(): "
                        + distinguishedName
                        + e.getMessage());
                mIsError = true;
                return null;
            }
            setUserCertificates = true;
        }
        if (!setUserCertificates) {
            Log.d(TAG, "No user-specified Certificates");
            return null;
        }

        TrustManagerFactory tmf;
        try {
            tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            mListener.onError("TrustManagerFactory.getInstance(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        try {
            tmf.init(ksDst);
        } catch (KeyStoreException e) {
            mListener.onError("TrustManagerFactory.init(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        return tmf.getTrustManagers();
    }

    @Nullable
    private KeyManager[] extractKeyManagers(@NonNull String alias) {
        Context context = mContext;

        PrivateKey privateKey;
        try {
            privateKey = KeyChain.getPrivateKey(
                    context.getApplicationContext(), alias);
        } catch (InterruptedException | KeyChainException e) {
            mListener.onError("KeyChain.getPrivateKey: " +
                    "alias(" + alias + "): " + e.getMessage());
            mIsError = true;
            return null;
        }
        if (privateKey == null) {
            mListener.onError("KeyChain.getPrivateKey: " +
                    "alias(" + alias + ") not found?");
            mIsError = true;
            return null;
        }

        X509Certificate[] chain;
        try {
            chain = KeyChain.getCertificateChain(
                    context.getApplicationContext(), alias);
        } catch (InterruptedException | KeyChainException e) {
            mListener.onError("KeyChain.getCertificateChain: "
                    + "alias(" + alias + "): " + e.getMessage());
            mIsError = true;
            return null;
        }
        if (chain == null) {
            mListener.onError("KeyChain.getCertificateChain: " +
                    "alias(" + alias + "): Empty chain?");
            mIsError = true;
            return null;
        }

        for (int i = 0, n = chain.length; i < n; i++) {
            X509Certificate cert = chain[i];
            Log.d(TAG, "----- cert[" + (i+1) + "/" + n + "]-----");
            Log.d(TAG, "Subject DN: " +
                    cert.getSubjectX500Principal().getName());
            Log.d(TAG, "Issuer DN: " +
                    cert.getIssuerX500Principal().getName());
            Log.d(TAG, "Type: " + cert.getType());
            Log.d(TAG, "Version: " + cert.getVersion());
            Log.d(TAG, "BasicConstraints: " + cert.getBasicConstraints());

            try {
                cert.checkValidity();
            } catch (CertificateExpiredException |
                    CertificateNotYetValidException e) {
                String distinguishedName =
                        cert.getSubjectX500Principal().getName();
                mListener.onError("X509Certificate.checkValidity("
                        + distinguishedName + "): "
                        + e.getMessage());
                mIsError = true;
                return null;
            }
        }

        KeyStore ks;
        try {
            /* We want a KeyStore instance for PKCS12, not default BKS. */
            //ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks = KeyStore.getInstance("PKCS12");
        } catch (KeyStoreException e) {
            mListener.onError("KeyStore.getInstance(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        try {
            ks.load(null);
        } catch (CertificateException |
                IOException |
                NoSuchAlgorithmException e) {
            mListener.onError("KeyStore.load(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        try {
            /*
             * The password passing here is used for protecting
             * the privateKey. It can be null.
             */
            ks.setKeyEntry(alias, privateKey, null, chain);
        } catch (KeyStoreException | IllegalArgumentException e) {
            mListener.onError("KeyStore.setKeyEntry(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        KeyManagerFactory kmf;
        try {
            kmf = KeyManagerFactory.getInstance("X509");
        } catch (NoSuchAlgorithmException e) {
            mListener.onError("KeyManagerFactory.getInstance(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        try {
            /*
             * The password passing here is used for recovering keys
             * in the KeyStore. It can be null.
             */
            kmf.init(ks, null);
        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | UnrecoverableKeyException e) {
            mListener.onError("TrustManagerFactory.init(): "
                    + e.getMessage());
            mIsError = true;
            return null;
        }

        return kmf.getKeyManagers();
    }

    public interface KeyChainParserListener {
        void onError(@NonNull String errmsg);
        void onParsed(@NonNull SSLContext sslContext);
    }
}
