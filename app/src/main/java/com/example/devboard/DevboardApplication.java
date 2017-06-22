package com.example.devboard;

import android.app.Application;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Map;

/**
 * Created by yoo2001818 on 17. 6. 22.
 */

public class DevboardApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Load default configuration
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
    }
}
