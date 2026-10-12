package net.minecraft.world.item.alchemy;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public record PotionContents(Optional<Holder<Potion>> potion, Optional<Integer> customColor, List<MobEffectInstance> customEffects) {
   public static final PotionContents EMPTY = new PotionContents(Optional.empty(), Optional.empty(), List.of());
   private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);
   private static final int BASE_POTION_COLOR = -13083194;
   private static final Codec<PotionContents> FULL_CODEC = RecordCodecBuilder.create(
      p_341577_ -> p_341577_.group(
               Potion.CODEC.optionalFieldOf("potion").forGetter(PotionContents::potion),
               Codec.INT.optionalFieldOf("custom_color").forGetter(PotionContents::customColor),
               MobEffectInstance.CODEC.listOf().optionalFieldOf("custom_effects", List.of()).forGetter(PotionContents::customEffects)
            )
            .apply(p_341577_, PotionContents::new)
   );
   public static final Codec<PotionContents> CODEC = Codec.withAlternative(FULL_CODEC, Potion.CODEC, PotionContents::new);
   public static final StreamCodec<RegistryFriendlyByteBuf, PotionContents> STREAM_CODEC = StreamCodec.composite(
      Potion.STREAM_CODEC.apply(ByteBufCodecs::optional),
      PotionContents::potion,
      ByteBufCodecs.INT.apply(ByteBufCodecs::optional),
      PotionContents::customColor,
      MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
      PotionContents::customEffects,
      PotionContents::new
   );

   public PotionContents(Holder<Potion> p_335062_) {
      this(Optional.of(p_335062_), Optional.empty(), List.of());
   }

   public static ItemStack createItemStack(Item pItem, Holder<Potion> pPotion) {
      ItemStack $$2 = new ItemStack(pItem);
      $$2.set(DataComponents.POTION_CONTENTS, new PotionContents(pPotion));
      return $$2;
   }

   public boolean is(Holder<Potion> pPotion) {
      return this.potion.isPresent() && this.potion.get().is(pPotion) && this.customEffects.isEmpty();
   }

   public Iterable<MobEffectInstance> getAllEffects() {
      if (this.potion.isEmpty()) {
         return this.customEffects;
      } else {
         return (Iterable<MobEffectInstance>)(this.customEffects.isEmpty()
            ? this.potion.get().value().getEffects()
            : Iterables.concat(this.potion.get().value().getEffects(), this.customEffects));
      }
   }

   public void forEachEffect(Consumer<MobEffectInstance> pAction) {
      if (this.potion.isPresent()) {
         for (MobEffectInstance $$1 : this.potion.get().value().getEffects()) {
            pAction.accept(new MobEffectInstance($$1));
         }
      }

      for (MobEffectInstance $$2 : this.customEffects) {
         pAction.accept(new MobEffectInstance($$2));
      }
   }

   public PotionContents withPotion(Holder<Potion> pPotion) {
      return new PotionContents(Optional.of(pPotion), this.customColor, this.customEffects);
   }

   public PotionContents withEffectAdded(MobEffectInstance pEffect) {
      return new PotionContents(this.potion, this.customColor, Util.copyAndAdd(this.customEffects, pEffect));
   }

   public int getColor() {
      return this.customColor.isPresent() ? this.customColor.get() : getColor(this.getAllEffects());
   }

   public static int getColor(Holder<Potion> pPotion) {
      return getColor(pPotion.value().getEffects());
   }

   public static int getColor(Iterable<MobEffectInstance> pEffects) {
      return getColorOptional(pEffects).orElse(-13083194);
   }

   public static OptionalInt getColorOptional(Iterable<MobEffectInstance> pEffects) {
      int $$1 = 0;
      int $$2 = 0;
      int $$3 = 0;
      int $$4 = 0;

      for (MobEffectInstance $$5 : pEffects) {
         if ($$5.isVisible()) {
            int $$6 = $$5.getEffect().value().getColor();
            int $$7 = $$5.getAmplifier() + 1;
            $$1 += $$7 * FastColor.ARGB32.red($$6);
            $$2 += $$7 * FastColor.ARGB32.green($$6);
            $$3 += $$7 * FastColor.ARGB32.blue($$6);
            $$4 += $$7;
         }
      }

      return $$4 == 0 ? OptionalInt.empty() : OptionalInt.of(FastColor.ARGB32.color($$1 / $$4, $$2 / $$4, $$3 / $$4));
   }

   public boolean hasEffects() {
      return !this.customEffects.isEmpty() ? true : this.potion.isPresent() && !this.potion.get().value().getEffects().isEmpty();
   }

   public List<MobEffectInstance> customEffects() {
      return Lists.transform(this.customEffects, MobEffectInstance::new);
   }

   public void addPotionTooltip(Consumer<Component> pTooltipAdder, float pDurationFactor, float pTicksPerSecond) {
      addPotionTooltip(this.getAllEffects(), pTooltipAdder, pDurationFactor, pTicksPerSecond);
   }

   public static void addPotionTooltip(Iterable<MobEffectInstance> pEffects, Consumer<Component> pTooltipAdder, float pDurationFactor, float pTicksPerSecond) {
      List<Pair<Holder<Attribute>, AttributeModifier>> $$4 = Lists.newArrayList();
      boolean $$5 = true;

      for (MobEffectInstance $$6 : pEffects) {
         $$5 = false;
         MutableComponent $$7 = Component.translatable($$6.getDescriptionId());
         Holder<MobEffect> $$8 = $$6.getEffect();
         $$8.value().createModifiers($$6.getAmplifier(), (p_329075_, p_331827_) -> $$4.add(new Pair(p_329075_, p_331827_)));
         if ($$6.getAmplifier() > 0) {
            $$7 = Component.translatable("potion.withAmplifier", $$7, Component.translatable("potion.potency." + $$6.getAmplifier()));
         }

         if (!$$6.endsWithin(20)) {
            $$7 = Component.translatable("potion.withDuration", $$7, MobEffectUtil.formatDuration($$6, pDurationFactor, pTicksPerSecond));
         }

         pTooltipAdder.accept($$7.withStyle($$8.value().getCategory().getTooltipFormatting()));
      }

      if ($$5) {
         pTooltipAdder.accept(NO_EFFECT);
      }

      if (!$$4.isEmpty()) {
         pTooltipAdder.accept(CommonComponents.EMPTY);
         pTooltipAdder.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));

         for (Pair<Holder<Attribute>, AttributeModifier> $$9 : $$4) {
            AttributeModifier $$10 = (AttributeModifier)$$9.getSecond();
            double $$11 = $$10.amount();
            double $$13;
            if ($$10.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE && $$10.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
               $$13 = $$10.amount();
            } else {
               $$13 = $$10.amount() * 100.0;
            }

            if ($$11 > 0.0) {
               pTooltipAdder.accept(
                  Component.translatable(
                        "attribute.modifier.plus." + $$10.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format($$13),
                        Component.translatable(((Attribute)((Holder)$$9.getFirst()).value()).getDescriptionId())
                     )
                     .withStyle(ChatFormatting.BLUE)
               );
            } else if ($$11 < 0.0) {
               $$13 *= -1.0;
               pTooltipAdder.accept(
                  Component.translatable(
                        "attribute.modifier.take." + $$10.operation().id(),
                        ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format($$13),
                        Component.translatable(((Attribute)((Holder)$$9.getFirst()).value()).getDescriptionId())
                     )
                     .withStyle(ChatFormatting.RED)
               );
            }
         }
      }
   }
}
