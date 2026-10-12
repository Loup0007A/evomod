package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicLike;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

public class WorldGenSettingsFix extends DataFix {
   private static final String VILLAGE = "minecraft:village";
   private static final String DESERT_PYRAMID = "minecraft:desert_pyramid";
   private static final String IGLOO = "minecraft:igloo";
   private static final String JUNGLE_TEMPLE = "minecraft:jungle_pyramid";
   private static final String SWAMP_HUT = "minecraft:swamp_hut";
   private static final String PILLAGER_OUTPOST = "minecraft:pillager_outpost";
   private static final String END_CITY = "minecraft:endcity";
   private static final String WOODLAND_MANSION = "minecraft:mansion";
   private static final String OCEAN_MONUMENT = "minecraft:monument";
   private static final ImmutableMap<String, WorldGenSettingsFix.StructureFeatureConfiguration> DEFAULTS = ImmutableMap.builder()
      .put("minecraft:village", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 10387312))
      .put("minecraft:desert_pyramid", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357617))
      .put("minecraft:igloo", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357618))
      .put("minecraft:jungle_pyramid", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357619))
      .put("minecraft:swamp_hut", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 14357620))
      .put("minecraft:pillager_outpost", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 8, 165745296))
      .put("minecraft:monument", new WorldGenSettingsFix.StructureFeatureConfiguration(32, 5, 10387313))
      .put("minecraft:endcity", new WorldGenSettingsFix.StructureFeatureConfiguration(20, 11, 10387313))
      .put("minecraft:mansion", new WorldGenSettingsFix.StructureFeatureConfiguration(80, 20, 10387319))
      .build();

   public WorldGenSettingsFix(Schema pOutputSchema) {
      super(pOutputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "WorldGenSettings building",
         this.getInputSchema().getType(References.WORLD_GEN_SETTINGS),
         p_17184_ -> p_17184_.update(DSL.remainderFinder(), WorldGenSettingsFix::fix)
      );
   }

   private static <T> Dynamic<T> noise(long pSeed, DynamicLike<T> pData, Dynamic<T> pSettings, Dynamic<T> pBiomeNoise) {
      return pData.createMap(
         ImmutableMap.of(
            pData.createString("type"),
            pData.createString("minecraft:noise"),
            pData.createString("biome_source"),
            pBiomeNoise,
            pData.createString("seed"),
            pData.createLong(pSeed),
            pData.createString("settings"),
            pSettings
         )
      );
   }

   private static <T> Dynamic<T> vanillaBiomeSource(Dynamic<T> pData, long pSeed, boolean pLegacyBiomeInitLayer, boolean pLargeBiomes) {
      Builder<Dynamic<T>, Dynamic<T>> $$4 = ImmutableMap.builder()
         .put(pData.createString("type"), pData.createString("minecraft:vanilla_layered"))
         .put(pData.createString("seed"), pData.createLong(pSeed))
         .put(pData.createString("large_biomes"), pData.createBoolean(pLargeBiomes));
      if (pLegacyBiomeInitLayer) {
         $$4.put(pData.createString("legacy_biome_init_layer"), pData.createBoolean(pLegacyBiomeInitLayer));
      }

      return pData.createMap($$4.build());
   }

   private static <T> Dynamic<T> fix(Dynamic<T> p_17186_) {
      DynamicOps<T> $$1 = p_17186_.getOps();
      long $$2 = p_17186_.get("RandomSeed").asLong(0L);
      Optional<String> $$3 = p_17186_.get("generatorName").asString().map(p_17227_ -> p_17227_.toLowerCase(Locale.ROOT)).result();
      Optional<String> $$4 = p_17186_.get("legacy_custom_options")
         .asString()
         .result()
         .map(Optional::of)
         .orElseGet(() -> $$3.equals(Optional.of("customized")) ? p_17186_.get("generatorOptions").asString().result() : Optional.empty());
      boolean $$5 = false;
      Dynamic<T> $$6;
      if ($$3.equals(Optional.of("customized"))) {
         $$6 = defaultOverworld(p_17186_, $$2);
      } else if ($$3.isEmpty()) {
         $$6 = defaultOverworld(p_17186_, $$2);
      } else {
         String $$28 = $$3.get();
         switch ($$28) {
            case "flat":
               OptionalDynamic<T> $$8 = p_17186_.get("generatorOptions");
               Map<Dynamic<T>, Dynamic<T>> $$9 = fixFlatStructures($$1, $$8);
               $$6 = p_17186_.createMap(
                  ImmutableMap.of(
                     p_17186_.createString("type"),
                     p_17186_.createString("minecraft:flat"),
                     p_17186_.createString("settings"),
                     p_17186_.createMap(
                        ImmutableMap.of(
                           p_17186_.createString("structures"),
                           p_17186_.createMap($$9),
                           p_17186_.createString("layers"),
                           $$8.get("layers")
                              .result()
                              .orElseGet(
                                 () -> p_17186_.createList(
                                       Stream.of(
                                          p_17186_.createMap(
                                             ImmutableMap.of(
                                                p_17186_.createString("height"),
                                                p_17186_.createInt(1),
                                                p_17186_.createString("block"),
                                                p_17186_.createString("minecraft:bedrock")
                                             )
                                          ),
                                          p_17186_.createMap(
                                             ImmutableMap.of(
                                                p_17186_.createString("height"),
                                                p_17186_.createInt(2),
                                                p_17186_.createString("block"),
                                                p_17186_.createString("minecraft:dirt")
                                             )
                                          ),
                                          p_17186_.createMap(
                                             ImmutableMap.of(
                                                p_17186_.createString("height"),
                                                p_17186_.createInt(1),
                                                p_17186_.createString("block"),
                                                p_17186_.createString("minecraft:grass_block")
                                             )
                                          )
                                       )
                                    )
                              ),
                           p_17186_.createString("biome"),
                           p_17186_.createString($$8.get("biome").asString("minecraft:plains"))
                        )
                     )
                  )
               );
               break;
            case "debug_all_block_states":
               $$6 = p_17186_.createMap(ImmutableMap.of(p_17186_.createString("type"), p_17186_.createString("minecraft:debug")));
               break;
            case "buffet":
               OptionalDynamic<T> $$12 = p_17186_.get("generatorOptions");
               OptionalDynamic<?> $$13 = $$12.get("chunk_generator");
               Optional<String> $$14 = $$13.get("type").asString().result();
               Dynamic<T> $$15;
               if (Objects.equals($$14, Optional.of("minecraft:caves"))) {
                  $$15 = p_17186_.createString("minecraft:caves");
                  $$5 = true;
               } else if (Objects.equals($$14, Optional.of("minecraft:floating_islands"))) {
                  $$15 = p_17186_.createString("minecraft:floating_islands");
               } else {
                  $$15 = p_17186_.createString("minecraft:overworld");
               }

               Dynamic<T> $$18 = $$12.get("biome_source")
                  .result()
                  .orElseGet(() -> p_17186_.createMap(ImmutableMap.of(p_17186_.createString("type"), p_17186_.createString("minecraft:fixed"))));
               Dynamic<T> $$20;
               if ($$18.get("type").asString().result().equals(Optional.of("minecraft:fixed"))) {
                  String $$19 = $$18.get("options")
                     .get("biomes")
                     .asStream()
                     .findFirst()
                     .flatMap(p_326673_ -> p_326673_.asString().result())
                     .orElse("minecraft:ocean");
                  $$20 = $$18.remove("options").set("biome", p_17186_.createString($$19));
               } else {
                  $$20 = $$18;
               }

               $$6 = noise($$2, p_17186_, $$15, $$20);
               break;
            default:
               boolean $$23 = $$3.get().equals("default");
               boolean $$24 = $$3.get().equals("default_1_1") || $$23 && p_17186_.get("generatorVersion").asInt(0) == 0;
               boolean $$25 = $$3.get().equals("amplified");
               boolean $$26 = $$3.get().equals("largebiomes");
               $$6 = noise(
                  $$2, p_17186_, p_17186_.createString($$25 ? "minecraft:amplified" : "minecraft:overworld"), vanillaBiomeSource(p_17186_, $$2, $$24, $$26)
               );
         }
      }

      boolean $$28 = p_17186_.get("MapFeatures").asBoolean(true);
      boolean $$29 = p_17186_.get("BonusChest").asBoolean(false);
      Builder<T, T> $$30 = ImmutableMap.builder();
      $$30.put($$1.createString("seed"), $$1.createLong($$2));
      $$30.put($$1.createString("generate_features"), $$1.createBoolean($$28));
      $$30.put($$1.createString("bonus_chest"), $$1.createBoolean($$29));
      $$30.put($$1.createString("dimensions"), vanillaLevels(p_17186_, $$2, $$6, $$5));
      $$4.ifPresent(p_17182_ -> $$30.put($$1.createString("legacy_custom_options"), $$1.createString(p_17182_)));
      return new Dynamic($$1, $$1.createMap($$30.build()));
   }

   protected static <T> Dynamic<T> defaultOverworld(Dynamic<T> pData, long pSeed) {
      return noise(pSeed, pData, pData.createString("minecraft:overworld"), vanillaBiomeSource(pData, pSeed, false, false));
   }

   protected static <T> T vanillaLevels(Dynamic<T> pData, long pSeed, Dynamic<T> p_17193_, boolean pCaves) {
      DynamicOps<T> $$4 = pData.getOps();
      return (T)$$4.createMap(
         ImmutableMap.of(
            $$4.createString("minecraft:overworld"),
            $$4.createMap(
               ImmutableMap.of(
                  $$4.createString("type"),
                  $$4.createString("minecraft:overworld" + (pCaves ? "_caves" : "")),
                  $$4.createString("generator"),
                  p_17193_.getValue()
               )
            ),
            $$4.createString("minecraft:the_nether"),
            $$4.createMap(
               ImmutableMap.of(
                  $$4.createString("type"),
                  $$4.createString("minecraft:the_nether"),
                  $$4.createString("generator"),
                  noise(
                        pSeed,
                        pData,
                        pData.createString("minecraft:nether"),
                        pData.createMap(
                           ImmutableMap.of(
                              pData.createString("type"),
                              pData.createString("minecraft:multi_noise"),
                              pData.createString("seed"),
                              pData.createLong(pSeed),
                              pData.createString("preset"),
                              pData.createString("minecraft:nether")
                           )
                        )
                     )
                     .getValue()
               )
            ),
            $$4.createString("minecraft:the_end"),
            $$4.createMap(
               ImmutableMap.of(
                  $$4.createString("type"),
                  $$4.createString("minecraft:the_end"),
                  $$4.createString("generator"),
                  noise(
                        pSeed,
                        pData,
                        pData.createString("minecraft:end"),
                        pData.createMap(
                           ImmutableMap.of(
                              pData.createString("type"), pData.createString("minecraft:the_end"), pData.createString("seed"), pData.createLong(pSeed)
                           )
                        )
                     )
                     .getValue()
               )
            )
         )
      );
   }

   private static <T> Map<Dynamic<T>, Dynamic<T>> fixFlatStructures(DynamicOps<T> pOps, OptionalDynamic<T> pGeneratorOptions) {
      MutableInt $$2 = new MutableInt(32);
      MutableInt $$3 = new MutableInt(3);
      MutableInt $$4 = new MutableInt(128);
      MutableBoolean $$5 = new MutableBoolean(false);
      Map<String, WorldGenSettingsFix.StructureFeatureConfiguration> $$6 = Maps.newHashMap();
      if (pGeneratorOptions.result().isEmpty()) {
         $$5.setTrue();
         $$6.put("minecraft:village", (WorldGenSettingsFix.StructureFeatureConfiguration)DEFAULTS.get("minecraft:village"));
      }

      pGeneratorOptions.get("structures")
         .flatMap(Dynamic::getMapValues)
         .ifSuccess(
            p_17257_ -> p_17257_.forEach(
                  (p_326671_, p_326672_) -> p_326672_.getMapValues()
                        .result()
                        .ifPresent(
                           p_145816_ -> p_145816_.forEach(
                                 (p_145807_, p_145808_) -> {
                                    String $$8 = p_326671_.asString("");
                                    String $$9 = p_145807_.asString("");
                                    String $$10 = p_145808_.asString("");
                                    if ("stronghold".equals($$8)) {
                                       $$5.setTrue();
                                       switch ($$9) {
                                          case "distance":
                                             $$2.setValue(getInt($$10, $$2.getValue(), 1));
                                             return;
                                          case "spread":
                                             $$3.setValue(getInt($$10, $$3.getValue(), 1));
                                             return;
                                          case "count":
                                             $$4.setValue(getInt($$10, $$4.getValue(), 1));
                                             return;
                                       }
                                    } else {
                                       switch ($$9) {
                                          case "distance":
                                             switch ($$8) {
                                                case "village":
                                                   setSpacing($$6, "minecraft:village", $$10, 9);
                                                   return;
                                                case "biome_1":
                                                   setSpacing($$6, "minecraft:desert_pyramid", $$10, 9);
                                                   setSpacing($$6, "minecraft:igloo", $$10, 9);
                                                   setSpacing($$6, "minecraft:jungle_pyramid", $$10, 9);
                                                   setSpacing($$6, "minecraft:swamp_hut", $$10, 9);
                                                   setSpacing($$6, "minecraft:pillager_outpost", $$10, 9);
                                                   return;
                                                case "endcity":
                                                   setSpacing($$6, "minecraft:endcity", $$10, 1);
                                                   return;
                                                case "mansion":
                                                   setSpacing($$6, "minecraft:mansion", $$10, 1);
                                                   return;
                                                default:
                                                   return;
                                             }
                                          case "separation":
                                             if ("oceanmonument".equals($$8)) {
                                                WorldGenSettingsFix.StructureFeatureConfiguration $$11 = $$6.getOrDefault(
                                                   "minecraft:monument", (WorldGenSettingsFix.StructureFeatureConfiguration)DEFAULTS.get("minecraft:monument")
                                                );
                                                int $$12 = getInt($$10, $$11.separation, 1);
                                                $$6.put(
                                                   "minecraft:monument",
                                                   new WorldGenSettingsFix.StructureFeatureConfiguration($$12, $$11.separation, $$11.salt)
                                                );
                                             }

                                             return;
                                          case "spacing":
                                             if ("oceanmonument".equals($$8)) {
                                                setSpacing($$6, "minecraft:monument", $$10, 1);
                                             }

                                             return;
                                       }
                                    }
                                 }
                              )
                        )
               )
         );
      Builder<Dynamic<T>, Dynamic<T>> $$7 = ImmutableMap.builder();
      $$7.put(
         pGeneratorOptions.createString("structures"),
         pGeneratorOptions.createMap(
            $$6.entrySet()
               .stream()
               .collect(Collectors.toMap(p_17225_ -> pGeneratorOptions.createString(p_17225_.getKey()), p_17222_ -> p_17222_.getValue().serialize(pOps)))
         )
      );
      if ($$5.isTrue()) {
         $$7.put(
            pGeneratorOptions.createString("stronghold"),
            pGeneratorOptions.createMap(
               ImmutableMap.of(
                  pGeneratorOptions.createString("distance"),
                  pGeneratorOptions.createInt($$2.getValue()),
                  pGeneratorOptions.createString("spread"),
                  pGeneratorOptions.createInt($$3.getValue()),
                  pGeneratorOptions.createString("count"),
                  pGeneratorOptions.createInt($$4.getValue())
               )
            )
         );
      }

      return $$7.build();
   }

   private static int getInt(String pString, int pDefaultValue) {
      return NumberUtils.toInt(pString, pDefaultValue);
   }

   private static int getInt(String pString, int pDefaultValue, int pMinValue) {
      return Math.max(pMinValue, getInt(pString, pDefaultValue));
   }

   private static void setSpacing(Map<String, WorldGenSettingsFix.StructureFeatureConfiguration> p_17236_, String p_17237_, String pSpacing, int p_17239_) {
      WorldGenSettingsFix.StructureFeatureConfiguration $$4 = p_17236_.getOrDefault(
         p_17237_, (WorldGenSettingsFix.StructureFeatureConfiguration)DEFAULTS.get(p_17237_)
      );
      int $$5 = getInt(pSpacing, $$4.spacing, p_17239_);
      p_17236_.put(p_17237_, new WorldGenSettingsFix.StructureFeatureConfiguration($$5, $$4.separation, $$4.salt));
   }

   static final class StructureFeatureConfiguration {
      public static final Codec<WorldGenSettingsFix.StructureFeatureConfiguration> CODEC = RecordCodecBuilder.create(
         p_17279_ -> p_17279_.group(
                  Codec.INT.fieldOf("spacing").forGetter(p_145830_ -> p_145830_.spacing),
                  Codec.INT.fieldOf("separation").forGetter(p_145828_ -> p_145828_.separation),
                  Codec.INT.fieldOf("salt").forGetter(p_145826_ -> p_145826_.salt)
               )
               .apply(p_17279_, WorldGenSettingsFix.StructureFeatureConfiguration::new)
      );
      final int spacing;
      final int separation;
      final int salt;

      public StructureFeatureConfiguration(int p_17271_, int p_17272_, int p_17273_) {
         this.spacing = p_17271_;
         this.separation = p_17272_;
         this.salt = p_17273_;
      }

      public <T> Dynamic<T> serialize(DynamicOps<T> pOps) {
         return new Dynamic(pOps, CODEC.encodeStart(pOps, this).result().orElse(pOps.emptyMap()));
      }
   }
}
