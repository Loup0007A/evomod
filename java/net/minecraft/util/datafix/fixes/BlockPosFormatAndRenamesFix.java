package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class BlockPosFormatAndRenamesFix extends DataFix {
   private static final List<String> PATROLLING_MOBS = List.of(
      "minecraft:witch", "minecraft:ravager", "minecraft:pillager", "minecraft:illusioner", "minecraft:evoker", "minecraft:vindicator"
   );

   public BlockPosFormatAndRenamesFix(Schema pOutputSchema) {
      super(pOutputSchema, false);
   }

   private Typed<?> fixFields(Typed<?> pData, Map<String, String> pRenames) {
      return pData.update(DSL.remainderFinder(), p_333105_ -> {
         for (Entry<String, String> $$2 : pRenames.entrySet()) {
            p_333105_ = p_333105_.renameAndFixField($$2.getKey(), $$2.getValue(), ExtraDataFixUtils::fixBlockPos);
         }

         return p_333105_;
      });
   }

   private <T> Dynamic<T> fixMapSavedData(Dynamic<T> p_328964_) {
      return p_328964_.update("frames", p_334922_ -> p_334922_.createList(p_334922_.asStream().map(p_334081_ -> {
            p_334081_ = p_334081_.renameAndFixField("Pos", "pos", ExtraDataFixUtils::fixBlockPos);
            p_334081_ = p_334081_.renameField("Rotation", "rotation");
            return p_334081_.renameField("EntityId", "entity_id");
         }))).update("banners", p_332873_ -> p_332873_.createList(p_332873_.asStream().map(p_333148_ -> {
            p_333148_ = p_333148_.renameField("Pos", "pos");
            p_333148_ = p_333148_.renameField("Color", "color");
            return p_333148_.renameField("Name", "name");
         })));
   }

   public TypeRewriteRule makeRule() {
      List<TypeRewriteRule> $$0 = new ArrayList<>();
      this.addEntityRules($$0);
      this.addBlockEntityRules($$0);
      $$0.add(
         this.fixTypeEverywhereTyped(
            "BlockPos format for map frames",
            this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA),
            p_332459_ -> p_332459_.update(DSL.remainderFinder(), p_335523_ -> p_335523_.update("data", this::fixMapSavedData))
         )
      );
      Type<?> $$1 = this.getInputSchema().getType(References.ITEM_STACK);
      $$0.add(
         this.fixTypeEverywhereTyped(
            "BlockPos format for compass target",
            $$1,
            ItemStackTagFix.createFixer($$1, "minecraft:compass"::equals, p_330014_ -> p_330014_.update("LodestonePos", ExtraDataFixUtils::fixBlockPos))
         )
      );
      return TypeRewriteRule.seq($$0);
   }

   private void addEntityRules(List<TypeRewriteRule> pOutput) {
      pOutput.add(this.createEntityFixer(References.ENTITY, "minecraft:bee", Map.of("HivePos", "hive_pos", "FlowerPos", "flower_pos")));
      pOutput.add(this.createEntityFixer(References.ENTITY, "minecraft:end_crystal", Map.of("BeamTarget", "beam_target")));
      pOutput.add(this.createEntityFixer(References.ENTITY, "minecraft:wandering_trader", Map.of("WanderTarget", "wander_target")));

      for (String $$1 : PATROLLING_MOBS) {
         pOutput.add(this.createEntityFixer(References.ENTITY, $$1, Map.of("PatrolTarget", "patrol_target")));
      }

      pOutput.add(
         this.fixTypeEverywhereTyped(
            "BlockPos format in Leash for mobs",
            this.getInputSchema().getType(References.ENTITY),
            p_332531_ -> p_332531_.update(DSL.remainderFinder(), p_333406_ -> p_333406_.renameAndFixField("Leash", "leash", ExtraDataFixUtils::fixBlockPos))
         )
      );
   }

   private void addBlockEntityRules(List<TypeRewriteRule> pOutput) {
      pOutput.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:beehive", Map.of("FlowerPos", "flower_pos")));
      pOutput.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:end_gateway", Map.of("ExitPortal", "exit_portal")));
   }

   private TypeRewriteRule createEntityFixer(TypeReference pReference, String pEntityId, Map<String, String> pRenames) {
      String $$3 = "BlockPos format in " + pRenames.keySet() + " for " + pEntityId + " (" + pReference.typeName() + ")";
      OpticFinder<?> $$4 = DSL.namedChoice(pEntityId, this.getInputSchema().getChoiceType(pReference, pEntityId));
      return this.fixTypeEverywhereTyped(
         $$3, this.getInputSchema().getType(pReference), p_329758_ -> p_329758_.updateTyped($$4, p_336142_ -> this.fixFields(p_336142_, pRenames))
      );
   }
}
