package br.com.shoppinglist.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.List;

/** Shared A4-like renderer used by both the native PDF and PNG exporters. */
public final class ShoppingListRenderer {
    public static final int PAGE_WIDTH = 595;
    public static final int PAGE_HEIGHT = 842;
    public static final int IMAGE_WIDTH = 1240;
    public static final int IMAGE_HEIGHT = 1754;

    public static final class ExportLine {
        public String text;
        public String emoji;
        public boolean firstOfItem;
        public boolean checked;
        public boolean lastOfItem;
    }

    public static final class ExportPage {
        public final List<ExportLine> lines = new ArrayList<>();
    }

    private ShoppingListRenderer() {}

    public static List<ExportPage> paginate(String title, List<AppDatabase.ShoppingItem> source) {
        List<ExportPage> pages = new ArrayList<>();
        Paint measure = new Paint(Paint.ANTI_ALIAS_FLAG);
        measure.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        measure.setTextSize(17.5f);
        final float maxTextWidth = 595f - 148f;
        final float bottom = 770f;
        float y = 148f;
        ExportPage page = new ExportPage();
        pages.add(page);

        boolean anyText = false;
        for (AppDatabase.ShoppingItem item : source) {
            String raw = item.text == null ? "" : item.text;
            if (raw.trim().isEmpty()) continue;
            anyText = true;
            List<String> wrapped = wrap(raw, measure, maxTextWidth);
            for (int i = 0; i < wrapped.size(); i++) {
                boolean first = i == 0;
                boolean last = i == wrapped.size() - 1;
                float h = first ? 30f : 23f;
                if (y + h > bottom && !page.lines.isEmpty()) {
                    page = new ExportPage();
                    pages.add(page);
                    y = 148f;
                }
                ExportLine line = new ExportLine();
                line.text = wrapped.get(i);
                line.firstOfItem = first;
                line.emoji = first ? (item.emoji == null ? "🛒" : item.emoji) : "";
                line.checked = item.checked;
                line.lastOfItem = last;
                page.lines.add(line);
                y += h;
                if (last) y += 8f;
            }
        }
        if (!anyText) {
            ExportLine empty = new ExportLine();
            empty.text = "Sua lista ainda está vazia.";
            empty.firstOfItem = true;
            empty.emoji = "🛒";
            page.lines.add(empty);
        }
        return pages;
    }

    private static List<String> wrap(String text, Paint paint, float maxWidth) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.replace("\r", "").split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                result.add(" ");
                continue;
            }
            String remaining = paragraph;
            while (!remaining.isEmpty()) {
                int count = paint.breakText(remaining, true, maxWidth, null);
                if (count <= 0) count = 1;
                int splitAt = count;
                if (count < remaining.length()) {
                    int space = remaining.lastIndexOf(' ', count);
                    if (space > 0) splitAt = space;
                }
                String line = remaining.substring(0, splitAt).trim();
                if (line.isEmpty()) line = remaining.substring(0, splitAt);
                result.add(line);
                remaining = remaining.substring(splitAt).trim();
            }
        }
        if (result.isEmpty()) result.add("");
        return result;
    }

    public static void drawPage(Context context, Canvas canvas, String title, ExportPage page, int pageNumber, int pageCount) {
        canvas.drawColor(Color.rgb(255, 254, 246));

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.rgb(198, 220, 244));
        line.setStrokeWidth(0.8f);
        for (float y = 132; y < PAGE_HEIGHT - 25; y += 32) {
            canvas.drawLine(28, y, PAGE_WIDTH - 24, y, line);
        }
        Paint margin = new Paint(Paint.ANTI_ALIAS_FLAG);
        margin.setColor(Color.rgb(235, 145, 145));
        margin.setStrokeWidth(1.2f);
        canvas.drawLine(58, 28, 58, PAGE_HEIGHT - 24, margin);

        Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        band.setColor(Color.rgb(255, 221, 115));
        canvas.drawRoundRect(68, 26, PAGE_WIDTH - 28, 108, 13, 13, band);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.rgb(81, 55, 23));
        titlePaint.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        titlePaint.setTextSize(24f);
        String safeTitle = title == null || title.trim().isEmpty() ? "Minha lista" : title.trim();
        if (titlePaint.measureText(safeTitle) > PAGE_WIDTH - 130) {
            while (safeTitle.length() > 3 && titlePaint.measureText(safeTitle + "…") > PAGE_WIDTH - 130) {
                safeTitle = safeTitle.substring(0, safeTitle.length() - 1);
            }
            safeTitle += "…";
        }
        canvas.drawText(safeTitle, 82, 62, titlePaint);

        Paint subtitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtitle.setColor(Color.rgb(130, 91, 41));
        subtitle.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        subtitle.setTextSize(11f);
        canvas.drawText("SHOPPING LIST  •  PÁGINA " + pageNumber + " DE " + pageCount, 83, 87, subtitle);

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.rgb(48, 54, 64));
        body.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        body.setTextSize(17.5f);

        Paint emoji = new Paint(Paint.ANTI_ALIAS_FLAG);
        emoji.setTextSize(21f);
        emoji.setTypeface(Typeface.DEFAULT);

        Paint unchecked = new Paint(Paint.ANTI_ALIAS_FLAG);
        unchecked.setColor(Color.rgb(125, 147, 110));
        unchecked.setStyle(Paint.Style.STROKE);
        unchecked.setStrokeWidth(1.8f);

        Paint checked = new Paint(Paint.ANTI_ALIAS_FLAG);
        checked.setColor(Color.rgb(69, 151, 83));
        checked.setStyle(Paint.Style.STROKE);
        checked.setStrokeWidth(2.2f);

        float y = 155f;
        for (ExportLine row : page.lines) {
            if (row.firstOfItem) {
                canvas.drawCircle(82, y - 5, 8, row.checked ? checked : unchecked);
                if (row.checked) {
                    Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tick.setColor(Color.rgb(69, 151, 83));
                    tick.setStrokeWidth(2.0f);
                    tick.setStyle(Paint.Style.STROKE);
                    Path path = new Path();
                    path.moveTo(78, y - 5);
                    path.lineTo(81, y - 2);
                    path.lineTo(87, y - 9);
                    canvas.drawPath(path, tick);
                }
                String stickerValue = row.emoji == null ? "🛒" : row.emoji;
                if (ProductAiEngine.isGenerated(stickerValue)) {
                    Bitmap generated = BitmapFactory.decodeFile(
                            ProductAiEngine.generatedFile(context, stickerValue).getAbsolutePath());
                    if (generated != null) {
                        Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                        canvas.drawBitmap(generated, null,
                                new RectF(100, y - 17, 128, y + 8), imagePaint);
                        generated.recycle();
                    } else {
                        canvas.drawText("🛒", 101, y + 2, emoji);
                    }
                } else {
                    StickerCatalog.Sticker customSticker = StickerCatalog.fromValue(stickerValue);
                    if (customSticker != null) {
                    Paint stickerBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
                    stickerBackground.setColor(customSticker.background);
                    canvas.drawRoundRect(100, y - 17, 128, y + 8, 5, 5, stickerBackground);
                    Paint stickerText = new Paint(Paint.ANTI_ALIAS_FLAG);
                    stickerText.setColor(customSticker.foreground);
                    stickerText.setTypeface(Typeface.DEFAULT);
                    stickerText.setTextSize(18f);
                    stickerText.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(customSticker.badge, 114, y + 5f, stickerText);
                    } else {
                        canvas.drawText(stickerValue, 101, y + 2, emoji);
                    }
                }
            }
            canvas.drawText(row.text == null ? "" : row.text, 136, y, body);
            y += row.firstOfItem ? 30f : 23f;
            if (row.lastOfItem) y += 8f;
        }
    }

    public static Bitmap renderImage(Context context, String title, ExportPage page, int pageNumber, int pageCount) {
        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(IMAGE_WIDTH / (float) PAGE_WIDTH, IMAGE_HEIGHT / (float) PAGE_HEIGHT);
        drawPage(context, canvas, title, page, pageNumber, pageCount);
        return bitmap;
    }
}
