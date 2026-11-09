package br.com.shoppinglist.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Downloads only the small Unicode emoji data file, never an APK.
 * The app keeps the bundled catalog as an offline fallback.
 */
public final class EmojiCatalogUpdater {
    private static final String PREFS = "emoji_catalog_updates";
    private static final long CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    private static final int MAX_MANIFEST_BYTES = 32 * 1024;
    private static final int MAX_CATALOG_BYTES = 3 * 1024 * 1024;
    private static final String BASE_URL =
            "https://raw.githubusercontent.com/768ull/Shopping-List/main/android/app/src/main/assets/emoji/";

    private EmojiCatalogUpdater() { }

    public static String getUnicodeVersion(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("version", EmojiCatalog.UNICODE_VERSION);
    }

    public static int getEmojiCount(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("count", EmojiCatalog.TOTAL_EMOJI_COUNT);
    }

    /** Checks in the background at most twice per day. No dialog or blocking network call. */
    public static void check(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long lastCheck = prefs.getLong("last_check", 0L);
        if (now - lastCheck < CHECK_INTERVAL_MS) return;
        prefs.edit().putLong("last_check", now).apply();

        Thread worker = new Thread(() -> downloadAndApply(app, prefs), "emoji-unicode-updater");
        worker.setPriority(Thread.NORM_PRIORITY - 1);
        worker.start();
    }

    private static void downloadAndApply(Context context, SharedPreferences prefs) {
        try {
            byte[] manifestBytes = download(BASE_URL + "emoji_catalog_manifest.json", MAX_MANIFEST_BYTES);
            JSONObject manifest = new JSONObject(new String(manifestBytes, StandardCharsets.UTF_8));
            String remoteVersion = manifest.optString("unicodeVersion", "");
            String remoteSha = manifest.optString("sha256", "").toLowerCase();
            int remoteCount = manifest.optInt("emojiCount", 0);
            if (remoteVersion.isEmpty() || remoteSha.length() != 64 || remoteCount < 1) return;

            String localVersion = prefs.getString("version", EmojiCatalog.UNICODE_VERSION);
            String localSha = prefs.getString("sha256", "").toLowerCase();
            int versionOrder = compareVersions(remoteVersion, localVersion);
            if (versionOrder < 0 || (versionOrder == 0 && remoteSha.equals(localSha))) return;

            byte[] catalog = download(BASE_URL + "emoji_catalog_pt.tsv", MAX_CATALOG_BYTES);
            if (catalog.length < 1000 || !sha256(catalog).equals(remoteSha)) return;

            int rows = countRows(catalog);
            if (rows != remoteCount) return;

            File destination = new File(context.getFilesDir(), "emoji_catalog_pt.tsv");
            File temporary = new File(context.getFilesDir(), "emoji_catalog_pt.tsv.tmp");
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(catalog);
                output.getFD().sync();
            }
            if (destination.exists() && !destination.delete()) {
                temporary.delete();
                return;
            }
            if (!temporary.renameTo(destination)) {
                temporary.delete();
                return;
            }

            prefs.edit()
                    .putString("version", remoteVersion)
                    .putString("sha256", remoteSha)
                    .putInt("count", rows)
                    .apply();
            EmojiCatalog.invalidate();
        } catch (Exception ignored) {
            // Keep the current offline catalog if the network or server is unavailable.
        }
    }

    private static byte[] download(String address, int maxBytes) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
        connection.setRequestProperty("User-Agent", "ShoppingList-EmojiCatalog/1.0");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + status);
            }
            int length = connection.getContentLength();
            if (length > maxBytes) throw new IllegalStateException("Arquivo remoto acima do limite.");
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    if (output.size() > maxBytes) {
                        throw new IllegalStateException("Arquivo remoto acima do limite.");
                    }
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder result = new StringBuilder(hash.length * 2);
        for (byte value : hash) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }

    private static int countRows(byte[] bytes) {
        String content = new String(bytes, StandardCharsets.UTF_8);
        int count = 0;
        for (String line : content.split("\\n")) {
            int separator = line.indexOf('\t');
            if (separator > 0 && separator < line.length() - 1) count++;
        }
        return count;
    }

    private static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int av = i < a.length ? parsePart(a[i]) : 0;
            int bv = i < b.length ? parsePart(b[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static int parsePart(String value) {
        try { return Integer.parseInt(value.replaceAll("[^0-9].*$", "")); }
        catch (Exception ignored) { return 0; }
    }
}
