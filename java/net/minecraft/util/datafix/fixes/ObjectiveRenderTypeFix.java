package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.Optional;

public class ObjectiveRenderTypeFix extends DataFix {
   public ObjectiveRenderTypeFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   private static String getRenderType(String pOldRenderType) {
      return pOldRenderType.equals("health") ? "hearts" : "integer";
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.OBJECTIVE);
      return this.fixTypeEverywhereTyped("ObjectiveRenderTypeFix", $$0, p_181041_ -> p_181041_.update(DSL.remainderFinder(), p_326632_ -> {
            Optional<String> $$1 = p_326632_.get("RenderType").asString().result();
            if ($$1.isEmpty()) {
               String $$2 = p_326632_.get("CriteriaName").asString("");
               String $$3 = getRenderType($$2);
               return p_326632_.set("RenderType", p_326632_.createString($$3));
            } else {
               return p_326632_;
            }
         }));
   }
}
