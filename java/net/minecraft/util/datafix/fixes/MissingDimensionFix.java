package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FieldFinder;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.CompoundList.CompoundListType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.List;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class MissingDimensionFix extends DataFix {
   public MissingDimensionFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   protected static <A> Type<Pair<A, Dynamic<?>>> fields(String pName, Type<A> pElement) {
      return DSL.and(DSL.field(pName, pElement), DSL.remainderType());
   }

   protected static <A> Type<Pair<Either<A, Unit>, Dynamic<?>>> optionalFields(String pName, Type<A> pElement) {
      return DSL.and(DSL.optional(DSL.field(pName, pElement)), DSL.remainderType());
   }

   protected static <A1, A2> Type<Pair<Either<A1, Unit>, Pair<Either<A2, Unit>, Dynamic<?>>>> optionalFields(
      String pName1, Type<A1> pElement1, String pName2, Type<A2> pElement2
   ) {
      return DSL.and(DSL.optional(DSL.field(pName1, pElement1)), DSL.optional(DSL.field(pName2, pElement2)), DSL.remainderType());
   }

   protected TypeRewriteRule makeRule() {
      Schema $$0 = this.getInputSchema();
      Type<?> $$1 = DSL.taggedChoiceType(
         "type",
         DSL.string(),
         ImmutableMap.of(
            "minecraft:debug",
            DSL.remainderType(),
            "minecraft:flat",
            flatType($$0),
            "minecraft:noise",
            optionalFields(
               "biome_source",
               DSL.taggedChoiceType(
                  "type",
                  DSL.string(),
                  ImmutableMap.of(
                     "minecraft:fixed",
                     fields("biome", $$0.getType(References.BIOME)),
                     "minecraft:multi_noise",
                     DSL.list(fields("biome", $$0.getType(References.BIOME))),
                     "minecraft:checkerboard",
                     fields("biomes", DSL.list($$0.getType(References.BIOME))),
                     "minecraft:vanilla_layered",
                     DSL.remainderType(),
                     "minecraft:the_end",
                     DSL.remainderType()
                  )
               ),
               "settings",
               DSL.or(DSL.string(), optionalFields("default_block", $$0.getType(References.BLOCK_NAME), "default_fluid", $$0.getType(References.BLOCK_NAME)))
            )
         )
      );
      CompoundListType<String, ?> $$2 = DSL.compoundList(NamespacedSchema.namespacedString(), fields("generator", $$1));
      Type<?> $$3 = DSL.and($$2, DSL.remainderType());
      Type<?> $$4 = $$0.getType(References.WORLD_GEN_SETTINGS);
      FieldFinder<?> $$5 = new FieldFinder("dimensions", $$3);
      if (!$$4.findFieldType("dimensions").equals($$3)) {
         throw new IllegalStateException();
      } else {
         OpticFinder<? extends List<? extends Pair<String, ?>>> $$6 = $$2.finder();
         return this.fixTypeEverywhereTyped(
            "MissingDimensionFix", $$4, p_16426_ -> p_16426_.updateTyped($$5, p_145517_ -> p_145517_.updateTyped($$6, p_326611_ -> {
                     if (!(p_326611_.getValue() instanceof List)) {
                        throw new IllegalStateException("List exptected");
                     } else if (((List)p_326611_.getValue()).isEmpty()) {
                        Dynamic<?> $$3x = (Dynamic<?>)p_16426_.get(DSL.remainderFinder());
                        Dynamic<?> $$4x = this.recreateSettings($$3x);
                        return (Typed)DataFixUtils.orElse($$2.readTyped($$4x).result().map(Pair::getFirst), p_326611_);
                     } else {
                        return p_326611_;
                     }
                  }))
         );
      }
   }

   protected static Type<? extends Pair<? extends Either<? extends Pair<? extends Either<?, Unit>, ? extends Pair<? extends Either<? extends List<? extends Pair<? extends Either<?, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>> flatType(
      Schema pSchema
   ) {
      return optionalFields(
         "settings",
         optionalFields("biome", pSchema.getType(References.BIOME), "layers", DSL.list(optionalFields("block", pSchema.getType(References.BLOCK_NAME))))
      );
   }

   private <T> Dynamic<T> recreateSettings(Dynamic<T> pDynamic) {
      long $$1 = pDynamic.get("seed").asLong(0L);
      return new Dynamic(pDynamic.getOps(), WorldGenSettingsFix.vanillaLevels(pDynamic, $$1, WorldGenSettingsFix.defaultOverworld(pDynamic, $$1), false));
   }
}
