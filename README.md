# Shopping List

## 📦 [VERSÕES E DOWNLOADS — ABRIR NO GITHUB](https://github.com/768ull/Shopping-List/releases)

Acesse esta seção pelo aplicativo GitHub para ver os lançamentos e baixar diretamente os arquivos **.APK** publicados. Use o arquivo APK individual do lançamento — não o ZIP de artefatos nem um arquivo **.AAB**, que é destinado à distribuição pela Play Store.

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

O ZIP continua disponível como cópia do projeto original, mas foi recriado sem a pasta `signing/`, arquivos JKS ou senhas. A fonte principal para manutenção é a pasta `android/`.

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

O workflow `Build Shopping List APK` usa a pasta `android/`, configura a assinatura de distribuição por GitHub Actions Secrets, compila o APK Release e publica o APK, o arquivo de assinatura V4 e o SHA-256 em um GitHub Release.

## Assinatura e atualizações

O aplicativo mantém o identificador `br.com.shoppinglist`. Para que uma atualização seja instalada por cima da versão existente, ela precisa ter um `versionCode` maior e a mesma identidade de assinatura.

O keystore que estava no ZIP original foi removido da versão atual do código e do ZIP público porque o repositório é público. A chave antiga apareceu em commits anteriores do histórico público e deve ser considerada exposta. Não a use como chave de distribuição segura.

Configure os quatro Secrets no GitHub em **Settings → Secrets and variables → Actions**:

- `SHOPPING_KEYSTORE_BASE64`
- `SHOPPING_KEYSTORE_PASSWORD`
- `SHOPPING_KEY_ALIAS`
- `SHOPPING_KEY_PASSWORD`

Antes de trocar a chave, considere se você já instalou uma compilação assinada com a chave anterior. Uma nova chave não atualiza normalmente instalações assinadas com outro certificado. O guia [CONFIGURAR_APK_E_ATUALIZACOES.md](CONFIGURAR_APK_E_ATUALIZACOES.md) explica essa decisão.

O manifesto público é:
`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

## Estado de validação

A migração dos arquivos para `android/` e a limpeza do ZIP foram concluídas pelo GitHub Actions. A compilação Release ainda depende dos Secrets de assinatura válidos e deve ser confirmada por uma execução do workflow de build.


## Assinatura das compilações e atualização

O APK produzido por `android-verify.yml` é somente de teste e usa o identificador `br.com.shoppinglist.debug`. A assinatura Debug foi configurada para persistir no cache do GitHub Actions, com execução agendada para manter o cache ativo. Não use esse artefato como Release.

A versão-base do projeto agora é `versionCode 2` / `1.0.1`. A distribuição aos usuários deve ser feita pelo workflow `Build Shopping List APK`, com o pacote `br.com.shoppinglist` e a mesma chave privada guardada nos quatro Actions Secrets em todas as versões.

**Limitação da primeira instalação Debug:** o primeiro APK Debug enviado em 9 de novembro de 2026 foi assinado com uma chave criada no runner temporário. Ela não foi armazenada. A assinatura dentro do APK contém apenas a chave pública, portanto não é possível reconstruir a chave privada nem assinar um APK de atualização compatível com aquela instalação. O Android bloqueia a substituição por outro certificado. O cache atual estabiliza as próximas compilações, mas não recupera a assinatura daquela primeira compilação.
