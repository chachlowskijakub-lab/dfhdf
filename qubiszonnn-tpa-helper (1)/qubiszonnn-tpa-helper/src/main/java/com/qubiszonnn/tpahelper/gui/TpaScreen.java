package com.qubiszonnn.tpahelper.gui;

import com.qubiszonnn.tpahelper.ChatTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Ciemne, nowoczesne GUI. Interakcję obsługują standardowe (niewidoczne) ButtonWidget-y,
 * a cały wygląd i animacje rysowane są ręcznie.
 */
public class TpaScreen extends Screen {
    private static final int ACCENT = 0x5B8CFF;
    private static final int GREEN = 0x4ADE80;
    private static final int RED = 0xF87171;
    private static final int ROW_H = 26;
    private static final int ROW_STEP = 29;

    /** Przygotowana komenda – zapamiętywana między otwarciami GUI. */
    private static String preparedNick = null;

    private static final class Btn {
        final ButtonWidget widget;
        final int row;
        String label;
        int accent;
        float hover;

        Btn(ButtonWidget widget, String label, int accent, int row) {
            this.widget = widget;
            this.label = label;
            this.accent = accent;
            this.row = row;
        }
    }

    private final long openTime = System.currentTimeMillis();
    private long lastFrame = openTime;
    private long copiedUntil = 0;
    private long sentUntil = 0;
    private int scroll = 0;
    private int lastVersion = -1;
    private boolean needsRebuild = false;

    private List<ChatTracker.Entry> entries = List.of();
    private final List<Btn> btns = new ArrayList<>();
    private Btn copyBtn;
    private Btn sendBtn;

    private int px, py, pw, ph, listTop, listBottom, visible;

    public TpaScreen() {
        super(Text.literal("Qubiszonnn TPA Helper"));
    }

    // ------------------------------------------------------------------ logika

    private String command() {
        return preparedNick == null ? null : "/tpahere " + preparedNick;
    }

    private void prepare(String nick) {
        preparedNick = nick;
    }

    private void copy() {
        String cmd = command();
        if (cmd == null || this.client == null) return;
        this.client.keyboard.setClipboard(cmd);
        copiedUntil = System.currentTimeMillis() + 1400;
    }

    private void send() {
        String cmd = command();
        if (cmd == null || this.client == null) return;
        ClientPlayNetworkHandler handler = this.client.getNetworkHandler();
        if (handler == null) return;
        handler.sendChatCommand(cmd.substring(1));
        sentUntil = System.currentTimeMillis() + 1400;
    }

    // ------------------------------------------------------------------ widgety

    @Override
    protected void init() {
        buildWidgets();
    }

    private Btn add(int x, int y, int w, int h, String label, int accent, int row, Runnable action) {
        ButtonWidget bw = ButtonWidget.builder(Text.empty(), b -> action.run())
                .dimensions(x, y, w, h)
                .build();
        bw.setAlpha(0f); // wygląd rysujemy sami
        this.addDrawableChild(bw);
        Btn btn = new Btn(bw, label, accent, row);
        btns.add(btn);
        return btn;
    }

    private void buildWidgets() {
        this.clearChildren();
        btns.clear();

        pw = Math.min(this.width - 16, 400);
        ph = Math.min(this.height - 16, 330);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        listTop = py + 116;
        listBottom = py + ph - 30;
        visible = Math.max(1, (listBottom - listTop) / ROW_STEP);

        entries = ChatTracker.snapshot();
        lastVersion = ChatTracker.version();
        int maxScroll = Math.max(0, entries.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        // nagłówek
        add(px + pw - 26, py + 5, 20, 20, "X", RED, -1, this::close);
        add(px + pw - 26 - 4 - 62, py + 5, 62, 20, "Wyczyść", RED, -1, () -> ChatTracker.clear());

        // karta komendy
        int half = (pw - 40 - 6) / 2;
        copyBtn = add(px + 20, py + 72, half, 20, "Kopiuj", ACCENT, -1, this::copy);
        sendBtn = add(px + 20 + half + 6, py + 72, half, 20, "Wyślij", GREEN, -1, this::send);

        // wiersze listy
        int rowRight = rowRight();
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= entries.size()) break;
            final String nick = entries.get(idx).name();
            int ry = listTop + i * ROW_STEP;
            int copyX = rowRight - 4 - 54;
            int prepX = copyX - 4 - 70;
            add(prepX, ry + 3, 70, 20, "Przygotuj", ACCENT, i, () -> prepare(nick));
            add(copyX, ry + 3, 54, 20, "Kopiuj", GREEN, i, () -> {
                prepare(nick);
                copy();
            });
        }
    }

    private int rowRight() {
        return px + pw - 12 - (entries.size() > visible ? 7 : 0);
    }

    @Override
    public void tick() {
        if (needsRebuild || ChatTracker.version() != lastVersion) {
            needsRebuild = false;
            buildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (entries.size() > visible && mouseY >= listTop && mouseY <= listBottom) {
            int max = Math.max(0, entries.size() - visible);
            int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(verticalAmount)));
            if (next != scroll) {
                scroll = next;
                buildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ------------------------------------------------------------------ rysowanie

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float a = ease((now - openTime) / 260f);

        boolean hasCmd = preparedNick != null;
        copyBtn.widget.active = hasCmd;
        sendBtn.widget.active = hasCmd;
        copyBtn.label = now < copiedUntil ? "Skopiowano!" : "Kopiuj";
        copyBtn.accent = now < copiedUntil ? GREEN : ACCENT;
        sendBtn.label = now < sentUntil ? "Wysłano!" : "Wyślij";

        // tło + poświata panelu
        ctx.fill(0, 0, this.width, this.height, argb(0.6f * a, 0x05060A));
        rrect(ctx, px - 2, py - 2, px + pw + 2, py + ph + 2, argb(0.16f * a, ACCENT));
        rrect(ctx, px - 1, py - 1, px + pw + 1, py + ph + 1, argb(0.35f * a, ACCENT));
        rrect(ctx, px, py, px + pw, py + ph, argb(a, 0x13151C));

        // nagłówek
        ctx.fillGradient(px + 1, py + 1, px + pw - 1, py + 30, argb(a, 0x262B47), argb(a, 0x151824));
        ctx.fill(px, py + 30, px + pw, py + 31, argb(a, ACCENT));
        int sx = px + (int) ((now % 2600) / 2600f * (pw + 80)) - 40;
        ctx.fill(Math.max(px, sx - 30), py + 30, Math.min(px + pw, sx + 30), py + 31, argb(0.9f * a, 0xFFFFFF));
        text(ctx, "Qubiszonnn TPA Helper", px + 12, py + 11, argb(a, 0xFFFFFF));

        // karta komendy
        int cx1 = px + 12, cy1 = py + 38, cx2 = px + pw - 12, cy2 = py + 98;
        boolean flash = now < copiedUntil || now < sentUntil;
        float pulse = hasCmd ? 0.55f + 0.45f * (float) Math.sin(now / 280.0) : 0f;
        int border = lerp(0x2A2E3D, flash ? GREEN : ACCENT, pulse);
        rrect(ctx, cx1, cy1, cx2, cy2, argb(a, border));
        rrect(ctx, cx1 + 1, cy1 + 1, cx2 - 1, cy2 - 1, argb(a, 0x191C27));
        text(ctx, "PRZYGOTOWANA KOMENDA", cx1 + 8, cy1 + 7, argb(a, 0x8A90A6));
        String cmd = command();
        text(ctx, cmd != null ? cmd : "Wybierz gracza z listy poniżej",
                cx1 + 8, cy1 + 20, argb(a, cmd != null ? 0x9DB8FF : 0x5E647A));

        // lista
        text(ctx, "Gracze, którzy pisali na czacie (" + entries.size() + ")", px + 14, py + 104, argb(a, 0x8A90A6));
        int rx1 = px + 12;
        int rx2 = rowRight();
        if (entries.isEmpty()) {
            String empty = "Nikt jeszcze nic nie napisał na czacie.";
            text(ctx, empty, px + (pw - this.textRenderer.getWidth(empty)) / 2, listTop + 36, argb(a, 0x5E647A));
        }
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= entries.size()) break;
            ChatTracker.Entry e = entries.get(idx);
            int ry = listTop + i * ROW_STEP;
            float ra = a * rowAnim(now, i);
            boolean hov = mx >= rx1 && mx < rx2 && my >= ry && my < ry + ROW_H;
            boolean sel = e.name().equals(preparedNick);
            if (sel) rrect(ctx, rx1, ry, rx2, ry + ROW_H, argb(ra * 0.9f, GREEN));
            int bgx = sel ? 1 : 0;
            rrect(ctx, rx1 + bgx, ry + bgx, rx2 - bgx, ry + ROW_H - bgx, argb(ra, hov ? 0x232739 : 0x1A1D29));
            ctx.fill(rx1 + 3, ry + 4, rx1 + 5, ry + ROW_H - 4, argb(ra, sel ? GREEN : ACCENT));
            text(ctx, e.name(), rx1 + 10, ry + 4, argb(ra, 0xFFFFFF));
            int maxW = (rx2 - 4 - 54 - 4 - 70 - 6) - (rx1 + 10);
            String msg = this.textRenderer.trimToWidth(e.lastMessage(), Math.max(10, maxW));
            text(ctx, msg, rx1 + 10, ry + 14, argb(ra, 0x7A8096));
        }

        // pasek przewijania
        if (entries.size() > visible) {
            int tx = px + pw - 16;
            int trackH = listBottom - listTop;
            int max = entries.size() - visible;
            int thumbH = Math.max(14, trackH * visible / entries.size());
            int thumbY = listTop + (trackH - thumbH) * scroll / max;
            ctx.fill(tx, listTop, tx + 3, listBottom, argb(0.7f * a, 0x232738));
            ctx.fill(tx, thumbY, tx + 3, thumbY + thumbH, argb(a, ACCENT));
        }

        // przyciski
        for (Btn b : btns) drawBtn(ctx, b, mx, my, dt, a, now);

        // stopka
        ctx.fill(px + 12, py + ph - 26, px + pw - 12, py + ph - 25, argb(a, 0x2A2E3D));
        String footer = "Create by qubiszonnn";
        float shimmer = 0.5f + 0.5f * (float) Math.sin(now / 700.0);
        text(ctx, footer, px + (pw - this.textRenderer.getWidth(footer)) / 2, py + ph - 18,
                argb(a, lerp(ACCENT, 0xFFFFFF, shimmer * 0.6f)));
    }

    private void drawBtn(DrawContext ctx, Btn b, int mx, int my, float dt, float a, long now) {
        ButtonWidget w = b.widget;
        int x1 = w.getX(), y1 = w.getY(), x2 = x1 + w.getWidth(), y2 = y1 + w.getHeight();
        boolean hov = w.active && mx >= x1 && mx < x2 && my >= y1 && my < y2;
        b.hover += ((hov ? 1f : 0f) - b.hover) * (1f - (float) Math.exp(-dt * 16f));
        float al = a * (b.row >= 0 ? rowAnim(now, b.row) : 1f);

        int edge = w.active ? lerp(0x3A3F55, b.accent, 0.35f + 0.65f * b.hover) : 0x2A2D38;
        int fill = w.active ? lerp(0x23273A, b.accent, 0.12f + 0.45f * b.hover) : 0x1C1F29;
        rrect(ctx, x1, y1, x2, y2, argb(al, edge));
        rrect(ctx, x1 + 1, y1 + 1, x2 - 1, y2 - 1, argb(al, fill));

        int tw = this.textRenderer.getWidth(b.label);
        int tx = x1 + (w.getWidth() - tw) / 2;
        int ty = y1 + (w.getHeight() - 8) / 2;
        text(ctx, b.label, tx, ty, argb(al, w.active ? 0xFFFFFF : 0x666B7C));
    }

    // ------------------------------------------------------------------ pomocnicze

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        if ((color >>> 24) < 8) return;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(s), x, y, color);
    }

    /** Kaskadowe pojawianie się wierszy po otwarciu GUI. */
    private float rowAnim(long now, int row) {
        return ease((now - openTime - 120 - row * 40L) / 240f);
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Prostokąt z zaokrąglonymi (ściętymi) rogami. */
    private static void rrect(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1 + 1, y1, x2 - 1, y2, color);
        ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }
}
