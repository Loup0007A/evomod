package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;

public class ChestedHorsesInventoryZeroIndexingFix extends DataFix {
   public ChestedHorsesInventoryZeroIndexingFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> $$0 = DSL.typeFinder(
         this.getInputSchema().getType(References.ITEM_STACK)
      );
      Type<?> $$1 = this.getInputSchema().getType(References.ENTITY);
      return TypeRewriteRule.seq(
         this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:llama"),
         new TypeRewriteRule[]{
            this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:trader_llama"),
            this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:mule"),
            this.horseLikeInventoryIndexingFixer($$0, $$1, "minecraft:donkey")
         }
      );
   }

   private TypeRewriteRule horseLikeInventoryIndexingFixer(
      OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>> pOpticFinder, Type<?> pType, String pEntityId
   ) {
      Type<?> $$3 = this.getInputSchema().getChoiceType(References.ENTITY, pEntityId);
      OpticFinder<?> $$4 = DSL.namedChoice(pEntityId, $$3);
      OpticFinder<?> $$5 = $$3.findField("Items");
      return this.fixTypeEverywhereTyped(
         "Fix non-zero indexing in chest horse type " + pEntityId,
         pType,
         p_333304_ -> p_333304_.updateTyped(
               $$4,
               p_334500_ -> p_334500_.updateTyped(
                     $$5,
                     p_328165_ -> p_328165_.update(
                           pOpticFinder,
                           p_334814_ -> p_334814_.mapSecond(
                                 p_335553_ -> p_335553_.mapSecond(
                                       p_330261_ -> p_330261_.mapSecond(
                                             p_334966_ -> p_334966_.update("Slot", p_333657_ -> p_333657_.createByte((byte)(p_333657_.asInt(2) - 2)))
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }
}
