package net.minecraftforge.event.entity;

import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

public class EntityEvent extends Event {
   private final Entity entity;

   public EntityEvent(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public static class EnteringSection extends EntityEvent {
      private final long packedOldPos;
      private final long packedNewPos;

      public EnteringSection(Entity entity, long packedOldPos, long packedNewPos) {
         super(entity);
         this.packedOldPos = packedOldPos;
         this.packedNewPos = packedNewPos;
      }

      public long getPackedOldPos() {
         return this.packedOldPos;
      }

      public long getPackedNewPos() {
         return this.packedNewPos;
      }

      public SectionPos getOldPos() {
         return SectionPos.of(this.packedOldPos);
      }

      public SectionPos getNewPos() {
         return SectionPos.of(this.packedNewPos);
      }

      public boolean didChunkChange() {
         return SectionPos.x(this.packedOldPos) != SectionPos.x(this.packedNewPos) || SectionPos.z(this.packedOldPos) != SectionPos.z(this.packedNewPos);
      }
   }

   public static class EntityConstructing extends EntityEvent {
      public EntityConstructing(Entity entity) {
         super(entity);
      }
   }
}
