# Compilação do Shopping List e atualizações

O repositório guarda o projeto-fonte dentro de `Shopping_List_Project_com_Atualizador.zip`.
O workflow **Build Shopping List APK** extrai o projeto, compila com Android SDK 37 / Android Gradle Plugin 9.5.0 / Gradle 9.6.0, verifica o APK, publica um GitHub Release e atualiza `update.json`.

## Atenção: chave de assinatura

O ZIP originalmente enviado ao repositório contém uma chave de desenvolvimento e o arquivo de credenciais dentro da pasta `signing/`. Como o repositório é público, essa chave deve ser considerada exposta e **não deve ser usada para assinar o APK publicado**. Gere uma chave nova para o primeiro APK que será distribuído. Depois, guarde uma cópia offline segura: perder essa chave impede atualizar o aplicativo instalado sem desinstalar.

O workflow ignora a chave que está no ZIP e exige uma chave privada guardada nos GitHub Actions Secrets.

## Criar uma nova chave privada

Em um computador com JDK instalado, execute:

```bash
keytool -genkeypair -v -keystore shopping-list-release.jks -alias shoppinglist -keyalg RSA -keysize 4096 -validity 10000
```

Guarde o arquivo `shopping-list-release.jks`, a senha do keystore, o alias e a senha da chave em um local privado. Não envie o keystore para o repositório.

Converta o arquivo para Base64 (em Linux/macOS):

```bash
base64 -w 0 shopping-list-release.jks
```

No GitHub, abra **Settings → Secrets and variables → Actions → New repository secret** e crie estes quatro secrets:

- `SHOPPING_KEYSTORE_BASE64`: todo o texto Base64 do keystore.
- `SHOPPING_KEYSTORE_PASSWORD`: senha do keystore.
- `SHOPPING_KEY_ALIAS`: alias usado no keytool (por exemplo, `shoppinglist`).
- `SHOPPING_KEY_PASSWORD`: senha da chave.

Nunca publique esses valores, nem os coloque no README ou em arquivos do repositório.

## Gerar o primeiro APK

1. Abra a aba **Actions** do repositório.
2. Selecione **Build Shopping List APK**.
3. Toque em **Run workflow**.
4. Para a primeira publicação, use `version_code=1`, `version_name=1.0` e uma descrição.
5. Aguarde o workflow ficar verde. O APK estará no Release `v1.0`, como `ShoppingList.apk`.

O workflow também publica `ShoppingList.apk.idsig` (assinatura V4) e atualiza `update.json`.

## Publicar atualizações futuras

Para cada atualização, execute o mesmo workflow e aumente o `version_code`: 2, 3, 4 etc. Use um nome novo, por exemplo `1.1`, `1.2`. A chave privada precisa continuar sendo a mesma. O workflow publica o APK e atualiza automaticamente o manifesto que o aplicativo consulta.

A instalação de atualização é confirmada pelo Android. A verificação manual de atualizações requer internet; todas as listas, gravações e exportações continuam locais/offline.
