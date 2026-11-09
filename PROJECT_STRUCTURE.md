# Estrutura do repositório

## Árvore atual

```text
Shopping-List/
├── .github/
│   └── workflows/
│       ├── android-build.yml
│       └── android-verify.yml
├── android/
│   ├── app/
│   │   ├── build.gradle
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/br/com/shoppinglist/app/
│   │       └── res/
│   ├── gradle/wrapper/gradle-wrapper.properties
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradle.properties
├── CONFIGURAR_APK_E_ATUALIZACOES.md
├── PROJECT_STRUCTURE.md
├── README.md
└── update.json
```

## Código Android

A fonte principal está em `android/`. O módulo `app` contém o código Java, manifesto, recursos gráficos, ícones e configuração de compilação.

Os principais arquivos Java são:
- `MainActivity.java`: telas e interações da lista.
- `AppDatabase.java`: armazenamento local SQLite.
- `ShoppingListRenderer.java`: paginação e renderização para PDF/PNG.
- `EmojiCatalog.java`: catálogo local de emojis.\n- `StickerCatalog.java`: adesivos gráficos locais para produtos sem emoji.\n- `assets/emoji/`: inventário Unicode Emoji 18.0, nomes em português e licença.
- `UpdateConfig.java` e `UpdateManager.java`: consulta e instalação assistida de atualizações.
- `ApkFileProvider.java`: entrega segura do APK ao instalador Android.

A cópia ZIP antiga não faz parte da branch `main` atual. `android/` é a fonte principal do projeto.

## Fluxo de compilação

1. Checkout da branch.
2. Localização da raiz Android em `android/`.
3. Preparação da chave privada por GitHub Actions Secrets.
4. Atualização de `versionCode` e `versionName`.
5. Instalação de JDK, Android SDK e Gradle.
6. Compilação do Release.
7. Verificação do package id, versionCode, SDK mínimo, ABI ARM64, integridade ZIP, assinaturas V1/V2/V3, arquivo `.idsig` V4 e SHA-256.
8. Publicação do APK e da assinatura V4 em uma Release do GitHub.
9. Atualização de `update.json`.

## Segurança da assinatura

A chave antiga do ZIP original foi exposta em commits anteriores do repositório público. O arquivo foi removido da cópia atual, mas o histórico GitHub ainda pode conter versões antigas. Para distribuição segura, use um novo keystore guardado em Actions Secrets. Verifique instalações locais anteriores antes de trocar a identidade de assinatura.

## Validação pendente

O workflow Debug compila e valida o APK de teste ARM64, incluindo integridade ZIP, package id, versionCode e assinatura V2. O APK Release de produção ainda depende dos Secrets de assinatura privada.
