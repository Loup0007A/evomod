package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;

public class MobSpawnerEntityIdentifiersFix extends DataFix {
   public MobSpawnerEntityIdentifiersFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   private Dynamic<?> fix(Dynamic<?> pDynamic) {
      if (!"MobSpawner".equals(pDynamic.get("id").asString(""))) {
         return pDynamic;
      } else {
         Optional<String> $$1 = pDynamic.get("EntityId").asString().result();
         if ($$1.isPresent()) {
            Dynamic<?> $$2 = (Dynamic<?>)DataFixUtils.orElse(pDynamic.get("SpawnData").result(), pDynamic.emptyMap());
            $$2 = $$2.set("id", $$2.createString($$1.get().isEmpty() ? "Pig" : $$1.get()));
            pDynamic = pDynamic.set("SpawnData", $$2);
            pDynamic = pDynamic.remove("EntityId");
         }

         Optional<? extends Stream<? extends Dynamic<?>>> $$3 = pDynamic.get("SpawnPotentials").asStreamOpt().result();
         if ($$3.isPresent()) {
            pDynamic = pDynamic.set(
               "SpawnPotentials",
               pDynamic.createList(
                  $$3.get()
                     .map(
                        p_326614_ -> {
                           Optional<String> $$1x = p_326614_.get("Type").asString().result();
                           if ($$1x.isPresent()) {
                              Dynamic<?> $$2 = ((Dynamic)DataFixUtils.orElse(p_326614_.get("Properties").result(), p_326614_.emptyMap()))
                                 .set("id", p_326614_.createString($$1x.get()));
                              return p_326614_.set("Entity", $$2).remove("Type").remove("Properties");
                           } else {
                              return (Dynamic)p_326614_;
                           }
                        }
                     )
               )
            );
         }

         return pDynamic;
      }
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
      return this.fixTypeEverywhereTyped("MobSpawnerEntityIdentifiersFix", this.getInputSchema().getType(References.UNTAGGED_SPAWNER), $$0, p_326613_ -> {
         Dynamic<?> $$2 = (Dynamic<?>)p_326613_.get(DSL.remainderFinder());
         $$2 = $$2.set("id", $$2.createString("MobSpawner"));
         DataResult<? extends Pair<? extends Typed<?>, ?>> $$3 = $$0.readTyped(this.fix($$2));
         return $$3.result().isEmpty() ? p_326613_ : (Typed)((Pair)$$3.result().get()).getFirst();
      });
   }
}
