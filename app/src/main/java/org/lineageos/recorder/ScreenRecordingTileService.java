// Copyright 2016 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.lineageos.recorder;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.widget.Toast;

import java.util.Locale;

import org.lineageos.recorder.RecorderActivity;

import org.lineageos.recorder.screen.OverlayService;
import org.lineageos.recorder.screen.ScreencastService;
import org.lineageos.recorder.sounds.RecorderBinder;
import org.lineageos.recorder.sounds.SoundRecorderService;
import org.lineageos.recorder.ui.SoundVisualizer;
import org.lineageos.recorder.utils.LastRecordHelper;
import org.lineageos.recorder.utils.OnBoardingHelper;
import org.lineageos.recorder.utils.Utils;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Override")
@TargetApi(Build.VERSION_CODES.N)
public class ScreenRecordingTileService
   extends TileService{
	private static final String SERVICE_STATUS_FLAG = "serviceStatus";
    private static final String PREFERENCES_KEY = "org.lineageos.recorder";
	private SharedPreferences mPrefs;
	
    /**
     * Called when the tile is added to the Quick Settings.
     * @return TileService constant indicating tile state
     */
    @Override
    public void onTileAdded() {
		mPrefs = getSharedPreferences(Utils.PREFS, 0);
        Log.d("QS", "Tile added");
    }

    /**
     * Called when this tile begins listening for events.
     */
    @Override
    public void onStartListening() {
		mPrefs = getSharedPreferences(Utils.PREFS, 0);
        Log.d("QS", "Start listening");
    }

    /**
     * Called when the user taps the tile.
     */

    @Override
    public void onClick() {
    	// Called when the user click the tile
		//final RecorderActivity mRecorderActivity = new RecorderActivity();
		//if (mRecorderActivity.checkScreenRecPermissions()) {
			
        //}
		
		Tile tile = getQsTile();        
        
		Icon tileIcon;
        String tileLabel;
        boolean isActive = getServiceStatus();
		mPrefs = getSharedPreferences(Utils.PREFS, 0);
		
        if (Utils.isScreenRecording(this)) {
			tileIcon = Icon.createWithResource(this, R.drawable.ic_action_screen_record);
            tileLabel = getString(R.string.main_screen_action);
			
			// Stop Screen Recording
			Utils.setStatus(this, Utils.UiStatus.NOTHING);
			startService(new Intent(ScreencastService.ACTION_STOP_SCREENCAST)
					.setClass(this, ScreencastService.class));
			
			// Stop Screen Recording Toast Message
			Toast.makeText(getApplicationContext(), getString(R.string.toast_stop_screen_recording_message), Toast.LENGTH_SHORT).show();  
			
			// Stop Screen Recording Logcat Message
			Log.d("QS", "Stopping Screen Recording Service");
		} else {
			tileIcon = Icon.createWithResource(this, R.drawable.ic_stop_screen);
            tileLabel = getString(R.string.stop);
			
			// Start Screen Recording
			boolean hasAudio = mPrefs.getBoolean(Utils.PREF_SCREEN_WITH_AUDIO, false);
			Intent fabIntent = new Intent(ScreencastService.ACTION_START_SCREENCAST);
            fabIntent.putExtra(ScreencastService.EXTRA_WITHAUDIO, hasAudio);
            startService(fabIntent.setClass(this, ScreencastService.class));
			
			// Stop Screen Recording Toast Message
			Toast.makeText(getApplicationContext(), getString(R.string.toast_start_screen_recording_message), Toast.LENGTH_SHORT).show(); 
			 
			// Stop Screen Recording Logcat Message
			Log.d("QS", "Startting Screen Recording Service");
        }
        tile.setIcon(tileIcon);
        tile.setLabel(tileLabel);
        tile.updateTile();
    }

    /**
     * Called when this tile moves out of the listening state.
     */
    @Override
    public void onStopListening() {
        Log.d("QS", "Stop Listening");
    }

    /**
     * Called when the user removes this tile from Quick Settings.
     */
    @Override
    public void onTileRemoved() {
        Log.d("QS", "Tile removed");
    }
	
	private boolean getServiceStatus() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
        boolean isActive = prefs.getBoolean(SERVICE_STATUS_FLAG, false);
        isActive = !isActive;

        prefs.edit().putBoolean(SERVICE_STATUS_FLAG, isActive).apply();

        return isActive;
    }
}
