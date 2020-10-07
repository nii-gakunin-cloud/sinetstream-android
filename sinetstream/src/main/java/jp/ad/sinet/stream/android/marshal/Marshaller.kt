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

package jp.ad.sinet.stream.android.marshal

import android.content.Context
import android.util.Log
import jp.ad.sinet.stream.android.api.SinetStreamIOException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.message.BinaryMessageEncoder
import java.io.IOException
import java.nio.ByteBuffer

class Marshaller(context: Context?) {
    private val mSchema: Schema?
    private val mEncoder: BinaryMessageEncoder<GenericRecord>?

    fun encode(timestamp: Long, message: ByteArray): ByteArray? {
        if (mSchema != null && mEncoder != null) {
            return try {
                val datum: GenericRecord = GenericData.Record(mSchema)

                datum.put(SchemaKeys.SCHEMA_KEY_TIMESTAMP.key, timestamp)
                datum.put(SchemaKeys.SCHEMA_KEY_MESSAGE.key, ByteBuffer.wrap(message))

                val buf = mEncoder.encode(datum)
                buf.array()
            } catch (e: IOException) {
                Log.w(TAG,"BinaryMessageEncoder.encode: $e")
                throw SinetStreamIOException(e)
            }
        }
        Log.w(TAG,"BinaryMessageEncoder is not yet initialized")
        return null
    }

    companion object {
        private val TAG =
            Marshaller::class.java.simpleName
    }

    init {
        val messageSchema = MessageSchema()
        if (context != null) {
            mSchema = messageSchema.getSchema(context)

            /*
            * If shouldCopy is true, then buffers returned by #encode(D)
            * are copied and will not be modified by future calls to encode.
            * https://avro.apache.org/docs/current/api/java/org/apache/avro/message/BinaryMessageEncoder.html
            */
            mEncoder = BinaryMessageEncoder(GenericData(), mSchema, true)
        } else {
            mSchema = null
            mEncoder = null
        }
    }
}
