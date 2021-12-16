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

import org.jetbrains.annotations.NotNull;

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
    private boolean mCryptoDebugEnabled = false;

    /**
     * Constructor -- Allocates a KeyUtil instance
     *
     * @param keyLength  -- Byte length of a secret key to be generated
     * @param keyDerivationAlgorithm -- Secret key derivation algorithm name,
     *                               such like "PBKDF2WithHmacSHA256"
     * @param cipherAlgorithm  -- Cipher algorithm name, such like "AES"
     * @param saltLength  -- Byte length of a cryptographic salt to be generated
     * @param iterationCount -- Iteration count to be used in block mode
     * @throws CryptoException  -- Invalid parameter cases
     */
    public KeyUtil(int keyLength,
                   @NonNull String keyDerivationAlgorithm,
                   @NonNull String cipherAlgorithm,
                   int saltLength,
                   int iterationCount)
            throws CryptoException {
        /*
         * KeyLength must be multiple of 8.
         */
        if (keyLength >= 8 && (keyLength % 8 == 0)) {
            this.mKeyLength = keyLength;
        } else {
            Log.e(TAG, "Invalid Key length: " + keyLength);
            throw new CryptoException(TAG + "Invalid Key length: " + keyLength, null);
        }

        /*
         * The salt value is generated at random and can be any length.
         */
        if (saltLength > 0) {
            this.mSaltLength = saltLength;
        } else {
            Log.e(TAG, "Invalid Salt length: " + saltLength);
            throw new CryptoException(TAG + "Invalid Salt length: " + saltLength, null);
        }

        this.mKeyDerivationAlgorithm = keyDerivationAlgorithm;
        this.mCipherAlgorithm = cipherAlgorithm;
        this.mIterationCount = iterationCount;
    }

    public void setDebugMode(boolean debugEnabled) {
        /* NB: Enabling debug degrades performance */
        mCryptoDebugEnabled = debugEnabled;
    }

    /**
     * Generates a cryptographic
     * <a href="https://en.wikipedia.org/wiki/Salt_(cryptography)">salt</a>,
     * which is a safeguard to ensure uniqueness of the hashed value calculated
     * from "password + salt", not just "password".
     *
     * @return The user-specified number of random bytes
     */
    @NotNull
    public final byte[] generateSalt() {
        byte[] salt = new byte[mSaltLength];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);

        return salt;
    }

    /**
     * Generates a secret key by password, which is introduced in blog articles[1][2].
     *
     * <p>
     *     [1]
     *     <a href="https://android-developers.googleblog.com/2016/06/security-crypto-provider-deprecated-in.html">
     *         Security "Crypto" provider deprecated in Android N
     *     </a>
     *     <br>
     *     [2]
     *     <a href="https://nelenkov.blogspot.com/2012/04/using-password-based-encryption-on.html">
     *         Using Password-based Encryption on Android
     *     </a>
     * </p>
     *
     * @param password -- User-specified plaintext password
     * @param salt -- Random bytes to be concatenated with the password
     * @return The generated SecretKey object
     * @throws CryptoException -- Invalid parameter cases
     */
    public final SecretKey getSecretKeyByPassword(
            @NonNull String password, @NonNull byte[] salt)
            throws CryptoException {
        /* Use this to derive the key from the password: */
        KeySpec keySpec;
        try {
            keySpec = new PBEKeySpec(
                    password.toCharArray(), salt, mIterationCount, mKeyLength);
        } catch (NullPointerException | IllegalArgumentException e) {
            Log.e(TAG, "PBEKeySpec: " + e.getMessage());
            throw new CryptoException("PBEKeySpec", e);
        }

        SecretKeyFactory keyFactory;
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

    /**
     * Generates an
     * <a href="https://docs.oracle.com/javase/8/docs/api/javax/crypto/spec/IvParameterSpec.html">IvParameterSpec</a>
     * object.
     *
     * @param cipher -- A Cipher object which holds the block size of the algorithm
     * @return The generated IvParameterSpec object
     */
    @Nullable
    public final IvParameterSpec getIvParameterSpec(@NonNull Cipher cipher) {
        if (mCryptoDebugEnabled) {
            Log.d(TAG, "Cipher{" +
                    "algorithm(" + cipher.getAlgorithm() + ")," +
                    "blocksize(" + cipher.getBlockSize() + ")}");
        }

        /*
         * [NB] Initial Vector must be specified in CBC mode
         */
        IvParameterSpec ivParameterSpec = null;
        int blockSize = cipher.getBlockSize();
        if (blockSize > 0) {
            byte[] iv = new byte[blockSize];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            ivParameterSpec = new IvParameterSpec(iv);
        } else {
            Log.w(TAG, cipher.getAlgorithm() + ": Not a block cipher");
        }
        return ivParameterSpec;
    }

    /**
     * Generates a
     * <a href="https://docs.oracle.com/javase/8/docs/api/javax/crypto/spec/GCMParameterSpec.html">GCMParameterSpec</a>
     * instance.
     *
     * @param cipher -- A Cipher object which holds the block size of the algorithm
     * @param authTagBits -- The authentication tag length (in bits)
     * @param initialVector -- The initial vector bytes
     * @return The generated GCMParameterSpec object
     * @throws CryptoException -- Invalid parameter cases
     */
    @Nullable
    public final GCMParameterSpec getGCMParameterSpec(
            @NonNull Cipher cipher,
            int authTagBits,
            @Nullable byte[] initialVector)
            throws CryptoException {
        if (mCryptoDebugEnabled) {
            Log.d(TAG, "Cipher{" +
                    "algorithm(" + cipher.getAlgorithm() + ")," +
                    "blocksize(" + cipher.getBlockSize() + ")}");
        }

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
        int blockSize = cipher.getBlockSize();
        if (blockSize > 0) {
            byte[] iv;
            if (initialVector != null) {
                iv = initialVector;
            } else {
                iv = new byte[blockSize]; /* a.k.a. nonce */
                SecureRandom secureRandom = new SecureRandom();
                secureRandom.nextBytes(iv);
            }

            try {
                gcmParameterSpec = new GCMParameterSpec(authTagBits, iv);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "GCMParameterSpec: " + e.getMessage());
                throw new CryptoException("GCMParameterSpec", e);
            }
        } else {
            Log.w(TAG, cipher.getAlgorithm() + ": Not a block cipher");
        }
        return gcmParameterSpec;
    }

    /**
     * Generates an
     * <a href="https://en.wikipedia.org/wiki/Initialization_vector">initialization vector</a>
     * data.
     *
     * @param length -- The byte length of the IV, which can be any positive integer
     * @return The random bytes of the specified length
     */
    public byte[] generateGCMInitializationVector(int length) {
        byte[] iv = new byte[length];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Returns the salt length specified at the constructor
     *
     * @return The salt length in bytes
     */
    public final int getSaltLength() {
        return mSaltLength;
    }
}
