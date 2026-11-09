# Estrutura do repositório

## Árvore atual

```text
Shopping-List/
├── .github/
│   └── workflows/
│       └── android-build.yml
├── CONFIGURAR_APK_E_ATUALIZACOES.md
├── PROJECT_STRUCTURE.md
├── README.md
├── Shopping_List_Project_com_Atualizador.zip
└── update.json
```

## Papel de cada item

- **ZIP do projeto:** contém o projeto Android original. O pipeline o extrai em um diretório temporário chamado `_project`.
- **Workflow:** localiza o projeto extraído no caminho esperado `_project/ShoppingListProject`, compila o módulo `app`, verifica o APK e publica os artefatos.
- **Manifesto:** o aplicativo deve consultar o arquivo `update.json` na branch `main` para verificar se existe uma versão mais recente.
- **Guia de configuração:** documenta os secrets de assinatura que precisam ser cadastrados no GitHub.

## Fluxo de compilação

1. Checkout da branch.
2. Extração de `Shopping_List_Project_com_Atualizador.zip`.
3. Preparação do keystore privado a partir dos GitHub Actions Secrets.
4. Atualização de `versionCode` e `versionName` no Gradle.
5. Instalação do JDK e Android SDK.
6. Compilação de `app:assembleRelease`.
7. Verificação da assinatura e cálculo SHA-256.
8. Publicação do APK no GitHub Release.
9. Atualização de `update.json`.

## Limitação conhecida e próxima migração

Esta árvore documenta os caminhos que estão efetivamente visíveis na raiz do repositório. O conteúdo interno do ZIP não foi expandido nesta revisão; portanto, não inventamos nomes de módulos, pastas ou arquivos internos. A migração correta para uma estrutura Android convencional deve preservar os arquivos reais do ZIP e só então ajustar o workflow para apontar ao `settings.gradle` e ao módulo `app` extraídos.
