package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public abstract class SimpleEntityRenameFix extends EntityRenameFix {
   public SimpleEntityRenameFix(String pName, Schema pOutputSchema, boolean pChangesType) {
      super(pName, pOutputSchema, pChangesType);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
      Pair<String, Dynamic<?>> $$2 = this.getNewNameAndTag(pEntityName, (Dynamic<?>)pTyped.getOrCreate(DSL.remainderFinder()));
      return Pair.of((String)$$2.getFirst(), pTyped.set(DSL.remainderFinder(), (Dynamic)$$2.getSecond()));
   }

   protected abstract Pair<String, Dynamic<?>> getNewNameAndTag(String pName, Dynamic<?> pTag);
}
