package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAP_SIZE = 128;
   private static final int HALF_MAP_SIZE = 64;
   public static final int MAX_SCALE = 4;
   public static final int TRACKED_DECORATION_LIMIT = 256;
   private static final String FRAME_PREFIX = "frame-";
   public final int centerX;
   public final int centerZ;
   public final ResourceKey<Level> dimension;
   private final boolean trackingPosition;
   private final boolean unlimitedTracking;
   public final byte scale;
   public byte[] colors = new byte[16384];
   public final boolean locked;
   private final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
   private final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
   private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
   final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
   private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
   private int trackedDecorationCount;

   public static SavedData.Factory<MapItemSavedData> factory() {
      return new SavedData.Factory<>(() -> {
         throw new IllegalStateException("Should never create an empty map saved data");
      }, MapItemSavedData::load, DataFixTypes.SAVED_DATA_MAP_DATA);
   }

   private MapItemSavedData(int pX, int pZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, boolean pLocked, ResourceKey<Level> pDimension) {
      this.scale = pScale;
      this.centerX = pX;
      this.centerZ = pZ;
      this.dimension = pDimension;
      this.trackingPosition = pTrackingPosition;
      this.unlimitedTracking = pUnlimitedTracking;
      this.locked = pLocked;
      this.setDirty();
   }

   public static MapItemSavedData createFresh(
      double pX, double pZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, ResourceKey<Level> pDimension
   ) {
      int $$6 = 128 * (1 << pScale);
      int $$7 = Mth.floor((pX + 64.0) / (double)$$6);
      int $$8 = Mth.floor((pZ + 64.0) / (double)$$6);
      int $$9 = $$7 * $$6 + $$6 / 2 - 64;
      int $$10 = $$8 * $$6 + $$6 / 2 - 64;
      return new MapItemSavedData($$9, $$10, pScale, pTrackingPosition, pUnlimitedTracking, false, pDimension);
   }

   public static MapItemSavedData createForClient(byte pScale, boolean pLocked, ResourceKey<Level> pDimension) {
      return new MapItemSavedData(0, 0, pScale, false, false, pLocked, pDimension);
   }

   public static MapItemSavedData load(CompoundTag p_164808_, HolderLookup.Provider p_332149_) {
      ResourceKey<Level> $$2 = (ResourceKey<Level>)DimensionType.parseLegacy(new Dynamic(NbtOps.INSTANCE, p_164808_.get("dimension")))
         .resultOrPartial(LOGGER::error)
         .orElseThrow(() -> new IllegalArgumentException("Invalid map dimension: " + p_164808_.get("dimension")));
      int $$3 = p_164808_.getInt("xCenter");
      int $$4 = p_164808_.getInt("zCenter");
      byte $$5 = (byte)Mth.clamp(p_164808_.getByte("scale"), 0, 4);
      boolean $$6 = !p_164808_.contains("trackingPosition", 1) || p_164808_.getBoolean("trackingPosition");
      boolean $$7 = p_164808_.getBoolean("unlimitedTracking");
      boolean $$8 = p_164808_.getBoolean("locked");
      MapItemSavedData $$9 = new MapItemSavedData($$3, $$4, $$5, $$6, $$7, $$8, $$2);
      byte[] $$10 = p_164808_.getByteArray("colors");
      if ($$10.length == 16384) {
         $$9.colors = $$10;
      }

      RegistryOps<Tag> $$11 = p_332149_.createSerializationContext(NbtOps.INSTANCE);

      for (MapBanner $$13 : MapBanner.LIST_CODEC
         .parse($$11, p_164808_.get("banners"))
         .resultOrPartial(p_327533_ -> LOGGER.warn("Failed to parse map banner: '{}'", p_327533_))
         .orElse(List.of())) {
         $$9.bannerMarkers.put($$13.getId(), $$13);
         $$9.addDecoration($$13.getDecoration(), null, $$13.getId(), (double)$$13.pos().getX(), (double)$$13.pos().getZ(), 180.0, $$13.name().orElse(null));
      }

      ListTag $$14 = p_164808_.getList("frames", 10);

      for (int $$15 = 0; $$15 < $$14.size(); $$15++) {
         MapFrame $$16 = MapFrame.load($$14.getCompound($$15));
         if ($$16 != null) {
            $$9.frameMarkers.put($$16.getId(), $$16);
            $$9.addDecoration(
               MapDecorationTypes.FRAME,
               null,
               getFrameKey($$16.getEntityId()),
               (double)$$16.getPos().getX(),
               (double)$$16.getPos().getZ(),
               (double)$$16.getRotation(),
               null
            );
         }
      }

      return $$9;
   }

   @Override
   public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      ResourceLocation.CODEC
         .encodeStart(NbtOps.INSTANCE, this.dimension.location())
         .resultOrPartial(LOGGER::error)
         .ifPresent(p_77954_ -> pTag.put("dimension", p_77954_));
      pTag.putInt("xCenter", this.centerX);
      pTag.putInt("zCenter", this.centerZ);
      pTag.putByte("scale", this.scale);
      pTag.putByteArray("colors", this.colors);
      pTag.putBoolean("trackingPosition", this.trackingPosition);
      pTag.putBoolean("unlimitedTracking", this.unlimitedTracking);
      pTag.putBoolean("locked", this.locked);
      RegistryOps<Tag> $$2 = pRegistries.createSerializationContext(NbtOps.INSTANCE);
      pTag.put("banners", (Tag)MapBanner.LIST_CODEC.encodeStart($$2, List.copyOf(this.bannerMarkers.values())).getOrThrow());
      ListTag $$3 = new ListTag();

      for (MapFrame $$4 : this.frameMarkers.values()) {
         $$3.add($$4.save());
      }

      pTag.put("frames", $$3);
      return pTag;
   }

   public MapItemSavedData locked() {
      MapItemSavedData $$0 = new MapItemSavedData(this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension);
      $$0.bannerMarkers.putAll(this.bannerMarkers);
      $$0.decorations.putAll(this.decorations);
      $$0.trackedDecorationCount = this.trackedDecorationCount;
      System.arraycopy(this.colors, 0, $$0.colors, 0, this.colors.length);
      $$0.setDirty();
      return $$0;
   }

   public MapItemSavedData scaled() {
      return createFresh(
         (double)this.centerX, (double)this.centerZ, (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension
      );
   }

   private static Predicate<ItemStack> mapMatcher(ItemStack pStack) {
      MapId $$1 = pStack.get(DataComponents.MAP_ID);
      return p_327526_ -> p_327526_ == pStack ? true : p_327526_.is(pStack.getItem()) && Objects.equals($$1, p_327526_.get(DataComponents.MAP_ID));
   }

   public void tickCarriedBy(Player pPlayer, ItemStack pMapStack) {
      if (!this.carriedByPlayers.containsKey(pPlayer)) {
         MapItemSavedData.HoldingPlayer $$2 = new MapItemSavedData.HoldingPlayer(pPlayer);
         this.carriedByPlayers.put(pPlayer, $$2);
         this.carriedBy.add($$2);
      }

      Predicate<ItemStack> $$3 = mapMatcher(pMapStack);
      if (!pPlayer.getInventory().contains($$3)) {
         this.removeDecoration(pPlayer.getName().getString());
      }

      for (int $$4 = 0; $$4 < this.carriedBy.size(); $$4++) {
         MapItemSavedData.HoldingPlayer $$5 = this.carriedBy.get($$4);
         String $$6 = $$5.player.getName().getString();
         if (!$$5.player.isRemoved() && ($$5.player.getInventory().contains($$3) || pMapStack.isFramed())) {
            if (!pMapStack.isFramed() && $$5.player.level().dimension() == this.dimension && this.trackingPosition) {
               this.addDecoration(MapDecorationTypes.PLAYER, $$5.player.level(), $$6, $$5.player.getX(), $$5.player.getZ(), (double)$$5.player.getYRot(), null);
            }
         } else {
            this.carriedByPlayers.remove($$5.player);
            this.carriedBy.remove($$5);
            this.removeDecoration($$6);
         }
      }

      if (pMapStack.isFramed() && this.trackingPosition) {
         ItemFrame $$7 = pMapStack.getFrame();
         BlockPos $$8 = $$7.getPos();
         MapFrame $$9 = this.frameMarkers.get(MapFrame.frameId($$8));
         if ($$9 != null && $$7.getId() != $$9.getEntityId() && this.frameMarkers.containsKey($$9.getId())) {
            this.removeDecoration(getFrameKey($$9.getEntityId()));
         }

         MapFrame $$10 = new MapFrame($$8, $$7.getDirection().get2DDataValue() * 90, $$7.getId());
         this.addDecoration(
            MapDecorationTypes.FRAME,
            pPlayer.level(),
            getFrameKey($$7.getId()),
            (double)$$8.getX(),
            (double)$$8.getZ(),
            (double)($$7.getDirection().get2DDataValue() * 90),
            null
         );
         this.frameMarkers.put($$10.getId(), $$10);
      }

      MapDecorations $$11 = pMapStack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
      if (!this.decorations.keySet().containsAll($$11.decorations().keySet())) {
         $$11.decorations().forEach((p_341967_, p_341968_) -> {
            if (!this.decorations.containsKey(p_341967_)) {
               this.addDecoration(p_341968_.type(), pPlayer.level(), p_341967_, p_341968_.x(), p_341968_.z(), (double)p_341968_.rotation(), null);
            }
         });
      }
   }

   private void removeDecoration(String pIdentifier) {
      MapDecoration $$1 = this.decorations.remove(pIdentifier);
      if ($$1 != null && $$1.type().value().trackCount()) {
         this.trackedDecorationCount--;
      }

      this.setDecorationsDirty();
   }

   public static void addTargetDecoration(ItemStack pStack, BlockPos pPos, String pType, Holder<MapDecorationType> pMapDecorationType) {
      MapDecorations.Entry $$4 = new MapDecorations.Entry(pMapDecorationType, (double)pPos.getX(), (double)pPos.getZ(), 180.0F);
      pStack.update(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY, p_327532_ -> p_327532_.withDecoration(pType, $$4));
      if (pMapDecorationType.value().hasMapColor()) {
         pStack.set(DataComponents.MAP_COLOR, new MapItemColor(pMapDecorationType.value().mapColor()));
      }
   }

   private void addDecoration(
      Holder<MapDecorationType> pDecorationType,
      @Nullable LevelAccessor pLevel,
      String pId,
      double pX,
      double pZ,
      double pYRot,
      @Nullable Component pDisplayName
   ) {
      int $$7 = 1 << this.scale;
      float $$8 = (float)(pX - (double)this.centerX) / (float)$$7;
      float $$9 = (float)(pZ - (double)this.centerZ) / (float)$$7;
      byte $$10 = (byte)((int)((double)($$8 * 2.0F) + 0.5));
      byte $$11 = (byte)((int)((double)($$9 * 2.0F) + 0.5));
      int $$12 = 63;
      byte $$13;
      if ($$8 >= -63.0F && $$9 >= -63.0F && $$8 <= 63.0F && $$9 <= 63.0F) {
         pYRot += pYRot < 0.0 ? -8.0 : 8.0;
         $$13 = (byte)((int)(pYRot * 16.0 / 360.0));
         if (this.dimension == Level.NETHER && pLevel != null) {
            int $$14 = (int)(pLevel.getLevelData().getDayTime() / 10L);
            $$13 = (byte)($$14 * $$14 * 34187121 + $$14 * 121 >> 15 & 15);
         }
      } else {
         if (!pDecorationType.is(MapDecorationTypes.PLAYER)) {
            this.removeDecoration(pId);
            return;
         }

         int $$15 = 320;
         if (Math.abs($$8) < 320.0F && Math.abs($$9) < 320.0F) {
            pDecorationType = MapDecorationTypes.PLAYER_OFF_MAP;
         } else {
            if (!this.unlimitedTracking) {
               this.removeDecoration(pId);
               return;
            }

            pDecorationType = MapDecorationTypes.PLAYER_OFF_LIMITS;
         }

         $$13 = 0;
         if ($$8 <= -63.0F) {
            $$10 = -128;
         }

         if ($$9 <= -63.0F) {
            $$11 = -128;
         }

         if ($$8 >= 63.0F) {
            $$10 = 127;
         }

         if ($$9 >= 63.0F) {
            $$11 = 127;
         }
      }

      MapDecoration $$18 = new MapDecoration(pDecorationType, $$10, $$11, $$13, Optional.ofNullable(pDisplayName));
      MapDecoration $$19 = this.decorations.put(pId, $$18);
      if (!$$18.equals($$19)) {
         if ($$19 != null && $$19.type().value().trackCount()) {
            this.trackedDecorationCount--;
         }

         if (pDecorationType.value().trackCount()) {
            this.trackedDecorationCount++;
         }

         this.setDecorationsDirty();
      }
   }

   @Nullable
   public Packet<?> getUpdatePacket(MapId pMapId, Player pPlayer) {
      MapItemSavedData.HoldingPlayer $$2 = this.carriedByPlayers.get(pPlayer);
      return $$2 == null ? null : $$2.nextUpdatePacket(pMapId);
   }

   private void setColorsDirty(int pX, int pZ) {
      this.setDirty();

      for (MapItemSavedData.HoldingPlayer $$2 : this.carriedBy) {
         $$2.markColorsDirty(pX, pZ);
      }
   }

   private void setDecorationsDirty() {
      this.setDirty();
      this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
   }

   public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player pPlayer) {
      MapItemSavedData.HoldingPlayer $$1 = this.carriedByPlayers.get(pPlayer);
      if ($$1 == null) {
         $$1 = new MapItemSavedData.HoldingPlayer(pPlayer);
         this.carriedByPlayers.put(pPlayer, $$1);
         this.carriedBy.add($$1);
      }

      return $$1;
   }

   public boolean toggleBanner(LevelAccessor pAccessor, BlockPos pPos) {
      double $$2 = (double)pPos.getX() + 0.5;
      double $$3 = (double)pPos.getZ() + 0.5;
      int $$4 = 1 << this.scale;
      double $$5 = ($$2 - (double)this.centerX) / (double)$$4;
      double $$6 = ($$3 - (double)this.centerZ) / (double)$$4;
      int $$7 = 63;
      if ($$5 >= -63.0 && $$6 >= -63.0 && $$5 <= 63.0 && $$6 <= 63.0) {
         MapBanner $$8 = MapBanner.fromWorld(pAccessor, pPos);
         if ($$8 == null) {
            return false;
         }

         if (this.bannerMarkers.remove($$8.getId(), $$8)) {
            this.removeDecoration($$8.getId());
            return true;
         }

         if (!this.isTrackedCountOverLimit(256)) {
            this.bannerMarkers.put($$8.getId(), $$8);
            this.addDecoration($$8.getDecoration(), pAccessor, $$8.getId(), $$2, $$3, 180.0, $$8.name().orElse(null));
            return true;
         }
      }

      return false;
   }

   public void checkBanners(BlockGetter pReader, int pX, int pZ) {
      Iterator<MapBanner> $$3 = this.bannerMarkers.values().iterator();

      while ($$3.hasNext()) {
         MapBanner $$4 = $$3.next();
         if ($$4.pos().getX() == pX && $$4.pos().getZ() == pZ) {
            MapBanner $$5 = MapBanner.fromWorld(pReader, $$4.pos());
            if (!$$4.equals($$5)) {
               $$3.remove();
               this.removeDecoration($$4.getId());
            }
         }
      }
   }

   public Collection<MapBanner> getBanners() {
      return this.bannerMarkers.values();
   }

   public void removedFromFrame(BlockPos pPos, int pEntityId) {
      this.removeDecoration(getFrameKey(pEntityId));
      this.frameMarkers.remove(MapFrame.frameId(pPos));
   }

   public boolean updateColor(int pX, int pZ, byte pColor) {
      byte $$3 = this.colors[pX + pZ * 128];
      if ($$3 != pColor) {
         this.setColor(pX, pZ, pColor);
         return true;
      } else {
         return false;
      }
   }

   public void setColor(int pX, int pZ, byte pColor) {
      this.colors[pX + pZ * 128] = pColor;
      this.setColorsDirty(pX, pZ);
   }

   public boolean isExplorationMap() {
      for (MapDecoration $$0 : this.decorations.values()) {
         if ($$0.type().value().explorationMapElement()) {
            return true;
         }
      }

      return false;
   }

   public void addClientSideDecorations(List<MapDecoration> pDecorations) {
      this.decorations.clear();
      this.trackedDecorationCount = 0;

      for (int $$1 = 0; $$1 < pDecorations.size(); $$1++) {
         MapDecoration $$2 = pDecorations.get($$1);
         this.decorations.put("icon-" + $$1, $$2);
         if ($$2.type().value().trackCount()) {
            this.trackedDecorationCount++;
         }
      }
   }

   public Iterable<MapDecoration> getDecorations() {
      return this.decorations.values();
   }

   public boolean isTrackedCountOverLimit(int pTrackedCount) {
      return this.trackedDecorationCount >= pTrackedCount;
   }

   private static String getFrameKey(int pEntityId) {
      return "frame-" + pEntityId;
   }

   public class HoldingPlayer {
      public final Player player;
      private boolean dirtyData = true;
      private int minDirtyX;
      private int minDirtyY;
      private int maxDirtyX = 127;
      private int maxDirtyY = 127;
      private boolean dirtyDecorations = true;
      private int tick;
      public int step;

      HoldingPlayer(final Player pPlayer) {
         this.player = pPlayer;
      }

      private MapItemSavedData.MapPatch createPatch() {
         int $$0 = this.minDirtyX;
         int $$1 = this.minDirtyY;
         int $$2 = this.maxDirtyX + 1 - this.minDirtyX;
         int $$3 = this.maxDirtyY + 1 - this.minDirtyY;
         byte[] $$4 = new byte[$$2 * $$3];

         for (int $$5 = 0; $$5 < $$2; $$5++) {
            for (int $$6 = 0; $$6 < $$3; $$6++) {
               $$4[$$5 + $$6 * $$2] = MapItemSavedData.this.colors[$$0 + $$5 + ($$1 + $$6) * 128];
            }
         }

         return new MapItemSavedData.MapPatch($$0, $$1, $$2, $$3, $$4);
      }

      @Nullable
      Packet<?> nextUpdatePacket(MapId pMapId) {
         MapItemSavedData.MapPatch $$1;
         if (this.dirtyData) {
            this.dirtyData = false;
            $$1 = this.createPatch();
         } else {
            $$1 = null;
         }

         Collection<MapDecoration> $$3;
         if (this.dirtyDecorations && this.tick++ % 5 == 0) {
            this.dirtyDecorations = false;
            $$3 = MapItemSavedData.this.decorations.values();
         } else {
            $$3 = null;
         }

         return $$3 == null && $$1 == null
            ? null
            : new ClientboundMapItemDataPacket(pMapId, MapItemSavedData.this.scale, MapItemSavedData.this.locked, $$3, $$1);
      }

      void markColorsDirty(int pX, int pZ) {
         if (this.dirtyData) {
            this.minDirtyX = Math.min(this.minDirtyX, pX);
            this.minDirtyY = Math.min(this.minDirtyY, pZ);
            this.maxDirtyX = Math.max(this.maxDirtyX, pX);
            this.maxDirtyY = Math.max(this.maxDirtyY, pZ);
         } else {
            this.dirtyData = true;
            this.minDirtyX = pX;
            this.minDirtyY = pZ;
            this.maxDirtyX = pX;
            this.maxDirtyY = pZ;
         }
      }

      private void markDecorationsDirty() {
         this.dirtyDecorations = true;
      }
   }

   public static record MapPatch(int startX, int startY, int width, int height, byte[] mapColors) {
      public static final StreamCodec<ByteBuf, Optional<MapItemSavedData.MapPatch>> STREAM_CODEC = StreamCodec.of(
         MapItemSavedData.MapPatch::write, MapItemSavedData.MapPatch::read
      );

      private static void write(ByteBuf p_334846_, Optional<MapItemSavedData.MapPatch> p_333957_) {
         if (p_333957_.isPresent()) {
            MapItemSavedData.MapPatch $$2 = p_333957_.get();
            p_334846_.writeByte($$2.width);
            p_334846_.writeByte($$2.height);
            p_334846_.writeByte($$2.startX);
            p_334846_.writeByte($$2.startY);
            FriendlyByteBuf.writeByteArray(p_334846_, $$2.mapColors);
         } else {
            p_334846_.writeByte(0);
         }
      }

      private static Optional<MapItemSavedData.MapPatch> read(ByteBuf p_332582_) {
         int $$1 = p_332582_.readUnsignedByte();
         if ($$1 > 0) {
            int $$2 = p_332582_.readUnsignedByte();
            int $$3 = p_332582_.readUnsignedByte();
            int $$4 = p_332582_.readUnsignedByte();
            byte[] $$5 = FriendlyByteBuf.readByteArray(p_332582_);
            return Optional.of(new MapItemSavedData.MapPatch($$3, $$4, $$1, $$2, $$5));
         } else {
            return Optional.empty();
         }
      }

      public void applyToMap(MapItemSavedData pSavedData) {
         for (int $$1 = 0; $$1 < this.width; $$1++) {
            for (int $$2 = 0; $$2 < this.height; $$2++) {
               pSavedData.setColor(this.startX + $$1, this.startY + $$2, this.mapColors[$$1 + $$2 * this.width]);
            }
         }
      }
   }
}
