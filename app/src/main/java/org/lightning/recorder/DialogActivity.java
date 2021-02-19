/*
 * Copyright (C) 2020-2021 The LineageOS Project
 * Copyright (C) 2020-2021 The lightning Project
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
package org.lightning.recorder;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Switch;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.lightning.recorder.utils.LastRecordHelper;
import org.lightning.recorder.utils.Utils;

public class DialogActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "dialogTitle";
    public static final String EXTRA_SETTINGS_SCREEN = "settingsScreen";
    public static final String EXTRA_DELETE_LAST_RECORDING = "deleteLastItem";
    private static final int REQUEST_LOCATION_PERMS = 214;

    private LinearLayout mRootView;
    private FrameLayout mContent;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setFinishOnTouchOutside(true);

        mPrefs = getSharedPreferences(Utils.PREFS, 0);

        Intent intent = getIntent();
        boolean deleteLastRecording = intent.getBooleanExtra(EXTRA_DELETE_LAST_RECORDING, false);

        if (deleteLastRecording) {
            deleteLastItem();
            return;
        }

        setContentView(R.layout.dialog_base);
        mRootView = findViewById(R.id.dialog_root);
        TextView title = findViewById(R.id.dialog_title);
        mContent = findViewById(R.id.dialog_content);

        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        int dialogTitle = intent.getIntExtra(EXTRA_TITLE, 0);
        boolean isSettingsScreen = intent.getBooleanExtra(EXTRA_SETTINGS_SCREEN, false);

        if (dialogTitle != 0) {
            title.setText(dialogTitle);
        }
        animateAppearance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void animateAppearance() {
        mRootView.setAlpha(0f);
        mRootView.animate()
                .alpha(1f)
                .setStartDelay(250)
                .start();
    }

    private void deleteLastItem() {
        Uri uri = LastRecordHelper.getLastItemUri(this);
        AlertDialog dialog = LastRecordHelper.deleteFile(this, uri);
        dialog.setOnDismissListener(d -> finish());
        dialog.show();
    }

    private View createContentView(@LayoutRes int layout) {
        LayoutInflater inflater = getLayoutInflater();
        return inflater.inflate(layout, mContent);
    }

}
