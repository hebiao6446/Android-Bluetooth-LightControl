
package org.mems;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

@SuppressLint("SetJavaScriptEnabled")
public class HelpWebSite extends Activity {

    public static String EXTRA_HTML = "source";
    public static String EXTRA_TITLE = "title";
    WebView mWebView;
//    private TopBarView mTitleLayoutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viphelp_website);

//        initTitle();
        final String url = getIntent().getStringExtra(EXTRA_HTML);
        Log.d("webviewcrash", "#39:" + url);
        if (url != null && url.length() > 0) {
            mWebView = (WebView) findViewById(R.id.webview);
            WebSettings wSet = mWebView.getSettings();
            wSet.setJavaScriptEnabled(true);
            wSet.setUseWideViewPort(true);
            wSet.setLoadWithOverviewMode(true);
            mWebView.loadUrl(url);
        }
    }

//    private void initTitle() {
//        mTitleLayoutView = (TopBarView) findViewById(R.id.titleView);
//        mTitleLayoutView.setTitleSize(FontManager.getTitleTextSize(this));
//        mTitleLayoutView.setTitle(getIntent().getStringExtra(EXTRA_TITLE));
//        mTitleLayoutView.setOnCancelListener(new TopBarView.OnCancelClickListener() {
//            @Override
//            public void onCancelClickListener() {
//                // TODO Auto-generated method stub
//                finish();
//            }
//        });
//    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.setVisibility(View.GONE);
            mWebView.destroy();
        }
        super.onDestroy();
    }
}
