package net.minecraft.client.quickplay;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuickPlay {
   public static final Component ERROR_TITLE = Component.translatable("quickplay.error.title");
   private static final Component INVALID_IDENTIFIER = Component.translatable("quickplay.error.invalid_identifier");
   private static final Component REALM_CONNECT = Component.translatable("quickplay.error.realm_connect");
   private static final Component REALM_PERMISSION = Component.translatable("quickplay.error.realm_permission");
   private static final Component TO_TITLE = Component.translatable("gui.toTitle");
   private static final Component TO_WORLD_LIST = Component.translatable("gui.toWorld");
   private static final Component TO_REALMS_LIST = Component.translatable("gui.toRealms");

   public static void connect(Minecraft pMinecraft, GameConfig.QuickPlayData pQuickPlayData, RealmsClient pRealmsClient) {
      String $$3 = pQuickPlayData.singleplayer();
      String $$4 = pQuickPlayData.multiplayer();
      String $$5 = pQuickPlayData.realms();
      if (!StringUtil.isBlank($$3)) {
         joinSingleplayerWorld(pMinecraft, $$3);
      } else if (!StringUtil.isBlank($$4)) {
         joinMultiplayerWorld(pMinecraft, $$4);
      } else if (!StringUtil.isBlank($$5)) {
         joinRealmsWorld(pMinecraft, pRealmsClient, $$5);
      }
   }

   private static void joinSingleplayerWorld(Minecraft pMinecraft, String pLevelName) {
      if (!pMinecraft.getLevelSource().levelExists(pLevelName)) {
         Screen $$2 = new SelectWorldScreen(new TitleScreen());
         pMinecraft.setScreen(new DisconnectedScreen($$2, ERROR_TITLE, INVALID_IDENTIFIER, TO_WORLD_LIST));
      } else {
         pMinecraft.createWorldOpenFlows().openWorld(pLevelName, () -> pMinecraft.setScreen(new TitleScreen()));
      }
   }

   private static void joinMultiplayerWorld(Minecraft pMinecraft, String pIp) {
      ServerList $$2 = new ServerList(pMinecraft);
      $$2.load();
      ServerData $$3 = $$2.get(pIp);
      if ($$3 == null) {
         $$3 = new ServerData(I18n.get("selectServer.defaultName"), pIp, ServerData.Type.OTHER);
         $$2.add($$3, true);
         $$2.save();
      }

      ServerAddress $$4 = ServerAddress.parseString(pIp);
      ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), pMinecraft, $$4, $$3, true, null);
   }

   private static void joinRealmsWorld(Minecraft pMinecraft, RealmsClient pRealmsClient, String pServerId) {
      long $$3;
      RealmsServerList $$4;
      try {
         $$3 = Long.parseLong(pServerId);
         $$4 = pRealmsClient.listRealms();
      } catch (NumberFormatException var9) {
         Screen $$6 = new RealmsMainScreen(new TitleScreen());
         pMinecraft.setScreen(new DisconnectedScreen($$6, ERROR_TITLE, INVALID_IDENTIFIER, TO_REALMS_LIST));
         return;
      } catch (RealmsServiceException var10) {
         Screen $$8 = new TitleScreen();
         pMinecraft.setScreen(new DisconnectedScreen($$8, ERROR_TITLE, REALM_CONNECT, TO_TITLE));
         return;
      }

      RealmsServer $$11 = $$4.servers.stream().filter(p_279424_ -> p_279424_.id == $$3).findFirst().orElse(null);
      if ($$11 == null) {
         Screen $$12 = new RealmsMainScreen(new TitleScreen());
         pMinecraft.setScreen(new DisconnectedScreen($$12, ERROR_TITLE, REALM_PERMISSION, TO_REALMS_LIST));
      } else {
         TitleScreen $$13 = new TitleScreen();
         GetServerDetailsTask $$14 = new GetServerDetailsTask($$13, $$11);
         pMinecraft.setScreen(new RealmsLongRunningMcoTaskScreen($$13, $$14));
      }
   }
}
