package com.herorush.deckbuilder;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Thin WebView shell around the Apps Script web app. */
public class MainActivity extends Activity {
    private static final int FILE_REQ = 1;
    private static final int BG = 0xFF10131C;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private WebView web;
    private ProgressBar bar;
    private LinearLayout errorView;
    private ValueCallback<Uri[]> filePicker;
    private String appHost;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        appHost = Uri.parse(BuildConfig.APP_URL).getHost();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        web = new WebView(this);
        web.setBackgroundColor(BG);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(ColorStateList.valueOf(0xFFEF4444));
        root.addView(bar, new FrameLayout.LayoutParams(-1, (int) (3 * getResources().getDisplayMetrics().density), Gravity.TOP));

        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        errorView.setBackgroundColor(BG);
        errorView.setVisibility(View.GONE);
        TextView msg = new TextView(this);
        msg.setText("Can't reach the deckbuilder.\nCheck your connection and try again.");
        msg.setTextColor(0xFFECE9E1);
        msg.setGravity(Gravity.CENTER);
        msg.setTextSize(16);
        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setOnClickListener(v -> { errorView.setVisibility(View.GONE); web.loadUrl(BuildConfig.APP_URL); });
        errorView.addView(msg);
        errorView.addView(retry);
        root.addView(errorView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // the app remembers theme and name in localStorage
        s.setSupportMultipleWindows(true);     // links with target=_blank open in the browser
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setBuiltInZoomControls(false);
        s.setAllowFileAccess(false);
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true); // Apps Script serves the page inside an iframe

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (!r.isForMainFrame()) return false;
                if (allowed(r.getUrl())) return false;
                openExternal(r.getUrl());
                return true;
            }
            @Override public void onPageStarted(WebView v, String url, Bitmap f) { bar.setVisibility(View.VISIBLE); }
            @Override public void onPageFinished(WebView v, String url) { bar.setVisibility(View.GONE); }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) errorView.setVisibility(View.VISIBLE);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) { bar.setProgress(p); }
            @Override public boolean onCreateWindow(WebView v, boolean dialog, boolean gesture, Message result) {
                WebView tmp = new WebView(MainActivity.this);
                tmp.setWebViewClient(new WebViewClient() {
                    @Override public void onPageStarted(WebView w, String url, Bitmap f) {
                        if (url != null && !url.equals("about:blank")) openExternal(Uri.parse(url));
                        w.stopLoading();
                        ui.post(w::destroy);
                    }
                });
                ((WebView.WebViewTransport) result.obj).setWebView(tmp);
                result.sendToTarget();
                return true;
            }
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (filePicker != null) filePicker.onReceiveValue(null);
                filePicker = cb;
                try { startActivityForResult(p.createIntent(), FILE_REQ); }
                catch (ActivityNotFoundException e) { filePicker = null; return false; }
                return true;
            }
        });

        if (saved != null) web.restoreState(saved); else web.loadUrl(BuildConfig.APP_URL);
    }

    /** Stay in the app for the web app and Google pages; send everything else to the browser. */
    private boolean allowed(Uri u) {
        String h = u.getHost();
        if (h == null || !"https".equals(u.getScheme())) return false;
        return h.equals(appHost) || h.equals("google.com") || h.endsWith(".google.com") || h.endsWith(".googleusercontent.com");
    }

    private void openExternal(Uri u) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) { }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == FILE_REQ && filePicker != null) {
            filePicker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            filePicker = null;
        } else super.onActivityResult(req, res, data);
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override protected void onPause() { web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
    @Override protected void onDestroy() { web.destroy(); super.onDestroy(); }
    @Override @SuppressWarnings("deprecation")
    public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
