package com.evolutionmod.items;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.core.Phenotype;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Viande d'un animal muté. La manger donne des effets liés à ses gènes et
 * transfère une infime partie de son génome au joueur (« on est ce qu'on mange »).
 */
public class MutantMeatItem extends Item {

    public MutantMeatItem() {
        super(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(7).saturationModifier(0.7f).build()));
    }

    public static ItemStack fromGenome(Genome genome) {
        ItemStack stack = new ItemStack(ModItems.MUTANT_MEAT.get());
        CompoundTag root = new CompoundTag();
        root.put("Source", genome.save());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    @Nullable
    private static Genome source(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains("Source")) return null;
        CompoundTag tag = data.copyTag().getCompound("Source");
        return Genome.isValid(tag) ? Genome.load(tag) : null;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            Genome g = source(stack);
            if (g != null) {
                applyEffects(g, user);
                transfer(g, user);
            }
        }
        return super.finishUsingItem(stack, level, user);
    }

    private static void applyEffects(Genome g, LivingEntity user) {
        if (g.get(GeneType.VENOM) > 0.2) {
            user.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (60 + g.get(GeneType.VENOM) * 200), 0));
        }
        if (g.get(GeneType.GLOW) > 0.3) user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0));
        if (g.get(GeneType.STRENGTH) > 1.3) user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 0));
        if (g.get(GeneType.SPEED) > 1.2) user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 0));
        if (g.get(GeneType.REGENERATION) > 0.3) user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        if (g.get(GeneType.FIRE_RESISTANCE) > 0.5) user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 900, 0));
        if (g.get(GeneType.ARMOR) > 3) user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
        if (g.get(GeneType.AGILITY) > 1.25) user.addEffect(new MobEffectInstance(MobEffects.JUMP, 900, 0));
    }

    /** Transfert horizontal : le génome du mangeur se rapproche très légèrement de la proie. */
    private static void transfer(Genome food, LivingEntity user) {
        Genome mine = EvolutionCapability.genome(user);
        if (mine == null) return;
        mine.pullTowards(food, 0.03);
        Phenotype.apply(user, mine, false);
        EvolutionManager.sync(user);
        if (user instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("evolutionmod.meat.transfer")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        Genome g = source(stack);
        if (g == null) return super.getName(stack);
        return Component.translatable("item.evolutionmod.mutant_meat.of", EvolutionManager.speciesComponent(g.species()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        Genome g = source(stack);
        if (g == null) return;
        GeneType trait = g.dominantTrait();
        if (trait != null) {
            tooltip.add(Component.translatable("evolutionmod.tip.dominant",
                Component.translatable(trait.translationKey())).withStyle(ChatFormatting.GOLD));
        }
        if (g.get(GeneType.VENOM) > 0.2) {
            tooltip.add(Component.translatable("evolutionmod.meat.venom").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("evolutionmod.meat.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
