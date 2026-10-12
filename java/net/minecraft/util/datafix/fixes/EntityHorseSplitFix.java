package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.Util;

public class EntityHorseSplitFix extends EntityRenameFix {
   public EntityHorseSplitFix(Schema pOutputSchema, boolean pChangesType) {
      super("EntityHorseSplitFix", pOutputSchema, pChangesType);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
      if (Objects.equals("EntityHorse", pEntityName)) {
         Dynamic<?> $$2 = (Dynamic<?>)pTyped.get(DSL.remainderFinder());
         int $$3 = $$2.get("Type").asInt(0);

         String $$4 = switch ($$3) {
            case 1 -> "Donkey";
            case 2 -> "Mule";
            case 3 -> "ZombieHorse";
            case 4 -> "SkeletonHorse";
            default -> "Horse";
         };
         Type<?> $$5 = (Type<?>)this.getOutputSchema().findChoiceType(References.ENTITY).types().get($$4);
         return Pair.of($$4, Util.writeAndReadTypedOrThrow(pTyped, $$5, p_326575_ -> p_326575_.remove("Type")));
      } else {
         return Pair.of(pEntityName, pTyped);
      }
   }
}
