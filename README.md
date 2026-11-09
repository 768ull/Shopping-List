# Shopping Lists

Aplicativo Android nativo para criar e organizar listas de compras. Os dados das listas ficam localmente no aparelho; a internet é utilizada somente quando a pessoa solicita a verificação de atualizações.

## Estrutura do projeto

O código-fonte agora está separado em pastas Android normais, em vez de ficar escondido apenas dentro de um ZIP:

```text
Shopping-List/
├── .github/workflows/
│   ├── android-build.yml
│   └── android-verify.yml
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
├── Shopping_List_Project_com_Atualizador.zip
└── update.json
```

O arquivo `Shopping_List_Project_com_Atualizador.zip` é uma cópia auxiliar do projeto; a fonte principal mantida é a pasta `android/`. O ZIP do código-fonte e o ZIP do projeto não são APKs instaláveis.

## Recursos do aplicativo

- Criar várias listas, abrir, fixar/desafixar e excluir.
- Guardar listas e produtos em SQLite local.
- Adicionar e editar produtos e marcar itens comprados.
- Emojis automáticos por palavras em português e seleção manual.
- Exportação de listas em PDF multipágina ou PNG.
- Verificação manual de atualizações via HTTPS e manifesto `update.json`.

## Compilar localmente

Abra a pasta `android/` no Android Studio. O projeto utiliza Android SDK Platform 37, Android Gradle Plugin 9.5.0, JDK 17 e Gradle 9.6.0.

Para compilar:
- Debug: `:app:assembleDebug`
- Release: `:app:assembleRelease`

O workflow `Build Shopping Lists APK` usa a pasta `android/`, configura a assinatura de distribuição por GitHub Actions Secrets, compila o APK Release e publica o APK, o arquivo de assinatura V4 e o SHA-256 em um GitHub Release.

## Assinatura e atualizações

O aplicativo mantém o identificador `br.com.shoppinglist`. Para que uma atualização seja instalada por cima da versão existente, ela precisa ter um `versionCode` maior e a mesma identidade de assinatura.

O keystore antigo foi removido da árvore atual, mas a chave privada foi incluída no ZIP em commits antigos e continua potencialmente acessível pelo histórico público. Portanto, ela deve ser considerada comprometida e não deve ser usada para uma nova distribuição.

Configure os quatro Secrets no GitHub em **Settings → Secrets and variables → Actions**:

- `SHOPPING_KEYSTORE_BASE64`
- `SHOPPING_KEYSTORE_PASSWORD`
- `SHOPPING_KEY_ALIAS`
- `SHOPPING_KEY_PASSWORD`

Antes de trocar a chave, considere se você já instalou uma compilação assinada com a chave anterior. Uma nova chave não atualiza normalmente instalações assinadas com outro certificado. O guia [CONFIGURAR_APK_E_ATUALIZACOES.md](CONFIGURAR_APK_E_ATUALIZACOES.md) explica essa decisão.

O manifesto público é:
`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

## Estado de validação

A compilação Debug `1.0.6` foi concluída com sucesso no GitHub Actions. O APK passou nas verificações de pacote, versão, arquitetura ARM64, integridade ZIP e assinatura V2. A Release oficial (`br.com.shoppinglist`) ainda depende de uma chave privada de distribuição configurada nos quatro Secrets e não foi publicada.


## Assinatura das compilações e atualização

O APK produzido por `android-verify.yml` é somente de teste e usa o identificador `br.com.shoppinglist.debug`. A assinatura Debug foi configurada para persistir no cache do GitHub Actions, com execução agendada para manter o cache ativo. Não use esse artefato como Release.

A versão-base do projeto agora é `versionCode 7` / `1.0.6`. A distribuição aos usuários deve ser feita pelo workflow `Build Shopping Lists APK`, com o pacote `br.com.shoppinglist` e a mesma chave privada guardada nos quatro Actions Secrets em todas as versões.

**Limitação da primeira instalação Debug:** o primeiro APK Debug enviado em 9 de novembro de 2026 foi assinado com uma chave criada no runner temporário. Ela não foi armazenada. A assinatura dentro do APK contém apenas a chave pública, portanto não é possível reconstruir a chave privada nem assinar um APK de atualização compatível com aquela instalação. O Android bloqueia a substituição por outro certificado. O cache atual estabiliza as próximas compilações, mas não recupera a assinatura daquela primeira compilação.

O APK Debug `1.0.6` está publicado como arquivo direto na release [debug-test](https://github.com/768ull/Shopping-List/releases/tag/debug-test). Baixe `ShoppingList-debug.apk` diretamente em **Assets**. O ZIP de artefatos das Actions serve apenas para inspeção; não extraia e instale a partir dele no celular.

## Baixar o aplicativo pelo GitHub Mobile

As versões instaláveis do Shopping Lists são publicadas na área **Releases** deste repositório. No aplicativo oficial do GitHub para Android, abra este repositório, entre em **Releases**, escolha a versão desejada e toque no arquivo **ShoppingList.apk** em **Assets**. Não use **Code → Download ZIP** para instalar: esse ZIP contém o código-fonte, não o aplicativo.

Para o APK de teste Debug, abra a release **debug-test** e toque diretamente em **ShoppingList-debug.apk** em **Assets**. Evite baixar o ZIP de artefatos das Actions no celular: o download grande pode chegar truncado e o Android mostra "Ocorreu um problema ao analisar o pacote" ao instalar o APK extraído.

- [Ver todas as versões publicadas (Releases)](https://github.com/768ull/Shopping-List/releases)
- [Baixar a versão mais recente](https://github.com/768ull/Shopping-List/releases/latest)

Cada nova publicação mantém as versões anteriores na lista de Releases. Uma versão só aparece ali depois que o workflow **Build Shopping Lists APK** concluir a compilação e publicar o APK. As versões antigas que nunca tiveram um APK publicado não podem ser mostradas como downloads sem que seus respectivos arquivos sejam recuperados e compilados.
