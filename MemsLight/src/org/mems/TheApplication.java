package org.mems;

import android.app.Application;
import android.util.Log;

/**
 * Created by shuizhu on 2014/12/2.
 */
public class TheApplication extends Application {

    private BLEController mController = null;

    @Override
    public void onCreate() {
        mController = BLEController.getInstance(this);
        mController.toString();
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        Log.i("TheApplication", "onTerminate");
        super.onTerminate();
    }
}
