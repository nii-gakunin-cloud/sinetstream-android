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
import javax.crypto.spec.GCMParameterSpec;

import jp.ad.sinet.stream.android.api.CryptoException;

public class CipherModeGCM extends CipherBase {
    private final String TAG = CipherModeGCM.class.getSimpleName();

    private final int AUTHTAG_BYTES = 16;
    private final int GCM_IV_BYTES = 16;

    /**
     * Constructor -- Allocates a CipherModeGCM instance
     *
     * @param keyLength -- Byte length of a secret key to be generated
     * @param keyDerivationAlgorithm -- Secret key derivation algorithm name,
     *                               such like "PBKDF2WithHmacSHA256"
     * @param cipherAlgorithm -- Cipher algorithm name, such like "AES"
     * @param saltLength -- Byte length of a cryptographic salt to be generated
     * @param iterationCount -- Iteration count to be used in block mode
     * @throws CryptoException -- Invalid parameter cases
     */
    public CipherModeGCM(int keyLength,
                         @NonNull String keyDerivationAlgorithm,
                         @NonNull String cipherAlgorithm,
                         int saltLength,
                         int iterationCount)
            throws CryptoException {
        super(keyLength, keyDerivationAlgorithm, cipherAlgorithm, saltLength, iterationCount);
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
        Log.d(TAG, "ENCRYPT: originalData(" + originalData.length + ")" + Arrays.toString(originalData));
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
         * Prepare secret key and GCM parameter spec for encryption.
         */
        byte[] salt = mKeyUtil.generateSalt();
        SecretKey secretKey = mKeyUtil.getSecretKeyByPassword(password, salt);

        int tlen = AUTHTAG_BYTES * 8; /* bytes -> bits */
        byte[] iv = mKeyUtil.generateGCMInitializationVector(GCM_IV_BYTES);
        GCMParameterSpec gcmParameterSpec =
                mKeyUtil.getGCMParameterSpec(cipher, tlen, iv);
        /* Uncomment only in debug phase
        if (gcmParameterSpec != null) {
            Log.d(TAG, "ENCRYPT: GCMParameterSpec{" +
                    "tlen(" + gcmParameterSpec.getTLen() + ")," +
                    "iv(" + Arrays.toString(gcmParameterSpec.getIV()) + ")}");
        } else {
            Log.d(TAG, "ENCRYPT: GCMParameterSpec(null)");
        }
         */

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            Log.w(TAG, "ENCRYPT: Cipher.init(): " + e.getMessage());
            throw new CryptoException(
                    "ENCRYPT: Cipher.init(): " + e.getMessage(), e.getCause());
        }

        /*
         * Treat "salt & iv" pair as the associated data.
         */
        try {
            cipher.updateAAD(salt);
            cipher.updateAAD(iv);
        } catch (IllegalArgumentException |
                IllegalStateException |
                UnsupportedOperationException e) {
            Log.w(TAG, "ENCRYPT: Cipher.updateAAD(): " + e.getMessage());
            throw new CryptoException(
                    "ENCRYPT: Cipher.updateAAD(): " + e.getMessage(), e.getCause());
        }

        /*
         * https://developer.android.com/reference/javax/crypto/Cipher#doFinal(byte[])
         *
         * If an AEAD mode such as GCM/CCM is being used, the authentication tag is
         * appended in the case of encryption, or verified in the case of decryption.
         */
        byte[] result;
        try {
            result = cipher.doFinal(originalData);
        } catch (BadPaddingException | IllegalBlockSizeException | IllegalStateException e) {
            Log.w(TAG, "ENCRYPT: Cipher.doFinal(): " + e.getMessage());
            throw new CryptoException(
                    "ENCRYPT: Cipher.doFinal(): " + e.getMessage(), e.getCause());
        }

        /*
         * Split the result into opaque and authentication tag.
         *
         *    |<-----  result  ----->|
         *    +------------+---------+
         *    |   opaque   | authtag |
         *    +------------+---------+
         */
        byte[] opaque = new byte[result.length - AUTHTAG_BYTES];
        byte[] authtag = new byte[AUTHTAG_BYTES];
        System.arraycopy(result, 0, opaque, 0, opaque.length);
        System.arraycopy(result, opaque.length, authtag, 0, authtag.length);

        /*
         * Assemble elements as the encrypted data.
         * NB: Treat "salt & iv" pair as the associated data.
         *
         *    |<---------  encryptedData  -------->|
         *    +------+----+--------------+---------+
         *    | salt | iv |    opaque    | authtag |
         *    +------+----+--------------+---------+
         *    |<-- aad -->|
         */
        iv = cipher.getIV();
        byte[] encryptedData = new byte[
                salt.length + iv.length + opaque.length + authtag.length];

        int offset = 0;
        System.arraycopy(salt, 0, encryptedData, offset, salt.length);
        offset += salt.length;
        System.arraycopy(iv, 0, encryptedData, offset, iv.length);
        offset += iv.length;
        System.arraycopy(opaque, 0, encryptedData, offset, opaque.length);
        offset += opaque.length;
        System.arraycopy(authtag, 0, encryptedData, offset, authtag.length);

        /* Uncomment only in debug phase
        Log.d(TAG, "ENCRYPT: salt(" + salt.length + ")" + Arrays.toString(salt));
        Log.d(TAG, "ENCRYPT: iv(" + iv.length + ")" + Arrays.toString(iv));
        Log.d(TAG, "ENCRYPT: opaque(" + opaque.length + ")" + Arrays.toString(opaque));
        Log.d(TAG, "ENCRYPT: authtag(" + authtag.length + ")" + Arrays.toString(authtag));
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
         * NB: Treat "salt & iv" pair as the associated data.
         *
         *    |<---------  encryptedData  -------->|
         *    +------+----+--------------+---------+
         *    | salt | iv |    opaque    | authtag |
         *    +------+----+--------------+---------+
         *    |<-- aad -->|
         */
        byte[] salt = new byte[mKeyUtil.getSaltLength()];
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] authtag = new byte[AUTHTAG_BYTES];
        int buflen = encryptedData.length - (salt.length + iv.length + authtag.length);
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
        offset += opaque.length;
        System.arraycopy(encryptedData, offset, authtag, 0, authtag.length);

        /* Uncomment only in debug phase
        Log.d(TAG, "DECRYPT: salt(" + salt.length + ")" + Arrays.toString(salt));
        Log.d(TAG, "DECRYPT: iv(" + iv.length + ")" + Arrays.toString(iv));
        Log.d(TAG, "DECRYPT: opaque(" + opaque.length + ")" + Arrays.toString(opaque));
        Log.d(TAG, "DECRYPT: authtag(" + authtag.length + ")" + Arrays.toString(authtag));
         */

        /*
         * Build probe from opaque and authentication tag.
         *
         *    |<-----  probe  ------>|
         *    +------------+---------+
         *    |   opaque   | authtag |
         *    +------------+---------+
         */
        byte[] probe = new byte[opaque.length + authtag.length];
        offset = 0;
        System.arraycopy(opaque, 0, probe, offset, opaque.length);
        offset += opaque.length;
        System.arraycopy(authtag, 0, probe, offset, authtag.length);

        /*
         * Prepare secret key and GCM parameter spec.
         */
        SecretKey secretKey = mKeyUtil.getSecretKeyByPassword(password, salt);

        int tlen = authtag.length * 8; /* bytes -> bits */
        GCMParameterSpec gcmParameterSpec =
                mKeyUtil.getGCMParameterSpec(cipher, tlen, iv);
        /* Uncomment only in debug phase
        if (gcmParameterSpec != null) {
            Log.d(TAG, "DECRYPT: GCMParameterSpec{" +
                    "tlen(" + gcmParameterSpec.getTLen() + ")," +
                    "iv(" + Arrays.toString(gcmParameterSpec.getIV()) + ")}");
        } else {
            Log.d(TAG, "DECRYPT: GCMParameterSpec(null)");
        }
         */

        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            Log.w(TAG, "DECRYPT: Cipher.init(): " + e.getMessage());
            throw new CryptoException(
                    "DECRYPT: Cipher.init(): " + e.getMessage(), e.getCause());
        }

        /*
         * Treat "salt & iv" pair as the associated data.
         */
        try {
            cipher.updateAAD(salt);
            cipher.updateAAD(iv);
        } catch (IllegalArgumentException |
                IllegalStateException |
                UnsupportedOperationException e) {
            Log.w(TAG, "DECRYPT: Cipher.updateAAD(): " + e.getMessage());
            throw new CryptoException(
                    "DECRYPT: Cipher.updateAAD(): " + e.getMessage(), e.getCause());
        }

        /*
         * https://developer.android.com/reference/javax/crypto/Cipher#doFinal(byte[])
         *
         * If an AEAD mode such as GCM/CCM is being used, the authentication tag is
         * appended in the case of encryption, or verified in the case of decryption.
         */
        byte[] originalData;
        try {
            originalData = cipher.doFinal(probe);
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
