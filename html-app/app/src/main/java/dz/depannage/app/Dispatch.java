package dz.depannage.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * التوزيع العادل: القاطرون المتاحون ضمن نطاق قريب (10 ثم 25 ثم 50 كم
 * إن قلّوا عن 3) يُرتَّبون عشوائيًا لتكافؤ الفرص. الخادم يطبّق نفس المنطق.
 */
public class Dispatch {

    static final int[] RADII = {10, 25, 50};
    static final int MIN_CANDIDATES = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static double km(double lat1, double lng1, double lat2, double lng2) {
        double r = Math.PI / 180;
        double x = (lat2 - lat1) * r;
        double y = (lng2 - lng1) * r;
        double q = Math.sin(x / 2) * Math.sin(x / 2)
                + Math.cos(lat1 * r) * Math.cos(lat2 * r) * Math.sin(y / 2) * Math.sin(y / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(q), Math.sqrt(1 - q));
    }

    /** users: قائمة الحسابات؛ يُختار منها القاطرون غير المشغولين الذين لهم موقع. */
    public static JSONObject assign(double lat, double lng, JSONArray users) {
        List<String> ids = new ArrayList<>();
        List<Double> dist = new ArrayList<>();
        for (int i = 0; i < users.length(); i++) {
            JSONObject u = users.optJSONObject(i);
            if (u == null || !"قاطر".equals(u.optString("role"))
                    || u.optBoolean("busy", false) || !u.has("lat") || !u.has("lng")) {
                continue;
            }
            ids.add(u.optString("phone"));
            dist.add(km(lat, lng, u.optDouble("lat"), u.optDouble("lng")));
        }
        int radius = RADII[RADII.length - 1];
        List<String> pool = new ArrayList<>();
        for (int r : RADII) {
            radius = r;
            pool.clear();
            for (int i = 0; i < ids.size(); i++) {
                if (dist.get(i) <= r) {
                    pool.add(ids.get(i));
                }
            }
            if (pool.size() >= MIN_CANDIDATES) {
                break;
            }
        }
        Collections.shuffle(pool, RANDOM);
        JSONObject out = new JSONObject();
        try {
            out.put("radiusKm", radius);
            out.put("total", pool.size());
            out.put("order", new JSONArray(pool));
        } catch (Exception ignored) {
        }
        return out;
    }
}
