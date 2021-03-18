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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import jp.ad.sinet.stream.android.api.CryptoException;

public class CipherModeCBC extends CipherBase {
    private final String TAG = CipherModeCBC.class.getSimpleName();

    /**
     * Constructor -- Allocates a KeyUtil instance
     *
     * @param keyLength              -- Byte length of a secret key to be generated
     * @param keyDerivationAlgorithm -- Secret key derivation algorithm name,
     *                               such like PBKDF2WithHmacSHA256
     * @param cipherAlgorithm        -- Cipher algorithm name, such like AES
     * @param iterationCount         -- Iteration count to be used in block mode
     * @throws CryptoException -- Invalid parameter cases
     */
    public CipherModeCBC(int keyLength,
                         @NonNull String keyDerivationAlgorithm,
                         @NonNull String cipherAlgorithm,
                         int iterationCount)
            throws CryptoException {
        super(keyLength, keyDerivationAlgorithm, cipherAlgorithm, iterationCount);
    }

    /**
     * Set transformation by parameter tuple {algorithm, mode, padding}
     *
     * @param cipherAlgorithm -- Cipher algorithm name, such like AES
     * @param feedbackMode    -- Feedback mode of the algorithm, such like CBC
     * @param paddingScheme   -- Padding scheme of the algorithm, such like PKCS5Padding
     * @throws CryptoException -- Invalid parameter cases
     */
    @Override
    public void setTransformation(@NonNull String cipherAlgorithm,
                                  @NonNull String feedbackMode,
                                  @NonNull String paddingScheme)
            throws CryptoException {
        super.setTransformation(cipherAlgorithm, feedbackMode, paddingScheme);
    }

    /**
     * encrypt -- Encrypts given data with predefined algorithm.
     *
     * @param originalData -- original data
     * @param password     -- a password to generate a secret key on the fly
     * @return byte[] -- encrypted data
     * @throws CryptoException -- Parameter error, cryptographic calculation mismatch, etc.
     */
    @Override
    public byte[] encrypt(byte[] originalData, @NonNull String password)
            throws CryptoException {
        super.encrypt(originalData, password);

        /* Uncomment only in debug phase
        Log.d(TAG, "ENCRYPT: " +
                "originalData(" + originalData.length + ")" + Arrays.toString(originalData));
         */

        /*
         * Allocate a Cipher instance for encryption.
         */
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(mTransformation);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.w(TAG, "ENCRYPT: Cipher(" + mTransformation + "): " + e.getMessage());
            throw new CryptoException(
                    "ENCRYPT: Cipher(" + mTransformation + "): " + e.getMessage(), e.getCause());
        }

        /*
         * Prepare secret key and IV parameter spec for encryption.
         */
        byte[] salt = mKeyUtil.generateSalt();
        SecretKey secretKey = mKeyUtil.getSecretKeyByPassword(password, salt);
        IvParameterSpec ivParameterSpec = mKeyUtil.getIvParameterSpec(cipher);
        /* Uncomment only in debug phase
        if (ivParameterSpec != null) {
            Log.d(TAG, "ENCRYPT: IV(" + ivParameterSpec.toString() + ")");
        } else {
            Log.d(TAG, "ENCRYPT: IV(null)");
        }
         */

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            Log.w(TAG, "ENCRYPT: Cipher.init(): " + e.getMessage());
            throw new CryptoException(
                    "ENCRYPT: Cipher.init(): " + e.getMessage(), e.getCause());
        }

        byte[] opaque;
        try {
            opaque = cipher.doFinal(originalData);
        } catch (BadPaddingException | IllegalBlockSizeException | IllegalStateException e) {
            Log.w(TAG, "ENCRYPT: Cipher.doFinal(): " + e.getMessage());
            throw new CryptoException(
                    "ENCRYPT: Cipher.doFinal(): " + e.getMessage(), e.getCause());
        }

        /*
         * Assemble elements as the encrypted data.
         *
         *    |<--------  encryptedData  ------->|
         *    +------+----+----------------------+
         *    | salt | iv |        opaque        |
         *    +------+----+----------------------+
         */
        byte[] iv = cipher.getIV();
        byte[] encryptedData = new byte[salt.length + iv.length + opaque.length];

        int offset = 0;
        System.arraycopy(salt, 0, encryptedData, offset, salt.length);
        offset += salt.length;
        System.arraycopy(iv, 0, encryptedData, offset, iv.length);
        offset += iv.length;
        System.arraycopy(opaque, 0, encryptedData, offset, opaque.length);

        /* Uncomment only in debug phase
        Log.d(TAG, "ENCRYPT: salt(" + salt.length + ")" + Arrays.toString(salt));
        Log.d(TAG, "ENCRYPT: iv(" + iv.length + ")" + Arrays.toString(iv));
        Log.d(TAG, "ENCRYPT: opaque(" + opaque.length + ")" + Arrays.toString(opaque));
         */

        return encryptedData;
    }

    /**
     * decrypt -- Decrypts given data with predefined algorithm.
     *
     * @param encryptedData -- the target data
     * @param password      -- a password to extract a secret key on the fly
     * @return byte[] -- original data
     * @throws CryptoException -- Parameter error, cryptographic calculation mismatch, etc.
     */
    @Override
    public byte[] decrypt(@NonNull byte[] encryptedData, @NonNull String password)
            throws CryptoException {
        super.decrypt(encryptedData, password);

        /*
         * Allocate a Cipher instance for decryption.
         */
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(mTransformation);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.w(TAG, "DECRYPT: Cipher(" + mTransformation + "): " + e.getMessage());
            throw new CryptoException(
                    "DECRYPT: Cipher(" + mTransformation + "): " + e.getMessage(), e.getCause());
        }

        /*
         * Disassemble elements from the encrypted data.
         *
         *    |<--------  encryptedData  ------->|
         *    +------+----+----------------------+
         *    | salt | iv |        opaque        |
         *    +------+----+----------------------+
         */
        byte[] salt = new byte[mKeyUtil.getSaltLength()];
        byte[] iv = new byte[cipher.getBlockSize()];
        int buflen = encryptedData.length - (salt.length + iv.length);
        if (buflen <= 0) {
            throw new CryptoException(
                    "DECRYPT: Invalid data length:" + buflen, null);
        }
        byte[] opaque = new byte[buflen];

        int offset = 0;
        System.arraycopy(encryptedData, offset, salt, 0, salt.length);
        offset += salt.length;
        System.arraycopy(encryptedData, offset, iv, 0, iv.length);
        offset += iv.length;
        System.arraycopy(encryptedData, offset, opaque, 0, opaque.length);

        /* Uncomment only in debug phase
        Log.d(TAG, "DECRYPT: salt(" + salt.length + ")" + Arrays.toString(salt));
        Log.d(TAG, "DECRYPT: iv(" + iv.length + ")" + Arrays.toString(iv));
        Log.d(TAG, "DECRYPT: opaque(" + opaque.length + ")" + Arrays.toString(opaque));
         */

        /*
         * Prepare secret key and IV parameter spec for decryption.
         */
        SecretKey secretKey = mKeyUtil.getSecretKeyByPassword(password, salt);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            Log.w(TAG, "DECRYPT: Cipher.init(): " + e.getMessage());
            throw new CryptoException(
                    "DECRYPT: Cipher.init(): " + e.getMessage(), e.getCause());
        }

        byte[] originalData;
        try {
            originalData = cipher.doFinal(opaque);
        } catch (BadPaddingException | IllegalBlockSizeException | IllegalStateException e) {
            Log.w(TAG, "DECRYPT: Cipher.doFinal(): " + e.getMessage());
            throw new CryptoException(
                    "DECRYPT: Cipher.doFinal(): " + e.getMessage(), e.getCause());
        }

        /* Uncomment only in debug phase
        Log.d(TAG, "DECRYPT: originalData(" + originalData.length + ")" + Arrays.toString(originalData));
         */

        return originalData;
    }
}
