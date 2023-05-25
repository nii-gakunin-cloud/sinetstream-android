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

import jp.ad.sinet.stream.android.AndroidMessageReaderFactory;
import jp.ad.sinet.stream.android.api.low.AsyncMessageReader;
import jp.ad.sinet.stream.android.api.low.ReaderMessageCallback;
import jp.ad.sinet.stream.android.config.parser.YamlTags;

/**
 * Provides a set of API functions to be a Reader (= subscriber)
 * in the SINETStream system.
 * <p>
 *     Due to the nature of messaging system, all methods listed below
 *     should be handled as asynchronous requests.
 *     <ul>
 *         <li>initialize + setup</li>
 *         <li>terminate</li>
 *     </ul>
 * </p>
 * <p>
 *     User of this class must implement the {@link SinetStreamReaderListener}
 *     in the calling {@link Activity}, so that the result of an asynchronous
 *     request or any error condition can be notified.
 * </p>
 */
public abstract class SinetStreamReader<T> {
    private final String TAG = SinetStreamReader.class.getSimpleName();

    private final Context mContext;
    private final SinetStreamReaderListener<T> mListener;
    private AsyncMessageReader<T> mReader = null;
    private boolean mServerReady = false;
    private boolean mIsSubscribed = false;

    private String mServerUrl = null;
    private String mAccount = null;
    private String mSecretKey = null;
    private boolean mUseRemoteConfig = false;

    private String mPredefinedDataStream = null;
    private String mPredefinedServiceName = null;

    /**
     * Constructs a SinetStreamReader instance.
     *
     * @param context the Application context which implements
     *                {@link SinetStreamReaderListener},
     *                usually it is the calling {@link Activity} itself.
     *
     * @throws RuntimeException if given context does not implement
     *                          the required listener.
     */
    public SinetStreamReader(@NonNull Context context) {
        if (context instanceof SinetStreamReaderListener) {
            mContext = context;

            /* Set annotation to suppress "unchecked cast" warning */
            //noinspection unchecked
            mListener = (SinetStreamReaderListener<T>) context;
        } else {
            throw new RuntimeException(context +
                    " must implement SinetStreamReaderListener");
        }
    }

    /**
     * Provide connection parameters to load configuration from remote server.
     * <p>
     *     User can choose the configuration source; either download from the
     *     designated remote configuration server, or read the predefined
     *     local file.
     * </p>
     * <p>
     *     To download from the remote server, user MUST call this method
     *     before {@code initialize()}.
     * </p>
     *
     * @param serverUrl The URL to access the remote configuration server.
     * @param account The user account to login the server.
     * @param secretKey The secret key for authentication.
     */
    public void setRemoteConfig(
            @NonNull String serverUrl,
            @NonNull String account,
            @NonNull String secretKey) {
        this.mServerUrl = serverUrl;
        this.mAccount = account;
        this.mSecretKey = secretKey;
        this.mUseRemoteConfig = true;
    }

    /**
     * Sets optional parameters to be used in the REST-API requests.
     *
     * @param dataStream The name of a set of SINETStream configuration
     *                   which has defined on the configuration server.
     * @param serviceName A dataStream consists from a single service
     *                    or multiple services. This parameter specifies
     *                    which service to take in the dataStream.
     */
    public void setPredefinedParameters(
            @Nullable String dataStream,
            @Nullable String serviceName) {
        mPredefinedDataStream = dataStream;
        mPredefinedServiceName = serviceName;
    }

    /**
     * Allocates a message Reader instance.
     * <p>
     *     This method internally allocates a message reader instance
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
     * @param alias the alias for a private key and certificate pair to be
     *              used for the transport layer security.
     */
    public void initialize(
            @NonNull String serviceName, @Nullable String alias) {
        if (mReader != null) {
            if (mDebugEnabled) {
                Log.d(TAG, "Initialize: Getting back from background");
            }

            /* Prompt user to call setup() next */
            mListener.onReaderConfigLoaded();
        } else {
            /* Get a new Reader instance. */
            AndroidMessageReaderFactory.Builder<T> builder =
                    new AndroidMessageReaderFactory.Builder<>();
            builder.setContext(mContext); // Mandatory
            builder.setService(serviceName); // Mandatory
            if (alias != null) {
                builder.addParameter(
                        YamlTags.KEY_EXTRA_ALIAS, alias); // Optional
            }

            try {
                AndroidMessageReaderFactory<T> amrf = builder.build();
                if (mUseRemoteConfig) {
                    amrf.setRemoteConfig(mServerUrl, mAccount, mSecretKey);
                    amrf.setPredefinedParameters(
                            mPredefinedDataStream, mPredefinedServiceName);
                }
                amrf.setDebugEnabled(mDebugEnabled);
                amrf.loadConfig(
                        new AndroidMessageReaderFactory.ReaderConfigLoaderListener() {
                            @Override
                            public void onReaderConfigLoaded() {
                                try {
                                    mReader = amrf.getAsyncReader();
                                    mListener.onReaderConfigLoaded();
                                } catch (InvalidConfigurationException |
                                        UnsupportedServiceException e) {
                                    mListener.onError(e.toString());
                                }
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                mListener.onError(description);
                            }
                        });
            } catch (NoConfigException |
                    InvalidConfigurationException |
                    NoServiceException |
                    UnsupportedServiceException e) {
                mListener.onError(e.toString());
            }
        }
    }

    /**
     * Check if initialization has successfully completed.
     *
     * @return true if a message Reader has allocated, false otherwise
     */
    public boolean isInitializationSuccess() {
        return (mReader != null);
    }

    /**
     * Sets up a connection to the Broker via underlying messaging system.
     * <p>
     *     Actually, this is the latter half of initialization process.<br>
     *     In this method, internal callback handlers
     *     {@link ReaderMessageCallback}
     *     will be associated with the
     *     {@link SinetStreamReader.SinetStreamReaderListener}
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
        if (mReader != null && !mServerReady) {
            /* Set collection of callback functions for this Reader instance. */
            setCallback();

            /*
             * Issue an asynchronous connect request to the Broker.
             * It's result will be notified by one of callback functions.
             */
            mReader.connect();
        } else {
            /* Show log message and do nothing here. */
            if (mDebugEnabled) {
                Log.d(TAG, "Setup: already done");
            }
        }
    }

    /**
     * Sets callback handlers for internal state management and user interaction.
     */
    private void setCallback() {
        if (mReader != null) {
            mReader.setCallback(new ReaderMessageCallback<>() {
                @Override
                public void onConnectionEstablished(boolean reconnect) {
                    // Successfully connected to the Broker
                    if (mDebugEnabled) {
                        Log.d(TAG, "onConnectionEstablished: reconnect(" + reconnect + ")");
                    }

                    /* Try auto-subscribe for convenience. */
                    mServerReady = true;
                    mReader.subscribe();
                }

                @Override
                public void onConnectionClosed(@Nullable String reason) {
                    // Connection closed
                    if (mDebugEnabled) {
                        Log.d(TAG, "onConnectionClosed: reason(" +
                                (reason != null ? reason : "Normal closure") + ")");
                    }
                    mServerReady = false;
                    if (reason != null) {
                        mListener.onError(reason);
                    } else {
                        mListener.onReaderStatusChanged(false);
                    }
                }

                @Override
                public void onReconnectInProgress() {
                    if (mDebugEnabled) {
                        Log.d(TAG, "onReconnectInProgress");
                    }
                    mServerReady = false;
                    mListener.onReaderReconnectInProgress();
                }

                @Override
                public void onSubscribed() {
                    // Now, we are ready to receive messages from Broker.
                    if (mDebugEnabled) {
                        Log.d(TAG, "onSubscribed");
                    }
                    mIsSubscribed = true;
                    mListener.onReaderStatusChanged(true);
                }

                @Override
                public void onUnsubscribed() {
                    // We won't receive messages anymore.
                    if (mDebugEnabled) {
                        Log.d(TAG, "onUnsubscribed");
                    }
                    mIsSubscribed = false;
                    mListener.onReaderStatusChanged(false);

                    if (mReader != null) {
                        //mReader.close();
                        mReader.disconnect();
                    }
                }

                @Override
                public void onMessageReceived(@NonNull Message<T> message) {
                    if (mDebugEnabled) {
                        Log.d(TAG, "onMessageReceived");
                    }
                    Long unixTime = message.getTimestamp();

                    mListener.onMessageReceived(
                            message.getTopic(),
                            (unixTime != null) ? unixTime : 0L,
                            message.getPayload());
                }

                @Override
                public void onError(@NonNull String description, Throwable exception) {
                    String errmsg = description;
                    if (exception != null) {
                        String stack = Arrays.toString(exception.getStackTrace());
                        Log.e(TAG, "onError(exception): \n" +
                                stack.replaceAll(", ", "\n "));
                        errmsg += ": " + exception;
                    }
                    mListener.onError(errmsg);
                }
            });
        }
    }

    /**
     * Disconnects from the Broker and cleans up allocated resources.
     */
    public void terminate() {
        if (mReader != null) {
            if (mIsSubscribed) {
                mReader.unsubscribe();
            } else {
                //mReader.close();
                mReader.disconnect();
            }
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
        if (mReader != null) {
            valueType = mReader.getValueType();
        } else {
            Log.w(TAG, "getValueType: Calling sequence failure");
        }
        return valueType;
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface SinetStreamReaderListener<T> {
        /**
         * Called when initialization process, including configuration loading,
         * has finished.
         * Now user can call "SinetStreamReader<T>.setup()" next.
         */
        void onReaderConfigLoaded();

        /**
         * Called when availability status has changed.
         *
         * @param isReady true if "connected and subscribed" to the Broker, false otherwise
         */
        void onReaderStatusChanged(boolean isReady);

        /**
         * Called when the broker connection has lost and auto-reconnect
         * procedure is in progress.
         */
        void onReaderReconnectInProgress();

        /**
         * Called when a message has received on any subscribed topic.
         *
         * @param topic a topic where received message came from
         * @param timestamp message publish date and time, measured in UnixTime format.
         * @param data received message contents
         */
        void onMessageReceived(@NonNull String topic,
                               long timestamp,
                               @NonNull T data);

        /**
         * Called when any error condition has met. The error might be detected
         * either at this sinetstream-android library, or at somewhere in the
         * lower level libraries.
         *
         * @param description brief description of the error.
         */
        void onError(@NonNull String description);
    }
}
