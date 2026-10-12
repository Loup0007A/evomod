package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
   private final TagKey<Instrument> instruments;

   public InstrumentItem(Item.Properties pProperties, TagKey<Instrument> pInstruments) {
      super(pProperties);
      this.instruments = pInstruments;
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
      Optional<ResourceKey<Instrument>> $$4 = this.getInstrument(pStack).flatMap(Holder::unwrapKey);
      if ($$4.isPresent()) {
         MutableComponent $$5 = Component.translatable(Util.makeDescriptionId("instrument", $$4.get().location()));
         pTooltipComponents.add($$5.withStyle(ChatFormatting.GRAY));
      }
   }

   public static ItemStack create(Item pItem, Holder<Instrument> pInstrument) {
      ItemStack $$2 = new ItemStack(pItem);
      $$2.set(DataComponents.INSTRUMENT, pInstrument);
      return $$2;
   }

   public static void setRandom(ItemStack pStack, TagKey<Instrument> pInstrumentTag, RandomSource pRandom) {
      Optional<Holder<Instrument>> $$3 = BuiltInRegistries.INSTRUMENT.getRandomElementOf(pInstrumentTag, pRandom);
      $$3.ifPresent(p_327152_ -> pStack.set(DataComponents.INSTRUMENT, (Holder<Instrument>)p_327152_));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pUsedHand);
      Optional<? extends Holder<Instrument>> $$4 = this.getInstrument($$3);
      if ($$4.isPresent()) {
         Instrument $$5 = $$4.get().value();
         pPlayer.startUsingItem(pUsedHand);
         play(pLevel, pPlayer, $$5);
         pPlayer.getCooldowns().addCooldown(this, $$5.useDuration());
         pPlayer.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResultHolder.consume($$3);
      } else {
         return InteractionResultHolder.fail($$3);
      }
   }

   @Override
   public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
      Optional<Holder<Instrument>> $$2 = this.getInstrument(pStack);
      return $$2.<Integer>map(p_248418_ -> p_248418_.value().useDuration()).orElse(0);
   }

   private Optional<Holder<Instrument>> getInstrument(ItemStack pStack) {
      Holder<Instrument> $$1 = pStack.get(DataComponents.INSTRUMENT);
      if ($$1 != null) {
         return Optional.of($$1);
      } else {
         Iterator<Holder<Instrument>> $$2 = BuiltInRegistries.INSTRUMENT.getTagOrEmpty(this.instruments).iterator();
         return $$2.hasNext() ? Optional.of($$2.next()) : Optional.empty();
      }
   }

   @Override
   public UseAnim getUseAnimation(ItemStack pStack) {
      return UseAnim.TOOT_HORN;
   }

   private static void play(Level pLevel, Player pPlayer, Instrument pInstrument) {
      SoundEvent $$3 = pInstrument.soundEvent().value();
      float $$4 = pInstrument.range() / 16.0F;
      pLevel.playSound(pPlayer, pPlayer, $$3, SoundSource.RECORDS, $$4, 1.0F);
      pLevel.gameEvent(GameEvent.INSTRUMENT_PLAY, pPlayer.position(), GameEvent.Context.of(pPlayer));
   }
}
