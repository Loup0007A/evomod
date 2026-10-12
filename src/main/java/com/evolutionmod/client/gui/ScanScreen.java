package com.evolutionmod.client.gui;

import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.Traits;
import com.evolutionmod.world.SpeciesRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Écran d'analyse génétique d'une créature : rendu 3D, traits, comparaison
 * de chaque gène avec la moyenne de son espèce.
 */
public class ScanScreen extends Screen {

    private final int entityId;
    private final Genome genome;
    private final SpeciesRecord species;
    private double scroll;
    private int wx, wy, ww, wh;
    private float spin;

    public ScanScreen(int entityId, CompoundTag genomeTag, CompoundTag speciesTag) {
        super(Component.translatable("evolutionmod.scan.title"));
        this.entityId = entityId;
        this.genome = Genome.load(genomeTag);
        SpeciesRecord r = null;
        if (speciesTag != null && speciesTag.contains("Id")) {
            try {
                r = SpeciesRecord.load(speciesTag);
            } catch (Exception ignored) {
                // données d'espèce indisponibles
            }
        }
        this.species = r;
    }

    @Override
    protected void init() {
        ww = Math.min(width - 16, 400);
        wh = Math.min(height - 16, 236);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        spin += 2.5f;
    }

    private LivingEntity entity() {
        if (minecraft == null || minecraft.level == null) return null;
        Entity e = minecraft.level.getEntity(entityId);
        return e instanceof LivingEntity l ? l : null;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
        GuiDraw.window(g, wx, wy, ww, wh);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        List<Component> tip = new ArrayList<>();
        LivingEntity entity = entity();

        // ── Colonne gauche : spécimen ──
        int lx = wx + 8, ly = wy + 8, lw = 120, lh = wh - 16;
        GuiDraw.panel(g, lx, ly, lw, lh);
        int boxH = Math.min(120, lh - 70);
        // grille « holographique »
        for (int i = 0; i < lw; i += 10) g.vLine(lx + i, ly + 1, ly + boxH, 0x1238A0C8);
        for (int j = 0; j < boxH; j += 10) g.hLine(lx + 1, lx + lw - 2, ly + j, 0x1238A0C8);
        int scanLine = ly + (int) ((spin * 1.2f) % boxH);
        g.hLine(lx + 1, lx + lw - 2, scanLine, 0x6057E3C2);
        if (entity != null) {
            float h = Math.max(0.3f, entity.getBbHeight() / Math.max(0.1f, entity.getScale()));
            float wdt = Math.max(0.3f, entity.getBbWidth() / Math.max(0.1f, entity.getScale()));
            int scale = (int) Math.max(6, Math.min((boxH - 20) / h, (lw - 20) / (wdt * 1.4f)));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, lx + 2, ly + 2, lx + lw - 2, ly + boxH - 2,
                scale, 0.0625f, mx, my, entity);
        } else {
            g.drawCenteredString(font, Component.translatable("evolutionmod.scan.out_of_range"), lx + lw / 2,
                ly + boxH / 2, GuiDraw.TEXT_FAINT);
        }
        g.hLine(lx + 1, lx + lw - 2, ly + boxH, 0xFF22343F);

        int iy = ly + boxH + 5;
        Component name = entity != null ? entity.getDisplayName() : EvolutionManager.speciesComponent(genome.species());
        g.drawCenteredString(font, Component.literal(GuiDraw.fit(font, name.getString(), lw - 6)), lx + lw / 2, iy, GuiDraw.TEXT);
        iy += 11;
        if (!genome.lineage().isEmpty()) {
            g.drawCenteredString(font, Component.literal(genome.lineage()).withStyle(ChatFormatting.LIGHT_PURPLE,
                ChatFormatting.ITALIC), lx + lw / 2, iy, 0xFFC58BFF);
            iy += 11;
        }
        if (entity != null) {
            String hp = String.format("❤ %.0f/%.0f", entity.getHealth(), entity.getMaxHealth());
            g.drawCenteredString(font, hp, lx + lw / 2, iy, 0xFFFF7A8A);
            iy += 11;
        }
        // Traits
        int tx = lx + 4;
        int tyy = iy + 2;
        for (Traits.Trait t : Traits.of(genome)) {
            Component label = Component.translatable(t.translationKey());
            int tw = font.width(label) + 8;
            if (tx + tw > lx + lw - 3) {
                tx = lx + 4;
                tyy += 13;
            }
            if (tyy + 11 > ly + lh - 2) break;
            tx += GuiDraw.chip(g, font, label, tx, tyy, t.color()) + 3;
        }

        // ── Colonne droite : analyse ──
        int rx = lx + lw + 6, ry = wy + 8, rw = wx + ww - 8 - rx;
        g.drawString(font, Component.literal("⌬ ").withStyle(ChatFormatting.AQUA).append(title.copy()
            .withStyle(ChatFormatting.BOLD)), rx, ry, GuiDraw.TEXT, true);
        ry += 14;

        Genome template = species != null ? species.template() : null;
        Genome origin = species != null ? species.origin() : null;
        double divIndiv = template != null ? genome.divergenceFrom(template) : 0;
        double divOrigin = origin != null ? genome.divergenceFrom(origin) : 0;

        int cx = rx;
        cx += GuiDraw.chip(g, font, Component.translatable("evolutionmod.scan.mutations", genome.totalMutations()),
            cx, ry, GuiDraw.ACCENT_2) + 3;
        cx += GuiDraw.chip(g, font, Component.translatable("evolutionmod.scan.generation", genome.generation()),
            cx, ry, GuiDraw.ACCENT) + 3;
        ry += 15;

        g.drawString(font, Component.translatable("evolutionmod.scan.vs_origin"), rx, ry, GuiDraw.TEXT_DIM, false);
        GuiDraw.bar(g, rx + 96, ry + 2, rw - 140, 5, divOrigin / 0.35, GuiDraw.divergenceColor(divOrigin));
        g.drawString(font, GuiDraw.pct(divOrigin), rx + rw - 38, ry, GuiDraw.TEXT, false);
        ry += 11;
        g.drawString(font, Component.translatable("evolutionmod.scan.vs_species"), rx, ry, GuiDraw.TEXT_DIM, false);
        GuiDraw.bar(g, rx + 96, ry + 2, rw - 140, 5, divIndiv / 0.2, GuiDraw.ACCENT);
        g.drawString(font, GuiDraw.pct(divIndiv), rx + rw - 38, ry, GuiDraw.TEXT, false);
        if (GuiDraw.inside(mx, my, rx, ry - 11, rw, 22)) {
            tip.add(Component.translatable("evolutionmod.scan.divergence_tip"));
        }
        ry += 14;

        // Légende
        g.drawString(font, Component.translatable("evolutionmod.scan.legend"), rx, ry, GuiDraw.TEXT_FAINT, false);
        ry += 11;

        // Liste des gènes
        int gh = wy + wh - 8 - ry;
        List<GeneType> genes = new ArrayList<>(genome.genes());
        genes.sort(Comparator.comparing((GeneType t) -> t.category).thenComparing(Enum::ordinal));
        int rowH = 13;
        int total = genes.size() * rowH;
        double max = Math.max(0, total - gh);
        scroll = Mth.clamp(scroll, 0, max);
        int labelW = Math.min(90, rw / 3);
        int valueW = 40;
        int barW = rw - labelW - valueW - 10;
        g.enableScissor(rx - 2, ry, rx + rw + 2, ry + gh);
        int y = ry - (int) scroll;
        for (GeneType t : genes) {
            if (y + rowH >= ry && y <= ry + gh) {
                double v = genome.get(t);
                double ref = template != null ? template.get(t) : t.neutral;
                boolean hover = GuiDraw.inside(mx, my, rx, y, rw, rowH) && my >= ry && my < ry + gh;
                if (hover) g.fill(rx, y - 1, rx + rw, y + rowH - 1, 0x1857E3C2);
                g.fill(rx - 2, y, rx, y + rowH - 2, t.category.color);
                g.drawString(font, GuiDraw.fit(font, Component.translatable(t.translationKey()).getString(), labelW - 4),
                    rx + 3, y + 2, 0xFFC9D6DC, false);
                GuiDraw.geneBar(g, rx + labelW, y + 3, barW, 5, t, v, ref, t.category.color);
                String val = t.format(v);
                g.drawString(font, val, rx + rw - font.width(val) - 2, y + 2, GuiDraw.TEXT, false);
                if (hover) {
                    tip.add(Component.translatable(t.translationKey()).withStyle(s -> s.withColor(t.category.color)));
                    tip.add(Component.translatable(t.descriptionKey()).withStyle(ChatFormatting.GRAY));
                    tip.add(Component.translatable("evolutionmod.journal.ref_species", t.format(ref))
                        .withStyle(ChatFormatting.DARK_GRAY));
                    if (genome.mutationsOf(t) > 0) {
                        tip.add(Component.translatable("evolutionmod.journal.gene_mutations", genome.mutationsOf(t))
                            .withStyle(ChatFormatting.DARK_PURPLE));
                    }
                }
            }
            y += rowH;
        }
        g.disableScissor();
        if (max > 0) {
            g.fill(rx + rw + 3, ry, rx + rw + 5, ry + gh, 0x40000000);
            int thumb = Math.max(12, (int) (gh * (gh / (double) total)));
            int ty = ry + (int) ((gh - thumb) * (scroll / max));
            g.fill(rx + rw + 3, ty, rx + rw + 5, ty + thumb, GuiDraw.BORDER_LIGHT);
        }

        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scroll -= scrollY * 16;
        return true;
    }
}
