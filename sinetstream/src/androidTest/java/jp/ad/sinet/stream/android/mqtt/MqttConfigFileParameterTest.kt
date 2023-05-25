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

package jp.ad.sinet.stream.android.mqtt

import androidx.test.platform.app.InstrumentationRegistry
import jp.ad.sinet.stream.android.AndroidMessageReaderFactory
import jp.ad.sinet.stream.android.AndroidMessageWriterFactory
import jp.ad.sinet.stream.android.api.Consistency
import jp.ad.sinet.stream.android.api.InvalidConfigurationException
import jp.ad.sinet.stream.android.api.ValueType
import jp.ad.sinet.stream.android.api.low.AsyncMessageReader
import jp.ad.sinet.stream.android.api.low.AsyncMessageWriter
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.util.*

typealias WriterOp = (AsyncMessageWriter<*>) -> Unit
typealias ReaderOp = (AsyncMessageReader<*>) -> Unit
typealias MqttWriterOp = (MqttAsyncMessageWriter<*>) -> Unit
typealias MqttReaderOp = (MqttAsyncMessageReader<*>) -> Unit

class ServiceTest : ReaderWriterTest {
    @Test
    fun asyncWriter() {
        super.asyncWriter { writer ->
            Assertions.assertEquals(TEST_SERVICE, writer.service)
        }
    }

    @Test
    fun asyncReader() {
        super.asyncReader { reader ->
            Assertions.assertEquals(TEST_SERVICE, reader.service)
        }
    }

    @BeforeEach
    fun setupServiceTestConfigFile() {
        setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            $TEST_SERVICE_2:
                type: mqtt
                transport: websocket
                topic: test-sinetstream-android-20191026
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
    }
}

class MqttBrokersTest {

    @Nested
    inner class StringTypeBrokers : NewInstanceTest {
        @BeforeEach
        fun setupBrokersInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class ListTypeBrokers : NewInstanceTest {
        @BeforeEach
        fun setupBrokersInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers:
                    - mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class MultipleBrokers : InvalidConfigurationTest {
        @BeforeEach
        fun setupBrokersInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers:
                    - mqtt1.vcp-handson.org
                    - mqtt2.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class NoBroker : InvalidConfigurationTest {
        @BeforeEach
        fun setupBrokersInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
            """.trimIndent())
        }
    }
}

class MqttTopicTest {

    val topic = "test-sinetstream-android-20191026"
    val topic2 = "test-sinetstream-android-20191026-2"

    @Nested
    inner class StringTypeTopic : ReaderWriterTest {
        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                Assertions.assertEquals(topic, writer.topic)
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                Assertions.assertEquals(topic, reader.topic)
            }
        }

        @BeforeEach
        fun setupTopicInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: $topic
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class ListType1Topic : ReaderWriterTest {
        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                Assertions.assertEquals(topic, writer.topic)
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                Assertions.assertEquals(topic, reader.topic)
            }
        }

        @BeforeEach
        fun setupTopicsInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic:
                    - $topic
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class ListTypeTopic : ReaderWriterTest {
        @Test
        fun asyncWriter() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertThrows<IllegalArgumentException> {
                AndroidMessageWriterFactory.Builder<String>(context)
                    .service(TEST_SERVICE).build().getAsyncWriter()
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                val topics = listOf(topic, topic2)
                Assertions.assertEquals(topics, reader.topics)
                Assertions.assertEquals(topics.joinToString(), reader.topic)
            }
        }

        @BeforeEach
        fun setupTopicsInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic:
                    - $topic
                    - $topic2
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class NoTopic {
        @Test
        fun asyncWriter() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertThrows<IllegalArgumentException> {
                AndroidMessageWriterFactory.Builder<String>(context)
                    .service(TEST_SERVICE).build().getAsyncWriter()
            }
        }

        @Test
        fun asyncReader() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            assertThrows<IllegalArgumentException> {
                AndroidMessageReaderFactory.Builder<String>(context)
                    .service(TEST_SERVICE).build().getAsyncReader()
            }
        }

        @BeforeEach
        fun setupTopicsInConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }
}

class ClientIdTest {

    val clientId = "test-android-sinetstream-client-20191021"

    @Nested
    inner class NotSpecifyClientId : ReaderWriterTest {
        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                assertTrue(writer.clientId.isNotBlank())
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                assertTrue(reader.clientId.isNotBlank())
            }
        }

        @BeforeEach
        fun setupClientIdTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class SpecifyClientId : ReaderWriterTest {
        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                Assertions.assertEquals(clientId, writer.clientId)
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                Assertions.assertEquals(clientId, reader.clientId)
            }
        }

        @BeforeEach
        fun setupServiceTestConfigFile() {
            setupConfigFile(
                """
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                client_id: $clientId
            """.trimIndent()
            )
        }
    }
}

class ConsistencyTest {

    interface ConsistencyValueTest : ReaderWriterTest {
        val consistency: Consistency

        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                Assertions.assertEquals(consistency, writer.consistency)
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                Assertions.assertEquals(consistency, reader.consistency)
            }
        }

        @BeforeEach
        fun setupConsistencyTestConfigFile() {
            setupConfigFile(
                """
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                consistency: $consistency
            """.trimIndent()
            )
        }
    }

    @Nested
    inner class AtMostOnce : ConsistencyValueTest {
        override val consistency: Consistency
            get() = Consistency.AT_MOST_ONCE
    }

    @Nested
    inner class AtLeastOnce : ConsistencyValueTest {
        override val consistency: Consistency
            get() = Consistency.AT_LEAST_ONCE
    }

    @Nested
    inner class ExactlyOnce : ConsistencyValueTest {
        override val consistency: Consistency
            get() = Consistency.EXACTLY_ONCE
    }

    @Nested
    inner class NoConsistencyTest : ReaderWriterTest {

        private val consistency: Consistency = Consistency.AT_MOST_ONCE

        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                Assertions.assertEquals(consistency, writer.consistency)
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                Assertions.assertEquals(consistency, reader.consistency)
            }
        }

        @BeforeEach
        fun setupConsistencyTestConfigFile() {
            setupConfigFile(
                """
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            """.trimIndent()
            )
        }
    }

    @Nested
    inner class BadConsistencyTest {
        @Test
        fun asyncWriter() {
            assertThrows<InvalidConfigurationException> {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                AndroidMessageWriterFactory.Builder<String>(context)
                    .service(TEST_SERVICE).build().getAsyncWriter()
            }
        }

        @Test
        fun asyncReader() {
            assertThrows<InvalidConfigurationException> {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                AndroidMessageReaderFactory.Builder<String>(context)
                    .service(TEST_SERVICE).build().getAsyncReader()
            }
        }

        @BeforeEach
        fun setupConsistencyTestConfigFile() {
            setupConfigFile(
                """
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                consistency: xxx
            """.trimIndent()
            )
        }
    }
}

class ValueTypeTest {

    interface ValueTypeValueTest : ReaderWriterTest {
        val valueType: ValueType

        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                Assertions.assertEquals(valueType, writer.valueType)
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                Assertions.assertEquals(valueType, reader.valueType)
            }
        }

        @BeforeEach
        fun setupConsistencyTestConfigFile() {
            setupConfigFile(
                """
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                value_type: ${valueType.toString().lowercase(Locale.getDefault())}
            """.trimIndent()
            )
        }
    }

    @Nested
    inner class Text : ValueTypeValueTest {
        override val valueType: ValueType
            get() = ValueType.TEXT
    }

    @Nested
    inner class ByteArray : ValueTypeValueTest {
        override val valueType: ValueType
            get() = ValueType.BYTE_ARRAY
    }

    @Nested
    inner class NoValueTypeTest : ReaderWriterTest {

        private val valueType: ValueType = ValueType.TEXT

        @Test
        fun asyncWriter() {
            super.asyncWriter { writer ->
                Assertions.assertEquals(valueType, writer.valueType)
            }
        }

        @Test
        fun asyncReader() {
            super.asyncReader { reader ->
                Assertions.assertEquals(valueType, reader.valueType)
            }
        }

        @BeforeEach
        fun setupConsistencyTestConfigFile() {
            setupConfigFile(
                """
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            """.trimIndent()
            )
        }
    }

    @Nested
    inner class BadValueTypeTest {
        @Test
        fun asyncWriter() {
            assertThrows<InvalidConfigurationException> {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                AndroidMessageWriterFactory.Builder<String>(context)
                    .service(TEST_SERVICE).build().getAsyncWriter()
            }
        }

        @Test
        fun asyncReader() {
            assertThrows<InvalidConfigurationException> {
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                AndroidMessageReaderFactory.Builder<String>(context)
                    .service(TEST_SERVICE).build().getAsyncReader()
            }
        }

        @BeforeEach
        fun setupConsistencyTestConfigFile() {
            setupConfigFile(
                """
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                value_type: xxx
            """.trimIndent()
            )
        }
    }
}

class BrokerUrlTest {

    interface BrokerUrlTest: MqttMessageIOTest {
        val expectedBrokerUrl: String

        @Test
        fun asyncWriter() {
            super.mqttAsyncWriter { writer ->
                Assertions.assertEquals(expectedBrokerUrl, writer.brokerUrl)
            }
        }

        @Test
        fun asyncReader() {
            super.mqttAsyncReader { reader ->
                Assertions.assertEquals(expectedBrokerUrl, reader.brokerUrl)
            }
        }
    }

    val broker = "mqtt.vcphandson.org"

    @Nested
    inner class DefaultSetting : BrokerUrlTest {
        override val expectedBrokerUrl: String
            get() = "tcp://$broker"

        @BeforeEach
        fun setupBrokerUrlTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: $broker
            """.trimIndent())
        }
    }

    @Nested
    inner class Tls : BrokerUrlTest {
        override val expectedBrokerUrl: String
            get() = "tcps://$broker"

        @BeforeEach
        fun setupBrokerUrlTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: $broker
                tls: true
            """.trimIndent())
        }
    }

    @Nested
    inner class NoTls : BrokerUrlTest {
        override val expectedBrokerUrl: String
            get() = "tcp://$broker"

        @BeforeEach
        fun setupBrokerUrlTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: $broker
                tls: false
            """.trimIndent())
        }
    }

    @Nested
    inner class Websocket : BrokerUrlTest {
        override val expectedBrokerUrl: String
            get() = "ws://$broker"

        @BeforeEach
        fun setupBrokerUrlTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: $broker
                transport: websocket
            """.trimIndent())
        }
    }

    @Nested
    inner class SecureWebsocket : BrokerUrlTest {
        override val expectedBrokerUrl: String
            get() = "wss://$broker"

        @BeforeEach
        fun setupBrokerUrlTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: $broker
                transport: websocket
                tls: true
            """.trimIndent())
        }
    }
}

class RetainTest {

    interface RetainTest : MqttMessageIOTest {
        val expected: Boolean

        @Test
        fun asyncWriter() {
            super.mqttAsyncWriter { writer ->
                Assertions.assertEquals(expected, writer.retain)
            }
        }

        @Test
        fun asyncReader() {
            super.mqttAsyncReader {}
        }
    }

    @Nested
    inner class DefaultSetting : RetainTest {
        override val expected: Boolean
            get() = false

        @BeforeEach
        fun setupRetainTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class RetainSetting : RetainTest {
        override val expected: Boolean
            get() = true

        @BeforeEach
        fun setupRetainTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                retain: true
            """.trimIndent())
        }
    }

    @Nested
    inner class NoRetainSetting : RetainTest {
        override val expected: Boolean
            get() = false

        @BeforeEach
        fun setupRetainTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                retain: false
            """.trimIndent())
        }
    }
}

class ProtocolTest {

    interface ProtocolTest : MqttMessageIOTest {
        val expected: MqttProtocol

        @Test
        fun asyncWriter() {
            super.mqttAsyncWriter { writer ->
                Assertions.assertEquals(expected, writer.protocol)
            }
        }

        @Test
        fun asyncReader() {
            super.mqttAsyncReader { reader ->
                Assertions.assertEquals(expected, reader.protocol)
            }
        }
    }

    @Nested
    inner class DefaultSetting : ProtocolTest {
        override val expected: MqttProtocol
            get() = MqttProtocol.MQTTv311

        @BeforeEach
        fun setupProtocolTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class V311Setting : ProtocolTest {
        override val expected: MqttProtocol
            get() = MqttProtocol.MQTTv311

        @BeforeEach
        fun setupProtocolTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                protocol: MQTTv311
            """.trimIndent())
        }
    }

    @Nested
    inner class V31Setting : ProtocolTest {
        override val expected: MqttProtocol
            get() = MqttProtocol.MQTTv31

        @BeforeEach
        fun setupProtocolTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                protocol: MQTTv31
            """.trimIndent())
        }
    }
}

class UsernamePasswordTest {
    @Nested
    inner class UsernamePasswordSetting : NewInstanceTest {
        @BeforeEach
        fun setupUsernamePasswordTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                username_pw_set:
                    username: user
                    password: pass
            """.trimIndent())
        }
    }

    @Nested
    inner class BadFormatSetting : InvalidConfigurationTest {
        @BeforeEach
        fun setupUsernamePasswordTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                username_pw_set: true
            """.trimIndent())
        }
    }
}

class ConnectParametersTest {
    @Nested
    inner class ConnectSetting : NewInstanceTest {
        @BeforeEach
        fun setupConnectTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                connect:
                    keepalive: 600
                    automatic_reconnect: true
                    connect_timeout: 180
            """.trimIndent())
        }
    }

    @Nested
    inner class DefaultSetting : MqttMessageIOTest {

        @Test
        fun asyncWriter() {
            /* Writer.connectOptions has obsoleted.
            super.mqttAsyncWriter { writer ->
                assertTrue(writer.connectOptions.isAutomaticReconnect)
                assertEquals(30, writer.connectOptions.keepAliveInterval)
            }
             */
        }

        @Test
        fun asyncReader() {
            /* Reader.connectOptions has obsoleted.
            super.mqttAsyncReader { reader ->
                assertTrue(reader.connectOptions.isAutomaticReconnect)
                assertEquals(30, reader.connectOptions.keepAliveInterval)
            }
             */
        }

        @BeforeEach
        fun setupConnectTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
            """.trimIndent())
        }
    }

    @Nested
    inner class BadFormatSetting : InvalidConfigurationTest {
        @BeforeEach
        fun setupConnectTestConfigFile() {
            setupConfigFile("""
            $TEST_SERVICE:
                type: mqtt
                topic: test-sinetstream-android-20191021
                brokers: mqtt.vcp-handson.org
                connect: true
            """.trimIndent())
        }
    }
}

interface ReaderWriterTest {
    fun asyncWriter(assertion: WriterOp?) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val writer = AndroidMessageWriterFactory.Builder<String>(context)
            .service(TEST_SERVICE).build().getAsyncWriter()
        assertion?.invoke(writer)
    }

    fun asyncReader(assertion: ReaderOp?) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reader = AndroidMessageReaderFactory.Builder<String>(context)
            .service(TEST_SERVICE).build().getAsyncReader()
        assertion?.invoke(reader)
    }
}

interface NewInstanceTest : ReaderWriterTest {
    @Test
    fun asyncWriter() {
        super.asyncWriter(null)
    }

    @Test
    fun asyncReader() {
        super.asyncReader(null)
    }
}

interface MqttMessageIOTest : ReaderWriterTest {

    fun mqttAsyncWriter(assertion: MqttWriterOp?) {
        super.asyncWriter { writer ->
            Assertions.assertEquals(MqttAsyncMessageWriter::class, writer::class)
            if (writer is MqttAsyncMessageWriter) {
                assertion?.invoke(writer)
            }
        }
    }

    fun mqttAsyncReader(assertion: MqttReaderOp?) {
        super.asyncReader { reader ->
            Assertions.assertEquals(MqttAsyncMessageReader::class, reader::class)
            if (reader is MqttAsyncMessageReader) {
                assertion?.invoke(reader)
            }
        }
    }
}

interface ExceptionTest<T : Throwable> {
    fun asyncWriter(clazz: Class<T>) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assertions.assertThrows(clazz) {
            AndroidMessageWriterFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncWriter()
        }
    }

    fun asyncReader(clazz: Class<T>) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assertions.assertThrows(clazz) {
            AndroidMessageReaderFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncReader()
        }
    }
}

interface InvalidConfigurationTest : ExceptionTest<InvalidConfigurationException> {
    @Test
    fun asyncWriter() {
        super.asyncWriter(InvalidConfigurationException::class.java)
    }

    @Test
    fun asyncReader() {
        super.asyncReader(InvalidConfigurationException::class.java)
    }
}

fun setupConfigFile(configuration: String) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    File(context.filesDir, CONFIG_FILE).bufferedWriter().use {
        it.write(configuration)
    }
}
