package org.mems;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by shuizhu on 2014/12/14.
 */
public class SharedPrefManager {

    private static final String PREFERENCE_FILE_COLOR = "color_file";
    private static final String PREFERENCE_FILE_COLOR_KEY = "color_key_";
    public static final String PRE_FILE_TIME = "timer";

    public static int getColor(Context context) {
        SharedPreferences sp= context.getSharedPreferences(PREFERENCE_FILE_COLOR, Context.MODE_PRIVATE);
        return sp.getInt(PREFERENCE_FILE_COLOR_KEY, 0);
    }
    public static void saveColor(Context context, int color) {
        SharedPreferences sp = context.getSharedPreferences(PREFERENCE_FILE_COLOR, Context.MODE_PRIVATE);
        sp.edit().putInt(PREFERENCE_FILE_COLOR_KEY, color).apply();
    }

    //
    public static final String PRE_KEY_timeOnStatus = "on_status";
    public static final String PRE_KEY_timeOffStatus = "off_status";
    public static final String PRE_KEY_timeOnMinute = "timeOnMinute";
    public static final String PRE_KEY_timeOnHour = "timeOnHour";
    public static final String PRE_KEY_timeOn_SPEED = "timeOnSpeed";
    public static final String PRE_KEY_timeOffHour = "timeOffHour";
    public static final String PRE_KEY_timeOffMinute = "timeOffMinute";
    public static final String PRE_KEY_timeOff_SPEED = "timeOffSpeed";
}
