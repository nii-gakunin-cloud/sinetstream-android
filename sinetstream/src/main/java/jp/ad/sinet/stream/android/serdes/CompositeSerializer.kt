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

package jp.ad.sinet.stream.android.serdes

import android.content.Context
import jp.ad.sinet.stream.android.api.Serializer
import jp.ad.sinet.stream.android.marshal.Marshaller
import jp.ad.sinet.stream.android.utils.Timestamped

class CompositeSerializer<T>(
    context: Context?,
    private val mGenericSerializer: Serializer<T>
) {
    private val mCompositeSerializer: Serializer<Timestamped<T>>

    private fun getCompositeSerializer(context: Context?): Serializer<Timestamped<T>> {
        val marshaller = Marshaller(context)

        return object :
            Serializer<Timestamped<T>> {
            override fun serialize(data: Timestamped<T>): ByteArray? {
                val bytes = mGenericSerializer.serialize(data.value)
                return bytes?.let { marshaller.encode(data.timestamp, it) }
            }
        }
    }

    fun toPayload(value: T?): ByteArray? {
        return if (value != null) {
            mCompositeSerializer.serialize(
                Timestamped(
                    value
                )
            )
        } else null
    }

    init {
        mCompositeSerializer = getCompositeSerializer(context)
    }
}
