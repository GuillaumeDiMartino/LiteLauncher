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
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
 * Minimal Android TV home screen: greeting, clock, and sections of tiles
 * (favorites, recents, all apps, quick settings).
 * No background services, no network, no third-party libraries.
 */
public class MainActivity extends Activity {

    private static final String KEY_ORDER = "order";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_FAV = "fav";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_SIZE = "size";
    private static final String KEY_RECENTS = "recents";
    private static final String PLAY_STORE = "com.android.vending";
    private static final int MAX_RECENTS = 5;

    /** Accent palettes: gradient start, gradient end (also the glow), focus ring. */
    private static final String[] ACCENT_NAMES = {"Corail", "Cyan", "Violet"};
    private static final int[][] ACCENTS = {
            {0xFFF6B27A, 0xFFE8687A, 0xFFF5A97F},
            {0xFF8BE9F5, 0xFF3F9BF5, 0xFF8BE0F5},
            {0xFFD4A5FF, 0xFF8A5CF6, 0xFFC9A2FF},
    };

    /** Tile sizes in dp (width, height). */
    private static final String[] SIZE_NAMES = {"Petites", "Normales", "Grandes"};
    private static final int[][] SIZES = {{128, 72}, {150, 84}, {184, 104}};
    private static final int[] FAV_SIZE = {196, 110};
    private static final int[] QUICK_SIZE = {90, 56};

    /** One installed app. */
    private static final class App {
        final ComponentName cn;
        final boolean leanback;
        final String label;
        final Drawable banner;
        final Drawable icon;

        App(ComponentName cn, boolean leanback, String label, Drawable banner, Drawable icon) {
            this.cn = cn;
            this.leanback = leanback;
            this.label = label;
            this.banner = banner;
            this.icon = icon;
        }

        String pkg() {
            return cn.getPackageName();
        }
    }

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout sections;
    private TextView greeting;
    private TextView clock;
    private TextView date;
    private TextView toast;
    private float density;

    /** Every launchable app, hidden ones included. */
    private List<App> allApps = new ArrayList<>();
    /** Apps opened from this launcher, most recent first (unfiltered). */
    private List<String> recentPkgs = new ArrayList<>();
    /** Set when an app was launched; the Récents row is rebuilt on return, not while launching. */
    private boolean recentsDirty;
    private String focusedKey;
    /** First row of tiles; focusing it scrolls back to the very top. */
    private View firstRow;

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
        String r = prefs.getString(KEY_RECENTS, "");
        if (!r.isEmpty()) recentPkgs = new ArrayList<>(Arrays.asList(r.split("\\|")));
        density = getResources().getDisplayMetrics().density;
        scroll = findViewById(R.id.scroll);
        sections = findViewById(R.id.sections);
        greeting = findViewById(R.id.greeting);
        clock = findViewById(R.id.clock);
        date = findViewById(R.id.date);
        toast = findViewById(R.id.toast);
        applyAccent();

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, f, RECEIVER_EXPORTED);
        else registerReceiver(packageReceiver, f);

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.removeCallbacks(tick);
        tick.run();
        if (recentsDirty) {
            recentsDirty = false;
            render();
        }
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

        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(h >= 5 && h < 18 ? "Bonjour" : h < 23 && h >= 18 ? "Bonsoir" : "Bonne nuit");
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
            // Largest slots a banner or icon is shown in: favorites tile, 48dp icon.
            Drawable banner = leanback ? fit(ai.loadBanner(pm), FAV_SIZE[0], FAV_SIZE[1], true) : null;
            out.put(ai.packageName, new App(
                    new ComponentName(ai.packageName, ai.name),
                    leanback,
                    ri.loadLabel(pm).toString(),
                    banner,
                    banner == null ? fit(ri.loadIcon(pm), 48, 48, false) : null));
        }
    }

    /**
     * Renders app art once into a bitmap no bigger than its largest tile, so we never
     * keep full-resolution banners or icons in memory. crop = centerCrop, else fitCenter.
     */
    private Drawable fit(Drawable d, int wDp, int hDp, boolean crop) {
        if (d == null) return null;
        int w = dp(wDp), h = dp(hDp);
        int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
        if (iw > 0 && ih > 0 && iw <= w && ih <= h) return d;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        if (iw > 0 && ih > 0) {
            float s = crop ? Math.max((float) w / iw, (float) h / ih) : Math.min((float) w / iw, (float) h / ih);
            int sw = Math.round(iw * s), sh = Math.round(ih * s);
            int left = (w - sw) / 2, top = (h - sh) / 2;
            d.setBounds(left, top, left + sw, top + sh);
        } else {
            d.setBounds(0, 0, w, h);
        }
        d.draw(new Canvas(b));
        return new BitmapDrawable(getResources(), b);
    }

    /** Visible apps in user order; unordered apps follow alphabetically. */
    private List<App> visibleApps() {
        Set<String> hidden = stringSet(KEY_HIDDEN);
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
            toast("Impossible d'ouvrir " + a.label);
            return;
        }
        rememberRecent(a.pkg());
    }

    /**
     * Récents = apps opened from this launcher. No usage-stats permission (Google TV returns
     * nothing to third-party launchers anyway), no system queries: one pref write per launch.
     */
    private void rememberRecent(String pkg) {
        List<String> r = new ArrayList<>(recentPkgs);
        r.remove(pkg);
        r.add(0, pkg);
        while (r.size() > MAX_RECENTS * 2) r.remove(r.size() - 1);
        if (r.equals(recentPkgs)) return;
        recentPkgs = r;
        prefs.edit().putString(KEY_RECENTS, String.join("|", r)).apply();
        recentsDirty = true;
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
        sections.removeAllViews();
        firstRow = null;
        int[] size = SIZES[sizeIndex()];
        Set<String> fav = stringSet(KEY_FAV);

        List<App> favApps = new ArrayList<>();
        List<App> rest = new ArrayList<>();
        Map<String, App> visibleByPkg = new HashMap<>();
        for (App a : visibleApps()) {
            (fav.contains(a.pkg()) ? favApps : rest).add(a);
            visibleByPkg.put(a.pkg(), a);
        }

        List<App> recents = new ArrayList<>();
        int maxRecents = Math.min(MAX_RECENTS, columns(size[0]));
        for (String pkg : recentPkgs) {
            App a = visibleByPkg.get(pkg);
            if (a != null) recents.add(a);
            if (recents.size() == maxRecents) break;
        }

        addSection("FAVORIS", appTiles(favApps, "", FAV_SIZE), FAV_SIZE);
        addSection("RÉCENTS", appTiles(recents, "r:", size), size);
        addSection("APPLICATIONS", appTiles(rest, "", size), size);
        addSection("RÉGLAGES RAPIDES", quickTiles(), QUICK_SIZE);

        View target = focusedKey == null ? null : sections.findViewWithTag(focusedKey);
        if (target != null) target.requestFocus();
        else focusFirst();
    }

    private List<View> appTiles(List<App> apps, String keyPrefix, int[] size) {
        List<View> tiles = new ArrayList<>();
        for (App a : apps) {
            tiles.add(makeTile(keyPrefix + a.pkg(), a.label, a.banner, a.icon, 0, size,
                    () -> launch(a), () -> showAppMenu(a)));
        }
        return tiles;
    }

    private List<View> quickTiles() {
        int lavender = getColor(R.color.lavender);
        List<View> t = new ArrayList<>();
        t.add(quick("#wifi", "Wi-Fi", R.drawable.ic_wifi, lavender, Settings.ACTION_WIFI_SETTINGS));
        t.add(quick("#bt", "Bluetooth", R.drawable.ic_bluetooth, lavender, Settings.ACTION_BLUETOOTH_SETTINGS));
        t.add(quick("#display", "Affichage", R.drawable.ic_display, lavender, Settings.ACTION_DISPLAY_SETTINGS));
        t.add(quick("#dream", "Veille", R.drawable.ic_moon, lavender, Settings.ACTION_DREAM_SETTINGS));
        if (!hasApp(PLAY_STORE) && isInstalled(PLAY_STORE)) {
            t.add(makeTile("#store", "Play Store", null, getDrawable(R.drawable.ic_store), lavender,
                    QUICK_SIZE, this::openPlayStore, null));
        }
        t.add(quick("#settings", "Paramètres", R.drawable.ic_settings, lavender, Settings.ACTION_SETTINGS));
        t.add(makeTile("#custom", "Style", null, getDrawable(R.drawable.ic_palette), lavender,
                QUICK_SIZE, this::showStyleMenu, null));
        if (!stringSet(KEY_HIDDEN).isEmpty()) {
            t.add(makeTile("#hidden", "Masquées", null, getDrawable(R.drawable.ic_eye), lavender,
                    QUICK_SIZE, this::showHiddenMenu, null));
        }
        return t;
    }

    private View quick(String key, String label, int icon, int tint, String action) {
        return makeTile(key, label, null, getDrawable(icon), tint, QUICK_SIZE,
                () -> startSafely(new Intent(action)), null);
    }

    /** Section title + tiles wrapped into rows. Empty sections are skipped. */
    private void addSection(String title, List<View> tiles, int[] sizeDp) {
        if (tiles.isEmpty()) return;
        boolean first = sections.getChildCount() == 0;

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(0xFF9C98E8);
        label.setTextSize(12);
        label.setLetterSpacing(0.22f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(first ? 30 : 4);
        lp.bottomMargin = dp(16);
        sections.addView(label, lp);

        int gap = getResources().getDimensionPixelSize(R.dimen.gap);
        int cols = columns(sizeDp[0]);
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.bottomMargin = gap + gap / 2;
                sections.addView(row, rp);
                if (firstRow == null) firstRow = row;
            }
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    dp(sizeDp[0]), ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i % cols != cols - 1) tp.setMarginEnd(gap);
            row.addView(tiles.get(i), tp);
        }
    }

    private int columns(int tileWdp) {
        int gap = getResources().getDimensionPixelSize(R.dimen.gap);
        int pad = getResources().getDimensionPixelSize(R.dimen.pad);
        int width = getResources().getDisplayMetrics().widthPixels - 2 * pad;
        return Math.max(1, (width + gap) / (dp(tileWdp) + gap));
    }

    /** iconTint = 0 keeps the icon's own colors. */
    private View makeTile(String key, String label, Drawable banner, Drawable icon, int iconTint, int[] sizeDp,
                          Runnable onClick, Runnable onLongClick) {
        View v = getLayoutInflater().inflate(R.layout.tile, sections, false);
        FrameLayout card = v.findViewById(R.id.card);
        ImageView bannerView = v.findViewById(R.id.banner);
        ImageView iconView = v.findViewById(R.id.icon);
        TextView labelView = v.findViewById(R.id.label);
        int[] accent = accent();

        card.getLayoutParams().height = dp(sizeDp[1]);
        int iconSize = dp(Math.min(48, Math.round(sizeDp[1] * 0.55f)));
        iconView.getLayoutParams().width = iconSize;
        iconView.getLayoutParams().height = iconSize;
        if (sizeDp == QUICK_SIZE) labelView.setTextSize(12);

        card.setTag(key);
        card.setClipToOutline(true);
        card.setForeground(focusRing(accent[2]));
        // Glow under the focused card: a hardware shadow, no blur pass.
        card.setOutlineSpotShadowColor(accent[1]);
        card.setOutlineAmbientShadowColor(accent[1]);
        card.setContentDescription(label);
        labelView.setText(label);

        if (banner != null) {
            bannerView.setImageDrawable(banner);
            iconView.setVisibility(View.GONE);
        } else {
            bannerView.setVisibility(View.GONE);
            iconView.setImageDrawable(icon);
            if (iconTint != 0) iconView.setImageTintList(ColorStateList.valueOf(iconTint));
        }

        float lift = 14 * density;
        DecelerateInterpolator ease = new DecelerateInterpolator(2f);
        card.setOnFocusChangeListener((view, has) -> {
            float s = has ? 1.08f : 1f;
            view.animate().scaleX(s).scaleY(s).translationZ(has ? lift : 0)
                    .setDuration(160).setInterpolator(ease).start();
            labelView.animate().alpha(has ? 1f : 0.6f).setDuration(160).start();
            if (has) {
                focusedKey = key;
                // ScrollView only scrolls enough to show the tile; on the first row,
                // go all the way up so the greeting and clock come back into view.
                if (view.getParent().getParent() == firstRow) scroll.post(() -> scroll.smoothScrollTo(0, 0));
            }
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
        View first = sections.findViewById(R.id.card);
        if (first != null) first.requestFocus();
    }

    // ---------------------------------------------------------------- accent

    private int[] accent() {
        return ACCENTS[Math.max(0, Math.min(ACCENTS.length - 1, prefs.getInt(KEY_ACCENT, 0)))];
    }

    private int sizeIndex() {
        return Math.max(0, Math.min(SIZES.length - 1, prefs.getInt(KEY_SIZE, 1)));
    }

    private GradientDrawable gradient(int[] accent, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{accent[0], accent[1]});
        g.setCornerRadius(radiusDp * density);
        return g;
    }

    private Drawable focusRing(int color) {
        GradientDrawable on = new GradientDrawable();
        on.setCornerRadius(16 * density);
        on.setStroke(dp(3), color);
        GradientDrawable off = new GradientDrawable();
        off.setCornerRadius(16 * density);
        off.setStroke(dp(1), 0x12FFFFFF);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, on);
        s.addState(new int[0], off);
        return s;
    }

    private void applyAccent() {
        int[] a = accent();
        date.setBackground(gradient(a, 999));
        toast.setBackground(gradient(a, 999));
        date.setOutlineSpotShadowColor(a[1]);
        toast.setOutlineSpotShadowColor(a[1]);
    }

    // ---------------------------------------------------------------- menus

    /** Shows a dialog whose focused row uses the accent gradient. */
    private void show(AlertDialog.Builder b) {
        AlertDialog d = b.show();
        ListView list = d.getListView();
        if (list == null) return;
        StateListDrawable sel = new StateListDrawable();
        sel.addState(new int[]{android.R.attr.state_focused},
                new InsetDrawable(gradient(accent(), 12), dp(10), 0, dp(10), 0));
        sel.addState(new int[0], new ColorDrawable(Color.TRANSPARENT));
        list.setSelector(sel);
    }

    private AlertDialog.Builder dialog(String title) {
        return new AlertDialog.Builder(this, R.style.Theme_Lite_Dialog).setTitle(title);
    }

    private void showAppMenu(App a) {
        boolean isFav = stringSet(KEY_FAV).contains(a.pkg());
        String[] items = {
                isFav ? "Retirer des favoris" : "Ajouter aux favoris",
                "Déplacer en premier",
                "Déplacer à gauche",
                "Déplacer à droite",
                "Masquer",
                "Infos de l'application",
                "Désinstaller"
        };
        show(dialog(a.label).setItems(items, (d, which) -> {
            switch (which) {
                case 0: toggleSet(KEY_FAV, a.pkg(), !isFav); break;
                case 1: move(a.pkg(), Integer.MIN_VALUE); break;
                case 2: move(a.pkg(), -1); break;
                case 3: move(a.pkg(), 1); break;
                case 4: toggleSet(KEY_HIDDEN, a.pkg(), true); break;
                case 5: startSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", a.pkg(), null))); break;
                case 6: startSafely(new Intent(Intent.ACTION_DELETE,
                        Uri.fromParts("package", a.pkg(), null))); break;
            }
        }));
    }

    private void showHiddenMenu() {
        Set<String> hidden = stringSet(KEY_HIDDEN);
        List<App> list = new ArrayList<>();
        for (App a : allApps) if (hidden.contains(a.pkg())) list.add(a);
        if (list.isEmpty()) return;
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = "Afficher " + list.get(i).label;
        show(dialog("Apps masquées")
                .setItems(labels, (d, which) -> toggleSet(KEY_HIDDEN, list.get(which).pkg(), false)));
    }

    private void showStyleMenu() {
        String[] items = {
                "Couleur : " + ACCENT_NAMES[Arrays.asList(ACCENTS).indexOf(accent())],
                "Taille des tuiles : " + SIZE_NAMES[sizeIndex()]
        };
        show(dialog("Style").setItems(items, (d, which) -> {
            if (which == 0) {
                show(dialog("Couleur").setSingleChoiceItems(ACCENT_NAMES, prefs.getInt(KEY_ACCENT, 0), (dd, i) -> {
                    prefs.edit().putInt(KEY_ACCENT, i).apply();
                    dd.dismiss();
                    applyAccent();
                    render();
                }));
            } else {
                show(dialog("Taille des tuiles").setSingleChoiceItems(SIZE_NAMES, sizeIndex(), (dd, i) -> {
                    prefs.edit().putInt(KEY_SIZE, i).apply();
                    dd.dismiss();
                    render();
                }));
            }
        }));
    }

    private final Runnable hideToast = () -> toast.animate().alpha(0f).setDuration(200)
            .withEndAction(() -> toast.setVisibility(View.GONE)).start();

    /** Accent pill message drawn in our own window (system toasts can't be styled). */
    private void toast(String msg) {
        toast.setText(msg);
        toast.setVisibility(View.VISIBLE);
        toast.animate().cancel();
        toast.animate().alpha(1f).setDuration(150).start();
        ui.removeCallbacks(hideToast);
        ui.postDelayed(hideToast, 1800);
    }

    private void startSafely(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            toast("Action indisponible");
        }
    }

    // ---------------------------------------------------------------- prefs

    private List<String> order() {
        String s = prefs.getString(KEY_ORDER, "");
        return s.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(s.split("\\|")));
    }

    private Set<String> stringSet(String key) {
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));
    }

    private void toggleSet(String key, String pkg, boolean add) {
        Set<String> s = stringSet(key);
        if (add) s.add(pkg);
        else s.remove(pkg);
        prefs.edit().putStringSet(key, s).apply();
        focusedKey = KEY_HIDDEN.equals(key) && add ? null : pkg;
        render();
    }

    /**
     * Moves an app within its own section (favorites or apps).
     * delta = Integer.MIN_VALUE moves it to the front.
     */
    private void move(String pkg, int delta) {
        Set<String> fav = stringSet(KEY_FAV);
        boolean inFav = fav.contains(pkg);
        List<String> section = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (App a : visibleApps()) (fav.contains(a.pkg()) == inFav ? section : other).add(a.pkg());

        int from = section.indexOf(pkg);
        if (from < 0) return;
        int to = delta == Integer.MIN_VALUE ? 0 : Math.max(0, Math.min(section.size() - 1, from + delta));
        section.remove(from);
        section.add(to, pkg);

        List<String> all = new ArrayList<>(inFav ? section : other);
        all.addAll(inFav ? other : section);
        prefs.edit().putString(KEY_ORDER, String.join("|", all)).apply();
        focusedKey = pkg;
        render();
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
