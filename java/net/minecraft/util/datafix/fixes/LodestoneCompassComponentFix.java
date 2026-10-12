package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class LodestoneCompassComponentFix extends ItemStackComponentRemainderFix {
   public LodestoneCompassComponentFix(Schema pOutputSchema) {
      super(pOutputSchema, "LodestoneCompassComponentFix", "minecraft:lodestone_target", "minecraft:lodestone_tracker");
   }

   @Override
   protected <T> Dynamic<T> fixComponent(Dynamic<T> pTag) {
      Optional<Dynamic<T>> $$1 = pTag.get("pos").result();
      Optional<Dynamic<T>> $$2 = pTag.get("dimension").result();
      pTag = pTag.remove("pos").remove("dimension");
      if ($$1.isPresent() && $$2.isPresent()) {
         pTag = pTag.set("target", pTag.emptyMap().set("pos", $$1.get()).set("dimension", $$2.get()));
      }

      return pTag;
   }
}
