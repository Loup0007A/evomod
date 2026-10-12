package net.minecraft.realms;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.RealmsServer;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConnect {
   static final Logger LOGGER = LogUtils.getLogger();
   final Screen onlineScreen;
   volatile boolean aborted;
   @Nullable
   Connection connection;

   public RealmsConnect(Screen pOnlineScreen) {
      this.onlineScreen = pOnlineScreen;
   }

   public void connect(final RealmsServer pServer, ServerAddress pAddress) {
      final Minecraft $$2 = Minecraft.getInstance();
      $$2.prepareForMultiplayer();
      $$2.getNarrator().sayNow(Component.translatable("mco.connect.success"));
      final String $$3 = pAddress.getHost();
      final int $$4 = pAddress.getPort();
      (new Thread("Realms-connect-task") {
            @Override
            public void run() {
               InetSocketAddress $$0 = null;

               try {
                  $$0 = new InetSocketAddress($$3, $$4);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection = Connection.connectToServer($$0, $$2.options.useNativeTransport(), $$2.getDebugOverlay().getBandwidthLogger());
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  ClientHandshakePacketListenerImpl $$1 = new ClientHandshakePacketListenerImpl(
                     RealmsConnect.this.connection, $$2, pServer.toServerData($$3), RealmsConnect.this.onlineScreen, false, null, p_120726_ -> {
                     }, null
                  );
                  if (pServer.isMinigameActive()) {
                     $$1.setMinigameName(pServer.minigameName);
                  }

                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.initiateServerboundPlayConnection($$3, $$4, $$1);
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.this.connection.send(new ServerboundHelloPacket($$2.getUser().getName(), $$2.getUser().getProfileId()));
                  $$2.updateReportEnvironment(ReportEnvironment.realm(pServer));
                  $$2.quickPlayLog().setWorldData(QuickPlayLog.Type.REALMS, String.valueOf(pServer.id), pServer.name);
                  $$2.getDownloadedPackSource().configureForServerControl(RealmsConnect.this.connection, ServerPackManager.PackPromptStatus.ALLOWED);
               } catch (Exception var5) {
                  $$2.getDownloadedPackSource().cleanupAfterDisconnect();
                  if (RealmsConnect.this.aborted) {
                     return;
                  }

                  RealmsConnect.LOGGER.error("Couldn't connect to world", var5);
                  String $$3 = var5.toString();
                  if ($$0 != null) {
                     String $$4 = $$0 + ":" + $$4;
                     $$3 = $$3.replaceAll($$4, "");
                  }

                  DisconnectedRealmsScreen $$5 = new DisconnectedRealmsScreen(
                     RealmsConnect.this.onlineScreen, CommonComponents.CONNECT_FAILED, Component.translatable("disconnect.genericReason", $$3)
                  );
                  $$2.execute(() -> $$2.setScreen($$5));
               }
            }
         })
         .start();
   }

   public void abort() {
      this.aborted = true;
      if (this.connection != null && this.connection.isConnected()) {
         this.connection.disconnect(Component.translatable("disconnect.genericReason"));
         this.connection.handleDisconnection();
      }
   }

   public void tick() {
      if (this.connection != null) {
         if (this.connection.isConnected()) {
            this.connection.tick();
         } else {
            this.connection.handleDisconnection();
         }
      }
   }
}
