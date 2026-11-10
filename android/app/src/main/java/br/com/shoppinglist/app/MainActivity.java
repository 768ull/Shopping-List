package br.com.shoppinglist.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import android.graphics.pdf.PdfDocument;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends android.app.Activity {
    private static final int REQUEST_PDF = 4101;
    private static final int REQUEST_IMAGE_FOLDER = 4102;
    private static final int REQUEST_UNKNOWN_SOURCES = 4103;
    private static final int REQUEST_BACKUP_EXPORT = 4104;
    private static final int REQUEST_BACKUP_IMPORT = 4105;
    private static final int MAX_BACKUP_BYTES = 40 * 1024 * 1024;
    private static final int MAX_STICKER_BACKUP_BYTES = 4 * 1024 * 1024;
    private java.io.File pendingUpdateApk;

    private static final int BG_YELLOW = Color.rgb(255, 239, 174);
    private static final int BG_YELLOW_LIGHT = Color.rgb(255, 247, 214);
    private static final int SUNFLOWER = Color.rgb(255, 198, 36);
    private static final int ORANGE = Color.rgb(235, 126, 35);
    private static final int INK = Color.rgb(76, 55, 31);
    private static final int MUTED = Color.rgb(128, 105, 72);
    private static final int PAPER = Color.rgb(255, 254, 246);
    private static final int GREEN = Color.rgb(71, 148, 84);

    private AppDatabase database;
    private FrameLayout root;
    private long currentListId = -1;
    private EditText titleEditor;
    private NotebookPaperLayout notebook;
    private boolean autoEmoji = true;
    private boolean changingTitle = false;
    private PendingExport pendingExport;
    private final Handler productAiHandler = new Handler(Looper.getMainLooper());
    private final Map<Long, Runnable> pendingProductAi = new HashMap<>();

    private static final class PendingExport {
        String title;
        List<ShoppingListRenderer.ExportPage> pages;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new AppDatabase(this);
        SharedPreferences prefs = getSharedPreferences("shopping_list_preferences", MODE_PRIVATE);
        autoEmoji = prefs.getBoolean("automatic_emojis", true);
        EmojiCatalogUpdater.check(getApplicationContext());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        root = new FrameLayout(this);
        setContentView(root);
        applyImmersiveMode();
        showHome();
    }

    @Override
    protected void onDestroy() {
        for (Runnable pending : pendingProductAi.values()) {
            productAiHandler.removeCallbacks(pending);
        }
        pendingProductAi.clear();
        super.onDestroy();
    }

    private void applyImmersiveMode() {
        View decor = getWindow().getDecorView();
        // Keep the navigation bar immersive, but do not draw edge-to-edge or
        // hide the status bar: fullscreen/layout flags prevent adjustResize
        // from shrinking the editor when the Android keyboard is displayed.
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decor.setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersiveMode();
    }

    private void showHome() {
        currentListId = -1;
        root.removeAllViews();
        root.setBackgroundColor(BG_YELLOW);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(20), dp(18), dp(34));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(2), dp(2), dp(2), dp(14));
        ImageView icon = new ImageView(this);
        icon.setImageResource(br.com.shoppinglist.app.R.mipmap.ic_launcher_custom);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.addView(icon, new LinearLayout.LayoutParams(dp(76), dp(76)));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setPadding(dp(12), 0, 0, 0);
        TextView name = label("Shopping Lists", 27, INK, true);
        heading.addView(name);
        TextView tagline = label("Seu caderno de compras", 14, MUTED, false);
        heading.addView(tagline);
        TextView offline = label("●  FUNCIONA OFFLINE", 10, GREEN, true);
        offline.setPadding(0, dp(6), 0, 0);
        heading.addView(offline);
        hero.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        content.addView(hero);

        TextView intro = label("Tudo o que você precisa, em um só lugar.", 15, INK, false);
        intro.setPadding(dp(4), 0, 0, dp(12));
        content.addView(intro);

        TextView checkUpdates = action("↻   VERIFICAR ATUALIZAÇÕES", INK, BG_YELLOW_LIGHT, 12, true);
        checkUpdates.setGravity(Gravity.CENTER);
        checkUpdates.setPadding(dp(12), dp(12), dp(12), dp(12));
        checkUpdates.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(14), Color.rgb(240, 213, 145), dp(1)));
        LinearLayout.LayoutParams updateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        updateParams.bottomMargin = dp(12);
        content.addView(checkUpdates, updateParams);
        checkUpdates.setOnClickListener(v -> checkForUpdates());

        LinearLayout backupRow = new LinearLayout(this);
        backupRow.setOrientation(LinearLayout.HORIZONTAL);
        backupRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView exportBackup = action("SALVAR BACKUP", INK, BG_YELLOW_LIGHT, 11, true);
        exportBackup.setGravity(Gravity.CENTER);
        exportBackup.setPadding(dp(8), dp(13), dp(8), dp(13));
        exportBackup.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(14), Color.rgb(240, 213, 145), dp(1)));
        TextView importBackup = action("RESTAURAR BACKUP", INK, BG_YELLOW_LIGHT, 11, true);
        importBackup.setGravity(Gravity.CENTER);
        importBackup.setPadding(dp(8), dp(13), dp(8), dp(13));
        importBackup.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(14), Color.rgb(240, 213, 145), dp(1)));
        LinearLayout.LayoutParams backupButtonParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        backupButtonParams.rightMargin = dp(6);
        backupRow.addView(exportBackup, backupButtonParams);
        LinearLayout.LayoutParams restoreButtonParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        restoreButtonParams.leftMargin = dp(6);
        backupRow.addView(importBackup, restoreButtonParams);
        LinearLayout.LayoutParams backupRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backupRowParams.bottomMargin = dp(12);
        content.addView(backupRow, backupRowParams);
        exportBackup.setOnClickListener(v -> chooseBackupExport());
        importBackup.setOnClickListener(v -> chooseBackupImport());

        TextView create = action("＋   CRIAR LISTA NOVA", Color.WHITE, ORANGE, 18, true);
        create.setGravity(Gravity.CENTER);
        create.setPadding(dp(16), dp(19), dp(16), dp(19));
        create.setBackground(roundDrawable(ORANGE, dp(22), 0, 0));
        create.setElevation(dp(4));
        create.setOnClickListener(v -> {
            long id = database.createList("Minha lista");
            showEditor(id, true);
        });
        LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        createParams.bottomMargin = dp(24);
        content.addView(create, createParams);

        LinearLayout section = new LinearLayout(this);
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setOrientation(LinearLayout.HORIZONTAL);
        TextView saved = label("Minhas listas", 20, INK, true);
        section.addView(saved);
        View flex = new View(this);
        section.addView(flex, new LinearLayout.LayoutParams(0, 1, 1));
        TextView count = label("SALVAS NO APARELHO", 10, MUTED, true);
        section.addView(count);
        content.addView(section);

        List<AppDatabase.ShoppingList> lists = database.getLists();
        if (lists.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(22), dp(32), dp(22), dp(32));
            empty.setBackground(roundDrawable(PAPER, dp(22), Color.rgb(237, 213, 151), dp(1)));
            TextView emptyEmoji = label("🛒", 38, INK, false);
            empty.addView(emptyEmoji);
            TextView emptyText = label("Ainda não há listas por aqui.", 15, INK, true);
            emptyText.setPadding(0, dp(10), 0, dp(4));
            empty.addView(emptyText);
            TextView emptySub = label("Toque em “Criar lista nova” para começar.", 13, MUTED, false);
            empty.addView(emptySub);
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            emptyParams.topMargin = dp(16);
            content.addView(empty, emptyParams);
        } else {
            for (AppDatabase.ShoppingList list : lists) {
                content.addView(makeListCard(list));
            }
        }

        TextView footer = label("As listas ficam guardadas somente neste aparelho.", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(22), 0, 0);
        content.addView(footer);
    }

    private View makeListCard(AppDatabase.ShoppingList list) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(12), dp(12));
        card.setBackground(roundDrawable(PAPER, dp(18), Color.rgb(237, 213, 151), dp(1)));
        card.setElevation(dp(2));

        TextView sticker = label("📝", 28, INK, false);
        card.addView(sticker, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        TextView title = label(displayTitle(list.title), 17, INK, true);
        details.addView(title);
        String detail = list.itemCount + (list.itemCount == 1 ? " produto" : " produtos")
                + "  •  " + shortDate(list.updatedAt);
        TextView subtitle = label(detail, 12, MUTED, false);
        subtitle.setPadding(0, dp(3), 0, 0);
        details.addView(subtitle);
        card.addView(details, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView pin = label(list.pinned ? "📌" : "›", list.pinned ? 21 : 30,
                list.pinned ? ORANGE : MUTED, true);
        pin.setGravity(Gravity.CENTER);
        card.addView(pin, new LinearLayout.LayoutParams(dp(38), dp(44)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(10);
        card.setLayoutParams(params);
        card.setOnClickListener(v -> showEditor(list.id, false));
        return card;
    }

    private void showEditor(long listId, boolean focusFirstItem) {
        AppDatabase.ShoppingList current = database.getList(listId);
        if (current == null) {
            showHome();
            return;
        }
        currentListId = listId;
        root.removeAllViews();
        root.setBackgroundColor(BG_YELLOW);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(12), dp(10), dp(12), dp(12));
        root.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹  VOLTAR", INK, BG_YELLOW_LIGHT, 13, true);
        back.setPadding(dp(12), dp(11), dp(12), dp(11));
        back.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(15), Color.rgb(240, 213, 145), dp(1)));
        back.setOnClickListener(v -> {
            hideKeyboard();
            showHome();
        });
        top.addView(back);

        TextView appName = label("Shopping Lists", 17, INK, true);
        appName.setGravity(Gravity.CENTER);
        top.addView(appName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView pin = action(current.pinned ? "📌 FIXADA" : "☆ FIXAR", INK,
                current.pinned ? Color.rgb(255, 218, 127) : BG_YELLOW_LIGHT, 12, true);
        pin.setPadding(dp(11), dp(11), dp(11), dp(11));
        pin.setBackground(roundDrawable(current.pinned ? Color.rgb(255, 218, 127) : BG_YELLOW_LIGHT,
                dp(15), Color.rgb(240, 213, 145), dp(1)));
        pin.setOnClickListener(v -> {
            AppDatabase.ShoppingList fresh = database.getList(currentListId);
            if (fresh != null) {
                database.setPinned(currentListId, !fresh.pinned);
                showEditor(currentListId, false);
            }
        });
        top.addView(pin);
        screen.addView(top);

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(0, dp(10), 0, dp(10));
        TextView save = action("✓ SALVO", GREEN, Color.rgb(230, 247, 226), 12, true);
        save.setGravity(Gravity.CENTER);
        save.setPadding(dp(8), dp(10), dp(8), dp(10));
        save.setBackground(roundDrawable(Color.rgb(230, 247, 226), dp(13), 0, 0));
        save.setOnClickListener(v -> Toast.makeText(this, "Sua lista já está salva no aplicativo.", Toast.LENGTH_SHORT).show());
        tools.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView export = action("⇩  EXPORTAR", Color.WHITE, ORANGE, 12, true);
        export.setGravity(Gravity.CENTER);
        export.setPadding(dp(8), dp(10), dp(8), dp(10));
        export.setBackground(roundDrawable(ORANGE, dp(13), 0, 0));
        export.setOnClickListener(v -> showExportDialog());
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.25f);
        exportParams.leftMargin = dp(7);
        tools.addView(export, exportParams);

        TextView delete = action("🗑  EXCLUIR", Color.rgb(151, 70, 50), Color.rgb(255, 231, 219), 12, true);
        delete.setGravity(Gravity.CENTER);
        delete.setPadding(dp(8), dp(10), dp(8), dp(10));
        delete.setBackground(roundDrawable(Color.rgb(255, 231, 219), dp(13), 0, 0));
        delete.setOnClickListener(v -> confirmDeleteList());
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        deleteParams.leftMargin = dp(7);
        tools.addView(delete, deleteParams);
        screen.addView(tools);

        LinearLayout emojiControls = new LinearLayout(this);
        emojiControls.setOrientation(LinearLayout.HORIZONTAL);
        emojiControls.setGravity(Gravity.CENTER_VERTICAL);
        emojiControls.setPadding(dp(3), 0, dp(3), dp(8));
        Switch autoSwitch = new Switch(this);
        autoSwitch.setText("Emojis e adesivos automáticos");
        autoSwitch.setTextColor(INK);
        autoSwitch.setTextSize(13);
        autoSwitch.setChecked(autoEmoji);
        autoSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            autoEmoji = checked;
            getSharedPreferences("shopping_list_preferences", MODE_PRIVATE)
                    .edit().putBoolean("automatic_emojis", checked).apply();
            if (checked) {
                for (AppDatabase.ShoppingItem item : database.getItems(currentListId)) {
                    if (!item.emojiManual) {
                        String guess = EmojiCatalog.automatic(item.text);
                        database.setItemEmoji(item.id, guess, false);
                    }
                }
                showEditor(currentListId, false);
            }
        });
        emojiControls.addView(autoSwitch);
        View switchSpacer = new View(this);
        emojiControls.addView(switchSpacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView help = label("Toque no emoji para trocar", 11, MUTED, false);
        emojiControls.addView(help);
        screen.addView(emojiControls);

        ScrollView paperScroll = new ScrollView(this);
        paperScroll.setFillViewport(true);
        paperScroll.setClipToPadding(false);
        paperScroll.setBackground(roundDrawable(PAPER, dp(20), Color.rgb(237, 213, 151), dp(1)));
        notebook = new NotebookPaperLayout(this);
        paperScroll.addView(notebook, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        screen.addView(paperScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        titleEditor = new EditText(this);
        titleEditor.setText(displayTitle(current.title));
        titleEditor.setSingleLine(true);
        titleEditor.setTextSize(23);
        titleEditor.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        titleEditor.setTextColor(INK);
        titleEditor.setHintTextColor(Color.rgb(175, 154, 117));
        titleEditor.setHint("Nome da lista");
        titleEditor.setPadding(0, dp(4), 0, dp(8));
        titleEditor.setBackgroundColor(Color.TRANSPARENT);
        notebook.addView(titleEditor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView subheading = label("MINHAS COMPRAS", 10, MUTED, true);
        subheading.setLetterSpacing(0.12f);
        subheading.setPadding(0, 0, 0, dp(14));
        notebook.addView(subheading);

        titleEditor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!changingTitle && currentListId != -1) database.updateTitle(currentListId, s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        List<AppDatabase.ShoppingItem> items = database.getItems(listId);
        EditText[] firstEditor = new EditText[1];
        for (AppDatabase.ShoppingItem item : items) {
            View row = makeItemRow(item, firstEditor);
            notebook.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        TextView addItem = action("＋  ADICIONAR PRODUTO", ORANGE, Color.rgb(255, 239, 205), 14, true);
        addItem.setGravity(Gravity.CENTER);
        addItem.setPadding(dp(14), dp(15), dp(14), dp(15));
        addItem.setBackground(roundDrawable(Color.rgb(255, 239, 205), dp(15), Color.rgb(246, 207, 142), dp(1)));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addParams.topMargin = dp(14);
        notebook.addView(addItem, addParams);
        addItem.setOnClickListener(v -> {
            int position = database.getItems(currentListId).size();
            database.addItem(currentListId, "", "🛒", false, position);
            showEditor(currentListId, true);
        });

        TextView note = label("Dica: toque no adesivo para escolher entre emojis e adesivos de categoria.", 11, MUTED, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(12), 0, dp(4));
        notebook.addView(note);

        if (focusFirstItem && firstEditor[0] != null) {
            firstEditor[0].requestFocus();
            firstEditor[0].postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(firstEditor[0], InputMethodManager.SHOW_IMPLICIT);
            }, 220);
        }
    }

    private View makeItemRow(AppDatabase.ShoppingItem item, EditText[] firstEditor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        CheckBox check = new CheckBox(this);
        check.setButtonTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{GREEN, Color.rgb(149, 137, 112)}));
        check.setChecked(item.checked);
        check.setPadding(0, 0, 0, 0);
        row.addView(check, new LinearLayout.LayoutParams(dp(34), dp(48)));
        check.setOnCheckedChangeListener((buttonView, checked) -> database.setItemChecked(item.id, checked));

        TextView sticker = label("", 25, INK, false);
        sticker.setGravity(Gravity.CENTER);
        renderSticker(sticker, item.emoji == null ? "🛒" : item.emoji);
        row.addView(sticker, new LinearLayout.LayoutParams(dp(43), dp(43)));
        sticker.setOnClickListener(v -> showEmojiPicker(item, sticker));

        EditText input = new EditText(this);
        input.setText(item.text == null ? "" : item.text);
        input.setTextSize(17);
        input.setTextColor(item.checked ? MUTED : INK);
        input.setHintTextColor(Color.rgb(175, 154, 117));
        input.setHint("Escreva um produto...");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(7), dp(6), dp(2), dp(6));
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(1);
        input.setMaxLines(8);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        row.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (firstEditor[0] == null && (item.text == null || item.text.isEmpty())) firstEditor[0] = input;

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = s.toString();
                database.setItemText(item.id, value);
                item.text = value;
                if (autoEmoji && !item.emojiManual) {
                    String detected = EmojiCatalog.automatic(value);
                    if (!detected.equals(item.emoji)) {
                        item.emoji = detected;
                        database.setItemEmoji(item.id, detected, false);
                        renderSticker(sticker, detected);
                    }
                    scheduleProductAi(item, input, sticker, value, detected);
                } else {
                    cancelProductAi(item.id);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        // Reclassifica itens já salvos e retoma a IA semântica ao abrir uma lista.
        if (autoEmoji && !item.emojiManual && item.text != null && item.text.trim().length() >= 3) {
            String refreshed = EmojiCatalog.automatic(item.text);
            if (!refreshed.equals(item.emoji)) {
                item.emoji = refreshed;
                database.setItemEmoji(item.id, refreshed, false);
                renderSticker(sticker, refreshed);
            }
            scheduleProductAi(item, input, sticker, item.text, refreshed);
        }

        TextView remove = label("×", 23, Color.rgb(178, 104, 77), true);
        remove.setGravity(Gravity.CENTER);
        row.addView(remove, new LinearLayout.LayoutParams(dp(30), dp(43)));
        remove.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Remover produto?")
                .setMessage("Este item será retirado da lista.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Remover", (dialog, which) -> {
                    database.deleteItem(item.id);
                    database.reorderItems(currentListId);
                    hideKeyboard();
                    showEditor(currentListId, false);
                }).show());
        return row;
    }

    private void scheduleProductAi(
            AppDatabase.ShoppingItem item,
            EditText input,
            TextView sticker,
            String rawText,
            String fallbackValue
    ) {
        cancelProductAi(item.id);
        String product = rawText == null ? "" : rawText.trim();
        if (product.length() < 3 || !StickerCatalog.isSticker(fallbackValue)) return;

        Runnable request = () -> {
            pendingProductAi.remove(item.id);
            if (isFinishing() || !autoEmoji || item.emojiManual) return;
            String currentText = input.getText() == null ? "" : input.getText().toString().trim();
            if (!product.equals(currentText)) return;
            if (!StickerCatalog.isSticker(item.emoji)) return;

            WeakReference<MainActivity> activityRef = new WeakReference<>(this);
            WeakReference<EditText> inputRef = new WeakReference<>(input);
            WeakReference<TextView> stickerRef = new WeakReference<>(sticker);
            ProductAiEngine.process(getApplicationContext(), product, new ProductAiEngine.Callback() {
                @Override public void onStatus(String message) {
                    MainActivity activity = activityRef.get();
                    TextView target = stickerRef.get();
                    EditText editor = inputRef.get();
                    if (activity == null || activity.isFinishing() || target == null || editor == null) return;
                    String current = editor.getText() == null ? "" : editor.getText().toString().trim();
                    if (!product.equals(current) || item.emojiManual) return;
                    target.setText("…");
                    target.setTextSize(20);
                    target.setTextColor(ORANGE);
                    target.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
                    target.setCompoundDrawables(null, null, null, null);
                    target.setBackground(roundDrawable(Color.rgb(255, 246, 219), dp(12),
                            Color.rgb(242, 220, 174), dp(1)));
                    target.setContentDescription("IA local trabalhando: " + message);
                }

                @Override public void onResult(String result) {
                    MainActivity activity = activityRef.get();
                    EditText editor = inputRef.get();
                    TextView target = stickerRef.get();
                    if (activity == null || activity.isFinishing() || editor == null) return;
                    String current = editor.getText() == null ? "" : editor.getText().toString().trim();
                    if (!product.equals(current) || item.emojiManual || !activity.autoEmoji) return;
                    item.emoji = result;
                    activity.database.setItemEmoji(item.id, result, false);
                    if (target != null && target.isAttachedToWindow()) {
                        activity.renderSticker(target, result);
                    }
                }

                @Override public void onError(String message) {
                    MainActivity activity = activityRef.get();
                    EditText editor = inputRef.get();
                    TextView target = stickerRef.get();
                    if (activity == null || activity.isFinishing() || editor == null) return;
                    String current = editor.getText() == null ? "" : editor.getText().toString().trim();
                    if (!product.equals(current) || item.emojiManual || !activity.autoEmoji) return;
                    item.emoji = fallbackValue;
                    activity.database.setItemEmoji(item.id, fallbackValue, false);
                    if (target != null && target.isAttachedToWindow()) {
                        activity.renderSticker(target, fallbackValue);
                    }
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                }
            });
        };
        pendingProductAi.put(item.id, request);
        productAiHandler.postDelayed(request, 1400L);
    }

    private void cancelProductAi(long itemId) {
        Runnable pending = pendingProductAi.remove(itemId);
        if (pending != null) productAiHandler.removeCallbacks(pending);
    }

    private void renderSticker(TextView view, String value) {
        if (ProductAiEngine.isGenerated(value)) {
            java.io.File imageFile = ProductAiEngine.generatedFile(this, value);
            Bitmap image = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (image != null) {
                int imageSize = dp(34);
                Bitmap scaled = Bitmap.createScaledBitmap(image, imageSize, imageSize, true);
                if (scaled != image) image.recycle();
                BitmapDrawable drawable = new BitmapDrawable(getResources(), scaled);
                drawable.setBounds(0, 0, imageSize, imageSize);
                view.setText("");
                view.setCompoundDrawablePadding(0);
                view.setCompoundDrawables(null, drawable, null, null);
                view.setBackground(roundDrawable(Color.WHITE, dp(12), Color.rgb(242, 220, 174), dp(1)));
                view.setContentDescription("Adesivo gerado localmente");
                return;
            }
        }
        view.setCompoundDrawables(null, null, null, null);
        StickerCatalog.Sticker custom = StickerCatalog.fromValue(value);
        if (custom != null) {
            view.setText(custom.badge);
            view.setTextSize(22);
            view.setTextColor(custom.foreground);
            view.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            view.setBackground(roundDrawable(custom.background, dp(12), Color.rgb(232, 215, 183), dp(1)));
            view.setContentDescription(custom.title);
        } else {
            view.setText(value == null || value.isEmpty() ? "?" : value);
            view.setTextSize(25);
            view.setTextColor(INK);
            view.setTypeface(Typeface.DEFAULT);
            view.setBackground(roundDrawable(Color.rgb(255, 246, 219), dp(12), Color.rgb(242, 220, 174), dp(1)));
        }
    }

    private void showEmojiPicker(AppDatabase.ShoppingItem item, TextView sticker) {
        EmojiCatalog.preload(this, null);
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(14), dp(14), dp(14), dp(14));
        outer.setBackground(roundDrawable(PAPER, dp(20), Color.rgb(234, 210, 159), dp(1)));

        TextView heading = label("Escolha ou digite um emoji", 18, INK, true);
        heading.setPadding(0, 0, 0, dp(4));
        outer.addView(heading);

        int emojiCount = EmojiCatalogUpdater.getEmojiCount(this);
        TextView catalogInfo = label(
                emojiCount + " emojis de produtos • Unicode " + EmojiCatalogUpdater.getUnicodeVersion(this)
                        + " • seus emojis do teclado ficam salvos neste aparelho",
                10, MUTED, false);
        catalogInfo.setPadding(0, 0, 0, dp(10));
        outer.addView(catalogInfo);

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(INK);
        search.setHintTextColor(MUTED);
        search.setHint("Pesquisar: feijão, bateria, detergente...");
        search.setPadding(dp(12), dp(9), dp(12), dp(9));
        search.setBackground(roundDrawable(Color.rgb(255, 250, 235), dp(12),
                Color.rgb(238, 218, 178), dp(1)));
        outer.addView(search);

        TextView keyboardHelp = label(
                "Use o teclado que preferir: virtual, Bluetooth ou físico. O emoji é salvo automaticamente.",
                10, MUTED, false);
        keyboardHelp.setPadding(dp(2), dp(7), 0, dp(3));
        outer.addView(keyboardHelp);

        EditText keyboardEmoji = new EditText(this);
        keyboardEmoji.setSingleLine(true);
        keyboardEmoji.setTextSize(14);
        keyboardEmoji.setTextColor(INK);
        keyboardEmoji.setHintTextColor(MUTED);
        keyboardEmoji.setHint("Toque aqui e use o emoji do seu teclado");
        keyboardEmoji.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        keyboardEmoji.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        keyboardEmoji.setPadding(dp(12), dp(8), dp(12), dp(8));
        keyboardEmoji.setBackground(roundDrawable(Color.rgb(255, 250, 235), dp(12),
                Color.rgb(238, 218, 178), dp(1)));
        outer.addView(keyboardEmoji);

        LinearLayout keyboardActions = new LinearLayout(this);
        keyboardActions.setOrientation(LinearLayout.HORIZONTAL);
        keyboardActions.setPadding(0, dp(5), 0, dp(3));
        TextView chooseKeyboard = action("TROCAR TECLADO", INK, BG_YELLOW_LIGHT, 11, true);
        chooseKeyboard.setGravity(Gravity.CENTER);
        chooseKeyboard.setPadding(dp(8), dp(10), dp(8), dp(10));
        chooseKeyboard.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(10),
                Color.rgb(240, 213, 145), dp(1)));
        TextView useKeyboardEmoji = action("CONCLUIR", Color.WHITE, ORANGE, 11, true);
        useKeyboardEmoji.setGravity(Gravity.CENTER);
        useKeyboardEmoji.setPadding(dp(8), dp(10), dp(8), dp(10));
        useKeyboardEmoji.setBackground(roundDrawable(ORANGE, dp(10), 0, 0));
        keyboardActions.addView(chooseKeyboard, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams useKeyboardParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        useKeyboardParams.leftMargin = dp(7);
        keyboardActions.addView(useKeyboardEmoji, useKeyboardParams);
        outer.addView(keyboardActions);

        chooseKeyboard.setOnClickListener(v -> {
            keyboardEmoji.requestFocus();
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (manager != null) manager.showInputMethodPicker();
        });
        // A entrada é aplicada e salva em tempo real pelo TextWatcher abaixo.
        // Este botão apenas fecha o seletor quando a pessoa terminar de digitar.
        useKeyboardEmoji.setOnClickListener(v -> {
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (manager != null) manager.hideSoftInputFromWindow(keyboardEmoji.getWindowToken(), 0);
            dialog.dismiss();
        });

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, dp(9), 0, dp(8));
        TextView emojiTab = action("EMOJIS", Color.WHITE, ORANGE, 12, true);
        emojiTab.setGravity(Gravity.CENTER);
        emojiTab.setPadding(dp(12), dp(10), dp(12), dp(10));
        emojiTab.setBackground(roundDrawable(ORANGE, dp(12), 0, 0));
        TextView stickerTab = action("🏷  ADESIVOS", INK, BG_YELLOW_LIGHT, 12, true);
        stickerTab.setGravity(Gravity.CENTER);
        stickerTab.setPadding(dp(12), dp(10), dp(12), dp(10));
        stickerTab.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(12),
                Color.rgb(240, 213, 145), dp(1)));
        tabs.addView(emojiTab, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams stickerTabParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        stickerTabParams.leftMargin = dp(7);
        tabs.addView(stickerTab, stickerTabParams);
        outer.addView(tabs);

        TextView resultInfo = label("Emojis salvos do seu teclado e catálogo de produtos.", 10, MUTED, false);
        resultInfo.setPadding(dp(2), 0, 0, dp(5));
        outer.addView(resultInfo);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        scroll.addView(grid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        outer.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));

        final boolean[] showingStickers = {false};
        final boolean[] preloadRequested = {false};
        final Runnable[] refreshGridRef = new Runnable[1];
        Runnable refreshGrid = () -> {
            grid.removeAllViews();
            if (showingStickers[0]) {
                grid.setColumnCount(4);
                search.setVisibility(View.GONE);
                keyboardHelp.setVisibility(View.GONE);
                keyboardEmoji.setVisibility(View.GONE);
                keyboardActions.setVisibility(View.GONE);
                resultInfo.setText("Adesivos próprios por categoria. Não dependem de fontes de emoji.");
                for (StickerCatalog.Sticker custom : StickerCatalog.all()) {
                    LinearLayout tile = new LinearLayout(this);
                    tile.setOrientation(LinearLayout.VERTICAL);
                    tile.setGravity(Gravity.CENTER);
                    tile.setPadding(dp(2), dp(4), dp(2), dp(4));
                    TextView badge = label(custom.badge, 24, custom.foreground, true);
                    badge.setGravity(Gravity.CENTER);
                    badge.setContentDescription(custom.title);
                    badge.setBackground(roundDrawable(custom.background, dp(11),
                            Color.rgb(232, 215, 183), dp(1)));
                    tile.addView(badge, new LinearLayout.LayoutParams(dp(43), dp(43)));
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = dp(67);
                    params.height = dp(53);
                    grid.addView(tile, params);
                    tile.setOnClickListener(v -> {
                        item.emoji = custom.value();
                        item.emojiManual = true;
                        database.setItemEmoji(item.id, item.emoji, true);
                        renderSticker(sticker, item.emoji);
                        dialog.dismiss();
                    });
                }
            } else {
                grid.setColumnCount(5);
                search.setVisibility(View.VISIBLE);
                keyboardHelp.setVisibility(View.VISIBLE);
                keyboardEmoji.setVisibility(View.VISIBLE);
                keyboardActions.setVisibility(View.VISIBLE);
                String query = search.getText() == null ? "" : search.getText().toString().trim();

                // Emojis digitados no teclado aparecem aqui no topo, mesmo que não
                // façam parte do catálogo curado de produtos.
                List<String> personalEmojis = PersonalEmojiCatalog.getAll(this);
                if (!personalEmojis.isEmpty()) {
                    TextView personalHeading = label("SEUS EMOJIS DO TECLADO", 10, MUTED, true);
                    personalHeading.setPadding(dp(3), dp(4), dp(3), dp(5));
                    GridLayout.LayoutParams headingParams = new GridLayout.LayoutParams();
                    headingParams.columnSpec = GridLayout.spec(0, 5);
                    headingParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    headingParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    grid.addView(personalHeading, headingParams);

                    for (String personalEmoji : personalEmojis) {
                        TextView choice = label(personalEmoji, 27, INK, false);
                        choice.setGravity(Gravity.CENTER);
                        choice.setContentDescription("Emoji salvo do teclado: " + personalEmoji);
                        choice.setBackground(roundDrawable(Color.rgb(255, 250, 235), dp(10),
                                Color.rgb(245, 230, 202), dp(1)));
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.width = dp(54);
                        params.height = dp(54);
                        params.setMargins(dp(2), dp(2), dp(2), dp(2));
                        grid.addView(choice, params);
                        choice.setOnClickListener(v -> {
                            item.emoji = personalEmoji;
                            item.emojiManual = true;
                            database.setItemEmoji(item.id, personalEmoji, true);
                            renderSticker(sticker, personalEmoji);
                            dialog.dismiss();
                        });
                    }
                }

                if (!query.isEmpty() && !EmojiCatalog.isLoaded()) {
                    resultInfo.setText("Preparando a pesquisa de " + emojiCount + " emojis...");
                    TextView loading = label("Carregando catálogo em segundo plano...", 13, MUTED, false);
                    loading.setGravity(Gravity.CENTER);
                    loading.setPadding(dp(10), dp(25), dp(10), dp(25));
                    grid.addView(loading, new GridLayout.LayoutParams());
                    if (!preloadRequested[0]) {
                        preloadRequested[0] = true;
                        EmojiCatalog.preload(this, () -> {
                            if (dialog.isShowing() && !showingStickers[0]) refreshGridRef[0].run();
                        });
                    }
                    return;
                }
                List<EmojiCatalog.EmojiEntry> matches = query.isEmpty()
                        ? EmojiCatalog.quick(this)
                        : EmojiCatalog.search(this, query, 80);
                resultInfo.setText(query.isEmpty()
                        ? "Seleção rápida • " + emojiCount + " emojis de produtos disponíveis."
                        : matches.size() + " produto(s) encontrados pelo nome.");
                if (matches.isEmpty()) {
                    TextView empty = label("Nenhum emoji encontrado. Experimente outra palavra ou abra Adesivos.", 13, MUTED, false);
                    empty.setGravity(Gravity.CENTER);
                    empty.setPadding(dp(10), dp(25), dp(10), dp(25));
                    grid.addView(empty, new GridLayout.LayoutParams());
                } else {
                    for (EmojiCatalog.EmojiEntry entry : matches) {
                        TextView choice = label(entry.emoji, 27, INK, false);
                        choice.setGravity(Gravity.CENTER);
                        choice.setContentDescription(entry.name);
                        choice.setBackground(roundDrawable(Color.rgb(255, 250, 235), dp(10),
                                Color.rgb(245, 230, 202), dp(1)));
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.width = dp(54);
                        params.height = dp(54);
                        params.setMargins(dp(2), dp(2), dp(2), dp(2));
                        grid.addView(choice, params);
                        choice.setOnClickListener(v -> {
                            item.emoji = entry.emoji;
                            item.emojiManual = true;
                            database.setItemEmoji(item.id, entry.emoji, true);
                            renderSticker(sticker, entry.emoji);
                            dialog.dismiss();
                        });
                    }
                }
            }
        };
        refreshGridRef[0] = refreshGrid;

        keyboardEmoji.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> entered = PersonalEmojiCatalog.remember(MainActivity.this, s);
                if (entered.isEmpty()) return;

                // Every emoji typed or pasted is saved automatically. The last one
                // in the input becomes the current product's emoji immediately.
                String selected = entered.get(entered.size() - 1);
                item.emoji = selected;
                item.emojiManual = true;
                database.setItemEmoji(item.id, selected, true);
                renderSticker(sticker, selected);
                resultInfo.setText("Emoji aplicado ao produto e salvo na aba Emojis deste aparelho.");
                if (!showingStickers[0]) refreshGrid.run();
            }

            @Override public void afterTextChanged(android.text.Editable s) { }
        });

        emojiTab.setOnClickListener(v -> {
            showingStickers[0] = false;
            emojiTab.setTextColor(Color.WHITE);
            emojiTab.setBackground(roundDrawable(ORANGE, dp(12), 0, 0));
            stickerTab.setTextColor(INK);
            stickerTab.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(12),
                    Color.rgb(240, 213, 145), dp(1)));
            refreshGrid.run();
        });
        stickerTab.setOnClickListener(v -> {
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (manager != null) manager.hideSoftInputFromWindow(keyboardEmoji.getWindowToken(), 0);
            showingStickers[0] = true;
            stickerTab.setTextColor(Color.WHITE);
            stickerTab.setBackground(roundDrawable(ORANGE, dp(12), 0, 0));
            emojiTab.setTextColor(INK);
            emojiTab.setBackground(roundDrawable(BG_YELLOW_LIGHT, dp(12),
                    Color.rgb(240, 213, 145), dp(1)));
            refreshGrid.run();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!showingStickers[0]) refreshGrid.run();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        TextView auto = action("Usar reconhecimento automático", ORANGE, Color.rgb(255, 239, 205), 13, true);
        auto.setGravity(Gravity.CENTER);
        auto.setPadding(dp(10), dp(12), dp(10), dp(12));
        auto.setBackground(roundDrawable(Color.rgb(255, 239, 205), dp(14), 0, 0));
        LinearLayout.LayoutParams autoParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        autoParams.topMargin = dp(10);
        outer.addView(auto, autoParams);
        auto.setOnClickListener(v -> {
            item.emojiManual = false;
            item.emoji = EmojiCatalog.automatic(item.text);
            database.setItemEmoji(item.id, item.emoji, false);
            renderSticker(sticker, item.emoji);
            dialog.dismiss();
        });

        refreshGrid.run();
        dialog.setContentView(outer);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    Math.min(getResources().getDisplayMetrics().widthPixels - dp(28), dp(390)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        // Focus the native Android input field so the installed keyboard opens
        // automatically. Physical and Bluetooth keyboards can type into it too.
        keyboardEmoji.requestFocus();
        keyboardEmoji.postDelayed(() -> {
            if (!dialog.isShowing() || showingStickers[0]) return;
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (manager != null) manager.showSoftInput(keyboardEmoji, InputMethodManager.SHOW_IMPLICIT);
        }, 180);
    }

    private void showExportDialog() {
        hideKeyboard();
        pendingExport = prepareCurrentExport();
        String[] choices = {
                "Salvar no aplicativo",
                "Salvar como PDF (ideal para listas grandes)",
                "Salvar como imagem (uma imagem por página)"
        };
        String recommendation = pendingExport.pages.size() == 1
                ? "Sua lista cabe em uma folha. Recomendamos salvar como imagem."
                : "Sua lista ocupa " + pendingExport.pages.size() + " páginas. Recomendamos salvar como PDF.";
        new AlertDialog.Builder(this)
                .setTitle("Salvar sua lista")
                .setMessage("A lista já é salva automaticamente no aparelho. " + recommendation
                        + " Escolha como deseja baixar:")
                .setPositiveButton("Baixar como PDF", (dialog, which) -> choosePdfLocation())
                .setNegativeButton("Baixar como imagem", (dialog, which) -> chooseImageFolder())
                .setNeutralButton("Fechar", null)
                .show();
    }

    private PendingExport prepareCurrentExport() {
        PendingExport export = new PendingExport();
        AppDatabase.ShoppingList list = database.getList(currentListId);
        export.title = list == null ? "Minha lista" : displayTitle(list.title);
        export.pages = ShoppingListRenderer.paginate(export.title, database.getItems(currentListId));
        return export;
    }

    private void chooseBackupExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "shopping-lists-backup_" + timestamp + ".json");
        startActivityForResult(intent, REQUEST_BACKUP_EXPORT);
    }

    private void chooseBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_BACKUP_IMPORT);
    }

    private void exportBackup(Uri destination) {
        new Thread(() -> {
            String error = null;
            try {
                JSONObject backup = buildPortableBackup();
                byte[] data = backup.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                if (data.length > MAX_BACKUP_BYTES) {
                    throw new IllegalStateException("O backup ficou maior que 40 MB. Salve menos imagens geradas e tente novamente.");
                }
                try (OutputStream output = getContentResolver().openOutputStream(destination, "w")) {
                    if (output == null) throw new IllegalStateException("Não foi possível abrir o arquivo escolhido.");
                    output.write(data);
                    output.flush();
                }
            } catch (Exception e) {
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            final String finalError = error;
            runOnUiThread(() -> {
                if (finalError == null) {
                    Toast.makeText(this, "Backup salvo. Guarde o arquivo para restaurar suas listas em outra instalação.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Falha ao salvar backup: " + finalError, Toast.LENGTH_LONG).show();
                }
            });
        }, "shopping-list-backup-export").start();
    }

    private JSONObject buildPortableBackup() throws Exception {
        JSONObject backup = database.exportBackup();
        JSONArray lists = backup.getJSONArray("lists");
        for (int i = 0; i < lists.length(); i++) {
            JSONObject list = lists.getJSONObject(i);
            JSONArray items = list.getJSONArray("items");
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.getJSONObject(j);
                String stickerValue = item.optString("emoji", "");
                if (!stickerValue.matches("generated:[0-9a-f]{64}")) continue;
                java.io.File stickerFile = ProductAiEngine.generatedFile(this, stickerValue);
                if (!stickerFile.isFile() || stickerFile.length() <= 0
                        || stickerFile.length() > MAX_STICKER_BACKUP_BYTES) continue;
                byte[] imageBytes;
                try (InputStream input = new java.io.FileInputStream(stickerFile);
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192];
                    int count;
                    while ((count = input.read(chunk)) != -1) {
                        buffer.write(chunk, 0, count);
                        if (buffer.size() > MAX_STICKER_BACKUP_BYTES) break;
                    }
                    imageBytes = buffer.toByteArray();
                }
                if (imageBytes.length > 0 && imageBytes.length <= MAX_STICKER_BACKUP_BYTES) {
                    item.put("generatedStickerBase64",
                            android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP));
                }
            }
        }
        return backup;
    }

    private void confirmImportBackup(Uri source) {
        new AlertDialog.Builder(this)
                .setTitle("Restaurar backup")
                .setMessage("As listas do arquivo serão adicionadas às listas que já existem. Nada será apagado. Importe cada backup apenas uma vez para evitar cópias duplicadas.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Restaurar", (dialog, which) -> importBackup(source))
                .show();
    }

    private void importBackup(Uri source) {
        new Thread(() -> {
            String error = null;
            int imported = 0;
            try {
                byte[] data;
                try (InputStream input = getContentResolver().openInputStream(source);
                     ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    if (input == null) throw new IllegalStateException("Não foi possível abrir o arquivo de backup.");
                    byte[] chunk = new byte[8192];
                    int count;
                    while ((count = input.read(chunk)) != -1) {
                        buffer.write(chunk, 0, count);
                        if (buffer.size() > MAX_BACKUP_BYTES) {
                            throw new IllegalStateException("O arquivo de backup ultrapassa 40 MB.");
                        }
                    }
                    data = buffer.toByteArray();
                }
                JSONObject backup = new JSONObject(new String(data, java.nio.charset.StandardCharsets.UTF_8));
                restoreGeneratedStickers(backup);
                imported = database.importBackup(backup);
            } catch (Exception e) {
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            final String finalError = error;
            final int finalImported = imported;
            runOnUiThread(() -> {
                if (finalError == null) {
                    showHome();
                    Toast.makeText(this, finalImported + " lista(s) restaurada(s) com seus produtos, marcações e emojis.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Não foi possível restaurar o backup: " + finalError, Toast.LENGTH_LONG).show();
                }
            });
        }, "shopping-list-backup-import").start();
    }

    private void restoreGeneratedStickers(JSONObject backup) throws Exception {
        JSONArray lists = backup.optJSONArray("lists");
        if (lists == null) return;
        for (int i = 0; i < lists.length(); i++) {
            JSONObject list = lists.optJSONObject(i);
            if (list == null) continue;
            JSONArray items = list.optJSONArray("items");
            if (items == null) continue;
            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.optJSONObject(j);
                if (item == null) continue;
                String stickerValue = item.optString("emoji", "");
                String encoded = item.optString("generatedStickerBase64", "");
                if (!stickerValue.matches("generated:[0-9a-f]{64}") || encoded.isEmpty()) continue;
                byte[] imageBytes = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP);
                if (imageBytes.length == 0 || imageBytes.length > MAX_STICKER_BACKUP_BYTES
                        || imageBytes.length < 8
                        || (imageBytes[0] & 0xff) != 0x89 || imageBytes[1] != 0x50
                        || imageBytes[2] != 0x4e || imageBytes[3] != 0x47) {
                    throw new IllegalStateException("Uma imagem do backup está inválida.");
                }
                java.io.File destination = ProductAiEngine.generatedFile(this, stickerValue);
                java.io.File parent = destination.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IllegalStateException("Não foi possível restaurar uma imagem gerada.");
                }
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(destination, false)) {
                    output.write(imageBytes);
                    output.flush();
                }
            }
        }
    }

    private void choosePdfLocation() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, safeFileName(pendingExport.title) + ".pdf");
        startActivityForResult(intent, REQUEST_PDF);
    }

    private void chooseImageFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMAGE_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_UNKNOWN_SOURCES) {
            if (pendingUpdateApk != null && android.os.Build.VERSION.SDK_INT >= 26
                    && getPackageManager().canRequestPackageInstalls()) {
                try {
                    UpdateManager.install(this, pendingUpdateApk, REQUEST_UNKNOWN_SOURCES);
                } catch (Exception e) {
                    Toast.makeText(this, "Não foi possível iniciar a instalação: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "A autorização para instalar atualizações não foi concedida.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode == REQUEST_BACKUP_EXPORT || requestCode == REQUEST_BACKUP_IMPORT) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
            Uri backupUri = data.getData();
            if (requestCode == REQUEST_BACKUP_EXPORT) exportBackup(backupUri);
            else confirmImportBackup(backupUri);
            return;
        }
        if (resultCode != RESULT_OK || data == null || data.getData() == null || pendingExport == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_PDF) {
            writePdf(uri, pendingExport);
        } else if (requestCode == REQUEST_IMAGE_FOLDER) {
            try {
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) { }
            writeImages(uri, pendingExport);
        }
    }

    private void checkForUpdates() {
        if (!isNetworkAvailable()) {
            new AlertDialog.Builder(this)
                    .setTitle("Sem conexão com a internet")
                    .setMessage("O Shopping Lists funciona normalmente offline. Para verificar atualizações, conecte-se à internet e tente novamente.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        AlertDialog progress = new AlertDialog.Builder(this)
                .setTitle("Verificando atualizações")
                .setMessage("Consultando a versão mais recente...")
                .setCancelable(false)
                .create();
        progress.show();
        UpdateManager.check(this, (update, message) -> {
            progress.dismiss();
            if (isFinishing()) return;
            if (update == null) {
                new AlertDialog.Builder(this)
                        .setTitle("Shopping Lists")
                        .setMessage(message == null ? "Não foi possível verificar atualizações." : message)
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            String detail = "Nova versão disponível: " + update.versionName + " (" + update.versionCode + ").";
            if (!update.changelog.trim().isEmpty()) detail += "\n\nNovidades:\n" + update.changelog;
            detail += "\n\nSuas listas serão mantidas durante a atualização.";
            new AlertDialog.Builder(this)
                    .setTitle("Atualização disponível")
                    .setMessage(detail)
                    .setNegativeButton("Depois", null)
                    .setPositiveButton("Baixar e instalar", (dialog, which) -> downloadUpdate(update))
                    .show();
        });
    }

    private void downloadUpdate(UpdateManager.UpdateInfo update) {
        AlertDialog progress = new AlertDialog.Builder(this)
                .setTitle("Baixando atualização")
                .setMessage("O aplicativo está baixando e verificando o arquivo...")
                .setCancelable(false)
                .create();
        progress.show();
        UpdateManager.download(this, update, (apk, error) -> {
            progress.dismiss();
            if (isFinishing()) return;
            if (apk == null) {
                new AlertDialog.Builder(this)
                        .setTitle("Falha na atualização")
                        .setMessage(error == null ? "Não foi possível baixar o APK." : error)
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            pendingUpdateApk = apk;
            try {
                UpdateManager.install(this, apk, REQUEST_UNKNOWN_SOURCES);
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível abrir o instalador: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager manager =
                    (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager == null) return false;
            android.net.NetworkInfo info = manager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void writePdf(Uri destination, PendingExport export) {
        new Thread(() -> {
            String error = null;
            PdfDocument document = new PdfDocument();
            try {
                for (int i = 0; i < export.pages.size(); i++) {
                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                            ShoppingListRenderer.PAGE_WIDTH, ShoppingListRenderer.PAGE_HEIGHT, i + 1).create();
                    PdfDocument.Page page = document.startPage(info);
                    ShoppingListRenderer.drawPage(this, page.getCanvas(), export.title, export.pages.get(i),
                            i + 1, export.pages.size());
                    document.finishPage(page);
                }
                try (OutputStream output = getContentResolver().openOutputStream(destination, "w")) {
                    if (output == null) throw new IllegalStateException("Não foi possível abrir o arquivo escolhido.");
                    document.writeTo(output);
                    output.flush();
                }
            } catch (Exception e) {
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            } finally {
                document.close();
            }
            final String finalError = error;
            runOnUiThread(() -> {
                if (finalError == null) Toast.makeText(this, "PDF salvo com " + export.pages.size() + " página(s).", Toast.LENGTH_LONG).show();
                else Toast.makeText(this, "Falha ao salvar PDF: " + finalError, Toast.LENGTH_LONG).show();
            });
        }, "shopping-list-pdf-export").start();
    }

    private void writeImages(Uri treeUri, PendingExport export) {
        new Thread(() -> {
            String error = null;
            int saved = 0;
            try {
                String treeId = DocumentsContract.getTreeDocumentId(treeUri);
                Uri parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId);
                ContentResolver resolver = getContentResolver();
                String base = safeFileName(export.title);
                for (int i = 0; i < export.pages.size(); i++) {
                    String filename = base + "_pagina_" + String.format(Locale.ROOT, "%02d", i + 1) + ".png";
                    Uri file = DocumentsContract.createDocument(resolver, parent, "image/png", filename);
                    if (file == null) throw new IllegalStateException("Não foi possível criar " + filename);
                    android.graphics.Bitmap bitmap = ShoppingListRenderer.renderImage(this, export.title,
                            export.pages.get(i), i + 1, export.pages.size());
                    try (OutputStream output = resolver.openOutputStream(file, "w")) {
                        if (output == null) throw new IllegalStateException("Não foi possível gravar " + filename);
                        if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) {
                            throw new IllegalStateException("Falha ao converter a imagem.");
                        }
                        output.flush();
                    } finally {
                        bitmap.recycle();
                    }
                    saved++;
                }
            } catch (Exception e) {
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            final String finalError = error;
            final int finalSaved = saved;
            runOnUiThread(() -> {
                if (finalError == null) Toast.makeText(this, finalSaved + " imagem(ns) salvas na pasta escolhida.", Toast.LENGTH_LONG).show();
                else Toast.makeText(this, finalSaved + " imagem(ns) salvas. Falha: " + finalError, Toast.LENGTH_LONG).show();
            });
        }, "shopping-list-image-export").start();
    }

    private void confirmDeleteList() {
        new AlertDialog.Builder(this)
                .setTitle("Excluir esta lista?")
                .setMessage("A lista e todos os produtos dela serão apagados deste aparelho.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    long id = currentListId;
                    currentListId = -1;
                    database.deleteList(id);
                    hideKeyboard();
                    showHome();
                }).show();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    @Override
    public void onBackPressed() {
        if (currentListId != -1) {
            hideKeyboard();
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    private TextView label(String text, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        else view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(true);
        return view;
    }

    private TextView action(String text, int textColor, int backgroundColor, float size, boolean bold) {
        TextView view = label(text, size, textColor, bold);
        view.setBackground(roundDrawable(backgroundColor, dp(14), 0, 0));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private GradientDrawable roundDrawable(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String displayTitle(String title) {
        return title == null || title.trim().isEmpty() ? "Minha lista" : title;
    }

    private String shortDate(long time) {
        if (time <= 0) return "agora";
        return new SimpleDateFormat("dd/MM", new Locale("pt", "BR")).format(new Date(time));
    }

    private String safeFileName(String value) {
        String safe = value == null ? "Minha_lista" : value.trim();
        safe = safe.replaceAll("[^\\p{L}\\p{N}_-]+", "_");
        safe = safe.replaceAll("_+", "_");
        if (safe.isEmpty()) safe = "Minha_lista";
        return safe;
    }

    private final class NotebookPaperLayout extends LinearLayout {
        private final Paint blueLines = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint redMargin = new Paint(Paint.ANTI_ALIAS_FLAG);

        NotebookPaperLayout(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            setWillNotDraw(false);
            setPadding(dp(54), dp(20), dp(16), dp(30));
            blueLines.setColor(Color.rgb(202, 222, 244));
            blueLines.setStrokeWidth(dp(1));
            redMargin.setColor(Color.rgb(237, 164, 156));
            redMargin.setStrokeWidth(dp(1));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(PAPER);
            float spacing = dp(52);
            float start = dp(94);
            for (float y = start; y < getHeight(); y += spacing) {
                canvas.drawLine(dp(12), y, getWidth() - dp(8), y, blueLines);
            }
            canvas.drawLine(dp(42), 0, dp(42), getHeight(), redMargin);
        }
    }
}
