package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class IntegerArgumentInfo implements ArgumentTypeInfo<IntegerArgumentType, IntegerArgumentInfo.Template> {
   public void serializeToNetwork(IntegerArgumentInfo.Template pTemplate, FriendlyByteBuf pBuffer) {
      boolean $$2 = pTemplate.min != Integer.MIN_VALUE;
      boolean $$3 = pTemplate.max != Integer.MAX_VALUE;
      pBuffer.writeByte(ArgumentUtils.createNumberFlags($$2, $$3));
      if ($$2) {
         pBuffer.writeInt(pTemplate.min);
      }

      if ($$3) {
         pBuffer.writeInt(pTemplate.max);
      }
   }

   public IntegerArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
      byte $$1 = pBuffer.readByte();
      int $$2 = ArgumentUtils.numberHasMin($$1) ? pBuffer.readInt() : Integer.MIN_VALUE;
      int $$3 = ArgumentUtils.numberHasMax($$1) ? pBuffer.readInt() : Integer.MAX_VALUE;
      return new IntegerArgumentInfo.Template($$2, $$3);
   }

   public void serializeToJson(IntegerArgumentInfo.Template pTemplate, JsonObject pJson) {
      if (pTemplate.min != Integer.MIN_VALUE) {
         pJson.addProperty("min", pTemplate.min);
      }

      if (pTemplate.max != Integer.MAX_VALUE) {
         pJson.addProperty("max", pTemplate.max);
      }
   }

   public IntegerArgumentInfo.Template unpack(IntegerArgumentType pArgument) {
      return new IntegerArgumentInfo.Template(pArgument.getMinimum(), pArgument.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<IntegerArgumentType> {
      final int min;
      final int max;

      Template(final int pMin, final int pMax) {
         this.min = pMin;
         this.max = pMax;
      }

      public IntegerArgumentType instantiate(CommandBuildContext pContext) {
         return IntegerArgumentType.integer(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<IntegerArgumentType, ?> type() {
         return IntegerArgumentInfo.this;
      }
   }
}
