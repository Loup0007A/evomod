package com.evolutionmod.brew;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.blocks.ModBlocks;
import com.evolutionmod.config.EvoConfig;
import com.evolutionmod.items.EvolutionPotionItem;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.NotifyMsg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Mixtures génétiques au chaudron.
 *
 * <ul>
 *   <li>Clic droit sur un chaudron rempli d'eau avec une plante, de la viande ou du
 *       poisson : l'ingrédient tombe dans l'eau.</li>
 *   <li>La mixture cuit entre 15 secondes et 2 minutes (au hasard, relancé à chaque ajout).</li>
 *   <li>Fiole vide sur une mixture prête : on obtient une « Mixture génétique ».</li>
 *   <li>Trop d'ingrédients (seuil caché entre 5 et 7) : le chaudron explose,
 *       laissant de la lave et du soufre.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public final class CauldronBrewing {

    private CauldronBrewing() {}

    private static final RandomSource RANDOM = RandomSource.create();
    public static final int MIN_TIME = 15 * 20;
    public static final int MAX_TIME = 120 * 20;

    public enum AddResult { ADDED, EXPLODED, NOT_CAULDRON }

    private static boolean isWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON);
    }

    // ───────────────────────── Interaction ─────────────────────────

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!isWaterCauldron(level.getBlockState(pos))) return;
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();

        if (BrewRecipe.isIngredient(held)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            if (level instanceof ServerLevel sl && EvoConfig.cauldronBrewing()) {
                String id = BrewRecipe.idOf(held.getItem());
                if (!player.getAbilities().instabuild) held.shrink(1);
                addIngredient(sl, pos, id, player);
            }
            return;
        }

        if (!(level instanceof ServerLevel sl)) return;
        BrewSavedData data = BrewSavedData.get(sl);
        BrewSavedData.Brew brew = data.get(pos);
        if (brew == null) return;

        if (held.is(Items.GLASS_BOTTLE)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            long left = brew.readyAt - sl.getGameTime();
            if (left > 0) {
                player.displayClientMessage(Component.translatable("evolutionmod.brew.not_ready", (left + 19) / 20)
                    .withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            ItemStack potion = EvolutionPotionItem.create(brew.items, brew.seed);
            if (!player.getAbilities().instabuild) held.shrink(1);
            if (!player.getInventory().add(potion)) player.drop(potion, false);
            sl.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 0.8f);
            LayeredCauldronBlock.lowerFillLevel(sl.getBlockState(pos), sl, pos);
            if (!isWaterCauldron(sl.getBlockState(pos))) data.remove(pos);
            return;
        }

        if (held.isEmpty()) {
            long left = Math.max(0, brew.readyAt - sl.getGameTime());
            Component status = left > 0
                ? Component.translatable("evolutionmod.brew.status_cooking", brew.items.size(), (left + 19) / 20)
                : Component.translatable("evolutionmod.brew.status_ready", brew.items.size());
            player.displayClientMessage(status.copy().withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** Ajoute un ingrédient dans le chaudron. Peut le faire exploser. */
    public static AddResult addIngredient(ServerLevel level, BlockPos pos, String itemId, Player player) {
        if (!isWaterCauldron(level.getBlockState(pos))) return AddResult.NOT_CAULDRON;
        BrewSavedData data = BrewSavedData.get(level);
        BrewSavedData.Brew brew = data.get(pos);
        if (brew == null) {
            brew = new BrewSavedData.Brew();
            brew.capacity = 5 + RANDOM.nextInt(3); // 5, 6 ou 7 : seuil secret
            brew.seed = RANDOM.nextLong();
        }
        brew.items.add(itemId);
        brew.readyAt = level.getGameTime() + MIN_TIME + RANDOM.nextInt(MAX_TIME - MIN_TIME + 1);
        brew.announced = false;
        data.put(pos, brew);

        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.7f, 1.2f);
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            12, 0.25, 0.05, 0.25, 0.1);

        if (brew.items.size() > brew.capacity) {
            explode(level, pos);
            return AddResult.EXPLODED;
        }
        if (player != null) {
            Component msg = brew.items.size() >= 5
                ? Component.translatable("evolutionmod.brew.unstable", brew.items.size()).withStyle(ChatFormatting.RED)
                : Component.translatable("evolutionmod.brew.added", brew.items.size()).withStyle(ChatFormatting.LIGHT_PURPLE);
            player.displayClientMessage(msg, true);
        }
        if (brew.items.size() >= 5) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 0.6f);
        }
        return AddResult.ADDED;
    }

    /** Le chaudron surchargé explose : lave au centre, soufre tout autour. */
    public static void explode(ServerLevel level, BlockPos pos) {
        BrewSavedData.get(level).remove(pos);
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.2f, false,
            Level.ExplosionInteraction.BLOCK);

        // Lave à la place du chaudron (ou du soufre si la lave ne tient pas)
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, RANDOM.nextFloat() < 0.65f ? Blocks.LAVA.defaultBlockState()
                : ModBlocks.SULFUR.get().defaultBlockState(), 3);
        }
        // Dépôts de soufre projetés autour
        int deposits = 3 + RANDOM.nextInt(5);
        for (int i = 0; i < deposits * 4 && deposits > 0; i++) {
            BlockPos p = pos.offset(RANDOM.nextInt(7) - 3, RANDOM.nextInt(3) - 1, RANDOM.nextInt(7) - 3);
            if (level.getBlockState(p).canBeReplaced() && !level.getBlockState(p).liquid()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                level.setBlock(p, ModBlocks.SULFUR.get().defaultBlockState(), 3);
                deposits--;
            }
        }
        // Vapeurs soufrées
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
            40, 1.5, 0.8, 1.5, 0.02);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(5))) {
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            if (e instanceof ServerPlayer sp) {
                EvoNetwork.sendToPlayer(sp, new NotifyMsg(NotifyMsg.Kind.DANGER, "evolutionmod.brew.exploded", List.of()));
            }
        }
    }

    // ───────────────────────── Cuisson ─────────────────────────

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % 10 != 0) return;
        BrewSavedData data = BrewSavedData.get(level);
        if (data.all().isEmpty()) return;
        Iterator<Map.Entry<Long, BrewSavedData.Brew>> it = data.all().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, BrewSavedData.Brew> e = it.next();
            BlockPos pos = BlockPos.of(e.getKey());
            if (!level.isLoaded(pos)) continue;
            if (!isWaterCauldron(level.getBlockState(pos))) {
                it.remove();
                data.setDirty();
                continue;
            }
            BrewSavedData.Brew brew = e.getValue();
            int color = BrewRecipe.compute(brew.items, brew.seed).color();
            double x = pos.getX() + 0.5, y = pos.getY() + 0.95, z = pos.getZ() + 0.5;
            if (level.getGameTime() < brew.readyAt) {
                level.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 3, 0.2, 0.02, 0.2, 0.0);
                if (brew.items.size() >= 5) {
                    level.sendParticles(ParticleTypes.SMOKE, x, y + 0.1, z, 2, 0.2, 0.05, 0.2, 0.01);
                }
            } else {
                level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | color),
                    x, y, z, 4, 0.2, 0.1, 0.2, 1.0);
                if (!brew.announced) {
                    brew.announced = true;
                    data.setDirty();
                    level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1f, 0.9f);
                    for (ServerPlayer p : level.players()) {
                        if (p.blockPosition().closerThan(pos, 24)) {
                            EvoNetwork.sendToPlayer(p, new NotifyMsg(NotifyMsg.Kind.BREW,
                                "evolutionmod.brew.ready", List.of(String.valueOf(brew.items.size()))));
                        }
                    }
                }
            }
        }
    }
}
