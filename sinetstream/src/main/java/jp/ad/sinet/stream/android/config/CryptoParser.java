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

package jp.ad.sinet.stream.android.config;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import jp.ad.sinet.stream.android.api.InvalidConfigurationException;

public class CryptoParser extends BaseParser {

    /* Entry point */
    public void parse(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        parseCrypto(myParams);
        parseCryptoDebug(myParams);
    }

    /*
     * Crypto part
     */
    private Boolean mCrypto = null;
    public void parseCrypto(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "crypto"; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mCrypto = true;
            parseAlgorithm(parent);
            parseKeyLength(parent);
            parseMode(parent);
            parsePadding(parent);
            parsePassword(parent);
            parseKeyDerivation(parent);
        }
    }

    @Nullable
    public final Boolean hasCrypto() {
        return mCrypto;
    }

    private String mAlgorithm = null;
    private void parseAlgorithm(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "algorithm"; /* Mandatory */
        String parsedValue = super.parseString(myParams, key, true);
        if (parsedValue != null && !parsedValue.equalsIgnoreCase("AES")) {
            throw new InvalidConfigurationException(
                    key + "(" + parsedValue + "): Unsupported value", null);
        }
        mAlgorithm = parsedValue;
    }

    @Nullable
    public final String getAlgorithm() {
        return mAlgorithm;
    }

    private Integer mKeyLength = 128; /* default */
    private void parseKeyLength(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "key_length"; /* Optional */
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue != null) {
            if (parsedValue instanceof Integer) {
                Integer probe = (Integer) parsedValue;
                switch (probe) {
                    case 128:
                    case 192:
                    case 256:
                        mKeyLength = probe;
                        break;
                    default:
                        throw new InvalidConfigurationException(
                                key + "(" + parsedValue + "): Out of range", null);
                }
            }
        }
    }

    @Nullable
    public final Integer getKeyLength() {
        return mKeyLength;
    }

    private String mMode = null;
    private void parseMode(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "mode"; /* Mandatory */
        String parsedValue = super.parseString(myParams, key, true);
        if (parsedValue != null) {
            /*
             * Cipher Algorithm Modes
             * https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#pkcs5Pad
             */
            switch (parsedValue) {
                case "CBC":
                    /* NOTYET SUPPORTED
                case "OFB":
                case "CTR":
                case "EAX":
                     */
                case "GCM":
                    mMode = parsedValue;
                    break;
                default:
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Unknown value", null);
            }
        }
    }

    @Nullable
    public final String getMode() {
        return mMode;
    }

    private String mPadding = "NoPadding"; /* default */
    private void parsePadding(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "padding"; /* Optional */
        String parsedValue = super.parseString(myParams, key, false);
        if (parsedValue != null) {
            /*
             * Cipher Algorithm Padding
             * https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#pkcs5Pad
             */
            switch (parsedValue) {
                case "none":
                    mPadding = "NoPadding";
                    break;
                case "pkcs7":
                    mPadding = "PKCS5Padding";
                    break;

                /* Future work */
                case "NoPadding":
                case "ISO10126Padding":
                case "OAEPPadding":
                case "PKCS1Padding":
                case "PKCS5Padding":
                case "SSL3Padding":
                    mPadding = parsedValue;
                    break;
                default:
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Unknown value", null);
            }
        }
    }

    @Nullable
    public final String getPadding() {
        return mPadding;
    }

    private String mPassword = null;
    private void parsePassword(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "password"; /* Mandatory */
        String parsedValue = super.parseString(myParams, key, true);
        if (parsedValue != null) {
            mPassword = parsedValue;
        }
    }

    @Nullable
    public final String getPassword() {
        return mPassword;
    }

    private Boolean mKeyDerivation = null;
    public void parseKeyDerivation(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "key_derivation"; /* Optional */
        Map<String,Object> parent = super.parseMap(myParams, key, false);
        if (parent != null) {
            mKeyDerivation = true;
            parseKeyDerivationAlgorithm(parent);
            parseSaltBytes(parent);
            parseIteration(parent);
        }
    }

    @Nullable
    public final Boolean hasKeyDerivation() {
        return mKeyDerivation;
    }

    private String mKeyDerivationAlgorithm = "PBKDF2WithHmacSHA256"; /* default */
    private void parseKeyDerivationAlgorithm(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "algorithm"; /* Optional */
        String parsedValue = super.parseString(myParams, key, false);
        if (parsedValue != null) {
            /*
             * https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SecretKeyFactory
             */
            switch (parsedValue) {
                case "pbkdf2":
                    mKeyDerivationAlgorithm = "PBKDF2WithHmacSHA256";
                    break;

                /* Future work */
                case "AES":
                case "ARCFOUR":
                case "DES":
                case "DESede":
                case "PBEWithMD5AndDES":
                case "PBKDF2WithHmacSHA1":
                case "PBKDF2withHmacSHA1And8BIT":
                case "PBKDF2WithHmacSHA224":
                case "PBKDF2WithHmacSHA256":
                case "PBKDF2WithHmacSHA384":
                case "PBKDF2WithHmacSHA512":
                    mKeyDerivationAlgorithm = parsedValue;
                    break;
                default:
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Unsupported value", null);
            }
        }
    }

    @Nullable
    public final String getKeyDerivationAlgorithm() {
        return mKeyDerivationAlgorithm;
    }

    private Integer mSaltBytes = 8; /* default */
    private void parseSaltBytes(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "salt_bytes"; /* Optional */
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue != null) {
            if (parsedValue instanceof Integer) {
                Integer probe = (Integer) parsedValue;
                if (probe > 0) {
                    mSaltBytes = probe;
                } else {
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Out of range", null);
                }
            }
        }
    }

    @Nullable
    public final Integer getSaltBytes() {
        return mSaltBytes;
    }

    private Integer mIteration = 10000; /* default */
    private void parseIteration(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "iteration"; /* Optional */
        Number parsedValue = super.parseNumber(myParams, key, false);
        if (parsedValue != null) {
            if (parsedValue instanceof Integer) {
                Integer probe = (Integer) parsedValue;
                if (probe > 0) {
                    mIteration = probe;
                } else {
                    throw new InvalidConfigurationException(
                            key + "(" + parsedValue + "): Out of range", null);
                }
            }
        }
    }

    @Nullable
    public final Integer getIteration() {
        return mIteration;
    }

    private Boolean mCryptoDebugEnabled = null;
    private void parseCryptoDebug(@NonNull Map<String,Object> myParams)
            throws InvalidConfigurationException {
        String key = "crypto_debug"; /* Optional */
        mCryptoDebugEnabled = super.parseBoolean(myParams, key, false);
    }

    @Nullable
    public final Boolean getCryptoDebug() {
        return mCryptoDebugEnabled;
    }
}
