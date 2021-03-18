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

import java.util.Arrays;

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
 *         <li>initialize + setup</li>
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
public class SinetStreamWriter<T> {
    private final String TAG = SinetStreamWriter.class.getSimpleName();

    private final Context mContext;
    private final SinetStreamWriterListener<T> mListener;
    private AsyncMessageWriter<T> mWriter = null;
    private boolean mServerReady = false;

    /**
     * Constructs a SinetStreamWriter instance.
     *
     * @param context the Application context which implements
     *                {@link SinetStreamWriterListener},
     *                usually it is the calling {@link Activity} itself.
     *
     * @throws RuntimeException if given context does not implement
     *                          the required listener.
     */
    public SinetStreamWriter(@NonNull Context context) {
        if (context instanceof SinetStreamWriterListener) {
            mContext = context;

            /* Set annotation to suppress "unchecked cast" warning */
            //noinspection unchecked
            mListener = (SinetStreamWriterListener<T>) context;
        } else {
            throw new RuntimeException(context.toString() +
                    " must implement SinetStreamWriterListener");
        }
    }

    /**
     * Allocates a message Writer instance.
     * <p>
     *     This method internally allocates a message writer instance
     *     which matches to the user-specified messaging system.<br>
     *     Currently, we only support MQTT for it.
     * </p>
     *
     * <p>
     *     Technically, initialization process is separated in two phases.
     *     <ol>
     *         <li>{@code initialize()}</li>
     *         - this method; for an instance allocation
     *         <li>{@link #setup()}</li>
     *         - next method; for preparation and initial connect request
     *     </ol>
     * </p>
     *
     * <p>
     *     This method {@code initialize()} runs synchronously.
     *     Once it returns without error, call {@code setup()} next.
     * </p>
     *
     * @param serviceName the service name to match configuration parameters.
     */
    public void initialize(@NonNull String serviceName) {
        if (mWriter != null) {
            /* Show log message and do nothing here. */
            Log.d(TAG, "Initialize: Getting back from background");
        } else {
            /* Get a new Writer instance. */
            try {
                AndroidMessageWriterFactory.Builder<T> builder =
                        new AndroidMessageWriterFactory.Builder<>();
                builder.setContext(mContext); // Mandatory
                builder.setService(serviceName); // Mandatory
                try {
                    AndroidMessageWriterFactory<T> amwf = builder.build();
                    mWriter = amwf.getAsyncWriter();
                } catch (NoConfigException |
                        InvalidConfigurationException |
                        NoServiceException |
                        UnsupportedServiceException e) {
                    mListener.onError(e.toString());
                }
            } catch (NoConfigException e) {
                mListener.onError(e.toString());
            }
        }
    }

    /**
     * Check if initialization has successfully completed.
     *
     * @return true if a message Writer has allocated, false otherwise
     */
    public boolean isInitializationSuccess() {
        return (mWriter != null);
    }

    /**
     * Sets up a connection to the Broker via underlying messaging system.
     * <p>
     *     Actually, this is the latter half of initialization process.<br>
     *     In this method, internal callback handlers
     *     {@link WriterMessageCallback}
     *     will be associated with the given
     *     {@link SinetStreamWriter.SinetStreamWriterListener}
     *     to notify events to the user level.<br>
     *     As the last step of setup sequence, initial connect request
     *     to the Broker will be issued via underlying messaging system.
     * </p>
     * <p>
     *     NB: Connection parameters will be specified by external configuration file.
     * </p>
     *
     * @see <a href=https://www.sinetstream.net/docs/userguide/config.html>
     *     https://www.sinetstream.net/docs/userguide/config.html</a>
     */
    public void setup() {
        /* Make sure this function is called just once */
        if (mWriter != null && !mServerReady) {
            /* Set collection of callback functions for this Writer instance. */
            setCallback();

            /*
             * Issue an asynchronous connect request to the Broker.
             * It's result will be notified by one of callback functions.
             */
            mWriter.connect();
        } else {
            /* Show log message and do nothing here. */
            Log.d(TAG, "Setup: already done");
        }
    }

    /**
     * Sets callback handlers for internal state management and user interaction.
     */
    private void setCallback() {
        mWriter.setCallback(new WriterMessageCallback<T>() {
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
            public void onPublished(T message, Object userData) {
                // Publish completed
                Log.d(TAG, "onPublished");
                mListener.onPublished(message, userData);
            }

            @Override
            public void onError(@NonNull String description, Throwable exception) {
                String errmsg = description;
                if (exception != null) {
                    String stack = Arrays.toString(exception.getStackTrace());
                    Log.e(TAG, "onError(exception): \n" +
                            stack.replaceAll(", ", "\n "));
                    errmsg += ": " + exception.toString();
                }
                mListener.onError(errmsg);
            }
        });
    }

    /**
     * Disconnects from the Broker and cleans up allocated resources.
     */
    public void terminate() {
        if (mWriter != null) {
            //mWriter.close();
            mWriter.disconnect();
        }
    }

    /**
     * Publishes given message to the Broker.
     *
     * @param message a message to be published.
     * @param userData User specified opaque object, to be returned by
     *                 {@link SinetStreamWriterListener#onPublished(Object, Object)
     *                 SinetStreamWriterListener#onPublished(T, Object)} as is.
     */
    public void publish(@NonNull T message, @Nullable Object userData) {
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

    /**
     * Relay "wrapper class detected" error message to the UI.
     *
     * @param description brief description of the error.
     */
    public void abort(@NonNull String description) {
        mListener.onError(description);
    }

    /**
     * Returns the ValueType specified by user.
     * <p>
     *     Since this class is defined using generics, its wrapper class
     *     with specific type must be consistent with {@link ValueType}.
     * </p>
     *
     * @return valueType the ValueType bound to this class
     */
    @Nullable
    public ValueType getValueType() {
        ValueType valueType = null;
        if (mWriter != null) {
            valueType = mWriter.getValueType();
        } else {
            Log.w(TAG, "getValueType: Calling sequence failure");
        }
        return valueType;
    }

    public interface SinetStreamWriterListener<T> {
        /**
         * Called when availability status has changed.
         *
         * @param isReady true if "connected" to the Broker, false otherwise
         */
        void onWriterStatusChanged(boolean isReady);

        /**
         * Called when {@link #publish(Object, Object) publish(T, userData)}
         * has completed successfully.
         *
         * @param message Original message for publish, not {@code null}.
         * @param userData User specified opaque object, passed by {@code publish()}.
         *
         * @apiNote This method is just a placeholder. Implement only if you need it.
         */
        default void onPublished(@NonNull T message, @Nullable Object userData) {

        }

        /**
         * Called when any error condition has met. The error might be detected
         * either at the sinetstream-android level, or at underneath library level.
         *
         * @param description brief description of the error.
         */
        void onError(@NonNull String description);
    }
}
