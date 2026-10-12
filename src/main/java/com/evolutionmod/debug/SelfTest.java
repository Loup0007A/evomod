package com.evolutionmod.debug;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.blocks.GeneticLabBlockEntity;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.items.GeneticCrystalItem;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.SpeciesRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.evolutionmod.core.Phenotype;
import com.evolutionmod.items.EvolutionScannerItem;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.JournalDataMsg;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-test serveur, activé uniquement avec -Devolutionmod.selftest=true
 * (utilisé par l'intégration continue). Sans cette propriété, cette classe
 * ne fait strictement rien.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public final class SelfTest {

    private SelfTest() {}

    private static final boolean ENABLED = Boolean.getBoolean("evolutionmod.selftest");
    private static int countdown = -1;
    private static int failures;

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!ENABLED) return;
        EvolutionMod.LOGGER.info("[SELFTEST] serveur démarré, test dans 60 ticks");
        countdown = 60;
    }

    // ── Scénario visuel (client de test connecté) ──
    private static final boolean KEEP = Boolean.getBoolean("evolutionmod.selftest.keep");
    private static ServerPlayer tester;
    private static int scenario = -1;
    private static volatile int zombieId = -1;
    private static volatile BlockPos labPos;

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ENABLED || !KEEP || !(event.getEntity() instanceof ServerPlayer sp)) return;
        tester = sp;
        scenario = 0;
        sp.getServer().getPlayerList().op(sp.getGameProfile());
    }

    private static void scenarioTick(MinecraftServer server) {
        if (scenario < 0 || tester == null) return;
        scenario++;
        ServerPlayer sp = tester;
        try {
            switch (scenario) {
                case 40 -> setupScene(sp);
                case 280 -> EvoNetwork.sendToPlayer(sp, new JournalDataMsg(EvolutionManager.buildJournal(sp)));
                case 520 -> {
                    if (sp.level().getEntity(zombieId) instanceof LivingEntity z) EvolutionScannerItem.scan(sp, z);
                }
                case 640 -> {
                    if (labPos != null && sp.level().getBlockEntity(labPos) instanceof GeneticLabBlockEntity lab
                        && sp.level().getEntity(zombieId) instanceof LivingEntity zombie) {
                        fillLab(sp.serverLevel(), lab, zombie);
                        sp.openMenu(lab);
                    }
                }
                case 1400 -> server.halt(false);
                default -> { }
            }
        } catch (Throwable t) {
            EvolutionMod.LOGGER.error("[SELFTEST] scénario en échec", t);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent.Post event) {
        if (ENABLED) scenarioTick(event.getServer());
        if (!ENABLED || countdown < 0) return;
        if (--countdown != 0) return;
        MinecraftServer server = event.getServer();
        try {
            run(server);
        } catch (Throwable t) {
            failures++;
            EvolutionMod.LOGGER.error("[SELFTEST] EXCEPTION", t);
        }
        EvolutionMod.LOGGER.info(failures == 0 ? "[SELFTEST] RESULT: OK" : "[SELFTEST] RESULT: FAILED ({})", failures);
        if (!KEEP) server.halt(false);
    }

    private static void check(boolean ok, String what) {
        if (ok) {
            EvolutionMod.LOGGER.info("[SELFTEST] ok   - {}", what);
        } else {
            failures++;
            EvolutionMod.LOGGER.error("[SELFTEST] FAIL - {}", what);
        }
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos base = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn);

        // 1. Apparition : génome créé, valeurs proches du vanilla
        List<LivingEntity> mobs = new ArrayList<>();
        EntityType<?>[] types = {EntityType.ZOMBIE, EntityType.COW, EntityType.COW, EntityType.CREEPER,
            EntityType.SPIDER, EntityType.SKELETON, EntityType.VILLAGER, EntityType.ARMOR_STAND};
        for (int i = 0; i < types.length; i++) {
            var e = types[i].create(level);
            if (e == null) continue;
            e.moveTo(base.getX() + 2 + i * 2, base.getY(), base.getZ() + 2, 0, 0);
            if (e instanceof Mob mob) {
                mob.setPersistenceRequired();
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
            }
            level.addFreshEntity(e);
            if (e instanceof LivingEntity l) mobs.add(l);
        }
        for (LivingEntity l : mobs) {
            Genome g = EvolutionCapability.genome(l);
            String id = EntityType.getKey(l.getType()).getPath();
            boolean shouldEvolve = !(id.equals("villager") || id.equals("armor_stand"));
            check((g != null) == shouldEvolve, id + " a un génome : " + (g != null));
            if (g != null) {
                check(Math.abs(g.get(GeneType.SIZE) - 1.0) < 0.3, id + " taille de départ ≈ 1 : " + g.get(GeneType.SIZE));
                check(l.getMaxHealth() < 60, id + " PV max raisonnables : " + l.getMaxHealth());
                check(Math.abs(l.getHealth() - l.getMaxHealth()) < 0.01, id + " naît en pleine santé");
            }
        }

        // 2. Cycles d'évolution
        EvolutionSavedData data = EvolutionSavedData.get(server);
        long before = data.totalMutations();
        for (int i = 0; i < 40; i++) EvolutionManager.runCycle(server, EvolutionManager.currentDay(server));
        check(data.cycles() >= 40, "40 cycles exécutés (" + data.cycles() + ")");
        check(data.totalMutations() > before, "des mutations ont eu lieu (" + (data.totalMutations() - before) + ")");
        for (SpeciesRecord r : data.species().values()) {
            EvolutionMod.LOGGER.info("[SELFTEST]   espèce {} : divergence {} lignées {}", r.id(),
                String.format("%.3f", r.divergence()), r.lineages());
        }
        check(data.species().containsKey("plant:oak"), "les plantes sont suivies");

        // 3. Expression : un zombie muté reçoit bien des modificateurs
        LivingEntity zombie = mobs.get(0);
        Genome zg = EvolutionCapability.genome(zombie);
        if (zg != null) {
            zg.set(GeneType.SIZE, 2.0);
            zg.set(GeneType.VITALITY, 2.0);
            com.evolutionmod.core.Phenotype.apply(zombie, zg, true);
            check(Math.abs(zombie.getAttributeValue(Attributes.SCALE) - 2.0) < 0.01,
                "taille 2.0 appliquée (" + zombie.getAttributeValue(Attributes.SCALE) + ")");
            check(Math.abs(zombie.getMaxHealth() - 40) < 0.5, "vitalité ×2 → 40 PV (" + zombie.getMaxHealth() + ")");
            zg.set(GeneType.SIZE, 1.0);
            zg.set(GeneType.VITALITY, 1.0);
            com.evolutionmod.core.Phenotype.apply(zombie, zg, false);
            check(Math.abs(zombie.getMaxHealth() - 20) < 0.5, "retour au neutre → 20 PV (" + zombie.getMaxHealth() + ")");
        }

        // 4. Reproduction : croisement réel
        LivingEntity cowA = mobs.get(1), cowB = mobs.get(2);
        Cow calf = EntityType.COW.create(level);
        if (calf != null) {
            calf.setBaby(true);
            calf.moveTo(base.getX(), base.getY(), base.getZ() + 4, 0, 0);
            EvolutionManager.onBreed(cowA, cowB, calf);
            level.addFreshEntity(calf);
            Genome cg = EvolutionCapability.genome(calf);
            check(cg != null && cg.generation() >= 1, "le veau hérite d'un génome croisé (gén. "
                + (cg == null ? "?" : cg.generation()) + ")");
        }

        // 5. Sélection : tuer par un joueur éloigne le gabarit
        SpeciesRecord zr = data.get("minecraft:zombie");
        if (zr != null && zg != null) {
            zg.set(GeneType.STRENGTH, 3.0);
            double beforeStr = zr.template().get(GeneType.STRENGTH);
            EvolutionManager.selectFor(zombie, 0.5);
            check(zr.template().get(GeneType.STRENGTH) > beforeStr, "sélection positive vers la force");
        }

        // 6. Laboratoire : clonage et fusion
        BlockPos labPos = base.above(3);
        level.setBlock(labPos, ModBlocks.GENETIC_LAB.get().defaultBlockState(), 3);
        if (level.getBlockEntity(labPos) instanceof GeneticLabBlockEntity lab && zg != null) {
            ItemStack a = GeneticCrystalItem.withGenome(new ItemStack(ModItems.GENETIC_CRYSTAL.get()), zg);
            lab.container().setItem(0, a);
            lab.container().setItem(1, new ItemStack(ModItems.GENETIC_CRYSTAL.get()));
            lab.container().setItem(2, new ItemStack(Items.REDSTONE, 4));
            var state = level.getBlockState(labPos);
            for (int i = 0; i < GeneticLabBlockEntity.PROCESS_TIME + 2; i++) {
                GeneticLabBlockEntity.serverTick(level, labPos, level.getBlockState(labPos), lab);
            }
            ItemStack out = lab.container().getItem(3);
            check(GeneticCrystalItem.hasGenome(out), "clonage : cristal produit");
            check(lab.container().getItem(2).getCount() == 3, "clonage : 1 redstone consommée");
            check(state.getBlock() == ModBlocks.GENETIC_LAB.get(), "bloc laboratoire présent");
        }

        // 7. Sauvegarde / rechargement du génome
        if (zg != null) {
            Genome copy = Genome.load(zg.save());
            check(copy.genes().equals(zg.genes()) && copy.totalMutations() == zg.totalMutations(),
                "sérialisation du génome identique");
        }
        var tag = data.save(new net.minecraft.nbt.CompoundTag(), server.registryAccess());
        var reloaded = EvolutionSavedData.load(tag, server.registryAccess());
        check(reloaded.species().size() == data.species().size(), "sauvegarde du monde rechargée ("
            + reloaded.species().size() + " espèces)");

        // 9. Adaptation globale (biome, chasse...)
        GeneType adapted = com.evolutionmod.core.Adaptation.trigger(level, base, "minecraft:zombie",
            com.evolutionmod.core.Adaptation.Cause.HUNTED, zombie);
        check(adapted != null, "adaptation du zombie : " + adapted);
        GeneType oakGene = com.evolutionmod.core.Adaptation.trigger(level, base, "plant:oak",
            com.evolutionmod.core.Adaptation.Cause.PLANT_BROKEN, null);
        check(oakGene != null, "adaptation du chêne : " + oakGene);

        // 10. Arbres visibles
        BlockPos treePos = base.offset(-6, 0, -6);
        level.setBlock(treePos, net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(), 3);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            level.setBlock(treePos.offset(dx, 1, dz), net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
        }
        int mutated = com.evolutionmod.core.TreeMutator.mutateNear(level, treePos, "oak",
            data.get("plant:oak").template(), 3, 1.0);
        check(mutated > 0, "feuillages de chêne mutés : " + mutated);

        // 11. Chaudron : mixture puis explosion
        BlockPos cauldron = base.offset(6, 0, -6);
        level.setBlock(cauldron.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(cauldron, net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3), 3);
        var r1 = com.evolutionmod.brew.CauldronBrewing.addIngredient(level, cauldron, "minecraft:carrot", null);
        com.evolutionmod.brew.CauldronBrewing.addIngredient(level, cauldron, "minecraft:cod", null);
        com.evolutionmod.brew.CauldronBrewing.addIngredient(level, cauldron, "minecraft:beef", null);
        var brew = com.evolutionmod.brew.BrewSavedData.get(level).get(cauldron);
        check(r1 == com.evolutionmod.brew.CauldronBrewing.AddResult.ADDED && brew != null && brew.items.size() == 3,
            "3 ingrédients dans le chaudron");
        if (brew != null) {
            long wait = brew.readyAt - level.getGameTime();
            check(wait >= 300 && wait <= 2400, "temps de cuisson entre 15 s et 2 min : " + wait / 20 + " s");
            var outcome = com.evolutionmod.brew.BrewRecipe.compute(brew.items, brew.seed);
            check(!outcome.deltas().isEmpty(), "la mixture a des effets génétiques : " + outcome.deltas());
            ItemStack potion = com.evolutionmod.items.EvolutionPotionItem.create(brew.items, brew.seed);
            check(potion.is(ModItems.EVOLUTION_POTION.get()), "potion créée");
            var cow = mobs.get(1);
            var summary = com.evolutionmod.items.EvolutionPotionItem.apply(cow, outcome);
            EvolutionMod.LOGGER.info("[SELFTEST]   potion sur une vache : {} (toxicité {})", summary,
                String.format("%.1f", outcome.toxicity()));
        }
        var last = com.evolutionmod.brew.CauldronBrewing.AddResult.ADDED;
        for (int i = 0; i < 6 && last == com.evolutionmod.brew.CauldronBrewing.AddResult.ADDED; i++) {
            last = com.evolutionmod.brew.CauldronBrewing.addIngredient(level, cauldron, "minecraft:apple", null);
        }
        check(last == com.evolutionmod.brew.CauldronBrewing.AddResult.EXPLODED, "le chaudron surchargé explose");
        var after = level.getBlockState(cauldron);
        check(after.is(net.minecraft.world.level.block.Blocks.LAVA) || after.is(ModBlocks.SULFUR.get()),
            "lave ou soufre à la place du chaudron : " + after);

        // 12. Catalyseur
        check(com.evolutionmod.items.EvolutionCatalystItem.mutate(mobs.get(4)), "catalyseur sur l'araignée");

        // 8. Journal
        check(!EvolutionManager.speciesComponent("minecraft:zombie").getString().isEmpty(), "nom d'espèce");
        server.saveEverything(true, true, true);
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
