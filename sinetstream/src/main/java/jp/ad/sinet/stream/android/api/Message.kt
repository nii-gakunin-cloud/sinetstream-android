/*
 * Copyright (c) 2020 National Institute of Informatics
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

package jp.ad.sinet.stream.android.api

import jp.ad.sinet.stream.android.utils.DateTimeUtil
import java.util.*

open class Message<T>(
    private val mValue: T,
    private val mTopic: String,
    private val mTimestampMicroseconds: Long?,
    private val mRawData: Any?
) {
    val payload: T
        get() = mValue

    val topic: String
        get() = mTopic

    val timestamp: Long?
        get() = if (Objects.isNull(mTimestampMicroseconds)) {
            // Old message format which has no timestamp
            null
        } else mTimestampMicroseconds?.div(1000L)

    val rawdata: Any?
        get() = mRawData

    override fun toString(): String {
        val dateTimeUtil = DateTimeUtil()
        return "Message {topic(" + topic + "),\n" +
                "timestamp(" + timestamp?.let { dateTimeUtil.toIso8601String(it) } + "),\n" +
                "value(" + payload.toString() + ")}"
    }
}
