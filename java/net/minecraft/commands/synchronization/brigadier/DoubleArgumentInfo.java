package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class DoubleArgumentInfo implements ArgumentTypeInfo<DoubleArgumentType, DoubleArgumentInfo.Template> {
   public void serializeToNetwork(DoubleArgumentInfo.Template pTemplate, FriendlyByteBuf pBuffer) {
      boolean $$2 = pTemplate.min != -Double.MAX_VALUE;
      boolean $$3 = pTemplate.max != Double.MAX_VALUE;
      pBuffer.writeByte(ArgumentUtils.createNumberFlags($$2, $$3));
      if ($$2) {
         pBuffer.writeDouble(pTemplate.min);
      }

      if ($$3) {
         pBuffer.writeDouble(pTemplate.max);
      }
   }

   public DoubleArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
      byte $$1 = pBuffer.readByte();
      double $$2 = ArgumentUtils.numberHasMin($$1) ? pBuffer.readDouble() : -Double.MAX_VALUE;
      double $$3 = ArgumentUtils.numberHasMax($$1) ? pBuffer.readDouble() : Double.MAX_VALUE;
      return new DoubleArgumentInfo.Template($$2, $$3);
   }

   public void serializeToJson(DoubleArgumentInfo.Template pTemplate, JsonObject pJson) {
      if (pTemplate.min != -Double.MAX_VALUE) {
         pJson.addProperty("min", pTemplate.min);
      }

      if (pTemplate.max != Double.MAX_VALUE) {
         pJson.addProperty("max", pTemplate.max);
      }
   }

   public DoubleArgumentInfo.Template unpack(DoubleArgumentType pArgument) {
      return new DoubleArgumentInfo.Template(pArgument.getMinimum(), pArgument.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<DoubleArgumentType> {
      final double min;
      final double max;

      Template(final double pMin, final double pMax) {
         this.min = pMin;
         this.max = pMax;
      }

      public DoubleArgumentType instantiate(CommandBuildContext pContext) {
         return DoubleArgumentType.doubleArg(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<DoubleArgumentType, ?> type() {
         return DoubleArgumentInfo.this;
      }
   }
}
