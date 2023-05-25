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

package jp.ad.sinet.stream.android.config.remote.keystore;

import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

class FingerprintHandler {
    private final String TAG = FingerprintHandler.class.getSimpleName();

    private final FingerprintHandlerListener mListener;

    public FingerprintHandler(FingerprintHandlerListener listener) {
        this.mListener = listener;
    }

    /**
     * Calculate fingerprint
     *
     * @param base64EncodedPublicKey public key in DER format
     * @return fingerprint, or {@code null} on failure.
     */
    @Nullable
    public String calc(@NonNull String base64EncodedPublicKey) {
        RSAPublicKey rsaPublicKey = loadRsaPublicKey(base64EncodedPublicKey);
        if (rsaPublicKey != null) {
            return calculateFingerprintForRsaPublicKey(rsaPublicKey);
        }
        return null;
    }

    @Nullable
    private RSAPublicKey loadRsaPublicKey(@NonNull String base64EncodedPublicKey) {
        byte[] key;
        try {
            key = Base64.decode(base64EncodedPublicKey, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            mListener.onError(TAG + ": Base64.decode: " + e.getMessage());
            return null;
        }

        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(key);
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA);
        } catch (NoSuchAlgorithmException e) {
            mListener.onError(TAG +
                    ": KeyFactory.getInstance(" + KeyProperties.KEY_ALGORITHM_RSA + "): " +
                    e.getMessage());
            return null;
        }

        RSAPublicKey rsaPublicKey;
        try {
            rsaPublicKey = (RSAPublicKey) kf.generatePublic(x509EncodedKeySpec);
        } catch (InvalidKeySpecException e) {
            mListener.onError(TAG + ": KeyFactory.generatePublic: " + e.getMessage());
            return null;
        }

        return rsaPublicKey;
    }

    @Nullable
    private String calculateFingerprintForRsaPublicKey(@NonNull RSAPublicKey rsaPublicKey) {
        /*
         * How to Calculate Fingerprint From SSH RSA Public Key in Java?
         * https://stackoverflow.com/questions/51059782/how-to-calculate-fingerprint-from-ssh-rsa-public-key-in-java
         */
        byte[] modulus = rsaPublicKey.getModulus().toByteArray(); // Java is 2sC bigendian
        byte[] exponent = rsaPublicKey.getPublicExponent().toByteArray(); // and so is SSH
        byte[] tag = "ssh-rsa".getBytes(); // charset very rarely matters here
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        byte[] encoded = null;

        String errorMessage = null;
        try {
            dos.writeInt(tag.length);
            dos.write(tag);

            dos.writeInt(exponent.length);
            dos.write(exponent);

            dos.writeInt(modulus.length);
            dos.write(modulus);

            encoded = os.toByteArray();
        } catch (IOException e) {
            errorMessage = TAG + ": I/O error: " + e.getMessage();
        } finally {
            try {
                dos.close();
            } catch (IOException e) {
                Log.e(TAG, "IO error: " + e.getMessage());
                /* Don't care this error */
            }
            try {
                os.close();
            } catch (IOException e) {
                Log.e(TAG, "IO error: " + e.getMessage());
                /* Don't care this error */
            }
        }
        if (errorMessage != null) {
            mListener.onError(errorMessage);
            return null;
        }

        // now hash that (you don't really need Apache)
        // assuming SHA256-base64 (see below)
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256);
        } catch (NoSuchAlgorithmException e) {
            mListener.onError(TAG +
                    "MessageDigest.getInstance(" + KeyProperties.DIGEST_SHA256 + "): " +
                    e.getMessage());
            return null;
        }

        byte[] result = digest.digest(encoded);
        String fingerprint = Base64.
                encodeToString(result, Base64.NO_WRAP).
                replaceAll("=", "");
        if (mDebugEnabled) {
            Log.d(TAG, "Fingerprint: " + fingerprint);
        }

        return fingerprint;
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface FingerprintHandlerListener {
        void onError(@NonNull String description);
    }
}
