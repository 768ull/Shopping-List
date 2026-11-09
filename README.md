# Shopping List

Aplicativo Android de lista de compras, com compilação automatizada pelo GitHub Actions e mecanismo de consulta de atualizações pelo manifesto `update.json`.

## Onde estão os arquivos

| Caminho | Finalidade |
|---|---|
| `Shopping_List_Project_com_Atualizador.zip` | Pacote atual do projeto-fonte Android. É extraído pelo workflow antes da compilação. |
| `.github/workflows/android-build.yml` | Pipeline de compilação, assinatura, validação e publicação do APK. |
| `update.json` | Manifesto consultado pelo aplicativo para detectar uma versão mais recente. |
| `CONFIGURAR_APK_E_ATUALIZACOES.md` | Instruções para configurar a chave privada e publicar versões. |
| `PROJECT_STRUCTURE.md` | Mapa do repositório e fluxo de compilação. |

## Compilar o APK

1. Configure os quatro secrets de assinatura descritos em [CONFIGURAR_APK_E_ATUALIZACOES.md](CONFIGURAR_APK_E_ATUALIZACOES.md).
2. Abra **Actions → Build Shopping List APK → Run workflow**.
3. Informe um `version_code` maior que o da versão publicada e um `version_name`.
4. O workflow extrai o pacote-fonte, compila o módulo Android `app`, valida a assinatura e publica `ShoppingList.apk` no GitHub Release.
5. O workflow atualiza `update.json` com versão, endereço do APK, SHA-256 e changelog.

## Atualizações do aplicativo

O endereço bruto do manifesto na branch principal é:

`https://raw.githubusercontent.com/768ull/Shopping-List/main/update.json`

Cada atualização precisa usar a mesma chave privada de assinatura da instalação anterior e um `version_code` maior. Não troque nem perca o keystore de distribuição: o Android exige a continuidade da identidade de assinatura para atualizar o aplicativo sem desinstalá-lo.

## Segurança

- Nunca coloque keystore, senhas ou arquivos `signing.properties` no repositório.
- Como o repositório é público, qualquer chave de desenvolvimento incluída em um ZIP antigo deve ser considerada comprometida.
- Use somente os GitHub Actions Secrets indicados no guia de configuração.
- Não publique APKs de distribuição assinados com uma chave de teste.

## Observação sobre a estrutura atual

O código-fonte Android ainda está armazenado dentro do ZIP, não como módulos e pastas navegáveis na raiz. O workflow conhece esse caminho porque extrai o arquivo e valida `ShoppingListProject/app/build.gradle`. Para deixar cada arquivo-fonte individualmente visível no GitHub, será necessário extrair o ZIP e migrar a árvore do projeto para a raiz do repositório, preservando o conteúdo original e conferindo os caminhos reais antes de alterar o pipeline.
