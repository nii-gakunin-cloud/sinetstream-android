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

package jp.ad.sinet.stream.android.api;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Provides a set of API functions to be a Reader (= subscriber)
 * in the SINETStream system.
 * <p>
 *     This class extends the generic {@link SinetStreamReader}
 *     to handle byte[] type user data.
 * </p>
 */
public class SinetStreamReaderBytes extends SinetStreamReader<byte[]> {
    private final String TAG = SinetStreamReaderBytes.class.getSimpleName();

    /**
     * Constructs a SinetStreamReaderBytes instance.
     *
     * @param context the Application context which implements
     *                {@link SinetStreamReaderListener},
     *                usually it is the calling {@link Activity} itself.
     * @throws RuntimeException if given context does not implement
     *                          the required listener.
     */
    public SinetStreamReaderBytes(@NonNull Context context) {
        super(context);
    }

    /**
     * Sets up oneself as a Reader which handles byte[] type.
     *
     * @param serviceName the service name to match configuration parameters.
     * @param alias the alias for a private key and certificate pair to be
     *              used for the transport layer security.
     * @see SinetStreamReader
     */
    @Override
    public void initialize(
            @NonNull String serviceName, @Nullable String alias) {
        super.initialize(serviceName, alias);

        if (super.isInitializationSuccess()) {
            ValueType valueType = getValueType();
            if (valueType != null && valueType.equals(ValueType.BYTE_ARRAY)) {
                super.setup();
            } else {
                super.abort(TAG + ": ValueType mismatch");
            }
        }
    }

    public interface SinetStreamReaderBytesListener
            extends SinetStreamReaderListener<byte[]> {

    }
}
