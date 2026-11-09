# Shopping List — projeto Android nativo

Aplicativo nativo de lista de compras, com visual amarelo e página de caderno. O projeto não contém WebView, não possui permissão `INTERNET` e não usa serviços remotos.

## Recursos implementados

- Criar várias listas, abrir, fixar/desafixar e excluir.
- Guardar listas e produtos em SQLite local; a gravação é feita enquanto a pessoa digita.
- Não há `maxLength`/limite artificial de caracteres nos produtos nem limite fixo de itens ou listas. O limite real é o espaço disponível no aparelho e os limites práticos do Android/SQLite.
- Cada produto tem um emoji adesivo; o modo automático identifica palavras comuns em português por um catálogo local. É possível escolher manualmente entre dezenas de emojis ou voltar ao automático.
- Exportação PDF multipágina em tamanho A4 usando `android.graphics.pdf.PdfDocument`.
- Exportação PNG em alta resolução (uma imagem por página) usando `Bitmap`; a pessoa escolhe a pasta de destino.
- A tela usa modo imersivo para ocultar as barras de status e navegação. O Android ainda pode mostrar temporariamente as barras quando a pessoa faz o gesto do sistema ou quando o teclado aparece.
- Ícone próprio para o aplicativo.

## SDK e compatibilidade

- `compileSdk = 37` (Android 17).
- `targetSdk = 37`.
- `minSdk = 21` (Android 5.0).
- Código Java + Views Android nativas; sem Jetpack Compose ou bibliotecas de exportação de terceiros.
- Android Studio/Android Gradle Plugin compatível com API 37 (AGP 9.5.0) e JDK 17 ou superior.

O SDK 37 é usado para compilar e direcionar o aplicativo. `minSdk 21` é o que define a versão mínima instalável; isso não significa compatibilidade com versões anteriores ao Android 5.0.

## Assinatura APK V1/V2/V3/V4

O `signingConfigs.release` ativa `enableV1Signing`, `enableV2Signing`, `enableV3Signing` e `enableV4Signing`. A assinatura V4 também cria o arquivo separado `.apk.idsig`; ele não substitui o APK e é usado em cenários de instalação incremental.

Foi criada uma chave de desenvolvimento local em `signing/shopping-list-release.jks`. As credenciais estão em `signing/signing.properties`, usadas pelo Gradle para assinar o build de release. **Não publique nem envie esses dois arquivos para um repositório público.** Faça uma cópia de segurança offline: versões futuras do aplicativo precisam ser assinadas com a mesma chave para instalar como atualização por cima da versão anterior.

> O arquivo `.gitignore` ignora a pasta `signing/` intencionalmente. Se você transferir o projeto para outro computador, copie a pasta `signing/` por um canal privado.

## Gerar APK de release

1. Instale Android Studio compatível com o Android 17.
2. No SDK Manager, instale Android SDK Platform 37 e Android SDK Build-Tools 36.0.0 (versão padrão informada pelo AGP 9.5.0).
3. Abra esta pasta no Android Studio e aguarde a sincronização do Gradle.
4. Execute a tarefa `:app:assembleRelease`.
5. O APK será gerado em `app/build/outputs/apk/release/app-release.apk`. Com V4 habilitada, o build também gera o arquivo `.idsig` correspondente.

Também é possível executar `./build-release.sh` (Linux/macOS, com Gradle e Android SDK configurados) ou `build-release.bat` (Windows). O script tenta validar a assinatura se `apksigner` estiver no `PATH`.

## Verificar as quatro assinaturas

Com Android SDK Build-Tools instalado:

```bash
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

O `apksigner` deve indicar V1, V2 e V3 habilitadas; a V4 é registrada no arquivo `.idsig` produzido ao lado do APK. O esquema V4 depende de V2 ou V3 e não é um quarto bloco embutido no APK.

## Observações

- Exportar PNG pede que a pessoa escolha uma pasta; em seguida o aplicativo cria um arquivo PNG para cada página.
- Exportar PDF pede o nome/local do arquivo com o seletor nativo de documentos do Android.
- Os emojis exibidos dependem das fontes de emoji disponíveis no sistema. Em Androids antigos alguns emojis recentes podem aparecer como símbolo genérico, mas o texto da lista permanece intacto.
- A geração do APK precisa do Android SDK/Gradle. O ambiente que originou este pacote não tinha Android SDK nem acesso de rede aos servidores de distribuição do Gradle, portanto o APK binário não foi compilado aqui. O projeto-fonte e a configuração de assinatura estão incluídos.

## Verificação de atualizações integrada

A tela inicial inclui **Verificar atualizações**. A consulta só acontece quando a pessoa toca nesse botão; a criação, edição, gravação das listas, PDF e PNG continuam locais e não precisam de internet.

O atualizador lê `update.json` em:

`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

Esse endereço está reservado para a publicação futura. O repositório público `768ull/Shopping-List` ainda precisa ser criado e receber o manifesto e os APKs de release para que a verificação encontre versões novas. Até isso ser publicado, a tela informará que a fonte ainda não está disponível.

Formato do arquivo `update.json`:

```json
{
  "versionCode": 2,
  "versionName": "1.1",
  "apkUrl": "https://github.com/768ull/Shopping-List/releases/download/v1.1/ShoppingList.apk",
  "sha256": "COLOQUE_AQUI_O_SHA256_DE_64_CARACTERES",
  "changelog": "Melhorias e correções."
}
```

Para cada versão publicada:

1. Aumente `versionCode` (por exemplo, de 1 para 2) e atualize `versionName`.
2. Gere o APK Release com a mesma chave `signing/shopping-list-release.jks` e o mesmo `applicationId` `br.com.shoppinglist`.
3. Publique o APK como asset de um GitHub Release no endereço usado em `apkUrl`.
4. Calcule o SHA-256 do APK publicado e atualize `sha256`.
5. Atualize o `update.json` na branch `main` do repositório público.

O aplicativo verifica que o pacote baixado é `br.com.shoppinglist`, que seu `versionCode` é maior e que o certificado de assinatura corresponde ao aplicativo instalado. O Android abre sua própria tela de instalação e pode pedir autorização para instalar apps dessa fonte. A confirmação final continua com a pessoa; não é possível instalar silenciosamente em um aparelho Android comum.

**Importante:** não troque nem perca a chave privada. Atualizações com outro certificado não serão instaladas sobre a versão anterior e poderão exigir desinstalação, o que apagaria os dados locais.
