package com.example.devboard;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by yoo2001818 on 17. 6. 22.
 */

public class ThemeLoader {
    public static int getTheme(Context c) {
        String themeName = PreferenceManager.getDefaultSharedPreferences(c)
                .getString(DevBoardIME.THEME, "default");
        int desiredTheme = R.style.AppTheme;
        // Separate internal R id and serialized id because R's ID is not portable.
        switch (themeName) {
            case "default":
                desiredTheme = R.style.AppTheme;
                break;
            case "white":
                desiredTheme = R.style.WhiteTheme;
                break;
            case "blue":
                desiredTheme = R.style.BlueTheme;
                break;
        }
        return desiredTheme;
    }
}
