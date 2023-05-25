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

package jp.ad.sinet.stream.android.api.low

import jp.ad.sinet.stream.android.api.Consistency
import jp.ad.sinet.stream.android.api.Message
import jp.ad.sinet.stream.android.api.Deserializer
import jp.ad.sinet.stream.android.api.Serializer
import jp.ad.sinet.stream.android.api.ValueType

interface AsyncMessageIO {
    val service: String
    val topic: String
    val clientId: String
    val consistency: Consistency
    val qos: Int
    val valueType: ValueType
    val dataEncryption: Boolean
        get() = false
    val config: Map<String, Any>
    fun close()
}

interface AsyncMessageReader<T> :
    AsyncMessageIO {
    /**
     * Binds given ReaderMessageCallback<T> to this AsyncMessageReader<T>.
     *
     * @param callback the ReaderMessageCallback<T> implementation, not {@code null}.
     */
    fun setCallback(callback: ReaderMessageCallback<T>)

    /**
     * Issues an async connect request to the Broker.
     * NB: Connection parameters will be specified by external configuration file.
     */
    fun connect()

    /**
     * Issues an async disconnect request to the Broker.
     */
    fun disconnect()

    /**
     * Issues an async subscribe request to the Broker.
     * NB:
     * <ul>
     *     <li> Subscribe parameters will be specified by external configuration file.
     *     <li> Connection to the Broker must have established beforehand.
     * </ul>
     */
    fun subscribe()

    /**
     * Issues an async unsubscribe request to the Broker.
     * NB: Connection to the Broker must have established beforehand.
     */
    fun unsubscribe()

    val topics: List<String>
    val deserializer: Deserializer<T>
}

interface ReaderMessageCallback<T> {
    /**
     * Called when AsyncMessageReader.connect() has completed successfully.
     *
     * @param reconnect {@code false} if this is the initial connection.
     */
    fun onConnectionEstablished(reconnect: Boolean)

    /**
     * Called when the broker connection has closed.
     * Possible cases are 1) user requested disconnect and completed normally,
     * or 2) Connection has closed by peer unexpectedly.
     *
     * @param reason the reason behind the loss of connection, {@code null} if normal closure.
     */
    fun onConnectionClosed(reason: String?)

    /**
     * Called when the broker connection has lost, and the auto-reconnect
     * sequence has triggered within the Paho Mqtt Android client library.
     * Once this attempt successfully completed, {@link onConnectionEstablished()}
     * will be called. Otherwise {@link onError()} will be called.
     *
     * NB1: This method is effective only if the automatic reconnect option
     * in the MqttConnectOptions has set as enabled.
     *
     * NB2: Once this method has called, user should stop calling {@link publish()}
     * until further notice by {@link onConnectionEstablished()}.
     */
    fun onReconnectInProgress()

    /**
     * Called when AsyncMessageWriter.subscribe() has completed successfully.
     */
    fun onSubscribed()

    /**
     * Called when AsyncMessageWriter.unsubscribe() has completed successfully.
     */
    fun onUnsubscribed()

    /**
     * Called when a message arrives from the server.
     *
     * @param message the SINETStream v1.1 format {@link Message<T>} object, not {@code null}.
     */
    fun onMessageReceived(message: Message<T>)

    /**
     * Called when any error condition has met. The error might be detected
     * either at this sinetstream-android library, or at somewhere in the
     * lower level libraries.
     *
     * @param description brief description of the error, not {@code null}.
     * @param exception optional info detected at the error point.
     */
    fun onError(description: String, exception: Throwable?)
}

interface AsyncMessageWriter<T> :
    AsyncMessageIO {
    /**
     * Binds given WriterMessageCallback<T> to this AsyncMessageWriter<T>.
     */
    fun setCallback(callback: WriterMessageCallback<T>)

    /**
     * Issues an async connect request to the Broker.
     * NB: Connection parameters will be specified by external configuration file.
     */
    fun connect()

    /**
     * Issues an async disconnect request to the Broker.
     */
    fun disconnect()

    /**
     * Publishes a message to a topic on the server.
     * NB: Connection to the Broker must have established beforehand.
     *
     * @param message the SINETStream {@link Message<T>} object, not {@code null}.
     * @param userData User specified opaque object, to be returned by
     *                 {@link WriterMessageCallback#onPublished()} as is.
     */
    fun publish(message: T, userData: Any?)

    val serializer: Serializer<T>
}

interface WriterMessageCallback<T> {
    /**
     * Called when AsyncMessageWriter.connect() has completed successfully.
     *
     * @param reconnect {@code false} if this is the initial connection.
     */
    fun onConnectionEstablished(reconnect: Boolean)

    /**
     * Called when the broker connection has closed.
     * Possible cases are 1) user requested disconnect and completed normally,
     * or 2) Connection has closed by peer unexpectedly.
     *
     * @param reason the reason behind the loss of connection, {@code null} if normal closure.
     */
    fun onConnectionClosed(reason: String?)

    /**
     * Called when the broker connection has lost, and the auto-reconnect
     * sequence has triggered within the Paho Mqtt Android client library.
     * Once this attempt successfully completed, {@link onConnectionEstablished()}
     * will be called. Otherwise {@link onError()} will be called.
     *
     * NB1: This method is effective only if the automatic reconnect option
     * in the MqttConnectOptions has set as enabled.
     *
     * NB2: Once this method has called, user should stop calling {@link publish()}
     * until further notice by {@link onConnectionEstablished()}.
     */
    fun onReconnectInProgress()

    /**
     * Called when AsyncMessageWriter.publish() has completed successfully.
     *
     * @param message the SINETStream {@link Message<T>} object, not {@code null}.
     * @param userData User specified opaque object, passed by {@code publish()}.
     */
    fun onPublished(message: T, userData: Any?)

    /**
     * Called when any error condition has met. The error might be detected
     * either at this sinetstream-android library, or at somewhere in the
     * lower level libraries.
     *
     * @param description brief description of the error, not {@code null}.
     * @param exception optional info detected at the error point.
     */
    fun onError(description: String, exception: Throwable?)
}
