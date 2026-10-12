package com.evolutionmod.client.gui;

import com.evolutionmod.core.EvolutionCommand;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.SpeciesProfile;
import com.evolutionmod.genetics.Traits;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.RequestJournalMsg;
import com.evolutionmod.world.HistoryEntry;
import com.evolutionmod.world.SpeciesRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Journal d'évolution : l'encyclopédie évolutive du monde.
 * Onglets : Monde, Espèces, Histoire, Mon génome.
 */
public class JournalScreen extends Screen {

    private enum Tab {
        WORLD("world"), SPECIES("species"), HISTORY("history"), SELF("self");

        final String key;

        Tab(String key) {
            this.key = key;
        }

        Component label() {
            return Component.translatable("evolutionmod.journal.tab." + key);
        }
    }

    private enum Filter {
        ALL, HOSTILE, PASSIVE, PLANT;

        Component label() {
            return Component.translatable("evolutionmod.journal.filter." + name().toLowerCase(Locale.ROOT));
        }

        boolean accepts(SpeciesRecord r) {
            return switch (this) {
                case ALL -> true;
                case HOSTILE -> r.kind() == SpeciesProfile.Kind.HOSTILE;
                case PASSIVE -> r.kind() == SpeciesProfile.Kind.PASSIVE;
                case PLANT -> r.kind().isPlant();
            };
        }
    }

    // Données
    private long day, cycles, mutations;
    private int speciations;
    private double pressure;
    private final List<Float> pressureHistory = new ArrayList<>();
    private final List<SpeciesRecord> species = new ArrayList<>();
    private final List<HistoryEntry> history = new ArrayList<>();
    private Genome playerGenome;
    private boolean playerEvolution;
    private boolean canForce;

    // État
    private Tab tab = Tab.WORLD;
    private Filter filter = Filter.ALL;
    private String selected;
    private double listScroll, detailScroll, historyScroll, selfScroll;
    private int refreshCooldown;

    // Géométrie
    private int wx, wy, ww, wh;
    private static final int HEADER = 24;
    private static final int TABS_H = 16;
    private static final int LIST_W = 140;

    public JournalScreen(CompoundTag data) {
        super(Component.translatable("evolutionmod.journal.title"));
        setData(data);
    }

    public void setData(CompoundTag tag) {
        day = tag.getLong("Day");
        cycles = tag.getLong("Cycles");
        mutations = tag.getLong("Mutations");
        speciations = tag.getInt("Speciations");
        pressure = tag.getDouble("Pressure");
        pressureHistory.clear();
        for (int bits : tag.getIntArray("PressureHistory")) pressureHistory.add(Float.intBitsToFloat(bits));

        species.clear();
        ListTag list = tag.getList("Species", 10);
        for (int i = 0; i < list.size(); i++) {
            try {
                species.add(SpeciesRecord.load(list.getCompound(i)));
            } catch (Exception ignored) {
                // espèce illisible : on l'ignore simplement
            }
        }
        species.sort(Comparator.comparingDouble(SpeciesRecord::divergence).reversed());

        history.clear();
        ListTag h = tag.getList("History", 10);
        for (int i = 0; i < h.size(); i++) history.add(HistoryEntry.load(h.getCompound(i)));

        playerEvolution = tag.getBoolean("PlayerEvolution");
        canForce = tag.getBoolean("CanForce");
        CompoundTag p = tag.getCompound("Player");
        playerGenome = Genome.isValid(p) ? Genome.load(p) : null;

        if (selected == null && !species.isEmpty()) selected = species.get(0).id();
    }

    @Override
    protected void init() {
        ww = Math.min(width - 16, 470);
        wh = Math.min(height - 16, 280);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (refreshCooldown > 0) refreshCooldown--;
    }

    // ───────────────────────── Rendu ─────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
        GuiDraw.window(g, wx, wy, ww, wh);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        List<Component> tooltip = new ArrayList<>();

        // En-tête
        g.drawString(font, Component.literal("✦ ").withStyle(ChatFormatting.AQUA).append(title.copy()
            .withStyle(ChatFormatting.BOLD)), wx + 8, wy + 8, GuiDraw.TEXT, true);
        Component dayText = Component.translatable("evolutionmod.journal.header_day", day, cycles);
        int refreshX = wx + ww - 20;
        int forceX = refreshX - 18;
        int textRight = canForce ? forceX - 6 : refreshX - 6;
        g.drawString(font, dayText, textRight - font.width(dayText), wy + 8, GuiDraw.TEXT_DIM, false);
        if (canForce) {
            boolean hoverForce = GuiDraw.inside(mouseX, mouseY, forceX, wy + 5, 14, 14);
            g.fill(forceX, wy + 5, forceX + 14, wy + 19, hoverForce ? 0xFF3A2A18 : 0xFF101B23);
            g.renderOutline(forceX, wy + 5, 14, 14, hoverForce ? GuiDraw.WARN : GuiDraw.BORDER);
            g.drawCenteredString(font, "⚡", forceX + 7, wy + 8, refreshCooldown > 0 ? GuiDraw.TEXT_FAINT : GuiDraw.WARN);
            if (hoverForce) {
                tooltip.add(Component.translatable("evolutionmod.journal.force").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("evolutionmod.journal.force.desc").withStyle(ChatFormatting.GRAY));
            }
        }
        boolean hoverRefresh = GuiDraw.inside(mouseX, mouseY, refreshX, wy + 5, 14, 14);
        g.fill(refreshX, wy + 5, refreshX + 14, wy + 19, hoverRefresh ? 0xFF1E3442 : 0xFF101B23);
        g.renderOutline(refreshX, wy + 5, 14, 14, hoverRefresh ? GuiDraw.BORDER_LIGHT : GuiDraw.BORDER);
        g.drawCenteredString(font, "⟳", refreshX + 7, wy + 8, refreshCooldown > 0 ? GuiDraw.TEXT_FAINT : GuiDraw.ACCENT);
        if (hoverRefresh) tooltip.add(Component.translatable("evolutionmod.journal.refresh"));

        // Onglets
        int tx = wx + 8;
        int ty = wy + HEADER;
        for (Tab t : Tab.values()) {
            int tw = font.width(t.label()) + 16;
            boolean hover = GuiDraw.inside(mouseX, mouseY, tx, ty, tw, TABS_H);
            GuiDraw.tab(g, font, t.label(), tx, ty, tw, TABS_H, t == tab, hover);
            tx += tw + 3;
        }
        g.hLine(wx + 4, wx + ww - 5, ty + TABS_H - 1, GuiDraw.BORDER);

        int cx = wx + 8, cy = ty + TABS_H + 6, cw = ww - 16, ch = wh - (cy - wy) - 8;
        switch (tab) {
            case WORLD -> renderWorld(g, cx, cy, cw, ch, mouseX, mouseY, tooltip);
            case SPECIES -> renderSpecies(g, cx, cy, cw, ch, mouseX, mouseY, tooltip);
            case HISTORY -> renderHistory(g, cx, cy, cw, ch);
            case SELF -> renderSelf(g, cx, cy, cw, ch, mouseX, mouseY, tooltip);
        }

        if (!tooltip.isEmpty()) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    // ── Onglet Monde ──────────────────────────────────────────

    private void renderWorld(GuiGraphics g, int x, int y, int w, int h, int mx, int my, List<Component> tip) {
        int leftW = (w - 6) * 5 / 11;
        int cardW = (leftW - 4) / 2;
        int cardH = 30;
        long tracked = species.size();
        Object[][] cards = {
            {"day", String.valueOf(day), GuiDraw.ACCENT},
            {"cycles", String.valueOf(cycles), GuiDraw.ACCENT},
            {"mutations", compact(mutations), GuiDraw.ACCENT_2},
            {"speciations", String.valueOf(speciations), GuiDraw.WARN},
            {"species", String.valueOf(tracked), GuiDraw.GOOD},
            {"pressure", String.format(Locale.ROOT, "%.0f%%", pressure * 100), GuiDraw.BAD},
        };
        for (int i = 0; i < cards.length; i++) {
            int cx = x + (i % 2) * (cardW + 4);
            int cy = y + (i / 2) * (cardH + 4);
            GuiDraw.panel(g, cx, cy, cardW, cardH);
            g.drawString(font, Component.translatable("evolutionmod.journal.stat." + cards[i][0]),
                cx + 5, cy + 4, GuiDraw.TEXT_DIM, false);
            g.pose().pushPose();
            g.pose().translate(cx + 5, cy + 15, 0);
            g.pose().scale(1.4f, 1.4f, 1f);
            g.drawString(font, (String) cards[i][1], 0, 0, (Integer) cards[i][2], true);
            g.pose().popPose();
            if (GuiDraw.inside(mx, my, cx, cy, cardW, cardH)) {
                tip.add(Component.translatable("evolutionmod.journal.stat." + cards[i][0] + ".desc"));
            }
        }

        // Graphique de pression
        int gy = y + 3 * (cardH + 4);
        int gh = h - (gy - y);
        GuiDraw.panel(g, x, gy, leftW, gh);
        GuiDraw.panelTitle(g, font, Component.translatable("evolutionmod.journal.pressure_chart"), x, gy, leftW);
        GuiDraw.sparkline(g, x + 6, gy + 20, leftW - 12, Math.max(10, gh - 26), pressureHistory, 1.0f, GuiDraw.BAD);

        // Classement des espèces
        int rx = x + leftW + 6, rw = w - leftW - 6;
        int listH = h * 6 / 10;
        GuiDraw.panel(g, rx, y, rw, listH);
        GuiDraw.panelTitle(g, font, Component.translatable("evolutionmod.journal.top_species"), rx, y, rw);
        int rowY = y + 20;
        int shown = 0;
        for (SpeciesRecord r : species) {
            if (rowY + 14 > y + listH - 2) break;
            boolean hover = GuiDraw.inside(mx, my, rx + 2, rowY - 1, rw - 4, 14);
            if (hover) g.fill(rx + 2, rowY - 2, rx + rw - 2, rowY + 12, 0x2057E3C2);
            String name = GuiDraw.fit(font, EvolutionManager.speciesComponent(r.id()).getString(), rw / 2 - 10);
            g.drawString(font, (shown + 1) + ". " + name, rx + 6, rowY + 1, GuiDraw.TEXT, false);
            int bx = rx + rw / 2 + 4, bw = rw / 2 - 48;
            GuiDraw.bar(g, bx, rowY + 2, bw, 5, r.divergence() / 0.35, GuiDraw.divergenceColor(r.divergence()));
            g.drawString(font, GuiDraw.pct(r.divergence()), bx + bw + 5, rowY + 1, GuiDraw.TEXT_DIM, false);
            if (hover) tip.add(Component.translatable("evolutionmod.journal.click_species"));
            rowY += 14;
            shown++;
        }
        if (species.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("evolutionmod.journal.no_species"), rx + 6, y + 22, rw - 12, GuiDraw.TEXT_DIM);
        }

        // Dernier événement
        int ey = y + listH + 6, eh = h - listH - 6;
        GuiDraw.panel(g, rx, ey, rw, eh);
        GuiDraw.panelTitle(g, font, Component.translatable("evolutionmod.journal.latest"), rx, ey, rw);
        int line = ey + 20;
        for (int i = history.size() - 1; i >= 0 && line + 10 < ey + eh; i--) {
            HistoryEntry e = history.get(i);
            g.fill(rx + 6, line + 2, rx + 10, line + 6, e.type().color);
            Component text = EvolutionCommand.historyText(e);
            String s = GuiDraw.fit(font, text.getString(), rw - 60);
            g.drawString(font, s, rx + 14, line, GuiDraw.TEXT, false);
            String d = Component.translatable("evolutionmod.journal.day", e.day()).getString();
            g.drawString(font, d, rx + rw - 6 - font.width(d), line, GuiDraw.TEXT_FAINT, false);
            line += 11;
        }
        if (history.isEmpty()) {
            g.drawString(font, Component.translatable("evolutionmod.journal.history.empty"), rx + 6, ey + 20, GuiDraw.TEXT_DIM, false);
        }
    }

    // ── Onglet Espèces ────────────────────────────────────────

    private List<SpeciesRecord> filtered() {
        List<SpeciesRecord> out = new ArrayList<>();
        for (SpeciesRecord r : species) if (filter.accepts(r)) out.add(r);
        return out;
    }

    private SpeciesRecord selectedRecord() {
        for (SpeciesRecord r : species) if (r.id().equals(selected)) return r;
        return null;
    }

    private int listX, listY, listW, listH;
    private int detailX, detailY, detailW, detailH;

    private void renderSpecies(GuiGraphics g, int x, int y, int w, int h, int mx, int my, List<Component> tip) {
        // Filtres
        int fx = x;
        for (Filter f : Filter.values()) {
            int fw = font.width(f.label()) + 10;
            boolean active = f == filter;
            boolean hover = GuiDraw.inside(mx, my, fx, y, fw, 12);
            g.fill(fx, y, fx + fw, y + 12, active ? 0xFF1E3442 : hover ? 0xFF16252F : 0xFF0D161D);
            g.renderOutline(fx, y, fw, 12, active ? GuiDraw.ACCENT : GuiDraw.BORDER);
            g.drawCenteredString(font, f.label(), fx + fw / 2, y + 2, active ? GuiDraw.TEXT : GuiDraw.TEXT_DIM);
            fx += fw + 2;
        }

        listX = x;
        listY = y + 16;
        listW = LIST_W;
        listH = h - 16;
        GuiDraw.panel(g, listX, listY, listW, listH);
        List<SpeciesRecord> rows = filtered();
        int rowH = 22;
        int maxScroll = Math.max(0, rows.size() * rowH - (listH - 4));
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        g.enableScissor(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1);
        int ry = listY + 2 - (int) listScroll;
        for (SpeciesRecord r : rows) {
            if (ry + rowH >= listY && ry <= listY + listH) {
                boolean sel = r.id().equals(selected);
                boolean hover = GuiDraw.inside(mx, my, listX + 2, ry, listW - 4, rowH - 2)
                    && my >= listY && my < listY + listH;
                if (sel) g.fill(listX + 2, ry, listX + listW - 2, ry + rowH - 2, 0x5038A0C8);
                else if (hover) g.fill(listX + 2, ry, listX + listW - 2, ry + rowH - 2, 0x2057E3C2);
                if (sel) g.vLine(listX + 2, ry, ry + rowH - 3, GuiDraw.ACCENT);
                String name = GuiDraw.fit(font, EvolutionManager.speciesComponent(r.id()).getString(), listW - 30);
                g.drawString(font, name, listX + 6, ry + 2, sel ? GuiDraw.TEXT : 0xFFC9D6DC, false);
                if (!r.lineages().isEmpty()) {
                    GuiDraw.chip(g, font, Component.literal(String.valueOf(r.lineages().size())),
                        listX + listW - 20, ry + 1, GuiDraw.WARN);
                }
                GuiDraw.bar(g, listX + 6, ry + 13, listW - 16, 3, r.divergence() / 0.35,
                    GuiDraw.divergenceColor(r.divergence()));
            }
            ry += rowH;
        }
        g.disableScissor();
        scrollbar(g, listX + listW - 3, listY + 2, listH - 4, listScroll, maxScroll, rows.size() * rowH);
        if (rows.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("evolutionmod.journal.no_species"), listX + 6, listY + 6, listW - 12, GuiDraw.TEXT_DIM);
        }

        // Détail
        detailX = listX + listW + 6;
        detailY = y + 16;
        detailW = w - listW - 6;
        detailH = h - 16;
        GuiDraw.panel(g, detailX, detailY, detailW, detailH);
        SpeciesRecord r = selectedRecord();
        if (r == null) {
            g.drawString(font, Component.translatable("evolutionmod.journal.select_species"), detailX + 8, detailY + 8, GuiDraw.TEXT_DIM, false);
            return;
        }
        renderSpeciesDetail(g, r, mx, my, tip);
    }

    private void renderSpeciesDetail(GuiGraphics g, SpeciesRecord r, int mx, int my, List<Component> tip) {
        int x = detailX + 8, y = detailY + 6, w = detailW - 16;
        Component name = EvolutionManager.speciesComponent(r.id());
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(1.3f, 1.3f, 1f);
        g.drawString(font, name, 0, 0, GuiDraw.TEXT, true);
        g.pose().popPose();
        Component kind = Component.translatable("evolutionmod.kind." + r.kind().name().toLowerCase(Locale.ROOT));
        g.drawString(font, kind, x + w - font.width(kind), y + 2, GuiDraw.TEXT_FAINT, false);
        y += 15;

        // Lignée
        MutableComponent chain = name.copy().withStyle(ChatFormatting.GRAY);
        for (String l : r.lineages()) {
            chain.append(Component.literal(" → ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(l).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        String chainText = GuiDraw.fit(font, chain.getString(), w);
        if (font.width(chain) <= w) g.drawString(font, chain, x, y, GuiDraw.TEXT, false);
        else g.drawString(font, chainText, x, y, 0xFFC58BFF, false);
        if (GuiDraw.inside(mx, my, x, y, w, 9) && !r.lineages().isEmpty()) {
            tip.add(Component.translatable("evolutionmod.journal.lineage_tip"));
            tip.add(chain);
        }
        y += 12;

        // Statistiques
        int cx = x;
        boolean plant = r.kind().isPlant();
        cx += GuiDraw.chip(g, font, Component.translatable("evolutionmod.journal.divergence",
            GuiDraw.pct(r.divergence())), cx, y, GuiDraw.divergenceColor(r.divergence())) + 3;
        if (!plant) {
            cx += GuiDraw.chip(g, font, Component.translatable("evolutionmod.journal.population", r.population()), cx, y, GuiDraw.ACCENT) + 3;
            cx += GuiDraw.chip(g, font, Component.translatable("evolutionmod.journal.births", r.births()), cx, y, GuiDraw.GOOD) + 3;
            if (cx + 60 < x + w) {
                GuiDraw.chip(g, font, Component.translatable("evolutionmod.journal.killed", r.killedByPlayers()), cx, y, GuiDraw.BAD);
            }
        } else {
            GuiDraw.chip(g, font, Component.translatable("evolutionmod.journal.since", r.firstSeenDay()), cx, y, GuiDraw.ACCENT);
        }
        y += 15;

        // Courbe de divergence
        int chartH = 30;
        GuiDraw.sparkline(g, x, y, w, chartH, r.divergenceHistory(), 0.35f, GuiDraw.ACCENT_2);
        g.drawString(font, Component.translatable("evolutionmod.journal.divergence_chart"), x + 3, y + 2, GuiDraw.TEXT_FAINT, false);
        if (GuiDraw.inside(mx, my, x, y, w, chartH)) tip.add(Component.translatable("evolutionmod.journal.divergence_chart.desc"));
        y += chartH + 6;

        // Traits
        List<Traits.Trait> traits = Traits.of(r.template());
        int tx = x;
        for (Traits.Trait t : traits) {
            Component label = Component.translatable(t.translationKey());
            int tw = font.width(label) + 8;
            if (tx + tw > x + w) break;
            tx += GuiDraw.chip(g, font, label, tx, y, t.color()) + 3;
        }
        if (!traits.isEmpty()) y += 15;

        // Gènes (défilant)
        int gy = y, gh = detailY + detailH - 6 - gy;
        detailScroll = renderGeneList(g, r.template(), r.origin(), x, gy, w, gh, detailScroll, mx, my, tip,
            "evolutionmod.journal.ref_origin");
    }

    /** Liste de gènes défilante. Renvoie le défilement borné. */
    private double renderGeneList(GuiGraphics g, Genome genome, Genome reference, int x, int y, int w, int h,
                                  double scroll, int mx, int my, List<Component> tip, String refKey) {
        List<GeneType> genes = new ArrayList<>(genome.genes());
        genes.sort(Comparator.comparing((GeneType t) -> t.category).thenComparing(Enum::ordinal));
        int rowH = 13;
        int total = genes.size() * rowH;
        double max = Math.max(0, total - h);
        scroll = Mth.clamp(scroll, 0, max);
        int labelW = Math.min(92, w / 3);
        int valueW = 40;
        int barW = w - labelW - valueW - 12;
        g.enableScissor(x - 2, y, x + w + 2, y + h);
        int ry = y - (int) scroll;
        GeneType.Category last = null;
        for (GeneType t : genes) {
            if (ry + rowH >= y && ry <= y + h) {
                if (t.category != last) g.fill(x - 2, ry, x, ry + rowH - 2, t.category.color);
                double v = genome.get(t);
                double ref = reference != null ? reference.get(t) : t.neutral;
                boolean hover = GuiDraw.inside(mx, my, x, ry, w, rowH) && my >= y && my < y + h;
                if (hover) g.fill(x, ry - 1, x + w, ry + rowH - 1, 0x1857E3C2);
                String label = GuiDraw.fit(font, Component.translatable(t.translationKey()).getString(), labelW - 4);
                g.drawString(font, label, x + 3, ry + 2, 0xFFC9D6DC, false);
                GuiDraw.geneBar(g, x + labelW, ry + 3, barW, 5, t, v, ref, t.category.color);
                String val = t.format(v);
                int diffColor = Math.abs(t.distance(v, ref)) < 0.02 ? GuiDraw.TEXT_DIM
                    : (t == GeneType.HUE ? GuiDraw.TEXT : (v > ref ? GuiDraw.GOOD : GuiDraw.WARN));
                g.drawString(font, val, x + w - font.width(val) - 2, ry + 2, diffColor, false);
                if (hover) {
                    tip.add(Component.translatable(t.translationKey()).withStyle(s -> s.withColor(t.category.color)));
                    tip.add(Component.translatable(t.descriptionKey()).withStyle(ChatFormatting.GRAY));
                    tip.add(Component.translatable(t.category.translationKey()).withStyle(ChatFormatting.DARK_GRAY));
                    tip.add(Component.translatable(refKey, t.format(ref)).withStyle(ChatFormatting.DARK_GRAY));
                    if (genome.mutationsOf(t) > 0) {
                        tip.add(Component.translatable("evolutionmod.journal.gene_mutations", genome.mutationsOf(t))
                            .withStyle(ChatFormatting.DARK_PURPLE));
                    }
                }
            }
            last = t.category;
            ry += rowH;
        }
        g.disableScissor();
        scrollbar(g, x + w + 3, y, h, scroll, max, total);
        return scroll;
    }

    // ── Onglet Histoire ───────────────────────────────────────

    private int histX, histY, histW, histH;

    private void renderHistory(GuiGraphics g, int x, int y, int w, int h) {
        histX = x;
        histY = y;
        histW = w;
        histH = h;
        GuiDraw.panel(g, x, y, w, h);
        if (history.isEmpty()) {
            g.drawWordWrap(font, Component.translatable("evolutionmod.journal.history.empty_long"), x + 10, y + 10, w - 20, GuiDraw.TEXT_DIM);
            return;
        }
        int rowH = 16;
        int total = history.size() * rowH;
        double max = Math.max(0, total - (h - 6));
        historyScroll = Mth.clamp(historyScroll, 0, max);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int ry = y + 4 - (int) historyScroll;
        // timeline verticale
        g.vLine(x + 58, y + 1, y + h - 2, 0xFF22343F);
        for (int i = history.size() - 1; i >= 0; i--) {
            HistoryEntry e = history.get(i);
            if (ry + rowH >= y && ry <= y + h) {
                String d = Component.translatable("evolutionmod.journal.day", e.day()).getString();
                g.drawString(font, d, x + 52 - font.width(d), ry + 3, GuiDraw.TEXT_FAINT, false);
                g.fill(x + 56, ry + 3, x + 61, ry + 8, e.type().color);
                Component text = EvolutionCommand.historyText(e);
                g.drawString(font, GuiDraw.fit(font, text.getString(), w - 80), x + 68, ry + 3, GuiDraw.TEXT, false);
            }
            ry += rowH;
        }
        g.disableScissor();
        scrollbar(g, x + w - 4, y + 2, h - 4, historyScroll, max, total);
    }

    // ── Onglet Mon génome ─────────────────────────────────────

    private int selfX, selfY, selfW, selfH;

    private void renderSelf(GuiGraphics g, int x, int y, int w, int h, int mx, int my, List<Component> tip) {
        GuiDraw.panel(g, x, y, w, h);
        if (!playerEvolution || playerGenome == null) {
            g.drawWordWrap(font, Component.translatable("evolutionmod.journal.self.disabled"), x + 10, y + 10, w - 20, GuiDraw.TEXT_DIM);
            return;
        }
        int infoW = Math.min(150, w / 3);
        int ix = x + 8, iy = y + 8;
        g.drawString(font, Component.translatable("evolutionmod.journal.self.title"), ix, iy, GuiDraw.ACCENT, true);
        iy += 14;
        Genome origin = Genome.fromProfile(SpeciesProfile.forEntity(minecraft.player));
        double div = playerGenome.divergenceFrom(origin);
        g.drawString(font, Component.translatable("evolutionmod.journal.divergence", GuiDraw.pct(div)), ix, iy, GuiDraw.TEXT, false);
        iy += 11;
        GuiDraw.bar(g, ix, iy, infoW - 16, 5, div / 0.35, GuiDraw.divergenceColor(div));
        iy += 11;
        Component mutLine = Component.translatable("evolutionmod.tip.mutations", playerGenome.totalMutations(),
            playerGenome.generation());
        g.drawWordWrap(font, mutLine, ix, iy, infoW - 12, GuiDraw.TEXT_DIM);
        iy += font.split(mutLine, infoW - 12).size() * 9 + 5;
        g.drawWordWrap(font, Component.translatable("evolutionmod.journal.self.explain"), ix, iy, infoW - 12, GuiDraw.TEXT_FAINT);
        int traitsY = iy + font.split(Component.translatable("evolutionmod.journal.self.explain"), infoW - 12).size() * 9 + 6;
        int tx = ix;
        for (Traits.Trait t : Traits.of(playerGenome)) {
            Component label = Component.translatable(t.translationKey());
            int tw = font.width(label) + 8;
            if (tx + tw > x + infoW) {
                tx = ix;
                traitsY += 13;
            }
            if (traitsY + 11 > y + h) break;
            tx += GuiDraw.chip(g, font, label, tx, traitsY, t.color()) + 3;
        }

        selfX = x + infoW + 4;
        selfY = y + 8;
        selfW = w - infoW - 16;
        selfH = h - 16;
        selfScroll = renderGeneList(g, playerGenome, origin, selfX, selfY, selfW, selfH, selfScroll, mx, my, tip,
            "evolutionmod.journal.ref_human");
    }

    private void scrollbar(GuiGraphics g, int x, int y, int h, double scroll, double max, int total) {
        if (max <= 0) return;
        g.fill(x, y, x + 2, y + h, 0x40000000);
        int thumb = Math.max(12, (int) (h * (h / (double) total)));
        int ty = y + (int) ((h - thumb) * (scroll / max));
        g.fill(x, ty, x + 2, ty + thumb, GuiDraw.BORDER_LIGHT);
    }

    private static String compact(long n) {
        if (n >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0);
        if (n >= 10_000) return String.format(Locale.ROOT, "%.1fk", n / 1000.0);
        return String.valueOf(n);
    }

    // ───────────────────────── Entrées ─────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            // Rafraîchir
            if (GuiDraw.inside(mx, my, wx + ww - 20, wy + 5, 14, 14) && refreshCooldown == 0) {
                EvoNetwork.sendToServer(new RequestJournalMsg());
                refreshCooldown = 20;
                click();
                return true;
            }
            // Forcer un cycle (opérateurs)
            if (canForce && GuiDraw.inside(mx, my, wx + ww - 38, wy + 5, 14, 14) && refreshCooldown == 0) {
                EvoNetwork.sendToServer(new com.evolutionmod.network.ForceCycleMsg(hasShiftDown() ? 5 : 1));
                refreshCooldown = 30;
                click();
                return true;
            }
            // Onglets
            int tx = wx + 8, ty = wy + HEADER;
            for (Tab t : Tab.values()) {
                int tw = font.width(t.label()) + 16;
                if (GuiDraw.inside(mx, my, tx, ty, tw, TABS_H)) {
                    if (tab != t) click();
                    tab = t;
                    return true;
                }
                tx += tw + 3;
            }
            int cx = wx + 8, cy = ty + TABS_H + 6, cw = ww - 16;
            if (tab == Tab.WORLD) {
                int leftW = (cw - 6) * 5 / 11;
                int rx = cx + leftW + 6, rw = cw - leftW - 6;
                int rowY = cy + 20;
                for (SpeciesRecord r : species) {
                    if (GuiDraw.inside(mx, my, rx + 2, rowY - 1, rw - 4, 14)) {
                        selected = r.id();
                        filter = Filter.ALL;
                        tab = Tab.SPECIES;
                        detailScroll = 0;
                        click();
                        return true;
                    }
                    rowY += 14;
                }
            } else if (tab == Tab.SPECIES) {
                int fx = cx;
                for (Filter f : Filter.values()) {
                    int fw = font.width(f.label()) + 10;
                    if (GuiDraw.inside(mx, my, fx, cy, fw, 12)) {
                        filter = f;
                        listScroll = 0;
                        click();
                        return true;
                    }
                    fx += fw + 2;
                }
                if (GuiDraw.inside(mx, my, listX, listY, listW, listH)) {
                    int idx = (int) ((my - listY - 2 + listScroll) / 22);
                    List<SpeciesRecord> rows = filtered();
                    if (idx >= 0 && idx < rows.size()) {
                        selected = rows.get(idx).id();
                        detailScroll = 0;
                        click();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        double delta = -scrollY * 18;
        switch (tab) {
            case SPECIES -> {
                if (GuiDraw.inside(mx, my, listX, listY, listW, listH)) listScroll += delta;
                else detailScroll += delta;
            }
            case HISTORY -> historyScroll += delta;
            case SELF -> selfScroll += delta;
            default -> { }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_TAB) {
            tab = Tab.values()[(tab.ordinal() + 1) % Tab.values().length];
            return true;
        }
        if (com.evolutionmod.client.ClientModEvents.OPEN_JOURNAL.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void click() {
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
