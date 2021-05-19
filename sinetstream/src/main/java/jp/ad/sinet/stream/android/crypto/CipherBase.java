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

import jp.ad.sinet.stream.android.api.CryptoException;

abstract class CipherBase {
    private final String TAG = CipherBase.class.getSimpleName();

    public final KeyUtil mKeyUtil;
    public String mTransformation = null;

    /**
     * Constructor -- Allocates a CipherBase instance
     *
     * @param keyLength  -- Byte length of a secret key to be generated
     * @param keyDerivationAlgorithm  -- Secret key derivation algorithm name, 
     *                                such like "PBKDF2WithHmacSHA256"
     * @param cipherAlgorithm -- Cipher algorithm name, such like "AES"
     * @param saltLength -- Byte length of a cryptographic salt to be generated
     * @param iterationCount -- Iteration count to be used in block mode
     * @throws CryptoException  -- Invalid parameter cases
     */
    public CipherBase(int keyLength,
                      @NonNull String keyDerivationAlgorithm,
                      @NonNull String cipherAlgorithm,
                      int saltLength,
                      int iterationCount)
            throws CryptoException {
        mKeyUtil = new KeyUtil(
                keyLength,
                keyDerivationAlgorithm,
                cipherAlgorithm,
                saltLength,
                iterationCount);
    }

    /**
     * Set transformation by parameter tuple {algorithm, mode, padding}
     *
     * @param cipherAlgorithm -- Cipher algorithm name, such like AES
     * @param feedbackMode -- Feedback mode of the algorithm, such like CBC
     * @param paddingScheme -- Padding scheme of the algorithm, such like PKCS5Padding
     * @throws CryptoException  -- Invalid parameter cases
     */
    public void setTransformation(@NonNull String cipherAlgorithm,
                                  @NonNull String feedbackMode,
                                  @NonNull String paddingScheme)
            throws CryptoException {
        mTransformation = String.join("/", cipherAlgorithm, feedbackMode, paddingScheme);
        /* Uncomment only in debug phase
        Log.d(TAG, "transformation: " + mTransformation);
         */
    }

    /**
     * encrypt -- Encrypts given data with predefined algorithm.
     * 
     * @param originalData  -- original data
     * @param password  -- a password to generate a secret key on the fly
     * @return byte[] -- encrypted data
     * @throws CryptoException  -- Parameter error, cryptographic calculation mismatch, etc.
     */
    public byte[] encrypt(byte[] originalData, @NonNull String password)
            throws CryptoException {
        if (mTransformation == null) {
            Log.w(TAG, "encrypt: Set transformation beforehand");
            throw new CryptoException(
                    "Calling sequence failure", null);
        }
        return originalData; // Just return given data as is.
    }

    /**
     * decrypt -- Decrypts given data with predefined algorithm.
     * 
     * @param encryptedData  -- the target data
     * @param password  -- a password to extract a secret key on the fly
     * @return byte[] -- original data
     * @throws CryptoException  -- Parameter error, cryptographic calculation mismatch, etc.
     */
    public byte[] decrypt(@NonNull byte[] encryptedData, @NonNull String password)
            throws CryptoException {
        if (mTransformation == null) {
            Log.w(TAG, "decrypt: Set transformation beforehand");
            throw new CryptoException(
                    "Calling sequence failure", null);
        }
        return encryptedData; // Just return given data as is.
    }
}
