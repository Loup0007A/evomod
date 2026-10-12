package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class EntityCatSplitFix extends SimpleEntityRenameFix {
   public EntityCatSplitFix(Schema pOutputSchema, boolean pChangesType) {
      super("EntityCatSplitFix", pOutputSchema, pChangesType);
   }

   @Override
   protected Pair<String, Dynamic<?>> getNewNameAndTag(String pName, Dynamic<?> pTag) {
      if (Objects.equals("minecraft:ocelot", pName)) {
         int $$2 = pTag.get("CatType").asInt(0);
         if ($$2 == 0) {
            String $$3 = pTag.get("Owner").asString("");
            String $$4 = pTag.get("OwnerUUID").asString("");
            if ($$3.length() > 0 || $$4.length() > 0) {
               pTag.set("Trusting", pTag.createBoolean(true));
            }
         } else if ($$2 > 0 && $$2 < 4) {
            pTag = pTag.set("CatType", pTag.createInt($$2));
            pTag = pTag.set("OwnerUUID", pTag.createString(pTag.get("OwnerUUID").asString("")));
            return Pair.of("minecraft:cat", pTag);
         }
      }

      return Pair.of(pName, pTag);
   }
}
