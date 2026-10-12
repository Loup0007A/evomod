package net.minecraft.world.level.timers;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;

public class FunctionTagCallback implements TimerCallback<MinecraftServer> {
   final ResourceLocation tagId;

   public FunctionTagCallback(ResourceLocation pTagId) {
      this.tagId = pTagId;
   }

   public void handle(MinecraftServer pObj, TimerQueue<MinecraftServer> pManager, long pGameTime) {
      ServerFunctionManager $$3 = pObj.getFunctions();

      for (CommandFunction<CommandSourceStack> $$5 : $$3.getTag(this.tagId)) {
         $$3.execute($$5, $$3.getGameLoopSender());
      }
   }

   public static class Serializer extends TimerCallback.Serializer<MinecraftServer, FunctionTagCallback> {
      public Serializer() {
         super(ResourceLocation.withDefaultNamespace("function_tag"), FunctionTagCallback.class);
      }

      public void serialize(CompoundTag p_82206_, FunctionTagCallback p_82207_) {
         p_82206_.putString("Name", p_82207_.tagId.toString());
      }

      public FunctionTagCallback deserialize(CompoundTag p_82204_) {
         ResourceLocation $$1 = ResourceLocation.parse(p_82204_.getString("Name"));
         return new FunctionTagCallback($$1);
      }
   }
}
