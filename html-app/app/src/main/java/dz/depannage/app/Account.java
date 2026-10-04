package dz.depannage.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** حسابي: الحالة (متاح/مشغول)، تحديث الموقع، حذف الحساب، وقوائم مقدّمي الخدمة. */
public class Account {

    private final NativeActivity a;
    private final Store s;

    Account(NativeActivity a, Store s) {
        this.a = a;
        this.s = s;
    }

    private String me() {
        return String.valueOf(s.session());
    }

    private void save(JSONObject u) {
        s.upsert("users", "phone", u);
    }

    void open() {
        final JSONObject u = s.findBy("users", "phone", me());
        if (u == null) {
            a.showAuth(false);
            return;
        }
        final String role = u.optString("role");
        LinearLayout c = a.column();
        c.addView(a.text("👤 حسابي", 24, Color.WHITE, true));
        c.addView(a.text(u.optString("name"), 18, Color.WHITE, true));
        c.addView(a.text(role + " · " + u.optString("phone"), 14, NativeActivity.MUTED, false));

        if (!"سائق".equals(role)) {
            if ("قاطر".equals(role)) {
                c.addView(a.text(a.flow.x.ratingText(u.optString("phone")), 14, NativeActivity.MUTED, false));
            }
            final boolean busy = u.optBoolean("busy", false);
            c.addView(a.button(
                    busy ? "⏳ مشغول (اضغط لتصبح متاحًا)" : "✅ متاح (اضغط لتصبح مشغولًا)",
                    busy ? NativeActivity.RED : 0xFF16A34A, v -> {
                        try {
                            u.put("busy", !busy);
                            save(u);
                        } catch (Exception ignored) {
                        }
                        open();
                    }));
            c.addView(a.button("📍 تحديث موقعي", NativeActivity.BLUE,
                    v -> a.flow.withLocation((lat, lng) -> {
                        try {
                            u.put("lat", lat);
                            u.put("lng", lng);
                            save(u);
                            a.toast("تم تحديث موقعك");
                        } catch (Exception ignored) {
                        }
                    })));
        }
        int unread = a.flow.x.unread();
        c.addView(a.button("🔔 الإشعارات" + (unread > 0 ? " (" + unread + ")" : ""),
                NativeActivity.SURFACE, v -> a.flow.x.openNotifications()));
        c.addView(a.button("📄 سياسة الخصوصية", NativeActivity.SURFACE, v -> a.flow.x.privacy()));
        c.addView(a.button("🗑️ حذف حسابي", NativeActivity.RED, v -> confirmDelete(u)));
        c.addView(a.button("تسجيل الخروج", NativeActivity.SURFACE, v -> {
            s.setSession(null);
            a.showAuth(false);
        }));
        a.showNav(c, "account");
    }

    private void confirmDelete(final JSONObject u) {
        new AlertDialog.Builder(a)
                .setTitle("حذف الحساب؟")
                .setMessage("سيُحذف حسابك من هذا الجهاز نهائيًا ولا يمكن التراجع.")
                .setPositiveButton("حذف", (d, w) -> {
                    JSONArray all = s.list("users");
                    JSONArray keep = new JSONArray();
                    for (int i = 0; i < all.length(); i++) {
                        JSONObject o = all.optJSONObject(i);
                        if (o != null && !u.optString("phone").equals(o.optString("phone"))) {
                            keep.put(o);
                        }
                    }
                    s.save("users", keep);
                    s.setSession(null);
                    a.toast("تم حذف حسابك");
                    a.showAuth(false);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    /* ---------- قائمة مقدّمي خدمة من نوع واحد، مرتبة بالأقرب ---------- */

    void openProviders(final String role, final String title) {
        a.flow.withLocation((lat, lng) -> {
            final List<Object[]> found = new ArrayList<>();
            JSONArray users = s.list("users");
            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.optJSONObject(i);
                if (u == null || !role.equals(u.optString("role")) || !u.has("lat")) {
                    continue;
                }
                found.add(new Object[]{u, Dispatch.km(lat, lng, u.optDouble("lat"), u.optDouble("lng"))});
            }
            Collections.sort(found, new Comparator<Object[]>() {
                @Override
                public int compare(Object[] x, Object[] y) {
                    return Double.compare((Double) x[1], (Double) y[1]);
                }
            });
            LinearLayout c = a.column();
            c.addView(a.text(title, 24, Color.WHITE, true));
            if (found.isEmpty()) {
                c.addView(a.text("لا يوجد مقدّم خدمة مسجّل من هذا النوع بعد.", 15, NativeActivity.TEXT, false));
            }
            for (Object[] row : found) {
                final JSONObject u = (JSONObject) row[0];
                LinearLayout card = new LinearLayout(a);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(a.dp(16), a.dp(16), a.dp(16), a.dp(16));
                card.setBackground(a.shape(NativeActivity.SURFACE, NativeActivity.BORDER, 16));
                card.addView(a.text(u.optString("name"), 17, Color.WHITE, true));
                card.addView(a.text(String.format(Locale.US, "📍 %.1f كم", (Double) row[1])
                        + (u.optBoolean("busy", false) ? " · ⏳ مشغول" : " · ✅ متاح"),
                        14, NativeActivity.MUTED, false));
                card.addView(a.button("📞 اتصل: " + u.optString("phone"), NativeActivity.BLUE,
                        v -> a.startActivity(new Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:" + u.optString("phone"))))));
                card.addView(a.button("🧭 توجّه", NativeActivity.SURFACE, v -> {
                    try {
                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination="
                                        + u.optDouble("lat") + "," + u.optDouble("lng"))));
                    } catch (Exception e) {
                        a.toast("تعذر فتح الخرائط");
                    }
                }));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
                p.topMargin = a.dp(10);
                card.setLayoutParams(p);
                c.addView(card);
            }
            c.addView(a.button("رجوع", NativeActivity.SURFACE, v -> a.showHome()));
            a.show(c);
        });
    }
}
