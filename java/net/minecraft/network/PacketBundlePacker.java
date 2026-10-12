package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;

public class PacketBundlePacker extends MessageToMessageDecoder<Packet<?>> {
   private final BundlerInfo bundlerInfo;
   @Nullable
   private BundlerInfo.Bundler currentBundler;

   public PacketBundlePacker(BundlerInfo pBundlerInfo) {
      this.bundlerInfo = pBundlerInfo;
   }

   protected void decode(ChannelHandlerContext pContext, Packet<?> pPacket, List<Object> p_265368_) throws Exception {
      if (this.currentBundler != null) {
         verifyNonTerminalPacket(pPacket);
         Packet<?> $$3 = this.currentBundler.addPacket(pPacket);
         if ($$3 != null) {
            this.currentBundler = null;
            p_265368_.add($$3);
         }
      } else {
         BundlerInfo.Bundler $$4 = this.bundlerInfo.startPacketBundling(pPacket);
         if ($$4 != null) {
            verifyNonTerminalPacket(pPacket);
            this.currentBundler = $$4;
         } else {
            p_265368_.add(pPacket);
            if (pPacket.isTerminal()) {
               pContext.pipeline().remove(pContext.name());
            }
         }
      }
   }

   private static void verifyNonTerminalPacket(Packet<?> pPacket) {
      if (pPacket.isTerminal()) {
         throw new DecoderException("Terminal message received in bundle");
      }
   }
}
