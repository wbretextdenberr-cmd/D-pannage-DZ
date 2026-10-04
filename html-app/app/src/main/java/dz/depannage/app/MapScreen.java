package dz.depannage.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.Locale;

/** المرحلة 2: الخريطة الأصلية (OpenStreetMap عبر osmdroid). */
public class MapScreen {

    static final String[] FILTERS = {"الكل", "قاطر", "ورشة ثابتة", "ورشة متنقلة", "محل قطع غيار"};

    private final NativeActivity a;
    private final Store s;
    private MapView map;
    private double myLat, myLng;
    private String currentFilter = "الكل";

    MapScreen(NativeActivity a, Store s) {
        this.a = a;
        this.s = s;
    }

    void open() {
        Configuration.getInstance().setUserAgentValue(a.getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(new File(a.getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(a.getCacheDir(), "osmdroid/tiles"));
        double[] q = a.flow.quick();
        render(q != null ? q[0] : 36.75, q != null ? q[1] : 3.06, q != null ? 13.0 : 6.0);
        a.flow.withLocation((lat, lng) -> recenter(lat, lng));
    }

    private void recenter(double lat, double lng) {
        if (map == null) {
            return;
        }
        myLat = lat;
        myLng = lng;
        map.getController().setZoom(13.0);
        map.getController().animateTo(new GeoPoint(lat, lng));
        refresh(currentFilter);
    }

    private void render(double lat, double lng, double zoom) {
        myLat = lat;
        myLng = lng;

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(NativeActivity.BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                v.setPadding(0, insets.getSystemWindowInsetTop(),
                        0, insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(a.dp(12), a.dp(8), a.dp(12), a.dp(8));
        bar.addView(a.text("🗺️ القريب مني", 18, Color.WHITE, true),
                new LinearLayout.LayoutParams(0, -2, 1f));
        final Spinner filter = new Spinner(a);
        filter.setAdapter(new ArrayAdapter<String>(a,
                android.R.layout.simple_spinner_dropdown_item, FILTERS));
        bar.addView(filter);
        root.addView(bar);

        map = new MapView(a);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(zoom);
        map.getController().setCenter(new GeoPoint(lat, lng));
        root.addView(map, new LinearLayout.LayoutParams(-1, 0, 1f));

        addBack(root);
        a.setContentView(root);
        root.requestApplyInsets();
        map.onResume();

        refresh(FILTERS[0]);
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                refresh(FILTERS[pos]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        });
    }

    private void addBack(LinearLayout root) {
        root.addView(a.button("رجوع", NativeActivity.SURFACE, v -> {
            if (map != null) {
                map.onDetach();
                map = null;
            }
            a.showHome();
        }));
    }

    private void refresh(String filter) {
        currentFilter = filter;
        if (map == null) {
            return;
        }
        map.getOverlays().clear();

        Marker me = new Marker(map);
        me.setPosition(new GeoPoint(myLat, myLng));
        me.setTitle("موقعي");
        me.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(me);

        JSONArray users = s.list("users");
        for (int i = 0; i < users.length(); i++) {
            final JSONObject u = users.optJSONObject(i);
            if (u == null || !u.has("lat") || !u.has("lng")
                    || "سائق".equals(u.optString("role"))) {
                continue;
            }
            if (!"الكل".equals(filter) && !filter.equals(u.optString("role"))) {
                continue;
            }
            Marker m = new Marker(map);
            m.setPosition(new GeoPoint(u.optDouble("lat"), u.optDouble("lng")));
            m.setTitle(u.optString("name"));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {
                    details(u);
                    return true;
                }
            });
            map.getOverlays().add(m);
        }
        map.invalidate();
    }

    private void details(final JSONObject u) {
        double km = Dispatch.km(myLat, myLng, u.optDouble("lat"), u.optDouble("lng"));
        String info = u.optString("role") + "\n"
                + String.format(Locale.US, "📍 %.1f كم", km);
        if ("قاطر".equals(u.optString("role"))) {
            info += "\n" + a.flow.x.ratingText(u.optString("phone"));
            if (u.optBoolean("busy", false)) {
                info += "\n⏳ مشغول حاليًا";
            }
        }
        new AlertDialog.Builder(a)
                .setTitle(u.optString("name"))
                .setMessage(info)
                .setPositiveButton("📞 اتصال", (d, w) -> a.startActivity(
                        new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + u.optString("phone")))))
                .setNeutralButton("🧭 توجّه", (d, w) -> {
                    try {
                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination="
                                        + u.optDouble("lat") + "," + u.optDouble("lng"))));
                    } catch (Exception e) {
                        a.toast("تعذر فتح الخرائط");
                    }
                })
                .setNegativeButton("إغلاق", null)
                .show();
    }
}
