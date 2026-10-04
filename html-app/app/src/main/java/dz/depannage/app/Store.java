package dz.depannage.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/** تخزين محلي تجريبي. عند ربط الخادم تُستبدل محتويات هذه الدوال. */
public class Store {

    private final SharedPreferences prefs;

    public Store(Context context) {
        prefs = context.getSharedPreferences("salakni_native", Context.MODE_PRIVATE);
    }

    public JSONArray list(String key) {
        try {
            return new JSONArray(prefs.getString(key, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void save(String key, JSONArray array) {
        prefs.edit().putString(key, array.toString()).apply();
    }

    public JSONObject findBy(String key, String field, String value) {
        JSONArray array = list(key);
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o != null && value.equals(o.optString(field))) {
                return o;
            }
        }
        return null;
    }

    public void upsert(String key, String field, JSONObject item) {
        try {
            JSONArray array = list(key);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                if (item.optString(field).equals(o.optString(field))) {
                    array.put(i, item);
                    save(key, array);
                    return;
                }
            }
            array.put(item);
            save(key, array);
        } catch (Exception ignored) {
        }
    }

    public String session() {
        return prefs.getString("session", null);
    }

    public void setSession(String phone) {
        if (phone == null) {
            prefs.edit().remove("session").apply();
        } else {
            prefs.edit().putString("session", phone).apply();
        }
    }
}
