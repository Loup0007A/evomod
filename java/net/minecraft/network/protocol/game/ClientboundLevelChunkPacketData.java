package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public class ClientboundLevelChunkPacketData {
   private static final int TWO_MEGABYTES = 2097152;
   private final CompoundTag heightmaps;
   private final byte[] buffer;
   private final List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

   public ClientboundLevelChunkPacketData(LevelChunk pLevelChunk) {
      this.heightmaps = new CompoundTag();

      for (Entry<Heightmap.Types, Heightmap> $$1 : pLevelChunk.getHeightmaps()) {
         if ($$1.getKey().sendToClient()) {
            this.heightmaps.put($$1.getKey().getSerializationKey(), new LongArrayTag($$1.getValue().getRawData()));
         }
      }

      this.buffer = new byte[calculateChunkSize(pLevelChunk)];
      extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), pLevelChunk);
      this.blockEntitiesData = Lists.newArrayList();

      for (Entry<BlockPos, BlockEntity> $$2 : pLevelChunk.getBlockEntities().entrySet()) {
         this.blockEntitiesData.add(ClientboundLevelChunkPacketData.BlockEntityInfo.create($$2.getValue()));
      }
   }

   public ClientboundLevelChunkPacketData(RegistryFriendlyByteBuf pBuffer, int pX, int pZ) {
      this.heightmaps = pBuffer.readNbt();
      if (this.heightmaps == null) {
         throw new RuntimeException("Can't read heightmap in packet for [" + pX + ", " + pZ + "]");
      } else {
         int $$3 = pBuffer.readVarInt();
         if ($$3 > 2097152) {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
         } else {
            this.buffer = new byte[$$3];
            pBuffer.readBytes(this.buffer);
            this.blockEntitiesData = ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.decode(pBuffer);
         }
      }
   }

   public void write(RegistryFriendlyByteBuf pBuffer) {
      pBuffer.writeNbt(this.heightmaps);
      pBuffer.writeVarInt(this.buffer.length);
      pBuffer.writeBytes(this.buffer);
      ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.encode(pBuffer, this.blockEntitiesData);
   }

   private static int calculateChunkSize(LevelChunk pChunk) {
      int $$1 = 0;

      for (LevelChunkSection $$2 : pChunk.getSections()) {
         $$1 += $$2.getSerializedSize();
      }

      return $$1;
   }

   private ByteBuf getWriteBuffer() {
      ByteBuf $$0 = Unpooled.wrappedBuffer(this.buffer);
      $$0.writerIndex(0);
      return $$0;
   }

   public static void extractChunkData(FriendlyByteBuf pBuffer, LevelChunk pChunk) {
      for (LevelChunkSection $$2 : pChunk.getSections()) {
         $$2.write(pBuffer);
      }
   }

   public Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> getBlockEntitiesTagsConsumer(int pChunkX, int pChunkZ) {
      return p_195663_ -> this.getBlockEntitiesTags(p_195663_, pChunkX, pChunkZ);
   }

   private void getBlockEntitiesTags(ClientboundLevelChunkPacketData.BlockEntityTagOutput pOutput, int pChunkX, int pChunkZ) {
      int $$3 = 16 * pChunkX;
      int $$4 = 16 * pChunkZ;
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

      for (ClientboundLevelChunkPacketData.BlockEntityInfo $$6 : this.blockEntitiesData) {
         int $$7 = $$3 + SectionPos.sectionRelative($$6.packedXZ >> 4);
         int $$8 = $$4 + SectionPos.sectionRelative($$6.packedXZ);
         $$5.set($$7, $$6.y, $$8);
         pOutput.accept($$5, $$6.type, $$6.tag);
      }
   }

   public FriendlyByteBuf getReadBuffer() {
      return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
   }

   public CompoundTag getHeightmaps() {
      return this.heightmaps;
   }

   static class BlockEntityInfo {
      public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelChunkPacketData.BlockEntityInfo> STREAM_CODEC = StreamCodec.ofMember(
         ClientboundLevelChunkPacketData.BlockEntityInfo::write, ClientboundLevelChunkPacketData.BlockEntityInfo::new
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, List<ClientboundLevelChunkPacketData.BlockEntityInfo>> LIST_STREAM_CODEC = STREAM_CODEC.apply(
         ByteBufCodecs.list()
      );
      final int packedXZ;
      final int y;
      final BlockEntityType<?> type;
      @Nullable
      final CompoundTag tag;

      private BlockEntityInfo(int pPackedXZ, int pY, BlockEntityType<?> pType, @Nullable CompoundTag pTag) {
         this.packedXZ = pPackedXZ;
         this.y = pY;
         this.type = pType;
         this.tag = pTag;
      }

      private BlockEntityInfo(RegistryFriendlyByteBuf p_335103_) {
         this.packedXZ = p_335103_.readByte();
         this.y = p_335103_.readShort();
         this.type = ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).decode(p_335103_);
         this.tag = p_335103_.readNbt();
      }

      private void write(RegistryFriendlyByteBuf p_332659_) {
         p_332659_.writeByte(this.packedXZ);
         p_332659_.writeShort(this.y);
         ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).encode(p_332659_, this.type);
         p_332659_.writeNbt(this.tag);
      }

      static ClientboundLevelChunkPacketData.BlockEntityInfo create(BlockEntity pBlockEntity) {
         CompoundTag $$1 = pBlockEntity.getUpdateTag(pBlockEntity.getLevel().registryAccess());
         BlockPos $$2 = pBlockEntity.getBlockPos();
         int $$3 = SectionPos.sectionRelative($$2.getX()) << 4 | SectionPos.sectionRelative($$2.getZ());
         return new ClientboundLevelChunkPacketData.BlockEntityInfo($$3, $$2.getY(), pBlockEntity.getType(), $$1.isEmpty() ? null : $$1);
      }
   }

   @FunctionalInterface
   public interface BlockEntityTagOutput {
      void accept(BlockPos pPos, BlockEntityType<?> pType, @Nullable CompoundTag pNbt);
   }
}
