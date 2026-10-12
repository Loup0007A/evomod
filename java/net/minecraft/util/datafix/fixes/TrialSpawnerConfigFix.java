package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TrialSpawnerConfigFix extends NamedEntityWriteReadFix {
   public TrialSpawnerConfigFix(Schema pOutputSchema) {
      super(pOutputSchema, true, "Trial Spawner config tag fixer", References.BLOCK_ENTITY, "minecraft:trial_spawner");
   }

   private static <T> Dynamic<T> moveToConfigTag(Dynamic<T> pData) {
      List<String> $$1 = List.of(
         "spawn_range",
         "total_mobs",
         "simultaneous_mobs",
         "total_mobs_added_per_player",
         "simultaneous_mobs_added_per_player",
         "ticks_between_spawn",
         "spawn_potentials",
         "loot_tables_to_eject",
         "items_to_drop_when_ominous"
      );
      Map<Dynamic<T>, Dynamic<T>> $$2 = new HashMap<>($$1.size());

      for (String $$3 : $$1) {
         Optional<Dynamic<T>> $$4 = pData.get($$3).get().result();
         if ($$4.isPresent()) {
            $$2.put(pData.createString($$3), $$4.get());
            pData = pData.remove($$3);
         }
      }

      return $$2.isEmpty() ? pData : pData.set("normal_config", pData.createMap($$2));
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> pTag) {
      return moveToConfigTag(pTag);
   }
}
