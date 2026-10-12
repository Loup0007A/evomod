package com.evolutionmod.debug;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.blocks.GeneticLabBlockEntity;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.client.HudOverlay;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.core.Phenotype;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.items.EvolutionScannerItem;
import com.evolutionmod.items.GeneticCrystalItem;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.network.NotifyMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Test visuel client (CI uniquement, -Devolutionmod.clienttest=true) : rejoint
 * un monde, met en scène des créatures mutées et capture chaque interface.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSelfTest {

    private ClientSelfTest() {}

    private static final boolean ENABLED = Boolean.getBoolean("evolutionmod.clienttest");
    private static int step;
    private static int wait = 200;
    private static volatile int zombieId = -1;
    private static volatile BlockPos labPos;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (--wait > 0) return;
        try {
            runStep(mc);
        } catch (Throwable t) {
            EvolutionMod.LOGGER.error("[CLIENTTEST] étape {} en échec", step, t);
            step = 99;
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
            c -> EvolutionMod.LOGGER.info("[CLIENTTEST] capture {} : {}", name, c.getString()));
    }

    private static ServerPlayer serverPlayer(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    private static void runStep(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        EvolutionMod.LOGGER.info("[CLIENTTEST] étape {}", step);
        switch (step) {
            case 0 -> {
                server.execute(() -> setupScene(serverPlayer(mc)));
                HudOverlay.push(new NotifyMsg(NotifyMsg.Kind.SPECIATION, "evolutionmod.notify.speciation",
                    List.of("#species:minecraft:zombie", "Neozombimorphus")));
                wait = 80;
            }
            case 1 -> {
                HudOverlay.push(new NotifyMsg(NotifyMsg.Kind.DAILY, "evolutionmod.notify.daily",
                    List.of("42", "318", "64")));
                wait = 10;
            }
            case 2 -> {
                shot(mc, "01_hud_scanner");
                server.execute(() -> com.evolutionmod.network.EvoNetwork.sendToPlayer(serverPlayer(mc),
                    new com.evolutionmod.network.JournalDataMsg(EvolutionManager.buildJournal(serverPlayer(mc)))));
                wait = 40;
            }
            case 3 -> {
                shot(mc, "02_journal_world");
                pressTab(mc);
                wait = 20;
            }
            case 4 -> {
                shot(mc, "03_journal_species");
                pressTab(mc);
                wait = 20;
            }
            case 5 -> {
                shot(mc, "04_journal_history");
                pressTab(mc);
                wait = 20;
            }
            case 6 -> {
                shot(mc, "05_journal_self");
                mc.setScreen(null);
                server.execute(() -> {
                    ServerPlayer sp = serverPlayer(mc);
                    if (sp.level().getEntity(zombieId) instanceof LivingEntity z) EvolutionScannerItem.scan(sp, z);
                });
                wait = 40;
            }
            case 7 -> {
                shot(mc, "06_scan");
                mc.setScreen(null);
                server.execute(() -> {
                    ServerPlayer sp = serverPlayer(mc);
                    if (labPos != null && sp.level().getBlockEntity(labPos) instanceof GeneticLabBlockEntity lab
                        && sp.level().getEntity(zombieId) instanceof LivingEntity zombie) {
                        fillLab(sp.serverLevel(), lab, zombie);
                        sp.openMenu(lab);
                    }
                });
                wait = 50;
            }
            case 8 -> {
                shot(mc, "07_lab");
                mc.setScreen(null);
                wait = 40;
            }
            default -> {
                EvolutionMod.LOGGER.info("[CLIENTTEST] terminé");
                mc.stop();
                return;
            }
        }
        step++;
    }

    private static void pressTab(Minecraft mc) {
        if (mc.screen != null) mc.screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
    }

    /** Côté serveur intégré : décor, créatures mutées, laboratoire garni. */
    private static void setupScene(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        level.setDayTime(6000);
        sp.setInvulnerable(true);
        sp.getInventory().clearContent();
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.EVOLUTION_SCANNER.get()));

        for (int i = 0; i < 25; i++) EvolutionManager.runCycle(level.getServer(), EvolutionManager.currentDay(level.getServer()));

        Vec3 look = Vec3.directionFromRotation(0, sp.getYRot());
        Vec3 front = sp.position().add(look.scale(4));

        Mob zombie = EntityType.ZOMBIE.create(level);
        if (zombie != null) {
            zombie.moveTo(front.x, sp.getY(), front.z, sp.getYRot() + 180, 0);
            zombie.setNoAi(true);
            zombie.setPersistenceRequired();
            level.addFreshEntity(zombie);
            Genome g = EvolutionCapability.genome(zombie);
            if (g != null) {
                g.set(GeneType.PIGMENT, 0.8);
                g.set(GeneType.HUE, 280);
                g.set(GeneType.GLOW, 0.8);
                g.set(GeneType.SIZE, 1.35);
                g.set(GeneType.VENOM, 0.5);
                g.set(GeneType.STRENGTH, 1.8);
                g.mutate(level.random, 1.0, 0.05);
                Phenotype.apply(zombie, g, true);
                EvolutionManager.sync(zombie);
            }
            zombieId = zombie.getId();
        }
        Vec3 side = Vec3.directionFromRotation(0, sp.getYRot() + 50).scale(5);
        for (int i = 0; i < 2; i++) {
            Mob cow = EntityType.COW.create(level);
            if (cow == null) continue;
            cow.moveTo(sp.getX() + side.x + i * 2, sp.getY(), sp.getZ() + side.z, 0, 0);
            cow.setNoAi(true);
            level.addFreshEntity(cow);
            Genome g = EvolutionCapability.genome(cow);
            if (g != null) {
                g.set(GeneType.PIGMENT, 0.6);
                g.set(GeneType.HUE, 140 + i * 60);
                g.set(GeneType.SIZE, 0.7 + i * 0.6);
                Phenotype.apply(cow, g, true);
                EvolutionManager.sync(cow);
            }
        }

        // Laboratoire derrière le joueur, en pleine fusion
        labPos = sp.blockPosition().offset(-(int) Math.round(look.x * 3), 0, -(int) Math.round(look.z * 3));
        level.setBlock(labPos, ModBlocks.GENETIC_LAB.get().defaultBlockState(), 3);
        sp.getInventory().add(GeneticCrystalItem.withGenome(new ItemStack(ModItems.GENETIC_CRYSTAL.get()),
            zombie != null && EvolutionCapability.genome(zombie) != null ? EvolutionCapability.genome(zombie) : Genome.fromProfile(
                com.evolutionmod.genetics.SpeciesProfile.forPlant("oak"))));
        sp.getInventory().add(new ItemStack(ModItems.MUTAGEN.get(), 8));
        sp.getInventory().add(new ItemStack(ModItems.EVOLUTION_JOURNAL.get()));
        // Regarder le zombie
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), 8f);
    }

    private static void fillLab(ServerLevel level, GeneticLabBlockEntity lab, LivingEntity zombie) {
        {
            Genome zg = EvolutionCapability.genome(zombie);
            if (zg != null) {
                Genome other = zg.copy();
                other.mutate(level.random, 1.0, 0.15);
                lab.container().setItem(0, GeneticCrystalItem.withGenome(new ItemStack(ModItems.GENETIC_CRYSTAL.get()), zg));
                lab.container().setItem(1, GeneticCrystalItem.withGenome(new ItemStack(ModItems.GENETIC_CRYSTAL.get()), other));
                lab.container().setItem(2, new ItemStack(Items.REDSTONE, 16));
            }
        }
    }
}
