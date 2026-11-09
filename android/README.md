# Shopping List — projeto Android nativo

Aplicativo Android nativo de lista de compras, com visual amarelo e página de caderno. As listas e os produtos são armazenados localmente em SQLite. A permissão de internet é usada somente para a verificação manual de atualizações.

## Recursos

- Criar várias listas, abrir, fixar/desafixar e excluir.
- Guardar listas e produtos em SQLite local.
- Adicionar e editar produtos e marcar itens comprados.
- Emojis automáticos por palavras comuns em português e seleção manual.
- Exportação PDF multipágina A4 e PNG em alta resolução.
- Verificação manual de atualizações por HTTPS, com validação de SHA-256, versão, pacote e certificado.
- Instalador do Android para confirmar a atualização; não há instalação silenciosa.

## SDK e compatibilidade

- `compileSdk = 37` (Android 17).
- `targetSdk = 37`.
- `minSdk = 21` (Android 5.0).
- Java + Views Android nativas, sem WebView ou Jetpack Compose.
- Android Gradle Plugin 9.5.0, Gradle 9.6.0 e JDK 17.
- Android SDK Platform 37.0 e Build Tools 36.0.0 no CI.

O SDK 37 é usado para compilar e direcionar o aplicativo. `minSdk 21` define a versão mínima instalável.

## Assinatura APK V1/V2/V3/V4

O `signingConfigs.release` ativa `enableV1Signing`, `enableV2Signing`, `enableV3Signing` e `enableV4Signing`. A assinatura V4 cria o arquivo separado `.idsig`.

**Não existe mais um keystore dentro desta pasta.** A chave de desenvolvimento que acompanhava o ZIP original foi retirada do código atual e da cópia atual do ZIP porque o repositório é público. Ela apareceu em commits anteriores do histórico público e deve ser considerada exposta.

O workflow `Build Shopping List APK` recebe a chave privada de distribuição por GitHub Actions Secrets:
- `SHOPPING_KEYSTORE_BASE64`
- `SHOPPING_KEYSTORE_PASSWORD`
- `SHOPPING_KEY_ALIAS`
- `SHOPPING_KEY_PASSWORD`

Para compilação local de Release, crie uma configuração privada em `signing/signing.properties` e mantenha o arquivo JKS fora do Git. Não publique credenciais. Antes de trocar uma chave usada por uma instalação já existente, confirme a identidade de assinatura: uma chave diferente não atualiza normalmente o aplicativo instalado.

## Compilar

Abra a pasta `android/` no Android Studio ou use Gradle 9.6.0:

```bash
gradle --no-daemon -p . :app:assembleDebug
```

O APK Debug ficará em `app/build/outputs/apk/debug/app-debug.apk`.

Para Release, configure a chave privada local ou execute **Actions → Build Shopping List APK → Run workflow** após cadastrar os quatro Secrets. O pipeline publica o APK, `.idsig`, SHA-256 e atualiza o manifesto raiz.

## Atualizações integradas

A tela inicial inclui **Verificar atualizações**. A consulta é manual; criar, editar e exportar listas não exige internet.

O manifesto usado pelo aplicativo é:

`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

A verificação exige versão maior, link HTTPS, SHA-256 válido, pacote `br.com.shoppinglist` e o mesmo certificado de assinatura da instalação atual. Depois disso, o aplicativo abre o instalador oficial do Android para a confirmação da pessoa.

O manifesto raiz começa sem APK publicado. Após a primeira publicação, o workflow atualiza `update.json` automaticamente.
