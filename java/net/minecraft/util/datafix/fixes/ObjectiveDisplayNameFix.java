package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class ObjectiveDisplayNameFix extends DataFix {
   public ObjectiveDisplayNameFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.OBJECTIVE);
      return this.fixTypeEverywhereTyped(
         "ObjectiveDisplayNameFix",
         $$0,
         p_181039_ -> p_181039_.update(DSL.remainderFinder(), p_308994_ -> p_308994_.update("DisplayName", ComponentDataFixUtils::wrapLiteralStringAsComponent))
      );
   }
}
