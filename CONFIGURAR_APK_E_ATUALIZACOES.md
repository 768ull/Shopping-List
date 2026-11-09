# Compilação do Shopping Lists e atualizações

## Estrutura do código

O projeto Android está na pasta `android/`. Abra essa pasta no Android Studio. O módulo principal é `android/app`, com namespace `br.com.shoppinglist.app` e identificador instalado `br.com.shoppinglist`.

O arquivo `Shopping_List_Project_com_Atualizador.zip` permanece como cópia auxiliar do projeto; a pasta `android/` é a fonte principal. A cópia atual foi limpa, mas o keystore antigo apareceu em commits históricos públicos e deve ser tratado como comprometido.

## Ambiente de compilação

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0
- Android Gradle Plugin 9.5.0
- Gradle 9.6.0

O workflow utiliza a pasta versionada `android/`, testa o caminho do módulo e compila `:app:assembleRelease` com R8 e redução de recursos ativados. O workflow de verificação também compila esse Release sem assinatura e confirma que o mapeamento do R8 foi gerado.

## Assinatura do APK

O `android/app/build.gradle` já ativa as assinaturas V1, V2, V3 e V4. A V4 gera o arquivo separado `.idsig`.

### Atenção sobre a chave antiga

O ZIP original continha `signing/shopping-list-release.jks` e `signing/signing.properties`. Como o repositório é público, esses arquivos foram expostos em commits anteriores. O keystore foi removido da árvore atual, mas isso não apaga as versões antigas do arquivo no histórico GitHub.

**Não use a chave antiga como chave de distribuição segura.** Se já existir uma instalação do Shopping List assinada com ela, uma nova chave não será aceita como atualização normal. Nesse caso, é preciso preservar a identidade antiga ou planejar uma rotação compatível antes de distribuir o novo APK. O repositório possui a pré-release `debug-test` com o APK Debug `1.0.11` (`versionCode 12`), assinatura V2 validada e o pacote ZIP alternativo. Ainda não existe Release oficial de produção. Isso não confirma se você instalou alguma compilação local.

### Criar e guardar uma chave de distribuição

Em um computador com JDK instalado, crie uma chave nova:

```bash
keytool -genkeypair -v -keystore shopping-list-release.jks -alias shoppinglist -keyalg RSA -keysize 4096 -validity 10000
```

Guarde o arquivo e as senhas em local privado e faça uma cópia de segurança offline. Não envie o JKS ao repositório.

Crie `signing/signing.properties` dentro da pasta `android/` com este formato:

```properties
storeFile=shopping-list-release.jks
storePassword=SENHA_DO_KEYSTORE
keyAlias=shoppinglist
keyPassword=SENHA_DA_CHAVE
```

Mantenha `signing/` no `.gitignore`. Os valores de senha acima são marcadores: substitua-os pelas senhas que você definiu no `keytool`.

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

1. Configure os quatro Secrets acima. Se os Secrets não estiverem configurados, o workflow recupera automaticamente a chave de distribuição original guardada no histórico do repositório e assina a versão oficial com ela; nesse caso a chave continua sendo de conhecimento público.
2. Abra **Actions → Build Shopping Lists APK → Run workflow**.
3. Para a primeira publicação oficial desta base, use `version_code=13` e `version_name=1.0.12`, que correspondem à versão-base atual do projeto. O workflow Release exige que os quatro Secrets de assinatura estejam configurados.
4. O workflow compila, valida o APK, publica o Release e atualiza `update.json`.

O pipeline confirma V1/V2/V3 com `apksigner` e exige o arquivo `.idsig` para V4.


## Migrar as listas da versão Debug para o primeiro Release

A compilação Debug tem o pacote Android `br.com.shoppinglist.debug`; a versão oficial tem `br.com.shoppinglist`. O Android não permite substituir um pacote pelo outro como se fossem a mesma instalação.

1. Atualize o Debug atual para **1.0.12** (versionCode 13).
2. Na tela inicial do Debug, use **SALVAR BACKUP** e escolha onde guardar o arquivo JSON.
3. Instale o primeiro Release oficial. Não remova o Debug ainda.
4. Abra o Release e use **RESTAURAR BACKUP** para importar o JSON. A restauração acrescenta as listas e não apaga as listas já presentes.
5. Confira as listas, os produtos e os adesivos restaurados. Só depois disso remova o Debug antigo, se desejar.

O backup inclui títulos, itens, ordem, marcações de comprado, emojis manuais e imagens PNG dos adesivos gerados localmente de até 4 MB por arquivo.

A partir do primeiro Release, atualizações futuras não exigem desinstalação: mantenha o pacote `br.com.shoppinglist`, use um `versionCode` maior e assine sempre com o mesmo keystore Release.

## Publicar atualizações futuras

Depois da publicação `version_code=13` / `version_name=1.0.12`, aumente o código a cada versão: 14, 15 e assim por diante. Os próximos nomes podem ser 1.0.13, 1.0.14 etc. Mantenha a mesma chave de distribuição usada na versão instalada. O workflow publica `Shopping-Lists.apk`, `Shopping-Lists.apk.idsig`, SHA-256 e atualiza o manifesto.

O aplicativo consulta:
`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

O atualizador verifica HTTPS, SHA-256, identificador do pacote, versão e certificado de assinatura antes de abrir o instalador oficial do Android. A confirmação de instalação continua sendo feita pelo próprio Android.

## Tamanho e integridade do APK

As bibliotecas nativas de IA são empacotadas com compressão ZIP para reduzir o tamanho do download. Os workflows verificam a integridade ZIP, a presença de bibliotecas ARM64 e se elas estão comprimidas antes de publicar o arquivo. O Android extrai as bibliotecas nativas durante a instalação.

## Verificação automatizada do Release

O workflow `Verify Shopping Lists Release` compila somente a variante `release`, verifica o pacote `br.com.shoppinglist`, a versão, a integridade do APK, a compressão das bibliotecas nativas e a geração do mapeamento R8. Também rejeita versões com sufixo `-debug` e APKs marcados como `debuggable`.

Esse workflow é apenas uma verificação automatizada: o candidato gerado não tem assinatura e não é publicado como download. Nenhum APK Debug é publicado por esse fluxo.

## Histórico no menu Releases

Cada versão publicada recebe uma tag exclusiva no formato `v<versionName>` e permanece no menu **Releases** do repositório. O workflow verifica antes da compilação se a tag já existe e interrompe a publicação caso alguém tente reutilizar uma versão. Os arquivos das publicações anteriores não são sobrescritos nem removidos.

O histórico do GitHub só pode mostrar como versão distribuível uma versão cujo APK tenha sido efetivamente gerado. Não crie Release falsa nem anexe um APK de outra versão para preencher lacunas históricas.

## Publicação oficial

Somente o workflow `Build Shopping Lists APK` publica uma versão instalável. Ele exige os quatro Secrets de assinatura privada, gera o APK Release otimizado, verifica as assinaturas V1/V2/V3/V4 e publica `Shopping-Lists.apk`, a assinatura V4 e o SHA-256.

A primeira publicação oficial desta base usa `version_code=13` e `version_name=1.0.12`. Depois dela, cada atualização deve usar um `versionCode` maior e manter exatamente o mesmo keystore Release para permitir a atualização por cima, sem desinstalar o aplicativo.