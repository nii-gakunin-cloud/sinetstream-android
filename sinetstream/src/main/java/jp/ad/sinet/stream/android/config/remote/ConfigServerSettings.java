/*
 * Copyright (C) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.android.config.remote;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import jp.ad.sinet.stream.android.config.remote.configclient.serverinfo.AuthJson;
import jp.ad.sinet.stream.android.config.remote.configclient.util.DialogUtil;

public class ConfigServerSettings {
    private final String TAG = ConfigServerSettings.class.getSimpleName();

    private final AppCompatActivity mActivity;
    private final ConfigServerSettingsListener mListener;
    private final ActivityResultLauncher<Intent> mActivityResultLauncher;
    private String mErrorMessage = null;

    public ConfigServerSettings(@NonNull AppCompatActivity activity,
                                @NonNull ConfigServerSettingsListener listener) {
        mActivity = activity;
        mListener = listener;

        /*
         * https://developer.android.com/training/basics/intents/result#register
         */
        mActivityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (mDebugEnabled) {
                            Log.d(TAG, "ActivityResultLauncher.onActivityResult: result=" + result);
                        }
                        processActivityResult(result);
                    }
                }
        );
    }

    public void launchDocumentPicker() {
        if (mActivity.isFinishing() || mActivity.isDestroyed()) {
            if (mDebugEnabled) {
                Log.d(TAG, "Calling Activity is now finishing. Do nothing here");
            }
            return;
        }
        if (mDebugEnabled) {
            Log.d(TAG, "Going to launch DocumentPicker");
        }

        DialogUtil dialogUtil = new DialogUtil(mActivity);
        dialogUtil.showModalDialog(
                "Access Info",
                "Pick \"auth.json\" for the configuration server access.",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /*
                         * https://developer.android.com/training/basics/intents/result#launch
                         */
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setTypeAndNormalize("application/json");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        try {
                            mActivityResultLauncher.launch(intent);
                        } catch (ActivityNotFoundException e) {
                            mListener.onError(TAG + " : DocumentPicker: " + e.getMessage());
                        }
                    }
                });
    }

    public void clearDocumentPicker() {
        if (mDebugEnabled) {
            Log.d(TAG, "Going to unregister ActivityResultLauncher");
        }
        mActivityResultLauncher.unregister();
    }

    private void processActivityResult(@NonNull ActivityResult result) {
        if (mDebugEnabled) {
            Log.d(TAG, "ActivityResult: " + result);
        }

        switch (result.getResultCode()) {
            case RESULT_OK:
                Intent intent = result.getData();
                if (intent != null) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        ContentResolver contentResolver = mActivity.getContentResolver();
                        contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        openDocument(uri);
                    } else {
                        Log.e(TAG, "ActivityResult returns OK, but URI is missing?");
                    }
                } else {
                    Log.e(TAG, "ActivityResult returns OK, but Intent is missing?");
                }
                break;
            case RESULT_CANCELED:
                mListener.onError("DocumentPicker canceled by user");
                break;
            default:
                mListener.onError("ActivityResult: Unknown ResultCode: " +
                        result.getResultCode());
                break;
        }
    }

    private void openDocument(@NonNull Uri uri) {
        if (mDebugEnabled) {
            Log.d(TAG, "Document: " + uri);
        }
        String content = readTextFromUri(uri);
        if (content != null) {
            if (mDebugEnabled) {
                Log.d(TAG, content);
            }
            processFileContent(content);
        } else {
            mListener.onError(mErrorMessage);
        }
    }

    @Nullable
    private String readTextFromUri(@NonNull Uri uri) {
        String content = null;
        InputStream inputStream;
        try {
            inputStream = mActivity.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            mErrorMessage = "uri(" + uri + "): " + e.getMessage();
            Log.e(TAG, mErrorMessage);
            return null;
        }
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream));
            try {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
            } catch (IOException e) {
                mErrorMessage = "BufferedReader.readLine: " + e.getMessage();
                Log.e(TAG, mErrorMessage);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "InputStream.close: " + e.getMessage());
                    /* Ignore this error */
                }
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "BufferedReader.close: " + e.getMessage());
                    /* Ignore this error */
                }
            }
        }
        return content;
    }

    private void processFileContent(@NonNull String content) {
        AuthJson authJson = new AuthJson();
        authJson.parseJsonData(content, new AuthJson.AuthJsonListener() {
            @Override
            public void onParsed(@NonNull String serverUrl,
                                 @NonNull String account,
                                 @NonNull String secretKey,
                                 @NonNull Date expirationDate) {
                mListener.onParsed(serverUrl, account, secretKey, expirationDate);
            }

            @Override
            public void onExpired() {
                mListener.onExpired();
            }

            @Override
            public void onError(@NonNull String description) {
                mListener.onError(description);
            }
        });
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface ConfigServerSettingsListener {
        /**
         * Called when user-specified settings file (auth.json) contains valid
         * parameter values.
         *
         * @param serverUrl The URL of the configuration server.
         * @param account The login account for the configuration server.
         * @param secretKey The API key published by the configuration server.
         * @param expirationDate Expiration date of the secretKey.
         */
        void onParsed(@NonNull String serverUrl,
                      @NonNull String account,
                      @NonNull String secretKey,
                      @NonNull Date expirationDate);

        /**
         * Called when user-specified settings file (auth.json) has expired.
         * User must update the file with the latest one.
         */
        void onExpired();

        /**
         * Called when any error condition has met. The error might be detected
         * either at the sinetstream-android level, or at underneath library level.
         *
         * @param description brief description of the error.
         */
        void onError(@NonNull String description);
    }
}
