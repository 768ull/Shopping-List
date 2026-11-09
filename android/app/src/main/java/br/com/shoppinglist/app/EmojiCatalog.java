package br.com.shoppinglist.app;

import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Offline Unicode Emoji 18.0 catalog plus a compact Portuguese product recognizer.
 * Recognition runs locally: no account, network, cloud API, or large language model.
 */
public final class EmojiCatalog {
    public static final String UNICODE_VERSION = "18.0";
    private static volatile List<EmojiEntry> cachedAll;

    public static final class EmojiEntry {
        public final String emoji;
        public final String name;
        EmojiEntry(String emoji, String name) {
            this.emoji = emoji;
            this.name = name;
        }
    }

    private static final String[][] RULES = {
        {"papel higienico|papel toalha|guardanapo", "🧻"},
        {"creme de leite|leite condensado|leite em po|leite|iogurte", "🥛"},
        {"feijao preto|feijao carioca|feijao branco|feijao|lentilha", "🫘"},
        {"arroz integral|arroz branco|arroz", "🍚"},
        {"maca verde|maca", "🍎"}, {"banana", "🍌"}, {"uva", "🍇"},
        {"morango", "🍓"}, {"laranja|tangerina|mexerica", "🍊"},
        {"melancia", "🍉"}, {"abacaxi", "🍍"}, {"manga", "🥭"},
        {"pera", "🍐"}, {"limao", "🍋"}, {"abacate", "🥑"},
        {"tomate", "🍅"}, {"cenoura", "🥕"}, {"batata doce", "🍠"},
        {"batata inglesa|batata", "🥔"}, {"cebola", "🧅"}, {"alho", "🧄"},
        {"brocolis", "🥦"}, {"alface|couve|repolho|verdura|espinafre", "🥬"},
        {"milho em lata|ervilha em lata|sardinha em lata|enlatado", "🥫"},
        {"milho", "🌽"}, {"pepino", "🥒"}, {"pimentao", "🫑"},
        {"pimenta", "🌶️"}, {"cogumelo", "🍄"}, {"pao|bisnaga|torrada", "🍞"},
        {"queijo|mussarela|muçarela|requeijao", "🧀"}, {"ovo|ovos", "🥚"},
        {"manteiga|margarina", "🧈"}, {"bacon|presunto|linguica|salsicha", "🥓"},
        {"carne moida|carne|bife", "🥩"}, {"frango|peito de frango|sobrecoxa", "🍗"},
        {"peixe|tilapia|sardinha", "🐟"}, {"camarao", "🍤"},
        {"macarrao|espaguete|massa", "🍝"}, {"sopa|miojo", "🍜"},
        {"sal|tempero", "🧂"}, {"mel", "🍯"}, {"biscoito|bolacha|cookie", "🍪"},
        {"chocolate|bombom", "🍫"}, {"bala|pirulito", "🍬"},
        {"bolo|torta|doce", "🍰"}, {"sorvete", "🍦"}, {"pizza", "🍕"},
        {"hamburguer", "🍔"}, {"batata frita", "🍟"}, {"pipoca", "🍿"},
        {"suco|refresco", "🧃"}, {"refrigerante|agua mineral|agua|refri", "🥤"},
        {"cafe em po|cafe", "☕"}, {"cha", "🍵"},
        {"sabonete|sabao em po|sabao", "🧼"},
        {"detergente|desinfetante|agua sanitaria|alvejante", "🧴"},
        {"shampoo|xampu|condicionador|creme hidratante|desodorante|perfume", "🧴"},
        {"esponja", "🧽"}, {"vassoura", "🧹"}, {"balde", "🪣"},
        {"escova de dente|pasta de dente|creme dental", "🪥"},
        {"racao|petisco|areia de gato", "🐾"}, {"planta|vaso|muda", "🪴"},
        {"celular|telefone|smartphone", "📱"}, {"televisao|tv", "📺"},
        {"computador|notebook|laptop", "💻"}, {"tablet", "📱"},
        {"mouse", "🖱️"}, {"teclado", "⌨️"}, {"impressora", "🖨️"},
        {"controle de videogame|controle de video game|video game|console", "🎮"},
        {"fone de ouvido|headphone|fone", "🎧"}, {"caixa de som|alto falante", "🔊"},
        {"camera fotografica|camera", "📷"},
        {"carregador|cabo usb|tomada|adaptador|extensao", "🔌"},
        {"pilha|bateria", "🔋"}, {"lampada led|lampada", "💡"},
        {"relogio", "⌚"}, {"oculos de sol", "🕶️"}, {"oculos", "👓"},
        {"camiseta|camisa", "👕"}, {"calca", "👖"}, {"vestido", "👗"},
        {"sapato|tenis|chinelo|sandalia", "👟"}, {"meia", "🧦"},
        {"bolsa|mochila", "🎒"}, {"lapis", "✏️"}, {"caneta", "🖊️"},
        {"caderno|agenda", "📓"}, {"livro", "📚"}, {"tesoura", "✂️"},
        {"cola escolar|cola", "🧴"}, {"martelo", "🔨"},
        {"chave de fenda|chave philips|parafuso", "🪛"},
        {"chave inglesa|alicate", "🔧"}, {"serrote|serra", "🪚"},
        {"trena|regua", "📏"}, {"prego", "📌"},
        {"vaso sanitario", "🚽"}, {"chuveiro", "🚿"}, {"banheira", "🛁"},
        {"colchao|travesseiro|cama", "🛏️"}, {"cadeira", "🪑"},
        {"lixeira|caixa de papelao|caixa", "📦"},
        {"presente|embalagem de presente", "🎁"}, {"chaveiro|chave", "🔑"},
        {"carro", "🚗"}, {"moto|motocicleta", "🏍️"},
        {"bicicleta", "🚲"}, {"pneu", "🛞"}, {"bola", "⚽"},
        {"boneca", "🪆"}, {"ursinho|pelucia", "🧸"}
    };

    private static final String[] QUICK = {
        "🍎","🍌","🍇","🍓","🍊","🍉","🍍","🥭","🍐","🍋","🥑","🍅",
        "🥕","🥔","🍠","🧅","🧄","🥦","🥬","🌽","🥒","🫑","🌶️","🍄",
        "🍞","🥐","🧀","🥚","🥛","🫘","🧈","🥓","🥩","🍗","🐟","🍤",
        "🍚","🍝","🍜","🥫","🧂","🍪","🍫","🍬","🍯","🍰","🍦","🍕",
        "🍔","🍟","🍿","🧃","🥤","☕","🍵","🧻","🧼","🧽","🧹","🪣",
        "🧴","🪥","🧺","🛒","📦","🐶","🐱","🐾","🌿","🪴","📱","📺",
        "💻","🖱️","⌨️","🖨️","🎮","🎧","🔊","📷","🔌","🔋","💡","⌚",
        "👕","👖","👗","👟","🧦","🎒","✏️","🖊️","📓","📚","✂️","🔨",
        "🪛","🔧","🪚","📏","🚽","🚿","🛁","🛏️","🪑","🎁","🔑","🚗",
        "🏍️","🚲","🛞","⚽","🧸","💊","🩹","🧯","🪜","🪟","🛠️","🏷️"
    };

    private EmojiCatalog() {}

    /** Loads the full 3,963 fully-qualified Unicode 18.0 emoji set from local APK assets. */
    public static List<EmojiEntry> all(Context context) {
        List<EmojiEntry> local = cachedAll;
        if (local != null) return local;
        synchronized (EmojiCatalog.class) {
            if (cachedAll != null) return cachedAll;
            List<EmojiEntry> result = new ArrayList<>();
            try {
                JSONObject universe = new JSONObject(readAsset(context, "emoji/emoji.json"));
                JSONObject portuguese = new JSONObject(readAsset(context, "emoji/emoji_pt.json"));
                Iterator<String> keys = universe.keys();
                while (keys.hasNext()) {
                    String glyph = keys.next();
                    JSONObject info = universe.optJSONObject(glyph);
                    if (info == null || info.optInt("status", 0) != 2) continue;
                    String localized = portuguese.optString(glyph, "");
                    if (localized == null || localized.trim().isEmpty()) continue;
                    String name = localized.replace(":", "").replace('_', ' ').trim();
                    result.add(new EmojiEntry(glyph, name));
                }
                Collections.sort(result, Comparator.comparing((EmojiEntry e) -> normalize(e.name)));
            } catch (Exception ignored) {
                result.clear();
                for (String glyph : QUICK) result.add(new EmojiEntry(glyph, glyph));
            }
            cachedAll = Collections.unmodifiableList(result);
            return cachedAll;
        }
    }

    public static List<EmojiEntry> quick(Context context) {
        List<EmojiEntry> all = all(context);
        List<EmojiEntry> result = new ArrayList<>();
        for (String glyph : QUICK) {
            for (EmojiEntry entry : all) {
                if (entry.emoji.equals(glyph)) {
                    result.add(entry);
                    break;
                }
            }
        }
        return result;
    }

    /** Searches the complete bundled emoji set by Portuguese name. */
    public static List<EmojiEntry> search(Context context, String query, int limit) {
        String q = normalize(query);
        List<EmojiEntry> result = new ArrayList<>();
        if (q.isEmpty()) return quick(context);
        for (EmojiEntry entry : all(context)) {
            if (normalize(entry.name).contains(q)) {
                result.add(entry);
                if (result.size() >= limit) break;
            }
        }
        return result;
    }

    /** Lightweight on-device product recognition: synonyms + accents + one/two-letter typo tolerance. */
    public static String automatic(String value) {
        String s = normalize(value);
        if (s.isEmpty()) return "sticker:OUTRO";
        for (String[] rule : RULES) {
            for (String term : rule[0].split("\\|")) {
                String normalizedTerm = normalize(term);
                if (hasPhrase(s, normalizedTerm) || fuzzyWordMatch(s, normalizedTerm)) return rule[1];
            }
        }
        return StickerCatalog.forText(s);
    }

    private static boolean hasPhrase(String text, String phrase) {
        return !phrase.isEmpty() && (" " + text + " ").contains(" " + phrase + " ");
    }

    private static boolean fuzzyWordMatch(String text, String term) {
        if (term.indexOf(' ') >= 0 || term.length() < 5) return false;
        String[] words = text.split(" ");
        int allowed = term.length() >= 9 ? 2 : 1;
        for (String word : words) {
            if (word.length() < 5 || Math.abs(word.length() - term.length()) > allowed) continue;
            if (distanceAtMost(word, term, allowed) <= allowed) return true;
        }
        return false;
    }

    private static int distanceAtMost(String a, String b, int max) {
        if (Math.abs(a.length() - b.length()) > max) return max + 1;
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            int rowMin = current[0];
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
                rowMin = Math.min(rowMin, current[j]);
            }
            if (rowMin > max) return max + 1;
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return s.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static String readAsset(Context context, String path) throws Exception {
        try (InputStream input = context.getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
