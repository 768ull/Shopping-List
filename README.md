# Shopping List

Aplicativo Android de lista de compras, com compilação automatizada pelo GitHub Actions e mecanismo de consulta de atualizações pelo manifesto `update.json`.

## Estrutura atual

| Caminho | Finalidade |
|---|---|
| `Shopping_List_Project_com_Atualizador.zip` | Projeto-fonte Android. O workflow testa o ZIP, extrai o projeto e localiza o módulo `app`. |
| `.github/workflows/android-build.yml` | Compilação, assinatura V1/V2/V3/V4, validação, artefato e publicação do APK. |
| `update.json` | Manifesto consultado pelo aplicativo para detectar uma versão mais recente. |
| `CONFIGURAR_APK_E_ATUALIZACOES.md` | Configuração da chave privada e publicação de versões. |
| `PROJECT_STRUCTURE.md` | Mapa da estrutura e do fluxo de compilação. |

## Compilar o APK

1. Configure os quatro GitHub Actions Secrets descritos em [CONFIGURAR_APK_E_ATUALIZACOES.md](CONFIGURAR_APK_E_ATUALIZACOES.md).
2. Abra **Actions → Build Shopping List APK → Run workflow**.
3. Na primeira publicação, use `version_code=1` e `version_name=1.0`, desde que `apkUrl` ainda esteja vazio em `update.json`.
4. Nas versões seguintes, aumente o `version_code` a cada publicação.
5. O workflow testa e extrai o ZIP, localiza o módulo Android, injeta a configuração de assinatura somente no diretório temporário, compila e verifica o APK.
6. O APK, o arquivo de assinatura V4 (`.idsig`) e o SHA-256 são guardados como artefato e publicados no GitHub Release.
7. Após a publicação, o workflow atualiza `update.json`.

O pipeline usa o Gradle Wrapper do projeto quando ele existe; caso contrário, utiliza Gradle 9.6.0.

## Atualizações do aplicativo

O endereço bruto do manifesto na branch principal é:

`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

Cada atualização precisa usar a mesma chave privada da instalação anterior e um `version_code` maior. O Android exige continuidade da identidade de assinatura para atualizar uma instalação existente.

## Segurança da assinatura

O ZIP enviado originalmente contém uma chave de desenvolvimento e credenciais dentro de `signing/`. Como o repositório é público, essa chave está exposta e não deve ser tratada como chave privada segura de distribuição.

O workflow não usa automaticamente a chave exposta do ZIP. Ele exige estes quatro GitHub Actions Secrets:

- `SHOPPING_KEYSTORE_BASE64`: conteúdo Base64 do keystore.
- `SHOPPING_KEYSTORE_PASSWORD`: senha do keystore.
- `SHOPPING_KEY_ALIAS`: alias da chave.
- `SHOPPING_KEY_PASSWORD`: senha da chave.

**Importante:** antes de substituir a chave, confirme se algum APK já foi distribuído ou instalado com a chave antiga. Uma nova chave não atualiza diretamente instalações assinadas com um certificado diferente. Se já houver instalações, é necessário preservar a identidade anterior ou planejar uma rotação de chave compatível; não troque o keystore às cegas.

Nunca coloque um keystore de distribuição, senhas ou `signing.properties` no repositório.

## Limitação conhecida

O código-fonte continua armazenado no ZIP. O workflow agora localiza o diretório real do módulo, mas a migração para arquivos Android individuais na raiz ainda exige abrir e revisar o conteúdo do arquivo compactado. A ferramenta de integração disponível nesta revisão não conseguiu fornecer os bytes do ZIP; portanto, não foi possível auditar as telas, dependências, permissões, lógica do atualizador nem executar uma compilação real.
