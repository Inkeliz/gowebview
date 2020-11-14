package com.inkeliz.gowebview;

import android.os.Bundle;

import android.view.ViewGroup;
import android.app.Activity;
import android.view.View;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.content.Context;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.webkit.WebView;
import android.util.Log;
import android.os.Build;
import android.net.Proxy;
import java.lang.reflect.*;
import android.util.ArrayMap;
import android.content.Intent;
import java.util.concurrent.Semaphore;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import java.security.cert.Certificate;
import android.net.http.SslCertificate;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class gowebview_android {
    private View primaryView;
    private WebView webBrowser;
    private PublicKey[] additionalCerts;

    public class gowebview_boolean {
        private boolean b;
        public void Set(Boolean r) {b = r;}
        public boolean Get() {return b;}
    }

    public class gowebview_webbrowser extends WebViewClient {
        @Override public void onReceivedSslError(WebView v, final SslErrorHandler sslHandler, SslError err){
            if (!err.hasError(SslError.SSL_UNTRUSTED)) {
                super.onReceivedSslError(v, sslHandler, err);
                return;
            }

            if (additionalCerts == null || additionalCerts.length == 0) {
                super.onReceivedSslError(v, sslHandler, err);
                return;
            }

            for (int i = 0; i < additionalCerts.length; i++) {
                try{
                    err.getCertificate().getX509Certificate().verify(additionalCerts[i]);
                    sslHandler.proceed();
                    return;
                } catch (Exception e) {

                }
            }

            super.onReceivedSslError(v, sslHandler, err);
        }
    }

    // Executed when call `New(config *Config)`
    public void webview_create(View v) {
        primaryView = v;

        final Semaphore mutex = new Semaphore(0);

        ((Activity)primaryView.getContext()).runOnUiThread(new Runnable() {
            public void run() {
                webBrowser = new WebView(primaryView.getContext());
                WebSettings webSettings = webBrowser.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setSafeBrowsingEnabled(false);
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
                webSettings.setUseWideViewPort(true);
                webSettings.setLoadWithOverviewMode(true);

                webBrowser.setWebViewClient(new gowebview_webbrowser());

                mutex.release();
            }
        });

        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Executed when call `.SetURL(url string)`
    public void webview_navigate(String url) {
        ((Activity)primaryView.getContext()).runOnUiThread(new Runnable() {
            public void run() {
               webBrowser.loadUrl(url);
            }
        });
    }

    // Executed when call `.Run()` or `.SetVisibility()`
    public void webview_run() {
        ((Activity)primaryView.getContext()).runOnUiThread(new Runnable() {
            public void run() {
                ((Activity)primaryView.getContext()).setContentView(webBrowser);
            }
        });
    }

    // Executed when call `.Destroy()`
    public void webview_destroy() {
        ((Activity)primaryView.getContext()).runOnUiThread(new Runnable() {
            public void run() {
                ((Activity)primaryView.getContext()).setContentView(primaryView);

                webBrowser.onPause();
                webBrowser.removeAllViews();
                webBrowser.pauseTimers();
                webBrowser.destroy();
            }
        });
    }

    // Executed when call `.SetVisibility()`
    public void webview_hide() {
        ((Activity)primaryView.getContext()).runOnUiThread(new Runnable() {
            public void run() {
                ((Activity)primaryView.getContext()).setContentView(primaryView);
            }
        });
    }

    public boolean webview_proxy(String host, String port) {
        final Semaphore mutex = new Semaphore(0);
        final gowebview_boolean result = new gowebview_boolean();

        ((Activity)primaryView.getContext()).runOnUiThread(new Runnable() {
            public void run() {
                Context app = webBrowser.getContext().getApplicationContext();

                System.setProperty("http.proxyHost", host);
                System.setProperty("http.proxyPort", port + "");
                System.setProperty("https.proxyHost", host);
                System.setProperty("https.proxyPort", port + "");

                try {
                    Field apk = app.getClass().getDeclaredField("mLoadedApk");
                    apk.setAccessible(true);

                    Field receivers = Class.forName("android.app.LoadedApk").getDeclaredField("mReceivers");
                    receivers.setAccessible(true);

                    for (Object map : ((ArrayMap) receivers.get(apk.get(app))).values()) {

                        for (Object receiver : ((ArrayMap) map).keySet()) {

                            Class<?> cls = receiver.getClass();
                            if (cls.getName().contains("ProxyChangeListener")) {
                                cls.getDeclaredMethod("onReceive", Context.class, Intent.class).invoke(receiver, app, new Intent(Proxy.PROXY_CHANGE_ACTION));
                            }
                        }

                    }

                    result.Set(true);
                } catch(Exception e) {
                    result.Set(false);
                }

                mutex.release();
            }
        });

        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result.Get();
    }

    public void webview_certs(String der) {
        String[] sCerts = der.split(";");


        additionalCerts = new PublicKey[sCerts.length];

        for (int i = 0; i < sCerts.length; i++) {
            InputStream streamCert = new ByteArrayInputStream(Base64.getDecoder().decode(sCerts[i]));
            try {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                 X509Certificate cert = (X509Certificate)factory.generateCertificate(streamCert);

                 additionalCerts[i] = cert.getPublicKey();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}