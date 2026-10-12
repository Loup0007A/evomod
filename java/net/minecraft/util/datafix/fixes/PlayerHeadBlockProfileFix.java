package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class PlayerHeadBlockProfileFix extends NamedEntityFix {
   public PlayerHeadBlockProfileFix(Schema pOutputSchema) {
      super(pOutputSchema, false, "PlayerHeadBlockProfileFix", References.BLOCK_ENTITY, "minecraft:skull");
   }

   @Override
   protected Typed<?> fix(Typed<?> pTyped) {
      return pTyped.update(DSL.remainderFinder(), this::fix);
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_332985_) {
      Optional<Dynamic<T>> $$1 = p_332985_.get("SkullOwner").result();
      Optional<Dynamic<T>> $$2 = p_332985_.get("ExtraType").result();
      Optional<Dynamic<T>> $$3 = $$1.or(() -> $$2);
      if ($$3.isEmpty()) {
         return p_332985_;
      } else {
         p_332985_ = p_332985_.remove("SkullOwner").remove("ExtraType");
         return p_332985_.set("profile", ItemStackComponentizationFix.fixProfile($$3.get()));
      }
   }
}
