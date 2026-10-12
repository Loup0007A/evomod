package com.mojang.realmsclient.client;

import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.BackupList;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.dto.PendingInvitesList;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsDescriptionDto;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerAddress;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.RealmsWorldResetDto;
import com.mojang.realmsclient.dto.ServerActivityList;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsHttpException;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.WorldGenerationInfo;
import com.mojang.util.UndashedUuid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsClient {
   public static final RealmsClient.Environment ENVIRONMENT = Optional.ofNullable(System.getenv("realms.environment"))
      .or(() -> Optional.ofNullable(System.getProperty("realms.environment")))
      .flatMap(RealmsClient.Environment::byName)
      .orElse(RealmsClient.Environment.PRODUCTION);
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String sessionId;
   private final String username;
   private final Minecraft minecraft;
   private static final String WORLDS_RESOURCE_PATH = "worlds";
   private static final String INVITES_RESOURCE_PATH = "invites";
   private static final String MCO_RESOURCE_PATH = "mco";
   private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
   private static final String ACTIVITIES_RESOURCE = "activities";
   private static final String OPS_RESOURCE = "ops";
   private static final String REGIONS_RESOURCE = "regions/ping/stat";
   private static final String TRIALS_RESOURCE = "trial";
   private static final String NOTIFICATIONS_RESOURCE = "notifications";
   private static final String PATH_LIST_ALL_REALMS = "/listUserWorldsOfType/any";
   private static final String PATH_CREATE_SNAPSHOT_REALM = "/$PARENT_WORLD_ID/createPrereleaseRealm";
   private static final String PATH_SNAPSHOT_ELIGIBLE_REALMS = "/listPrereleaseEligibleWorlds";
   private static final String PATH_INITIALIZE = "/$WORLD_ID/initialize";
   private static final String PATH_GET_ACTIVTIES = "/$WORLD_ID";
   private static final String PATH_GET_LIVESTATS = "/liveplayerlist";
   private static final String PATH_GET_SUBSCRIPTION = "/$WORLD_ID";
   private static final String PATH_OP = "/$WORLD_ID/$PROFILE_UUID";
   private static final String PATH_PUT_INTO_MINIGAMES_MODE = "/minigames/$MINIGAME_ID/$WORLD_ID";
   private static final String PATH_AVAILABLE = "/available";
   private static final String PATH_TEMPLATES = "/templates/$WORLD_TYPE";
   private static final String PATH_WORLD_JOIN = "/v1/$ID/join/pc";
   private static final String PATH_WORLD_GET = "/$ID";
   private static final String PATH_WORLD_INVITES = "/$WORLD_ID";
   private static final String PATH_WORLD_UNINVITE = "/$WORLD_ID/invite/$UUID";
   private static final String PATH_PENDING_INVITES_COUNT = "/count/pending";
   private static final String PATH_PENDING_INVITES = "/pending";
   private static final String PATH_ACCEPT_INVITE = "/accept/$INVITATION_ID";
   private static final String PATH_REJECT_INVITE = "/reject/$INVITATION_ID";
   private static final String PATH_UNINVITE_MYSELF = "/$WORLD_ID";
   private static final String PATH_WORLD_UPDATE = "/$WORLD_ID";
   private static final String PATH_SLOT = "/$WORLD_ID/slot/$SLOT_ID";
   private static final String PATH_WORLD_OPEN = "/$WORLD_ID/open";
   private static final String PATH_WORLD_CLOSE = "/$WORLD_ID/close";
   private static final String PATH_WORLD_RESET = "/$WORLD_ID/reset";
   private static final String PATH_DELETE_WORLD = "/$WORLD_ID";
   private static final String PATH_WORLD_BACKUPS = "/$WORLD_ID/backups";
   private static final String PATH_WORLD_DOWNLOAD = "/$WORLD_ID/slot/$SLOT_ID/download";
   private static final String PATH_WORLD_UPLOAD = "/$WORLD_ID/backups/upload";
   private static final String PATH_CLIENT_COMPATIBLE = "/client/compatible";
   private static final String PATH_TOS_AGREED = "/tos/agreed";
   private static final String PATH_NEWS = "/v1/news";
   private static final String PATH_MARK_NOTIFICATIONS_SEEN = "/seen";
   private static final String PATH_DISMISS_NOTIFICATIONS = "/dismiss";
   private static final GuardedSerializer GSON = new GuardedSerializer();

   public static RealmsClient create() {
      Minecraft $$0 = Minecraft.getInstance();
      return create($$0);
   }

   public static RealmsClient create(Minecraft pMinecraft) {
      String $$1 = pMinecraft.getUser().getName();
      String $$2 = pMinecraft.getUser().getSessionId();
      return new RealmsClient($$2, $$1, pMinecraft);
   }

   public RealmsClient(String pSessionId, String pUsername, Minecraft pMinecraft) {
      this.sessionId = pSessionId;
      this.username = pUsername;
      this.minecraft = pMinecraft;
      RealmsClientConfig.setProxy(pMinecraft.getProxy());
   }

   public RealmsServerList listRealms() throws RealmsServiceException {
      String $$0 = this.url("worlds");
      if (RealmsMainScreen.isSnapshot()) {
         $$0 = $$0 + "/listUserWorldsOfType/any";
      }

      String $$1 = this.execute(Request.get($$0));
      return RealmsServerList.parse($$1);
   }

   public List<RealmsServer> listSnapshotEligibleRealms() throws RealmsServiceException {
      String $$0 = this.url("worlds/listPrereleaseEligibleWorlds");
      String $$1 = this.execute(Request.get($$0));
      return RealmsServerList.parse($$1).servers;
   }

   public RealmsServer createSnapshotRealm(Long pParentId) throws RealmsServiceException {
      String $$1 = String.valueOf(pParentId);
      String $$2 = this.url("worlds" + "/$PARENT_WORLD_ID/createPrereleaseRealm".replace("$PARENT_WORLD_ID", $$1));
      return RealmsServer.parse(this.execute(Request.post($$2, $$1)));
   }

   public List<RealmsNotification> getNotifications() throws RealmsServiceException {
      String $$0 = this.url("notifications");
      String $$1 = this.execute(Request.get($$0));
      return RealmsNotification.parseList($$1);
   }

   private static JsonArray uuidListToJsonArray(List<UUID> pUuidList) {
      JsonArray $$1 = new JsonArray();

      for (UUID $$2 : pUuidList) {
         if ($$2 != null) {
            $$1.add($$2.toString());
         }
      }

      return $$1;
   }

   public void notificationsSeen(List<UUID> pUuidList) throws RealmsServiceException {
      String $$1 = this.url("notifications/seen");
      this.execute(Request.post($$1, GSON.toJson(uuidListToJsonArray(pUuidList))));
   }

   public void notificationsDismiss(List<UUID> pUuidList) throws RealmsServiceException {
      String $$1 = this.url("notifications/dismiss");
      this.execute(Request.post($$1, GSON.toJson(uuidListToJsonArray(pUuidList))));
   }

   public RealmsServer getOwnRealm(long pId) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$ID".replace("$ID", String.valueOf(pId)));
      String $$2 = this.execute(Request.get($$1));
      return RealmsServer.parse($$2);
   }

   public ServerActivityList getActivity(long pWorldId) throws RealmsServiceException {
      String $$1 = this.url("activities" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$2 = this.execute(Request.get($$1));
      return ServerActivityList.parse($$2);
   }

   public RealmsServerPlayerLists getLiveStats() throws RealmsServiceException {
      String $$0 = this.url("activities/liveplayerlist");
      String $$1 = this.execute(Request.get($$0));
      return RealmsServerPlayerLists.parse($$1);
   }

   public RealmsServerAddress join(long pServerId) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/v1/$ID/join/pc".replace("$ID", pServerId + ""));
      String $$2 = this.execute(Request.get($$1, 5000, 30000));
      return RealmsServerAddress.parse($$2);
   }

   public void initializeRealm(long pWorldId, String pName, String pDescription) throws RealmsServiceException {
      RealmsDescriptionDto $$3 = new RealmsDescriptionDto(pName, pDescription);
      String $$4 = this.url("worlds" + "/$WORLD_ID/initialize".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$5 = GSON.toJson($$3);
      this.execute(Request.post($$4, $$5, 5000, 10000));
   }

   public boolean hasParentalConsent() throws RealmsServiceException {
      String $$0 = this.url("mco/available");
      String $$1 = this.execute(Request.get($$0));
      return Boolean.parseBoolean($$1);
   }

   public RealmsClient.CompatibleVersionResponse clientCompatible() throws RealmsServiceException {
      String $$0 = this.url("mco/client/compatible");
      String $$1 = this.execute(Request.get($$0));

      try {
         return RealmsClient.CompatibleVersionResponse.valueOf($$1);
      } catch (IllegalArgumentException var5) {
         throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse($$1));
      }
   }

   public void uninvite(long pWorldId, UUID pPlayerUuid) throws RealmsServiceException {
      String $$2 = this.url(
         "invites" + "/$WORLD_ID/invite/$UUID".replace("$WORLD_ID", String.valueOf(pWorldId)).replace("$UUID", UndashedUuid.toString(pPlayerUuid))
      );
      this.execute(Request.delete($$2));
   }

   public void uninviteMyselfFrom(long pWorldId) throws RealmsServiceException {
      String $$1 = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(pWorldId)));
      this.execute(Request.delete($$1));
   }

   public RealmsServer invite(long pWorldId, String pPlayerName) throws RealmsServiceException {
      PlayerInfo $$2 = new PlayerInfo();
      $$2.setName(pPlayerName);
      String $$3 = this.url("invites" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$4 = this.execute(Request.post($$3, GSON.toJson($$2)));
      return RealmsServer.parse($$4);
   }

   public BackupList backupsFor(long pWorldId) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$2 = this.execute(Request.get($$1));
      return BackupList.parse($$2);
   }

   public void update(long pWorldId, String pName, String pDescription) throws RealmsServiceException {
      RealmsDescriptionDto $$3 = new RealmsDescriptionDto(pName, pDescription);
      String $$4 = this.url("worlds" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(pWorldId)));
      this.execute(Request.post($$4, GSON.toJson($$3)));
   }

   public void updateSlot(long pWorldId, int pSlotId, RealmsWorldOptions pWorldOptions) throws RealmsServiceException {
      String $$3 = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(pWorldId)).replace("$SLOT_ID", String.valueOf(pSlotId)));
      String $$4 = pWorldOptions.toJson();
      this.execute(Request.post($$3, $$4));
   }

   public boolean switchSlot(long pWorldId, int pSlotId) throws RealmsServiceException {
      String $$2 = this.url("worlds" + "/$WORLD_ID/slot/$SLOT_ID".replace("$WORLD_ID", String.valueOf(pWorldId)).replace("$SLOT_ID", String.valueOf(pSlotId)));
      String $$3 = this.execute(Request.put($$2, ""));
      return Boolean.valueOf($$3);
   }

   public void restoreWorld(long pWorldId, String pBackupId) throws RealmsServiceException {
      String $$2 = this.url("worlds" + "/$WORLD_ID/backups".replace("$WORLD_ID", String.valueOf(pWorldId)), "backupId=" + pBackupId);
      this.execute(Request.put($$2, "", 40000, 600000));
   }

   public WorldTemplatePaginatedList fetchWorldTemplates(int pPage, int pPageSize, RealmsServer.WorldType pWorldType) throws RealmsServiceException {
      String $$3 = this.url(
         "worlds" + "/templates/$WORLD_TYPE".replace("$WORLD_TYPE", pWorldType.toString()), String.format(Locale.ROOT, "page=%d&pageSize=%d", pPage, pPageSize)
      );
      String $$4 = this.execute(Request.get($$3));
      return WorldTemplatePaginatedList.parse($$4);
   }

   public Boolean putIntoMinigameMode(long pWorldId, String pMinigameId) throws RealmsServiceException {
      String $$2 = "/minigames/$MINIGAME_ID/$WORLD_ID".replace("$MINIGAME_ID", pMinigameId).replace("$WORLD_ID", String.valueOf(pWorldId));
      String $$3 = this.url("worlds" + $$2);
      return Boolean.valueOf(this.execute(Request.put($$3, "")));
   }

   public Ops op(long pWorldId, UUID pProfileUuid) throws RealmsServiceException {
      String $$2 = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(pWorldId)).replace("$PROFILE_UUID", UndashedUuid.toString(pProfileUuid));
      String $$3 = this.url("ops" + $$2);
      return Ops.parse(this.execute(Request.post($$3, "")));
   }

   public Ops deop(long pWorldId, UUID pProfileUuid) throws RealmsServiceException {
      String $$2 = "/$WORLD_ID/$PROFILE_UUID".replace("$WORLD_ID", String.valueOf(pWorldId)).replace("$PROFILE_UUID", UndashedUuid.toString(pProfileUuid));
      String $$3 = this.url("ops" + $$2);
      return Ops.parse(this.execute(Request.delete($$3)));
   }

   public Boolean open(long pWorldId) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID/open".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$2 = this.execute(Request.put($$1, ""));
      return Boolean.valueOf($$2);
   }

   public Boolean close(long pWorldId) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID/close".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$2 = this.execute(Request.put($$1, ""));
      return Boolean.valueOf($$2);
   }

   public Boolean resetWorldWithSeed(long pWorldId, WorldGenerationInfo pGenerationInfo) throws RealmsServiceException {
      RealmsWorldResetDto $$2 = new RealmsWorldResetDto(
         pGenerationInfo.seed(), -1L, pGenerationInfo.levelType().getDtoIndex(), pGenerationInfo.generateStructures(), pGenerationInfo.experiments()
      );
      String $$3 = this.url("worlds" + "/$WORLD_ID/reset".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$4 = this.execute(Request.post($$3, GSON.toJson($$2), 30000, 80000));
      return Boolean.valueOf($$4);
   }

   public Boolean resetWorldWithTemplate(long pWorldId, String pWorldTemplateId) throws RealmsServiceException {
      RealmsWorldResetDto $$2 = new RealmsWorldResetDto(null, Long.valueOf(pWorldTemplateId), -1, false, Set.of());
      String $$3 = this.url("worlds" + "/$WORLD_ID/reset".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$4 = this.execute(Request.post($$3, GSON.toJson($$2), 30000, 80000));
      return Boolean.valueOf($$4);
   }

   public Subscription subscriptionFor(long pWorldId) throws RealmsServiceException {
      String $$1 = this.url("subscriptions" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(pWorldId)));
      String $$2 = this.execute(Request.get($$1));
      return Subscription.parse($$2);
   }

   public int pendingInvitesCount() throws RealmsServiceException {
      return this.pendingInvites().pendingInvites.size();
   }

   public PendingInvitesList pendingInvites() throws RealmsServiceException {
      String $$0 = this.url("invites/pending");
      String $$1 = this.execute(Request.get($$0));
      PendingInvitesList $$2 = PendingInvitesList.parse($$1);
      $$2.pendingInvites.removeIf(this::isBlocked);
      return $$2;
   }

   private boolean isBlocked(PendingInvite p_87198_) {
      return this.minecraft.getPlayerSocialManager().isBlocked(p_87198_.realmOwnerUuid);
   }

   public void acceptInvitation(String pInviteId) throws RealmsServiceException {
      String $$1 = this.url("invites" + "/accept/$INVITATION_ID".replace("$INVITATION_ID", pInviteId));
      this.execute(Request.put($$1, ""));
   }

   public WorldDownload requestDownloadInfo(long pWorldId, int pSlotId) throws RealmsServiceException {
      String $$2 = this.url(
         "worlds" + "/$WORLD_ID/slot/$SLOT_ID/download".replace("$WORLD_ID", String.valueOf(pWorldId)).replace("$SLOT_ID", String.valueOf(pSlotId))
      );
      String $$3 = this.execute(Request.get($$2));
      return WorldDownload.parse($$3);
   }

   @Nullable
   public UploadInfo requestUploadInfo(long pWorldId, @Nullable String pToken) throws RealmsServiceException {
      String $$2 = this.url("worlds" + "/$WORLD_ID/backups/upload".replace("$WORLD_ID", String.valueOf(pWorldId)));
      return UploadInfo.parse(this.execute(Request.put($$2, UploadInfo.createRequest(pToken))));
   }

   public void rejectInvitation(String pInviteId) throws RealmsServiceException {
      String $$1 = this.url("invites" + "/reject/$INVITATION_ID".replace("$INVITATION_ID", pInviteId));
      this.execute(Request.put($$1, ""));
   }

   public void agreeToTos() throws RealmsServiceException {
      String $$0 = this.url("mco/tos/agreed");
      this.execute(Request.post($$0, ""));
   }

   public RealmsNews getNews() throws RealmsServiceException {
      String $$0 = this.url("mco/v1/news");
      String $$1 = this.execute(Request.get($$0, 5000, 10000));
      return RealmsNews.parse($$1);
   }

   public void sendPingResults(PingResult pPingResult) throws RealmsServiceException {
      String $$1 = this.url("regions/ping/stat");
      this.execute(Request.post($$1, GSON.toJson(pPingResult)));
   }

   public Boolean trialAvailable() throws RealmsServiceException {
      String $$0 = this.url("trial");
      String $$1 = this.execute(Request.get($$0));
      return Boolean.valueOf($$1);
   }

   public void deleteRealm(long pWorldId) throws RealmsServiceException {
      String $$1 = this.url("worlds" + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(pWorldId)));
      this.execute(Request.delete($$1));
   }

   private String url(String pPath) {
      return this.url(pPath, null);
   }

   private String url(String pPath, @Nullable String pQuery) {
      try {
         return new URI(ENVIRONMENT.protocol, ENVIRONMENT.baseUrl, "/" + pPath, pQuery, null).toASCIIString();
      } catch (URISyntaxException var4) {
         throw new IllegalArgumentException(pPath, var4);
      }
   }

   private String execute(Request<?> pRequest) throws RealmsServiceException {
      pRequest.cookie("sid", this.sessionId);
      pRequest.cookie("user", this.username);
      pRequest.cookie("version", SharedConstants.getCurrentVersion().getName());
      pRequest.addSnapshotHeader(RealmsMainScreen.isSnapshot());

      try {
         int $$1 = pRequest.responseCode();
         if ($$1 != 503 && $$1 != 277) {
            String $$3 = pRequest.text();
            if ($$1 >= 200 && $$1 < 300) {
               return $$3;
            } else if ($$1 == 401) {
               String $$4 = pRequest.getHeader("WWW-Authenticate");
               LOGGER.info("Could not authorize you against Realms server: {}", $$4);
               throw new RealmsServiceException(new RealmsError.AuthenticationError($$4));
            } else {
               RealmsError $$5 = RealmsError.parse($$1, $$3);
               throw new RealmsServiceException($$5);
            }
         } else {
            int $$2 = pRequest.getRetryAfterHeader();
            throw new RetryCallException($$2, $$1);
         }
      } catch (RealmsHttpException var5) {
         throw new RealmsServiceException(RealmsError.CustomError.connectivityError(var5));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum CompatibleVersionResponse {
      COMPATIBLE,
      OUTDATED,
      OTHER;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Environment {
      PRODUCTION("pc.realms.minecraft.net", "https"),
      STAGE("pc-stage.realms.minecraft.net", "https"),
      LOCAL("localhost:8080", "http");

      public final String baseUrl;
      public final String protocol;

      private Environment(final String pBaseUrl, final String pProtocol) {
         this.baseUrl = pBaseUrl;
         this.protocol = pProtocol;
      }

      public static Optional<RealmsClient.Environment> byName(String pName) {
         String var1 = pName.toLowerCase(Locale.ROOT);

         return switch (var1) {
            case "production" -> Optional.of(PRODUCTION);
            case "local" -> Optional.of(LOCAL);
            case "stage", "staging" -> Optional.of(STAGE);
            default -> Optional.empty();
         };
      }
   }
}
