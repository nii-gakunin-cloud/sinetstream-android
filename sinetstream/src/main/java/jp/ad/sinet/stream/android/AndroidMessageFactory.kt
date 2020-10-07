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

package jp.ad.sinet.stream.android

import android.content.Context
import jp.ad.sinet.stream.android.api.*
import jp.ad.sinet.stream.android.api.low.AsyncMessageReader
import jp.ad.sinet.stream.android.api.low.AsyncMessageWriter
import jp.ad.sinet.stream.android.mqtt.MqttAsyncMessageReader
import jp.ad.sinet.stream.android.mqtt.MqttAsyncMessageWriter

class AndroidMessageReaderFactory<T>(
        val context: Context?,
        private val service: String,
        private val parameters: Map<String, Any>) {

    data class Builder<T>(
        var context: Context? = null,
        var service: String? = null,
        var topics: MutableList<String>? = null,
        var consistency: Consistency? = null,
        var clientId: String? = null,
        var valueType: ValueType? = null,
        var parameters: MutableMap<String, Any> = mutableMapOf(),
        var deserializer: Deserializer<T>? = null) {

        fun context(context: Context) = apply { this.context = context }
        fun service(service: String) = apply { this.service = service }
        fun topics(topics: List<String>) = apply { this.topics = ArrayList(topics) }
        fun topic(topic: String) = apply {
            this.topics?.add(topic) ?: let { this.topics = mutableListOf(topic) }
        }
        fun addTopic(topic: String) = topic
        fun consistency(consistency: Consistency) = apply { this.consistency = consistency }
        fun clientId(clientId: String) = apply { this.clientId = clientId }
        fun valueType(valueType: ValueType) = apply { this.valueType = valueType }
        fun parameters(parameters: Map<String, *>) = apply { this.parameters =
            toNonNullMap(parameters)
        }
        fun addParameter(key: String, value: Any) = apply { this.parameters[key] = value }
        fun deserializer(deserializer: Deserializer<T>) = apply { this.deserializer = deserializer }

        fun build(): AndroidMessageReaderFactory<T> {
            if (topics == null) {
                getTopics(parameters)
                    ?.let { topics = it.toMutableList() }
            }
            topics?.let { if (it.isNotEmpty()) { parameters["topic"] = it } }
            consistency?.let { parameters["consistency"] = it }
            valueType?.let { parameters["value_type"] = it }
            clientId?.let { parameters["client_id"] = it }
            deserializer?.let { parameters["deserializer"] = it }

            if (context != null && (context!!.applicationContext != null)) {
                // ok
            } else {
                throw InvalidConfigurationException(
                    "Mandatory parameter 'context' is missing."
                )
            }
            service?.let {
                return AndroidMessageReaderFactory(
                    context,
                    it,
                    parameters
                )
            } ?: throw InvalidConfigurationException(
                "Mandatory parameter 'service' is missing."
            )
        }
    }

    fun getAsyncReader(): AsyncMessageReader<T> {
        val params = mutableMapOf<String, Any>()
        context?.applicationContext?.filesDir?.let { params.putAll(
            AndroidConfigLoader.load(
                it,
                service
            )
        ) }
        params.putAll(parameters)

        if (params["type"] != null) {
            when (val msgType = params["type"]) {
                "mqtt" -> return MqttAsyncMessageReader(
                    context,
                    service,
                    params
                )
                else -> throw UnsupportedServiceException(
                    "Unknown messaging system: $msgType"
                )
            }
        } else {
            throw InvalidConfigurationException(
                "Mandatory parameter 'type' is missing."
            )
        }
    }
}

internal fun getTopics(map: Map<String, Any>): List<String>? {
    return map["topic"]?.let { topic ->
        when (topic) {
            is List<*> -> topic.filterIsInstance<String>()
            is String -> listOf(topic)
            else -> emptyList()
        }
    }
}

internal fun <T, U> toNonNullMap(map: Map<T, U?>): MutableMap<T, U> {
    return LinkedHashMap(map.mapNotNull { it.value?.let { v -> it.key to v } }.toMap())
}

class AndroidMessageWriterFactory<T>(
        val context: Context?,
        private val service: String,
        private val parameters: Map<String, Any>) {

    data class Builder<T>(
        var context: Context? = null,
        var service: String? = null,
        var topic: String? = null,
        var consistency: Consistency? = null,
        var clientId: String? = null,
        var valueType: ValueType? = null,
        var parameters: MutableMap<String, Any> = mutableMapOf(),
        var serializer: Serializer<T>? = null) {

        fun context(context: Context) = apply { this.context = context }
        fun service(service: String) = apply { this.service = service }
        fun topic(topic: String) = apply { this.topic = topic }
        fun consistency(consistency: Consistency) = apply { this.consistency = consistency }
        fun clientId(clientId: String) = apply { this.clientId = clientId }
        fun valueType(valueType: ValueType) = apply { this.valueType = valueType }
        fun parameters(parameters: Map<String, *>) = apply { this.parameters =
            toNonNullMap(parameters)
        }
        fun addParameter(key: String, value: Any) = apply { this.parameters[key] = value }
        fun serializer(serializer: Serializer<T>) = apply { this.serializer = serializer }

        fun build(): AndroidMessageWriterFactory<T> {
            topic?.let { parameters["topic"] = it }
            consistency?.let { parameters["consistency"] = it }
            valueType?.let { parameters["value_type"] = it }
            clientId?.let { parameters["client_id"] = it }
            serializer?.let { parameters["serializer"] = it }

            if (context != null && (context!!.applicationContext != null)) {
                // ok
            } else {
                throw InvalidConfigurationException(
                    "Mandatory parameter 'context' is missing."
                )
            }
            service?.let {
                return AndroidMessageWriterFactory(
                    context,
                    it,
                    parameters
                )
            } ?: throw InvalidConfigurationException(
                "Mandatory parameter 'service' is missing."
            )
        }
    }

    fun getAsyncWriter(): AsyncMessageWriter<T> {
        val params = mutableMapOf<String, Any>()
        params.putAll(
            AndroidConfigLoader.load(
                context?.applicationContext?.filesDir,
                service
            )
        )
        params["consistency"]?.let {
            if (it is String) {
                params["consistency"] = Consistency.valueOf(it)
            }
        }
        params["value_type"]?.let {
            if (it is String) {
                params["value_type"] = ValueType.valueOf(it)
            }
        }
        params.putAll(parameters)

        if (params["type"] != null) {
            when (val msgType = params["type"]) {
                "mqtt" -> return MqttAsyncMessageWriter(
                    context,
                    service,
                    params
                )
                else -> throw InvalidConfigurationException(
                    "Unknown messaging system: $msgType"
                )
            }
        } else {
            throw InvalidConfigurationException(
                "Mandatory parameter 'type' is missing."
            )
        }
    }
}
