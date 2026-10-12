package net.minecraft.world.flag;

import com.mojang.serialization.Codec;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

public class FeatureFlags {
   public static final FeatureFlag VANILLA;
   public static final FeatureFlag BUNDLE;
   public static final FeatureFlag TRADE_REBALANCE;
   public static final FeatureFlagRegistry REGISTRY;
   public static final Codec<FeatureFlagSet> CODEC;
   public static final FeatureFlagSet VANILLA_SET;
   public static final FeatureFlagSet DEFAULT_FLAGS;

   public static String printMissingFlags(FeatureFlagSet pEnabledFeatures, FeatureFlagSet pRequestedFeatures) {
      return printMissingFlags(REGISTRY, pEnabledFeatures, pRequestedFeatures);
   }

   public static String printMissingFlags(FeatureFlagRegistry pRegistry, FeatureFlagSet pEnabledFeatures, FeatureFlagSet pRequestedFeatures) {
      Set<ResourceLocation> $$3 = pRegistry.toNames(pRequestedFeatures);
      Set<ResourceLocation> $$4 = pRegistry.toNames(pEnabledFeatures);
      return $$3.stream().filter(p_251831_ -> !$$4.contains(p_251831_)).map(ResourceLocation::toString).collect(Collectors.joining(", "));
   }

   public static boolean isExperimental(FeatureFlagSet pSet) {
      return !pSet.isSubsetOf(VANILLA_SET);
   }

   static {
      FeatureFlagRegistry.Builder $$0 = new FeatureFlagRegistry.Builder("main");
      VANILLA = $$0.createVanilla("vanilla");
      BUNDLE = $$0.createVanilla("bundle");
      TRADE_REBALANCE = $$0.createVanilla("trade_rebalance");
      REGISTRY = $$0.build();
      CODEC = REGISTRY.codec();
      VANILLA_SET = FeatureFlagSet.of(VANILLA);
      DEFAULT_FLAGS = VANILLA_SET;
   }
}
