# Estrutura do repositório

## Arquivos visíveis

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

## Conteúdo do ZIP

O código-fonte Android continua dentro de `Shopping_List_Project_com_Atualizador.zip`. A estrutura interna esperada contém um módulo Android `app` e um arquivo Gradle de configuração. O workflow não depende mais do nome fixo da pasta `ShoppingListProject`: ele procura exatamente um `app/build.gradle` e localiza a raiz pelo `settings.gradle` ou `settings.gradle.kts`.

## Fluxo de compilação

1. Checkout completo da branch.
2. Teste de integridade e extração do ZIP.
3. Localização do módulo `app` e da raiz Gradle.
4. Validação do keystore privado, alias e senhas por GitHub Actions Secrets.
5. Atualização de `versionCode` e `versionName`.
6. Configuração das assinaturas V1, V2, V3 e V4.
7. Instalação do JDK, Android SDK e Gradle.
8. Compilação do APK Release.
9. Verificação de V1/V2/V3, existência do arquivo V4 `.idsig` e cálculo SHA-256.
10. Armazenamento do artefato e publicação do GitHub Release.
11. Atualização de `update.json`.

## Segurança

A chave de desenvolvimento incluída no ZIP está exposta porque o repositório é público. O workflow não a utiliza automaticamente: exige uma chave privada de distribuição em GitHub Actions Secrets. Antes de trocar a chave, verifique se já existem instalações usando a chave antiga, pois a identidade de assinatura precisa ser preservada para atualizações normais.

## Limitação da auditoria

A integração GitHub usada nesta sessão não conseguiu fornecer os bytes do ZIP. Por isso, a árvore interna, as dependências, o código do atualizador e os recursos do aplicativo não foram inspecionados nem modificados nesta revisão. Não inventamos nomes de arquivos internos nem declaramos uma compilação como aprovada sem executá-la.
