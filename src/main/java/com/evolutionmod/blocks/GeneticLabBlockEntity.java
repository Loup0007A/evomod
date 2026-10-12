package com.evolutionmod.blocks;

import com.evolutionmod.genetics.Genome;
import com.evolutionmod.items.GeneticCrystalItem;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.menu.GeneticLabMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Logique du laboratoire génétique.
 *
 * Emplacements : 0 = cristal source (A), 1 = réactif (cristal B, cristal vide
 * ou mutagène), 2 = énergie (poudre de redstone / glowstone), 3 = sortie.
 */
public class GeneticLabBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_REAGENT = 1;
    public static final int SLOT_ENERGY = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int PROCESS_TIME = 120;

    public enum Mode {
        NONE, FUSION, CLONE, MUTATION;

        public static Mode of(int i) {
            return i >= 0 && i < values().length ? values()[i] : NONE;
        }
    }

    private final SimpleContainer items = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            GeneticLabBlockEntity.this.setChanged();
        }
    };

    private int progress;
    private int mode;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESS_TIME;
                case 2 -> mode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 2) mode = value;
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public GeneticLabBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GENETIC_LAB_BE.get(), pos, state);
    }

    public SimpleContainer container() {
        return items;
    }

    // ───────────────────────── Logique ─────────────────────────

    public static Mode detectMode(ItemStack source, ItemStack reagent) {
        if (!GeneticCrystalItem.hasGenome(source)) return Mode.NONE;
        if (reagent.is(ModItems.MUTAGEN.get())) return Mode.MUTATION;
        if (reagent.is(ModItems.GENETIC_CRYSTAL.get())) {
            return GeneticCrystalItem.hasGenome(reagent) ? Mode.FUSION : Mode.CLONE;
        }
        return Mode.NONE;
    }

    public static boolean isEnergy(ItemStack stack) {
        return stack.is(Items.REDSTONE) || stack.is(Items.GLOWSTONE_DUST);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GeneticLabBlockEntity lab) {
        Mode m = detectMode(lab.items.getItem(SLOT_SOURCE), lab.items.getItem(SLOT_REAGENT));
        boolean canRun = m != Mode.NONE && isEnergy(lab.items.getItem(SLOT_ENERGY))
            && lab.items.getItem(SLOT_OUTPUT).isEmpty();
        lab.mode = m.ordinal();

        boolean wasLit = state.getValue(GeneticLabBlock.LIT);
        if (canRun) {
            lab.progress++;
            if (lab.progress >= PROCESS_TIME) {
                lab.progress = 0;
                lab.craft(level, m);
            }
        } else if (lab.progress > 0) {
            lab.progress = Math.max(0, lab.progress - 2);
        }
        if (wasLit != canRun) {
            level.setBlock(pos, state.setValue(GeneticLabBlock.LIT, canRun), 3);
        }
        if (canRun) lab.setChanged();
    }

    private void craft(Level level, Mode m) {
        RandomSource random = level.getRandom();
        ItemStack source = items.getItem(SLOT_SOURCE);
        ItemStack reagent = items.getItem(SLOT_REAGENT);
        Genome a = GeneticCrystalItem.getGenome(source);
        if (a == null) return;

        Genome result;
        switch (m) {
            case FUSION -> {
                Genome b = GeneticCrystalItem.getGenome(reagent);
                if (b == null || !a.species().equals(b.species())) {
                    // Espèces incompatibles : la fusion échoue et le réactif est perdu
                    reagent.shrink(1);
                    items.getItem(SLOT_ENERGY).shrink(1);
                    level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.8f);
                    items.setChanged();
                    return;
                }
                result = Genome.crossover(a, b, random);
            }
            case CLONE -> result = a.copy();
            case MUTATION -> {
                result = a.copy();
                int n = 2 + random.nextInt(3);
                for (int i = 0; i < n; i++) result.mutateRandomGene(random, 0.18);
            }
            default -> {
                return;
            }
        }

        ItemStack out = GeneticCrystalItem.withGenome(new ItemStack(ModItems.GENETIC_CRYSTAL.get()), result);
        if (m == Mode.MUTATION) {
            source.shrink(1);   // le cristal source est transformé
        }
        reagent.shrink(1);
        items.getItem(SLOT_ENERGY).shrink(1);
        items.setItem(SLOT_OUTPUT, out);
        items.setChanged();
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.2f);
    }

    // ───────────────────────── Menu ─────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.evolutionmod.genetic_lab");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneticLabMenu(id, inventory, items, data);
    }

    // ───────────────────────── Sauvegarde ─────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(items.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) list.set(i, items.getItem(i));
        ContainerHelper.saveAllItems(tag, list, registries);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(items.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, registries);
        for (int i = 0; i < list.size(); i++) items.setItem(i, list.get(i));
        progress = tag.getInt("Progress");
    }
}
