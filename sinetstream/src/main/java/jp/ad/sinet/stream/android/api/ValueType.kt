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

enum class ValueType {
    TEXT {
        override val serializer: Serializer<String?> =
            StringSerializer()
        override val deserializer: Deserializer<String?> =
            StringDeserializer()
    },
//    IMAGE,
    BYTE_ARRAY {
        override val serializer: Serializer<ByteArray?> =
            ByteArraySerializer()
        override val deserializer: Deserializer<ByteArray?> =
            ByteArrayDeserializer()
    },
    ;

    abstract val serializer: Serializer<*>
    abstract val deserializer: Deserializer<*>
}

interface Serializer<T> {
    fun serialize(data: T): ByteArray?
}

interface Deserializer<T> {
    fun deserialize(bytes: ByteArray?): T
}

class StringSerializer : Serializer<String?> {
    override fun serialize(data: String?): ByteArray? {
        return data?.toByteArray()
    }
}

class StringDeserializer : Deserializer<String?> {
    override fun deserialize(bytes: ByteArray?): String? {
        return bytes?.toString(Charsets.UTF_8)
    }
}

class ByteArraySerializer :
    Serializer<ByteArray?> {
    override fun serialize(data: ByteArray?): ByteArray? {
        return data
    }
}

class ByteArrayDeserializer :
    Deserializer<ByteArray?> {
    override fun deserialize(bytes: ByteArray?): ByteArray? {
        return bytes
    }
}
