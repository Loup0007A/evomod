package net.minecraftforge.network.filters;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.channel.ChannelHandler.Sharable;
import java.util.Collections;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.gametest.framework.TestClassNameArgument;
import net.minecraft.gametest.framework.TestFunctionArgument;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

@Sharable
public class VanillaConnectionNetworkFilter extends VanillaPacketFilter {
   public VanillaConnectionNetworkFilter() {
      super(
         ImmutableMap.builder()
            .put(handler(ClientboundUpdateAttributesPacket.class, VanillaConnectionNetworkFilter::filterEntityProperties))
            .put(handler(ClientboundCommandsPacket.class, VanillaConnectionNetworkFilter::filterCommandList))
            .build()
      );
   }

   @Override
   protected boolean isNecessary(Connection connection) {
      return NetworkContext.get(connection).getType() == ConnectionType.VANILLA;
   }

   @NotNull
   private static ClientboundUpdateAttributesPacket filterEntityProperties(ClientboundUpdateAttributesPacket msg) {
      ClientboundUpdateAttributesPacket newPacket = new ClientboundUpdateAttributesPacket(msg.getEntityId(), Collections.emptyList());
      msg.getValues().stream().filter(snapshot -> {
         ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(snapshot.attribute().get());
         return key != null && key.getNamespace().equals("minecraft");
      }).forEach(snapshot -> newPacket.getValues().add(snapshot));
      return newPacket;
   }

   @NotNull
   private static ClientboundCommandsPacket filterCommandList(ClientboundCommandsPacket packet) {
      CommandBuildContext commandBuildContext = Commands.createValidationContext(VanillaRegistries.createLookup());
      RootCommandNode<SharedSuggestionProvider> root = packet.getRoot(commandBuildContext);
      RootCommandNode<SharedSuggestionProvider> newRoot = CommandTreeCleaner.cleanArgumentTypes(root, argType -> {
         if (!(argType instanceof TestFunctionArgument) && !(argType instanceof TestClassNameArgument)) {
            ArgumentTypeInfo<?, ?> info = ArgumentTypeInfos.byClass(argType);
            ResourceLocation id = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(info);
            return id != null && (id.getNamespace().equals("minecraft") || id.getNamespace().equals("brigadier"));
         } else {
            return false;
         }
      });
      return new ClientboundCommandsPacket(newRoot);
   }
}
