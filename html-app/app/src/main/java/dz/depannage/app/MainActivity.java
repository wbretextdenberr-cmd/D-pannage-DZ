package dz.depannage.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        android.webkit.WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setGeolocationEnabled(true);

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin,
                    GeolocationPermissions.Callback callback) {

                if (checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                        ||
                    checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    callback.invoke(origin, true, false);

                    // الإذن ممنوح: إن كان GPS مغلقًا نعرض نافذة التفعيل
                    promptEnableLocationIfNeeded();

                } else {

                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;

                    requestPermissions(
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            LOCATION_PERMISSION_REQUEST
                    );
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                return handleUrl(
                        request.getUrl().toString()
                );
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                return handleUrl(url);
            }
        });

        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    private boolean handleUrl(String url) {

        if (url.startsWith("tel:")) {

            try {

                Intent intent = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse(url)
                );

                startActivity(intent);

            } catch (Exception ignored) {
            }

            return true;
        }

        return false;
    }

    /* =====================================================
       خدمة الموقع (GPS)
    ===================================================== */

    private boolean isLocationEnabled() {

        try {

            LocationManager manager =
                    (LocationManager) getSystemService(
                            Context.LOCATION_SERVICE
                    );

            if (manager == null) {
                return true;
            }

            return manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            ) || manager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            );

        } catch (Exception e) {
            return true;
        }
    }

    private void promptEnableLocationIfNeeded() {

        if (isLocationEnabled() || isFinishing()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("تفعيل الموقع")
                .setMessage(
                        "خدمة الموقع (GPS) مغلقة على هاتفك. "
                                + "فعّلها ليتمكن التطبيق من تحديد موقعك، "
                                + "ثم عد واضغط الزر مرة أخرى."
                )
                .setPositiveButton(
                        "فتح الإعدادات",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(
                                    DialogInterface dialog,
                                    int which) {

                                try {

                                    startActivity(
                                            new Intent(
                                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                            )
                                    );

                                } catch (Exception ignored) {
                                }
                            }
                        }
                )
                .setNegativeButton("لاحقًا", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST) {

            boolean granted = false;

            for (int result : grantResults) {

                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (pendingGeoCallback != null) {

                pendingGeoCallback.invoke(
                        pendingGeoOrigin,
                        granted,
                        false
                );
            }

            pendingGeoCallback = null;
            pendingGeoOrigin = null;

            if (granted) {
                promptEnableLocationIfNeeded();
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
}
