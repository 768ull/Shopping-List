package br.com.shoppinglist.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.BreakIterator;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Stores emojis entered through the user's Android keyboard.
 *
 * The saved collection is local to this installation and is independent from
 * the curated product catalog and from the built-in illustrated sticker set.
 */
public final class PersonalEmojiCatalog {
    private static final String PREFS_NAME = "shopping_list_preferences";
    private static final String PREF_KEY = "personal_keyboard_emojis_v1";
    private static final int MAX_SAVED_EMOJIS = 120;

    private PersonalEmojiCatalog() { }

    /**
     * Adds every emoji grapheme found in the supplied text to the beginning of
     * the personal collection. New entries are kept first and duplicates move
     * to the front, so the collection behaves like a persistent recent list.
     *
     * @return the unique emojis extracted from this text, in typing order.
     */
    public static synchronized List<String> remember(Context context, CharSequence input) {
        List<String> extracted = extractEmojis(input);
        if (extracted.isEmpty()) return extracted;

        List<String> saved = getAll(context);
        for (String emoji : extracted) {
            saved.remove(emoji);
            saved.add(0, emoji);
        }
        while (saved.size() > MAX_SAVED_EMOJIS) {
            saved.remove(saved.size() - 1);
        }

        JSONArray json = new JSONArray();
        for (String emoji : saved) json.put(emoji);
        preferences(context).edit().putString(PREF_KEY, json.toString()).apply();
        return extracted;
    }

    /** Returns the locally saved emojis, newest first. */
    public static synchronized List<String> getAll(Context context) {
        String raw = preferences(context).getString(PREF_KEY, "[]");
        List<String> result = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(raw);
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (int i = 0; i < json.length(); i++) {
                String emoji = json.optString(i, "");
                if (isEmojiGrapheme(emoji)) unique.add(emoji);
            }
            result.addAll(unique);
        } catch (Exception ignored) {
            // A malformed personal catalog should never prevent the product editor from opening.
        }
        return result;
    }

    /** Splits text into Unicode grapheme clusters and returns only emoji clusters. */
    static List<String> extractEmojis(CharSequence input) {
        List<String> result = new ArrayList<>();
        if (input == null || input.length() == 0) return result;

        String value = input.toString();
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(value);
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String grapheme = value.substring(start, end);
            if (isEmojiGrapheme(grapheme)) unique.add(grapheme);
        }
        result.addAll(unique);
        return result;
    }

    private static boolean isEmojiGrapheme(String value) {
        if (value == null || value.isEmpty()) return false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (isEmojiBase(codePoint) || codePoint == 0x20E3) return true;
            offset += Character.charCount(codePoint);
        }
        return false;
    }

    private static boolean isEmojiBase(int codePoint) {
        // Supplementary emoji, flags, pictographs and the common symbol blocks.
        if (codePoint >= 0x1F000 && codePoint <= 0x1FAFF
                && !(codePoint >= 0x1F3FB && codePoint <= 0x1F3FF)) return true;
        if (codePoint >= 0x2600 && codePoint <= 0x27BF) return true;
        if (codePoint >= 0x2300 && codePoint <= 0x23FF) return true;
        if (codePoint >= 0x2B00 && codePoint <= 0x2BFF) return true;
        // Emoji-capable symbols that normally use U+FE0F on phone keyboards.
        return codePoint == 0x00A9 || codePoint == 0x00AE
                || codePoint == 0x203C || codePoint == 0x2049
                || codePoint == 0x2122 || codePoint == 0x2139
                || codePoint == 0x24C2 || codePoint == 0x3030
                || codePoint == 0x303D || codePoint == 0x3297
                || codePoint == 0x3299;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
