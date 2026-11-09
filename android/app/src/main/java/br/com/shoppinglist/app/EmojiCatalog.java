package br.com.shoppinglist.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    public static final int CATALOG_SCHEMA = 1;
    public static final int TOTAL_EMOJI_COUNT = 411;
    private static volatile List<EmojiEntry> cachedAll;
    private static volatile boolean loading;
    private static final Object LOAD_LOCK = new Object();
    private static final List<Runnable> LOAD_CALLBACKS = new ArrayList<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static final class EmojiEntry {
        public final String emoji;
        public final String name;
        private final String searchable;
        EmojiEntry(String emoji, String name) {
            this.emoji = emoji;
            this.name = name;
            this.searchable = normalize(name + " " + searchAliases(emoji));
        }
    }

    private static final String[][] RULES = {
        // Produtos de limpeza precisam ser reconhecidos antes de qualquer regra de água/bebida.
        {"agua sanitaria|cloro|desinfetante|alvejante|detergente", "🧴"},
        {"papel higienico|papel toalha|guardanapo", "🧻"},
        {"creme de leite|leite condensado|leite em po|leite|iogurte", "🥛"},
        {"manteiga|margarina", "🧈"},
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
        {"linguica calabresa|linguica toscana|linguica de frango|linguica defumada|linguica", "sticker:LINGUICA"},
        {"salame italiano|salame", "sticker:SALAME"},
        {"salsicha de frango|salsicha viena|salsicha", "sticker:SALSICHA"},
        {"presunto|apresuntado", "sticker:PRESUNTO"}, {"mortadela", "sticker:MORTADELA"},
        {"bacon", "🥓"}, {"carne moida|carne|bife", "🥩"},
        {"frango|peito de frango|sobrecoxa", "🍗"},
        {"peixe|tilapia|sardinha", "🐟"}, {"camarao", "🍤"},
        {"macarrao|espaguete|massa", "🍝"}, {"sopa|miojo", "🍜"},
        {"sal|tempero", "🧂"}, {"mel", "🍯"}, {"biscoito|bolacha|cookie", "🍪"},
        {"chocolate|bombom", "🍫"}, {"bala|pirulito", "🍬"},
        {"bolo|torta|doce", "🍰"}, {"sorvete", "🍦"}, {"pizza", "🍕"},
        {"hamburguer", "🍔"}, {"batata frita", "🍟"}, {"pipoca", "🍿"},
        {"suco|suco natural|suco integral|suco de caixinha|nectar|refresco|polpa de fruta", "🧃"},
        {"refrigerante|refri", "🥤"},
        {"agua de coco", "🥥"},
        {"agua mineral|agua com gas|agua sem gas|agua potavel|garrafa de agua|agua", "💧"},
        {"cafe em po|cafe soluvel|capsula de cafe|cappuccino|capuccino|espresso|cafe", "☕"},
        {"cha gelado|cha pronto|cha mate|infusao|cha", "🍵"},
        {"sabonete|sabao em po|sabao", "🧼"},
        {"detergente|desinfetante|agua sanitaria|alvejante", "🧴"},
        {"shampoo|xampu|condicionador|creme hidratante|desodorante|perfume", "🧴"},
        {"esponja", "🧽"}, {"vassoura", "🧹"}, {"balde", "🪣"},
        {"escova de dente|pasta de dente|creme dental", "🪥"},
        {"racao|petisco|areia de gato", "🦴"}, {"planta|vaso|muda", "🪴"},
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
        "🍎","🍏","🍐","🍑","🍒","🍓","🫐","🥝","🍇","🍈","🍉","🍊","🍋","🍌","🍍","🥭","🥥",
        "🥑","🍆","🥔","🥕","🌽","🌶️","🫑","🥒","🥬","🥦","🧄","🧅","🥜","🫘","🌰","🍄","🫚","🫛","🍅",
        "🍞","🥐","🥖","🫓","🥨","🥯","🥞","🧇","🧀","🥚","🥓","🥩","🍗","🍖","🌭","🍔","🍟","🍕",
        "🥪","🥙","🌮","🌯","🫔","🥗","🥘","🫕","🍝","🍜","🍲","🍛","🍣","🍱","🥟","🍤","🦀","🦞","🦐","🦑","🐟","🦪",
        "🍦","🍧","🍨","🍩","🍪","🎂","🍰","🧁","🍫","🍬","🍭","🍯","🥛","🧈","🧂","🥫","🫙",
        "🧃","🥤","🧋","🍵","☕","🍶","🍺","🍷","🥃","💐","🌹","🌺","🌻","🌷","🪴","🌱","🌵",
        "🧻","🧼","🧽","🧹","🪣","🧴","🪥","🧺","🧯","🪜",
        "📱","📞","📟","📠","💻","🖥️","🖨️","⌨️","🖱️","🖲️","💽","🔌","🔋","📷","📸","📹","🎥","📺","🎧","🎙️","🎤","🔊",
        "👓","🕶️","👕","👖","👗","🥻","🩱","🩲","🩳","👟","🥾","👞","🥿","🧦","🧤","🧣","🧥","🧢","🎒","🧳","💍","💄","👜","👛","👝",
        "✏️","🖊️","🖋️","✒️","📓","📚","📒","📔","📖","📕","📗","📘","📙","📎","🖇️","✂️","📏","📐","📌",
        "🔨","🪓","⛏️","🛠️","🪛","🔧","🔩","🗜️","🪚","🧰" ,"🔑","🔒",
        "🚪","🪞","🪟","🛏️","🛋️","🪑","🚽","🪠","🚿","🛁","🪤","🪒",
        "💉","💊","🩹","🩼","🩺","🚗","🏍️","🛵","🚲","🛹","🛼","🛴","🛞","🦽",
        "⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🎱","🏓","🏸","🥅","🎮","🎲","🧩","🧸","🪆","🪀","🦴","🎁","📦"
    };

    private EmojiCatalog() {}

    /**
     * Reads a compact TSV catalog. Call preload() from the UI before search so
     * disk I/O and sorting never run while Android is drawing the dialog.
     */
    public static List<EmojiEntry> all(Context context) {
        List<EmojiEntry> local = cachedAll;
        if (local != null) return local;
        synchronized (EmojiCatalog.class) {
            if (cachedAll != null) return cachedAll;
            List<EmojiEntry> result = new ArrayList<>();
            File downloaded = new File(context.getFilesDir(), "emoji_catalog_pt.tsv");
            boolean downloadedCatalogIsCurrent = context
                    .getSharedPreferences("emoji_catalog_updates", Context.MODE_PRIVATE)
                    .getInt("catalog_schema", 0) == CATALOG_SCHEMA;
            try {
                InputStream source = downloadedCatalogIsCurrent && downloaded.isFile() && downloaded.length() > 0
                        ? new FileInputStream(downloaded)
                        : context.getAssets().open("emoji/emoji_catalog_pt.tsv");
                try (InputStream input = source;
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"), 8192)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int separator = line.indexOf('\t');
                        if (separator <= 0 || separator >= line.length() - 1) continue;
                        String glyph = line.substring(0, separator);
                        String name = line.substring(separator + 1).trim();
                        if (!glyph.isEmpty() && !name.isEmpty()) result.add(new EmojiEntry(glyph, name));
                    }
                }
                if (result.isEmpty()) throw new IllegalStateException("Catálogo vazio.");
                Collections.sort(result, Comparator.comparing((EmojiEntry entry) -> entry.searchable));
            } catch (Exception ignored) {
                result.clear();
                for (String glyph : QUICK) {
                    String aliases = searchAliases(glyph);
                    result.add(new EmojiEntry(glyph, aliases.isEmpty() ? glyph : aliases));
                }
            }
            cachedAll = Collections.unmodifiableList(result);
            return cachedAll;
        }
    }

    /** Invalidates the in-memory index after a verified catalog download. */
    public static void invalidate() {
        synchronized (EmojiCatalog.class) {
            cachedAll = null;
        }
    }

    /** Loads and indexes the catalog on a worker thread, then returns to the UI thread. */
    public static void preload(Context context, Runnable callback) {
        synchronized (LOAD_LOCK) {
            if (cachedAll != null) {
                if (callback != null) MAIN_HANDLER.post(callback);
                return;
            }
            if (callback != null) LOAD_CALLBACKS.add(callback);
            if (loading) return;
            loading = true;
        }
        Context appContext = context.getApplicationContext();
        Thread worker = new Thread(() -> {
            try {
                all(appContext);
            } finally {
                List<Runnable> callbacks;
                synchronized (LOAD_LOCK) {
                    loading = false;
                    callbacks = new ArrayList<>(LOAD_CALLBACKS);
                    LOAD_CALLBACKS.clear();
                }
                for (Runnable action : callbacks) MAIN_HANDLER.post(action);
            }
        }, "shopping-list-emoji-loader");
        worker.setPriority(Thread.NORM_PRIORITY - 1);
        worker.start();
    }

    public static boolean isLoaded() {
        return cachedAll != null;
    }

    /** Small first screen: does not parse the complete catalog. */
    public static List<EmojiEntry> quick(Context context) {
        List<EmojiEntry> result = new ArrayList<>(QUICK.length);
        for (String glyph : QUICK) {
            String aliases = searchAliases(glyph);
            result.add(new EmojiEntry(glyph, aliases.isEmpty() ? glyph : aliases));
        }
        return result;
    }

    /** Accepts a single emoji inserted by any Android keyboard, but only if it is a product entry. */
    public static String matchProductEmoji(Context context, CharSequence rawValue) {
        if (rawValue == null) return null;
        String selected = rawValue.toString().trim();
        if (selected.isEmpty()) return null;
        for (EmojiEntry entry : all(context)) {
            if (entry.emoji.equals(selected)) return entry.emoji;
        }
        return null;
    }

    /** Searches the cached product-only Unicode catalog without disk access or repeated normalization. */
    public static List<EmojiEntry> search(Context context, String query, int limit) {
        String q = normalize(query);
        List<EmojiEntry> source = cachedAll;
        if (source == null) return quick(context);
        List<EmojiEntry> result = new ArrayList<>();
        if (q.isEmpty()) return quick(context);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        for (EmojiEntry entry : source) {
            if (entry.searchable.contains(q)) {
                result.add(entry);
                if (result.size() >= safeLimit) break;
            }
        }
        return result;
    }

    private static String searchAliases(String glyph) {
        if ("🫘".equals(glyph)) return "feijao feijoes graos leguminosas";
        if ("🍚".equals(glyph)) return "arroz arroz cozido arroz branco";
        if ("📺".equals(glyph)) return "tv televisao televisão";
        if ("💻".equals(glyph)) return "notebook computador laptop pc";
        if ("🧴".equals(glyph)) return "detergente desinfetante agua sanitaria shampoo xampu loção";
        if ("🧼".equals(glyph)) return "sabao sabonete sabão";
        if ("🧽".equals(glyph)) return "esponja limpeza";
        if ("🔋".equals(glyph)) return "bateria pilha pilhas";
        if ("🧹".equals(glyph)) return "vassoura limpeza";
        if ("🧻".equals(glyph)) return "papel higienico papel toalha guardanapo";
        if ("🪥".equals(glyph)) return "escova de dente pasta de dente higiene bucal";
        if ("📱".equals(glyph)) return "celular smartphone telefone tablet";
        if ("🎧".equals(glyph)) return "fone fone de ouvido headphone headset";
        if ("🔌".equals(glyph)) return "carregador cabo usb tomada adaptador";
        if ("🍗".equals(glyph)) return "frango galinha";
        if ("🥩".equals(glyph)) return "carne bife carne moida";
        if ("🐟".equals(glyph)) return "peixe tilapia sardinha";
        if ("🥛".equals(glyph)) return "leite iogurte creme de leite";
        if ("🧸".equals(glyph)) return "pelucia ursinho brinquedo";
        if ("📦".equals(glyph)) return "caixa embalagem pacote";
        return "";
    }

    /** Lightweight on-device product recognition: synonyms + accents + one/two-letter typo tolerance. */
    public static String automatic(String value) {
        String s = normalize(value);
        if (s.isEmpty()) return StickerCatalog.PREFIX + "OUTRO";
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

}
