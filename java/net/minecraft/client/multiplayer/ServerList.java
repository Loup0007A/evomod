package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ProcessorMailbox<Runnable> IO_MAILBOX = ProcessorMailbox.create(Util.backgroundExecutor(), "server-list-io");
   private static final int MAX_HIDDEN_SERVERS = 16;
   private final Minecraft minecraft;
   private final List<ServerData> serverList = Lists.newArrayList();
   private final List<ServerData> hiddenServerList = Lists.newArrayList();

   public ServerList(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void load() {
      try {
         this.serverList.clear();
         this.hiddenServerList.clear();
         CompoundTag $$0 = NbtIo.read(this.minecraft.gameDirectory.toPath().resolve("servers.dat"));
         if ($$0 == null) {
            return;
         }

         ListTag $$1 = $$0.getList("servers", 10);

         for (int $$2 = 0; $$2 < $$1.size(); $$2++) {
            CompoundTag $$3 = $$1.getCompound($$2);
            ServerData $$4 = ServerData.read($$3);
            if ($$3.getBoolean("hidden")) {
               this.hiddenServerList.add($$4);
            } else {
               this.serverList.add($$4);
            }
         }
      } catch (Exception var6) {
         LOGGER.error("Couldn't load server list", var6);
      }
   }

   public void save() {
      try {
         ListTag $$0 = new ListTag();

         for (ServerData $$1 : this.serverList) {
            CompoundTag $$2 = $$1.write();
            $$2.putBoolean("hidden", false);
            $$0.add($$2);
         }

         for (ServerData $$3 : this.hiddenServerList) {
            CompoundTag $$4 = $$3.write();
            $$4.putBoolean("hidden", true);
            $$0.add($$4);
         }

         CompoundTag $$5 = new CompoundTag();
         $$5.put("servers", $$0);
         Path $$6 = this.minecraft.gameDirectory.toPath();
         Path $$7 = Files.createTempFile($$6, "servers", ".dat");
         NbtIo.write($$5, $$7);
         Path $$8 = $$6.resolve("servers.dat_old");
         Path $$9 = $$6.resolve("servers.dat");
         Util.safeReplaceFile($$9, $$7, $$8);
      } catch (Exception var7) {
         LOGGER.error("Couldn't save server list", var7);
      }
   }

   public ServerData get(int pIndex) {
      return this.serverList.get(pIndex);
   }

   @Nullable
   public ServerData get(String pIp) {
      for (ServerData $$1 : this.serverList) {
         if ($$1.ip.equals(pIp)) {
            return $$1;
         }
      }

      for (ServerData $$2 : this.hiddenServerList) {
         if ($$2.ip.equals(pIp)) {
            return $$2;
         }
      }

      return null;
   }

   @Nullable
   public ServerData unhide(String pIp) {
      for (int $$1 = 0; $$1 < this.hiddenServerList.size(); $$1++) {
         ServerData $$2 = this.hiddenServerList.get($$1);
         if ($$2.ip.equals(pIp)) {
            this.hiddenServerList.remove($$1);
            this.serverList.add($$2);
            return $$2;
         }
      }

      return null;
   }

   public void remove(ServerData pServerData) {
      if (!this.serverList.remove(pServerData)) {
         this.hiddenServerList.remove(pServerData);
      }
   }

   public void add(ServerData pServer, boolean pHidden) {
      if (pHidden) {
         this.hiddenServerList.add(0, pServer);

         while (this.hiddenServerList.size() > 16) {
            this.hiddenServerList.remove(this.hiddenServerList.size() - 1);
         }
      } else {
         this.serverList.add(pServer);
      }
   }

   public int size() {
      return this.serverList.size();
   }

   public void swap(int pPos1, int pPos2) {
      ServerData $$2 = this.get(pPos1);
      this.serverList.set(pPos1, this.get(pPos2));
      this.serverList.set(pPos2, $$2);
      this.save();
   }

   public void replace(int pIndex, ServerData pServer) {
      this.serverList.set(pIndex, pServer);
   }

   private static boolean set(ServerData pServer, List<ServerData> pServerList) {
      for (int $$2 = 0; $$2 < pServerList.size(); $$2++) {
         ServerData $$3 = pServerList.get($$2);
         if ($$3.name.equals(pServer.name) && $$3.ip.equals(pServer.ip)) {
            pServerList.set($$2, pServer);
            return true;
         }
      }

      return false;
   }

   public static void saveSingleServer(ServerData pServer) {
      IO_MAILBOX.tell(() -> {
         ServerList $$1 = new ServerList(Minecraft.getInstance());
         $$1.load();
         if (!set(pServer, $$1.serverList)) {
            set(pServer, $$1.hiddenServerList);
         }

         $$1.save();
      });
   }
}
