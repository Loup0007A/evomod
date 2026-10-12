package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public abstract class ClientboundMoveEntityPacket implements Packet<ClientGamePacketListener> {
   protected final int entityId;
   protected final short xa;
   protected final short ya;
   protected final short za;
   protected final byte yRot;
   protected final byte xRot;
   protected final boolean onGround;
   protected final boolean hasRot;
   protected final boolean hasPos;

   protected ClientboundMoveEntityPacket(
      int pEntityId, short pXa, short pYa, short pZa, byte pYRot, byte pXRot, boolean pOnGround, boolean pHasRot, boolean pHasPos
   ) {
      this.entityId = pEntityId;
      this.xa = pXa;
      this.ya = pYa;
      this.za = pZa;
      this.yRot = pYRot;
      this.xRot = pXRot;
      this.onGround = pOnGround;
      this.hasRot = pHasRot;
      this.hasPos = pHasPos;
   }

   @Override
   public abstract PacketType<? extends ClientboundMoveEntityPacket> type();

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleMoveEntity(this);
   }

   @Override
   public String toString() {
      return "Entity_" + super.toString();
   }

   @Nullable
   public Entity getEntity(Level pLevel) {
      return pLevel.getEntity(this.entityId);
   }

   public short getXa() {
      return this.xa;
   }

   public short getYa() {
      return this.ya;
   }

   public short getZa() {
      return this.za;
   }

   public byte getyRot() {
      return this.yRot;
   }

   public byte getxRot() {
      return this.xRot;
   }

   public boolean hasRotation() {
      return this.hasRot;
   }

   public boolean hasPosition() {
      return this.hasPos;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public static class Pos extends ClientboundMoveEntityPacket {
      public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.Pos> STREAM_CODEC = Packet.codec(
         ClientboundMoveEntityPacket.Pos::write, ClientboundMoveEntityPacket.Pos::read
      );

      public Pos(int pEntityId, short pXa, short pYa, short pZa, boolean pOnGround) {
         super(pEntityId, pXa, pYa, pZa, (byte)0, (byte)0, pOnGround, false, true);
      }

      private static ClientboundMoveEntityPacket.Pos read(FriendlyByteBuf p_179001_) {
         int $$1 = p_179001_.readVarInt();
         short $$2 = p_179001_.readShort();
         short $$3 = p_179001_.readShort();
         short $$4 = p_179001_.readShort();
         boolean $$5 = p_179001_.readBoolean();
         return new ClientboundMoveEntityPacket.Pos($$1, $$2, $$3, $$4, $$5);
      }

      private void write(FriendlyByteBuf p_132549_) {
         p_132549_.writeVarInt(this.entityId);
         p_132549_.writeShort(this.xa);
         p_132549_.writeShort(this.ya);
         p_132549_.writeShort(this.za);
         p_132549_.writeBoolean(this.onGround);
      }

      @Override
      public PacketType<ClientboundMoveEntityPacket.Pos> type() {
         return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS;
      }
   }

   public static class PosRot extends ClientboundMoveEntityPacket {
      public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.PosRot> STREAM_CODEC = Packet.codec(
         ClientboundMoveEntityPacket.PosRot::write, ClientboundMoveEntityPacket.PosRot::read
      );

      public PosRot(int pEntityId, short pXa, short pYa, short pZa, byte pYRot, byte pXRot, boolean pOnGround) {
         super(pEntityId, pXa, pYa, pZa, pYRot, pXRot, pOnGround, true, true);
      }

      private static ClientboundMoveEntityPacket.PosRot read(FriendlyByteBuf p_179003_) {
         int $$1 = p_179003_.readVarInt();
         short $$2 = p_179003_.readShort();
         short $$3 = p_179003_.readShort();
         short $$4 = p_179003_.readShort();
         byte $$5 = p_179003_.readByte();
         byte $$6 = p_179003_.readByte();
         boolean $$7 = p_179003_.readBoolean();
         return new ClientboundMoveEntityPacket.PosRot($$1, $$2, $$3, $$4, $$5, $$6, $$7);
      }

      private void write(FriendlyByteBuf p_132564_) {
         p_132564_.writeVarInt(this.entityId);
         p_132564_.writeShort(this.xa);
         p_132564_.writeShort(this.ya);
         p_132564_.writeShort(this.za);
         p_132564_.writeByte(this.yRot);
         p_132564_.writeByte(this.xRot);
         p_132564_.writeBoolean(this.onGround);
      }

      @Override
      public PacketType<ClientboundMoveEntityPacket.PosRot> type() {
         return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS_ROT;
      }
   }

   public static class Rot extends ClientboundMoveEntityPacket {
      public static final StreamCodec<FriendlyByteBuf, ClientboundMoveEntityPacket.Rot> STREAM_CODEC = Packet.codec(
         ClientboundMoveEntityPacket.Rot::write, ClientboundMoveEntityPacket.Rot::read
      );

      public Rot(int pEntityId, byte pYRot, byte pXRot, boolean pOnGround) {
         super(pEntityId, (short)0, (short)0, (short)0, pYRot, pXRot, pOnGround, true, false);
      }

      private static ClientboundMoveEntityPacket.Rot read(FriendlyByteBuf p_179005_) {
         int $$1 = p_179005_.readVarInt();
         byte $$2 = p_179005_.readByte();
         byte $$3 = p_179005_.readByte();
         boolean $$4 = p_179005_.readBoolean();
         return new ClientboundMoveEntityPacket.Rot($$1, $$2, $$3, $$4);
      }

      private void write(FriendlyByteBuf p_132576_) {
         p_132576_.writeVarInt(this.entityId);
         p_132576_.writeByte(this.yRot);
         p_132576_.writeByte(this.xRot);
         p_132576_.writeBoolean(this.onGround);
      }

      @Override
      public PacketType<ClientboundMoveEntityPacket.Rot> type() {
         return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_ROT;
      }
   }
}
