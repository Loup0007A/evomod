package com.evolutionmod.items;

import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Fruit d'une plante mutée. Sa puissance (gène POTENCY) détermine ses effets,
 * son rendement (YIELD) sa valeur nutritive.
 */
public class MutantFruitItem extends Item {

    public MutantFruitItem() {
        super(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(4).saturationModifier(0.6f).alwaysEdible().build()));
    }

    public static ItemStack fromPlant(Genome plant, String plantName) {
        ItemStack stack = new ItemStack(ModItems.MUTANT_FRUIT.get());
        CompoundTag root = new CompoundTag();
        root.putString("Plant", plantName);
        root.putDouble("Potency", plant.get(GeneType.POTENCY));
        root.putDouble("Yield", plant.get(GeneType.YIELD));
        root.putDouble("Glow", plant.get(GeneType.GLOW));
        root.putDouble("Hue", plant.get(GeneType.HUE));
        root.putDouble("Pigment", plant.get(GeneType.PIGMENT));
        root.putString("Lineage", plant.lineage());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            CompoundTag t = data(stack);
            double potency = t.getDouble("Potency");
            double yield = t.contains("Yield") ? t.getDouble("Yield") : 1.0;
            int dur = (int) (300 + potency * 1500);
            if (yield > 1.2) user.addEffect(new MobEffectInstance(MobEffects.SATURATION, (int) (yield * 4), 0));
            if (t.getDouble("Glow") > 0.3) user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, dur * 2, 0));
            if (potency > 0.15) user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (60 + potency * 140), 0));
            if (potency > 0.35) user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 0));
            if (potency > 0.55) user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, dur, 1));
            if (potency > 0.75) user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 0));
            if (potency > 0.9) user.addEffect(new MobEffectInstance(MobEffects.LUCK, dur * 2, 0));
        }
        return super.finishUsingItem(stack, level, user);
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag t = data(stack);
        if (!t.contains("Plant")) return super.getName(stack);
        int rgb = java.awt.Color.HSBtoRGB((float) (t.getDouble("Hue") / 360.0),
            (float) Math.max(0.35, t.getDouble("Pigment")), 1.0f) & 0xFFFFFF;
        return Component.translatable("item.evolutionmod.mutant_fruit.of",
                Component.translatable("evolutionmod.species." + t.getString("Plant")))
            .withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        CompoundTag t = data(stack);
        if (!t.contains("Plant")) return;
        if (!t.getString("Lineage").isEmpty()) {
            tooltip.add(Component.translatable("evolutionmod.tip.lineage", t.getString("Lineage"))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("evolutionmod.fruit.potency",
            String.format("%.0f%%", t.getDouble("Potency") * 100)).withStyle(ChatFormatting.GREEN));
    }
}
