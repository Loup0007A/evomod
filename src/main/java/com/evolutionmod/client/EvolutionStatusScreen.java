package com.evolutionmod.client;

import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.network.EvolutionStatusPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Écran interactif pour /evolution status.
 *
 * Affiche une liste cliquable de toutes les espèces évolvables (plantes,
 * mobs, et joueur) à gauche, et le détail complet des gènes de l'espèce
 * sélectionnée à droite — barre de progression par gène, génération de
 * mutation, lignée évolutive.
 *
 * Ouvert côté client uniquement, à la réception d'un EvolutionStatusPacket
 * (voir EvolutionCommand côté serveur pour le déclenchement).
 */
public class EvolutionStatusScreen extends Screen {

    private static final int LIST_WIDTH = 160;
    private static final int PADDING = 8;

    private final Map<String, net.minecraft.nbt.CompoundTag> species;
    private final long currentDay;
    private final long totalMutations;
    private final int speciationEvents;
    private final double environmentalPressure;

    private SpeciesList speciesList;
    private String selectedSpecies;
    private Genome selectedGenome;

    public EvolutionStatusScreen(EvolutionStatusPacket packet) {
        super(Component.literal("Évolution"));
        this.currentDay = packet.getCurrentDay();
        this.totalMutations = packet.getTotalMutations();
        this.speciationEvents = packet.getSpeciationEvents();
        this.environmentalPressure = packet.getEnvironmentalPressure();
        this.species = packet.getSpeciesGenomes();
    }

    @Override
    protected void init() {
        // (minecraft, width, bottom, top, itemHeight) — voir note du constructeur
        // de SpeciesList pour le détail de cette signature vérifiée.
        this.speciesList = new SpeciesList(this.minecraft, LIST_WIDTH, this.height - 35, 40, 22);
        this.speciesList.setX(PADDING);

        List<String> names = new ArrayList<>(species.keySet());
        names.sort(String::compareTo);
        for (String name : names) {
            this.speciesList.addEntry(name);
        }

        this.addRenderableWidget(this.speciesList);

        if (!names.isEmpty() && selectedSpecies == null) {
            selectSpecies(names.get(0));
        }
    }

    private void selectSpecies(String name) {
        this.selectedSpecies = name;
        net.minecraft.nbt.CompoundTag tag = species.get(name);
        this.selectedGenome = tag != null ? Genome.fromNBT(tag) : null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        // ── En-tête : statistiques globales ──────────────────────────
        graphics.drawCenteredString(this.font, "§6§l═══ ÉTAT DE L'ÉVOLUTION ═══",
            this.width / 2, 12, 0xFFFFFF);
        graphics.drawString(this.font,
            "§7Jour: §f" + currentDay + "   §7Mutations: §f" + totalMutations +
            "   §7Spéciations: §f" + speciationEvents +
            "   §7Pression: §f" + String.format("%.0f%%", environmentalPressure * 100),
            PADDING, 26, 0xFFFFFF, false);

        // ── Panneau de détail (à droite de la liste) ─────────────────
        int detailX = PADDING + LIST_WIDTH + 16;
        int detailWidth = this.width - detailX - PADDING;
        renderDetailPanel(graphics, detailX, 40, detailWidth);
    }

    private void renderDetailPanel(GuiGraphics graphics, int x, int y, int width) {
        if (selectedSpecies == null || selectedGenome == null) {
            graphics.drawString(this.font, "§7Sélectionnez une espèce dans la liste.", x, y, 0xAAAAAA, false);
            return;
        }

        // Cadre de fond simple
        graphics.fill(x - 4, y - 4, x + width, this.height - 35, 0x55000000);

        int cursorY = y + 4;
        graphics.drawString(this.font, "§e§l" + selectedSpecies.toUpperCase(), x, cursorY, 0xFFFFFF, false);
        cursorY += 14;
        graphics.drawString(this.font, "§7Lignée: §f" + selectedGenome.getSpeciesLineage(), x, cursorY, 0xFFFFFF, false);
        cursorY += 11;
        graphics.drawString(this.font, "§7Mutations accumulées: §f" + selectedGenome.getTotalMutations(), x, cursorY, 0xFFFFFF, false);
        cursorY += 11;
        graphics.drawString(this.font, "§7Fitness: §f" + String.format("%.3f", selectedGenome.getFitnessScore()), x, cursorY, 0xFFFFFF, false);
        cursorY += 16;

        graphics.drawString(this.font, "§6── Gènes ──", x, cursorY, 0xFFFFFF, false);
        cursorY += 12;

        for (Map.Entry<Gene.GeneType, Gene> entry : selectedGenome.getAllGenes().entrySet()) {
            Gene gene = entry.getValue();
            double[] bounds = Gene.getValueBounds(gene.getType());
            double percent = bounds[1] > bounds[0]
                ? (gene.getValue() - bounds[0]) / (bounds[1] - bounds[0])
                : 0.0;

            String barColor = gene.getGeneration() > 8 ? "§c" : gene.getGeneration() > 3 ? "§e" : "§a";
            String bar = buildBar(percent, 12);

            graphics.drawString(this.font,
                barColor + padRight(gene.getType().name(), 18) + " §7" + bar +
                " §f" + String.format("%.2f", gene.getValue()) + " §8(g" + gene.getGeneration() + ")",
                x, cursorY, 0xFFFFFF, false);
            cursorY += 10;

            if (cursorY > this.height - 45) break; // évite de déborder hors de l'écran
        }
    }

    private String buildBar(double percent, int segments) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, percent)) * segments);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        return bar.toString();
    }

    private String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Ne met pas le jeu en pause en solo — c'est un écran d'info, pas un menu.
    }

    // ──────────────────────────────────────────────────────
    //  Liste cliquable des espèces
    // ──────────────────────────────────────────────────────

    private class SpeciesList extends ObjectSelectionList<SpeciesList.Entry> {

        /**
         * NOTE 1.21.1 (confirmé via le code source réel de Forge,
         * net.minecraftforge.client.gui.widget.ModListWidget) : le
         * constructeur d'ObjectSelectionList prend (Minecraft, width, bottom,
         * top, itemHeight) — SEULEMENT 4 paramètres après minecraft, avec
         * bottom AVANT top dans l'ordre d'appel. Une signature à 5 paramètres
         * (width, height, top, bottom, itemHeight) que j'avais d'abord
         * supposée par analogie avec d'autres listes n'existe pas ici.
         */
        public SpeciesList(net.minecraft.client.Minecraft mc, int width, int bottom, int top, int itemHeight) {
            super(mc, width, bottom, top, itemHeight);
        }

        public void addEntry(String name) {
            this.addEntry(new Entry(name));
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String name;

            Entry(String name) {
                this.name = name;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean isSelected = name.equals(selectedSpecies);
                int color = isSelected ? 0xFFFFFF55 : 0xFFFFFFFF;
                graphics.drawString(EvolutionStatusScreen.this.font, formatName(name), left + 4, top + 6, color, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                selectSpecies(name);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(name);
            }

            private String formatName(String raw) {
                String display = raw.replace('_', ' ');
                return display.substring(0, 1).toUpperCase() + display.substring(1);
            }
        }
    }
}
