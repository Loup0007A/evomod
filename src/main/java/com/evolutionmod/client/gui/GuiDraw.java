package com.evolutionmod.client.gui;

import com.evolutionmod.genetics.GeneType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Petite boîte à outils de dessin pour des interfaces « laboratoire » sans
 * textures : panneaux, barres de gènes, graphiques, badges.
 */
public final class GuiDraw {

    private GuiDraw() {}

    // Palette
    public static final int BG_DARK = 0xF0101820;
    public static final int BG_PANEL = 0xE0182430;
    public static final int BG_INSET = 0xC00B1218;
    public static final int BORDER = 0xFF2F4A5C;
    public static final int BORDER_LIGHT = 0xFF4FA3C7;
    public static final int ACCENT = 0xFF57E3C2;
    public static final int ACCENT_2 = 0xFFB98CFF;
    public static final int TEXT = 0xFFE6F1F5;
    public static final int TEXT_DIM = 0xFF8FA6B2;
    public static final int TEXT_FAINT = 0xFF5B7280;
    public static final int GOOD = 0xFF7BE07B;
    public static final int WARN = 0xFFFFB347;
    public static final int BAD = 0xFFFF5B7A;

    /** Fenêtre principale avec liseré lumineux et en-tête. */
    public static void window(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF05090C);
        g.fillGradient(x, y, x + w, y + h, 0xF2131E28, 0xF20C141B);
        g.renderOutline(x, y, w, h, BORDER);
        g.hLine(x + 1, x + w - 2, y + 1, 0x403FD7FF);
        // coins décoratifs
        int c = BORDER_LIGHT;
        g.hLine(x, x + 6, y, c);
        g.vLine(x, y, y + 6, c);
        g.hLine(x + w - 7, x + w - 1, y, c);
        g.vLine(x + w - 1, y, y + 6, c);
        g.hLine(x, x + 6, y + h - 1, c);
        g.vLine(x, y + h - 7, y + h - 1, c);
        g.hLine(x + w - 7, x + w - 1, y + h - 1, c);
        g.vLine(x + w - 1, y + h - 7, y + h - 1, c);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG_INSET);
        g.renderOutline(x, y, w, h, 0xFF22343F);
    }

    public static void panelTitle(GuiGraphics g, Font font, Component title, int x, int y, int w) {
        g.drawString(font, title, x + 4, y + 4, ACCENT, false);
        g.hLine(x + 3, x + w - 4, y + 14, 0xFF22343F);
    }

    /** Barre de progression simple. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int color) {
        g.fill(x, y, x + w, y + h, 0xFF0A1015);
        int fw = (int) Math.round(Math.max(0, Math.min(1, frac)) * w);
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, brighten(color, 1.25f), color);
        }
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF1C2A33);
    }

    /**
     * Barre de gène : remplissage = valeur actuelle, trait blanc = valeur
     * d'origine (ancêtre), trait gris = neutre vanilla.
     */
    public static void geneBar(GuiGraphics g, int x, int y, int w, int h, GeneType type,
                               double value, double reference, int color) {
        g.fill(x, y, x + w, y + h, 0xFF0A1015);
        if (type == GeneType.HUE) {
            // spectre complet + curseur
            for (int i = 0; i < w; i++) {
                int rgb = java.awt.Color.HSBtoRGB(i / (float) w, 0.8f, 0.9f) | 0xFF000000;
                g.vLine(x + i, y, y + h - 1, (rgb & 0x00FFFFFF) | 0x90000000);
            }
            int px = x + (int) Math.round(type.normalize(value) * (w - 1));
            g.fill(px - 1, y - 1, px + 2, y + h + 1, 0xFFFFFFFF);
        } else {
            double n = type.normalize(type.neutral);
            double v = type.normalize(value);
            int nx = x + (int) Math.round(n * w);
            int vx = x + (int) Math.round(v * w);
            if (type.neutral == type.min) {
                if (vx > x) g.fillGradient(x, y, vx, y + h, brighten(color, 1.3f), color);
            } else {
                // barre centrée sur le neutre : vers la droite = renforcé, vers la gauche = affaibli
                int c = vx >= nx ? color : desaturate(color);
                g.fillGradient(Math.min(nx, vx), y, Math.max(nx, vx) + (vx == nx ? 1 : 0), y + h,
                    brighten(c, 1.3f), c);
                g.vLine(nx, y - 1, y + h, 0xFF6B7F8A);
            }
            int rx = x + (int) Math.round(type.normalize(reference) * w);
            if (Math.abs(rx - vx) > 1) g.vLine(Math.min(x + w - 1, rx), y - 2, y + h + 1, 0xCCFFFFFF);
        }
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF1C2A33);
    }

    /** Petit graphique en courbe (valeurs 0..max). */
    public static void sparkline(GuiGraphics g, int x, int y, int w, int h, List<Float> values,
                                 float max, int color) {
        g.fill(x, y, x + w, y + h, 0xFF0A1015);
        for (int i = 1; i < 4; i++) g.hLine(x, x + w - 1, y + h * i / 4, 0x2236505E);
        if (values.isEmpty()) return;
        int n = values.size();
        float m = Math.max(max, 1.0e-4f);
        int prevX = -1, prevY = -1;
        for (int i = 0; i < n; i++) {
            int px = n == 1 ? x + w - 1 : x + (int) ((w - 1) * (i / (float) (n - 1)));
            int py = y + h - 1 - (int) ((h - 2) * Math.min(1f, values.get(i) / m));
            g.fill(px, py, px + 1, y + h, (color & 0x00FFFFFF) | 0x30000000);
            if (prevX >= 0) line(g, prevX, prevY, px, py, color);
            prevX = px;
            prevY = py;
        }
        g.fill(prevX - 1, prevY - 1, prevX + 2, prevY + 2, 0xFFFFFFFF);
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF1C2A33);
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int px = x0 + (steps == 0 ? 0 : (x1 - x0) * i / steps);
            int py = y0 + (steps == 0 ? 0 : (y1 - y0) * i / steps);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    /** Petite étiquette colorée. Renvoie sa largeur. */
    public static int chip(GuiGraphics g, Font font, Component text, int x, int y, int color) {
        int w = font.width(text) + 8;
        g.fill(x, y, x + w, y + 11, (color & 0x00FFFFFF) | 0x40000000);
        g.renderOutline(x, y, w, 11, color);
        g.drawString(font, text, x + 4, y + 2, color, false);
        return w;
    }

    /** Onglet cliquable. */
    public static void tab(GuiGraphics g, Font font, Component text, int x, int y, int w, int h,
                           boolean active, boolean hover) {
        int bg = active ? 0xFF1E3442 : hover ? 0xFF16252F : 0xFF101B23;
        g.fill(x, y, x + w, y + h, bg);
        g.renderOutline(x, y, w, h, active ? BORDER_LIGHT : BORDER);
        if (active) g.hLine(x + 1, x + w - 2, y + h - 1, bg);
        if (active) g.hLine(x + 2, x + w - 3, y + 1, ACCENT);
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, active ? TEXT : TEXT_DIM);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static int brighten(int argb, float f) {
        int a = argb >>> 24;
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int gg = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    public static int desaturate(int argb) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF, gg = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int grey = (r + gg + b) / 3;
        r = (r + grey * 2) / 3 * 3 / 4;
        gg = (gg + grey * 2) / 3 * 3 / 4;
        b = (b + grey * 2) / 3 * 3 / 4;
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    public static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    /** Couleur associée à un niveau de divergence (vert → orange → rose). */
    public static int divergenceColor(double d) {
        if (d < 0.07) return GOOD;
        if (d < 0.18) return WARN;
        return BAD;
    }

    public static String pct(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", v * 100);
    }

    /** Tronque un texte pour qu'il tienne dans la largeur donnée. */
    public static String fit(Font font, String s, int width) {
        if (font.width(s) <= width) return s;
        String ell = "…";
        while (!s.isEmpty() && font.width(s + ell) > width) s = s.substring(0, s.length() - 1);
        return s + ell;
    }
}
