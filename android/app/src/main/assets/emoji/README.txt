Shopping Lists — Catálogo de emojis de produtos

Versão dos dados: Unicode Emoji 18.0 (15 de setembro de 2026).
O aplicativo não carrega o catálogo inteiro de emojis Unicode. O seletor contém somente
ícones filtrados que representam produtos físicos que podem aparecer em uma lista de compras.

Arquivos usados pelo aplicativo:
- emoji_catalog_pt.tsv: emojis de produtos e seus nomes para pesquisa em português.
- emoji_catalog_manifest.json: versão Unicode, tipo/esquema do catálogo, quantidade e SHA-256.

O gerador em tools/update_unicode_emoji.py lê a lista oficial Unicode e aplica uma lista
de subcategorias permitidas, removendo rostos, pessoas, gestos, bandeiras, símbolos de
estado/ação e animais que não representam mercadorias. O catálogo é atualizado pelo
GitHub Actions e o aplicativo verifica a versão publicada em segundo plano.

Integração com teclados:
Gboard, Microsoft SwiftKey e outros teclados Android podem inserir emojis Unicode no
campo de texto do aplicativo. O Shopping Lists não importa os pacotes privados de cada
teclado; valida o caractere inserido contra o catálogo de produtos para impedir emojis
fora da finalidade de compras.

Fontes:
- Unicode Emoji 18.0: https://www.unicode.org/Public/18.0.0/emoji/emoji-test.txt
- Unicode Emoji sequences: https://www.unicode.org/Public/18.0.0/emoji/emoji-sequences.txt
- Unicode Emoji ZWJ sequences: https://www.unicode.org/Public/18.0.0/emoji/emoji-zwj-sequences.txt
- Dados de nomes em português: projeto CLDR, https://github.com/unicode-org/cldr

Os arquivos Unicode estão cobertos pela licença Unicode V3 incluída nesta pasta.
A aparência final de cada emoji depende da fonte de emojis disponível no Android.
