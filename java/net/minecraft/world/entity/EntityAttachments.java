package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class EntityAttachments {
   private final Map<EntityAttachment, List<Vec3>> attachments;

   EntityAttachments(Map<EntityAttachment, List<Vec3>> pAttachments) {
      this.attachments = pAttachments;
   }

   public static EntityAttachments createDefault(float pWidth, float pHeight) {
      return builder().build(pWidth, pHeight);
   }

   public static EntityAttachments.Builder builder() {
      return new EntityAttachments.Builder();
   }

   public EntityAttachments scale(float pXScale, float pYScale, float pZScale) {
      Map<EntityAttachment, List<Vec3>> $$3 = new EnumMap<>(EntityAttachment.class);

      for (Entry<EntityAttachment, List<Vec3>> $$4 : this.attachments.entrySet()) {
         $$3.put($$4.getKey(), scalePoints($$4.getValue(), pXScale, pYScale, pZScale));
      }

      return new EntityAttachments($$3);
   }

   private static List<Vec3> scalePoints(List<Vec3> pAttachmentPoints, float pXScale, float pYScale, float pZScale) {
      List<Vec3> $$4 = new ArrayList<>(pAttachmentPoints.size());

      for (Vec3 $$5 : pAttachmentPoints) {
         $$4.add($$5.multiply((double)pXScale, (double)pYScale, (double)pZScale));
      }

      return $$4;
   }

   @Nullable
   public Vec3 getNullable(EntityAttachment pAttachment, int pIndex, float pYRot) {
      List<Vec3> $$3 = this.attachments.get(pAttachment);
      return pIndex >= 0 && pIndex < $$3.size() ? transformPoint($$3.get(pIndex), pYRot) : null;
   }

   public Vec3 get(EntityAttachment pAttachment, int pIndex, float pYRot) {
      Vec3 $$3 = this.getNullable(pAttachment, pIndex, pYRot);
      if ($$3 == null) {
         throw new IllegalStateException("Had no attachment point of type: " + pAttachment + " for index: " + pIndex);
      } else {
         return $$3;
      }
   }

   public Vec3 getClamped(EntityAttachment pAttachment, int pIndex, float pYRot) {
      List<Vec3> $$3 = this.attachments.get(pAttachment);
      if ($$3.isEmpty()) {
         throw new IllegalStateException("Had no attachment points of type: " + pAttachment);
      } else {
         Vec3 $$4 = $$3.get(Mth.clamp(pIndex, 0, $$3.size() - 1));
         return transformPoint($$4, pYRot);
      }
   }

   private static Vec3 transformPoint(Vec3 pPoint, float pYRot) {
      return pPoint.yRot(-pYRot * (float) (Math.PI / 180.0));
   }

   public static class Builder {
      private final Map<EntityAttachment, List<Vec3>> attachments = new EnumMap<>(EntityAttachment.class);

      Builder() {
      }

      public EntityAttachments.Builder attach(EntityAttachment pAttachment, float pX, float pY, float pZ) {
         return this.attach(pAttachment, new Vec3((double)pX, (double)pY, (double)pZ));
      }

      public EntityAttachments.Builder attach(EntityAttachment pAttachment, Vec3 pPoas) {
         this.attachments.computeIfAbsent(pAttachment, p_333992_ -> new ArrayList<>(1)).add(pPoas);
         return this;
      }

      public EntityAttachments build(float pWidth, float pHeight) {
         Map<EntityAttachment, List<Vec3>> $$2 = new EnumMap<>(EntityAttachment.class);

         for (EntityAttachment $$3 : EntityAttachment.values()) {
            List<Vec3> $$4 = this.attachments.get($$3);
            $$2.put($$3, $$4 != null ? List.copyOf($$4) : $$3.createFallbackPoints(pWidth, pHeight));
         }

         return new EntityAttachments($$2);
      }
   }
}
