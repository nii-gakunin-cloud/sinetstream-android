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

package jp.ad.sinet.stream.android.crypto;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import jp.ad.sinet.stream.android.api.CryptoException;

public class KeyUtil {
    private final String TAG = KeyUtil.class.getSimpleName();

    private final int mKeyLength;
    private final int mSaltLength;

    private final String mKeyDerivationAlgorithm;
    private final String mCipherAlgorithm;
    private final int mIterationCount;

    public KeyUtil(int keyLength,
                   @NonNull String keyDerivationAlgorithm,
                   @NonNull String cipherAlgorithm,
                   int iterationCount)
            throws CryptoException {
        /*
         * KeyLength must be multiple of 8.
         */
        if (keyLength >= 8 && (keyLength % 8 == 0)) {
            this.mKeyLength = keyLength;
            this.mSaltLength = KeyLength2SaltLength(keyLength);
        } else {
            Log.e(TAG, "Invalid Key length: " + keyLength);
            throw new CryptoException(TAG + "Invalid Key length: " + keyLength, null);
        }

        this.mKeyDerivationAlgorithm = keyDerivationAlgorithm;
        this.mCipherAlgorithm = cipherAlgorithm;
        this.mIterationCount = iterationCount;
    }

    public final byte[] generateSalt() throws CryptoException {
        byte[] salt = new byte[mSaltLength];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);

        return salt;
    }

    /*
     * Generate a secret key by password, which is introduced in blog articles[1][2].
     *
     * [1] Security "Crypto" provider deprecated in Android N
     * https://android-developers.googleblog.com/2016/06/security-crypto-provider-deprecated-in.html
     *
     * [2] Using Password-based Encryption on Android
     * https://nelenkov.blogspot.com/2012/04/using-password-based-encryption-on.html
     */
    public final SecretKey getSecretKeyByPassword(
            @NonNull String password, @NonNull byte[] salt)
            throws CryptoException {

        if (SaltLength2KeyLength(salt.length) != mKeyLength) {
            Log.e(TAG, "Invalid salt length: " + salt.length);
            throw new CryptoException(TAG + "Invalid salt length: " + salt.length, null);
        }

        /* Use this to derive the key from the password: */
        KeySpec keySpec = new PBEKeySpec(
                password.toCharArray(), salt, mIterationCount, mKeyLength);

        SecretKeyFactory keyFactory;
        //final String algorithm = "PBKDF2WithHmacSHA1";
        try {
            keyFactory = SecretKeyFactory.getInstance(mKeyDerivationAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SecretKeyFactory(" + mKeyDerivationAlgorithm + "): " + e.getMessage());
            throw new CryptoException("SecretKeyFactory(" + mKeyDerivationAlgorithm + ")", e);
        }

        byte[] keyBytes;
        try {
            keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "KeyFactory.generateSecret: " + e.getMessage());
            throw new CryptoException("KeyFactory.generateSecret", e);
        }

        SecretKey key;
        try {
            key = new SecretKeySpec(keyBytes, mCipherAlgorithm);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "SecretKeySpec(" + mCipherAlgorithm + "): " + e.getMessage());
            throw new CryptoException("SecretKeySpec(" + mCipherAlgorithm + ")", e);
        }

        return key;
    }

    @Nullable
    public final IvParameterSpec getIvParameterSpec(@NonNull Cipher cipher) {
        /* Uncomment only in debug phase
        Log.d(TAG, "Cipher{" +
                "algorithm(" + cipher.getAlgorithm() + ")," +
                "blocksize(" + cipher.getBlockSize() + ")}");
         */

        /*
         * [NB] Initial Vector must be specified in CBC mode
         */
        IvParameterSpec ivParameterSpec = null;
        if (cipher.getBlockSize() > 0) {
            int blocksize = cipher.getBlockSize();
            byte[] iv = new byte[blocksize];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            ivParameterSpec = new IvParameterSpec(iv);
        }
        return ivParameterSpec;
    }

    /*
     * https://docs.oracle.com/javase/8/docs/api/javax/crypto/spec/GCMParameterSpec.html
     */
    @Nullable
    public final GCMParameterSpec getGCMParameterSpec(
            @NonNull Cipher cipher,
            int authTagBits,
            @Nullable byte[] initialVector)
            throws CryptoException {
        /* Uncomment only in debug phase
        Log.d(TAG, "Cipher{" +
                "algorithm(" + cipher.getAlgorithm() + ")," +
                "blocksize(" + cipher.getBlockSize() + ")}");
         */

        /*
         * Auth tag length must be multiple of 8.
         */
        if (authTagBits < 8 || (authTagBits % 8 != 0)) {
            throw new CryptoException(
                    "GCMParameterSpec: Invalid auth tag length:" + authTagBits, null);
        }

        /*
         * [NB] Initial Vector must be specified in GCM mode
         */
        GCMParameterSpec gcmParameterSpec = null;
        if (cipher.getBlockSize() > 0) {
            byte[] iv;
            if (initialVector != null) {
                iv = initialVector;
            } else {
                int blocksize = cipher.getBlockSize();
                iv = new byte[blocksize]; /* a.k.a. nonce */
                SecureRandom secureRandom = new SecureRandom();
                secureRandom.nextBytes(iv);
            }
            gcmParameterSpec = new GCMParameterSpec(authTagBits, iv);
        }
        return gcmParameterSpec;
    }

    public byte[] generateGCMInitializationVector(int length) {
        byte[] iv = new byte[length];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }

    public byte[] generateAdditionalAuthenticatedData(int length) {
        return new SecureRandom().generateSeed(length);
    }

    private int KeyLength2SaltLength(int KeyLength) {
        return KeyLength / 8;
    }

    private int SaltLength2KeyLength(int SaltLength) {
        return SaltLength * 8;
    }

    public final int getSaltLength() {
        return mSaltLength;
    }
}
