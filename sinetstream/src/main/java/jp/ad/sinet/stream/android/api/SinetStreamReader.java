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

package jp.ad.sinet.stream.android.api;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.ad.sinet.stream.android.AndroidMessageReaderFactory;
import jp.ad.sinet.stream.android.api.low.AsyncMessageReader;
import jp.ad.sinet.stream.android.api.low.ReaderMessageCallback;

/**
 * Provides a set of API functions to be a Reader (= subscriber)
 * in the SINETStream system.
 * <p>
 *     Due to the nature of messaging system, all methods listed below
 *     should be handled as asynchronous requests.
 *     <ul>
 *         <li>initialize</li>
 *         <li>terminate</li>
 *     </ul>
 * </p>
 * <p>
 *     User of this class must implement the {@link SinetStreamReaderListener}
 *     in the calling {@link Activity}, so that the result of an asynchronous
 *     request or any error condition can be notified.
 * </p>
 */
public class SinetStreamReader {
    private final String TAG = SinetStreamReader.class.getSimpleName();

    private final Context mContext;
    private final SinetStreamReader.SinetStreamReaderListener mListener;
    private AsyncMessageReader<String> mReader = null;
    private boolean mServerReady = false;
    private boolean mIsSubscribed = false;

    /**
     * Constructs a SinetStreamReader instance.
     *
     * @param context the Application context which implements
     *                {@link SinetStreamReader.SinetStreamReaderListener},
     *                usually it is the calling {@link Activity} itself.
     *
     * @throws RuntimeException if given context does not implement
     *                          the required listener.
     */
    public SinetStreamReader(@NonNull Context context) {
        if (context instanceof SinetStreamReader.SinetStreamReaderListener) {
            mContext = context;
            mListener = (SinetStreamReader.SinetStreamReaderListener) context;
        } else {
            throw new RuntimeException(context.toString() +
                    " must implement SinetStreamReaderListener");
        }
    }

    /**
     * Connects to the broker and prepares oneself as a subscriber.
     *
     * <p>
     *     NB: Connection parameters will be specified by external configuration file.
     *     See https://www.sinetstream.net/docs/userguide/config.html
     * </p>
     *
     * @param serviceName the service name to match configuration parameters.
     */
    public void initialize(@NonNull String serviceName) {
        if (mReader != null) {
            Log.w(TAG, "Initialize: Calling sequence failure");
        } else {
            /* Get a new Reader instance. */
            AndroidMessageReaderFactory.Builder<String> builder =
                    new AndroidMessageReaderFactory.Builder<>();
            builder.setContext(mContext); // Mandatory
            builder.setService(serviceName); // Mandatory

            try {
                AndroidMessageReaderFactory<String> amrf = builder.build();
                mReader = amrf.getAsyncReader();
            } catch (NoConfigException |
                    InvalidConfigurationException |
                    UnsupportedServiceException e) {
                mListener.onError(e.toString());
                return;
            }

            /*
            try {
                mReader = new AndroidMessageReaderFactory.
                        Builder<String>().
                        service("service-1").
                        context(mContext).
                        build().
                        getAsyncReader();
            } catch (NoConfigException e) {
                mListener.onError(e.toString());
                return;
            }
             */

            /* Set collection of callback functions for this Reader instance. */
            setCallback();

            /*
             * Issue an async connect request to Broker.
             * It's result will be notified by one of callback functions.
             */
            mReader.connect();
        }
    }

    /**
     * Sets callback handlers for internal state management and user interaction.
     */
    private void setCallback() {
        if (mReader != null) {
            mReader.setCallback(new ReaderMessageCallback<String>() {
                @Override
                public void onConnectionEstablished() {
                    // Successfully connected to the Broker
                    Log.d(TAG, "onConnectionEstablished");

                    /* Try auto-subscribe for convenience. */
                    mServerReady = true;
                    mReader.subscribe();
                }

                @Override
                public void onConnectionClosed(@Nullable String reason) {
                    // Connection closed
                    Log.d(TAG, "onConnectionClosed: reason(" +
                            (reason != null? reason : "Normal closure") + ")");
                    mServerReady = false;
                    if (reason != null) {
                        mListener.onError(reason);
                    } else {
                        mListener.onReaderStatusChanged(false);
                    }
                }

                @Override
                public void onSubscribed() {
                    // Now, we are ready to receive messages from Broker.
                    Log.d(TAG, "onSubscribed");
                    mIsSubscribed = true;
                    mListener.onReaderStatusChanged(true);
                }

                @Override
                public void onUnsubscribed() {
                    // We won't receive messages anymore.
                    Log.d(TAG, "onUnsubscribed");
                    mIsSubscribed = false;
                    mListener.onReaderStatusChanged(false);

                    if (mReader != null) {
                        mReader.close();
                        //mReader.disconnect();
                    }
                }

                @Override
                public void onMessageReceived(@NonNull Message<String> message) {
                    Log.d(TAG, "onMessageReceived");
                    mListener.onMessageReceived(message.toString());
                }

                @Override
                public void onError(@NonNull String description, Throwable exception) {
                    String errmsg = description;
                    if (exception != null) {
                        errmsg += ": " + exception.toString();
                    }
                    mListener.onError(errmsg);
                }
            });
        }
    }

    /**
     * Disconnects from the broker and cleans up allocated resources.
     */
    public void terminate() {
        if (mReader != null) {
            if (mIsSubscribed) {
                mReader.unsubscribe();
            } else {
                mReader.close();
                //mReader.disconnect();
            }
        }
    }

    public interface SinetStreamReaderListener {
        /**
         * Called when availability status has changed.
         *
         * @param isReady true if "connected and subscribed" to the broker, false otherwise
         */
        void onReaderStatusChanged(boolean isReady);

        /**
         * Called when a message has received on any subscribed topic.
         *
         * @param data received message contents
         */
        void onMessageReceived(@NonNull String data);

        /**
         * Called when any error condition has met. The error might be detected
         * either at the sinetstream-android level, or at underneath library level.
         *
         * @param description brief description of the error.
         */
        void onError(@NonNull String description);
    }
}
