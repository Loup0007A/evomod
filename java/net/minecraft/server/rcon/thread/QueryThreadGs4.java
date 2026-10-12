package net.minecraft.server.rcon.thread;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.NetworkDataOutputStream;
import net.minecraft.server.rcon.PktUtils;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

public class QueryThreadGs4 extends GenericThread {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String GAME_TYPE = "SMP";
   private static final String GAME_ID = "MINECRAFT";
   private static final long CHALLENGE_CHECK_INTERVAL = 30000L;
   private static final long RESPONSE_CACHE_TIME = 5000L;
   private long lastChallengeCheck;
   private final int port;
   private final int serverPort;
   private final int maxPlayers;
   private final String serverName;
   private final String worldName;
   private DatagramSocket socket;
   private final byte[] buffer = new byte[1460];
   private String hostIp;
   private String serverIp;
   private final Map<SocketAddress, QueryThreadGs4.RequestChallenge> validChallenges;
   private final NetworkDataOutputStream rulesResponse;
   private long lastRulesResponse;
   private final ServerInterface serverInterface;

   private QueryThreadGs4(ServerInterface pServerInterface, int pPort) {
      super("Query Listener");
      this.serverInterface = pServerInterface;
      this.port = pPort;
      this.serverIp = pServerInterface.getServerIp();
      this.serverPort = pServerInterface.getServerPort();
      this.serverName = pServerInterface.getServerName();
      this.maxPlayers = pServerInterface.getMaxPlayers();
      this.worldName = pServerInterface.getLevelIdName();
      this.lastRulesResponse = 0L;
      this.hostIp = "0.0.0.0";
      if (!this.serverIp.isEmpty() && !this.hostIp.equals(this.serverIp)) {
         this.hostIp = this.serverIp;
      } else {
         this.serverIp = "0.0.0.0";

         try {
            InetAddress $$2 = InetAddress.getLocalHost();
            this.hostIp = $$2.getHostAddress();
         } catch (UnknownHostException var4) {
            LOGGER.warn("Unable to determine local host IP, please set server-ip in server.properties", var4);
         }
      }

      this.rulesResponse = new NetworkDataOutputStream(1460);
      this.validChallenges = Maps.newHashMap();
   }

   @Nullable
   public static QueryThreadGs4 create(ServerInterface pServerInterface) {
      int $$1 = pServerInterface.getProperties().queryPort;
      if (0 < $$1 && 65535 >= $$1) {
         QueryThreadGs4 $$2 = new QueryThreadGs4(pServerInterface, $$1);
         return !$$2.start() ? null : $$2;
      } else {
         LOGGER.warn("Invalid query port {} found in server.properties (queries disabled)", $$1);
         return null;
      }
   }

   private void sendTo(byte[] pData, DatagramPacket pRequestPacket) throws IOException {
      this.socket.send(new DatagramPacket(pData, pData.length, pRequestPacket.getSocketAddress()));
   }

   private boolean processPacket(DatagramPacket pRequestPacket) throws IOException {
      byte[] $$1 = pRequestPacket.getData();
      int $$2 = pRequestPacket.getLength();
      SocketAddress $$3 = pRequestPacket.getSocketAddress();
      LOGGER.debug("Packet len {} [{}]", $$2, $$3);
      if (3 <= $$2 && -2 == $$1[0] && -3 == $$1[1]) {
         LOGGER.debug("Packet '{}' [{}]", PktUtils.toHexString($$1[2]), $$3);
         switch ($$1[2]) {
            case 0:
               if (!this.validChallenge(pRequestPacket)) {
                  LOGGER.debug("Invalid challenge [{}]", $$3);
                  return false;
               } else if (15 == $$2) {
                  this.sendTo(this.buildRuleResponse(pRequestPacket), pRequestPacket);
                  LOGGER.debug("Rules [{}]", $$3);
               } else {
                  NetworkDataOutputStream $$4 = new NetworkDataOutputStream(1460);
                  $$4.write(0);
                  $$4.writeBytes(this.getIdentBytes(pRequestPacket.getSocketAddress()));
                  $$4.writeString(this.serverName);
                  $$4.writeString("SMP");
                  $$4.writeString(this.worldName);
                  $$4.writeString(Integer.toString(this.serverInterface.getPlayerCount()));
                  $$4.writeString(Integer.toString(this.maxPlayers));
                  $$4.writeShort((short)this.serverPort);
                  $$4.writeString(this.hostIp);
                  this.sendTo($$4.toByteArray(), pRequestPacket);
                  LOGGER.debug("Status [{}]", $$3);
               }
            default:
               return true;
            case 9:
               this.sendChallenge(pRequestPacket);
               LOGGER.debug("Challenge [{}]", $$3);
               return true;
         }
      } else {
         LOGGER.debug("Invalid packet [{}]", $$3);
         return false;
      }
   }

   private byte[] buildRuleResponse(DatagramPacket pRequestPacket) throws IOException {
      long $$1 = Util.getMillis();
      if ($$1 < this.lastRulesResponse + 5000L) {
         byte[] $$2 = this.rulesResponse.toByteArray();
         byte[] $$3 = this.getIdentBytes(pRequestPacket.getSocketAddress());
         $$2[1] = $$3[0];
         $$2[2] = $$3[1];
         $$2[3] = $$3[2];
         $$2[4] = $$3[3];
         return $$2;
      } else {
         this.lastRulesResponse = $$1;
         this.rulesResponse.reset();
         this.rulesResponse.write(0);
         this.rulesResponse.writeBytes(this.getIdentBytes(pRequestPacket.getSocketAddress()));
         this.rulesResponse.writeString("splitnum");
         this.rulesResponse.write(128);
         this.rulesResponse.write(0);
         this.rulesResponse.writeString("hostname");
         this.rulesResponse.writeString(this.serverName);
         this.rulesResponse.writeString("gametype");
         this.rulesResponse.writeString("SMP");
         this.rulesResponse.writeString("game_id");
         this.rulesResponse.writeString("MINECRAFT");
         this.rulesResponse.writeString("version");
         this.rulesResponse.writeString(this.serverInterface.getServerVersion());
         this.rulesResponse.writeString("plugins");
         this.rulesResponse.writeString(this.serverInterface.getPluginNames());
         this.rulesResponse.writeString("map");
         this.rulesResponse.writeString(this.worldName);
         this.rulesResponse.writeString("numplayers");
         this.rulesResponse.writeString(this.serverInterface.getPlayerCount() + "");
         this.rulesResponse.writeString("maxplayers");
         this.rulesResponse.writeString(this.maxPlayers + "");
         this.rulesResponse.writeString("hostport");
         this.rulesResponse.writeString(this.serverPort + "");
         this.rulesResponse.writeString("hostip");
         this.rulesResponse.writeString(this.hostIp);
         this.rulesResponse.write(0);
         this.rulesResponse.write(1);
         this.rulesResponse.writeString("player_");
         this.rulesResponse.write(0);
         String[] $$4 = this.serverInterface.getPlayerNames();

         for (String $$5 : $$4) {
            this.rulesResponse.writeString($$5);
         }

         this.rulesResponse.write(0);
         return this.rulesResponse.toByteArray();
      }
   }

   private byte[] getIdentBytes(SocketAddress pAddress) {
      return this.validChallenges.get(pAddress).getIdentBytes();
   }

   private Boolean validChallenge(DatagramPacket pRequestPacket) {
      SocketAddress $$1 = pRequestPacket.getSocketAddress();
      if (!this.validChallenges.containsKey($$1)) {
         return false;
      } else {
         byte[] $$2 = pRequestPacket.getData();
         return this.validChallenges.get($$1).getChallenge() == PktUtils.intFromNetworkByteArray($$2, 7, pRequestPacket.getLength());
      }
   }

   private void sendChallenge(DatagramPacket pRequestPacket) throws IOException {
      QueryThreadGs4.RequestChallenge $$1 = new QueryThreadGs4.RequestChallenge(pRequestPacket);
      this.validChallenges.put(pRequestPacket.getSocketAddress(), $$1);
      this.sendTo($$1.getChallengeBytes(), pRequestPacket);
   }

   private void pruneChallenges() {
      if (this.running) {
         long $$0 = Util.getMillis();
         if ($$0 >= this.lastChallengeCheck + 30000L) {
            this.lastChallengeCheck = $$0;
            this.validChallenges.values().removeIf(p_11546_ -> p_11546_.before($$0));
         }
      }
   }

   @Override
   public void run() {
      LOGGER.info("Query running on {}:{}", this.serverIp, this.port);
      this.lastChallengeCheck = Util.getMillis();
      DatagramPacket $$0 = new DatagramPacket(this.buffer, this.buffer.length);

      try {
         while (this.running) {
            try {
               this.socket.receive($$0);
               this.pruneChallenges();
               this.processPacket($$0);
            } catch (SocketTimeoutException var8) {
               this.pruneChallenges();
            } catch (PortUnreachableException var9) {
            } catch (IOException var10) {
               this.recoverSocketError(var10);
            }
         }
      } finally {
         LOGGER.debug("closeSocket: {}:{}", this.serverIp, this.port);
         this.socket.close();
      }
   }

   @Override
   public boolean start() {
      if (this.running) {
         return true;
      } else {
         return !this.initSocket() ? false : super.start();
      }
   }

   private void recoverSocketError(Exception pException) {
      if (this.running) {
         LOGGER.warn("Unexpected exception", pException);
         if (!this.initSocket()) {
            LOGGER.error("Failed to recover from exception, shutting down!");
            this.running = false;
         }
      }
   }

   private boolean initSocket() {
      try {
         this.socket = new DatagramSocket(this.port, InetAddress.getByName(this.serverIp));
         this.socket.setSoTimeout(500);
         return true;
      } catch (Exception var2) {
         LOGGER.warn("Unable to initialise query system on {}:{}", new Object[]{this.serverIp, this.port, var2});
         return false;
      }
   }

   static class RequestChallenge {
      private final long time = new Date().getTime();
      private final int challenge;
      private final byte[] identBytes;
      private final byte[] challengeBytes;
      private final String ident;

      public RequestChallenge(DatagramPacket pDatagramPacket) {
         byte[] $$1 = pDatagramPacket.getData();
         this.identBytes = new byte[4];
         this.identBytes[0] = $$1[3];
         this.identBytes[1] = $$1[4];
         this.identBytes[2] = $$1[5];
         this.identBytes[3] = $$1[6];
         this.ident = new String(this.identBytes, StandardCharsets.UTF_8);
         this.challenge = RandomSource.create().nextInt(16777216);
         this.challengeBytes = String.format(Locale.ROOT, "\t%s%d\u0000", this.ident, this.challenge).getBytes(StandardCharsets.UTF_8);
      }

      public Boolean before(long pCurrentTime) {
         return this.time < pCurrentTime;
      }

      public int getChallenge() {
         return this.challenge;
      }

      public byte[] getChallengeBytes() {
         return this.challengeBytes;
      }

      public byte[] getIdentBytes() {
         return this.identBytes;
      }

      public String getIdent() {
         return this.ident;
      }
   }
}
