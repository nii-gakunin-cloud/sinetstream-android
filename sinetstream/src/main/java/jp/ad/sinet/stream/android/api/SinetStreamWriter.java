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

import jp.ad.sinet.stream.android.AndroidMessageWriterFactory;
import jp.ad.sinet.stream.android.api.low.AsyncMessageWriter;
import jp.ad.sinet.stream.android.api.low.WriterMessageCallback;

/**
 * Provides a set of API functions to be a Writer (= publisher)
 * in the SINETStream system.
 * <p>
 *     Due to the nature of messaging system, all methods listed below
 *     should be handled as asynchronous requests.
 *     <ul>
 *         <li>initialize</li>
 *         <li>terminate</li>
 *         <li>publish</li>
 *     </ul>
 * </p>
 * <p>
 *     User of this class must implement the {@link SinetStreamWriterListener}
 *     in the calling {@link Activity}, so that the result of an asynchronous
 *     request or any error condition can be notified.
 * </p>
 */
public class SinetStreamWriter {
    private final String TAG = SinetStreamWriter.class.getSimpleName();

    private final Context mContext;
    private final SinetStreamWriterListener mListener;
    private AsyncMessageWriter<String> mWriter = null;
    private boolean mServerReady = false;

    /**
     * Constructs a SinetStreamWriter instance.
     *
     * @param context the Application context which implements
     *                {@link SinetStreamReader.SinetStreamReaderListener},
     *                usually it is the calling {@link Activity} itself.
     *
     * @throws RuntimeException if given context does not implement
     *                          the required listener.
     */
    public SinetStreamWriter(@NonNull Context context) {
        if (context instanceof SinetStreamWriter.SinetStreamWriterListener) {
            mContext = context;
            mListener = (SinetStreamWriter.SinetStreamWriterListener) context;
        } else {
            throw new RuntimeException(context.toString() +
                    " must implement SinetStreamWriterListener");
        }
    }

    /**
     * Connects to the broker and prepares oneself as a publisher.
     *
     * <p>
     *     NB: Connection parameters will be specified by external configuration file.
     *     See https://www.sinetstream.net/docs/userguide/config.html
     * </p>
     *
     * @param serviceName the service name to match configuration parameters.
     */
    public void initialize(@NonNull String serviceName) {
        if (mWriter != null) {
            Log.w(TAG, "Initialize: Calling sequence failure");
        } else {
            /* Get a new Writer instance. */
            try {
                AndroidMessageWriterFactory.Builder<String> builder =
                        new AndroidMessageWriterFactory.Builder<>();
                builder.setContext(mContext); // Mandatory
                builder.setService(serviceName); // Mandatory
                try {
                    AndroidMessageWriterFactory<String> amwf = builder.build();
                    mWriter = amwf.getAsyncWriter();
                } catch (
                        NoConfigException |
                                InvalidConfigurationException |
                                UnsupportedServiceException e) {
                    mListener.onError(e.toString());
                    return;
                }

                /*
                mWriter = new AndroidMessageWriterFactory.
                        Builder<String>().
                        service("service-1").
                        context(mContext).
                        build().
                        getAsyncWriter();
                 */
            } catch (NoConfigException e) {
                mListener.onError(e.toString());
                return;
            }

            /* Set collection of callback functions for this Writer instance. */
            setCallback();

            /*
             * Issue an async connect request to Broker.
             * It's result will be notified by one of callback functions.
             */
            mWriter.connect();
        }
    }

    /**
     * Sets callback handlers for internal state management and user interaction.
     */
    private void setCallback() {
        mWriter.setCallback(new WriterMessageCallback<String>() {
            @Override
            public void onConnectionEstablished() {
                // Successfully connected to the Broker
                Log.d(TAG, "onConnectionEstablished");

                /* OK, now we are ready to publish */
                mServerReady = true;
                mListener.onWriterStatusChanged(true);
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
                    mListener.onWriterStatusChanged(false);
                }
            }

            @Override
            public void onPublished(String message, Object userData) {
                // Publish completed
                Log.d(TAG, "onPublished");
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

    /**
     * Disconnects from the broker and cleans up allocated resources.
     */
    public void terminate() {
        if (mWriter != null) {
            mWriter.close();
            //mWriter.disconnect();
        }
    }

    /**
     * Publishes given message to the broker.
     *
     * @param message a message to be published.
     * @param userData User specified opaque object, to be returned by
     *                 {@link WriterMessageCallback<String>#onPublished()} as is.
     */
    public void publish(@NonNull String message, @Nullable Object userData) {
        if (mWriter != null) {
            if (mServerReady) {
                mWriter.publish(message, userData);
            } else {
                Log.w(TAG, "publish: Server is not yet ready");
            }
        } else {
            Log.w(TAG, "Publish: Calling sequence failure");
            mListener.onError("Publish: Calling sequence failure");
        }
    }

    public interface SinetStreamWriterListener {
        /**
         * Called when availability status has changed.
         *
         * @param isReady true if "connected" to the broker, false otherwise
         */
        void onWriterStatusChanged(boolean isReady);

        /**
         * Called when any error condition has met. The error might be detected
         * either at the sinetstream-android level, or at underneath library level.
         *
         * @param description brief description of the error.
         */
        void onError(@NonNull String description);
    }
}
