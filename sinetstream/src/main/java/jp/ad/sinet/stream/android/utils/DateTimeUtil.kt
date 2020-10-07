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

package jp.ad.sinet.stream.android.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class DateTimeUtil {
    private val mSimpleDateFormat: SimpleDateFormat
    val unixTime: Long
        get() = System.currentTimeMillis()

    fun toIso8601String(unixTime: Long): String {
        val dstDateString: String
        dstDateString = try {
            mSimpleDateFormat.format(Date(unixTime))
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Invalid unixTime(" + unixTime + "): " + e.message)
            "" + unixTime /* Fallback to raw value */
        }
        return dstDateString
    }

    companion object {
        private val TAG = DateTimeUtil::class.java.simpleName
    }

    init {
        /* We use standard ISO 8601 format. */
        val timestampFormatIso8601 = "yyyyMMdd'T'HHmmss.SSSZ"
        mSimpleDateFormat = SimpleDateFormat(timestampFormatIso8601, Locale.US)

        /* We need TimeZone to convert from UTC to local time. */
        val tz = TimeZone.getDefault()
        mSimpleDateFormat.timeZone = tz
    }
}
