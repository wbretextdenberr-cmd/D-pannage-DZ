package dz.depannage.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** المرحلة 1ج: تقييم، إشعارات، مشاركة موقع، خصوصية، قطع غيار، تسجيل المحل. */
public class Extras {

    interface Pick {
        void got(String value);
    }

    interface Multi {
        void got(JSONArray values);
    }

    interface ShopCb {
        void got(JSONObject info);
    }

    static final String[] TYPES = {"هيكل ميكانيك وكهرباء", "كهرباء وحدها"};

    private final NativeActivity a;
    private final Store s;

    Extras(NativeActivity a, Store s) {
        this.a = a;
        this.s = s;
    }

    private String me() {
        return String.valueOf(s.session());
    }

    /* ---------- الإشعارات ---------- */

    void notify(String owner, String title, String body) {
        try {
            JSONObject n = new JSONObject();
            n.put("id", "N-" + System.nanoTime());
            n.put("owner", owner);
            n.put("title", title);
            n.put("body", body);
            n.put("ts", System.currentTimeMillis());
            n.put("read", false);
            s.upsert("notifications", "id", n);
        } catch (Exception ignored) {
        }
    }

    int unread() {
        int c = 0;
        JSONArray l = s.list("notifications");
        for (int i = 0; i < l.length(); i++) {
            JSONObject o = l.optJSONObject(i);
            if (o != null && me().equals(o.optString("owner")) && !o.optBoolean("read")) {
                c++;
            }
        }
        return c;
    }

    void openNotifications() {
        LinearLayout c = a.column();
        c.addView(a.text("🔔 الإشعارات", 24, Color.WHITE, true));
        JSONArray l = s.list("notifications");
        JSONArray all = new JSONArray();
        int shown = 0;
        try {
            for (int i = l.length() - 1; i >= 0; i--) {
                JSONObject o = l.optJSONObject(i);
                if (o == null || !me().equals(o.optString("owner"))) {
                    continue;
                }
                shown++;
                String when = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                        .format(new Date(o.optLong("ts")));
                c.addView(a.text(o.optString("title"), 16, Color.WHITE, true));
                c.addView(a.text(o.optString("body") + " · " + when, 13, NativeActivity.MUTED, false));
            }
            for (int i = 0; i < l.length(); i++) {
                JSONObject o = l.getJSONObject(i);
                if (me().equals(o.optString("owner"))) {
                    o.put("read", true);
                }
                all.put(o);
            }
        } catch (Exception ignored) {
        }
        s.save("notifications", all);
        if (shown == 0) {
            c.addView(a.text("لا توجد إشعارات بعد.", 15, NativeActivity.TEXT, false));
        }
        c.addView(a.button("مسح السجل", NativeActivity.SURFACE, v -> {
            JSONArray keep = new JSONArray();
            JSONArray cur = s.list("notifications");
            for (int i = 0; i < cur.length(); i++) {
                JSONObject o = cur.optJSONObject(i);
                if (o != null && !me().equals(o.optString("owner"))) {
                    keep.put(o);
                }
            }
            s.save("notifications", keep);
            openNotifications();
        }));
        c.addView(a.button("رجوع", NativeActivity.SURFACE, v -> a.showHome()));
        a.show(c);
    }

    /* ---------- التقييم ---------- */

    void rate(final JSONObject r) {
        final JSONObject t = r.optJSONObject("acceptedBy");
        if (t == null) {
            a.showHome();
            return;
        }
        final String[] labels = {"⭐", "⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐⭐"};
        final int[] pick = {4};
        new AlertDialog.Builder(a)
                .setTitle("قيّم القاطر " + t.optString("name"))
                .setSingleChoiceItems(labels, 4, (d, w) -> pick[0] = w)
                .setPositiveButton("إرسال", (d, w) -> {
                    try {
                        JSONObject x = new JSONObject();
                        x.put("requestId", r.optString("id"));
                        x.put("provider", t.optString("phone"));
                        x.put("stars", pick[0] + 1);
                        s.upsert("ratings", "requestId", x);
                        a.toast("شكرًا لتقييمك");
                    } catch (Exception ignored) {
                    }
                    a.showHome();
                })
                .setNegativeButton("تخطي", (d, w) -> a.showHome())
                .setCancelable(false)
                .show();
    }

    String ratingText(String phone) {
        JSONArray l = s.list("ratings");
        int n = 0, sum = 0;
        for (int i = 0; i < l.length(); i++) {
            JSONObject o = l.optJSONObject(i);
            if (o != null && phone.equals(o.optString("provider"))) {
                n++;
                sum += o.optInt("stars");
            }
        }
        return n == 0 ? "بدون تقييمات بعد" : String.format(Locale.US, "⭐ %.1f (%d)", sum / (double) n, n);
    }

    /* ---------- مشاركة الموقع ---------- */

    void share(final String phone) {
        a.flow.withLocation((lat, lng) -> {
            final String link = "https://www.google.com/maps?q=" + lat + "," + lng;
            final String text = "أحتاج مساعدة على الطريق. موقعي: " + link + " (Salakni DZ)";
            new AlertDialog.Builder(a)
                    .setTitle("📍 مشاركة موقعي")
                    .setItems(new String[]{"💬 رسالة SMS", "🟢 واتساب", "🗺️ فتح موقعي في الخرائط"},
                            (d, w) -> {
                                try {
                                    if (w == 0) {
                                        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone));
                                        i.putExtra("sms_body", text);
                                        a.startActivity(i);
                                    } else if (w == 1) {
                                        String num = phone.length() > 1 ? "213" + phone.substring(1) : "";
                                        a.startActivity(new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("https://wa.me/" + num + "?text=" + Uri.encode(text))));
                                    } else {
                                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                                    }
                                } catch (Exception e) {
                                    a.toast("تعذر فتح التطبيق");
                                }
                            })
                    .setNegativeButton("إلغاء", null)
                    .show();
        });
    }

    /* ---------- الخصوصية ---------- */

    void privacy() {
        new AlertDialog.Builder(a)
                .setTitle("📄 سياسة الخصوصية")
                .setMessage("ما هو Salakni DZ؟ تطبيق يربط سائقي المركبات المعطلة بمقدّمي خدمات القطر "
                        + "والإصلاح وقطع الغيار القريبين.\n\n"
                        + "البيانات: الاسم ورقم الهاتف ونوع الحساب وموقعك حين تطلب خدمة أو تسجّل كمقدّم "
                        + "خدمة، وبيانات الطلبات والتقييمات.\n\n"
                        + "لا نتتبع موقعك في الخلفية ولا نبيع بياناتك.\n\n"
                        + "في هذه النسخة تُحفظ البيانات على هاتفك، وعند ربط خادم تُحدَّث هذه السياسة.\n\n"
                        + "يرى القاطر المسافة والوجهة ونوع العطل دون رقمك، ويظهر لك رقمه بعد قبوله طلبك.\n\n"
                        + "لحذف بياناتك امسح بيانات التطبيق من إعدادات الهاتف.")
                .setPositiveButton("إغلاق", null)
                .show();
    }

    /* ---------- قطع الغيار ---------- */

    private void pickOne(String title, final String[] items, final Pick cb) {
        new AlertDialog.Builder(a).setTitle(title)
                .setItems(items, (d, w) -> cb.got(items[w]))
                .setNegativeButton("إلغاء", null).show();
    }

    void openParts() {
        pickOne("نوع المركبة", Flow.VEHICLES, veh ->
                pickOne("نوع القطع", TYPES, type -> askBrand(veh, type)));
    }

    private void askBrand(final String veh, final String type) {
        final EditText e = new EditText(a);
        e.setHint("العلامة (اختياري، مثل Renault)");
        new AlertDialog.Builder(a).setTitle("علامة المركبة").setView(e)
                .setPositiveButton("بحث", (d, w) -> {
                    final String brand = e.getText().toString().trim();
                    a.flow.withLocation((lat, lng) -> results(veh, type, brand, lat, lng));
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private boolean has(JSONArray arr, String val) {
        if (arr == null || arr.length() == 0) {
            return true;
        }
        for (int i = 0; i < arr.length(); i++) {
            if (val.equalsIgnoreCase(arr.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private void results(String veh, String type, String brand, double lat, double lng) {
        final List<JSONObject> found = new ArrayList<>();
        final List<Double> dist = new ArrayList<>();
        JSONArray users = s.list("users");
        for (int i = 0; i < users.length(); i++) {
            JSONObject u = users.optJSONObject(i);
            if (u == null || !"محل قطع غيار".equals(u.optString("role"))) {
                continue;
            }
            if (!has(u.optJSONArray("vehicles"), veh) || !has(u.optJSONArray("types"), type)) {
                continue;
            }
            if (!brand.isEmpty() && !has(u.optJSONArray("brands"), brand)) {
                continue;
            }
            found.add(u);
            dist.add(u.has("lat") ? Dispatch.km(lat, lng, u.optDouble("lat"), u.optDouble("lng")) : 9999.0);
        }
        Integer[] order = new Integer[found.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        java.util.Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer x, Integer y) {
                return Double.compare(dist.get(x), dist.get(y));
            }
        });
        LinearLayout c = a.column();
        c.addView(a.text("⚙️ محلات قطع الغيار", 24, Color.WHITE, true));
        c.addView(a.text(veh + " · " + type + (brand.isEmpty() ? "" : " · " + brand),
                13, NativeActivity.MUTED, false));
        if (order.length == 0) {
            c.addView(a.text("لا يوجد محل قطع غيار مسجّل لهذا الاختيار بعد.", 15, NativeActivity.TEXT, false));
        }
        for (int idx : order) {
            final JSONObject u = found.get(idx);
            LinearLayout card = new LinearLayout(a);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(a.dp(16), a.dp(16), a.dp(16), a.dp(16));
            card.setBackground(a.shape(NativeActivity.SURFACE, NativeActivity.BORDER, 16));
            card.addView(a.text("🔩 " + u.optString("name"), 17, Color.WHITE, true));
            double km = dist.get(idx);
            card.addView(a.text(km > 9000 ? "المسافة غير معروفة"
                    : String.format(Locale.US, "📍 %.1f كم", km), 14, NativeActivity.MUTED, false));
            card.addView(a.button("📞 اتصل: " + u.optString("phone"), NativeActivity.BLUE,
                    v -> a.startActivity(new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + u.optString("phone"))))));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.topMargin = a.dp(10);
            card.setLayoutParams(p);
            c.addView(card);
        }
        c.addView(a.button("رجوع", NativeActivity.SURFACE, v -> a.showHome()));
        a.show(c);
    }

    /* ---------- تسجيل محل قطع الغيار ---------- */

    private void multi(final String title, final String[] items, final Multi cb) {
        final boolean[] on = new boolean[items.length];
        new AlertDialog.Builder(a).setTitle(title)
                .setMultiChoiceItems(items, on, (d, w, checked) -> on[w] = checked)
                .setPositiveButton("التالي", (d, w) -> {
                    JSONArray out = new JSONArray();
                    for (int i = 0; i < items.length; i++) {
                        if (on[i]) {
                            out.put(items[i]);
                        }
                    }
                    if (out.length() == 0) {
                        a.toast("اختر خيارًا واحدًا على الأقل");
                        multi(title, items, cb);
                    } else {
                        cb.got(out);
                    }
                })
                .setNegativeButton("إلغاء", null).setCancelable(false).show();
    }

    void pickShop(final ShopCb cb) {
        multi("المركبات التي يبيع لها المحل", Flow.VEHICLES, veh ->
                multi("نوع القطع", TYPES, types -> {
                    final EditText e = new EditText(a);
                    e.setHint("العلامات مفصولة بفاصلة (فارغ = كل العلامات)");
                    new AlertDialog.Builder(a).setTitle("العلامات").setView(e)
                            .setPositiveButton("تم", (d, w) -> {
                                try {
                                    JSONArray brands = new JSONArray();
                                    for (String b : e.getText().toString().split("[,،]")) {
                                        if (!b.trim().isEmpty()) {
                                            brands.put(b.trim());
                                        }
                                    }
                                    JSONObject info = new JSONObject();
                                    info.put("vehicles", veh);
                                    info.put("types", types);
                                    info.put("brands", brands);
                                    cb.got(info);
                                } catch (Exception ignored) {
                                }
                            })
                            .setCancelable(false).show();
                }));
    }
}
