package net.minecraft.world.scores;

import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;

public class Score implements ReadOnlyScoreInfo {
   private static final String TAG_SCORE = "Score";
   private static final String TAG_LOCKED = "Locked";
   private static final String TAG_DISPLAY = "display";
   private static final String TAG_FORMAT = "format";
   private int value;
   private boolean locked = true;
   @Nullable
   private Component display;
   @Nullable
   private NumberFormat numberFormat;

   @Override
   public int value() {
      return this.value;
   }

   public void value(int pValue) {
      this.value = pValue;
   }

   @Override
   public boolean isLocked() {
      return this.locked;
   }

   public void setLocked(boolean pLocked) {
      this.locked = pLocked;
   }

   @Nullable
   public Component display() {
      return this.display;
   }

   public void display(@Nullable Component pDisplay) {
      this.display = pDisplay;
   }

   @Nullable
   @Override
   public NumberFormat numberFormat() {
      return this.numberFormat;
   }

   public void numberFormat(@Nullable NumberFormat pNumberFormat) {
      this.numberFormat = pNumberFormat;
   }

   public CompoundTag write(HolderLookup.Provider pLevelRegistry) {
      CompoundTag $$1 = new CompoundTag();
      $$1.putInt("Score", this.value);
      $$1.putBoolean("Locked", this.locked);
      if (this.display != null) {
         $$1.putString("display", Component.Serializer.toJson(this.display, pLevelRegistry));
      }

      if (this.numberFormat != null) {
         NumberFormatTypes.CODEC
            .encodeStart(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), this.numberFormat)
            .ifSuccess(p_309357_ -> $$1.put("format", p_309357_));
      }

      return $$1;
   }

   public static Score read(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
      Score $$2 = new Score();
      $$2.value = pTag.getInt("Score");
      $$2.locked = pTag.getBoolean("Locked");
      if (pTag.contains("display", 8)) {
         $$2.display = Component.Serializer.fromJson(pTag.getString("display"), pLevelRegistry);
      }

      if (pTag.contains("format", 10)) {
         NumberFormatTypes.CODEC
            .parse(pLevelRegistry.createSerializationContext(NbtOps.INSTANCE), pTag.get("format"))
            .ifSuccess(p_309359_ -> $$2.numberFormat = p_309359_);
      }

      return $$2;
   }
}
