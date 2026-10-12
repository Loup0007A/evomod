package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Optional;
import java.util.UUID;

public class EntityStringUuidFix extends DataFix {
   public EntityStringUuidFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "EntityStringUuidFix",
         this.getInputSchema().getType(References.ENTITY),
         p_15697_ -> p_15697_.update(
               DSL.remainderFinder(),
               p_326577_ -> {
                  Optional<String> $$1 = p_326577_.get("UUID").asString().result();
                  if ($$1.isPresent()) {
                     UUID $$2 = UUID.fromString($$1.get());
                     return p_326577_.remove("UUID")
                        .set("UUIDMost", p_326577_.createLong($$2.getMostSignificantBits()))
                        .set("UUIDLeast", p_326577_.createLong($$2.getLeastSignificantBits()));
                  } else {
                     return p_326577_;
                  }
               }
            )
      );
   }
}
