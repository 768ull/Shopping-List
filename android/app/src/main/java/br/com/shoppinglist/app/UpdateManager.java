package br.com.shoppinglist.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Verifica manualmente um manifesto HTTPS e prepara a instalação de atualizações assinadas. */
final class UpdateManager {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final long MAX_APK_BYTES = 150L * 1024L * 1024L;

    interface CheckCallback {
        void onResult(UpdateInfo update, String message);
    }

    interface DownloadCallback {
        void onResult(File apk, String error);
    }

    static final class UpdateInfo {
        final int versionCode;
        final String versionName;
        final String apkUrl;
        final String changelog;
        final String sha256;

        UpdateInfo(int code, String name, String url, String notes, String digest) {
            versionCode = code;
            versionName = name;
            apkUrl = url;
            changelog = notes;
            sha256 = digest;
        }
    }

    private UpdateManager() { }

    static void check(Activity activity, CheckCallback callback) {
        EXECUTOR.execute(() -> {
            UpdateInfo found = null;
            String message = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(UpdateConfig.MANIFEST_URL);
                if (!"https".equalsIgnoreCase(url.getProtocol())) {
                    throw new IllegalStateException("O endereço de atualização precisa usar HTTPS.");
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("User-Agent", "ShoppingList-Android");
                int status = connection.getResponseCode();
                URL finalManifestUrl = connection.getURL();
                if (!"https".equalsIgnoreCase(finalManifestUrl.getProtocol())) {
                    throw new IllegalStateException("A consulta foi redirecionada para um endereço sem HTTPS.");
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException(status == 404
                            ? "A fonte de atualizações ainda não foi publicada."
                            : "Não foi possível consultar as atualizações (HTTP " + status + ").");
                }
                String body;
                try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] chunk = new byte[4096];
                    int read;
                    while ((read = input.read(chunk)) != -1) {
                        if (buffer.size() + read > 1024 * 1024) {
                            throw new IllegalStateException("O manifesto de atualização é grande demais.");
                        }
                        buffer.write(chunk, 0, read);
                    }
                    body = buffer.toString("UTF-8");
                }
                JSONObject json = new JSONObject(body);
                int remoteCode = json.getInt("versionCode");
                if (remoteCode < 1) throw new IllegalStateException("O manifesto contém um versionCode inválido.");
                String remoteName = json.optString("versionName", "").trim();
                if (remoteName.isEmpty()) throw new IllegalStateException("O manifesto não informa versionName.");
                String apkUrl = json.optString("apkUrl", "");
                String notes = json.optString("changelog", "");
                String digest = json.optString("sha256", "").replace(" ", "").toLowerCase(Locale.ROOT);
                if (remoteCode <= installedVersionCode(activity)) {
                    message = "Você já está usando a versão mais recente do Shopping Lists.";
                } else {
                    if (apkUrl.trim().isEmpty()) throw new IllegalStateException("O manifesto não informa o link do APK.");
                    URL downloadUrl = new URL(apkUrl);
                    if (!"https".equalsIgnoreCase(downloadUrl.getProtocol())) {
                        throw new IllegalStateException("O link do APK precisa usar HTTPS.");
                    }
                    if (!digest.matches("[0-9a-f]{64}")) {
                        throw new IllegalStateException("A nova versão precisa informar o SHA-256 do APK no manifesto.");
                    }
                    found = new UpdateInfo(remoteCode, remoteName, apkUrl, notes, digest);
                }
            } catch (Exception e) {
                message = e.getMessage() == null ? "Não foi possível verificar as atualizações." : e.getMessage();
            } finally {
                if (connection != null) connection.disconnect();
            }
            final UpdateInfo result = found;
            final String resultMessage = message;
            MAIN.post(() -> callback.onResult(result, resultMessage));
        });
    }

    static void download(Activity activity, UpdateInfo update, DownloadCallback callback) {
        EXECUTOR.execute(() -> {
            File output = null;
            String error = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(update.apkUrl);
                if (!"https".equalsIgnoreCase(url.getProtocol())) {
                    throw new IllegalStateException("Download bloqueado: o endereço não usa HTTPS.");
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("User-Agent", "ShoppingList-Android");
                int status = connection.getResponseCode();
                URL finalDownloadUrl = connection.getURL();
                if (!"https".equalsIgnoreCase(finalDownloadUrl.getProtocol())) {
                    throw new IllegalStateException("O download foi redirecionado para um endereço sem HTTPS.");
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Falha ao baixar a atualização (HTTP " + status + ").");
                }
                long expected = connection.getContentLength();
                if (expected > MAX_APK_BYTES) throw new IllegalStateException("O APK excede o tamanho permitido.");
                File directory = new File(activity.getCacheDir(), "updates");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("Não foi possível preparar a pasta de atualização.");
                }
                output = new File(directory, "update.apk");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long total = 0;
                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream fileOutput = new FileOutputStream(output, false)) {
                    byte[] chunk = new byte[32 * 1024];
                    int read;
                    while ((read = input.read(chunk)) != -1) {
                        total += read;
                        if (total > MAX_APK_BYTES) throw new IllegalStateException("O APK excede o tamanho permitido.");
                        digest.update(chunk, 0, read);
                        fileOutput.write(chunk, 0, read);
                    }
                    fileOutput.flush();
                }
                if (total == 0 || (expected > 0 && total != expected)) {
                    throw new IllegalStateException("O download do APK ficou incompleto.");
                }
                if (!update.sha256.isEmpty() && !update.sha256.equals(toHex(digest.digest()))) {
                    throw new IllegalStateException("A verificação SHA-256 falhou. O arquivo foi descartado.");
                }
                verifyApk(activity, output, update.versionCode);
            } catch (Exception e) {
                if (output != null && output.exists()) output.delete();
                error = e.getMessage() == null ? "Não foi possível baixar a atualização." : e.getMessage();
                output = null;
            } finally {
                if (connection != null) connection.disconnect();
            }
            final File result = output;
            final String resultError = error;
            MAIN.post(() -> callback.onResult(result, resultError));
        });
    }

    static void install(Activity activity, File apk, int requestCode) throws Exception {
        if (apk == null || !apk.isFile()) throw new IllegalStateException("O arquivo de atualização não foi encontrado.");
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(settings, requestCode);
            return;
        }
        Uri contentUri = Uri.parse("content://" + activity.getPackageName() + ".apkprovider/update.apk");
        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        install.setDataAndType(contentUri, UpdateConfig.APK_MIME_TYPE);
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(install);
    }

    private static int installedVersionCode(Activity activity) throws PackageManager.NameNotFoundException {
        PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
        return Build.VERSION.SDK_INT >= 28 ? (int) info.getLongVersionCode() : info.versionCode;
    }

    private static void verifyApk(Activity activity, File apk, int expectedVersionCode) throws Exception {
        PackageManager pm = activity.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo installed = pm.getPackageInfo(activity.getPackageName(), flags);
        PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        if (candidate == null || !activity.getPackageName().equals(candidate.packageName)) {
            throw new IllegalStateException("O arquivo baixado não é o Shopping Lists.");
        }
        long candidateCode = Build.VERSION.SDK_INT >= 28
                ? candidate.getLongVersionCode() : candidate.versionCode;
        if (candidateCode != expectedVersionCode || candidateCode <= installedVersionCode(activity)) {
            throw new IllegalStateException("A versão do APK não corresponde exatamente ao manifesto ou não é mais recente.");
        }
        android.content.pm.Signature[] installedSignatures;
        android.content.pm.Signature[] candidateSignatures;
        if (Build.VERSION.SDK_INT >= 28) {
            installedSignatures = installed.signingInfo.getApkContentsSigners();
            candidateSignatures = candidate.signingInfo.getApkContentsSigners();
        } else {
            installedSignatures = installed.signatures;
            candidateSignatures = candidate.signatures;
        }
        if (!sameSignatures(installedSignatures, candidateSignatures)) {
            throw new IllegalStateException("A assinatura do APK não corresponde à versão instalada. A atualização foi bloqueada para proteger suas listas.");
        }
    }

    private static boolean sameSignatures(android.content.pm.Signature[] first,
                                          android.content.pm.Signature[] second) {
        if (first == null || second == null || first.length == 0 || first.length != second.length) return false;
        java.util.ArrayList<String> a = new java.util.ArrayList<>();
        java.util.ArrayList<String> b = new java.util.ArrayList<>();
        for (android.content.pm.Signature signature : first) a.add(signature.toCharsString());
        for (android.content.pm.Signature signature : second) b.add(signature.toCharsString());
        java.util.Collections.sort(a);
        java.util.Collections.sort(b);
        return a.equals(b);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }
}
