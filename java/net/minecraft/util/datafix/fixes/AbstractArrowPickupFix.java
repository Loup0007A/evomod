package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;

public class AbstractArrowPickupFix extends DataFix {
   public AbstractArrowPickupFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Schema $$0 = this.getInputSchema();
      return this.fixTypeEverywhereTyped("AbstractArrowPickupFix", $$0.getType(References.ENTITY), this::updateProjectiles);
   }

   private Typed<?> updateProjectiles(Typed<?> p_145048_) {
      p_145048_ = this.updateEntity(p_145048_, "minecraft:arrow", AbstractArrowPickupFix::updatePickup);
      p_145048_ = this.updateEntity(p_145048_, "minecraft:spectral_arrow", AbstractArrowPickupFix::updatePickup);
      return this.updateEntity(p_145048_, "minecraft:trident", AbstractArrowPickupFix::updatePickup);
   }

   private static Dynamic<?> updatePickup(Dynamic<?> p_145054_) {
      if (p_145054_.get("pickup").result().isPresent()) {
         return p_145054_;
      } else {
         boolean $$1 = p_145054_.get("player").asBoolean(true);
         return p_145054_.set("pickup", p_145054_.createByte((byte)($$1 ? 1 : 0))).remove("player");
      }
   }

   private Typed<?> updateEntity(Typed<?> pTyped, String pChoiceName, Function<Dynamic<?>, Dynamic<?>> pUpdater) {
      Type<?> $$3 = this.getInputSchema().getChoiceType(References.ENTITY, pChoiceName);
      Type<?> $$4 = this.getOutputSchema().getChoiceType(References.ENTITY, pChoiceName);
      return pTyped.updateTyped(DSL.namedChoice(pChoiceName, $$3), $$4, p_145057_ -> p_145057_.update(DSL.remainderFinder(), pUpdater));
   }
}
