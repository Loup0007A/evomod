package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class EnchantRandomlyFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<EnchantRandomlyFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_341996_ -> commonFields(p_341996_)
            .and(
               p_341996_.group(
                  RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("options").forGetter(p_341991_ -> p_341991_.options),
                  Codec.BOOL.optionalFieldOf("only_compatible", true).forGetter(p_341992_ -> p_341992_.onlyCompatible)
               )
            )
            .apply(p_341996_, EnchantRandomlyFunction::new)
   );
   private final Optional<HolderSet<Enchantment>> options;
   private final boolean onlyCompatible;

   EnchantRandomlyFunction(List<LootItemCondition> p_298352_, Optional<HolderSet<Enchantment>> p_297532_, boolean p_344714_) {
      super(p_298352_);
      this.options = p_297532_;
      this.onlyCompatible = p_344714_;
   }

   @Override
   public LootItemFunctionType<EnchantRandomlyFunction> getType() {
      return LootItemFunctions.ENCHANT_RANDOMLY;
   }

   @Override
   public ItemStack run(ItemStack pStack, LootContext pContext) {
      RandomSource $$2 = pContext.getRandom();
      boolean $$3 = pStack.is(Items.BOOK);
      boolean $$4 = !$$3 && this.onlyCompatible;
      Stream<Holder<Enchantment>> $$5 = this.options
         .map(HolderSet::stream)
         .orElseGet(() -> pContext.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().map(Function.identity()))
         .filter(p_341995_ -> !$$4 || p_341995_.value().canEnchant(pStack));
      List<Holder<Enchantment>> $$6 = $$5.toList();
      Optional<Holder<Enchantment>> $$7 = Util.getRandomSafe($$6, $$2);
      if ($$7.isEmpty()) {
         LOGGER.warn("Couldn't find a compatible enchantment for {}", pStack);
         return pStack;
      } else {
         return enchantItem(pStack, $$7.get(), $$2);
      }
   }

   private static ItemStack enchantItem(ItemStack pStack, Holder<Enchantment> pEnchantment, RandomSource pRandom) {
      int $$3 = Mth.nextInt(pRandom, pEnchantment.value().getMinLevel(), pEnchantment.value().getMaxLevel());
      if (pStack.is(Items.BOOK)) {
         pStack = new ItemStack(Items.ENCHANTED_BOOK);
      }

      pStack.enchant(pEnchantment, $$3);
      return pStack;
   }

   public static EnchantRandomlyFunction.Builder randomEnchantment() {
      return new EnchantRandomlyFunction.Builder();
   }

   public static EnchantRandomlyFunction.Builder randomApplicableEnchantment(HolderLookup.Provider pRegistries) {
      return randomEnchantment().withOneOf(pRegistries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<EnchantRandomlyFunction.Builder> {
      private Optional<HolderSet<Enchantment>> options = Optional.empty();
      private boolean onlyCompatible = true;

      protected EnchantRandomlyFunction.Builder getThis() {
         return this;
      }

      public EnchantRandomlyFunction.Builder withEnchantment(Holder<Enchantment> pEnchantment) {
         this.options = Optional.of(HolderSet.direct(pEnchantment));
         return this;
      }

      public EnchantRandomlyFunction.Builder withOneOf(HolderSet<Enchantment> pEnchantments) {
         this.options = Optional.of(pEnchantments);
         return this;
      }

      public EnchantRandomlyFunction.Builder allowingIncompatibleEnchantments() {
         this.onlyCompatible = false;
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new EnchantRandomlyFunction(this.getConditions(), this.options, this.onlyCompatible);
      }
   }
}
