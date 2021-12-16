/*
 * Copyright (c) 2021 National Institute of Informatics
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

package jp.ad.sinet.stream.android.net.cert;

import android.app.Activity;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.Principal;

public class KeyChainHandler {
    /**
     * checkCertificate -- Let user select the alias for a private key and
     * certificate pair for authentication.
     * <p>
     *     This is a wrapper function of {@link KeyChain#choosePrivateKeyAlias(
     *     Activity, KeyChainAliasCallback, String[], Principal[], String, int, String)},
     *     to show a system dialog letting user choose a certificate
     *     installed in the system credential storage.
     * </p>
     * <p>
     *     On success, the alias for the private key and certificate pair
     *     will be returned via {@link KeyChainAliasCallback#alias(String)}.
     *     Otherwise, the alias will be {@code null}.
     * </p>
     * @param activity -- The Activity context which launches a sub-Activity.
     * @param listener -- Callback functions to handle the processing results.
     */
    public void checkCertificate(
            @NonNull Activity activity, @NonNull KeyChainListener listener) {
        KeyChain.choosePrivateKeyAlias(
                // The Activity context which launches a sub-Activity.
                activity,

                // The callback function to handle the chosen alias.
                new KeyChainAliasCallback() {
                    @Override
                    public void alias(@Nullable String alias) {
                        listener.onPrivateKeyAlias(alias);
                    }
                },

                // List of acceptable key types. null for any
                new String[] { KeyProperties.KEY_ALGORITHM_RSA },

                // issuer, null for any
                null,

                // host name of server requesting the cert,
                // null if unavailable
                null,

                // port of server requesting the cert, -1 for any
                -1,

                // alias to preselect, null if unavailable
                null);
    }

    public interface KeyChainListener {
        /**
         * Called when user picked the alias, or operation has canceled.
         *
         * @param alias -- User chosen alias, or {@code null} if no value has chosen.
         */
        void onPrivateKeyAlias(@Nullable String alias);
    }
}
