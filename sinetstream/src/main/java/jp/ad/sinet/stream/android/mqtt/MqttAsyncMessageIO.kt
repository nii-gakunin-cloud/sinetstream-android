/*
 * Copyright (C) 2020 National Institute of Informatics
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

package jp.ad.sinet.stream.android.mqtt

// import org.eclipse.paho.android.service.MqttDeliveryTokenAndroid
import android.content.Context
import android.util.Log
import jp.ad.sinet.stream.android.AndroidConfigLoader.KEY_DESERIALIZER
import jp.ad.sinet.stream.android.AndroidConfigLoader.KEY_SERIALIZER
import jp.ad.sinet.stream.android.api.*
import jp.ad.sinet.stream.android.api.low.AsyncMessageReader
import jp.ad.sinet.stream.android.api.low.AsyncMessageWriter
import jp.ad.sinet.stream.android.api.low.ReaderMessageCallback
import jp.ad.sinet.stream.android.api.low.WriterMessageCallback
import jp.ad.sinet.stream.android.config.ConfigParser
import jp.ad.sinet.stream.android.crypto.CipherHandler
import jp.ad.sinet.stream.android.net.UriBuilder
import jp.ad.sinet.stream.android.serdes.CompositeDeserializer
import jp.ad.sinet.stream.android.serdes.CompositeSerializer
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.MqttAsyncClient.generateClientId
import java.util.concurrent.atomic.AtomicBoolean

const val LOG_TAG = "SINETStream"
const val KEY_MQTT_TRANSPORT = "transport"
const val KEY_MQTT_RETAIN = "retain"
const val KEY_MQTT_PROTOCOL = "protocol"
const val KEY_MQTT_USER_PW = "username_pw_set"
const val KEY_MQTT_USERNAME = "username"
const val KEY_MQTT_PASSWORD = "password"
const val KEY_MQTT_CONNECT = "connect"
const val KEY_MQTT_KEEPALIVE = "keepalive"
const val KEY_MQTT_AUTOMATIC_RECONNECT = "automatic_reconnect"
const val KEY_MQTT_CONNECT_TIMEOUT = "connect_timeout"

abstract class MqttMessageIO(
    context: Context?,
    config: Map<String, Any>
) {
    private val configParser: ConfigParser = ConfigParser()

    val consistency: Consistency = Consistency.AT_MOST_ONCE // Unused
    val brokerUrl: String = "" // Unused
    val protocol: MqttProtocol = MqttProtocol.MQTTv311  // Unused

    val topicArray: Array<String>
    val qosArray: IntArray
    val retain: Boolean
    val valueType: ValueType
    val clientId: String
    val connectOptions: MqttConnectOptions
    val mCipherHandler: CipherHandler?
    val mCryptoPassword: String

    protected val client: MqttAndroidClient
    private val isClosed: AtomicBoolean = AtomicBoolean(false)

    init {
        configParser.parse(config)

        topicArray = configParser.topics
        qosArray = IntArray(topicArray.size) { configParser.qos }
        retain = configParser.retain
        valueType = configParser.valueType
        clientId = setClientId()

        val uriBuilder = UriBuilder(configParser)
        val serverUris: Array<String> = uriBuilder.buildBrokerUris()
        client = MqttAndroidClient(context, serverUris[0], clientId)

        val connectOptionBuilder =
            MqttConnectOptionBuilder(context, configParser, serverUris)
        connectOptions = connectOptionBuilder.buildMqttConnectOptions()

        if (configParser.cryptoEnabled) {
            mCipherHandler = getCipherHandler()
            mCryptoPassword = configParser.cryptoPassword
        } else {
            mCipherHandler = null
            mCryptoPassword = ""
        }
    }

    fun close() {
        if (!isClosed.getAndSet(true)) {
            /*
             * Workaround to prevent weird error, such like shown below,
             * after close() call.
             * https://github.com/eclipse/paho.mqtt.android/issues/366
             */
            client.unregisterResources()

            client.close()
        }
    }

    private fun setClientId(): String {
        val clientId: String
        val probe: String? = configParser.clientId
        if (probe != null && probe.isNotEmpty()) {
            clientId = probe
        } else {
            clientId = generateClientId()  // defined in MqttAsyncClient
        }
        return clientId
    }

    fun dumpServerUris() : String {
        return if (connectOptions.serverURIs != null) {
            dumpStringArray("Brokers", connectOptions.serverURIs)
        } else {
            "Broker {" + client.serverURI + "}"
        }
    }

    fun dumpTopics(array: Array<String>) : String {
        return dumpStringArray("Topics", array)
    }

    private fun dumpStringArray(label: String, array: Array<String>): String {
        var stringValue = "$label {"
        for (i in array.indices) {
            stringValue += array[i]
            if (i < array.size - 1) {
                stringValue += ','
            }
        }
        stringValue += "}"
        return stringValue
    }

    private fun getCipherHandler(): CipherHandler {
        val cipherHandler: CipherHandler
        try {
            cipherHandler = CipherHandler(
                    configParser.keyLength,
                    configParser.keyDerivationAlgorithm,
                    configParser.cryptoAlgorithm,
                    configParser.saltBytes,
                    configParser.iterationCount)

            cipherHandler.setTransformation(
                    configParser.cryptoAlgorithm,
                    configParser.feedbackMode,
                    configParser.paddingScheme)
        } catch (e: CryptoException) {
            throw CryptoException(e.message, e)
        }
        return cipherHandler
    }
}

class MqttAsyncMessageReader<T>(
    context: Context?,
    override val service: String,
    override val config: Map<String, Any>
) : MqttMessageIO(context, config),
    AsyncMessageReader<T> {

    /* Following parameters are obsoleted, but leave here for now... */
    override val topics: List<String> = ArrayList() // Unused
    override val topic: String = "" // Unused
    override val qos: Int = -1 // Unused

    override val deserializer: Deserializer<T> = getDeserializer(config)
    private val compositeDeserializer: CompositeDeserializer<T> =
        CompositeDeserializer(
            context,
            deserializer
        )

    private lateinit var mCallback: ReaderMessageCallback<T>

    companion object {
        private val TAG = LOG_TAG + "(" + MqttAsyncMessageReader::class.java.simpleName + ")"
    }

    override fun setCallback(callback: ReaderMessageCallback<T>) {
        mCallback = callback // Keep given callback for later reference

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                Log.d(TAG, "connectComplete {reconnect($reconnect),serverURI($serverURI)")
                if (reconnect) {
                    /* Automatic reconnect case */
                    Log.d(TAG, "Reconnected to Server($serverURI)")
                } else {
                    Log.d(TAG, "Connected to Server($serverURI)")
                }
                callback.onConnectionEstablished()
            }

            override fun connectionLost(cause: Throwable?) {
                if (cause != null) {
                    Log.w(TAG, "connectionLost: $cause")
                    callback.onConnectionClosed(cause.toString())
                } else {
                    Log.d(TAG, "Disconnect completed")
                    callback.onConnectionClosed(null)
                }
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                var payload = mqttMessage.payload ?: ByteArray(0)

                if (mCipherHandler != null) {
                    try {
                        payload = mCipherHandler.decrypt(payload, mCryptoPassword)
                    } catch (e: CryptoException) {
                        val description = e.message ?: "Decrypt Failure"
                        val cause = e.cause

                        mCallback.onError(description, cause)
                        return
                    }
                }

                try {
                    val message = compositeDeserializer.toMessage(topic, payload, mqttMessage)

                    if (message != null) {
                        Log.d(TAG, "messageArrived: $message")
                        callback.onMessageReceived(message)
                    } else {
                        Log.w(TAG, "messageArrived: Failed to deserialize")
                    }
                } catch (exception: ClassCastException) {
                    callback.onError("messageArrived", exception)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                // No such case for MqttMessageReader
                Log.d(TAG, "deliveryComplete: token: $token")
            }
        })
    }

    override fun connect() {
        Log.d(TAG, "Going to CONNECT " + dumpServerUris())
        try {
            client.connect(connectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "connect: OK")
                    /*
                     * NB:
                     * At this point, async connection request has merely accepted.
                     * We need to wait MqttCallbackExtended#connectComplete().
                     */
                    // mCallback.onConnectionEstablished()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    mCallback.onError("connect: NG", exception)
                }
            })
        } catch (exception: MqttException) {
            mCallback.onError("connect: EX", exception)
        }
    }

    override fun disconnect() {
        /*
         * Once the connection with the broker has established, it is
         * completely okay to call MqttAndroid.disconnect() at any time.
         *
         * On the other hand, if we abort the ongoing connection request,
         * we need to call MqttAndroid.disconnect() to clean the internal
         * state of the MqttService.
         */
        Log.d(TAG, "Going to DISCONNECT " + dumpServerUris())
        try {
            client.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "disconnect: OK")
                    mCallback.onConnectionClosed(null/* Normal closure */)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    mCallback.onError("disconnect: NG", exception)
                }
            })
        } catch (exception: MqttException) {
            mCallback.onError("disconnect: EX", exception)
        } catch (exception: IllegalArgumentException) {
            mCallback.onError("disconnect: EX", exception)
        }
    }

    override fun subscribe() {
        if (client.isConnected) {
            Log.d(TAG, "Going to SUBSCRIBE " + dumpTopics(topicArray))
            try {
                client.subscribe(topicArray, qosArray,
                    null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            if (asyncActionToken != null) {
                                Log.d(TAG, "subscribe: " +
                                        dumpTopics(asyncActionToken.topics) + ": OK")
                            } else {
                                Log.d(TAG, "subscribe: OK")
                            }
                            mCallback.onSubscribed()
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            if (asyncActionToken != null) {
                                Log.w(TAG, "subscribe: " +
                                        dumpTopics(asyncActionToken.topics) + ": NG")
                            } else {
                                Log.w(TAG, "subscribe: NG")
                            }
                            mCallback.onError("subscribe: NG", exception)
                        }
                    })
            } catch (exception: MqttSecurityException) {
                mCallback.onError("subscribe: EX", exception)
            } catch (exception: MqttException) {
                mCallback.onError("subscribe: EX", exception)
            } catch (exception: IllegalArgumentException) {
                mCallback.onError("subscribe: EX", exception)
            }
        } else {
            mCallback.onError("subscribe: Not yet connected", null)
        }
    }

    override fun unsubscribe() {
        if (client.isConnected) {
            Log.d(TAG, "Going to UNSUBSCRIBE " + dumpTopics(topicArray))
            try {
                client.unsubscribe(topicArray,
                    null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            if (asyncActionToken != null && asyncActionToken.topics != null) {
                                Log.d(TAG, "unsubscribe: " +
                                        dumpTopics(asyncActionToken.topics) + ": OK")
                            } else {
                                Log.d(TAG, "unsubscribe: OK")
                            }
                            mCallback.onUnsubscribed()
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            if (asyncActionToken != null && asyncActionToken.topics != null) {
                                Log.d(TAG, "unsubscribe: " +
                                        dumpTopics(asyncActionToken.topics) + ": NG")
                            } else {
                                Log.d(TAG, "unsubscribe: NG")
                            }
                            mCallback.onError("unsubscribe: NG", exception)
                        }
                    })
            } catch (exception: MqttException) {
                mCallback.onError("unsubscribe: EX", exception)
            }
        } else {
            mCallback.onError("unsubscribe: Not yet connected", null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDeserializer(params: Map<String, Any>): Deserializer<T> {
        params[KEY_DESERIALIZER]?.let {
            if (it is Deserializer<*>) {
                return it as Deserializer<T>
            }
        }
        return valueType.deserializer as Deserializer<T>
    }
}

class MqttAsyncMessageWriter<T>(
    context: Context?,
    override val service: String,
    override val config: Map<String, Any>
) : MqttMessageIO(context, config),
    AsyncMessageWriter<T> {

    /* Following parameters are obsoleted, but leave here for now... */
    override val topic: String = "" // Unused
    override val qos: Int = -1 // Unused

    override val serializer: Serializer<T> = getSerializer(config)
    private val compositeSerializer: CompositeSerializer<T> =
        CompositeSerializer(
            context,
            serializer
        )

    private lateinit var mCallback: WriterMessageCallback<T>

    companion object {
        private val TAG = LOG_TAG + "(" + MqttAsyncMessageWriter::class.java.simpleName + ")"
    }

    override fun setCallback(callback: WriterMessageCallback<T>) {
        mCallback = callback // Keep given callback for later reference

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                Log.d(TAG, "connectComplete {reconnect($reconnect),serverURI($serverURI)")
                if (reconnect) {
                    /* Automatic reconnect case */
                    Log.d(TAG, "Reconnected to Server($serverURI)")
                } else {
                    Log.d(TAG, "Connected to Server($serverURI)")
                }
                callback.onConnectionEstablished()
            }

            override fun connectionLost(cause: Throwable?) {
                if (cause != null) {
                    Log.w(TAG, "connectionLost: $cause")
                    callback.onConnectionClosed(cause.toString())
                } else {
                    Log.d(TAG, "Disconnect completed")
                    callback.onConnectionClosed(null)
                }
            }

            @Throws(Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                // No such case for MqttMessageWriter
                /*
                 * This function seems to be called only if client_id is
                 * specified in the SINETStream config file.
                 */
                Log.d(TAG, "messageArrived: topic($topic),message(...)")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {
                Log.d(TAG, "deliveryComplete: token: $token")
                /*
                 * As IMqttActionListener#onSuccess() for publish() must have
                 * already called, this callback can be omitted.
                 */
                // callback.onPublished()
            }
        })
    }

    override fun connect() {
        Log.d(TAG, "Going to CONNECT " + dumpServerUris())
        try {
            client.connect(connectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "connect: OK")
                    /*
                     * NB:
                     * At this point, async connection request has merely accepted.
                     * We need to wait MqttCallbackExtended#connectComplete().
                     */
                    // mCallback.onConnectionEstablished()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    mCallback.onError("connect: NG", exception)
                }
            })
        } catch (exception: MqttException) {
            mCallback.onError("connect: EX", exception)
        }
    }

    override fun disconnect() {
        /*
         * Once the connection with the broker has established, it is
         * completely okay to call MqttAndroid.disconnect() at any time.
         *
         * On the other hand, if we abort the ongoing connection request,
         * we need to call MqttAndroid.disconnect() to clean the internal
         * state of the MqttService.
         */
        Log.d(TAG, "Going to DISCONNECT " + dumpServerUris())
        try {
            client.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "disconnect: OK")
                    mCallback.onConnectionClosed(null/* Normal closure */)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    mCallback.onError("disconnect: NG", exception)
                }
            })
        } catch (exception: MqttException) {
            mCallback.onError("disconnect: EX", exception)
        }
    }

    override fun publish(message: T, userData: Any?) {
        if (client.isConnected) {
            try {
                var payload = compositeSerializer.toPayload(message)
                if (payload == null) {
                    Log.w(TAG, "Null payload, skip publish")
                    return
                }

                if (mCipherHandler != null) {
                    try {
                        payload = mCipherHandler.encrypt(payload, mCryptoPassword)
                    } catch (e: CryptoException) {
                        val description = e.message ?: "Encrypt Failure"
                        val cause = e.cause

                        mCallback.onError(description, cause)
                        return
                    }
                }

                client.publish(topicArray[0], payload, qosArray[0], retain,
                    null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "publish: OK")
                            mCallback.onPublished(message, userData)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            mCallback.onError("publish: NG", exception)
                        }
                    })
            } catch (exception: MqttPersistenceException) {
                mCallback.onError("publish: EX", exception)
            } catch (exception: IllegalArgumentException) {
                mCallback.onError("publish: EX", exception)
            } catch (exception: MqttException) {
                mCallback.onError("publish: EX", exception)
            } catch (exception: ClassCastException) {
                mCallback.onError("publish: EX", exception)
            }
        } else {
            mCallback.onError("Publish: Not yet connected", null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSerializer(params: Map<String, Any>): Serializer<T> {
        params[KEY_SERIALIZER]?.let {
            if (it is Serializer<*>) {
                return it as Serializer<T>
            }
        }
        return valueType.serializer as Serializer<T>
    }
}

enum class MqttProtocol(val version: Int) {
    MQTTv31(MqttConnectOptions.MQTT_VERSION_3_1),
    MQTTv311(MqttConnectOptions.MQTT_VERSION_3_1_1),
    ;
}
