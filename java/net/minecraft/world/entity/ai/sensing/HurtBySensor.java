package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class HurtBySensor extends Sensor<LivingEntity> {
   @Override
   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
   }

   @Override
   protected void doTick(ServerLevel pLevel, LivingEntity pEntity) {
      Brain<?> $$2 = pEntity.getBrain();
      DamageSource $$3 = pEntity.getLastDamageSource();
      if ($$3 != null) {
         $$2.setMemory(MemoryModuleType.HURT_BY, pEntity.getLastDamageSource());
         Entity $$4 = $$3.getEntity();
         if ($$4 instanceof LivingEntity) {
            $$2.setMemory(MemoryModuleType.HURT_BY_ENTITY, (LivingEntity)$$4);
         }
      } else {
         $$2.eraseMemory(MemoryModuleType.HURT_BY);
      }

      $$2.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(p_341386_ -> {
         if (!p_341386_.isAlive() || p_341386_.level() != pLevel) {
            $$2.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
         }
      });
   }
}
