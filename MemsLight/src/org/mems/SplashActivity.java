package org.mems;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

public class SplashActivity extends Activity {
	/**
	 * Called when the activity is first created.
	 */
	@Override  
    public void onCreate(Bundle icicle) {  
        super.onCreate(icicle);
        
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        setContentView(R.layout.splash_activity);
        
        //Display the current version number  
        PackageManager pm = getPackageManager();  
        try {
        	PackageInfo pi = pm.getPackageInfo("org.mems", 0);
             TextView versionNumber = (TextView) findViewById(R.id.versionNumber);  
             versionNumber.setText("Version " + pi.versionName);  
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        
        new Handler().postDelayed(new Runnable() {
            public void run() {  
            	Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);  
            	SplashActivity.this.startActivity(mainIntent);  
            	SplashActivity.this.finish();  
            }  
        }, 2000); //2000 for release  
    }}
