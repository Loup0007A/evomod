package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.AABB;

public class NearestLivingEntitySensor<T extends LivingEntity> extends Sensor<T> {
   @Override
   protected void doTick(ServerLevel pLevel, T pEntity) {
      AABB $$2 = pEntity.getBoundingBox().inflate((double)this.radiusXZ(), (double)this.radiusY(), (double)this.radiusXZ());
      List<LivingEntity> $$3 = pLevel.getEntitiesOfClass(LivingEntity.class, $$2, p_26717_ -> p_26717_ != pEntity && p_26717_.isAlive());
      $$3.sort(Comparator.comparingDouble(pEntity::distanceToSqr));
      Brain<?> $$4 = pEntity.getBrain();
      $$4.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, $$3);
      $$4.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(pEntity, $$3));
   }

   protected int radiusXZ() {
      return 16;
   }

   protected int radiusY() {
      return 16;
   }

   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}
