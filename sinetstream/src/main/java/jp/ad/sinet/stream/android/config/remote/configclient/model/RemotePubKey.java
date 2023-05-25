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

package jp.ad.sinet.stream.android.config.remote.configclient.model;

import androidx.annotation.NonNull;

public class RemotePubKey {
    private final int mId;
    private final String mFingerprint;
    private final String mComment;
    private final boolean mIsDefaultKey;
    private final String mCreatedAt;

    public RemotePubKey(int id,
                        @NonNull String fingerprint,
                        @NonNull String comment,
                        boolean isDefaultKey,
                        @NonNull String createdAt) {
        this.mId = id;
        this.mFingerprint = fingerprint;
        this.mComment = comment;
        this.mIsDefaultKey = isDefaultKey;
        this.mCreatedAt = createdAt;
    }

    public int getId() {
        return mId;
    }

    @NonNull
    public String getFingerprint() {
        return mFingerprint;
    }

    @NonNull
    public String getComment() {
        return mComment;
    }

    public boolean isDefaultKey() {
        return mIsDefaultKey;
    }

    @NonNull
    public String getCreatedAt() {
        return mCreatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        String content = "{";
        content += "id(" + mId + ")" + ",";
        content += "fingerprint(" + mFingerprint + ")" + ",";
        content += "comment(" + mComment + ")" + ",";
        content += "default(" + mIsDefaultKey + ")" + ",";
        content += "createdAt(" + mCreatedAt + ")";
        content += "}";
        return content;
    }
}
