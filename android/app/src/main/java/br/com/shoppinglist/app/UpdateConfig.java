package br.com.shoppinglist.app;

/**
 * Endpoint público do manifesto de atualização.
 * O repositório precisa publicar update.json na branch main.
 */
final class UpdateConfig {
    private UpdateConfig() { }

    // Repositório reservado para as futuras versões do Shopping List.
    static final String MANIFEST_URL =
            "https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json";

    static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
}
