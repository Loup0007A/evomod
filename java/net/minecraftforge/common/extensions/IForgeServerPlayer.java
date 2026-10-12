package net.minecraftforge.common.extensions;

import io.netty.buffer.Unpooled;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkInitialization;
import net.minecraftforge.network.packets.OpenContainer;

public interface IForgeServerPlayer {
   private ServerPlayer self() {
      return (ServerPlayer)this;
   }

   default void openMenu(MenuProvider containerSupplier, BlockPos pos) {
      this.openMenu(containerSupplier, buf -> buf.writeBlockPos(pos));
   }

   default void openMenu(MenuProvider containerSupplier, Consumer<FriendlyByteBuf> extraDataWriter) {
      ServerPlayer player = this.self();
      if (!player.level().isClientSide) {
         player.doCloseContainer();
         player.nextContainerCounter();
         int openContainerId = player.containerCounter;
         FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
         extraDataWriter.accept(extraData);
         extraData.readerIndex(0);
         FriendlyByteBuf output = new FriendlyByteBuf(Unpooled.buffer());
         output.writeVarInt(extraData.readableBytes());
         output.writeBytes(extraData);
         if (output.readableBytes() <= 32600 && output.readableBytes() >= 1) {
            AbstractContainerMenu c = containerSupplier.createMenu(openContainerId, player.getInventory(), player);
            if (c != null) {
               OpenContainer msg = new OpenContainer(c.getType(), openContainerId, containerSupplier.getDisplayName(), output);
               NetworkInitialization.PLAY.send(msg, player.connection.getConnection());
               player.containerMenu = c;
               player.initMenu(player.containerMenu);
               ForgeEventFactory.onPlayerOpenContainer(player, c);
            }
         } else {
            throw new IllegalArgumentException("Invalid PacketBuffer for openGui, found " + output.readableBytes() + " bytes");
         }
      }
   }
}
