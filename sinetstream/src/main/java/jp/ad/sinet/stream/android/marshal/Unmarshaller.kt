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
import jp.ad.sinet.stream.android.api.InvalidMessageException
import jp.ad.sinet.stream.android.utils.Timestamped
import org.apache.avro.AvroRuntimeException
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.message.BinaryMessageDecoder
import java.io.IOException
import java.nio.ByteBuffer

class Unmarshaller(context: Context?) {
    private val mDecoder: BinaryMessageDecoder<GenericRecord>?

    fun decode(bytes: ByteArray): Timestamped<ByteArray>? {
        if (mDecoder != null) {
            return try {
                val datum = mDecoder.decode(bytes)

                val timestamp =
                    datum[SchemaKeys.SCHEMA_KEY_TIMESTAMP.key] as Long
                val message =
                    (datum[SchemaKeys.SCHEMA_KEY_MESSAGE.key] as ByteBuffer).array()

                Timestamped(message, timestamp)
            } catch (e: IOException) {
                /*
                 * Both BadHeaderException and MissingSchemaException are
                 * subclass of AvroRuntimeException
                 */
                Log.w(TAG,"BinaryMessageDecoder.decode: $e")
                throw InvalidMessageException(e)
            } catch (e: AvroRuntimeException) {
                Log.w(TAG, "BinaryMessageDecoder.decode: $e")
                throw InvalidMessageException(e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG,"BinaryMessageDecoder.decode: $e")
                throw InvalidMessageException(e)
            }
        }
        Log.w(TAG, "BinaryMessageEncoder is not yet initialized")
        return null
    }

    companion object {
        private val TAG =
            Unmarshaller::class.java.simpleName
    }

    init {
        val messageSchema = MessageSchema()
        if (context != null) {
            val schema = messageSchema.getSchema(context)

            /*
             * If readSchema is null, the write schema of an incoming buffer
             * is used as read schema for that datum instance.
             *
             * https://avro.apache.org/docs/current/api/java/org/apache/avro/message/BinaryMessageDecoder.html
             */
            mDecoder = BinaryMessageDecoder(GenericData(), schema)
        } else {
            mDecoder = null
        }
    }
}
