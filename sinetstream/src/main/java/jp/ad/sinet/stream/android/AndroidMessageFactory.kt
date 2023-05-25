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
import jp.ad.sinet.stream.android.config.local.AndroidConfigLoader
import jp.ad.sinet.stream.android.config.remote.RemoteConfigLoader
import jp.ad.sinet.stream.android.mqtt.MqttAsyncMessageReader
import jp.ad.sinet.stream.android.mqtt.MqttAsyncMessageWriter
import java.io.File

class AndroidMessageReaderFactory<T>(
    val context: Context?,
    private val service: String,
    private val parameters: Map<String, Any>
) {
    var mConfigParameters: Map<String, Any>? = null
    var mConfigAttachments: Map<String, Any>? = null

    private lateinit var mServerUrl: String
    private lateinit var mAccount: String
    private lateinit var mSecretKey: String
    private var mUseRemoteConfig: Boolean = false

    private var mPredefinedDataStream: String? = null
    private var mPredefinedServiceName: String? = null

    private var mDebugEnabled: Boolean = false

    interface ReaderConfigLoaderListener {
        fun onReaderConfigLoaded()
        fun onError(description: String)
    }

    data class Builder<T>(
        var context: Context? = null,
        var service: String? = null,
        var topics: MutableList<String>? = null,
        var consistency: Consistency? = null,
        var clientId: String? = null,
        var valueType: ValueType? = null,
        var apiParameters: MutableMap<String, Any> = mutableMapOf(),
        var deserializer: Deserializer<T>? = null) {

        fun context(context: Context) = apply { this.context = context }
        fun service(service: String) = apply { this.service = service }
        fun topics(topics: List<String>) = apply { this.topics = ArrayList(topics) }
        fun topic(topic: String) = apply {
            this.topics?.add(topic) ?: let { this.topics = mutableListOf(topic) }
        }
        fun consistency(consistency: Consistency) = apply { this.consistency = consistency }
        fun clientId(clientId: String) = apply { this.clientId = clientId }
        fun valueType(valueType: ValueType) = apply { this.valueType = valueType }
        fun parameters(parameters: Map<String, *>) = apply { this.apiParameters =
            toNonNullMap(parameters)
        }
        fun addParameter(key: String, value: Any) = apply { this.apiParameters[key] = value }
        fun deserializer(deserializer: Deserializer<T>) = apply { this.deserializer = deserializer }

        fun build(): AndroidMessageReaderFactory<T> {
            if (topics == null) {
                getTopics(apiParameters)
                    ?.let { topics = it.toMutableList() }
            }
            topics?.let { if (it.isNotEmpty()) { apiParameters["topic"] = it } }
            consistency?.let { apiParameters["consistency"] = it }
            valueType?.let { apiParameters["value_type"] = it }
            clientId?.let { apiParameters["client_id"] = it }
            deserializer?.let { apiParameters["deserializer"] = it }

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
                    apiParameters
                )
            } ?: throw InvalidConfigurationException(
                "Mandatory parameter 'service' is missing."
            )
        }
    }

    fun setRemoteConfig(serverUrl: String, account: String, secretKey: String) {
        this.mServerUrl = serverUrl
        this.mAccount = account
        this.mSecretKey = secretKey
        this.mUseRemoteConfig = true
    }

    fun setPredefinedParameters(predefinedDataStream: String?,
                                predefinedServiceName: String?) {
        this.mPredefinedDataStream = predefinedDataStream
        this.mPredefinedServiceName = predefinedServiceName
    }

    fun setDebugEnabled(enabled: Boolean) {
        this.mDebugEnabled = enabled
    }

    fun loadConfig(listener: ReaderConfigLoaderListener) {
        if (context != null) {
            if (mUseRemoteConfig) {
                val loader = RemoteConfigLoader(mServerUrl, mAccount, mSecretKey)
                if (mPredefinedDataStream != null && mPredefinedServiceName != null) {
                    loader.setPredefinedParameters(
                        mPredefinedDataStream, mPredefinedServiceName)
                }
                loader.enableDebug(mDebugEnabled)
                loader.load(context, "Reader",
                    object:RemoteConfigLoader.RemoteConfigListener {
                        override fun onLoaded(
                            parameters: MutableMap<String, Any>,
                            attachments: MutableMap<String, Any>?
                        ) {
                            mConfigParameters = parameters
                            mConfigAttachments = attachments
                            listener.onReaderConfigLoaded()
                        }

                        override fun onError(description: String) {
                            listener.onError(description)
                        }
                    })
            } else {
                val filesDir: File = context.filesDir
                val loader = AndroidConfigLoader
                mConfigParameters = loader.load(filesDir, service)
                listener.onReaderConfigLoaded()
            }
        } else {
            listener.onError("Calling sequence failure")
        }
    }

    fun getAsyncReader(): AsyncMessageReader<T> {
        val params = mutableMapOf<String, Any>()

        if (mConfigParameters != null) {
            params.putAll(mConfigParameters!!)
        } else {
            throw InvalidConfigurationException(
                "Calling sequence failure; call `loadConfig()` beforehand."
            )
        }
        params["consistency"]?.let {
            if (it is String) {
                params["consistency"] = Consistency.valueOf(it)
            }
        }
        params["value_type"]?.let {
            if (it is String) {
                val key: String = it.uppercase()
                params["value_type"] = ValueType.valueOf(key)
            }
        }
        params.putAll(parameters)

        if (params["type"] != null) {
            when (val msgType = params["type"]) {
                "mqtt" -> return MqttAsyncMessageReader(
                    context,
                    service,
                    params,
                    mConfigAttachments,
                    mDebugEnabled
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
    private val parameters: Map<String, Any>
) {
    var mConfigParameters: Map<String, Any>? = null
    var mConfigAttachments: Map<String, Any>? = null

    private lateinit var mServerUrl: String
    private lateinit var mAccount: String
    private lateinit var mSecretKey: String
    private var mUseRemoteConfig: Boolean = false

    private var mPredefinedDataStream: String? = null
    private var mPredefinedServiceName: String? = null

    private var mDebugEnabled: Boolean = false

    interface WriterConfigLoaderListener {
        fun onWriterConfigLoaded()
        fun onError(description: String)
    }

    data class Builder<T>(
        var context: Context? = null,
        var service: String? = null,
        var topics: MutableList<String>? = null,
        var consistency: Consistency? = null,
        var clientId: String? = null,
        var valueType: ValueType? = null,
        var apiParameters: MutableMap<String, Any> = mutableMapOf(),
        var serializer: Serializer<T>? = null) {

        fun context(context: Context) = apply { this.context = context }
        fun service(service: String) = apply { this.service = service }
        fun topics(topics: List<String>) = apply { this.topics = ArrayList(topics) }
        fun topic(topic: String) = apply {
            this.topics?.add(topic) ?: let { this.topics = mutableListOf(topic) }
        }
        fun consistency(consistency: Consistency) = apply { this.consistency = consistency }
        fun clientId(clientId: String) = apply { this.clientId = clientId }
        fun valueType(valueType: ValueType) = apply { this.valueType = valueType }
        fun parameters(parameters: Map<String, *>) = apply { this.apiParameters =
            toNonNullMap(parameters)
        }
        fun addParameter(key: String, value: Any) = apply { this.apiParameters[key] = value }
        fun serializer(serializer: Serializer<T>) = apply { this.serializer = serializer }

        fun build(): AndroidMessageWriterFactory<T> {
            if (topics == null) {
                getTopics(apiParameters)
                    ?.let { topics = it.toMutableList() }
            }
            topics?.let { if (it.isNotEmpty()) { apiParameters["topic"] = it } }
            consistency?.let { apiParameters["consistency"] = it }
            valueType?.let { apiParameters["value_type"] = it }
            clientId?.let { apiParameters["client_id"] = it }
            serializer?.let { apiParameters["serializer"] = it }

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
                    apiParameters
                )
            } ?: throw InvalidConfigurationException(
                "Mandatory parameter 'service' is missing."
            )
        }
    }

    fun setRemoteConfig(serverUrl: String, account: String, secretKey: String) {
        this.mServerUrl = serverUrl
        this.mAccount = account
        this.mSecretKey = secretKey
        this.mUseRemoteConfig = true
    }

    fun setPredefinedParameters(predefinedDataStream: String?,
                                predefinedServiceName: String?) {
        this.mPredefinedDataStream = predefinedDataStream
        this.mPredefinedServiceName = predefinedServiceName
    }

    fun setDebugEnabled(enabled: Boolean) {
        this.mDebugEnabled = enabled
    }

    fun loadConfig(listener: WriterConfigLoaderListener) {
        if (context != null) {
            if (mUseRemoteConfig) {
                val loader = RemoteConfigLoader(mServerUrl, mAccount, mSecretKey)
                if (mPredefinedDataStream != null && mPredefinedServiceName != null) {
                    loader.setPredefinedParameters(
                        mPredefinedDataStream, mPredefinedServiceName)
                }
                loader.enableDebug(mDebugEnabled)
                loader.load(context, "Writer",
                    object:RemoteConfigLoader.RemoteConfigListener {
                        override fun onLoaded(
                            parameters: MutableMap<String, Any>,
                            attachments: MutableMap<String, Any>?
                        ) {
                            mConfigParameters = parameters
                            mConfigAttachments = attachments
                            listener.onWriterConfigLoaded()
                        }

                        override fun onError(description: String) {
                            listener.onError(description)
                        }
                    })
            } else {
                val filesDir: File = context.filesDir
                val loader = AndroidConfigLoader
                mConfigParameters = loader.load(filesDir, service)
                listener.onWriterConfigLoaded()
            }
        }
    }

    fun getAsyncWriter(): AsyncMessageWriter<T> {
        val params = mutableMapOf<String, Any>()

        if (mConfigParameters != null) {
            params.putAll(mConfigParameters!!)
        } else {
            throw InvalidConfigurationException(
                "Calling sequence failure; call `loadConfig()` beforehand."
            )
        }
        params["consistency"]?.let {
            if (it is String) {
                params["consistency"] = Consistency.valueOf(it)
            }
        }
        params["value_type"]?.let {
            if (it is String) {
                val key: String = it.uppercase()
                params["value_type"] = ValueType.valueOf(key)
            }
        }
        params.putAll(parameters)

        if (params["type"] != null) {
            when (val msgType = params["type"]) {
                "mqtt" -> return MqttAsyncMessageWriter(
                    context,
                    service,
                    params,
                    mConfigAttachments,
                    mDebugEnabled
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
