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

public class CipherHandler {
    private final String TAG = CipherHandler.class.getSimpleName();

    private final int mKeyLength;
    private final String mKeyDerivationAlgorithm;
    private final String mCipherAlgorithm;
    private final int mSaltLength;
    private final int mIterationCount;

    private String mFeedbackMode;
    private CipherModeCBC mCipherModeCBC = null;
    private CipherModeGCM mCipherModeGCM = null;

    public CipherHandler(int keyLength,
                         @NonNull String keyDerivationAlgorithm,
                         @NonNull String cipherAlgorithm,
                         int saltLength,
                         int iterationCount)
            throws CryptoException {
        this.mKeyLength = keyLength;
        this.mKeyDerivationAlgorithm = keyDerivationAlgorithm;
        this.mCipherAlgorithm = cipherAlgorithm;
        this.mSaltLength = saltLength;
        this.mIterationCount = iterationCount;
    }

    public void setTransformation(@NonNull String cipherAlgorithm,
                                  @NonNull String feedbackMode,
                                  @NonNull String paddingScheme)
            throws CryptoException {
        mFeedbackMode = feedbackMode;
        switch (mFeedbackMode) {
            case "CBC":
                mCipherModeCBC = new CipherModeCBC(
                        mKeyLength,
                        mKeyDerivationAlgorithm,
                        mCipherAlgorithm,
                        mSaltLength,
                        mIterationCount);
                mCipherModeCBC.setTransformation(cipherAlgorithm, feedbackMode, paddingScheme);
                break;
            case "GCM":
                mCipherModeGCM = new CipherModeGCM(
                        mKeyLength,
                        mKeyDerivationAlgorithm,
                        mCipherAlgorithm,
                        mSaltLength,
                        mIterationCount);
                mCipherModeGCM.setTransformation(cipherAlgorithm, feedbackMode, paddingScheme);
                break;
            default:
                Log.w(TAG, "transformation: Unknown mode: " + feedbackMode);
                throw new CryptoException(
                        "transformation: Unknown mode: " + feedbackMode, null);
        }
    }

    public byte[] encrypt(byte[] originalData, @NonNull String password)
            throws CryptoException {
        byte[] encryptedData;
        switch (mFeedbackMode) {
            case "CBC":
                encryptedData = mCipherModeCBC.encrypt(originalData, password);
                break;
            case "GCM":
                encryptedData = mCipherModeGCM.encrypt(originalData, password);
                break;
            default:
                throw new CryptoException(
                        "encrypt: Invalid calling sequence", null);
        }
        return encryptedData;
    }

    public byte[] decrypt(@NonNull byte[] encryptedData, @NonNull String password)
            throws CryptoException {
        byte[] originalData;
        switch (mFeedbackMode) {
            case "CBC":
                originalData = mCipherModeCBC.decrypt(encryptedData, password);
                break;
            case "GCM":
                originalData = mCipherModeGCM.decrypt(encryptedData, password);
                break;
            default:
                throw new CryptoException(
                        "decrypt: Invalid calling sequence", null);
        }
        return originalData;
    }
}
