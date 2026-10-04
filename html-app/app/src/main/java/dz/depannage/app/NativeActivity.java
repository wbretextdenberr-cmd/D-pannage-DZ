package dz.depannage.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** Salakni DZ بالكود الأصلي - المرحلة 1أ: الدخول والتسجيل والرئيسية والطوارئ. */
public class NativeActivity extends Activity {

    static final int BG = 0xFF0B1324, SURFACE = 0xFF172238, BORDER = 0xFF26385C,
            BLUE = 0xFF2563EB, RED = 0xFFDC2626, TEXT = 0xFFE6EDF7, MUTED = 0xFF94A3B8;

    static final String[] ROLES = {"سائق", "قاطر", "ورشة ثابتة", "ورشة متنقلة", "محل قطع غيار"};

    Store store;
    Flow flow;
    MapScreen mapScreen;
    Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
        getWindow().setNavigationBarColor(BG);
        getWindow().setStatusBarColor(BG);
        store = new Store(this);
            flow = new Flow(this, store);
            mapScreen = new MapScreen(this, store);
            account = new Account(this, store);

            String phone = store.session();
            if (phone != null && store.findBy("users", "phone", phone) != null) {
                showHome();
            } else {
                showAuth(false);
            }
        } catch (Throwable t) {
            showError(t);
        }
    }

    /** يعرض سبب التعطل على الشاشة بدل إغلاق التطبيق. */
    private void showError(Throwable t) {
        java.io.StringWriter w = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(w));
        TextView tv = new TextView(this);
        tv.setText("حدث خطأ أثناء التشغيل. صوّر هذه الشاشة وأرسلها:\n\n" + w);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11);
        tv.setTextIsSelectable(true);
        tv.setPadding(24, 60, 24, 24);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF7F1D1D);
        sv.addView(tv);
        setContentView(sv);
    }

    /* ---------- أدوات الواجهة ---------- */

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        if (stroke != 0) {
            g.setStroke(dp(1), stroke);
        }
        return g;
    }

    LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(24));
        return c;
    }

    void show(LinearLayout content) {
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(BG);
        sv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sv.addView(content);
        sv.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                v.setPadding(0, insets.getSystemWindowInsetTop(),
                        0, insets.getSystemWindowInsetBottom());
                return insets;
            }
        });
        setContentView(sv);
        sv.requestApplyInsets();
    }

    TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return t;
    }

    TextView button(String label, int fill, View.OnClickListener l) {
        TextView t = text(label, 16, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        t.setBackground(shape(fill, 0, 12));
        t.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(10);
        t.setLayoutParams(p);
        return t;
    }

    EditText field(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setInputType(inputType);
        e.setBackground(shape(0xFF0F1A30, 0xFF2F4268, 12));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(10);
        e.setLayoutParams(p);
        return e;
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /* ---------- الدخول والتسجيل ---------- */

    void showAuth(final boolean register) {
        LinearLayout c = column();

        TextView title = text("Salakni DZ", 30, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        c.addView(title);

        TextView sub = text(register ? "إنشاء حساب" : "تسجيل الدخول", 18, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(14));
        c.addView(sub);

        final EditText name = field("الاسم", InputType.TYPE_CLASS_TEXT);
        final EditText phone = field("رقم الهاتف (05 / 06 / 07)", InputType.TYPE_CLASS_PHONE);
        final EditText pw = field("كلمة المرور (4 أحرف على الأقل)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final Spinner role = new Spinner(this);
        role.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, ROLES));

        if (register) {
            c.addView(name);
        }
        c.addView(phone);
        c.addView(pw);
        if (register) {
            c.addView(role);
        }

        c.addView(button(register ? "إنشاء الحساب" : "دخول", BLUE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ph = Auth.normalize(phone.getText().toString());
                String pass = pw.getText().toString();
                if (!Auth.validPhone(ph)) {
                    toast("أدخل رقمًا جزائريًا صحيحًا (جيزي أو موبيليس أو أوريدو)");
                    return;
                }
                if (register) {
                    register(name.getText().toString().trim(), ph, pass,
                            ROLES[role.getSelectedItemPosition()]);
                } else {
                    login(ph, pass);
                }
            }
        }));

        c.addView(button(register ? "لدي حساب، دخول" : "إنشاء حساب جديد", SURFACE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showAuth(!register);
                    }
                }));

        show(c);
    }

    private void login(String phone, String password) {
        JSONObject user = store.findBy("users", "phone", phone);
        if (user == null) {
            toast("لا يوجد حساب بهذا الرقم. أنشئ حسابًا أولًا.");
            return;
        }
        if (!user.optString("passwordHash").equals(Auth.hash(phone, password))) {
            toast("رقم الهاتف أو كلمة المرور غير صحيحة");
            return;
        }
        store.setSession(phone);
        showHome();
    }

    private void register(final String name, final String phone, final String password,
                          final String role) {
        if (name.isEmpty()) {
            toast("أدخل اسمك");
            return;
        }
        if (password.length() < 4) {
            toast("كلمة المرور 4 أحرف على الأقل");
            return;
        }
        if (store.findBy("users", "phone", phone) != null) {
            toast("هذا الرقم مسجّل من قبل. سجّل الدخول");
            return;
        }
        askOtp(phone, new Runnable() {
            @Override
            public void run() {
                if ("سائق".equals(role)) {
                    saveUser(name, phone, password, role, null, null);
                } else if ("محل قطع غيار".equals(role)) {
                    flow.x.pickShop(new Extras.ShopCb() {
                        @Override
                        public void got(final JSONObject info) {
                            flow.withLocation(new Flow.Callback() {
                                @Override
                                public void got(double lat, double lng) {
                                    saveUser(name, phone, password, role, new double[]{lat, lng}, info);
                                }
                            });
                        }
                    });
                } else {
                    flow.withLocation(new Flow.Callback() {
                        @Override
                        public void got(double lat, double lng) {
                            saveUser(name, phone, password, role, new double[]{lat, lng}, null);
                        }
                    });
                }
            }
        });
    }

    private void saveUser(String name, String phone, String password, String role,
                          double[] loc, JSONObject extra) {
        try {
            JSONObject u = new JSONObject();
            u.put("phone", phone);
            u.put("name", name);
            u.put("role", role);
            u.put("passwordHash", Auth.hash(phone, password));
            u.put("createdAt", System.currentTimeMillis());
            if (loc != null) {
                u.put("lat", loc[0]);
                u.put("lng", loc[1]);
                u.put("busy", false);
            }
            if (extra != null) {
                java.util.Iterator<String> keys = extra.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    u.put(key, extra.get(key));
                }
            }
            store.upsert("users", "phone", u);
            store.setSession(phone);
            showHome();
        } catch (Exception e) {
            toast("تعذر إنشاء الحساب");
        }
    }

    /** وضع تجريبي: الرمز يظهر في النافذة. الخادم يرسله عبر SMS. */
    private void askOtp(String phone, final Runnable onOk) {
        final String code = Auth.newOtp();
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("الرمز المكوّن من 6 أرقام");
        new AlertDialog.Builder(this)
                .setTitle("تأكيد رقم الهاتف")
                .setMessage("أرسلنا رمزًا إلى " + phone + "\nوضع تجريبي: الرمز هو " + code)
                .setView(input)
                .setPositiveButton("تأكيد", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        if (code.equals(input.getText().toString().trim())) {
                            onOk.run();
                        } else {
                            toast("الرمز غير صحيح");
                        }
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    /* ---------- الرئيسية ---------- */

    /* ---------- الشريط السفلي ---------- */

    void showNav(LinearLayout content, String active) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(content);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(0xFF0F1A30);
        nav.setPadding(0, dp(6), 0, dp(6));
        String[][] items = {
                {"home", "🏠", "الرئيسية"}, {"map", "📍", "القريب مني"},
                {"account", "👤", "حسابي"}, {"sos", "🚨", "طوارئ"}
        };
        for (final String[] it : items) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(0, dp(4), 0, dp(4));
            int color = it[0].equals(active) ? 0xFF60A5FA : MUTED;
            TextView icon = text(it[1], 22, Color.WHITE, false);
            icon.setGravity(Gravity.CENTER);
            TextView label = text(it[2], 11, color, it[0].equals(active));
            label.setGravity(Gravity.CENTER);
            cell.addView(icon);
            cell.addView(label);
            cell.setOnClickListener(v -> navGo(it[0]));
            nav.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        root.addView(nav);

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                v.setPadding(0, insets.getSystemWindowInsetTop(),
                        0, insets.getSystemWindowInsetBottom());
                return insets;
            }
        });
        setContentView(root);
        root.requestApplyInsets();
    }

    private void navGo(String id) {
        if ("home".equals(id)) {
            showHome();
        } else if ("map".equals(id)) {
            mapScreen.open();
        } else if ("account".equals(id)) {
            account.open();
        } else {
            emergency();
        }
    }

    private TextView circle(String emoji, int fill, View.OnClickListener l) {
        TextView t = text(emoji, 20, Color.WHITE, false);
        t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        t.setBackground(g);
        t.setOnClickListener(l);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(46), dp(46)));
        return t;
    }

    private LinearLayout tile(final String[] it) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(14), dp(10), dp(14));
        t.setBackground(shape(SURFACE, BORDER, 20));
        TextView icon = text(it[0], 40, Color.WHITE, false);
        icon.setGravity(Gravity.CENTER);
        TextView title = text(it[1], 16, TEXT, true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(8), 0, dp(2));
        TextView sub = text(it[2], 12, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        t.addView(icon);
        t.addView(title);
        t.addView(sub);
        t.setOnClickListener(v -> {
            if (it[1].startsWith("قطر")) {
                flow.openTowRequest();
            } else if (it[1].startsWith("ورشة")) {
                account.openProviders("ورشة متنقلة", "🔧 ورش متنقلة قريبة");
            } else {
                flow.x.openParts();
            }
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(150), 1f);
        p.setMargins(dp(6), dp(6), dp(6), dp(6));
        t.setLayoutParams(p);
        return t;
    }

    /* ---------- الرئيسية ---------- */

    void showHome() {
        JSONObject user = store.findBy("users", "phone", String.valueOf(store.session()));
        LinearLayout c = column();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(circle("👤", BLUE, v -> account.open()));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.CENTER);
        TextView name = text("Salakni DZ", 24, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        TextView tag = text("مساعدتك عند تعطل مركبتك", 12, MUTED, false);
        tag.setGravity(Gravity.CENTER);
        titles.addView(name);
        titles.addView(tag);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        header.addView(circle("🚨", RED, v -> emergency()));
        c.addView(header);

        TextView hello = text("مرحبًا " + (user == null ? "" : user.optString("name"))
                + " · " + (user == null ? "" : user.optString("role")), 13, MUTED, false);
        hello.setPadding(0, dp(14), 0, 0);
        c.addView(hello);

        TextView services = text("الخدمات", 18, Color.WHITE, true);
        services.setPadding(0, dp(10), 0, dp(4));
        c.addView(services);

        String[][] items = {
                {"🚗", "قطر السيارات", "مساعدة ونقل السيارة"},
                {"🚛", "قطر الشاحنات", "أعطال ونجدة الشاحنات"},
                {"🔧", "ورشة متنقلة", "مصلح قريب منك"},
                {"⚙️", "قطع الغيار", "محلات قطع الغيار القريبة"}
        };
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(tile(items[r * 2]));
            row.addView(tile(items[r * 2 + 1]));
            c.addView(row);
        }

        if (user != null && "قاطر".equals(user.optString("role"))) {
            c.addView(button("📥 الطلبات المتاحة", BLUE, v -> flow.openTowerRequests()));
        }
        if (flow.activeRequest() != null) {
            c.addView(button("📄 طلبك الحالي", BLUE, v -> flow.showActive()));
        }
        c.addView(button("🗺️ افتح الخريطة وموقعك", SURFACE, v -> mapScreen.open()));

        showNav(c, "home");
    }

    private void emergency() {
        final String[] numbers = {"1055", "17", "14"};
        String[] names = {"🛡️ الدرك الوطني (1055)", "🚓 الشرطة (17)", "🚒 الحماية المدنية (14)", "📍 شارك موقعي مع شخص قريب"};
        new AlertDialog.Builder(this)
                .setTitle("🚨 اتصال طارئ")
                .setItems(names, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        if (which == 3) {
                            flow.x.share("");
                            return;
                        }
                        startActivity(new Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:" + numbers[which])));
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == Flow.PERM) {
            flow.onPermission(results);
        }
    }
}
