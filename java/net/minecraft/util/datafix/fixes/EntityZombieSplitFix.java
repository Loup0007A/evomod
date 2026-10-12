package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.function.Supplier;
import net.minecraft.Util;

public class EntityZombieSplitFix extends EntityRenameFix {
   private final Supplier<Type<?>> zombieVillagerType = Suppliers.memoize(() -> this.getOutputSchema().getChoiceType(References.ENTITY, "ZombieVillager"));

   public EntityZombieSplitFix(Schema pOutputSchema) {
      super("EntityZombieSplitFix", pOutputSchema, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
      if (!pEntityName.equals("Zombie")) {
         return Pair.of(pEntityName, pTyped);
      } else {
         Dynamic<?> $$2 = (Dynamic<?>)pTyped.getOptional(DSL.remainderFinder()).orElseThrow();
         int $$3 = $$2.get("ZombieType").asInt(0);
         String $$4;
         Typed<?> $$5;
         switch ($$3) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
               $$4 = "ZombieVillager";
               $$5 = this.changeSchemaToZombieVillager(pTyped, $$3 - 1);
               break;
            case 6:
               $$4 = "Husk";
               $$5 = pTyped;
               break;
            default:
               $$4 = "Zombie";
               $$5 = pTyped;
         }

         return Pair.of($$4, $$5.update(DSL.remainderFinder(), p_333056_ -> p_333056_.remove("ZombieType")));
      }
   }

   private Typed<?> changeSchemaToZombieVillager(Typed<?> pTyped, int pProfession) {
      return Util.writeAndReadTypedOrThrow(pTyped, this.zombieVillagerType.get(), p_329329_ -> p_329329_.set("Profession", p_329329_.createInt(pProfession)));
   }
}
