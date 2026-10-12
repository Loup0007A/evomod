package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

public class ChunkRenamesFix extends DataFix {
   public ChunkRenamesFix(Schema pOutputSchema) {
      super(pOutputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> $$1 = $$0.findField("Level");
      OpticFinder<?> $$2 = $$1.type().findField("Structures");
      Type<?> $$3 = this.getOutputSchema().getType(References.CHUNK);
      Type<?> $$4 = $$3.findFieldType("structures");
      return this.fixTypeEverywhereTyped("Chunk Renames; purge Level-tag", $$0, $$3, p_199427_ -> {
         Typed<?> $$4x = p_199427_.getTyped($$1);
         Typed<?> $$5 = appendChunkName($$4x);
         $$5 = $$5.set(DSL.remainderFinder(), mergeRemainders(p_199427_, (Dynamic)$$4x.get(DSL.remainderFinder())));
         $$5 = renameField($$5, "TileEntities", "block_entities");
         $$5 = renameField($$5, "TileTicks", "block_ticks");
         $$5 = renameField($$5, "Entities", "entities");
         $$5 = renameField($$5, "Sections", "sections");
         $$5 = $$5.updateTyped($$2, $$4, p_185128_ -> renameField(p_185128_, "Starts", "starts"));
         $$5 = renameField($$5, "Structures", "structures");
         return $$5.update(DSL.remainderFinder(), p_199429_ -> p_199429_.remove("Level"));
      });
   }

   private static Typed<?> renameField(Typed<?> pTyped, String pOldName, String pNewName) {
      return renameFieldHelper(pTyped, pOldName, pNewName, pTyped.getType().findFieldType(pOldName))
         .update(DSL.remainderFinder(), p_199439_ -> p_199439_.remove(pOldName));
   }

   private static <A> Typed<?> renameFieldHelper(Typed<?> pTyped, String pOldName, String pNewName, Type<A> pType) {
      Type<Either<A, Unit>> $$4 = DSL.optional(DSL.field(pOldName, pType));
      Type<Either<A, Unit>> $$5 = DSL.optional(DSL.field(pNewName, pType));
      return pTyped.update($$4.finder(), $$5, Function.identity());
   }

   private static <A> Typed<Pair<String, A>> appendChunkName(Typed<A> pTyped) {
      return new Typed(DSL.named("chunk", pTyped.getType()), pTyped.getOps(), Pair.of("chunk", pTyped.getValue()));
   }

   private static <T> Dynamic<T> mergeRemainders(Typed<?> pTyped, Dynamic<T> pDynamic) {
      DynamicOps<T> $$2 = pDynamic.getOps();
      Dynamic<T> $$3 = ((Dynamic)pTyped.get(DSL.remainderFinder())).convert($$2);
      DataResult<T> $$4 = $$2.getMap(pDynamic.getValue()).flatMap(p_199433_ -> $$2.mergeToMap($$3.getValue(), p_199433_));
      return $$4.result().map(p_199436_ -> new Dynamic($$2, p_199436_)).orElse(pDynamic);
   }
}
