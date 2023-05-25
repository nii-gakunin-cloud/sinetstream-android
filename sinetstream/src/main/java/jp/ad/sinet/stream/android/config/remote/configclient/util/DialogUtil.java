/*
 * Copyright (c) 2022 National Institute of Informatics
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

package jp.ad.sinet.stream.android.config.remote.configclient.util;

import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class DialogUtil {
    private final Context mContext;

    public DialogUtil(@NonNull Context context) {
        mContext = context;
    }

    public void showModalDialog(
            @NonNull String title,
            @NonNull String message,
            @Nullable DialogInterface.OnClickListener onClickListener) {
        /* Use simple AlertDialog.Builder rather than MaterialAlertDialogBuilder. */
        AlertDialog.Builder dialogBuilder =
                new AlertDialog.Builder(mContext);

        dialogBuilder.setTitle(title);
        dialogBuilder.setIcon(android.R.drawable.ic_dialog_info);
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton(android.R.string.ok, onClickListener);

        /* Don't let dismiss by clicking outside of the dialog window */
        dialogBuilder.setCancelable(false);

        dialogBuilder.create();
        dialogBuilder.show();
    }

    public void showSingleChoiceDialog(
            @NonNull String title,
            @NonNull String[] items,
            int checkedItem,
            @Nullable DialogInterface.OnClickListener onItemClickListener,
            @Nullable DialogInterface.OnClickListener onButtonClickListener) {
        /* Use simple AlertDialog.Builder rather than MaterialAlertDialogBuilder. */
        AlertDialog.Builder dialogBuilder =
                new AlertDialog.Builder(mContext);

        dialogBuilder.setTitle(title);
        /*
         * [NB]
         * SetMessage and SetSingleChoiceItems seems to be exclusive use;
         * if we set message here, item list will not be shown.
         *
        dialogBuilder.setMessage(message);
         */
        dialogBuilder.setSingleChoiceItems(items, checkedItem, onItemClickListener);
        dialogBuilder.setPositiveButton(android.R.string.ok, onButtonClickListener);

        /* Don't let dismiss by clicking outside of the dialog window */
        dialogBuilder.setCancelable(false);

        dialogBuilder.create();
        dialogBuilder.show();
    }
}
