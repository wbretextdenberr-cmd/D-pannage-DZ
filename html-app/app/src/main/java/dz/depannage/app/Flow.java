package dz.depannage.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONObject;

/** المرحلة 1ب: الموقع، طلب القطر، قبول القاطر، الاتصال. */
public class Flow {

    interface Callback {
        void got(double lat, double lng);
    }

    static final int PERM = 77;
    static final String[] FAULTS = {"غير محدد", "بطارية", "إطار", "وقود", "محرك", "حادث", "أخرى"};
    static final String[] VEHICLES = {"سيارة", "شاحنة", "دراجة نارية"};

    private final NativeActivity a;
    private final Store s;
    private Callback pending;
    private String screen = "";
    private final Handler handler = new Handler(Looper.getMainLooper());

    Flow(NativeActivity a, Store s) {
        this.a = a;
        this.s = s;
    }

    /* ---------- الموقع ---------- */

    void withLocation(final Callback cb) {
        if (a.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            pending = cb;
            a.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERM);
            return;
        }
        final LocationManager lm = (LocationManager) a.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps && !net) {
            new AlertDialog.Builder(a).setTitle("تفعيل الموقع")
                    .setMessage("خدمة الموقع (GPS) مغلقة. فعّلها ثم أعد المحاولة.")
                    .setPositiveButton("فتح الإعدادات", (d, w) ->
                            a.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("لاحقًا", null).show();
            return;
        }
        Location best = null;
        try {
            for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) {
                    best = l;
                }
            }
        } catch (SecurityException ignored) {
        }
        if (best != null) {
            cb.got(best.getLatitude(), best.getLongitude());
            return;
        }
        a.toast("جارٍ تحديد موقعك...");
        try {
            lm.requestSingleUpdate(gps ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location l) {
                            cb.got(l.getLatitude(), l.getLongitude());
                        }

                        @Override
                        public void onStatusChanged(String p, int st, Bundle e) {
                        }

                        @Override
                        public void onProviderEnabled(String p) {
                        }

                        @Override
                        public void onProviderDisabled(String p) {
                        }
                    }, Looper.getMainLooper());
        } catch (SecurityException e) {
            a.toast("إذن الموقع مطلوب");
        }
    }

    void onPermission(int[] results) {
        boolean ok = false;
        for (int r : results) {
            if (r == PackageManager.PERMISSION_GRANTED) {
                ok = true;
            }
        }
        Callback c = pending;
        pending = null;
        if (ok && c != null) {
            withLocation(c);
        } else {
            a.toast("إذن الموقع مطلوب لإكمال العملية");
        }
    }

    /* ---------- أدوات ---------- */

    private Spinner spinner(String[] items) {
        Spinner sp = new Spinner(a);
        sp.setAdapter(new ArrayAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item, items));
        return sp;
    }

    private String me() {
        return String.valueOf(s.session());
    }

    JSONObject activeRequest() {
        JSONArray all = s.list("requests");
        for (int i = all.length() - 1; i >= 0; i--) {
            JSONObject r = all.optJSONObject(i);
            if (r != null && me().equals(r.optString("driverPhone"))
                    && ("جديد".equals(r.optString("status"))
                    || "اختاره قاطر".equals(r.optString("status")))) {
                return r;
            }
        }
        return null;
    }

    private void setStatus(JSONObject r, String status) {
        try {
            r.put("status", status);
            s.upsert("requests", "id", r);
        } catch (Exception ignored) {
        }
    }

    /* ---------- السائق: طلب قطر ---------- */

    void openTowRequest() {
        if (activeRequest() != null) {
            showActive();
            return;
        }
        screen = "form";
        LinearLayout c = a.column();
        c.addView(a.text("🚗 طلب قطر", 24, Color.WHITE, true));
        c.addView(a.text("نوع العطل", 14, NativeActivity.MUTED, false));
        final Spinner fault = spinner(FAULTS);
        c.addView(fault);
        c.addView(a.text("نوع المركبة", 14, NativeActivity.MUTED, false));
        final Spinner vehicle = spinner(VEHICLES);
        c.addView(vehicle);
        final EditText wilaya = a.field("ولاية الوجهة", InputType.TYPE_CLASS_TEXT);
        final EditText commune = a.field("بلدية الوجهة", InputType.TYPE_CLASS_TEXT);
        c.addView(wilaya);
        c.addView(commune);
        c.addView(a.button("📍 إرسال الطلب بموقعي الحالي", NativeActivity.BLUE, v -> {
            final String w = wilaya.getText().toString().trim();
            final String m = commune.getText().toString().trim();
            if (w.isEmpty() || m.isEmpty()) {
                a.toast("أدخل ولاية وبلدية الوجهة");
                return;
            }
            final String f = FAULTS[fault.getSelectedItemPosition()];
            final String veh = VEHICLES[vehicle.getSelectedItemPosition()];
            withLocation((lat, lng) -> create(lat, lng, f, veh, w, m));
        }));
        c.addView(a.button("رجوع", NativeActivity.SURFACE, v -> a.showHome()));
        a.show(c);
    }

    private void create(double lat, double lng, String fault, String vehicle,
                        String wilaya, String commune) {
        try {
            JSONObject u = s.findBy("users", "phone", me());
            JSONObject r = new JSONObject();
            r.put("id", "REQ-" + System.currentTimeMillis());
            r.put("driverPhone", me());
            r.put("driverName", u == null ? "" : u.optString("name"));
            r.put("lat", lat);
            r.put("lng", lng);
            r.put("fault", fault);
            r.put("vehicle", vehicle);
            r.put("wilaya", wilaya);
            r.put("commune", commune);
            r.put("status", "جديد");
            r.put("dispatch", Dispatch.assign(lat, lng, s.list("users")));
            r.put("createdAt", System.currentTimeMillis());
            s.upsert("requests", "id", r);
            a.toast("✅ تم تسجيل طلبك");
            showActive();
        } catch (Exception e) {
            a.toast("تعذر تسجيل الطلب");
        }
    }

    /* ---------- السائق: الطلب الحالي ---------- */

    void showActive() {
        final JSONObject r = activeRequest();
        if (r == null) {
            a.showHome();
            return;
        }
        screen = "active";
        LinearLayout c = a.column();
        c.addView(a.text("طلبك الحالي", 24, Color.WHITE, true));
        c.addView(a.text("الحالة: " + r.optString("status"), 16, NativeActivity.TEXT, false));
        c.addView(a.text("العطل: " + r.optString("fault") + " · " + r.optString("vehicle"),
                14, NativeActivity.MUTED, false));
        c.addView(a.text("الوجهة: " + r.optString("wilaya") + " - " + r.optString("commune"),
                14, NativeActivity.MUTED, false));

        if ("جديد".equals(r.optString("status"))) {
            JSONObject d = r.optJSONObject("dispatch");
            int total = d == null ? 0 : d.optInt("total");
            c.addView(a.text(total > 0
                            ? "🚛 القاطرون المتاحون القريبون (حتى " + d.optInt("radiusKm") + " كم): " + total
                            : "لا يوجد قاطر متاح ضمن النطاق حاليًا",
                    15, NativeActivity.TEXT, false));
            handler.postDelayed(() -> {
                if ("active".equals(screen)) {
                    showActive();
                }
            }, 5000);
        } else {
            final JSONObject t = r.optJSONObject("acceptedBy");
            if (t != null) {
                c.addView(a.text("🚛 القاطر: " + t.optString("name"), 17, Color.WHITE, true));
                c.addView(a.button("📞 اتصل بالقاطر: " + t.optString("phone"), NativeActivity.BLUE,
                        v -> a.startActivity(new Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:" + t.optString("phone"))))));
            }
            c.addView(a.button("✅ تم إنجاز الطلب", 0xFF16A34A, v -> {
                setStatus(r, "منجز");
                a.toast("شكرًا لاستخدامك Salakni DZ");
                a.showHome();
            }));
        }
        c.addView(a.button("❌ إلغاء الطلب", NativeActivity.RED, v -> {
            setStatus(r, "ملغى");
            a.toast("تم إلغاء الطلب");
            a.showHome();
        }));
        c.addView(a.button("رجوع", NativeActivity.SURFACE, v -> a.showHome()));
        a.show(c);
    }

    /* ---------- القاطر: الطلبات المتاحة ---------- */

    void openTowerRequests() {
        screen = "tower";
        final JSONObject me = s.findBy("users", "phone", me());
        LinearLayout c = a.column();
        c.addView(a.text("📥 الطلبات المتاحة", 24, Color.WHITE, true));
        c.addView(a.text("تظهر لك الطلبات القريبة. اختر ما يناسبك وسيتصل بك السائق.",
                13, NativeActivity.MUTED, false));
        JSONArray all = s.list("requests");
        int shown = 0;
        for (int i = all.length() - 1; i >= 0; i--) {
            final JSONObject r = all.optJSONObject(i);
            JSONObject d = r == null ? null : r.optJSONObject("dispatch");
            if (r == null || d == null || !"جديد".equals(r.optString("status"))
                    || d.optJSONArray("order") == null
                    || d.optJSONArray("order").toString().indexOf("\"" + me() + "\"") < 0) {
                continue;
            }
            shown++;
            String km = "غير معروف";
            if (me != null && me.has("lat")) {
                km = String.format("%.1f كم", Dispatch.km(me.optDouble("lat"), me.optDouble("lng"),
                        r.optDouble("lat"), r.optDouble("lng")));
            }
            LinearLayout card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(a.dp(16), a.dp(16), a.dp(16), a.dp(16));
            card.setBackground(a.shape(NativeActivity.SURFACE, NativeActivity.BORDER, 16));
            card.addView(a.text("🚗 يبعد عنك: " + km, 16, Color.WHITE, true));
            card.addView(a.text("الوجهة: " + r.optString("wilaya") + " - " + r.optString("commune"),
                    14, NativeActivity.MUTED, false));
            card.addView(a.text("العطل: " + r.optString("fault") + " · " + r.optString("vehicle"),
                    14, NativeActivity.MUTED, false));
            card.addView(a.button("✅ أقبل هذا الطلب", NativeActivity.BLUE, v -> accept(r.optString("id"))));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = a.dp(10);
            card.setLayoutParams(p);
            c.addView(card);
        }
        if (shown == 0) {
            c.addView(a.text("لا توجد طلبات متاحة الآن.", 15, NativeActivity.TEXT, false));
        }
        c.addView(a.button("تحديث", NativeActivity.SURFACE, v -> openTowerRequests()));
        c.addView(a.button("رجوع", NativeActivity.SURFACE, v -> a.showHome()));
        a.show(c);
    }

    private void accept(String id) {
        JSONObject r = s.findBy("requests", "id", id);
        if (r == null || !"جديد".equals(r.optString("status"))) {
            a.toast("الطلب لم يعد متاحًا");
            openTowerRequests();
            return;
        }
        try {
            JSONObject u = s.findBy("users", "phone", me());
            JSONObject t = new JSONObject();
            t.put("phone", me());
            t.put("name", u == null ? "" : u.optString("name"));
            r.put("acceptedBy", t);
            r.put("acceptedAt", System.currentTimeMillis());
            setStatus(r, "اختاره قاطر");
            a.toast("✅ اخترت الطلب. سيتصل بك السائق قريبًا");
        } catch (Exception ignored) {
        }
        openTowerRequests();
    }
}
