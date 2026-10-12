package net.minecraftforge.common.util;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public interface INBTBuilder {
   default INBTBuilder.Builder nbt() {
      return new INBTBuilder.Builder();
   }

   public static class Builder implements INBTBuilder {
      private final CompoundTag tag = new CompoundTag();

      public CompoundTag build() {
         return this.tag;
      }

      public INBTBuilder.Builder tag(String key, Tag value) {
         this.tag.put(key, value);
         return this;
      }

      public INBTBuilder.Builder putByte(String key, byte value) {
         this.tag.putByte(key, value);
         return this;
      }

      public INBTBuilder.Builder putShort(String key, short value) {
         this.tag.putShort(key, value);
         return this;
      }

      public INBTBuilder.Builder putInt(String key, int value) {
         this.tag.putInt(key, value);
         return this;
      }

      public INBTBuilder.Builder putLong(String key, long value) {
         this.tag.putLong(key, value);
         return this;
      }

      public INBTBuilder.Builder putFloat(String key, float value) {
         this.tag.putFloat(key, value);
         return this;
      }

      public INBTBuilder.Builder putDouble(String key, double value) {
         this.tag.putDouble(key, value);
         return this;
      }

      public INBTBuilder.Builder putByteArray(String key, byte... value) {
         this.tag.putByteArray(key, value);
         return this;
      }

      public INBTBuilder.Builder putByteArray(String key, List<Byte> value) {
         this.tag.putByteArray(key, value);
         return this;
      }

      public INBTBuilder.Builder putIntArray(String key, int... value) {
         this.tag.putIntArray(key, value);
         return this;
      }

      public INBTBuilder.Builder putIntArray(String key, List<Integer> value) {
         this.tag.putIntArray(key, value);
         return this;
      }

      public INBTBuilder.Builder putLongArray(String key, long... value) {
         this.tag.putLongArray(key, value);
         return this;
      }

      public INBTBuilder.Builder putLongArray(String key, List<Long> value) {
         this.tag.putLongArray(key, value);
         return this;
      }

      public INBTBuilder.Builder put(String key, boolean value) {
         this.tag.putBoolean(key, value);
         return this;
      }

      public INBTBuilder.Builder put(String key, String value) {
         this.tag.putString(key, value);
         return this;
      }

      public INBTBuilder.Builder put(String key, UUID value) {
         this.tag.putUUID(key, value);
         return this;
      }
   }
}
