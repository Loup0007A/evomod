package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record BonusLevelTableCondition(Holder<Enchantment> enchantment, List<Float> values) implements LootItemCondition {
   public static final MapCodec<BonusLevelTableCondition> CODEC = RecordCodecBuilder.mapCodec(
      p_342021_ -> p_342021_.group(
               Enchantment.CODEC.fieldOf("enchantment").forGetter(BonusLevelTableCondition::enchantment),
               ExtraCodecs.nonEmptyList(Codec.FLOAT.listOf()).fieldOf("chances").forGetter(BonusLevelTableCondition::values)
            )
            .apply(p_342021_, BonusLevelTableCondition::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.TABLE_BONUS;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.TOOL);
   }

   public boolean test(LootContext pContext) {
      ItemStack $$1 = pContext.getParamOrNull(LootContextParams.TOOL);
      int $$2 = $$1 != null ? EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, $$1) : 0;
      float $$3 = this.values.get(Math.min($$2, this.values.size() - 1));
      return pContext.getRandom().nextFloat() < $$3;
   }

   public static LootItemCondition.Builder bonusLevelFlatChance(Holder<Enchantment> pEnchantment, float... pValues) {
      List<Float> $$2 = new ArrayList<>(pValues.length);

      for (float $$3 : pValues) {
         $$2.add($$3);
      }

      return () -> new BonusLevelTableCondition(pEnchantment, $$2);
   }
}
