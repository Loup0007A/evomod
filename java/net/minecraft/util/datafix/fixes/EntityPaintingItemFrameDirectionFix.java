package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class EntityPaintingItemFrameDirectionFix extends DataFix {
   private static final int[][] DIRECTIONS = new int[][]{{0, 0, 1}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}};

   public EntityPaintingItemFrameDirectionFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   private Dynamic<?> doFix(Dynamic<?> pDynamic, boolean pFixDirection, boolean pFixItemRotation) {
      if ((pFixDirection || pFixItemRotation) && pDynamic.get("Facing").asNumber().result().isEmpty()) {
         int $$3;
         if (pDynamic.get("Direction").asNumber().result().isPresent()) {
            $$3 = pDynamic.get("Direction").asByte((byte)0) % DIRECTIONS.length;
            int[] $$4 = DIRECTIONS[$$3];
            pDynamic = pDynamic.set("TileX", pDynamic.createInt(pDynamic.get("TileX").asInt(0) + $$4[0]));
            pDynamic = pDynamic.set("TileY", pDynamic.createInt(pDynamic.get("TileY").asInt(0) + $$4[1]));
            pDynamic = pDynamic.set("TileZ", pDynamic.createInt(pDynamic.get("TileZ").asInt(0) + $$4[2]));
            pDynamic = pDynamic.remove("Direction");
            if (pFixItemRotation && pDynamic.get("ItemRotation").asNumber().result().isPresent()) {
               pDynamic = pDynamic.set("ItemRotation", pDynamic.createByte((byte)(pDynamic.get("ItemRotation").asByte((byte)0) * 2)));
            }
         } else {
            $$3 = pDynamic.get("Dir").asByte((byte)0) % DIRECTIONS.length;
            pDynamic = pDynamic.remove("Dir");
         }

         pDynamic = pDynamic.set("Facing", pDynamic.createByte((byte)$$3));
      }

      return pDynamic;
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getChoiceType(References.ENTITY, "Painting");
      OpticFinder<?> $$1 = DSL.namedChoice("Painting", $$0);
      Type<?> $$2 = this.getInputSchema().getChoiceType(References.ENTITY, "ItemFrame");
      OpticFinder<?> $$3 = DSL.namedChoice("ItemFrame", $$2);
      Type<?> $$4 = this.getInputSchema().getType(References.ENTITY);
      TypeRewriteRule $$5 = this.fixTypeEverywhereTyped(
         "EntityPaintingFix",
         $$4,
         p_15516_ -> p_15516_.updateTyped($$1, $$0, p_145300_ -> p_145300_.update(DSL.remainderFinder(), p_145302_ -> this.doFix(p_145302_, true, false)))
      );
      TypeRewriteRule $$6 = this.fixTypeEverywhereTyped(
         "EntityItemFrameFix",
         $$4,
         p_15504_ -> p_15504_.updateTyped($$3, $$2, p_145296_ -> p_145296_.update(DSL.remainderFinder(), p_145298_ -> this.doFix(p_145298_, false, true)))
      );
      return TypeRewriteRule.seq($$5, $$6);
   }
}
