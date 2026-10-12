package com.evolutionmod.client.gui;

import com.evolutionmod.blocks.GeneticLabBlockEntity;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.items.GeneticCrystalItem;
import com.evolutionmod.menu.GeneticLabMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Interface du laboratoire génétique, entièrement dessinée (pas de texture) :
 * emplacements, flux animé, barre de progression et aperçu comparatif des
 * deux génomes.
 */
public class GeneticLabScreen extends AbstractContainerScreen<GeneticLabMenu> {

    private int animTick;

    public GeneticLabScreen(GeneticLabMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GeneticLabMenu.IMAGE_WIDTH;
        this.imageHeight = GeneticLabMenu.IMAGE_HEIGHT;
        this.inventoryLabelX = GeneticLabMenu.INV_X;
        this.inventoryLabelY = GeneticLabMenu.INV_Y - 11;
        this.titleLabelX = 8;
        this.titleLabelY = 7;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        animTick++;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mx, int my) {
        int x = leftPos, y = topPos;
        GuiDraw.window(g, x, y, imageWidth, imageHeight);

        // Zone de travail
        GuiDraw.panel(g, x + 6, y + 19, 136, 90);
        // Zone d'aperçu
        GuiDraw.panel(g, x + 146, y + 19, imageWidth - 152, 90);

        // Emplacements (cadres)
        for (int i = 0; i < 4; i++) {
            Slot s = menu.slots.get(i);
            int color = switch (i) {
                case GeneticLabBlockEntity.SLOT_SOURCE -> GuiDraw.ACCENT;
                case GeneticLabBlockEntity.SLOT_REAGENT -> GuiDraw.ACCENT_2;
                case GeneticLabBlockEntity.SLOT_ENERGY -> 0xFFFF5B5B;
                default -> GuiDraw.WARN;
            };
            slotFrame(g, x + s.x, y + s.y, color, i == GeneticLabBlockEntity.SLOT_OUTPUT);
        }
        for (int i = 4; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF0A1015);
            g.renderOutline(x + s.x - 1, y + s.y - 1, 18, 18, 0xFF22343F);
        }

        // Flux : A ─┐
        //           ├──► sortie
        //       B ─┘
        float progress = menu.progress() / (float) menu.maxProgress();
        boolean running = menu.progress() > 0;
        int pipe = running ? GuiDraw.ACCENT : 0xFF2A3B46;
        int ax = x + GeneticLabMenu.SOURCE_X + 18, ay = y + GeneticLabMenu.SOURCE_Y + 8;
        int bx = x + GeneticLabMenu.REAGENT_X + 18, by = y + GeneticLabMenu.REAGENT_Y + 8;
        int jx = x + 82;
        int jy = y + GeneticLabMenu.OUTPUT_Y + 8;
        g.fill(ax, ay - 1, jx, ay + 1, pipe);
        g.fill(bx, by - 1, jx, by + 1, pipe);
        g.fill(jx - 1, ay - 1, jx + 1, by + 1, pipe);
        // Barre de progression horizontale vers la sortie
        int px0 = jx, px1 = x + GeneticLabMenu.OUTPUT_X - 9;
        g.fill(px0, jy - 3, px1, jy + 3, 0xFF0A1015);
        int fill = (int) ((px1 - px0) * progress);
        if (fill > 0) g.fillGradient(px0, jy - 3, px0 + fill, jy + 3, 0xFF9BFFE6, GuiDraw.ACCENT);
        g.renderOutline(px0 - 1, jy - 4, px1 - px0 + 2, 8, 0xFF22343F);
        // pointe de flèche
        for (int i = 0; i < 4; i++) g.vLine(px1 + i, jy - 3 + i, jy + 3 - i, fill >= px1 - px0 - 1 ? GuiDraw.ACCENT : 0xFF2A3B46);
        // Bulles animées
        if (running) {
            for (int i = 0; i < 3; i++) {
                int t = (animTick * 2 + i * 13) % (jx - ax);
                g.fill(ax + t, ay - 1, ax + t + 2, ay + 1, 0xFFFFFFFF);
                g.fill(bx + t, by - 1, bx + t + 2, by + 1, 0xFFFFFFFF);
            }
        }
        // Lien énergie
        int ex = x + GeneticLabMenu.ENERGY_X + 8, ey = y + GeneticLabMenu.ENERGY_Y;
        g.vLine(ex, jy + 4, ey - 2, running ? 0xFFFF5B5B : 0xFF3A2A2E);
    }

    private static void slotFrame(GuiGraphics g, int sx, int sy, int color, boolean big) {
        int pad = big ? 4 : 1;
        g.fill(sx - pad, sy - pad, sx + 16 + pad, sy + 16 + pad, 0xFF0A1015);
        g.renderOutline(sx - pad, sy - pad, 18 + 2 * pad - 2, 18 + 2 * pad - 2, color);
        g.fill(sx - pad, sy - pad, sx - pad + 3, sy - pad + 1, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, GuiDraw.ACCENT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiDraw.TEXT_DIM, false);

        // Étiquettes d'emplacements
        g.drawString(font, "A", GeneticLabMenu.SOURCE_X - 10, GeneticLabMenu.SOURCE_Y + 4, GuiDraw.ACCENT, false);
        g.drawString(font, "B", GeneticLabMenu.REAGENT_X - 10, GeneticLabMenu.REAGENT_Y + 4, GuiDraw.ACCENT_2, false);
        g.drawString(font, "⚡", GeneticLabMenu.ENERGY_X - 10, GeneticLabMenu.ENERGY_Y + 4, 0xFFFF5B5B, false);

        // Mode actuel (calculé localement à partir des emplacements)
        GeneticLabBlockEntity.Mode mode = GeneticLabBlockEntity.detectMode(
            menu.container().getItem(GeneticLabBlockEntity.SLOT_SOURCE),
            menu.container().getItem(GeneticLabBlockEntity.SLOT_REAGENT));
        Component modeText = Component.translatable("evolutionmod.lab.mode." + mode.name().toLowerCase(Locale.ROOT));
        int modeColor = mode == GeneticLabBlockEntity.Mode.NONE ? GuiDraw.TEXT_FAINT : GuiDraw.WARN;
        g.drawCenteredString(font, modeText, 74, 23, modeColor);
        if (menu.progress() > 0) {
            String pct = (menu.progress() * 100 / menu.maxProgress()) + "%";
            g.drawCenteredString(font, pct, 98, GeneticLabMenu.OUTPUT_Y + 14, GuiDraw.TEXT);
        }

        renderPreview(g);
    }

    /** Aperçu comparatif des génomes A et B (ou A et résultat attendu). */
    private void renderPreview(GuiGraphics g) {
        int px = 150, py = 23, pw = imageWidth - 160;
        ItemStack a = menu.container().getItem(GeneticLabBlockEntity.SLOT_SOURCE);
        ItemStack b = menu.container().getItem(GeneticLabBlockEntity.SLOT_REAGENT);
        Genome ga = GeneticCrystalItem.getGenome(a);
        Genome gb = GeneticCrystalItem.getGenome(b);

        if (ga == null) {
            g.drawWordWrap(font, Component.translatable("evolutionmod.lab.help"), px, py, pw, GuiDraw.TEXT_FAINT);
            return;
        }
        g.drawString(font, GuiDraw.fit(font, EvolutionManager.speciesComponent(ga.species()).getString(), pw),
            px, py, GuiDraw.ACCENT, false);
        py += 10;
        if (gb != null && !gb.species().equals(ga.species())) {
            g.drawWordWrap(font, Component.translatable("evolutionmod.lab.incompatible").withStyle(ChatFormatting.RED),
                px, py, pw, GuiDraw.BAD);
            return;
        }
        List<GeneType> genes = new ArrayList<>(ga.genes());
        genes.removeIf(t -> t == GeneType.HUE);
        genes.sort(Comparator.comparingDouble((GeneType t) -> -t.distance(ga.get(t), t.neutral)));
        int rows = Math.min(genes.size(), 7);
        for (int i = 0; i < rows; i++) {
            GeneType t = genes.get(i);
            String name = GuiDraw.fit(font, Component.translatable(t.translationKey()).getString(), 44);
            g.drawString(font, name, px, py + 1, 0xFFB0C0C8, false);
            int bx = px + 46, bw = pw - 46;
            GuiDraw.geneBar(g, bx, py + 1, bw, 3, t, ga.get(t), gb != null ? gb.get(t) : t.neutral, GuiDraw.ACCENT);
            if (gb != null) {
                GuiDraw.geneBar(g, bx, py + 6, bw, 2, t, gb.get(t), gb.get(t), GuiDraw.ACCENT_2);
            }
            py += 10;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        renderTooltip(g, mx, my);
        // Aide sur le mode
        if (GuiDraw.inside(mx, my, leftPos + 20, topPos + 20, 110, 12)) {
            g.renderComponentTooltip(font, List.of(
                Component.translatable("evolutionmod.lab.mode.fusion").withStyle(ChatFormatting.GOLD),
                Component.translatable("evolutionmod.lab.mode.fusion.desc").withStyle(ChatFormatting.GRAY),
                Component.translatable("evolutionmod.lab.mode.clone").withStyle(ChatFormatting.GOLD),
                Component.translatable("evolutionmod.lab.mode.clone.desc").withStyle(ChatFormatting.GRAY),
                Component.translatable("evolutionmod.lab.mode.mutation").withStyle(ChatFormatting.GOLD),
                Component.translatable("evolutionmod.lab.mode.mutation.desc").withStyle(ChatFormatting.GRAY),
                Component.translatable("evolutionmod.lab.energy").withStyle(ChatFormatting.RED)), mx, my);
        }
    }
}
