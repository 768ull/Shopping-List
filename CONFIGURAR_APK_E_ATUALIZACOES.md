# Compilação do Shopping Lists e atualizações

## Estrutura do código

O projeto Android está na pasta `android/`. Abra essa pasta no Android Studio. O módulo principal é `android/app`, com namespace `br.com.shoppinglist.app` e identificador instalado `br.com.shoppinglist`.

A cópia ZIP antiga não está na branch `main` atual. A pasta `android/` é a fonte principal. O antigo ZIP continha a chave privada Release, que foi exposta no histórico público; essa chave deve ser tratada como comprometida.

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

O ZIP original continha `signing/shopping-list-release.jks` e `signing/signing.properties`. Como o repositório é público, esses arquivos foram expostos em commits anteriores. O keystore foi removido da árvore atual, mas isso não apaga as versões antigas do arquivo no histórico GitHub.

**Não use a chave antiga como chave de distribuição segura.** Se já existir uma instalação do Shopping List assinada com ela, uma nova chave não será aceita como atualização normal. Nesse caso, é preciso preservar a identidade antiga ou planejar uma rotação compatível antes de distribuir o novo APK. O repositório possui a pré-release de teste `debug-test` com o APK `1.0.6`; ainda não possui uma Release oficial de produção. Isso não confirma se você instalou alguma compilação local.

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
2. Abra **Actions → Build Shopping Lists APK → Run workflow**.
3. Para a primeira publicação oficial, use `version_code=7` e `version_name=1.0.6`, que correspondem à versão-base atual do projeto; o manifesto inicial ainda não contém APK oficial publicado.
4. O workflow compila, valida o APK, publica o Release e atualiza `update.json`.

O pipeline confirma V1/V2/V3 com `apksigner` e exige o arquivo `.idsig` para V4.

## Publicar atualizações futuras

Aumente `version_code` em cada publicação: a próxima será 8, depois 9 e assim por diante. Use nomes de versão crescentes, por exemplo 1.0.7 e 1.0.8. Mantenha a mesma chave de distribuição usada na versão instalada. O workflow publica `ShoppingList.apk`, `ShoppingList.apk.idsig`, SHA-256 e atualiza o manifesto.

O aplicativo consulta:
`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

O atualizador verifica HTTPS, SHA-256, identificador do pacote, versão e certificado de assinatura antes de abrir o instalador oficial do Android. A confirmação de instalação continua sendo feita pelo próprio Android.

## Tamanho e integridade do APK

As bibliotecas nativas de IA são empacotadas com compressão ZIP para reduzir o tamanho do download. Os workflows verificam a integridade ZIP, a presença de bibliotecas ARM64 e se elas estão comprimidas antes de publicar o arquivo. O Android extrai as bibliotecas nativas durante a instalação.

## Compilação Debug e assinatura persistente

O workflow `Verify Shopping Lists Android` gera somente um APK de teste com identificador `br.com.shoppinglist.debug`. Ele não deve ser tratado como APK de distribuição. A chave Debug é guardada no cache do GitHub Actions, com builds serializados e execução agendada para manter esse cache ativo. A assinatura Debug atual tem fingerprint SHA-256 `92cc0214682fe7b5c8310703f9dc22143264baf383a95acd8775867db77949a4`. Isso mantém futuras compilações Debug compatíveis enquanto o cache existir.

**Atenção para a instalação Debug feita antes dessa correção:** a primeira compilação Debug (run 42650212939) foi assinada com uma chave aleatória gerada no runner temporário do GitHub, que não foi armazenada. Essa chave privada não está dentro do APK e não pode ser reconstruída a partir do certificado. Portanto, um APK assinado com a nova chave persistente não consegue atualizar aquela instalação. Não existe uma alteração de Gradle ou do APK que contorne a verificação de assinatura do Android.

As atualizações normais distribuídas aos usuários devem usar o workflow Release, o pacote `br.com.shoppinglist` e os quatro Secrets de assinatura privada. Nunca use artefatos Debug como atualizações de Release.
