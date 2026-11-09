# Shopping Lists — projeto Android nativo

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
- `minSdk = 30` (Android 11).
- Java + Views Android nativas, sem WebView ou Jetpack Compose.
- Android Gradle Plugin 9.5.0, Gradle 9.6.0 e JDK 17.
- Android SDK Platform 37.0 e Build Tools 36.0.0 no CI.

O SDK 37 é usado para compilar e direcionar o aplicativo. `minSdk 30` define a versão mínima instalável (Android 11).

## Assinatura APK V1/V2/V3/V4

O `signingConfigs.release` ativa `enableV1Signing`, `enableV2Signing`, `enableV3Signing` e `enableV4Signing`. A assinatura V4 cria o arquivo separado `.idsig`.

**Não existe mais um keystore dentro desta pasta.** A chave de desenvolvimento que acompanhava o ZIP original foi retirada do código atual e da cópia atual do ZIP porque o repositório é público. Ela apareceu em commits anteriores do histórico público e deve ser considerada exposta.

O workflow `Build Shopping Lists APK` recebe a chave privada de distribuição por GitHub Actions Secrets:
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

Para Release, configure a chave privada local ou execute **Actions → Build Shopping Lists APK → Run workflow** após cadastrar os quatro Secrets. O pipeline publica o APK, `.idsig`, SHA-256 e atualiza o manifesto raiz.

## Atualizações integradas

A tela inicial inclui **Verificar atualizações**. A consulta é manual; criar, editar e exportar listas não exige internet.

O manifesto usado pelo aplicativo é:

`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

A verificação exige versão maior, link HTTPS, SHA-256 válido, pacote `br.com.shoppinglist` e o mesmo certificado de assinatura da instalação atual. Depois disso, o aplicativo abre o instalador oficial do Android para a confirmação da pessoa.

O manifesto raiz começa sem APK publicado. Após a primeira publicação, o workflow atualiza `update.json` automaticamente.


## Compilações Debug e atualizações

O artefato do workflow `Verify Shopping List Android` é somente um APK de teste com identificador `br.com.shoppinglist.debug`. Ele agora usa uma chave Debug persistida no cache do GitHub Actions, renovado por um agendamento periódico. A versão-base está em `versionCode 6` /`versionName 1.0.5`.

Não distribua esse artefato como atualização Release. Para atualizações de usuários, use o workflow `Build Shopping Lists APK`, com identificador `br.com.shoppinglist` e a mesma chave privada Release protegida nos Actions Secrets.

O APK de teste é publicado como download direto na release [debug-test](https://github.com/768ull/Shopping-List/releases/tag/debug-test): toque no arquivo `ShoppingList-debug.apk` em **Assets**. Não baixe o ZIP de artefatos das Actions no celular; downloads grandes de artefatos costumam chegar corrompidos e o APK extraído faz o Android exibir "Ocorreu um problema ao analisar o pacote" na instalação.

O primeiro APK Debug entregue antes da criação do cache foi assinado com uma chave temporária do runner, que não foi guardada. Não é possível gerar outra APK que atualize aquela instalação sem a chave privada original; o Android exige que o certificado de assinatura corresponda. A correção do cache evita que esse problema volte a acontecer nas próximas compilações Debug, mas não recupera a primeira chave perdida.

## Baixar pelo aplicativo GitHub no Android

As versões instaláveis são publicadas em [Releases](https://github.com/768ull/Shopping-List/releases). No GitHub Mobile, abra o repositório → **Releases** → escolha a versão → **Assets** → **ShoppingList.apk**. O ZIP de **Code → Download ZIP** é apenas o código-fonte.

Para o APK de teste Debug, abra a release **debug-test** e toque diretamente em **ShoppingList-debug.apk** em **Assets**. Evite o ZIP de artefatos das Actions no celular: o download pode chegar truncado e o Android mostra "Ocorreu um problema ao analisar o pacote" ao instalar o APK extraído.

Cada versão nova fica como uma Release separada e as anteriores continuam disponíveis. A publicação depende de uma execução concluída do workflow **Build Shopping Lists APK** e dos quatro Secrets privados de assinatura indicados acima. Se uma versão antiga nunca teve APK publicado, ela não pode ser baixada retroativamente sem recuperar aquele build.
