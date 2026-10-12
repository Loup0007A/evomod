package com.evolutionmod.menu;

import com.evolutionmod.blocks.GeneticLabBlockEntity;
import com.evolutionmod.items.GeneticCrystalItem;
import com.evolutionmod.items.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu (conteneur) du laboratoire génétique. */
public class GeneticLabMenu extends AbstractContainerMenu {

    public static final int IMAGE_WIDTH = 256;
    public static final int IMAGE_HEIGHT = 206;
    public static final int INV_X = 48;
    public static final int INV_Y = 124;

    public static final int SOURCE_X = 26, SOURCE_Y = 34;
    public static final int REAGENT_X = 26, REAGENT_Y = 78;
    public static final int ENERGY_X = 70, ENERGY_Y = 86;
    public static final int OUTPUT_X = 116, OUTPUT_Y = 56;

    private final Container container;
    private final ContainerData data;

    /** Constructeur client. */
    public GeneticLabMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(4), new SimpleContainerData(3));
    }

    public GeneticLabMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.GENETIC_LAB.get(), id);
        checkContainerSize(container, 4);
        checkContainerDataCount(data, 3);
        this.container = container;
        this.data = data;
        container.startOpen(inventory.player);

        addSlot(new Slot(container, GeneticLabBlockEntity.SLOT_SOURCE, SOURCE_X, SOURCE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GeneticCrystalItem.hasGenome(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, GeneticLabBlockEntity.SLOT_REAGENT, REAGENT_X, REAGENT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.GENETIC_CRYSTAL.get()) || stack.is(ModItems.MUTAGEN.get());
            }
        });
        addSlot(new Slot(container, GeneticLabBlockEntity.SLOT_ENERGY, ENERGY_X, ENERGY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GeneticLabBlockEntity.isEnergy(stack);
            }
        });
        addSlot(new Slot(container, GeneticLabBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, data.get(1));
    }

    public GeneticLabBlockEntity.Mode mode() {
        return GeneticLabBlockEntity.Mode.of(data.get(2));
    }

    public Container container() {
        return container;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int containerEnd = 4;
        int invEnd = slots.size();

        if (index < containerEnd) {
            if (!moveItemStackTo(stack, containerEnd, invEnd, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            if (GeneticCrystalItem.hasGenome(stack) && !slots.get(0).hasItem()) {
                moved = moveItemStackTo(stack, 0, 1, false);
            }
            if (!moved && (stack.is(ModItems.GENETIC_CRYSTAL.get()) || stack.is(ModItems.MUTAGEN.get()))) {
                moved = moveItemStackTo(stack, 1, 2, false);
            }
            if (!moved && GeneticLabBlockEntity.isEnergy(stack)) {
                moved = moveItemStackTo(stack, 2, 3, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
