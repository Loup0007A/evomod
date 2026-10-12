package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;

public class LevelLegacyWorldGenSettingsFix extends DataFix {
   private static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
   private static final List<String> OLD_SETTINGS_KEYS = List.of(
      "RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest"
   );

   public LevelLegacyWorldGenSettingsFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LevelLegacyWorldGenSettingsFix", this.getInputSchema().getType(References.LEVEL), p_311579_ -> p_311579_.update(DSL.remainderFinder(), p_309406_ -> {
               Dynamic<?> $$1 = p_309406_.get("WorldGenSettings").orElseEmptyMap();

               for (String $$2 : OLD_SETTINGS_KEYS) {
                  Optional<? extends Dynamic<?>> $$3 = p_309406_.get($$2).result();
                  if ($$3.isPresent()) {
                     p_309406_ = p_309406_.remove($$2);
                     $$1 = $$1.set($$2, $$3.get());
                  }
               }

               return p_309406_.set("WorldGenSettings", $$1);
            })
      );
   }
}
