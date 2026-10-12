package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class OminousBannerRenameFix extends ItemStackTagFix {
   public OminousBannerRenameFix(Schema pSchema) {
      super(pSchema, "OminousBannerRenameFix", p_216698_ -> p_216698_.equals("minecraft:white_banner"));
   }

   @Override
   protected <T> Dynamic<T> fixItemStackTag(Dynamic<T> pItemStackTag) {
      Optional<? extends Dynamic<?>> $$1 = pItemStackTag.get("display").result();
      if ($$1.isPresent()) {
         Dynamic<?> $$2 = (Dynamic<?>)$$1.get();
         Optional<String> $$3 = $$2.get("Name").asString().result();
         if ($$3.isPresent()) {
            String $$4 = $$3.get();
            $$4 = $$4.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"");
            $$2 = $$2.set("Name", $$2.createString($$4));
         }

         return pItemStackTag.set("display", $$2);
      } else {
         return pItemStackTag;
      }
   }
}
