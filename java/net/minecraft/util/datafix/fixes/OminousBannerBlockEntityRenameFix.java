package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class OminousBannerBlockEntityRenameFix extends NamedEntityFix {
   public OminousBannerBlockEntityRenameFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType, "OminousBannerBlockEntityRenameFix", References.BLOCK_ENTITY, "minecraft:banner");
   }

   @Override
   protected Typed<?> fix(Typed<?> pTyped) {
      return pTyped.update(DSL.remainderFinder(), this::fixTag);
   }

   private Dynamic<?> fixTag(Dynamic<?> p_16553_) {
      Optional<String> $$1 = p_16553_.get("CustomName").asString().result();
      if ($$1.isPresent()) {
         String $$2 = $$1.get();
         $$2 = $$2.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"");
         return p_16553_.set("CustomName", p_16553_.createString($$2));
      } else {
         return p_16553_;
      }
   }
}
