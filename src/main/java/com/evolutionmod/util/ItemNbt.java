package com.evolutionmod.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * Utilitaire de compatibilité 1.21.1 pour le stockage NBT dans les ItemStack.
 *
 * Depuis 1.20.5, ItemStack#getOrCreateTag(), #getTag() et #hasTag() ont été
 * supprimés. Les données arbitraires sont désormais stockées via le data
 * component {@link DataComponents#CUSTOM_DATA} (type {@link CustomData}),
 * qui encapsule un CompoundTag classique.
 *
 * Cette classe centralise le pont entre l'ancienne API (familière, simple)
 * et la nouvelle, pour éviter de répéter ce boilerplate dans chaque item.
 */
public final class ItemNbt {

    private ItemNbt() {}

    /** Lit le CompoundTag personnalisé, ou un tag vide s'il n'existe pas. */
    public static CompoundTag get(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    /** True si le stack possède un tag personnalisé non vide. */
    public static boolean has(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

    /** True si le tag personnalisé contient la clé donnée. */
    public static boolean hasKey(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(key);
    }

    /** Écrit le CompoundTag fourni comme données personnalisées du stack. */
    public static void set(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /**
     * Lit le tag actuel, le modifie via le callback fourni, puis le réécrit.
     * Pattern recommandé pour toute mise à jour incrémentale (équivalent de
     * l'ancien {@code stack.getOrCreateTag().putXxx(...)} en une étape).
     */
    public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
        CompoundTag tag = get(stack);
        updater.accept(tag);
        set(stack, tag);
    }

    /** Supprime une clé précise du tag personnalisé (laisse le reste intact). */
    public static void removeKey(ItemStack stack, String key) {
        update(stack, tag -> tag.remove(key));
    }
}
