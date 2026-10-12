package net.minecraft.server.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.server.ServerInfo;
import org.slf4j.Logger;

public class LegacyQueryHandler extends ChannelInboundHandlerAdapter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerInfo server;

   public LegacyQueryHandler(ServerInfo pServer) {
      this.server = pServer;
   }

   public void channelRead(ChannelHandlerContext pContext, Object pMessage) {
      ByteBuf $$2 = (ByteBuf)pMessage;
      $$2.markReaderIndex();
      boolean $$3 = true;

      try {
         try {
            if ($$2.readUnsignedByte() != 254) {
               return;
            }

            SocketAddress $$4 = pContext.channel().remoteAddress();
            int $$5 = $$2.readableBytes();
            if ($$5 == 0) {
               LOGGER.debug("Ping: (<1.3.x) from {}", $$4);
               String $$6 = createVersion0Response(this.server);
               sendFlushAndClose(pContext, createLegacyDisconnectPacket(pContext.alloc(), $$6));
            } else {
               if ($$2.readUnsignedByte() != 1) {
                  return;
               }

               if ($$2.isReadable()) {
                  if (!readCustomPayloadPacket($$2)) {
                     return;
                  }

                  LOGGER.debug("Ping: (1.6) from {}", $$4);
               } else {
                  LOGGER.debug("Ping: (1.4-1.5.x) from {}", $$4);
               }

               String $$7 = createVersion1Response(this.server);
               sendFlushAndClose(pContext, createLegacyDisconnectPacket(pContext.alloc(), $$7));
            }

            $$2.release();
            $$3 = false;
         } catch (RuntimeException var11) {
         }
      } finally {
         if ($$3) {
            $$2.resetReaderIndex();
            pContext.channel().pipeline().remove(this);
            pContext.fireChannelRead(pMessage);
         }
      }
   }

   private static boolean readCustomPayloadPacket(ByteBuf pBuffer) {
      short $$1 = pBuffer.readUnsignedByte();
      if ($$1 != 250) {
         return false;
      } else {
         String $$2 = LegacyProtocolUtils.readLegacyString(pBuffer);
         if (!"MC|PingHost".equals($$2)) {
            return false;
         } else {
            int $$3 = pBuffer.readUnsignedShort();
            if (pBuffer.readableBytes() != $$3) {
               return false;
            } else {
               short $$4 = pBuffer.readUnsignedByte();
               if ($$4 < 73) {
                  return false;
               } else {
                  String $$5 = LegacyProtocolUtils.readLegacyString(pBuffer);
                  int $$6 = pBuffer.readInt();
                  return $$6 <= 65535;
               }
            }
         }
      }
   }

   private static String createVersion0Response(ServerInfo pServer) {
      return String.format(Locale.ROOT, "%s§%d§%d", pServer.getMotd(), pServer.getPlayerCount(), pServer.getMaxPlayers());
   }

   private static String createVersion1Response(ServerInfo pServer) {
      return String.format(
         Locale.ROOT,
         "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
         127,
         pServer.getServerVersion(),
         pServer.getMotd(),
         pServer.getPlayerCount(),
         pServer.getMaxPlayers()
      );
   }

   private static void sendFlushAndClose(ChannelHandlerContext pContext, ByteBuf pBuffer) {
      pContext.pipeline().firstContext().writeAndFlush(pBuffer).addListener(ChannelFutureListener.CLOSE);
   }

   private static ByteBuf createLegacyDisconnectPacket(ByteBufAllocator pBufferAllocator, String pReason) {
      ByteBuf $$2 = pBufferAllocator.buffer();
      $$2.writeByte(255);
      LegacyProtocolUtils.writeLegacyString($$2, pReason);
      return $$2;
   }
}
