package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;

public class BlendingDataRemoveFromNetherEndFix extends DataFix {
   public BlendingDataRemoveFromNetherEndFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getOutputSchema().getType(References.CHUNK);
      return this.fixTypeEverywhereTyped(
         "BlendingDataRemoveFromNetherEndFix",
         $$0,
         p_240286_ -> p_240286_.update(DSL.remainderFinder(), p_240254_ -> updateChunkTag(p_240254_, p_240254_.get("__context")))
      );
   }

   private static Dynamic<?> updateChunkTag(Dynamic<?> pChunkTag, OptionalDynamic<?> pContext) {
      boolean $$2 = "minecraft:overworld".equals(pContext.get("dimension").asString().result().orElse(""));
      return $$2 ? pChunkTag : pChunkTag.remove("blending_data");
   }
}
