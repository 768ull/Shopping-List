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
├── update.json
└── update-debug.json
```

O arquivo `Shopping_List_Project_com_Atualizador.zip` é uma cópia auxiliar do projeto; a fonte principal mantida é a pasta `android/`. O ZIP do código-fonte e o ZIP do projeto não são APKs instaláveis.

## Recursos do aplicativo

- Criar várias listas, abrir, fixar/desafixar e excluir.
- Guardar listas e produtos em SQLite local.
- Adicionar e editar produtos e marcar itens comprados.
- Emojis automáticos por palavras em português e seleção manual.
- Exportação de listas em PDF multipágina ou PNG.\n- Backup e restauração em JSON para migrar listas, produtos, marcações, emojis manuais e imagens de adesivos gerados entre instalações.
- Verificação manual de atualizações via HTTPS e manifesto `update.json`.
- Catálogo de **436 emojis de produtos** (Unicode filtrado), sem rostos, gestos, sinais ou animais usados como substitutos de mercadorias.
- Bloqueio de pictogramas genéricos (🛍️, 🛒 e 🧠), reconhecimento local ampliado para produtos e marcas brasileiras de bebidas e alimentos, e indicador visual da IA local enquanto identifica ou cria um adesivo.
- Compatibilidade com emojis inseridos por qualquer teclado Android, virtual, físico ou Bluetooth; emojis usados são salvos automaticamente na coleção pessoal da aba Emojis, sem depender do catálogo curado.
- Atualização automática do catálogo filtrado via GitHub Actions e verificação em segundo plano no aplicativo.

## Compilar localmente

Abra a pasta `android/` no Android Studio. O projeto utiliza Android SDK Platform 36, Android Gradle Plugin 9.5.0, JDK 17 e Gradle 9.6.0.

Para compilar:
- Debug: `:app:assembleDebug`
- Release: `:app:assembleRelease`

O workflow `Build Shopping Lists APK` usa a pasta `android/`, configura a assinatura de distribuição por GitHub Actions Secrets, compila o APK Release otimizado com R8 e redução de recursos e publica o APK, o arquivo de assinatura V4 e o SHA-256 em um GitHub Release. O workflow de verificação também compila um Release otimizado sem assinatura para testar o R8 antes da distribuição.

## Assinatura e atualizações

O aplicativo mantém o identificador `br.com.shoppinglist`. Para que uma atualização seja instalada por cima da versão existente, ela precisa ter um `versionCode` maior e a mesma identidade de assinatura.

O keystore antigo foi removido da árvore atual, mas a chave privada foi incluída no ZIP em commits antigos e continua potencialmente acessível pelo histórico público. Portanto, ela deve ser considerada comprometida e não deve ser usada para uma nova distribuição.

Configure os quatro Secrets no GitHub em **Settings → Secrets and variables → Actions**:

- `SHOPPING_KEYSTORE_BASE64`
- `SHOPPING_KEYSTORE_PASSWORD`
- `SHOPPING_KEY_ALIAS`
- `SHOPPING_KEY_PASSWORD`

Antes de trocar a chave, considere se você já instalou uma compilação assinada com a chave anterior. Uma nova chave não atualiza normalmente instalações assinadas com outro certificado. O guia [CONFIGURAR_APK_E_ATUALIZACOES.md](CONFIGURAR_APK_E_ATUALIZACOES.md) explica essa decisão.

Os manifestos públicos são separados por variante para impedir que uma instalação receba um APK de pacote incompatível:
- Release oficial: `https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`
- Debug de teste: `https://raw.githubusercontent.com/768ull/Shopping-List/main/update-debug.json`

A versão Debug **1.0.12 (versionCode 13)** está publicada na Release de teste `debug-test`. O manifesto `update-debug.json` aponta para o APK dessa Release e contém o SHA-256 conferido do arquivo. O manifesto Release continua sem APK publicado até que a distribuição oficial assinada esteja pronta.

## Estado de validação

A base atual do aplicativo é **1.0.12 (versionCode 13)**. O Release oficial usa o pacote `br.com.shoppinglist`, otimização R8 e redução de recursos. A publicação instalável só acontece pelo workflow **Build Shopping Lists APK**, depois de a assinatura privada ser configurada nos quatro Secrets do GitHub Actions.

O workflow **Verify Shopping Lists Release** agora compila e valida exclusivamente a variante Release otimizada. Ele não gera nem publica APK Debug, e verifica que o pacote oficial não tenha sufixo `-debug` nem esteja marcado como `debuggable`.


## Compilações de teste e distribuição oficial

A variante Debug é reservada a testes de desenvolvimento e não é o aplicativo oficial. Ela usa outro identificador Android e pode exibir a marcação `-debug`; não instale compilações Debug como atualização da versão oficial.

O workflow **Verify Shopping Lists Release** serve somente para validar a compilação otimizada sem assinatura. Ele não publica arquivos instaláveis. O único fluxo autorizado a publicar o APK distribuível é **Build Shopping Lists APK**, que gera `Shopping-Lists.apk` com o pacote `br.com.shoppinglist`, assinatura V1/V2/V3/V4, otimização R8 e checksum. Ele exige os quatro Secrets de assinatura descritos em [CONFIGURAR_APK_E_ATUALIZACOES.md](CONFIGURAR_APK_E_ATUALIZACOES.md).

## Migração para a primeira versão oficial

A versão Debug instalada usa o identificador Android `br.com.shoppinglist.debug`; o Release oficial usa `br.com.shoppinglist`. Como o Android considera esses pacotes aplicativos diferentes, a primeira troca não pode substituir diretamente o Debug.

1. Atualize primeiro o aplicativo de teste para a versão Debug **1.0.12** (versionCode 13), assinada com o certificado Debug já preservado.
2. Na tela inicial, toque em **SALVAR BACKUP** e guarde o arquivo JSON.
3. Instale a versão oficial Release **1.0.12**. Os dois aplicativos podem ficar instalados ao mesmo tempo.
4. No Release, toque em **RESTAURAR BACKUP** e selecione o arquivo JSON. A importação acrescenta as listas sem apagar as que já existam.
5. Abra algumas listas e confira os itens antes de remover o aplicativo Debug antigo.

O backup leva títulos, produtos, posição, marcações de compra, emojis e adesivos PNG gerados localmente (quando cada imagem tem até 4 MB). A importação restaura as imagens dentro do armazenamento privado do aplicativo.

**Depois dessa migração inicial**, as atualizações da versão oficial são instaladas por cima, sem desinstalar, desde que cada nova versão use o mesmo identificador `br.com.shoppinglist`, um `versionCode` maior e exatamente a mesma chave privada Release. O atualizador verifica o certificado antes de abrir o instalador do Android.

## Histórico de versões do repositório

Cada publicação oficial deve criar uma entrada própria no menu **Releases**, identificada por uma tag exclusiva (`v1.0.12`, `v1.0.13`, `v1.0.14` e assim por diante). Publicar uma atualização não apaga nem substitui as versões anteriores. O workflow recusa tags já existentes e não sobrescreve arquivos de uma Release existente.

As entradas antigas só podem conter APK quando o arquivo daquela versão foi realmente compilado e publicado. Não são criados downloads fictícios para versões cujo APK não está disponível.

## Baixar o aplicativo oficial pelo GitHub Mobile

As versões instaláveis oficiais ficam em **Releases** deste repositório. Baixe somente o arquivo **Shopping-Lists.apk** de uma versão publicada pelo workflow **Build Shopping Lists APK**. Não use **Code → Download ZIP**, artefatos de verificação ou APKs de teste: eles não são a distribuição oficial.

- [Ver todas as versões oficiais (Releases)](https://github.com/768ull/Shopping-List/releases)
- [Baixar a versão oficial mais recente](https://github.com/768ull/Shopping-List/releases/latest)

Uma versão só estará disponível quando o workflow de distribuição concluir a compilação Release, validar a assinatura e publicar o APK.