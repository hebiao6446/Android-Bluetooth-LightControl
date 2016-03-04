package org.mems;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by shuizhu on 2014/12/6.
 */
public class ShutdownReceiver extends BroadcastReceiver {
    public static final String EXTRA_SHUTDOWN = "shutdown";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_SHUTDOWN)){
            Intent intent1 = new Intent();
            intent1.setClass(context, BLEService.class);
            intent1.putExtra(EXTRA_SHUTDOWN, true);
            context.startService(intent1);
        }
    }
}
