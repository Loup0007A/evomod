package com.evolutionmod.menu;

import com.evolutionmod.EvolutionMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, EvolutionMod.MOD_ID);

    public static final RegistryObject<MenuType<GeneticLabMenu>> GENETIC_LAB = MENUS.register("genetic_lab",
        () -> IForgeMenuType.create((id, inventory, buf) -> new GeneticLabMenu(id, inventory)));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
