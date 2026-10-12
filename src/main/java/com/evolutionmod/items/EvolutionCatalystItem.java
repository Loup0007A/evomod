package com.evolutionmod.items;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.core.Phenotype;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.NotifyMsg;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.HistoryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalyseur évolutif : force une évolution.
 * <ul>
 *   <li>Clic droit sur une créature : 2 à 4 mutations immédiates.</li>
 *   <li>Clic droit dans le vide : vous mutez vous-même.</li>
 *   <li>Maj + clic droit dans le vide : un cycle d'évolution complet pour tout le monde.</li>
 * </ul>
 */
public class EvolutionCatalystItem extends Item {

    public EvolutionCatalystItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!mutate(target)) {
            player.displayClientMessage(Component.translatable("evolutionmod.crystal.no_genome")
                .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        player.displayClientMessage(Component.translatable("evolutionmod.catalyst.mob", target.getDisplayName())
            .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        consume(player, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (player.isShiftKeyDown()) {
            var server = sp.getServer();
            EvolutionManager.runCycle(server, EvolutionManager.currentDay(server));
            EvolutionSavedData.get(server).addHistory(new HistoryEntry(EvolutionManager.currentDay(server),
                HistoryEntry.Type.FORCED, "minecraft:player", sp.getGameProfile().getName(), 1));
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
            player.getCooldowns().addCooldown(this, 100);
        } else if (!mutate(sp)) {
            player.displayClientMessage(Component.translatable("evolutionmod.journal.self.disabled")
                .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        consume(player, hand);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static void consume(Player player, InteractionHand hand) {
        if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
    }

    /** Force 2 à 4 mutations sur une créature. */
    public static boolean mutate(LivingEntity target) {
        Genome g = EvolutionCapability.genome(target);
        if (g == null) return false;
        int n = 2 + target.getRandom().nextInt(3);
        List<GeneType> changed = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            GeneType t = g.mutateRandomGene(target.getRandom(), 0.15);
            if (t != null) changed.add(t);
        }
        Phenotype.apply(target, g, false);
        EvolutionManager.sync(target);
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + target.getBbHeight() / 2,
                target.getZ(), 40, target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, 0.3);
            level.playSound(null, target.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.NEUTRAL, 0.8f, 1.3f);
        }
        if (target instanceof ServerPlayer sp && !changed.isEmpty()) {
            EvoNetwork.sendToPlayer(sp, new NotifyMsg(NotifyMsg.Kind.PLAYER, "evolutionmod.notify.player_mutation",
                List.of("#gene:" + changed.get(0).name())));
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("evolutionmod.catalyst.tip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("evolutionmod.catalyst.tip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("evolutionmod.catalyst.tip3").withStyle(ChatFormatting.GOLD));
    }
}
