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
import jp.ad.sinet.stream.android.api.Deserializer
import jp.ad.sinet.stream.android.api.Message
import jp.ad.sinet.stream.android.marshal.Unmarshaller
import jp.ad.sinet.stream.android.utils.Timestamped

class CompositeDeserializer<T>(
    context: Context?,
    private val mGenericDeserializer: Deserializer<T>
) {
    private val mCompositeDeserializer: Deserializer<Timestamped<T>?>

    private fun getCompositeDeserializer(context: Context?): Deserializer<Timestamped<T>?> {
        val unmarshaller =
            Unmarshaller(context)

        return object :
            Deserializer<Timestamped<T>?> {
            override fun deserialize(bytes: ByteArray?): Timestamped<T>? {
                var datum: Timestamped<T>? = null
                if (bytes != null) {
                    @Suppress("UNCHECKED_CAST")
                    datum = unmarshaller.decode(bytes) as Timestamped<T>
                }
                var value: T? = null
                if (datum != null) {
                    value = mGenericDeserializer.deserialize(datum.value as ByteArray)
                }
                return if (value != null && datum != null) {
                    Timestamped(
                        value,
                        datum.timestamp
                    )
                } else null
            }
        }
    }

    fun toMessage(topic: String?, bytes: ByteArray?, rawData: Any?): Message<T>? {
        if (topic != null && bytes != null) {
            val datum = mCompositeDeserializer.deserialize(bytes)
            if (datum != null) {
                return Message(
                    datum.value,
                    topic,
                    datum.timestamp,
                    rawData
                )
            }
        }
        return null
    }

    init {
        mCompositeDeserializer = getCompositeDeserializer(context)
    }
}
