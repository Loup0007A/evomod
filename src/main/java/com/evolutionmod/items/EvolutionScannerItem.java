package com.evolutionmod.items;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.JournalDataMsg;
import com.evolutionmod.network.ScanResultMsg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Scanner d'évolution : clic droit sur une créature pour ouvrir son analyse
 * génétique détaillée. Tenu en main, il affiche aussi un résumé en surimpression
 * sur la créature visée.
 */
public class EvolutionScannerItem extends Item {

    public EvolutionScannerItem() {
        super(new Item.Properties().stacksTo(1).durability(256));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        scan((ServerPlayer) player, target);
        player.getItemInHand(hand).hurtAndBreak(1, player,
            hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    public static boolean scan(ServerPlayer player, LivingEntity target) {
        Genome genome = EvolutionCapability.genome(target);
        if (genome == null) {
            player.displayClientMessage(Component.translatable("evolutionmod.scanner.nothing")
                .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
            SoundSource.PLAYERS, 0.5f, 1.8f);
        EvoNetwork.sendToPlayer(player, new ScanResultMsg(target.getId(), genome.save(),
            EvolutionManager.speciesSummary(player.getServer(), genome)));
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown()) {
                scan(sp, sp); // auto-analyse
            } else {
                EvoNetwork.sendToPlayer(sp, new JournalDataMsg(EvolutionManager.buildJournal(sp)));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("evolutionmod.scanner.tip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("evolutionmod.scanner.tip2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("evolutionmod.scanner.tip3").withStyle(ChatFormatting.DARK_GRAY));
    }
}
