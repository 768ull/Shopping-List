package br.com.shoppinglist.app;

import android.app.Activity;

/**
 * Endpoint do manifesto de atualização, definido por variante no Gradle.
 * Debug e Release usam feeds separados para evitar instalar pacotes incompatíveis.
 */
final class UpdateConfig {
    private UpdateConfig() { }

    static String manifestUrl(Activity activity) {
        return activity.getString(R.string.update_manifest_url);
    }

    static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
}
