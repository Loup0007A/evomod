package net.minecraft.world.level.saveddata.maps;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public class MapFrame {
   private final BlockPos pos;
   private final int rotation;
   private final int entityId;

   public MapFrame(BlockPos pPos, int pRotation, int pEntityId) {
      this.pos = pPos;
      this.rotation = pRotation;
      this.entityId = pEntityId;
   }

   @Nullable
   public static MapFrame load(CompoundTag pCompoundTag) {
      Optional<BlockPos> $$1 = NbtUtils.readBlockPos(pCompoundTag, "pos");
      if ($$1.isEmpty()) {
         return null;
      } else {
         int $$2 = pCompoundTag.getInt("rotation");
         int $$3 = pCompoundTag.getInt("entity_id");
         return new MapFrame($$1.get(), $$2, $$3);
      }
   }

   public CompoundTag save() {
      CompoundTag $$0 = new CompoundTag();
      $$0.put("pos", NbtUtils.writeBlockPos(this.pos));
      $$0.putInt("rotation", this.rotation);
      $$0.putInt("entity_id", this.entityId);
      return $$0;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public int getRotation() {
      return this.rotation;
   }

   public int getEntityId() {
      return this.entityId;
   }

   public String getId() {
      return frameId(this.pos);
   }

   public static String frameId(BlockPos pPos) {
      return "frame-" + pPos.getX() + "," + pPos.getY() + "," + pPos.getZ();
   }
}
