package net.minecraftforge.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

public abstract class PartEntity<T extends Entity> extends Entity {
   private final T parent;

   public PartEntity(T parent) {
      super(parent.getType(), parent.level());
      this.parent = parent;
   }

   public T getParent() {
      return this.parent;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_344981_) {
      throw new UnsupportedOperationException();
   }
}
