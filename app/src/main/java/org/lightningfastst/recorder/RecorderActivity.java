/*
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2017-2020 The LightningFastRom
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
package org.lightningfastst.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.lightningfastst.recorder.sounds.RecorderBinder;
import org.lightningfastst.recorder.sounds.SoundRecorderService;
import org.lightningfastst.recorder.ui.SoundVisualizer;
import org.lightningfastst.recorder.utils.LastRecordHelper;
import org.lightningfastst.recorder.utils.Utils;
import org.lightningfastst.recorder.Recordings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class RecorderActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "RecorderActivity";

    private static final int REQUEST_SOUND_REC_PERMS = 440;
    private static final int REQUEST_DIALOG_ACTIVITY = 441;
    private static final int REQUEST_AUDIO_VIDEO = 442;

    private static final int[] PERMISSION_ERROR_MESSAGE_RES_IDS = {
            0,
            R.string.dialog_permissions_mic,
            R.string.dialog_permissions_phone,
            R.string.dialog_permissions_mic_phone,
    };

    private ServiceConnection mConnection;
    private SoundRecorderService mSoundService;
    private SharedPreferences mPrefs;

    private ConstraintLayout mConstraintRoot;

    private FloatingActionButton mSoundFab;
    private ImageView mSoundLast;
    private ListView mRecordings;

    private RelativeLayout mRecordingLayout;
    private TextView mRecordingText;
    private SoundVisualizer mRecordingVisualizer;

    private File mFile;
    private File mList[];


    private final BroadcastReceiver mTelephonyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(TelephonyManager.EXTRA_STATE, -1);
                if (state == TelephonyManager.CALL_STATE_OFFHOOK &&
                        Utils.isSoundRecording(context)) {
                    toggleSoundRecorder();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activty_constraint);

        mConstraintRoot = findViewById(R.id.main_root);

        mSoundFab = findViewById(R.id.sound_fab);
        mRecordings = findViewById(R.id.recordings);

        mRecordingLayout = findViewById(R.id.main_recording);
        mRecordingText = findViewById(R.id.main_recording_text);
        mRecordingVisualizer = findViewById(R.id.main_recording_visualizer);

        mSoundFab.setOnClickListener(v -> toggleSoundRecorder());

        bindSoundRecService();
        init_Recorging_list();
    }

    private void init_Recorging_list() {

        mFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "SoundRecords");
        mList = mFile.listFiles();
        ArrayList<String> recordings = new ArrayList<String>();
        //Log.d(TAG, "init_Recorging_list: "+mList);


        if (mFile.isDirectory()) {
            if(mFile.list().length == 0) {
            } else {
                for (File recording : mList) {
                    recordings.add(recording.getName().toString());
                }
            }
        }

//        Log.d(TAG, "init_Recorging_list: "+recordings.toString());
        Recordings recordingsList = new
                Recordings(RecorderActivity.this, recordings);
        mRecordings.setAdapter(recordingsList);
        mRecordings.setOnItemClickListener(
        new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                openLastSound(recordings.get(position).toString());
            }
        });
        updateLastItemStatus();
    }

    @Override
    public void onDestroy() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mTelephonyReceiver,
                new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mTelephonyReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] results) {
        if (requestCode == REQUEST_SOUND_REC_PERMS && hasAllAudioRecorderPermissions()) {
            toggleAfterPermissionRequest(requestCode);
            return;
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ||
                shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
            // Explain the user why the denied permission is needed
            int error = 0;

            if (!hasAudioPermission()) {
                error |= 1;
            }
            if (!hasPhoneReaderPermission()) {
                error |= 1 << 1;
            }

            String message = getString(PERMISSION_ERROR_MESSAGE_RES_IDS[error]);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_permissions_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.dialog_permissions_ask,
                            (dialog, position) -> {
                                dialog.dismiss();
                                askPermissionsAgain(requestCode);
                            })
                    .setNegativeButton(R.string.dialog_permissions_dismiss, null)
                    .show();
        } else {
            // User has denied all the required permissions "forever"
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_permissions_title)
                    .setMessage(R.string.snack_permissions_no_permission)
                    .setPositiveButton(R.string.dialog_permissions_dismiss, null)
                    .show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (Utils.KEY_RECORDING.equals(key)) {
            refresh();
        }
    }


    private void toggleAfterPermissionRequest(int requestCode) {
        switch (requestCode) {
            case REQUEST_SOUND_REC_PERMS:
                bindSoundRecService();
                new Handler().postDelayed(this::toggleSoundRecorder, 500);
                break;
        }
    }

    private void askPermissionsAgain(int requestCode) {
        switch (requestCode) {
            case REQUEST_SOUND_REC_PERMS:
                checkSoundRecPermissions();
                break;
        }
    }

    private void toggleSoundRecorder() {
        if (checkSoundRecPermissions()) {
            return;
        }

        if (mSoundService == null) {
            bindSoundRecService();
            return;
        }

        if (mSoundService.isRecording()) {
            // Stop
            mSoundService.stopRecording();
            stopService(new Intent(this, SoundRecorderService.class));
            Utils.setStatus(this, Utils.UiStatus.NOTHING);
        } else {
            // Start
            startService(new Intent(this, SoundRecorderService.class));
            mSoundService.startRecording();
            Utils.setStatus(this, Utils.UiStatus.SOUND);
        }
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            finish();
        }
    }

    private void refresh() {
        ConstraintSet set = new ConstraintSet();
        if (Utils.isRecording(this)) {

            mRecordingText.setVisibility(View.VISIBLE);
            mRecordingLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.sound));
            mRecordingVisualizer.setVisibility(View.VISIBLE);
            mRecordings.setVisibility(View.INVISIBLE);
            mSoundFab.setImageResource(R.drawable.ic_stop_sound);
            mRecordingVisualizer.onAudioLevelUpdated(0);
            if (mSoundService != null) {
                mSoundService.setAudioListener(mRecordingVisualizer);
            }
            set.clone(this, R.layout.constraint_sound);
        } else {
            mRecordingText.setVisibility(View.INVISIBLE);
            mSoundFab.setImageResource(R.drawable.ic_action_sound_record);
            mSoundFab.setSelected(false);
            mRecordingVisualizer.setVisibility(View.INVISIBLE);
            mRecordings.setVisibility(View.VISIBLE);
            set.clone(this, R.layout.constraint_default);
        }

        updateLastItemStatus();
        updateSystemUIColors();
        init_Recorging_list();
        TransitionManager.beginDelayedTransition(mConstraintRoot);
        set.applyTo(mConstraintRoot);
    }

    private boolean hasAudioPermission() {
        int result = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasPhoneReaderPermission() {
        int result = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasDrawOverOtherAppsPermission() {
        return Settings.canDrawOverlays(this);
    }

    private boolean hasAllAudioRecorderPermissions() {
        return hasAudioPermission() && hasPhoneReaderPermission();
    }

    private boolean checkSoundRecPermissions() {
        ArrayList<String> permissions = new ArrayList<>();

        if (!hasAudioPermission()) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!hasPhoneReaderPermission()) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (permissions.isEmpty()) {
            return false;
        }

        String[] permissionArray = permissions.toArray(new String[0]);
        requestPermissions(permissionArray, REQUEST_SOUND_REC_PERMS);
        return true;
    }

    private void setupConnection() {
        checkSoundRecPermissions();
        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                mSoundService = ((RecorderBinder) binder).getService();
                mSoundService.setAudioListener(mRecordingVisualizer);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mSoundService = null;
            }
        };
    }

    private void bindSoundRecService() {
        if (mSoundService == null && hasAllAudioRecorderPermissions()) {
            setupConnection();
            bindService(new Intent(this, SoundRecorderService.class),
                    mConnection, BIND_AUTO_CREATE);
        }
    }

    private void updateLastItemStatus() {
        if (mFile.isDirectory()) {
            if (mFile.list().length == 0) {
                mRecordings.setVisibility(View.VISIBLE);
            } else {
                mRecordings.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updateSystemUIColors() {
        int navigationBarColor;

        if (Utils.isRecording(this)) {
            navigationBarColor = ContextCompat.getColor(this, R.color.screen);
            ;
        } else {
            navigationBarColor = ContextCompat.getColor(this, R.color.sound);
        }
        getWindow().setNavigationBarColor(Utils.darkenedColor(navigationBarColor));
    }

    private void showDialog(Intent intent, View view) {
        String transitionName = getString(R.string.transition_dialog_name);
        view.setTransitionName(transitionName);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                view, transitionName);
        ActivityCompat.startActivityForResult(this, intent,
                REQUEST_DIALOG_ACTIVITY, options.toBundle());
    }

    private void openLastSound(String fName) {
        Intent intent = new Intent(this, DialogActivity.class);
        intent.putExtra(DialogActivity.EXTRA_TITLE, fName);
        intent.putExtra(DialogActivity.EXTRA_LAST_SOUND, true);
        showDialog(intent, mSoundLast);
    }
}
