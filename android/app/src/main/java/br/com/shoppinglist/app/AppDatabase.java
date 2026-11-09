package br.com.shoppinglist.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Local-only storage. SQLite TEXT fields have no app-imposed character cap. */
public final class AppDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "shopping_list_local.db";
    private static final int DB_VERSION = 1;

    public static final class ShoppingList {
        public long id;
        public String title;
        public boolean pinned;
        public int itemCount;
        public long updatedAt;
    }

    public static final class ShoppingItem {
        public long id;
        public long listId;
        public String text;
        public String emoji;
        public boolean checked;
        public boolean emojiManual;
    }

    public AppDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE lists (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL DEFAULT 'Minha lista'," +
                "pinned INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE items (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "list_id INTEGER NOT NULL," +
                "text TEXT NOT NULL DEFAULT ''," +
                "emoji TEXT NOT NULL DEFAULT '🛒'," +
                "checked INTEGER NOT NULL DEFAULT 0," +
                "emoji_manual INTEGER NOT NULL DEFAULT 0," +
                "position INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(list_id) REFERENCES lists(_id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_items_list_position ON items(list_id, position, _id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first public format. Future schema changes must be additive/migrated.
    }

    public long createList(String title) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("title", cleanTitle(title));
        values.put("created_at", now);
        values.put("updated_at", now);
        long id = db.insertOrThrow("lists", null, values);
        addItem(id, "", "🛒", false, 0);
        return id;
    }

    public long addItem(long listId, String text, String emoji, boolean manual, int position) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("list_id", listId);
        values.put("text", text == null ? "" : text);
        values.put("emoji", emoji == null || emoji.isEmpty() ? "🛒" : emoji);
        values.put("emoji_manual", manual ? 1 : 0);
        values.put("position", position);
        long id = db.insertOrThrow("items", null, values);
        touchList(listId);
        return id;
    }

    public void updateTitle(long listId, String title) {
        ContentValues values = new ContentValues();
        values.put("title", title == null ? "" : title);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("lists", values, "_id=?", new String[]{String.valueOf(listId)});
    }

    public void setPinned(long listId, boolean pinned) {
        ContentValues values = new ContentValues();
        values.put("pinned", pinned ? 1 : 0);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("lists", values, "_id=?", new String[]{String.valueOf(listId)});
    }

    public void setItemText(long itemId, String text) {
        ContentValues values = new ContentValues();
        values.put("text", text == null ? "" : text);
        getWritableDatabase().update("items", values, "_id=?", new String[]{String.valueOf(itemId)});
        touchListForItem(itemId);
    }

    public void setItemChecked(long itemId, boolean checked) {
        ContentValues values = new ContentValues();
        values.put("checked", checked ? 1 : 0);
        getWritableDatabase().update("items", values, "_id=?", new String[]{String.valueOf(itemId)});
        touchListForItem(itemId);
    }

    public void setItemEmoji(long itemId, String emoji, boolean manual) {
        ContentValues values = new ContentValues();
        values.put("emoji", emoji == null || emoji.isEmpty() ? "🛒" : emoji);
        values.put("emoji_manual", manual ? 1 : 0);
        getWritableDatabase().update("items", values, "_id=?", new String[]{String.valueOf(itemId)});
    }

    public void deleteItem(long itemId) {
        getWritableDatabase().delete("items", "_id=?", new String[]{String.valueOf(itemId)});
    }

    public void deleteList(long listId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("items", "list_id=?", new String[]{String.valueOf(listId)});
            db.delete("lists", "_id=?", new String[]{String.valueOf(listId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<ShoppingList> getLists() {
        List<ShoppingList> result = new ArrayList<>();
        String sql = "SELECT l._id,l.title,l.pinned,l.updated_at,COUNT(CASE WHEN TRIM(i.text) <> '' THEN 1 END) AS item_count " +
                "FROM lists l LEFT JOIN items i ON i.list_id=l._id " +
                "GROUP BY l._id ORDER BY l.pinned DESC,l.updated_at DESC,l._id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) {
                ShoppingList list = new ShoppingList();
                list.id = c.getLong(0);
                list.title = c.getString(1);
                list.pinned = c.getInt(2) != 0;
                list.updatedAt = c.getLong(3);
                list.itemCount = c.getInt(4);
                result.add(list);
            }
        }
        return result;
    }

    public ShoppingList getList(long listId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,title,pinned,updated_at,(SELECT COUNT(*) FROM items WHERE list_id=lists._id AND TRIM(text) <> '') " +
                        "FROM lists WHERE _id=?", new String[]{String.valueOf(listId)})) {
            if (!c.moveToFirst()) return null;
            ShoppingList list = new ShoppingList();
            list.id = c.getLong(0);
            list.title = c.getString(1);
            list.pinned = c.getInt(2) != 0;
            list.updatedAt = c.getLong(3);
            list.itemCount = c.getInt(4);
            return list;
        }
    }

    public List<ShoppingItem> getItems(long listId) {
        List<ShoppingItem> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(
                "items", new String[]{"_id", "list_id", "text", "emoji", "checked", "emoji_manual"},
                "list_id=?", new String[]{String.valueOf(listId)}, null, null, "position ASC,_id ASC")) {
            while (c.moveToNext()) {
                ShoppingItem item = new ShoppingItem();
                item.id = c.getLong(0);
                item.listId = c.getLong(1);
                item.text = c.getString(2);
                item.emoji = c.getString(3);
                item.checked = c.getInt(4) != 0;
                item.emojiManual = c.getInt(5) != 0;
                result.add(item);
            }
        }
        return result;
    }

    public void reorderItems(long listId) {
        List<ShoppingItem> items = getItems(listId);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < items.size(); i++) {
                ContentValues values = new ContentValues();
                values.put("position", i);
                db.update("items", values, "_id=?", new String[]{String.valueOf(items.get(i).id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void touchListForItem(long itemId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT list_id FROM items WHERE _id=?",
                new String[]{String.valueOf(itemId)})) {
            if (c.moveToFirst()) touchList(c.getLong(0));
        }
    }

    private void touchList(long listId) {
        ContentValues values = new ContentValues();
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("lists", values, "_id=?", new String[]{String.valueOf(listId)});
    }

    /** Exporta uma cópia portátil das listas, itens, estados e emojis. */
    public JSONObject exportBackup() throws JSONException {
        JSONObject backup = new JSONObject();
        backup.put("format", "shopping-lists-backup");
        backup.put("schemaVersion", 1);
        backup.put("exportedAt", System.currentTimeMillis());

        JSONArray listsJson = new JSONArray();
        for (ShoppingList list : getLists()) {
            JSONObject listJson = new JSONObject();
            listJson.put("title", list.title == null ? "Minha lista" : list.title);
            listJson.put("pinned", list.pinned);
            listJson.put("updatedAt", list.updatedAt);

            JSONArray itemsJson = new JSONArray();
            List<ShoppingItem> items = getItems(list.id);
            for (int index = 0; index < items.size(); index++) {
                ShoppingItem item = items.get(index);
                JSONObject itemJson = new JSONObject();
                itemJson.put("text", item.text == null ? "" : item.text);
                itemJson.put("emoji", item.emoji == null ? "🛒" : item.emoji);
                itemJson.put("checked", item.checked);
                itemJson.put("emojiManual", item.emojiManual);
                itemJson.put("position", index);
                itemsJson.put(itemJson);
            }
            listJson.put("items", itemsJson);
            listsJson.put(listJson);
        }
        backup.put("lists", listsJson);
        return backup;
    }

    /** Importa um backup sem apagar as listas que já existem neste aparelho. */
    public int importBackup(JSONObject backup) throws JSONException {
        if (backup == null
                || !"shopping-lists-backup".equals(backup.optString("format"))
                || backup.optInt("schemaVersion", -1) != 1) {
            throw new JSONException("Este arquivo não é um backup válido do Shopping Lists.");
        }

        JSONArray listsJson = backup.optJSONArray("lists");
        if (listsJson == null || listsJson.length() == 0) {
            throw new JSONException("O backup não contém listas para importar.");
        }
        if (listsJson.length() > 1000) {
            throw new JSONException("O backup contém listas demais.");
        }

        int totalItems = 0;
        for (int i = 0; i < listsJson.length(); i++) {
            JSONObject listJson = listsJson.optJSONObject(i);
            if (listJson == null || listJson.optJSONArray("items") == null) {
                throw new JSONException("O backup contém uma lista inválida.");
            }
            totalItems += listJson.optJSONArray("items").length();
            if (totalItems > 50000) {
                throw new JSONException("O backup contém produtos demais.");
            }
        }

        SQLiteDatabase db = getWritableDatabase();
        int importedLists = 0;
        db.beginTransaction();
        try {
            for (int i = 0; i < listsJson.length(); i++) {
                JSONObject listJson = listsJson.getJSONObject(i);
                String title = cleanTitle(listJson.optString("title", "Minha lista"));
                long now = System.currentTimeMillis();
                long updatedAt = Math.max(0L, listJson.optLong("updatedAt", now));

                ContentValues listValues = new ContentValues();
                listValues.put("title", title);
                listValues.put("pinned", listJson.optBoolean("pinned", false) ? 1 : 0);
                listValues.put("created_at", now);
                listValues.put("updated_at", updatedAt);
                long newListId = db.insertOrThrow("lists", null, listValues);

                JSONArray itemsJson = listJson.getJSONArray("items");
                for (int j = 0; j < itemsJson.length(); j++) {
                    JSONObject itemJson = itemsJson.getJSONObject(j);
                    String emoji = itemJson.optString("emoji", "🛒");
                    if (emoji.trim().isEmpty()) emoji = "🛒";

                    ContentValues itemValues = new ContentValues();
                    itemValues.put("list_id", newListId);
                    itemValues.put("text", itemJson.optString("text", ""));
                    itemValues.put("emoji", emoji);
                    itemValues.put("checked", itemJson.optBoolean("checked", false) ? 1 : 0);
                    itemValues.put("emoji_manual", itemJson.optBoolean("emojiManual", false) ? 1 : 0);
                    itemValues.put("position", Math.max(0, itemJson.optInt("position", j)));
                    db.insertOrThrow("items", null, itemValues);
                }
                importedLists++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return importedLists;
    }

    private static String cleanTitle(String value) {
        String title = value == null ? "" : value.trim();
        return title.isEmpty() ? "Minha lista" : title;
    }
}
