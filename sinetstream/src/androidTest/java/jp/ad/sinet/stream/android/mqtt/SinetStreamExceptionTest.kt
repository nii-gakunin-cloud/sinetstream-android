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
import jp.ad.sinet.stream.android.api.InvalidConfigurationException
import jp.ad.sinet.stream.android.api.NoConfigException
import jp.ad.sinet.stream.android.api.NoServiceException
import jp.ad.sinet.stream.android.api.UnsupportedServiceException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class NoConfigExceptionTest {
    @Test
    fun asyncWriter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<NoConfigException> {
            AndroidMessageWriterFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncWriter()
        }
    }

    @Test
    fun asyncReader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<NoConfigException> {
            AndroidMessageReaderFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncReader()
        }
    }

    @BeforeEach
    fun cleanConfigFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configFile = File(context.filesDir, CONFIG_FILE)
        if (configFile.exists()) {
            configFile.delete()
        }
    }
}

class NoServiceExceptionTest {

    @Test
    fun asyncWriter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<NoServiceException> {
            AndroidMessageWriterFactory.Builder<String>(context)
                .service(TEST_SERVICE_X).build().getAsyncWriter()
        }
    }

    @Test
    fun asyncReader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<NoServiceException> {
            AndroidMessageReaderFactory.Builder<String>(context)
                .service(TEST_SERVICE_X).build().getAsyncReader()
        }
    }

    @BeforeEach
    fun setupConfigFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configFile = File(context.filesDir, CONFIG_FILE)
        configFile.bufferedWriter().use { f ->
            f.write("""
                $TEST_SERVICE:
                  type: mqtt
            """.trimIndent())
        }
    }
}

class UnsupportedServiceExceptionTest {

    @Test
    fun asyncWriter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<UnsupportedServiceException> {
            AndroidMessageWriterFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncWriter()
        }
    }

    @Test
    fun asyncReader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<UnsupportedServiceException> {
            AndroidMessageReaderFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncReader()
        }
    }

    @BeforeEach
    fun setupConfigFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configFile = File(context.filesDir, CONFIG_FILE)
        configFile.bufferedWriter().use { f ->
            f.write(
                """
                $TEST_SERVICE:
                  type: xxx
            """.trimIndent()
            )
        }
    }
}

class InvalidConfigurationExceptionTest {
    @Test
    fun asyncWriter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<InvalidConfigurationException> {
            AndroidMessageWriterFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncWriter()
        }
    }

    @Test
    fun asyncReader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThrows<InvalidConfigurationException> {
            AndroidMessageReaderFactory.Builder<String>(context)
                .service(TEST_SERVICE).build().getAsyncReader()
        }
    }

    @BeforeEach
    fun setupBadFormatConfigFile() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configFile = File(context.filesDir, CONFIG_FILE)
        configFile.bufferedWriter().use { f ->
            f.write(
                """
                [$TEST_SERVICE]
                type=mqtt
            """.trimIndent()
            )
        }
    }
}
