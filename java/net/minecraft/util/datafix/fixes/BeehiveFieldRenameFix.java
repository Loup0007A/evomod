package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class BeehiveFieldRenameFix extends DataFix {
   public BeehiveFieldRenameFix(Schema pOutputSchema) {
      super(pOutputSchema, true);
   }

   private Dynamic<?> fixBeehive(Dynamic<?> p_334771_) {
      return p_334771_.remove("Bees");
   }

   private Dynamic<?> fixBee(Dynamic<?> p_335732_) {
      p_335732_ = p_335732_.remove("EntityData");
      p_335732_ = p_335732_.renameField("TicksInHive", "ticks_in_hive");
      return p_335732_.renameField("MinOccupationTicks", "min_ticks_in_hive");
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:beehive");
      OpticFinder<?> $$1 = DSL.namedChoice("minecraft:beehive", $$0);
      ListType<?> $$2 = (ListType<?>)$$0.findFieldType("Bees");
      Type<?> $$3 = $$2.getElement();
      OpticFinder<?> $$4 = DSL.fieldFinder("Bees", $$2);
      OpticFinder<?> $$5 = DSL.typeFinder($$3);
      Type<?> $$6 = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> $$7 = this.getOutputSchema().getType(References.BLOCK_ENTITY);
      return this.fixTypeEverywhereTyped(
         "BeehiveFieldRenameFix",
         $$6,
         $$7,
         p_336050_ -> ExtraDataFixUtils.cast(
               $$7,
               p_336050_.updateTyped(
                  $$1,
                  p_334762_ -> p_334762_.update(DSL.remainderFinder(), this::fixBeehive)
                        .updateTyped($$4, p_334908_ -> p_334908_.updateTyped($$5, p_331823_ -> p_331823_.update(DSL.remainderFinder(), this::fixBee)))
               )
            )
      );
   }
}
