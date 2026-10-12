package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class FloatArgumentInfo implements ArgumentTypeInfo<FloatArgumentType, FloatArgumentInfo.Template> {
   public void serializeToNetwork(FloatArgumentInfo.Template pTemplate, FriendlyByteBuf pBuffer) {
      boolean $$2 = pTemplate.min != -Float.MAX_VALUE;
      boolean $$3 = pTemplate.max != Float.MAX_VALUE;
      pBuffer.writeByte(ArgumentUtils.createNumberFlags($$2, $$3));
      if ($$2) {
         pBuffer.writeFloat(pTemplate.min);
      }

      if ($$3) {
         pBuffer.writeFloat(pTemplate.max);
      }
   }

   public FloatArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
      byte $$1 = pBuffer.readByte();
      float $$2 = ArgumentUtils.numberHasMin($$1) ? pBuffer.readFloat() : -Float.MAX_VALUE;
      float $$3 = ArgumentUtils.numberHasMax($$1) ? pBuffer.readFloat() : Float.MAX_VALUE;
      return new FloatArgumentInfo.Template($$2, $$3);
   }

   public void serializeToJson(FloatArgumentInfo.Template pTemplate, JsonObject pJson) {
      if (pTemplate.min != -Float.MAX_VALUE) {
         pJson.addProperty("min", pTemplate.min);
      }

      if (pTemplate.max != Float.MAX_VALUE) {
         pJson.addProperty("max", pTemplate.max);
      }
   }

   public FloatArgumentInfo.Template unpack(FloatArgumentType pArgument) {
      return new FloatArgumentInfo.Template(pArgument.getMinimum(), pArgument.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<FloatArgumentType> {
      final float min;
      final float max;

      Template(final float pMin, final float pMax) {
         this.min = pMin;
         this.max = pMax;
      }

      public FloatArgumentType instantiate(CommandBuildContext pContext) {
         return FloatArgumentType.floatArg(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<FloatArgumentType, ?> type() {
         return FloatArgumentInfo.this;
      }
   }
}
