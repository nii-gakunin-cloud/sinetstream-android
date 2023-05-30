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

package jp.ad.sinet.stream.android.config.remote.configclient.net;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.OperationCanceledException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class HttpsGetTask extends AlternateAsyncTask<Integer> {
    private final String TAG = HttpsGetTask.class.getSimpleName();

    private final WeakReference<Context> mWeakReference;
    private final String mUrlString;
    private final HttpsGetTaskListener mListener;
    private final String HTTP_METHOD_GET = "GET";

    private String mErrMsg = null;
    private JSONObject mResponseData = null;
    private JSONArray mResponseArray = null;

    private HashMap<String, String> mExtraHeaders = null;
    private SSLContext mSslContext = null;

    public HttpsGetTask(@NonNull Context context,
                        @NonNull String urlString,
                        @NonNull HttpsGetTaskListener listener) {
        this.mWeakReference = new WeakReference<>(context);
        this.mUrlString = urlString;
        this.mListener = listener;
    }

    public void setExtraHeaders(@NonNull HashMap<String,String> headers) {
        /*
         * Optional settings:
         * User may want to set some extra HTTP headers.
         */
        mExtraHeaders = headers;
    }

    public void setSslContext(@NonNull SSLContext sslContext) {
        /*
         * Optional settings:
         * Set if client-certificate or self-signed server certificate is used.
         */
        mSslContext = sslContext;
    }

    @Override
    protected Integer doInBackground() {
        int httpResponseCode = -1;
        URL url;

        try {
            url = new URL(mUrlString);
        } catch (MalformedURLException e) {
            mErrMsg = "URL[" + mUrlString + "]: " + e.getMessage();
            Log.w(TAG, mErrMsg);
            return httpResponseCode;
        }

        try {
            httpResponseCode = downloadJsonData(url);
        } catch (OperationCanceledException e) {
            mErrMsg = "URL[" + mUrlString + "]: " + e.getMessage();
            Log.w(TAG, mErrMsg);
        }
        return httpResponseCode;
    }

    @Override
    protected void onPostExecute(@NonNull Integer result) {
        if (mDebugEnabled) {
            Log.d(TAG, "onPostExecute: httpResponseCode=" + result);
        }

        Context context = mWeakReference.get();
        if (context != null) {
            if (mErrMsg != null) {
                mListener.onError(mErrMsg);
            } else {
                switch (result) {
                    case HttpURLConnection.HTTP_OK:
                        if (mResponseData != null) {
                            mListener.onDownloadFinished(mResponseData);
                        } else if (mResponseArray != null) {
                            mListener.onDownloadFinished(mResponseArray);
                        }
                        break;
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                    case HttpURLConnection.HTTP_NOT_FOUND:
                    default:
                        /* On error cases in the background, "mErrMsg" must have set */
                        Log.w(TAG, "XXX: Unexpected result: " + result);
                        break;
                }
            }
        } else {
            Log.w(TAG, "Context has gone");
        }
    }

    private void setConnectionParameters(
            @NonNull HttpsURLConnection httpsUrlConnection) {

        int TIMEOUT_CONNECT = 30000;
        httpsUrlConnection.setConnectTimeout(TIMEOUT_CONNECT);
        int TIMEOUT_READ = 5000;
        httpsUrlConnection.setReadTimeout(TIMEOUT_READ);
        httpsUrlConnection.setInstanceFollowRedirects(false);

        try {
            httpsUrlConnection.setRequestMethod(HTTP_METHOD_GET);
        } catch (ProtocolException e) {
            mErrMsg = "HttpsUrlConnection.setRequestMethod(" +
                    HTTP_METHOD_GET + "): " + e.getMessage();
            Log.w(TAG, mErrMsg);
            return;
        }

        httpsUrlConnection.setDoInput(true); /* Read payload */
        //httpsUrlConnection.setDoOutput(true); /* Write payload */
        httpsUrlConnection.setUseCaches(false);

        httpsUrlConnection.setRequestProperty(
                "Connection", "Keep-Alive");
        httpsUrlConnection.setRequestProperty(
                "Content-Type", "application/json");
        httpsUrlConnection.setRequestProperty(
                "Accept", "application/json");

        if (mExtraHeaders != null) {
            for (Map.Entry<String, String> data : mExtraHeaders.entrySet()) {
                String key = data.getKey();
                String val = data.getValue();
                String probe = httpsUrlConnection.getRequestProperty(key);
                if (probe != null) {
                    /* Overwrite */
                    if (mDebugEnabled) {
                        Log.d(TAG, "key(" + key + "): " + probe + " -> " + val);
                    }
                    httpsUrlConnection.setRequestProperty(key, val);
                } else {
                    httpsUrlConnection.addRequestProperty(key, val);
                }
            }
        }

        if (mSslContext != null) {
            SSLSocketFactory sslSocketFactory;
            try {
                sslSocketFactory = mSslContext.getSocketFactory();
            } catch (IllegalStateException e) {
                mErrMsg = "SSLContext.getSocketFactory(): " + e.getMessage();
                Log.w(TAG, mErrMsg);
                return;
            }

            try {
                httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);
            } catch (IllegalArgumentException | SecurityException e) {
                mErrMsg = "HttpsUrlConnection.setSSLSocketFactory(): " + e.getMessage();
                Log.w(TAG, mErrMsg);
            }
        } else {
            if (mDebugEnabled) {
                Log.d(TAG, "SSLContext is NOT set");
            }
        }
    }

    private int downloadJsonData(@NonNull URL url) {
        int httpResponseCode = -1;

        HttpsURLConnection httpsUrlConnection;
        String responseJsonString = null;

        // We need to define some parameters to establish the SSL connection with server.
        try {
            httpsUrlConnection = (HttpsURLConnection)url.openConnection();
        } catch (IOException e) {
            mErrMsg = "URL(" + mUrlString + "): openConnection(): " + e.getMessage();
            Log.w(TAG, mErrMsg);
            return httpResponseCode;
        }

        setConnectionParameters(httpsUrlConnection);
        if (mErrMsg != null) {
            return httpResponseCode;
        }

        try {
            httpsUrlConnection.connect();

            /*
             * [NB]
             * By default, URLConnection.getContentLength() returns -1.
             *
             * URLConnection.getContentLength() returns the number of bytes
             * transmitted and cannot be used to predict how many bytes can be
             * read from URLConnection.getInputStream() for compressed streams.
             * Instead, read that stream until it is exhausted, i.e. when
             * InputStream#read returns -1.
             *
             * https://developer.android.com/reference/java/net/HttpURLConnection#performance
             */
            int responseCode = httpsUrlConnection.getResponseCode();
            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    responseJsonString =
                            getContent(url, httpsUrlConnection, true);
                    if (responseJsonString != null && responseJsonString.isEmpty()) {
                        mErrMsg = HTTP_METHOD_GET + " " + mUrlString + ":\n";
                        mErrMsg += responseCode + " " + httpsUrlConnection.getResponseMessage() + "\n";
                        mErrMsg += "=> Empty response data?";
                        Log.w(TAG, mErrMsg);
                        break;
                    }
                    break;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                case HttpURLConnection.HTTP_NOT_FOUND:
                default:
                    mErrMsg = HTTP_METHOD_GET + " " + mUrlString + ":\n";
                    mErrMsg += responseCode + " " + httpsUrlConnection.getResponseMessage();

                    /* On HTTP error cases, additional info may exist */
                    String errorJsonString =
                            getContent(url, httpsUrlConnection, false);
                    if (errorJsonString != null && !errorJsonString.isEmpty()) {
                        mErrMsg += "\n==\n";
                        mErrMsg += errorJsonString;
                    }

                    Log.w(TAG, mErrMsg);
                    break;
            }
            httpResponseCode = responseCode;
        } catch (IOException e) {
            mErrMsg = "I/O error: " + mUrlString + ": " + e.getMessage();
            Log.w(TAG, mErrMsg);
        } finally {
            httpsUrlConnection.disconnect();
        }

        if (mErrMsg == null && responseJsonString != null) {
            if (responseJsonString.startsWith("[")) {
                try {
                    mResponseArray = new JSONArray(responseJsonString);
                } catch (JSONException e) {
                    mErrMsg = "Invalid JSONArray: " + e.getMessage();
                    httpResponseCode = -1;
                }
            } else {
                try {
                    mResponseData = new JSONObject(responseJsonString);
                } catch (JSONException e) {
                    mErrMsg = "Invalid JSONObject: " + e.getMessage();
                    httpResponseCode = -1;
                }
            }
        }
        return httpResponseCode;
    }

    @Nullable
    private String getContent(
            @NonNull URL url,
            @NonNull HttpsURLConnection httpsUrlConnection,
            boolean isResponseBody) {
        String contentString = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(
                    new InputStreamReader(
                            (isResponseBody ?
                                    httpsUrlConnection.getInputStream():
                                    httpsUrlConnection.getErrorStream()),
                            StandardCharsets.UTF_8));

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line.trim());
            }
            contentString = stringBuilder.toString();
        } catch (IOException e) {
            mErrMsg = "I/O error: " + url + ": " + e.getMessage();
            Log.w(TAG, mErrMsg);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.w(TAG, "BufferedReader.close: " + e.getMessage());
                    /* Don't care for this case */
                }
            }
        }
        return contentString;
    }

    private boolean mDebugEnabled = false;
    public void enableDebug(boolean enabled) {
        mDebugEnabled = enabled;
    }

    public interface HttpsGetTaskListener {
        void onDownloadFinished(@NonNull JSONObject responseData);
        void onDownloadFinished(@NonNull JSONArray responseData);

        void onError(@NonNull String description);
    }
}
