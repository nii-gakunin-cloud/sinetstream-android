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

package jp.ad.sinet.stream.android.config.remote.configclient.api;

import android.content.Context;
import android.content.DialogInterface;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jp.ad.sinet.stream.android.config.remote.configclient.api.rest.GetSecret;
import jp.ad.sinet.stream.android.config.remote.configclient.constants.CryptoTypes;
import jp.ad.sinet.stream.android.config.remote.configclient.constants.JsonTags;
import jp.ad.sinet.stream.android.config.remote.configclient.util.DialogUtil;
import jp.ad.sinet.stream.android.config.remote.keystore.KeyPairHandler;

public class SecretHandler {
    private final String TAG = SecretHandler.class.getSimpleName();

    private final Context mContext;
    private final String mConfigServerUrl;
    private final String mAuthToken;
    private final SecretHandlerListener mListener;

    private KeyPairHandler mKeyPairHandler;
    private int chosenItemIndex;
    private String mAlias = null;
    private String mFingerprint = null;

    final int VERSION_BYTES = 2;
    final int PUBLIC_KEY_TYPE_BYTES = 1;
    final int SHARED_KEY_TYPE_BYTES = 1;
    final int HEADER_BYTES = VERSION_BYTES + PUBLIC_KEY_TYPE_BYTES + SHARED_KEY_TYPE_BYTES;

    public SecretHandler(@NonNull Context context,
                         @NonNull String configServerUrl,
                         @NonNull String authToken,
                         @NonNull SecretHandlerListener listener) {
        this.mContext = context;
        this.mConfigServerUrl = configServerUrl;
        this.mAuthToken = authToken;
        this.mListener = listener;
    }

    public void run(@Nullable String dialogLabel) {
        mKeyPairHandler = new KeyPairHandler(new KeyPairHandler.KeyPairHandlerListener() {
            @Override
            public void onAliasNames(@NonNull String[] aliases) {
                if (aliases.length > 0) {
                    pickupAlias(aliases, dialogLabel);
                } else {
                    mListener.onError(TAG + ": KeyStore: Empty aliases?");
                }
            }

            @Override
            public void onPublicKey(@NonNull String alias, @NonNull String base64String) {
                /* Not used here */
            }

            @Override
            public void onError(@NonNull String description) {
                /* Relay error info to the listener */
                mListener.onError(description);
            }
        });

        /* Relay developer option */
        mKeyPairHandler.enableDebug(mDebugEnabled);

        mKeyPairHandler.listAliases();
    }

    private void pickupAlias(@NonNull String[] aliases, @Nullable String dialogLabel) {
        if (aliases.length > 1) {
            chosenItemIndex = 0;
            String title = ((dialogLabel != null) ?
                    ("[" + dialogLabel + "] ") : "") +
                    "KeyStore: Aliases";
            DialogUtil dialogUtil = new DialogUtil(mContext);
            dialogUtil.showSingleChoiceDialog(
                    title,
                    aliases,
                    chosenItemIndex,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            chosenItemIndex = which;
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mAlias = aliases[chosenItemIndex];
                            mFingerprint = mKeyPairHandler.calcFingerprint(mAlias);
                            if (mFingerprint != null) {
                                mListener.onReady(SecretHandler.this);
                            }
                        }
                    });
        } else {
            mAlias = aliases[0];
            mFingerprint = mKeyPairHandler.calcFingerprint(mAlias);
            if (mFingerprint != null) {
                mListener.onReady(SecretHandler.this);
            }
        }
    }

    public void handleConfigSecrets(@NonNull JSONArray secrets) {
        if (mAlias == null || mFingerprint == null) {
            mListener.onError(TAG + ": Calling sequence failure");
            return;
        }

        for (int i = 0, n = secrets.length(); i < n; i++) {
            JSONObject elem;
            try {
                elem = secrets.getJSONObject(i);
            } catch (JSONException e) {
                mListener.onError(TAG + ": " + e.getMessage());
                return;
            }

            if (elem.has(JsonTags.KEY_SECRET_IDS)) {
                handleDataEncryptionKeys(elem);
            } else
            if (elem.has(JsonTags.KEY_SECRET_ID)) {
                handleOtherSecrets(elem);
            }
        }
    }

    private void handleDataEncryptionKeys(@NonNull JSONObject jsonObject) {
        if (mDebugEnabled) {
            Log.d(TAG, "DataEncryptionKeys: SecretIDs: " + jsonObject);
        }
        Log.w(TAG, "DataEncryptionKeys: Not yet supported");
    }

    private void handleOtherSecrets(@NonNull JSONObject jsonObject) {
        String id, target;

        String key = JsonTags.KEY_SECRET_ID;
        if (jsonObject.has(key)) {
            try {
                id = jsonObject.getString(key);
            } catch (JSONException e) {
                mListener.onError(TAG + ": key(" + key + "): " + e.getMessage());
                return;
            }
        } else {
            mListener.onError(TAG + ": key(" + key + ") not found");
            return;
        }

        key = JsonTags.KEY_SECRET_TARGET;
        if (jsonObject.has(key)) {
            try {
                target = jsonObject.getString(key);
            } catch (JSONException e) {
                mListener.onError(TAG +
                        ": Secret{id(" + id + ")}" +
                        ": key(" + key + "): " + e.getMessage());
                return;
            }
        } else {
            mListener.onError(TAG +
                    ": Secret{id(" + id + ")}" +
                    ": key(" + key + ") not found");
            return;
        }

        if (target.endsWith(JsonTags.KEY_ATTACHMENT_TLS_CERTFILE_DATA)) {
            getSecretInfo(id);
        } else {
            /*
             * Given target does not end with the expected suffix.
             * To prevent the listener from infinite waiting, return error here.
             */
            mListener.onError(TAG +
                    ": Secret{id(" + id + "),target(" + target + ")}" +
                    ": Unknown target");
        }
    }

    private void getSecretInfo(@NonNull String secretId) {
        GetSecret getSecret =
                new GetSecret(
                        mContext,
                        mConfigServerUrl,
                        mFingerprint,
                        new GetSecret.GetSecretListener() {
                            @Override
                            public void onSecretInfo(@NonNull JSONObject secret) {
                                parseSecretInfo(secret);
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                /* Relay error info to the listener */
                                mListener.onError(description);
                            }
                        });

        /* Relay developer option */
        getSecret.enableDebug(mDebugEnabled);

        getSecret.run(secretId, mAuthToken);
    }

    private void parseSecretInfo(@NonNull JSONObject jsonObject) {
        String id, target, value;

        String key = JsonTags.KEY_SECRET_ID;
        if (jsonObject.has(key)) {
            try {
                id = jsonObject.getString(key);
            } catch (JSONException e) {
                mListener.onError(TAG + ": key(" + key + "): " + e.getMessage());
                return;
            }
        } else {
            mListener.onError(TAG + ": key(" + key + ") not found");
            return;
        }

        key = JsonTags.KEY_SECRET_FINGERPRINT;
        if (jsonObject.has(key)) {
            try {
                String fingerprint = jsonObject.getString(key);
                /*
                 * The configuration server puts the prefix "SHA256:" to the
                 * raw fingerprint value.
                 * Make sure that the given fingerprint is identical with the
                 * locally calculated one.
                 */
                if (! fingerprint.equals("SHA256:" + mFingerprint)) {
                    mListener.onError(TAG +
                            ": Secret{id(" + id + ")}" +
                            ": Fingerprint unmatched");
                    return;
                }
            } catch (JSONException e) {
                mListener.onError(TAG + ": key(" + key + "): " + e.getMessage());
                return;
            }
        } else {
            mListener.onError(TAG +
                    ": Secret{id(" + id + ")}" +
                    ": key(" + key + ") not found");
            return;
        }

        key = JsonTags.KEY_SECRET_TARGET;
        if (jsonObject.has(key)) {
            try {
                target = jsonObject.getString(key);
            } catch (JSONException e) {
                mListener.onError(TAG +
                        ": Secret{id(" + id + ")}" +
                        ": key(" + key + "): " + e.getMessage());
                return;
            }
        } else {
            mListener.onError(TAG +
                    ": Secret{id(" + id + ")}" +
                    ": key(" + key + ") not found");
            return;
        }

        key = JsonTags.KEY_SECRET_VALUE;
        if (jsonObject.has(key)) {
            try {
                value = jsonObject.getString(key);
            } catch (JSONException e) {
                mListener.onError(TAG +
                        ": Secret{id(" + id + "),target(" + target + ")}" +
                        ": key(" + key + "): " + e.getMessage());
                return;
            }
        } else {
            mListener.onError(TAG +
                    ": Secret{id(" + id + "),target(" + target + ")}" +
                    ": key(" + key + ") not found");
            return;
        }

        if (target.endsWith(JsonTags.KEY_ATTACHMENT_TLS_CERTFILE_DATA)) {
            byte[] bytes = Base64.getDecoder().decode(value);
            parseEncryptedData(target, bytes);
        } else {
            /*
             * Given target does not end with the expected suffix.
             * To prevent the listener from infinite waiting, return error here.
             */
            mListener.onError(TAG +
                    ": Secret{id(" + id + "),target(" + target + ")}" +
                    ": Unknown target");
        }
    }

    private boolean hasValidHeader(@NonNull final byte[] encryptedData) {
        /*
         * Disassemble elements from the encrypted data.
         * Here we check the header part.
         *
         *    |<---------  encryptedData  -------->|
         *      0  1  2  3
         *    +--+--+--+--+------------------------+
         *    | ver |pk|sk|  ...                   |
         *    +--+--+--+--+------------------------+
         *    |<-- hdr -->|
         *
         *     ver: Data format version
         *     pk: Private Key type
         *     sk: Shared Key type
         */
        if (encryptedData.length < HEADER_BYTES) {
            mListener.onError(TAG + ": Too short header length: " +
                    encryptedData.length);
            return false;
        }

        byte[] version = new byte[VERSION_BYTES];
        byte pkType = encryptedData[2];
        byte skType = encryptedData[3];

        System.arraycopy(encryptedData, 0, version, 0, version.length);

        short shortVal = ByteBuffer.wrap(version).getShort();
        if (shortVal != 0x1) {
            mListener.onError(TAG +
                    ": Unknown header version: " + Arrays.toString(version));
            return false;
        }
        switch (pkType) {
            case CryptoTypes.PK_RSA_OAEP_SHA256:
            case CryptoTypes.PK_RSA_OAEP_SHA1:
                break;
            default:
                mListener.onError(TAG +
                        ": Unknown public key type: " + pkType);
                return false;
        }
        if (skType != 0x1) {
            mListener.onError(TAG +
                    ": Unknown shared key type: " + skType);
            return false;
        }

        return true;
    }

    private void parseEncryptedData(@NonNull String target,
                                    @NonNull byte[] encryptedData) {
        /*
         * Disassemble elements from the encrypted data.
         *
         *    |<---------  encryptedData  --------->|
         *    +-----+-----+----+----------+---------+
         *    | hdr | key | iv |  opaque  | authtag |
         *    +-----+-----+----+----------+---------+
         *    |<---- aad ----->|
         *
         *     hdr: header
         *     key: Shared key encrypted by public key
         *     iv: Initial vector
         *     opaque: User data encrypted by the shared key
         *     authtag: Authentication tag
         *     aad: Associated data
         */
        /* Check if the given byte array has an expected header */
        if (! hasValidHeader(encryptedData)) {
            return;
        }

        Integer rsaPublicKeySize = mKeyPairHandler.getPublicKeySize(mAlias);
        if (rsaPublicKeySize == null) {
            mListener.onError(
                    TAG + ": Alias(" + mAlias + "): Cannot get PublicKey size");
            return;
        }
        int keySize = (rsaPublicKeySize + (8 - 1)) / 8;
        final int GCM_IV_BYTES = 12; /* AES-GCM-256 */
        final int AUTHTAG_BYTES = 16; /* GCM */

        byte[] encryptedKey = new byte[keySize];
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] authtag = new byte[AUTHTAG_BYTES];

        int opaqueSize = encryptedData.length -
                (HEADER_BYTES + encryptedKey.length + iv.length + authtag.length);
        if (opaqueSize <= 0) {
            mListener.onError(
                    TAG + ": Invalid ENCRYPTED data length: " + opaqueSize);
            return;
        }
        byte[] opaque = new byte[opaqueSize];

        int offset = HEADER_BYTES;
        System.arraycopy(encryptedData, offset, encryptedKey, 0, encryptedKey.length);
        offset += encryptedKey.length;
        System.arraycopy(encryptedData, offset, iv, 0, iv.length);
        offset += iv.length;
        System.arraycopy(encryptedData, offset, opaque, 0, opaque.length);
        offset += opaque.length;
        System.arraycopy(encryptedData, offset, authtag, 0, authtag.length);

        int aadSize = HEADER_BYTES + encryptedKey.length + iv.length;
        byte[] aad = new byte[aadSize];
        System.arraycopy(encryptedData, 0, aad, 0, aad.length);

        byte[] decryptedKey = mKeyPairHandler.decryptBytes(mAlias, encryptedKey);
        if (decryptedKey == null) {
            return;
        }

        byte[] certData = decryptOpaqueData(decryptedKey, iv, opaque, authtag, aad);
        if (certData == null) {
            return;
        }
        mListener.onClientCertificate(target, certData);
    }

    private byte[] decryptOpaqueData(@NonNull byte[] sharedKey,
                                     @NonNull byte[] iv,
                                     @NonNull byte[] opaque,
                                     @NonNull byte[] authtag,
                                     @NonNull byte[] aad) {
        /*
         * Allocate a Cipher instance for decryption.
         */
        final String transformation =
                KeyProperties.KEY_ALGORITHM_AES + "/" +
                KeyProperties.BLOCK_MODE_GCM + "/" +
                KeyProperties.ENCRYPTION_PADDING_NONE;
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(transformation);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            mListener.onError(TAG +
                    ": DECRYPT#2: Cipher.getInstance(" + transformation + "): " + e.getMessage());
            return null;
        }

        /*
         * Prepare secret key and GCM parameter spec.
         */
        SecretKeySpec keySpec =
                new SecretKeySpec(sharedKey, KeyProperties.KEY_ALGORITHM_AES);

        int tlen = authtag.length * 8; /* bytes -> bits */
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tlen, iv);
        if (mDebugEnabled) {
            Log.d(TAG, "DECRYPT#2: GCMParameterSpec{" +
                    "tlen(" + gcmParameterSpec.getTLen() + ")," +
                    "iv(" + Arrays.toString(gcmParameterSpec.getIV()) + ")}");
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            mListener.onError(
                    TAG + ": DECRYPT#2: Cipher.init(): " + e.getMessage());
            return null;
        }

        try {
            cipher.updateAAD(aad);
        } catch (IllegalArgumentException |
                IllegalStateException |
                UnsupportedOperationException e) {
            mListener.onError(
                    TAG + ": DECRYPT#2: Cipher.updateAAD(): " + e.getMessage());
            return null;
        }

        /*
         * Build probe from opaque and authentication tag.
         *
         *    |<-----  probe  ------>|
         *    +------------+---------+
         *    |   opaque   | authtag |
         *    +------------+---------+
         */
        byte[] probe = new byte[opaque.length + authtag.length];
        int offset = 0;
        System.arraycopy(opaque, 0, probe, offset, opaque.length);
        offset += opaque.length;
        System.arraycopy(authtag, 0, probe, offset, authtag.length);

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
            mListener.onError(
                    TAG + ": DECRYPT#2: Cipher.doFinal(): " + e.getMessage());
            return null;
        }

        return originalData;
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface SecretHandlerListener {
        void onReady(@NonNull SecretHandler secretHandler);
        void onClientCertificate(@NonNull String target,
                                 @NonNull byte[] certificate);
        void onError(@NonNull String description);
    }
}
