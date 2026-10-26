package fr.lite.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal Android TV home screen: clock + a grid of installed apps.
 * No background services, no network, no third-party libraries.
 */
public class MainActivity extends Activity {

    private static final String KEY_ORDER = "order";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_SETTINGS = "#settings";
    private static final String KEY_HIDDEN_TILE = "#hidden";
    private static final String KEY_STORE = "#store";
    private static final String PLAY_STORE = "com.android.vending";

    /** One installed app. */
    private static final class App {
        final ComponentName cn;
        final boolean leanback;
        final String label;
        final Drawable banner;
        final Drawable icon;
        final int tint;

        App(ComponentName cn, boolean leanback, String label, Drawable banner, Drawable icon, int tint) {
            this.cn = cn;
            this.leanback = leanback;
            this.label = label;
            this.banner = banner;
            this.icon = icon;
            this.tint = tint;
        }

        String pkg() {
            return cn.getPackageName();
        }
    }

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout grid;
    private TextView clock;
    private TextView date;

    /** Every launchable app, hidden ones included. */
    private List<App> allApps = new ArrayList<>();
    private String focusedKey;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            long now = System.currentTimeMillis();
            ui.postDelayed(this, 60_000 - now % 60_000 + 50);
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        scroll = findViewById(R.id.scroll);
        grid = findViewById(R.id.grid);
        clock = findViewById(R.id.clock);
        date = findViewById(R.id.date);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, f, RECEIVER_EXPORTED);
        else registerReceiver(packageReceiver, f);

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.removeCallbacks(tick);
        tick.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packageReceiver);
        io.shutdownNow();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home: jump back to the top.
        scroll.smoothScrollTo(0, 0);
        focusFirst();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // A launcher has nowhere to go back to.
    }

    // ---------------------------------------------------------------- clock

    private void updateClock() {
        Date now = new Date();
        String pattern = DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm";
        clock.setText(new SimpleDateFormat(pattern, Locale.getDefault()).format(now));
        String d = new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now);
        date.setText(d.isEmpty() ? d : Character.toUpperCase(d.charAt(0)) + d.substring(1));
    }

    // ---------------------------------------------------------------- apps

    private void loadApps() {
        io.execute(() -> {
            PackageManager pm = getPackageManager();
            Map<String, App> byPkg = new LinkedHashMap<>();
            collect(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, true, byPkg);
            collect(pm, Intent.CATEGORY_LAUNCHER, false, byPkg);
            List<App> list = new ArrayList<>(byPkg.values());
            ui.post(() -> {
                allApps = list;
                render();
            });
        });
    }

    private void collect(PackageManager pm, String category, boolean leanback, Map<String, App> out) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(category);
        for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
            ActivityInfo ai = ri.activityInfo;
            if (ai.packageName.equals(getPackageName()) || out.containsKey(ai.packageName)) continue;
            Drawable banner = leanback ? ai.loadBanner(pm) : null;
            Drawable icon = ri.loadIcon(pm);
            out.put(ai.packageName, new App(
                    new ComponentName(ai.packageName, ai.name),
                    leanback,
                    ri.loadLabel(pm).toString(),
                    banner,
                    icon,
                    banner == null ? tintOf(icon) : 0));
        }
    }

    /** Visible apps in user order; unordered apps follow alphabetically. */
    private List<App> visibleApps() {
        Set<String> hidden = hidden();
        List<String> order = order();
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < order.size(); i++) idx.put(order.get(i), i);

        List<App> out = new ArrayList<>();
        for (App a : allApps) if (!hidden.contains(a.pkg())) out.add(a);
        out.sort((a, b) -> {
            Integer ia = idx.get(a.pkg()), ib = idx.get(b.pkg());
            if (ia != null && ib != null) return ia - ib;
            if (ia != null) return -1;
            if (ib != null) return 1;
            return a.label.compareToIgnoreCase(b.label);
        });
        return out;
    }

    private void launch(App a) {
        Intent i = new Intent(Intent.ACTION_MAIN)
                .addCategory(a.leanback ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER)
                .setComponent(a.cn)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir " + a.label, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasApp(String pkg) {
        for (App a : allApps) if (a.pkg().equals(pkg)) return true;
        return false;
    }

    private boolean isInstalled(String pkg) {
        try {
            return getPackageManager().getApplicationInfo(pkg, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Google TV hides the Play Store icon, so try its launch intents, then a market link. */
    private void openPlayStore() {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(PLAY_STORE);
        if (i == null) i = pm.getLaunchIntentForPackage(PLAY_STORE);
        if (i == null) {
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=apps")).setPackage(PLAY_STORE);
        }
        startSafely(i);
    }

    // ---------------------------------------------------------------- UI

    private void render() {
        grid.removeAllViews();
        int tileW = getResources().getDimensionPixelSize(R.dimen.tile_w);
        int gap = getResources().getDimensionPixelSize(R.dimen.gap);
        int pad = getResources().getDimensionPixelSize(R.dimen.pad);
        int width = getResources().getDisplayMetrics().widthPixels - 2 * pad;
        int cols = Math.max(3, (width + gap) / (tileW + gap));

        List<View> tiles = new ArrayList<>();
        for (App a : visibleApps()) {
            tiles.add(makeTile(a.pkg(), a.label, a.banner, a.icon, a.tint,
                    () -> launch(a), () -> showAppMenu(a)));
        }
        if (!hasApp(PLAY_STORE) && isInstalled(PLAY_STORE)) {
            tiles.add(makeTile(KEY_STORE, "Play Store", null, getDrawable(R.drawable.ic_store), 0,
                    this::openPlayStore, null));
        }
        tiles.add(makeTile(KEY_SETTINGS,"Paramètres", null, getDrawable(R.drawable.ic_settings), 0,
                () -> startSafely(new Intent(Settings.ACTION_SETTINGS)), null));
        if (!hidden().isEmpty()) {
            tiles.add(makeTile(KEY_HIDDEN_TILE, "Apps masquées", null, getDrawable(R.drawable.ic_eye), 0,
                    this::showHiddenMenu, null));
        }

        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rp.bottomMargin = gap + gap / 2;
                grid.addView(row, rp);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    tileW, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i % cols != cols - 1) lp.setMarginEnd(gap);
            row.addView(tiles.get(i), lp);
        }

        View target = focusedKey == null ? null : grid.findViewWithTag(focusedKey);
        if (target != null) target.requestFocus();
        else focusFirst();
    }

    private View makeTile(String key, String label, Drawable banner, Drawable icon, int tint,
                          Runnable onClick, Runnable onLongClick) {
        View v = getLayoutInflater().inflate(R.layout.tile, grid, false);
        FrameLayout card = v.findViewById(R.id.card);
        ImageView bannerView = v.findViewById(R.id.banner);
        ImageView iconView = v.findViewById(R.id.icon);
        TextView labelView = v.findViewById(R.id.label);

        card.setTag(key);
        card.setClipToOutline(true);
        card.setContentDescription(label);
        labelView.setText(label);

        if (banner != null) {
            bannerView.setImageDrawable(banner);
            iconView.setVisibility(View.GONE);
        } else {
            bannerView.setVisibility(View.GONE);
            iconView.setImageDrawable(icon);
            if (tint != 0) card.setBackgroundTintList(ColorStateList.valueOf(tint));
        }

        float lift = 10 * getResources().getDisplayMetrics().density;
        DecelerateInterpolator ease = new DecelerateInterpolator(2f);
        card.setOnFocusChangeListener((view, has) -> {
            float s = has ? 1.1f : 1f;
            view.animate().scaleX(s).scaleY(s).translationZ(has ? lift : 0)
                    .setDuration(160).setInterpolator(ease).start();
            labelView.animate().alpha(has ? 1f : 0.55f).setDuration(160).start();
            if (has) focusedKey = key;
        });
        card.setOnClickListener(x -> onClick.run());
        if (onLongClick != null) {
            card.setOnLongClickListener(x -> {
                onLongClick.run();
                return true;
            });
        }
        return v;
    }

    private void focusFirst() {
        if (grid.getChildCount() == 0) return;
        View first = grid.findViewById(R.id.card);
        if (first != null) first.requestFocus();
    }

    // ---------------------------------------------------------------- menus

    private void showAppMenu(App a) {
        String[] items = {
                "Déplacer en premier",
                "Déplacer à gauche",
                "Déplacer à droite",
                "Masquer",
                "Infos de l'application"
        };
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(a.label)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: move(a.pkg(), Integer.MIN_VALUE); break;
                        case 1: move(a.pkg(), -1); break;
                        case 2: move(a.pkg(), 1); break;
                        case 3: setHidden(a.pkg(), true); break;
                        case 4: startSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", a.pkg(), null))); break;
                    }
                })
                .show();
    }

    private void showHiddenMenu() {
        Set<String> hidden = hidden();
        List<App> list = new ArrayList<>();
        for (App a : allApps) if (hidden.contains(a.pkg())) list.add(a);
        if (list.isEmpty()) return;
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = "Afficher " + list.get(i).label;
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Apps masquées")
                .setItems(labels, (d, which) -> setHidden(list.get(which).pkg(), false))
                .show();
    }

    private void startSafely(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, "Action indisponible", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------- prefs

    private List<String> order() {
        String s = prefs.getString(KEY_ORDER, "");
        return s.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(s.split("\\|")));
    }

    private Set<String> hidden() {
        return new HashSet<>(prefs.getStringSet(KEY_HIDDEN, new HashSet<>()));
    }

    /** delta = Integer.MIN_VALUE moves the app to the front. */
    private void move(String pkg, int delta) {
        List<String> pkgs = new ArrayList<>();
        for (App a : visibleApps()) pkgs.add(a.pkg());
        int from = pkgs.indexOf(pkg);
        if (from < 0) return;
        int to = delta == Integer.MIN_VALUE ? 0 : Math.max(0, Math.min(pkgs.size() - 1, from + delta));
        pkgs.remove(from);
        pkgs.add(to, pkg);
        prefs.edit().putString(KEY_ORDER, String.join("|", pkgs)).apply();
        focusedKey = pkg;
        render();
    }

    private void setHidden(String pkg, boolean hide) {
        Set<String> h = hidden();
        if (hide) h.add(pkg);
        else h.remove(pkg);
        prefs.edit().putStringSet(KEY_HIDDEN, h).apply();
        if (hide && pkg.equals(focusedKey)) focusedKey = null;
        if (!hide) focusedKey = pkg;
        render();
    }

    // ---------------------------------------------------------------- util

    /** Muted, dark card color derived from the app icon. */
    private static int tintOf(Drawable icon) {
        if (icon == null) return 0;
        Drawable d = icon.getConstantState() != null ? icon.getConstantState().newDrawable().mutate() : icon;
        Bitmap b = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, 8, 8);
        d.draw(new Canvas(b));
        long r = 0, g = 0, bl = 0;
        int n = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int p = b.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                r += Color.red(p);
                g += Color.green(p);
                bl += Color.blue(p);
                n++;
            }
        }
        b.recycle();
        if (n == 0) return 0;
        float[] hsv = new float[3];
        Color.colorToHSV(Color.rgb((int) (r / n), (int) (g / n), (int) (bl / n)), hsv);
        hsv[1] = Math.min(hsv[1], 0.55f);
        hsv[2] = 0.26f;
        return Color.HSVToColor(hsv);
    }
}
