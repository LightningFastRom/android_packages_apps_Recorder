/*
 * Copyright (C) 2017-2021 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.recorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.lineageos.recorder.utils.LastRecordHelper;
import org.lineageos.recorder.utils.Utils;

public class DialogActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "dialogTitle";
    public static final String EXTRA_DELETE_LAST_RECORDING = "deleteLastItem";

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setFinishOnTouchOutside(true);

        Intent intent = getIntent();
        boolean deleteLastRecording = intent.getBooleanExtra(EXTRA_DELETE_LAST_RECORDING, false);

        if (deleteLastRecording) {
            deleteLastItem();
            return;
        }

        int dialogTitle = intent.getIntExtra(EXTRA_TITLE, 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(null)
                .setOnDismissListener(dialogInterface -> finish())
                .create();

        if (dialogTitle != 0) {
            dialog.setTitle(dialogTitle);
        }
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void deleteLastItem() {
        Uri uri = LastRecordHelper.getLastItemUri(this);
        AlertDialog dialog = LastRecordHelper.deleteFile(this, uri);
        dialog.setOnDismissListener(d -> finish());
        dialog.show();
    }
}
