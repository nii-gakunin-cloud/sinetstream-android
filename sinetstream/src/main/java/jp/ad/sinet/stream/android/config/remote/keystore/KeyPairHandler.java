/*
 * Copyright (C) 2022 National Institute of Informatics
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

import android.annotation.SuppressLint;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class KeyPairHandler {
    private final String TAG = KeyPairHandler.class.getSimpleName();

    private static final String KEY_PROVIDER = "AndroidKeyStore";

    private KeyStore mKeyStore = null;

    private final KeyPairHandlerListener mListener;
    private final FingerprintHandler mFingerprintHandler;

    private boolean mHeteroPlatform = true;
    private boolean mUseOaepHashSha1 = true;

    public KeyPairHandler(@NonNull KeyPairHandlerListener listener) {
        this.mListener = listener;
        this.mFingerprintHandler = new FingerprintHandler(
                new FingerprintHandler.FingerprintHandlerListener() {
                    @Override
                    public void onError(@NonNull String description) {
                        listener.onError(description);
                    }
                }
        );

        loadAndroidKeyStore();
    }

    private void loadAndroidKeyStore() {
        KeyStore ks;
        try {
            ks = KeyStore.getInstance(KEY_PROVIDER);
        } catch (KeyStoreException e) {
            mListener.onError(TAG +
                    ": KeyStore.getInstance(" + KEY_PROVIDER + "): " + e);
            return;
        }

        try {
            ks.load(null);
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            mListener.onError(TAG + ": KeyStore.load(null): " + e);
        }

        /*
         * KeyStore is just a container and can have entries with following types.
         *   KeyStore.PrivateKeyEntry
         *   KeyStore.SecretKeyEntry
         *   KeyStore.TrustedCertificateEntry
         */
        mKeyStore = ks;
    }

    /**
     * Create a new key pair if needed.
     * <p>
     *     Create RSA key pair for encryption/decryption using RSA OAEP.
     *     See {@link KeyGenParameterSpec} for details.
     * </p>
     *
     * @param alias key alias
     */
    public void createOrReusePublicKey(@NonNull String alias) {
        try {
            if (mKeyStore.containsAlias(alias)) {
                reuseKeyPair(alias);
            } else {
                createKeyPair(alias);
            }
        } catch (KeyStoreException e) {
            mListener.onError(TAG + ": KeyStore.containsAlias: " +
                    "alias(" + alias + "): " + e);
        }
    }

    private void reuseKeyPair(@NonNull String alias) {
        PublicKey publicKey = lookupPublicKey(alias);
        if (publicKey != null) {
            String publicKeyString = publicKeyToString(publicKey);
            if (publicKeyString != null) {
                mListener.onPublicKey(alias, publicKeyString);
            }
        }
    }

    private void createKeyPair(@NonNull String alias) {
        /*
         * KeyGenParameterSpec
         *
         * Example: RSA key pair for encryption/decryption using RSA OAEP
         * https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html#example:-rsa-key-pair-for-encryptiondecryption-using-rsa-oaep
         */
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, KEY_PROVIDER);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            mListener.onError(TAG + ": KeyPairGenerator.getInstance(" +
                    KeyProperties.KEY_ALGORITHM_RSA + ", " + KEY_PROVIDER +
                    "): " + e);
            return;
        }

        KeyGenParameterSpec.Builder builder =
                new KeyGenParameterSpec.Builder(alias,
                        KeyProperties.PURPOSE_ENCRYPT |
                                KeyProperties.PURPOSE_DECRYPT);

        if (mUseOaepHashSha1) {
            builder.setDigests(
                    KeyProperties.DIGEST_SHA1);
        } else {
            builder.setDigests(
                    KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512);
        }
        builder.setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_RSA_OAEP);

        KeyGenParameterSpec spec = builder.build();
        try {
            generator.initialize(spec);
        } catch (InvalidAlgorithmParameterException | UnsupportedOperationException e) {
            mListener.onError(TAG + ": KeyPairGenerator.initialize: " + e);
            return;
        }

        KeyPair keyPair = generator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        //PrivateKey privateKey = keyPair.getPrivate(); // Don't expose private key

        String publicKeyString = publicKeyToString(publicKey);
        if (publicKeyString != null) {
            mListener.onPublicKey(alias, publicKeyString);
        }
    }

    /**
     * Delete the specified key pair from KeyStore.
     *
     * @param alias key alias
     */
    public void deleteKeyPair(@NonNull String alias) {
        try {
            if (! mKeyStore.containsAlias(alias)) {
                mListener.onError(TAG + ": KeyStore: " +
                        "Unknown alias(" + alias + ")");
                return;
            }
        } catch (KeyStoreException e) {
            mListener.onError(TAG + ": KeyStore.containsAlias: " +
                    "alias(" + alias + "): " + e);
            return;
        }

        try {
            mKeyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            mListener.onError(TAG + ": KeyStore.deleteEntry: " +
                    "alias(" + alias + "): " + e);
        }
    }

    /**
     * Get the list of key pair aliases from KeyStore.
     */
    public void listAliases() {
        final Enumeration<String> allAliasNames;
        try {
            allAliasNames = mKeyStore.aliases();
        } catch (KeyStoreException e) {
            mListener.onError(TAG + ": KeyStore.aliases: " + e);
            return;
        }

        ArrayList<String> filteredAliases = new ArrayList<>();
        while (allAliasNames.hasMoreElements()) {
            String alias = allAliasNames.nextElement();
            try {
                if (mKeyStore.entryInstanceOf(
                        alias, KeyStore.PrivateKeyEntry.class)) {
                    filteredAliases.add(alias);
                }
            } catch (KeyStoreException e) {
                mListener.onError(TAG + ": KeyStore.entryInstanceOf: " + e);
                return;
            }
        }
        mListener.onAliasNames(filteredAliases.toArray(new String[0]));
    }

    /**
     * Calculate the public-key fingerprint.
     * <p>
     *     In public-key cryptography, a public key fingerprint is a short
     *     sequence of bytes used to identify a longer public key.
     *     See <a href="https://en.wikipedia.org/wiki/Public_key_fingerprint">Public key fingerprint</a>
     *     for details.
     * </p>
     *
     * @param alias key alias
     * @return fingerprint, or {@code null} on failure.
     */
    @Nullable
    public String calcFingerprint(@NonNull String alias) {
        String fingerprint = null;
        try {
            if (mKeyStore.entryInstanceOf(
                    alias, KeyStore.PrivateKeyEntry.class)) {
                PublicKey publicKey = lookupPublicKey(alias);
                if (publicKey == null) {
                    /* mListener.onError() has already called */
                    return null;
                }
                String publicKeyString = publicKeyToString(publicKey);
                if (publicKeyString != null) {
                    fingerprint = mFingerprintHandler.calc(publicKeyString);
                }
            } else {
                mListener.onError(TAG + ": Not a KeyStore.PrivateKeyEntry: " +
                        alias + ")");
                return null;
            }
        } catch (KeyStoreException e) {
            mListener.onError(TAG + ": KeyStore.entryInstanceOf: " + e);
            return null;
        }
        return fingerprint;
    }

    @Nullable
    private PrivateKey lookupPrivateKey(@NonNull String alias) {
        PrivateKey privateKey = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Key key;
            try {
                key = mKeyStore.getKey(alias, null);
            } catch (KeyStoreException |
                    NoSuchAlgorithmException |
                    UnrecoverableKeyException e) {
                mListener.onError(TAG + ": KeyStore.getKey: " +
                        "alias(" + alias + "): " + e);
                return null;
            }

            if (key != null) {
                if (key instanceof PrivateKey) {
                    privateKey = (PrivateKey) key;
                } else {
                    mListener.onError(TAG + ": KeyStore.getKey: " +
                            "alias(" + alias + "): " + "Not a PrivateKey");
                }
            } else {
                mListener.onError(TAG + ": alias(" + alias + "): " +
                        "KeyStore entry not found");
            }
        } else {
            KeyStore.Entry entry;
            try {
                entry = mKeyStore.getEntry(alias, null);
            } catch (KeyStoreException |
                    NoSuchAlgorithmException |
                    UnrecoverableEntryException e) {
                mListener.onError(TAG + ": KeyStore.getEntry: " +
                        "alias(" + alias + "): " + e);
                return null;
            }

            if (entry != null) {
                if (entry instanceof KeyStore.PrivateKeyEntry) {
                    KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) entry;
                    privateKey = pkEntry.getPrivateKey();
                } else {
                    mListener.onError(TAG + ": KeyStore.getEntry: " +
                            "alias(" + alias + "): " + "Not a PrivateKeyEntry");
                }
            } else {
                mListener.onError(TAG + ": alias(" + alias + "): " +
                        "KeyStore entry not found");
            }
        }
        return privateKey;
    }

    @Nullable
    private PublicKey lookupPublicKey(@NonNull String alias) {
        PublicKey publicKey = null;
        KeyStore.Entry entry;
        try {
            entry = mKeyStore.getEntry(alias, null);
        } catch (KeyStoreException |
                NoSuchAlgorithmException |
                UnrecoverableEntryException e) {
            mListener.onError(TAG + ": KeyStore.getEntry: " +
                    "alias(" + alias + "): " + e);
            return null;
        }

        if (entry != null) {
            if (entry instanceof KeyStore.PrivateKeyEntry) {
                KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) entry;
                Certificate certificate = pkEntry.getCertificate();
                publicKey = certificate.getPublicKey();
            } else {
                mListener.onError(TAG + ": KeyStore.getEntry: " +
                        "alias(" + alias + "): " + "Not a PrivateKeyEntry");
            }
        } else {
            mListener.onError(TAG + ": alias(" + alias + "): " +
                    "KeyStore entry not found");
        }
        return publicKey;
    }

    @Nullable
    private String publicKeyToString(@NonNull PublicKey publicKey) {
        String base64String = null;
        byte[] bytes = publicKey.getEncoded();
        if (bytes != null) {
            base64String = Base64.encodeToString(bytes, Base64.NO_WRAP);
        } else {
            mListener.onError(TAG + ": PublicKey.getEncoded: " +
                    "This Key does not support encoding?");
        }
        return base64String;
    }

    /**
     * Get the specified public key, as a base64-encoded string.
     *
     * @param alias key alias
     * @return base64-encoded string, or {@code null} on failure.
     */
    @Nullable
    public String getBase64EncodedPublicKey(@NonNull String alias) {
        PublicKey publicKey = lookupPublicKey(alias);
        if (publicKey != null) {
            return publicKeyToString(publicKey);
        }
        return null;
    }

    /**
     * Get the detailed information of the public key.
     *
     * @param alias key alias
     * @return the attributes of the public key, or {@code null} on failure.
     */
    @Nullable
    public String getPublicKeyInfo(@NonNull String alias) {
        PublicKey publicKey = lookupPublicKey(alias);
        if (publicKey != null) {
            if (mDebugEnabled) {
                Log.d(TAG, "Alias(" + alias + ") => PublicKey: " + publicKey);
            }
            String info = "";

            info += "Format:\n" + publicKey.getFormat() + "\n";
            info += "\n";
            info += "Algorithm:\n" + publicKey.getAlgorithm() + "\n";
            info += "\n";

            String publicKeyString = publicKeyToString(publicKey);
            if (publicKeyString != null) {
                String fingerprint = mFingerprintHandler.calc(publicKeyString);
                if (fingerprint == null) {
                    /* mListener.onError() has already called */
                    return null;
                }
                info += "Fingerprint:\n" + fingerprint + "\n";
                info += "\n";
            }

            if (publicKey instanceof RSAPublicKey) {
                RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
                if (mDebugEnabled) {
                    Log.d(TAG, "RSAPublicKey: " + rsaPublicKey);
                }
                BigInteger publicExponent = rsaPublicKey.getPublicExponent();
                info += "[RSA] PublicExponent (" + publicExponent.bitLength() + " bit): " +
                        publicExponent + "\n";
                info += "\n";
                BigInteger modulus = rsaPublicKey.getModulus();
                info += "[RSA] Modulus (" + modulus.bitLength() + " bit):\n" +
                        modulus + "\n";
                info += "\n";
            }
            info += "Base64Encoded:\n" + publicKeyToString(publicKey);
            return info;
        }
        return null;
    }

    /**
     * Get the size of public key
     *
     * @param alias key alias
     * @return key size in bits, or {@code null} on failure.
     */
    @Nullable
    public Integer getPublicKeySize(@NonNull String alias) {
        PublicKey publicKey = lookupPublicKey(alias);
        if (publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            return rsaPublicKey.getModulus().bitLength();
        }
        return null;
    }

    /**
     * Encrypt the given plain text
     *
     * @param alias key alias
     * @param plainText string to be encrypted
     *
     * @return base64-encoded cipher text, or {@code null} on failure.
     */
    @Nullable
    public String encryptString(
            @NonNull String alias,
            @NonNull String plainText) {
        byte [] bytes = encryptBytes(alias,
                plainText.getBytes(StandardCharsets.UTF_8));
        if (bytes != null) {
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }
        return null;
    }

    /**
     * Encrypt the given byte array
     *
     * @param alias key alias
     * @param inputBytes byte array to be encrypted
     *
     * @return encrypted byte array, or {@code null} on failure.
     */
    @SuppressLint("ObsoleteSdkInt")
    @Nullable
    public byte[] encryptBytes(
            @NonNull String alias,
            @NonNull byte[] inputBytes) {
        PublicKey publicKey = lookupPublicKey(alias);
        if (publicKey == null) {
            /* mListener.onError() has already called */
            return null;
        }

        Cipher cipher;
        String transformation = getTransformation();
        try {
            cipher = Cipher.getInstance(transformation); /* Never be null */
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            mListener.onError(TAG + ": Cipher.getInstance(" +
                    transformation + "): " + e);
            return null;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                OAEPParameterSpec spec = buildOaepParameterSpec();
                cipher.init(Cipher.ENCRYPT_MODE, publicKey, spec);
                if (mDebugEnabled) {
                    dumpCipherParameters(cipher, spec);
                }
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                if (mDebugEnabled) {
                    dumpCipherParameters(cipher, null);
                }
            }
        } catch (UnsupportedOperationException |
                InvalidKeyException |
                InvalidAlgorithmParameterException e) {
            mListener.onError(TAG + ": Cipher.init(ENCRYPT_MODE): " + e);
            return null;
        }

        byte[] outputBytes;
        try {
            outputBytes = cipher.doFinal(inputBytes);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            mListener.onError(TAG + ": Cipher.doFinal(ENCRYPT_MODE): " + e);
            return null;
        }
        return outputBytes;
    }

    /**
     * Decrypt the given cipher text
     *
     * @param alias key alias
     * @param encryptedText base64-encoded cipher text
     *
     * @return original plain text, or {@code null} on failure.
     */
    @Nullable
    public String decryptString(
            @NonNull String alias,
            @NonNull String encryptedText) {
        byte[] bytes = decryptBytes(alias, Base64.decode(encryptedText, Base64.DEFAULT));
        if (bytes != null) {
            return new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Decrypt the given byte array
     *
     * @param alias key alias
     * @param inputBytes cipher data
     *
     * @return original byte array, or {@code null} on failure.
     */
    @SuppressLint("ObsoleteSdkInt")
    @Nullable
    public byte[] decryptBytes(
            @NonNull String alias,
            @NonNull byte[] inputBytes) {
        PrivateKey privateKey = lookupPrivateKey(alias);
        if (privateKey == null) {
            /* mListener.onError() has already called */
            return null;
        }

        Cipher cipher;
        String transformation = getTransformation();
        try {
            cipher = Cipher.getInstance(transformation); /* Never be null */
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            mListener.onError(TAG + ": Cipher.getInstance(" +
                    transformation + "): " + e);
            return null;
        }

        /*
         * Cipher.init() optionally takes the algorithm-specific parameter.
         * Though it may not needed in DECRYPT mode, we intentionally
         * set it for operational clarity.
         */
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                OAEPParameterSpec spec = buildOaepParameterSpec();
                cipher.init(Cipher.DECRYPT_MODE, privateKey, spec);
                dumpCipherParameters(cipher, spec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                dumpCipherParameters(cipher, null);
            }
        } catch (UnsupportedOperationException |
                InvalidKeyException |
                InvalidAlgorithmParameterException e) {
            mListener.onError(TAG + ": Cipher.init(DECRYPT_MODE): " + e);
            return null;
        }

        byte[] outputBytes;
        try {
            outputBytes = cipher.doFinal(inputBytes);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            mListener.onError(TAG + ": Cipher.doFinal(DECRYPT_MODE): " + e);
            return null;
        }
        return outputBytes;
    }

    @SuppressLint("ObsoleteSdkInt")
    private String getTransformation() {
        String transformation = "";

        /*
         * Extracting and encrypting using public key out of AndroidKeyStore
         * results in IllegalBlockSizeException during decryption
         * -- Bouncy Castle RSA OAEP implementation incompatible with RI and
         *    Android Keystore
         * https://issuetracker.google.com/issues/37075898
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            transformation += KeyProperties.KEY_ALGORITHM_RSA + "/";
            transformation += KeyProperties.BLOCK_MODE_ECB + "/";
            if (mUseOaepHashSha1) {
                //transformation += KeyProperties.ENCRYPTION_PADDING_RSA_OAEP;
                transformation += "OAEPWithSHA-1AndMGF1Padding";
            } else {
                transformation += "OAEPWithSHA-256AndMGF1Padding";
            }
        } else {
            transformation += KeyProperties.KEY_ALGORITHM_RSA + "/";
            transformation += KeyProperties.BLOCK_MODE_ECB + "/";
            transformation += KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
        }
        return transformation;
    }

    @NonNull
    private OAEPParameterSpec buildOaepParameterSpec() {
        OAEPParameterSpec spec;
        if (mHeteroPlatform) {
            /*
             * For interoperability with the crypto implementation in NodeJS,
             * we need to use the IDENTICAL hash function (SHA-X) both for
             * the OAEP and MGF1 [1][2].
             *
             * [1] Different encryption Android vs pure Java
             *     - RSA/ECB/OAEPWithMD5AndMGF1Padding
             * https://stackoverflow.com/questions/62099069
             *
             * [2] Java Decrypt with NodeJS Encrypt <RSA_PKCS1_OAEP_PADDING>
             *     padding and <sha256> oaepHash
             * https://stackoverflow.com/questions/67389228
             */
            if (mUseOaepHashSha1) {
                /*
                 * Though SHA1 is not optimal nowadays, we have no choice
                 * for interoperability with NodeJS.
                 */
                spec = new OAEPParameterSpec(
                        KeyProperties.DIGEST_SHA1, "MGF1",
                        MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            } else {
                /*
                 * Cipher.init(DECRYPT_MODE) fails with MGF1ParameterSpec.SHA256 [1],
                 * which contradicts with Android developers guide [2].
                 *
                 * [1] javax.crypto.IllegalBlockSizeException when decrypting data
                 *     using Android KeyStore RSA private key
                 * https://issuetracker.google.com/issues/36708951#comment23
                 *
                 * [2] Implementation complexities -- OAEP MGF1 message digest
                 * https://developer.android.com/guide/topics/security/cryptography#oaep-mgf1-digest
                 */
                /*
                 * Therefore, this combination does NOT work until Android's
                 * crypto implementation supports SHA-256 for MGF1 digest.
                 */
                spec = new OAEPParameterSpec(
                        KeyProperties.DIGEST_SHA256, "MGF1",
                        MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            }
        } else {
            spec = new OAEPParameterSpec(
                    KeyProperties.DIGEST_SHA256, "MGF1",
                    MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
        }
        return spec;
    }

    private void dumpCipherParameters(@NonNull Cipher cipher,
                                      @Nullable OAEPParameterSpec oaepParameterSpec) {
        String s = "Cipher:\n";
        s += "Provider: {"
                + cipher.getProvider().getName() + ", "
                + cipher.getProvider().getVersion() + ", "
                + cipher.getProvider().getInfo()
                + "}\n";
        s += "Algorithm: " + cipher.getAlgorithm() + "\n";
        /*
         * Cipher.getBlockSize() must be called AFTER initialization.
         * s += "BlockSize: " + cipher.getBlockSize() + "\n";
         */

        if (oaepParameterSpec != null) {
            s += "OAEP digest: " + oaepParameterSpec.getDigestAlgorithm() + "\n";
            s += "OAEP MGF : " + oaepParameterSpec.getMGFAlgorithm() + "\n";
            s += "OAEP MGF1: " + ((MGF1ParameterSpec)oaepParameterSpec.getMGFParameters()).
                    getDigestAlgorithm();
        }
        Log.d(TAG, s);
    }

    public void setHeteroPlatform(boolean enabled) {
        mHeteroPlatform = enabled;
    }

    public void setUseOaepHashSha1(boolean enabled) {
        mUseOaepHashSha1 = enabled;
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;

        /* Relay developer option */
        mFingerprintHandler.enableDebug(mDebugEnabled);
    }

    public interface KeyPairHandlerListener {
        void onAliasNames(@NonNull String[] aliases);
        void onPublicKey(@NonNull String alias, @NonNull String base64String);
        void onError(@NonNull String description);
    }
}
