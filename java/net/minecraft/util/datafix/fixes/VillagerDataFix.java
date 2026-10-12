package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class VillagerDataFix extends NamedEntityFix {
   public VillagerDataFix(Schema pOutputSchema, String pEntityName) {
      super(pOutputSchema, false, "Villager profession data fix (" + pEntityName + ")", References.ENTITY, pEntityName);
   }

   @Override
   protected Typed<?> fix(Typed<?> pTyped) {
      Dynamic<?> $$1 = (Dynamic<?>)pTyped.get(DSL.remainderFinder());
      return pTyped.set(
         DSL.remainderFinder(),
         $$1.remove("Profession")
            .remove("Career")
            .remove("CareerLevel")
            .set(
               "VillagerData",
               $$1.createMap(
                  ImmutableMap.of(
                     $$1.createString("type"),
                     $$1.createString("minecraft:plains"),
                     $$1.createString("profession"),
                     $$1.createString(upgradeData($$1.get("Profession").asInt(0), $$1.get("Career").asInt(0))),
                     $$1.createString("level"),
                     (Dynamic)DataFixUtils.orElse($$1.get("CareerLevel").result(), $$1.createInt(1))
                  )
               )
            )
      );
   }

   private static String upgradeData(int pProfession, int pCareer) {
      if (pProfession == 0) {
         if (pCareer == 2) {
            return "minecraft:fisherman";
         } else if (pCareer == 3) {
            return "minecraft:shepherd";
         } else {
            return pCareer == 4 ? "minecraft:fletcher" : "minecraft:farmer";
         }
      } else if (pProfession == 1) {
         return pCareer == 2 ? "minecraft:cartographer" : "minecraft:librarian";
      } else if (pProfession == 2) {
         return "minecraft:cleric";
      } else if (pProfession == 3) {
         if (pCareer == 2) {
            return "minecraft:weaponsmith";
         } else {
            return pCareer == 3 ? "minecraft:toolsmith" : "minecraft:armorer";
         }
      } else if (pProfession == 4) {
         return pCareer == 2 ? "minecraft:leatherworker" : "minecraft:butcher";
      } else {
         return pProfession == 5 ? "minecraft:nitwit" : "minecraft:none";
      }
   }
}
