package com.gpstracker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryReceiver extends BroadcastReceiver {

    // એક જ લેવલ પર વારંવાર અપડેટ ન થાય તે માટે
    private static int lastUpdatedLevel = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level != -1 && scale != -1) {
            int batteryPct = (int) ((level / (float) scale) * 100);

            // જો બેટરી 15, 10, કે 5 હોય અને તે લેવલ હમણાં જ અપડેટ ન કર્યું હોય
            if ((batteryPct == 15 || batteryPct == 10 || batteryPct == 5) && batteryPct != lastUpdatedLevel) {

                lastUpdatedLevel = batteryPct; // લેવલ સેવ કરી લો
                updateBatteryUsingHelper(batteryPct);
                Log.d("BatteryReceiver", "Triggering update for: " + batteryPct + "%");

            } else if (batteryPct != 15 && batteryPct != 10 && batteryPct != 5) {
                // જ્યારે બેટરી લેવલ બદલાય (દા.ત. 14% થાય), ત્યારે જૂનું લેવલ રીસેટ કરો
                lastUpdatedLevel = -1;
            }
        }
    }

    private void updateBatteryUsingHelper(int batteryLevel) {
        FirebaseHelper helper = new FirebaseHelper();
        if (helper.isUserLoggedIn()) {
            // "15%" સ્ટ્રિંગ તરીકે મોકલવા માટે જો તમારા મોડેલમાં ફેરફાર હોય તો
            helper.updateBatteryLevel(batteryLevel);
            Log.d("BatteryReceiver", "Battery level updated: " + batteryLevel + "%");
        }
    }
}