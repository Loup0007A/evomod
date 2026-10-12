package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyExplosionDecay extends LootItemConditionalFunction {
   public static final MapCodec<ApplyExplosionDecay> CODEC = RecordCodecBuilder.mapCodec(
      p_297802_ -> commonFields(p_297802_).apply(p_297802_, ApplyExplosionDecay::new)
   );

   private ApplyExplosionDecay(List<LootItemCondition> p_301217_) {
      super(p_301217_);
   }

   @Override
   public LootItemFunctionType<ApplyExplosionDecay> getType() {
      return LootItemFunctions.EXPLOSION_DECAY;
   }

   @Override
   public ItemStack run(ItemStack pStack, LootContext pContext) {
      Float $$2 = pContext.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
      if ($$2 != null) {
         RandomSource $$3 = pContext.getRandom();
         float $$4 = 1.0F / $$2;
         int $$5 = pStack.getCount();
         int $$6 = 0;

         for (int $$7 = 0; $$7 < $$5; $$7++) {
            if ($$3.nextFloat() <= $$4) {
               $$6++;
            }
         }

         pStack.setCount($$6);
      }

      return pStack;
   }

   public static LootItemConditionalFunction.Builder<?> explosionDecay() {
      return simpleBuilder(ApplyExplosionDecay::new);
   }
}
