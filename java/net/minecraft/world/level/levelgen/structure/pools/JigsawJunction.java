package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

public class JigsawJunction {
   private final int sourceX;
   private final int sourceGroundY;
   private final int sourceZ;
   private final int deltaY;
   private final StructureTemplatePool.Projection destProjection;

   public JigsawJunction(int pSourceX, int pSourceGroundY, int pSourceZ, int pDeltaY, StructureTemplatePool.Projection pDestProjection) {
      this.sourceX = pSourceX;
      this.sourceGroundY = pSourceGroundY;
      this.sourceZ = pSourceZ;
      this.deltaY = pDeltaY;
      this.destProjection = pDestProjection;
   }

   public int getSourceX() {
      return this.sourceX;
   }

   public int getSourceGroundY() {
      return this.sourceGroundY;
   }

   public int getSourceZ() {
      return this.sourceZ;
   }

   public int getDeltaY() {
      return this.deltaY;
   }

   public StructureTemplatePool.Projection getDestProjection() {
      return this.destProjection;
   }

   public <T> Dynamic<T> serialize(DynamicOps<T> pOps) {
      Builder<T, T> $$1 = ImmutableMap.builder();
      $$1.put(pOps.createString("source_x"), pOps.createInt(this.sourceX))
         .put(pOps.createString("source_ground_y"), pOps.createInt(this.sourceGroundY))
         .put(pOps.createString("source_z"), pOps.createInt(this.sourceZ))
         .put(pOps.createString("delta_y"), pOps.createInt(this.deltaY))
         .put(pOps.createString("dest_proj"), pOps.createString(this.destProjection.getName()));
      return new Dynamic(pOps, pOps.createMap($$1.build()));
   }

   public static <T> JigsawJunction deserialize(Dynamic<T> pDynamic) {
      return new JigsawJunction(
         pDynamic.get("source_x").asInt(0),
         pDynamic.get("source_ground_y").asInt(0),
         pDynamic.get("source_z").asInt(0),
         pDynamic.get("delta_y").asInt(0),
         StructureTemplatePool.Projection.byName(pDynamic.get("dest_proj").asString(""))
      );
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         JigsawJunction $$1 = (JigsawJunction)pOther;
         if (this.sourceX != $$1.sourceX) {
            return false;
         } else if (this.sourceZ != $$1.sourceZ) {
            return false;
         } else {
            return this.deltaY != $$1.deltaY ? false : this.destProjection == $$1.destProjection;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.sourceX;
      $$0 = 31 * $$0 + this.sourceGroundY;
      $$0 = 31 * $$0 + this.sourceZ;
      $$0 = 31 * $$0 + this.deltaY;
      return 31 * $$0 + this.destProjection.hashCode();
   }

   @Override
   public String toString() {
      return "JigsawJunction{sourceX="
         + this.sourceX
         + ", sourceGroundY="
         + this.sourceGroundY
         + ", sourceZ="
         + this.sourceZ
         + ", deltaY="
         + this.deltaY
         + ", destProjection="
         + this.destProjection
         + "}";
   }
}
