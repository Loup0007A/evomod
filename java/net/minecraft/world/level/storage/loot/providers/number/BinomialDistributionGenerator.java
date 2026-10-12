package net.minecraft.world.level.storage.loot.providers.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public record BinomialDistributionGenerator(NumberProvider n, NumberProvider p) implements NumberProvider {
   public static final MapCodec<BinomialDistributionGenerator> CODEC = RecordCodecBuilder.mapCodec(
      p_297459_ -> p_297459_.group(
               NumberProviders.CODEC.fieldOf("n").forGetter(BinomialDistributionGenerator::n),
               NumberProviders.CODEC.fieldOf("p").forGetter(BinomialDistributionGenerator::p)
            )
            .apply(p_297459_, BinomialDistributionGenerator::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.BINOMIAL;
   }

   @Override
   public int getInt(LootContext pLootContext) {
      int $$1 = this.n.getInt(pLootContext);
      float $$2 = this.p.getFloat(pLootContext);
      RandomSource $$3 = pLootContext.getRandom();
      int $$4 = 0;

      for (int $$5 = 0; $$5 < $$1; $$5++) {
         if ($$3.nextFloat() < $$2) {
            $$4++;
         }
      }

      return $$4;
   }

   @Override
   public float getFloat(LootContext pLootContext) {
      return (float)this.getInt(pLootContext);
   }

   public static BinomialDistributionGenerator binomial(int pN, float pP) {
      return new BinomialDistributionGenerator(ConstantValue.exactly((float)pN), ConstantValue.exactly(pP));
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return Sets.union(this.n.getReferencedContextParams(), this.p.getReferencedContextParams());
   }
}
