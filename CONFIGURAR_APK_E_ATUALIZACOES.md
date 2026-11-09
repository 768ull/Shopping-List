# Compilação do Shopping List e atualizações

## Estrutura do código

O projeto Android está na pasta `android/`. Abra essa pasta no Android Studio. O módulo principal é `android/app`, com namespace `br.com.shoppinglist.app` e identificador instalado `br.com.shoppinglist`.

O ZIP `Shopping_List_Project_com_Atualizador.zip` foi mantido como cópia do projeto original, mas a versão atual dele e a pasta `android/` não incluem keystores nem arquivos de senha.

## Ambiente de compilação

- JDK 17
- Android SDK Platform 37
- Android SDK Build Tools 36.0.0
- Android Gradle Plugin 9.5.0
- Gradle 9.6.0

O workflow utiliza a pasta versionada `android/`, testa o caminho do módulo e compila `:app:assembleRelease`.

## Assinatura do APK

O `android/app/build.gradle` já ativa as assinaturas V1, V2, V3 e V4. A V4 gera o arquivo separado `.idsig`.

### Atenção sobre a chave antiga

O ZIP original continha `signing/shopping-list-release.jks` e `signing/signing.properties`. Como o repositório é público, esses arquivos foram expostos em commits anteriores. A cópia atual do ZIP foi limpa e o keystore foi removido da árvore `android/`, mas a remoção do arquivo atual não apaga automaticamente as versões antigas do histórico GitHub.

**Não use a chave antiga como chave de distribuição segura.** Se já existir uma instalação do Shopping List assinada com ela, uma nova chave não será aceita como atualização normal. Nesse caso, é preciso preservar a identidade antiga ou planejar uma rotação compatível antes de distribuir o novo APK. O repositório ainda não possui GitHub Release publicado, mas isso não confirma se você instalou alguma compilação local.

### Criar e guardar uma chave de distribuição

Em um computador com JDK instalado, crie uma chave nova:

```bash
keytool -genkeypair -v -keystore shopping-list-release.jks -alias shoppinglist -keyalg RSA -keysize 4096 -validity 10000
```

Guarde o arquivo e as senhas em local privado e faça uma cópia de segurança offline. Não envie o JKS ao repositório.

Converta o keystore para Base64. Em Linux/macOS:

```bash
base64 -w 0 shopping-list-release.jks
```

No GitHub, abra **Settings → Secrets and variables → Actions → New repository secret** e crie:

- `SHOPPING_KEYSTORE_BASE64`: conteúdo Base64 do arquivo JKS.
- `SHOPPING_KEYSTORE_PASSWORD`: senha do keystore.
- `SHOPPING_KEY_ALIAS`: alias definido no keytool.
- `SHOPPING_KEY_PASSWORD`: senha da chave.

O workflow decodifica o keystore no diretório temporário `android/signing/`, cria `signing.properties` para a compilação, valida o alias e usa a configuração `signingConfigs.release` já existente no projeto. Esses arquivos temporários não são enviados ao Git.

## Gerar o primeiro APK

1. Configure os quatro Secrets acima.
2. Abra **Actions → Build Shopping List APK → Run workflow**.
3. Para a primeira publicação, use `version_code=1` e `version_name=1.0`, enquanto `apkUrl` estiver vazio no manifesto.
4. O workflow compila, valida o APK, publica o Release e atualiza `update.json`.

O pipeline confirma V1/V2/V3 com `apksigner` e exige o arquivo `.idsig` para V4.

## Publicar atualizações futuras

Aumente `version_code` em cada publicação: 2, 3, 4 etc. Use um nome de versão como 1.1 ou 1.2. Mantenha a mesma chave de distribuição usada na versão instalada. O workflow publica `ShoppingList.apk`, `ShoppingList.apk.idsig`, SHA-256 e atualiza o manifesto.

O aplicativo consulta:
`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

O atualizador verifica HTTPS, SHA-256, identificador do pacote, versão e certificado de assinatura antes de abrir o instalador oficial do Android. A confirmação de instalação continua sendo feita pelo próprio Android.
