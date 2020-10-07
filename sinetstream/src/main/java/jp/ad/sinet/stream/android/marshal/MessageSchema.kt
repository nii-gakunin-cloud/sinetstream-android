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
import jp.ad.sinet.stream.R
import jp.ad.sinet.stream.android.api.SinetStreamException
import jp.ad.sinet.stream.android.api.SinetStreamIOException
import org.apache.avro.Schema
import org.apache.avro.SchemaNormalization
import java.io.IOException
import java.security.NoSuchAlgorithmException

class MessageSchema {
    private val mParser = Schema.Parser()

    fun getSchema(context: Context?): Schema? {
        if (context != null) {
            val fileName = "message_schema.avsc"
            return try {
                val `is` = context.resources?.openRawResource(R.raw.message_schema)
                mParser.parse(`is`)
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "SCHEMA($fileName): $e"
                )
                throw SinetStreamIOException(
                    fileName,
                    e
                )
            }
        }
        Log.w(
            TAG,
            "Empty Context"
        )
        return null
    }

    fun getFingerprint(schema: Schema): ByteArray {
        val fingerprintName = "CRC-64-AVRO"
        return try {
            SchemaNormalization.parsingFingerprint(fingerprintName, schema)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(
                TAG,
                "Fingerprint($fingerprintName): $e"
            )
            throw SinetStreamException(
                fingerprintName,
                e
            )
        }
    }

    companion object {
        private val TAG = MessageSchema::class.java.simpleName
    }
}
