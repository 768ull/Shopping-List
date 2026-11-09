package br.com.shoppinglist.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Offline, hand-drawn-style category badges for products without a suitable emoji. */
public final class StickerCatalog {
    public static final String PREFIX = "sticker:";
    public static final class Sticker {
        public final String id;
        public final String title;
        public final String badge;
        public final int background;
        public final int foreground;

        Sticker(String id, String title, String badge, int background, int foreground) {
            this.id = id;
            this.title = title;
            this.badge = badge;
            this.background = background;
            this.foreground = foreground;
        }
        public String value() { return PREFIX + id; }
    }

    private static final Sticker[] ITEMS = {
        new Sticker("OUTRO", "Produto sem categoria", "🛍️", 0xFFE9E2D5, 0xFF594B38),
        new Sticker("ALIMENTO", "Alimentos e ingredientes", "🍎", 0xFFFFE4B8, 0xFF8B4A18),
        new Sticker("LINGUICA", "Linguiça", "🌭", 0xFFFFE2C2, 0xFF8D4527),
        new Sticker("SALAME", "Salame", "🍖", 0xFFFFD9D2, 0xFF923D37),
        new Sticker("SALSICHA", "Salsicha", "🌭", 0xFFFFE7CB, 0xFF9A5524),
        new Sticker("PRESUNTO", "Presunto", "🍖", 0xFFFFE2D0, 0xFF96533D),
        new Sticker("MORTADELA", "Mortadela", "🍖", 0xFFFFDCE0, 0xFF8B3E52),
        new Sticker("LIMPEZA", "Limpeza", "🧹", 0xFFDDF2EA, 0xFF176B51),
        new Sticker("ELETRONICO", "Eletrônicos e acessórios", "📱", 0xFFDDEBFF, 0xFF28558F),
        new Sticker("ROUPA", "Roupas e calçados", "👕", 0xFFFFDFE8, 0xFF963D5C),
        new Sticker("BELEZA", "Higiene e beleza", "🧴", 0xFFF2DFF5, 0xFF76417F),
        new Sticker("PAPELARIA", "Papelaria e escritório", "✏️", 0xFFE4E8FF, 0xFF444D91),
        new Sticker("FERRAMENTA", "Ferramentas", "🔧", 0xFFFFE8D2, 0xFF88501F),
        new Sticker("CASA", "Casa e utilidades", "🏠", 0xFFFFE9D6, 0xFF89552B),
        new Sticker("COZINHA", "Cozinha e utensílios", "🍳", 0xFFFFE8B8, 0xFF805516),
        new Sticker("PET", "Produtos para animais", "🐾", 0xFFE4F1D9, 0xFF3F6A32),
        new Sticker("BRINQUEDO", "Brinquedos e jogos", "🧸", 0xFFFFE1B5, 0xFF93541C),
        new Sticker("AUTOMOVEL", "Carro e moto", "🚗", 0xFFDCEBEE, 0xFF245A65),
        new Sticker("BEBE", "Bebês e crianças", "🍼", 0xFFFFE3E8, 0xFF9B475B),
        new Sticker("ESPORTE", "Esportes e lazer", "⚽", 0xFFDFF0D9, 0xFF3F6D38),
        new Sticker("JARDIM", "Jardim e plantas", "🪴", 0xFFDDF0D8, 0xFF376B36),
        new Sticker("MOVEIS", "Móveis e decoração", "🛋️", 0xFFEADFCF, 0xFF70543A),
        new Sticker("FARMACIA", "Farmácia e cuidados", "💊", 0xFFFFE0DC, 0xFF98443B),
        new Sticker("CONSTRUCAO", "Construção e materiais", "🧱", 0xFFFFE7C8, 0xFF8B5B26),
        new Sticker("PRESENTE", "Presentes e acessórios", "🎁", 0xFFF6DFF0, 0xFF81436D)
    };

    private StickerCatalog() {}

    public static List<Sticker> all() {
        List<Sticker> result = new ArrayList<>();
        Collections.addAll(result, ITEMS);
        return result;
    }

    public static boolean isSticker(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static Sticker fromValue(String value) {
        if (!isSticker(value)) return null;
        String id = value.substring(PREFIX.length());
        for (Sticker item : ITEMS) if (item.id.equals(id)) return item;
        return ITEMS[0];
    }

    public static String forText(String value) {
        String s = normalize(value);
        if (s.isEmpty()) return ITEMS[0].value();
        if (hasAny(s, "linguica", "linguica calabresa", "linguica toscana")) return "sticker:LINGUICA";
        if (hasAny(s, "salame")) return "sticker:SALAME";
        if (hasAny(s, "salsicha")) return "sticker:SALSICHA";
        if (hasAny(s, "presunto", "apresuntado")) return "sticker:PRESUNTO";
        if (hasAny(s, "mortadela")) return "sticker:MORTADELA";
        if (hasAny(s, "detergente", "desinfetante", "agua sanitaria", "agua sanitaria", "agua sanitaria",
                "limpador", "desengordurante", "amaciante", "sabao em po", "sabao liquido",
                "alvejante", "amaciante", "lustra moveis", "limpa vidro", "cera", "polidor", "vassoura", "rodo", "balde", "pano de chao", "pano de limpeza", "saco de lixo", "inseticida", "luva de limpeza", "escova de limpeza", "produto de limpeza", "material de limpeza"))
            return "sticker:LIMPEZA";
        if (hasAny(s, "celular", "telefone", "smartphone", "televisao", "tv", "computador",
                "notebook", "laptop", "tablet", "mouse", "teclado", "carregador", "cabo usb",
                "tomada", "fone", "headphone", "caixa de som", "camera", "roteador", "console", "cabo hdmi", "controle remoto", "webcam", "microfone", "smartwatch", "monitor", "capinha de celular", "pelicula de celular", "cartao de memoria",
                "video game", "controle", "pilha", "bateria", "pendrive", "memoria", "hd externo", "eletronico", "eletronica", "eletronicos", "eletronicas", "acessorio eletronico"))
            return "sticker:ELETRONICO";
        if (hasAny(s, "camiseta", "calca", "bermuda", "vestido", "saia", "blusa", "camisa",
                "meia", "cueca", "calcinha", "sutia", "tenis", "sapato", "chinelo", "sandalia",
                "jaqueta", "casaco", "bone", "cinto", "roupa"))
            return "sticker:ROUPA";
        if (hasAny(s, "shampoo", "xampu", "condicionador", "creme dental", "pasta de dente",
                "escova de dente", "fio dental", "desodorante", "perfume", "hidratante",
                "protetor solar", "maquiagem", "batom", "esmalte", "sabonete", "absorvente", "pente", "escova de cabelo", "pinça", "algodao", "acetona", "lixa de unha", "removedor de esmalte",
                "fralda", "barbeador", "lamina de barbear"))
            return "sticker:BELEZA";
        if (hasAny(s, "caderno", "material escolar", "papelaria", "lapis", "caneta", "borracha escolar", "apontador", "marca texto",
                "marcador", "papel sulfite", "folha a4", "grampeador", "clipe", "cola escolar",
                "tesoura", "regua", "pasta escolar", "agenda", "livro", "etiqueta"))
            return "sticker:PAPELARIA";
        if (hasAny(s, "martelo", "chave de fenda", "chave philips", "furadeira", "parafuso",
                "prego", "porca", "arruela", "alicate", "serrote", "trena", "fita isolante",
                "lixa", "broca", "chave inglesa", "ferramenta"))
            return "sticker:FERRAMENTA";
        if (hasAny(s, "copo", "prato", "talher", "garfo", "faca", "colher", "panela", "frigideira",
                "assadeira", "peneira", "escorredor", "pote", "marmita", "garrafa", "caneca",
                "tabua de cortar", "utensilio"))
            return "sticker:COZINHA";
        if (hasAny(s, "racao", "areia de gato", "brinquedo para cachorro", "coleira", "guia",
                "comedouro", "bebedouro", "petisco", "shampoo para cachorro", "tapete higienico"))
            return "sticker:PET";
        if (hasAny(s, "boneca", "carrinho", "lego", "quebra cabeca", "quebra-cabeca", "jogo de tabuleiro",
                "bola infantil", "brinquedo", "pelucia", "puzzle"))
            return "sticker:BRINQUEDO";
        if (hasAny(s, "oleo de motor", "pneu", "calota", "limpa vidro automotivo", "palheta",
                "capacete", "corrente de moto", "vela de ignicao", "acessorio automotivo"))
            return "sticker:AUTOMOVEL";
        if (hasAny(s, "mamadeira", "chupeta", "berco", "carrinho de bebe", "pomada para assadura",
                "lenço umedecido", "lenco umedecido", "papinha", "bico de mamadeira"))
            return "sticker:BEBE";
        if (hasAny(s, "bola", "raquete", "halter", "peso de academia", "tapete de yoga",
                "bicicleta", "capacete de bicicleta", "corda de pular", "artigo esportivo"))
            return "sticker:ESPORTE";
        if (hasAny(s, "planta", "vaso", "muda", "terra vegetal", "adubo", "semente", "regador",
                "mangueira de jardim", "fertilizante", "jardim"))
            return "sticker:JARDIM";
        if (hasAny(s, "sofa", "cadeira", "mesa", "estante", "cama", "colchao", "travesseiro",
                "cortina", "tapete", "almofada", "luminaria", "abajur", "decoracao"))
            return "sticker:MOVEIS";
        if (hasAny(s, "remedio", "medicamento", "termometro", "curativo", "esparadrapo",
                "vitamina", "algodao", "alcool 70", "mascara descartavel"))
            return "sticker:FARMACIA";
        if (hasAny(s, "cimento", "tinta", "tijolo", "areia de construcao", "rejunte", "cimento",
                "torneira", "cano pvc", "conexao pvc", "fita veda rosca", "material de obra"))
            return "sticker:CONSTRUCAO";
        if (hasAny(s, "presente", "cartao", "cartao presente", "embalagem", "papel de presente",
                "chaveiro", "bolsa", "mochila", "carteira", "oculos"))
            return "sticker:PRESENTE";
        if (hasAny(s, "arroz", "feijao", "macarrao", "farinha", "acucar", "sal", "fruta",
                "verdura", "legume", "carne", "leite", "queijo", "pao", "biscoito", "cafe",
                "suco", "refrigerante", "tempero", "alimento", "alimentos", "comida", "manteiga", "margarina", "acucar", "farinha", "oleo", "vinagre", "cereal", "aveia", "granola", "fuba", "farofa", "molho", "geleia", "gelatina", "fermento", "achocolatado", "condimento", "maionese", "ketchup", "mostarda", "azeitona", "palmito", "atum", "salame", "mortadela", "salmao", "pasta de amendoim", "leite de coco", "mandioca", "inhame"))
            return "sticker:ALIMENTO";
        if (hasAny(s, "papel higienico", "papel toalha", "guardanapo", "lixeira", "escova de limpeza",
                "esponja", "sabao", "limpeza"))
            return "sticker:LIMPEZA";
        if (hasAny(s, "lampada", "pilha", "fio", "extensao", "adaptador", "interruptor"))
            return "sticker:ELETRONICO";
        if (hasAny(s, "panela", "pote", "prato", "copo", "talher", "cozinha"))
            return "sticker:COZINHA";
        return ITEMS[0].value();
    }

    private static boolean hasAny(String text, String... words) {
        String padded = " " + text + " ";
        for (String word : words) {
            String normalized = normalize(word);
            if (!normalized.isEmpty() && padded.contains(" " + normalized + " ")) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return s.replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
