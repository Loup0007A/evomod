package net.minecraft.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record LockCode(String key) {
   public static final LockCode NO_LOCK = new LockCode("");
   public static final Codec<LockCode> CODEC = Codec.STRING.xmap(LockCode::new, LockCode::key);
   public static final String TAG_LOCK = "Lock";

   public boolean unlocksWith(ItemStack pStack) {
      if (this.key.isEmpty()) {
         return true;
      } else {
         Component $$1 = pStack.get(DataComponents.CUSTOM_NAME);
         return $$1 != null && this.key.equals($$1.getString());
      }
   }

   public void addToTag(CompoundTag pNbt) {
      if (!this.key.isEmpty()) {
         pNbt.putString("Lock", this.key);
      }
   }

   public static LockCode fromTag(CompoundTag pNbt) {
      return pNbt.contains("Lock", 8) ? new LockCode(pNbt.getString("Lock")) : NO_LOCK;
   }
}
