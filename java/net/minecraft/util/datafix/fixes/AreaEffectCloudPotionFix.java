package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class AreaEffectCloudPotionFix extends NamedEntityFix {
   public AreaEffectCloudPotionFix(Schema pOutputSchema) {
      super(pOutputSchema, false, "AreaEffectCloudPotionFix", References.ENTITY, "minecraft:area_effect_cloud");
   }

   @Override
   protected Typed<?> fix(Typed<?> pTyped) {
      return pTyped.update(DSL.remainderFinder(), this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_332584_) {
      Optional<Dynamic<T>> $$1 = p_332584_.get("Color").result();
      Optional<Dynamic<T>> $$2 = p_332584_.get("effects").result();
      Optional<Dynamic<T>> $$3 = p_332584_.get("Potion").result();
      p_332584_ = p_332584_.remove("Color").remove("effects").remove("Potion");
      if ($$1.isEmpty() && $$2.isEmpty() && $$3.isEmpty()) {
         return p_332584_;
      } else {
         Dynamic<T> $$4 = p_332584_.emptyMap();
         if ($$1.isPresent()) {
            $$4 = $$4.set("custom_color", $$1.get());
         }

         if ($$2.isPresent()) {
            $$4 = $$4.set("custom_effects", $$2.get());
         }

         if ($$3.isPresent()) {
            $$4 = $$4.set("potion", $$3.get());
         }

         return p_332584_.set("potion_contents", $$4);
      }
   }
}
