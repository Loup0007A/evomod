package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyBonusCount extends LootItemConditionalFunction {
   private static final Map<ResourceLocation, ApplyBonusCount.FormulaType> FORMULAS = Stream.of(
         ApplyBonusCount.BinomialWithBonusCount.TYPE, ApplyBonusCount.OreDrops.TYPE, ApplyBonusCount.UniformBonusCount.TYPE
      )
      .collect(Collectors.toMap(ApplyBonusCount.FormulaType::id, Function.identity()));
   private static final Codec<ApplyBonusCount.FormulaType> FORMULA_TYPE_CODEC = ResourceLocation.CODEC.comapFlatMap(p_297073_ -> {
      ApplyBonusCount.FormulaType $$1 = FORMULAS.get(p_297073_);
      return $$1 != null ? DataResult.success($$1) : DataResult.error(() -> "No formula type with id: '" + p_297073_ + "'");
   }, ApplyBonusCount.FormulaType::id);
   private static final MapCodec<ApplyBonusCount.Formula> FORMULA_CODEC = ExtraCodecs.dispatchOptionalValue(
      "formula", "parameters", FORMULA_TYPE_CODEC, ApplyBonusCount.Formula::getType, ApplyBonusCount.FormulaType::codec
   );
   public static final MapCodec<ApplyBonusCount> CODEC = RecordCodecBuilder.mapCodec(
      p_341977_ -> commonFields(p_341977_)
            .and(
               p_341977_.group(
                  Enchantment.CODEC.fieldOf("enchantment").forGetter(p_297072_ -> p_297072_.enchantment),
                  FORMULA_CODEC.forGetter(p_297058_ -> p_297058_.formula)
               )
            )
            .apply(p_341977_, ApplyBonusCount::new)
   );
   private final Holder<Enchantment> enchantment;
   private final ApplyBonusCount.Formula formula;

   private ApplyBonusCount(List<LootItemCondition> p_298095_, Holder<Enchantment> p_298508_, ApplyBonusCount.Formula p_79905_) {
      super(p_298095_);
      this.enchantment = p_298508_;
      this.formula = p_79905_;
   }

   @Override
   public LootItemFunctionType<ApplyBonusCount> getType() {
      return LootItemFunctions.APPLY_BONUS;
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.TOOL);
   }

   @Override
   public ItemStack run(ItemStack pStack, LootContext pContext) {
      ItemStack $$2 = pContext.getParamOrNull(LootContextParams.TOOL);
      if ($$2 != null) {
         int $$3 = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, $$2);
         int $$4 = this.formula.calculateNewCount(pContext.getRandom(), pStack.getCount(), $$3);
         pStack.setCount($$4);
      }

      return pStack;
   }

   public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(
      Holder<Enchantment> pEnchantment, float pProbability, int pExtraRounds
   ) {
      return simpleBuilder(p_341983_ -> new ApplyBonusCount(p_341983_, pEnchantment, new ApplyBonusCount.BinomialWithBonusCount(pExtraRounds, pProbability)));
   }

   public static LootItemConditionalFunction.Builder<?> addOreBonusCount(Holder<Enchantment> pEnchantment) {
      return simpleBuilder(p_341979_ -> new ApplyBonusCount(p_341979_, pEnchantment, new ApplyBonusCount.OreDrops()));
   }

   public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> pEnchantment) {
      return simpleBuilder(p_341988_ -> new ApplyBonusCount(p_341988_, pEnchantment, new ApplyBonusCount.UniformBonusCount(1)));
   }

   public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> pEnchantment, int pBonusMultiplier) {
      return simpleBuilder(p_341986_ -> new ApplyBonusCount(p_341986_, pEnchantment, new ApplyBonusCount.UniformBonusCount(pBonusMultiplier)));
   }

   static record BinomialWithBonusCount(int extraRounds, float probability) implements ApplyBonusCount.Formula {
      private static final Codec<ApplyBonusCount.BinomialWithBonusCount> CODEC = RecordCodecBuilder.create(
         p_299643_ -> p_299643_.group(
                  Codec.INT.fieldOf("extra").forGetter(ApplyBonusCount.BinomialWithBonusCount::extraRounds),
                  Codec.FLOAT.fieldOf("probability").forGetter(ApplyBonusCount.BinomialWithBonusCount::probability)
               )
               .apply(p_299643_, ApplyBonusCount.BinomialWithBonusCount::new)
      );
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(
         ResourceLocation.withDefaultNamespace("binomial_with_bonus_count"), CODEC
      );

      @Override
      public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
         for (int $$3 = 0; $$3 < pEnchantmentLevel + this.extraRounds; $$3++) {
            if (pRandom.nextFloat() < this.probability) {
               pOriginalCount++;
            }
         }

         return pOriginalCount;
      }

      @Override
      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }

   interface Formula {
      int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel);

      ApplyBonusCount.FormulaType getType();
   }

   static record FormulaType(ResourceLocation id, Codec<? extends ApplyBonusCount.Formula> codec) {
   }

   static record OreDrops() implements ApplyBonusCount.Formula {
      public static final Codec<ApplyBonusCount.OreDrops> CODEC = Codec.unit(ApplyBonusCount.OreDrops::new);
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(ResourceLocation.withDefaultNamespace("ore_drops"), CODEC);

      @Override
      public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
         if (pEnchantmentLevel > 0) {
            int $$3 = pRandom.nextInt(pEnchantmentLevel + 2) - 1;
            if ($$3 < 0) {
               $$3 = 0;
            }

            return pOriginalCount * ($$3 + 1);
         } else {
            return pOriginalCount;
         }
      }

      @Override
      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }

   static record UniformBonusCount(int bonusMultiplier) implements ApplyBonusCount.Formula {
      public static final Codec<ApplyBonusCount.UniformBonusCount> CODEC = RecordCodecBuilder.create(
         p_297464_ -> p_297464_.group(Codec.INT.fieldOf("bonusMultiplier").forGetter(ApplyBonusCount.UniformBonusCount::bonusMultiplier))
               .apply(p_297464_, ApplyBonusCount.UniformBonusCount::new)
      );
      public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(
         ResourceLocation.withDefaultNamespace("uniform_bonus_count"), CODEC
      );

      @Override
      public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
         return pOriginalCount + pRandom.nextInt(this.bonusMultiplier * pEnchantmentLevel + 1);
      }

      @Override
      public ApplyBonusCount.FormulaType getType() {
         return TYPE;
      }
   }
}
