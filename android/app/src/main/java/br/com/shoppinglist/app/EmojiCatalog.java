package br.com.shoppinglist.app;

import java.text.Normalizer;
import java.util.Locale;

/** Offline emoji stickers: small local Portuguese keyword catalog, no network or API calls. */
public final class EmojiCatalog {
    public static final String[] PALETTE = {
            "🍎", "🍌", "🍇", "🍓", "🍊", "🍉", "🍍", "🥭", "🍐", "🍋",
            "🥑", "🍅", "🥕", "🥔", "🧅", "🧄", "🥦", "🥬", "🌽", "🥒",
            "🍞", "🥖", "🥐", "🥯", "🥨", "🧀", "🥚", "🥛", "🧈", "🥓",
            "🥩", "🍗", "🍖", "🐟", "🍤", "🍚", "🍝", "🍜", "🥫", "🧂",
            "🍪", "🍫", "🍬", "🍯", "🧃", "🥤", "🧋", "☕", "🧻", "🧼",
            "🧽", "🧴", "🪥", "🧺", "🛒", "📦", "🐶", "🐱", "🌿", "🪴"
    };

    private EmojiCatalog() {}

    public static String automatic(String value) {
        String s = normalize(value);
        if (s.isEmpty()) return "🛒";
        if (containsAny(s, "maca", "maçã", "macas", "maçãs", "apple")) return "🍎";
        if (containsAny(s, "banana", "bananas")) return "🍌";
        if (containsAny(s, "uva", "uvas")) return "🍇";
        if (containsAny(s, "morango", "morangos")) return "🍓";
        if (containsAny(s, "laranja", "tangerina", "mexerica")) return "🍊";
        if (containsAny(s, "melancia")) return "🍉";
        if (containsAny(s, "abacaxi")) return "🍍";
        if (containsAny(s, "manga", "mangas")) return "🥭";
        if (containsAny(s, "pera")) return "🍐";
        if (containsAny(s, "limao", "limão")) return "🍋";
        if (containsAny(s, "abacate")) return "🥑";
        if (containsAny(s, "tomate")) return "🍅";
        if (containsAny(s, "cenoura")) return "🥕";
        if (containsAny(s, "batata")) return "🥔";
        if (containsAny(s, "cebola")) return "🧅";
        if (containsAny(s, "alho")) return "🧄";
        if (containsAny(s, "brocolis", "brócolis")) return "🥦";
        if (containsAny(s, "alface", "couve", "repolho", "verdura")) return "🥬";
        if (containsAny(s, "milho")) return "🌽";
        if (containsAny(s, "pepino")) return "🥒";
        if (containsAny(s, "pao", "pão", "bisnaga", "torrada")) return "🍞";
        if (containsAny(s, "queijo", "mussarela", "muçarela", "requeijao", "requeijão")) return "🧀";
        if (containsAny(s, "ovo", "ovos")) return "🥚";
        if (containsAny(s, "leite", "iogurte", "creme de leite")) return "🥛";
        if (containsAny(s, "manteiga", "margarina")) return "🧈";
        if (containsAny(s, "bacon", "presunto", "linguica", "linguiça")) return "🥓";
        if (containsAny(s, "carne", "bife", "moida", "moída")) return "🥩";
        if (containsAny(s, "frango", "peito de frango")) return "🍗";
        if (containsAny(s, "peixe", "tilapia", "tilápia", "sardinha")) return "🐟";
        if (containsAny(s, "camarao", "camarão")) return "🍤";
        if (containsAny(s, "arroz", "feijao", "feijão")) return "🍚";
        if (containsAny(s, "macarrao", "macarrão", "espaguete", "massa")) return "🍝";
        if (containsAny(s, "sopa", "miojo")) return "🍜";
        if (containsAny(s, "milho em lata", "ervilha", "sardinha em lata", "enlatado")) return "🥫";
        if (containsAny(s, "sal", "tempero")) return "🧂";
        if (containsAny(s, "biscoito", "bolacha", "cookie")) return "🍪";
        if (containsAny(s, "chocolate", "bombom")) return "🍫";
        if (containsAny(s, "bala", "pirulito")) return "🍬";
        if (containsAny(s, "mel")) return "🍯";
        if (containsAny(s, "suco", "refresco")) return "🧃";
        if (containsAny(s, "refrigerante", "agua", "água", "refri")) return "🥤";
        if (containsAny(s, "cafe", "café")) return "☕";
        if (containsAny(s, "papel higienico", "papel higiênico", "guardanapo", "papel toalha")) return "🧻";
        if (containsAny(s, "sabonete", "sabao", "sabão", "detergente")) return "🧼";
        if (containsAny(s, "esponja")) return "🧽";
        if (containsAny(s, "shampoo", "xampu", "condicionador", "creme")) return "🧴";
        if (containsAny(s, "escova de dente", "pasta de dente", "creme dental")) return "🪥";
        if (containsAny(s, "racao", "ração", "petisco", "areia de gato")) return "🐾";
        if (containsAny(s, "planta", "vaso", "muda")) return "🪴";
        return "🛒";
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            String t = normalize(term);
            if (!t.isEmpty() && value.contains(t)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
