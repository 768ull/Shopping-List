# Compilação do Shopping List e atualizações

O projeto Android está em `Shopping_List_Project_com_Atualizador.zip`. O workflow testa o pacote, extrai os arquivos, localiza o módulo `app` e procura a raiz Gradle pelo arquivo `settings.gradle` ou `settings.gradle.kts`.

O pipeline instala JDK 17, Android SDK API 37, Build Tools 36.0.0 e usa o Gradle Wrapper do projeto quando disponível; caso contrário, usa Gradle 9.6.0.

## Assinatura do APK

O workflow configura explicitamente as assinaturas Android V1, V2, V3 e V4 no build de Release. As assinaturas V1, V2 e V3 ficam no APK; a V4 é fornecida em um arquivo separado chamado `.idsig`.

### Chave incluída no ZIP

O ZIP originalmente enviado contém uma chave de desenvolvimento e o arquivo de credenciais em `signing/`. Como o repositório é público, essa chave e suas credenciais devem ser consideradas expostas. O workflow não usa essa chave automaticamente.

**Não substitua a chave antiga sem verificar se algum APK já foi distribuído ou instalado com ela.** O Android não permite atualizar normalmente um aplicativo instalado com um APK assinado por um certificado diferente. Se já houver instalações usando a chave antiga, é necessário preservar a mesma identidade de assinatura ou planejar uma rotação compatível antes de publicar uma chave nova.

### Configurar os GitHub Actions Secrets

Crie ou selecione uma chave privada de distribuição adequada. Não a envie ao repositório.

Converta o arquivo para Base64. Em Linux/macOS:

```bash
base64 -w 0 shopping-list-release.jks
```

No GitHub, abra **Settings → Secrets and variables → Actions → New repository secret** e configure:

- `SHOPPING_KEYSTORE_BASE64`: todo o texto Base64 do keystore.
- `SHOPPING_KEYSTORE_PASSWORD`: senha do keystore.
- `SHOPPING_KEY_ALIAS`: alias da chave.
- `SHOPPING_KEY_PASSWORD`: senha da chave.

O workflow decodifica o keystore em um diretório temporário, valida o alias com `keytool` e injeta a configuração de assinatura apenas durante a compilação. Os valores secretos não são gravados no Git.

## Gerar a primeira versão

1. Abra **Actions** no repositório e selecione **Build Shopping List APK**.
2. Clique em **Run workflow**.
3. Use `version_code=1` e `version_name=1.0` enquanto `apkUrl` estiver vazio em `update.json`.
4. Aguarde a execução terminar. O APK só é publicado se a compilação, a verificação das assinaturas V1/V2/V3 e a geração do arquivo V4 `.idsig` forem bem-sucedidas.

A primeira versão pode usar o número 1 mesmo que o manifesto inicial já contenha `versionCode: 1`, desde que ainda não exista um `apkUrl` publicado.

## Publicar atualizações futuras

Para cada atualização, aumente o `version_code`: 2, 3, 4 etc. Use um `version_name` apropriado, como 1.1 ou 1.2. Mantenha a mesma chave privada usada na instalação anterior. O workflow publica o APK e atualiza `update.json` com URL, SHA-256, versão e changelog.

## Validações do pipeline

O workflow:
- testa a integridade do ZIP antes de extrair;
- exige exatamente um `app/build.gradle` e encontra a raiz Gradle;
- valida os quatro Secrets e o alias do keystore;
- rejeita números de versão inválidos ou repetidos após a primeira publicação;
- configura V1, V2, V3 e V4;
- compila o módulo Release e exige um APK e o arquivo `.idsig`;
- verifica V1/V2/V3 com `apksigner`, gera SHA-256, guarda o artefato e publica o Release;
- atualiza `update.json` somente depois da publicação.

A integração desta revisão não conseguiu ler os bytes internos do ZIP nem iniciar uma execução do GitHub Actions. Portanto, a configuração do pipeline foi corrigida, mas o código-fonte do app e o APK ainda precisam ser validados por uma execução real.
