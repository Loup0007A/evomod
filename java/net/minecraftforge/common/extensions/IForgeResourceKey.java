package net.minecraftforge.common.extensions;

import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IForgeResourceKey<T> {
   private ResourceKey<T> self() {
      return (ResourceKey<T>)this;
   }

   default Holder<T> getOrThrow(BlockEntity blockEntity) {
      return this.getOrThrow(blockEntity.getLevel());
   }

   default Holder<T> getOrThrow(Entity entity) {
      return this.getOrThrow(entity.registryAccess());
   }

   default Holder<T> getOrThrow(Level level) {
      return this.getOrThrow(level.registryAccess());
   }

   default Holder<T> getOrThrow(RegistryAccess registryAccess) {
      Objects.requireNonNull(registryAccess, "registryAccess was null");
      return registryAccess.registryOrThrow(this.self().registryKey()).getHolderOrThrow(this.self());
   }
}
