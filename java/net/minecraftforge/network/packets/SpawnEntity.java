package net.minecraftforge.network.packets;

import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class SpawnEntity {
   public static final StreamCodec<RegistryFriendlyByteBuf, SpawnEntity> STREAM_CODEC = StreamCodec.ofMember(SpawnEntity::encode, SpawnEntity::decode);
   private final Entity entity;
   private final int typeId;
   private final int entityId;
   private final UUID uuid;
   private final double posX;
   private final double posY;
   private final double posZ;
   private final byte pitch;
   private final byte yaw;
   private final byte headYaw;
   private final int velX;
   private final int velY;
   private final int velZ;
   private final FriendlyByteBuf buf;

   @Internal
   public SpawnEntity(Entity e) {
      this.entity = e;
      this.typeId = BuiltInRegistries.ENTITY_TYPE.getId(e.getType());
      this.entityId = e.getId();
      this.uuid = e.getUUID();
      this.posX = e.getX();
      this.posY = e.getY();
      this.posZ = e.getZ();
      this.pitch = (byte)Mth.floor(e.getXRot() * 256.0F / 360.0F);
      this.yaw = (byte)Mth.floor(e.getYRot() * 256.0F / 360.0F);
      this.headYaw = (byte)((int)(e.getYHeadRot() * 256.0F / 360.0F));
      Vec3 vec3d = e.getDeltaMovement();
      double d1 = Mth.clamp(vec3d.x, -3.9, 3.9);
      double d2 = Mth.clamp(vec3d.y, -3.9, 3.9);
      double d3 = Mth.clamp(vec3d.z, -3.9, 3.9);
      this.velX = (int)(d1 * 8000.0);
      this.velY = (int)(d2 * 8000.0);
      this.velZ = (int)(d3 * 8000.0);
      this.buf = null;
   }

   private SpawnEntity(
      int typeId,
      int entityId,
      UUID uuid,
      double posX,
      double posY,
      double posZ,
      byte pitch,
      byte yaw,
      byte headYaw,
      int velX,
      int velY,
      int velZ,
      FriendlyByteBuf buf
   ) {
      this.entity = null;
      this.typeId = typeId;
      this.entityId = entityId;
      this.uuid = uuid;
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.pitch = pitch;
      this.yaw = yaw;
      this.headYaw = headYaw;
      this.velX = velX;
      this.velY = velY;
      this.velZ = velZ;
      this.buf = buf;
   }

   public static void encode(SpawnEntity msg, FriendlyByteBuf buf) {
      buf.writeVarInt(msg.typeId);
      buf.writeInt(msg.entityId);
      buf.writeLong(msg.uuid.getMostSignificantBits());
      buf.writeLong(msg.uuid.getLeastSignificantBits());
      buf.writeDouble(msg.posX);
      buf.writeDouble(msg.posY);
      buf.writeDouble(msg.posZ);
      buf.writeByte(msg.pitch);
      buf.writeByte(msg.yaw);
      buf.writeByte(msg.headYaw);
      buf.writeShort(msg.velX);
      buf.writeShort(msg.velY);
      buf.writeShort(msg.velZ);
      if (msg.entity instanceof IEntityAdditionalSpawnData entityAdditionalSpawnData) {
         FriendlyByteBuf spawnDataBuffer = new FriendlyByteBuf(Unpooled.buffer());
         entityAdditionalSpawnData.writeSpawnData(spawnDataBuffer);
         buf.writeVarInt(spawnDataBuffer.readableBytes());
         buf.writeBytes(spawnDataBuffer);
         spawnDataBuffer.release();
      } else {
         buf.writeVarInt(0);
      }
   }

   public static SpawnEntity decode(FriendlyByteBuf buf) {
      return new SpawnEntity(
         buf.readVarInt(),
         buf.readInt(),
         new UUID(buf.readLong(), buf.readLong()),
         buf.readDouble(),
         buf.readDouble(),
         buf.readDouble(),
         buf.readByte(),
         buf.readByte(),
         buf.readByte(),
         buf.readShort(),
         buf.readShort(),
         buf.readShort(),
         readSpawnDataPacket(buf)
      );
   }

   private static FriendlyByteBuf readSpawnDataPacket(FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      if (count > 0) {
         FriendlyByteBuf spawnDataBuffer = new FriendlyByteBuf(Unpooled.buffer());
         spawnDataBuffer.writeBytes(buf, count);
         return spawnDataBuffer;
      } else {
         return new FriendlyByteBuf(Unpooled.buffer());
      }
   }

   public static void handle(SpawnEntity msg, CustomPayloadEvent.Context ctx) {
      try {
         EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byId(msg.typeId);
         Optional<Level> world = LogicalSidedProvider.CLIENTWORLD.get(ctx.isClientSide());
         Entity e = world.<Entity>map(w -> type.customClientSpawn(msg, w)).orElse(null);
         if (e != null) {
            e.syncPacketPositionCodec(msg.posX, msg.posY, msg.posZ);
            e.absMoveTo(msg.posX, msg.posY, msg.posZ, (float)(msg.yaw * 360) / 256.0F, (float)(msg.pitch * 360) / 256.0F);
            e.setYHeadRot((float)(msg.headYaw * 360) / 256.0F);
            e.setYBodyRot((float)(msg.headYaw * 360) / 256.0F);
            e.setId(msg.entityId);
            e.setUUID(msg.uuid);
            if (world.orElse(null) instanceof ClientLevel cworld) {
               cworld.addEntity(e);
            }

            e.lerpMotion((double)msg.velX / 8000.0, (double)msg.velY / 8000.0, (double)msg.velZ / 8000.0);
            if (e instanceof IEntityAdditionalSpawnData entityAdditionalSpawnData) {
               entityAdditionalSpawnData.readSpawnData(msg.buf);
            }

            return;
         }
      } finally {
         msg.buf.release();
      }
   }

   public Entity getEntity() {
      return this.entity;
   }

   public int getTypeId() {
      return this.typeId;
   }

   public int getEntityId() {
      return this.entityId;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public double getPosX() {
      return this.posX;
   }

   public double getPosY() {
      return this.posY;
   }

   public double getPosZ() {
      return this.posZ;
   }

   public byte getPitch() {
      return this.pitch;
   }

   public byte getYaw() {
      return this.yaw;
   }

   public byte getHeadYaw() {
      return this.headYaw;
   }

   public int getVelX() {
      return this.velX;
   }

   public int getVelY() {
      return this.velY;
   }

   public int getVelZ() {
      return this.velZ;
   }

   public FriendlyByteBuf getAdditionalData() {
      return this.buf;
   }
}
