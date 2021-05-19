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

import jp.ad.sinet.stream.android.api.*
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.parser.ParserException
import java.io.File
import java.util.*

object AndroidConfigLoader {
    const val KEY_CONSISTENCY = "consistency"
    const val KEY_BROKERS = "brokers"
    const val KEY_SERIALIZER = "serializer"
    const val KEY_DESERIALIZER = "deserializer"
    const val KEY_TLS = "tls"

    fun load(dir: File?, serviceName: String): Map<String, Any> {
        val configFile = File(dir, ApiKeys.CONFIG_FILENAME)
        if (!configFile.canRead()) {
            throw NoConfigException("$configFile: Cannot read?")
        }
        val params = try {
            Yaml().load<Map<String, Map<String, Any>>>(configFile.inputStream())
        } catch (ex: ParserException) {
            throw InvalidConfigurationException(ex.message, ex)
        } catch (ex: YAMLException) {
            throw InvalidConfigurationException(ex.message, ex)
        }

        if (params == null || params.isEmpty()) {
            throw InvalidConfigurationException(
                "Empty configuration file?", null
            )
        }
        return params[serviceName]?.let {
            translate(
                it
            )
        } ?: throw NoServiceException("Unknown $serviceName")
    }

    private fun translate(params: Map<String, Any>): Map<String, Any> {
        val p = params.toMutableMap()
        replaceEnum<Consistency>(
            p,
            KEY_CONSISTENCY
        )
        replaceEnum<ValueType>(
            p,
            "value_type"
        )
        return p
    }

    private inline fun <reified E : Enum<*>> replaceEnum(
        params: MutableMap<String, Any>,
        key: String
    ) {
        params[key]?.let {
            when (it) {
                is E -> it
                is String -> it.let {
                    try {
                        E::class.java.getMethod("valueOf", String::class.java).invoke(
                            null, it.uppercase(
                                Locale.ROOT
                            )
                        ) as E
                    } catch (ex: Exception) {
                        throw InvalidConfigurationException(
                            "An incorrect value was specified for $key.: $it",
                            ex
                        )
                    }
                }
                else -> null
            }
        }?.let { params[key] = it }
    }
}