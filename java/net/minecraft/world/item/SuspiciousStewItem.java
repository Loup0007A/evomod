package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;

public class SuspiciousStewItem extends Item {
   public static final int DEFAULT_DURATION = 160;

   public SuspiciousStewItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
      if (pTooltipFlag.isCreative()) {
         List<MobEffectInstance> $$4 = new ArrayList<>();
         SuspiciousStewEffects $$5 = pStack.getOrDefault(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY);

         for (SuspiciousStewEffects.Entry $$6 : $$5.effects()) {
            $$4.add($$6.createEffectInstance());
         }

         PotionContents.addPotionTooltip($$4, pTooltipComponents::add, 1.0F, pContext.tickRate());
      }
   }

   @Override
   public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving) {
      SuspiciousStewEffects $$3 = pStack.getOrDefault(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY);

      for (SuspiciousStewEffects.Entry $$4 : $$3.effects()) {
         pEntityLiving.addEffect($$4.createEffectInstance());
      }

      return super.finishUsingItem(pStack, pLevel, pEntityLiving);
   }
}
