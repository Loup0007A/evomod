package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
   static final ResourceLocation INFO_SPRITE = ResourceLocation.withDefaultNamespace("icon/info");
   static final ResourceLocation NEW_REALM_SPRITE = ResourceLocation.withDefaultNamespace("icon/new_realm");
   static final ResourceLocation EXPIRED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expired");
   static final ResourceLocation EXPIRES_SOON_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expires_soon");
   static final ResourceLocation OPEN_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/open");
   static final ResourceLocation CLOSED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/closed");
   private static final ResourceLocation INVITE_SPRITE = ResourceLocation.withDefaultNamespace("icon/invite");
   private static final ResourceLocation NEWS_SPRITE = ResourceLocation.withDefaultNamespace("icon/news");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation LOGO_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/title/realms.png");
   private static final ResourceLocation NO_REALMS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/no_realms.png");
   private static final Component TITLE = Component.translatable("menu.online");
   private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
   private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
   private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
   static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
   private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
   private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
   private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
   private static final Component INCOMPATIBLE_POPUP_TITLE = Component.translatable("mco.compatibility.incompatible.popup.title");
   private static final Component INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE = Component.translatable("mco.compatibility.incompatible.releaseType.popup.message");
   private static final int BUTTON_WIDTH = 100;
   private static final int BUTTON_COLUMNS = 3;
   private static final int BUTTON_SPACING = 4;
   private static final int CONTENT_WIDTH = 308;
   private static final int LOGO_WIDTH = 128;
   private static final int LOGO_HEIGHT = 34;
   private static final int LOGO_TEXTURE_WIDTH = 128;
   private static final int LOGO_TEXTURE_HEIGHT = 64;
   private static final int LOGO_PADDING = 5;
   private static final int HEADER_HEIGHT = 44;
   private static final int FOOTER_PADDING = 11;
   private static final int NEW_REALM_SPRITE_WIDTH = 40;
   private static final int NEW_REALM_SPRITE_HEIGHT = 20;
   private static final int ENTRY_WIDTH = 216;
   private static final int ITEM_HEIGHT = 36;
   private static final boolean SNAPSHOT = !SharedConstants.getCurrentVersion().isStable();
   private static boolean snapshotToggle = SNAPSHOT;
   private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
   @Nullable
   private DataFetcher.Subscription dataSubscription;
   private final Set<UUID> handledSeenNotifications = new HashSet<>();
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private final Screen lastScreen;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   RealmsMainScreen.RealmSelectionList realmSelectionList;
   private RealmsServerList serverList;
   private List<RealmsServer> availableSnapshotServers = List.of();
   RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists();
   private volatile boolean trialsAvailable;
   @Nullable
   private volatile String newsLink;
   long lastClickTime;
   private final List<RealmsNotification> notifications = new ArrayList<>();
   private Button addRealmButton;
   private RealmsMainScreen.NotificationButton pendingInvitesButton;
   private RealmsMainScreen.NotificationButton newsButton;
   private RealmsMainScreen.LayoutState activeLayoutState;
   @Nullable
   private HeaderAndFooterLayout layout;

   public RealmsMainScreen(Screen pLastScreen) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
   }

   @Override
   public void init() {
      this.serverList = new RealmsServerList(this.minecraft);
      this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
      Component $$0 = Component.translatable("mco.invites.title");
      this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
         $$0, INVITE_SPRITE, p_296029_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, $$0))
      );
      Component $$1 = Component.translatable("mco.news");
      this.newsButton = new RealmsMainScreen.NotificationButton($$1, NEWS_SPRITE, p_296035_ -> {
         String $$1x = this.newsLink;
         if ($$1x != null) {
            ConfirmLinkScreen.confirmLinkNow(this, $$1x);
            if (this.newsButton.notificationCount() != 0) {
               RealmsPersistence.RealmsPersistenceData $$2 = RealmsPersistence.readFile();
               $$2.hasUnreadNews = false;
               RealmsPersistence.writeFile($$2);
               this.newsButton.setNotificationCount(0);
            }
         }
      });
      this.newsButton.setTooltip(Tooltip.create($$1));
      this.playButton = Button.builder(PLAY_TEXT, p_86659_ -> play(this.getSelectedServer(), this)).width(100).build();
      this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, p_86672_ -> this.configureClicked(this.getSelectedServer())).width(100).build();
      this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, p_86622_ -> this.onRenew(this.getSelectedServer())).width(100).build();
      this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, p_86679_ -> this.leaveClicked(this.getSelectedServer())).width(100).build();
      this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), p_296032_ -> this.openTrialAvailablePopup())
         .size(100, 20)
         .build();
      this.backButton = Button.builder(CommonComponents.GUI_BACK, p_325094_ -> this.onClose()).width(100).build();
      if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
         this.addRenderableWidget(
            CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"))
               .create(5, 5, 100, 20, Component.literal("Realm"), (p_308035_, p_308036_) -> {
                  snapshotToggle = p_308036_;
                  this.availableSnapshotServers = List.of();
                  this.debugRefreshDataFetchers();
               })
         );
      }

      this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
      this.updateButtonStates();
      this.availability.thenAcceptAsync(p_296034_ -> {
         Screen $$1x = p_296034_.createErrorScreen(this.lastScreen);
         if ($$1x == null) {
            this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.minecraft.setScreen($$1x);
         }
      }, this.screenExecutor);
   }

   public static boolean isSnapshot() {
      return SNAPSHOT && snapshotToggle;
   }

   @Override
   protected void repositionElements() {
      if (this.layout != null) {
         this.realmSelectionList.updateSize(this.width, this.layout);
         this.layout.arrangeElements();
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void updateLayout() {
      if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
         this.updateLayout(RealmsMainScreen.LayoutState.NO_REALMS);
      } else {
         this.updateLayout(RealmsMainScreen.LayoutState.LIST);
      }
   }

   private void updateLayout(RealmsMainScreen.LayoutState pLayoutState) {
      if (this.activeLayoutState != pLayoutState) {
         if (this.layout != null) {
            this.layout.visitWidgets(p_325098_ -> this.removeWidget(p_325098_));
         }

         this.layout = this.createLayout(pLayoutState);
         this.activeLayoutState = pLayoutState;
         this.layout.visitWidgets(p_325096_ -> {
            AbstractWidget var10000 = this.addRenderableWidget(p_325096_);
         });
         this.repositionElements();
      }
   }

   private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState pLayoutState) {
      HeaderAndFooterLayout $$1 = new HeaderAndFooterLayout(this);
      $$1.setHeaderHeight(44);
      $$1.addToHeader(this.createHeader());
      Layout $$2 = this.createFooter(pLayoutState);
      $$2.arrangeElements();
      $$1.setFooterHeight($$2.getHeight() + 22);
      $$1.addToFooter($$2);
      switch (pLayoutState) {
         case LOADING:
            $$1.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
            break;
         case NO_REALMS:
            $$1.addToContents(this.createNoRealmsContent());
            break;
         case LIST:
            $$1.addToContents(this.realmSelectionList);
      }

      return $$1;
   }

   private Layout createHeader() {
      int $$0 = 90;
      LinearLayout $$1 = LinearLayout.horizontal().spacing(4);
      $$1.defaultCellSetting().alignVerticallyMiddle();
      $$1.addChild(this.pendingInvitesButton);
      $$1.addChild(this.newsButton);
      LinearLayout $$2 = LinearLayout.horizontal();
      $$2.defaultCellSetting().alignVerticallyMiddle();
      $$2.addChild(SpacerElement.width(90));
      $$2.addChild(ImageWidget.texture(128, 34, LOGO_LOCATION, 128, 64), LayoutSettings::alignHorizontallyCenter);
      $$2.addChild(new FrameLayout(90, 44)).addChild($$1, LayoutSettings::alignHorizontallyRight);
      return $$2;
   }

   private Layout createFooter(RealmsMainScreen.LayoutState pLayoutState) {
      GridLayout $$1 = new GridLayout().spacing(4);
      GridLayout.RowHelper $$2 = $$1.createRowHelper(3);
      if (pLayoutState == RealmsMainScreen.LayoutState.LIST) {
         $$2.addChild(this.playButton);
         $$2.addChild(this.configureButton);
         $$2.addChild(this.renewButton);
         $$2.addChild(this.leaveButton);
      }

      $$2.addChild(this.addRealmButton);
      $$2.addChild(this.backButton);
      return $$1;
   }

   private LinearLayout createNoRealmsContent() {
      LinearLayout $$0 = LinearLayout.vertical().spacing(8);
      $$0.defaultCellSetting().alignHorizontallyCenter();
      $$0.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
      FocusableTextWidget $$1 = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false, 4);
      $$0.addChild($$1);
      return $$0;
   }

   void updateButtonStates() {
      RealmsServer $$0 = this.getSelectedServer();
      this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
      this.playButton.active = $$0 != null && this.shouldPlayButtonBeActive($$0);
      this.renewButton.active = $$0 != null && this.shouldRenewButtonBeActive($$0);
      this.leaveButton.active = $$0 != null && this.shouldLeaveButtonBeActive($$0);
      this.configureButton.active = $$0 != null && this.shouldConfigureButtonBeActive($$0);
   }

   boolean shouldPlayButtonBeActive(RealmsServer pRealmsServer) {
      boolean $$1 = !pRealmsServer.expired && pRealmsServer.state == RealmsServer.State.OPEN;
      return $$1 && (pRealmsServer.isCompatible() || pRealmsServer.needsUpgrade() || isSelfOwnedServer(pRealmsServer));
   }

   private boolean shouldRenewButtonBeActive(RealmsServer pRealmsServer) {
      return pRealmsServer.expired && isSelfOwnedServer(pRealmsServer);
   }

   private boolean shouldConfigureButtonBeActive(RealmsServer pRealmsServer) {
      return isSelfOwnedServer(pRealmsServer) && pRealmsServer.state != RealmsServer.State.UNINITIALIZED;
   }

   private boolean shouldLeaveButtonBeActive(RealmsServer pRealmsServer) {
      return !isSelfOwnedServer(pRealmsServer);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }
   }

   public static void refreshPendingInvites() {
      Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
   }

   public static void refreshServerList() {
      Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
   }

   private void debugRefreshDataFetchers() {
      for (DataFetcher.Task<?> $$0 : this.minecraft.realmsDataFetcher().getTasks()) {
         $$0.reset();
      }
   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher pDataFetcher) {
      DataFetcher.Subscription $$1 = pDataFetcher.dataFetcher.createSubscription();
      $$1.subscribe(pDataFetcher.serverListUpdateTask, p_308037_ -> {
         this.serverList.updateServersList(p_308037_.serverList());
         this.availableSnapshotServers = p_308037_.availableSnapshotServers();
         this.refreshListAndLayout();
         boolean $$1x = false;

         for (RealmsServer $$2 : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer($$2)) {
               $$1x = true;
            }
         }

         if (!regionsPinged && $$1x) {
            regionsPinged = true;
            this.pingRegions();
         }
      });
      callRealmsClient(RealmsClient::getNotifications, p_274622_ -> {
         this.notifications.clear();
         this.notifications.addAll(p_274622_);

         for (RealmsNotification $$1x : p_274622_) {
            if ($$1x instanceof RealmsNotification.InfoPopup $$2) {
               PopupScreen $$3 = $$2.buildScreen(this, this::dismissNotification);
               if ($$3 != null) {
                  this.minecraft.setScreen($$3);
                  this.markNotificationsAsSeen(List.of($$1x));
                  break;
               }
            }
         }

         if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
            this.refreshListAndLayout();
         }
      });
      $$1.subscribe(pDataFetcher.pendingInvitesTask, p_296027_ -> {
         this.pendingInvitesButton.setNotificationCount(p_296027_);
         this.pendingInvitesButton.setTooltip(p_296027_ == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
         if (p_296027_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", p_296027_));
         }
      });
      $$1.subscribe(pDataFetcher.trialAvailabilityTask, p_296031_ -> this.trialsAvailable = p_296031_);
      $$1.subscribe(pDataFetcher.onlinePlayersTask, p_340705_ -> this.onlinePlayersPerRealm = p_340705_);
      $$1.subscribe(pDataFetcher.newsTask, p_296037_ -> {
         pDataFetcher.newsManager.updateUnreadNews(p_296037_);
         this.newsLink = pDataFetcher.newsManager.newsLink();
         this.newsButton.setNotificationCount(pDataFetcher.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
      });
      return $$1;
   }

   private void markNotificationsAsSeen(Collection<RealmsNotification> pNotifications) {
      List<UUID> $$1 = new ArrayList<>(pNotifications.size());

      for (RealmsNotification $$2 : pNotifications) {
         if (!$$2.seen() && !this.handledSeenNotifications.contains($$2.uuid())) {
            $$1.add($$2.uuid());
         }
      }

      if (!$$1.isEmpty()) {
         callRealmsClient(p_274625_ -> {
            p_274625_.notificationsSeen($$1);
            return null;
         }, p_274630_ -> this.handledSeenNotifications.addAll($$1));
      }
   }

   private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> pCall, Consumer<T> pOnFinish) {
      Minecraft $$2 = Minecraft.getInstance();
      CompletableFuture.<T>supplyAsync(() -> {
         try {
            return pCall.request(RealmsClient.create($$2));
         } catch (RealmsServiceException var3) {
            throw new RuntimeException(var3);
         }
      }).thenAcceptAsync(pOnFinish, $$2).exceptionally(p_274626_ -> {
         LOGGER.error("Failed to execute call to Realms Service", p_274626_);
         return null;
      });
   }

   private void refreshListAndLayout() {
      RealmsServer $$0 = this.getSelectedServer();
      this.realmSelectionList.clear();

      for (RealmsNotification $$1 : this.notifications) {
         if (this.addListEntriesForNotification($$1)) {
            this.markNotificationsAsSeen(List.of($$1));
            break;
         }
      }

      for (RealmsServer $$2 : this.availableSnapshotServers) {
         this.realmSelectionList.addEntry(new RealmsMainScreen.AvailableSnapshotEntry($$2));
      }

      for (RealmsServer $$3 : this.serverList) {
         RealmsMainScreen.Entry $$4;
         if (isSnapshot() && !$$3.isSnapshotRealm()) {
            if ($$3.state == RealmsServer.State.UNINITIALIZED) {
               continue;
            }

            $$4 = new RealmsMainScreen.ParentEntry($$3);
         } else {
            $$4 = new RealmsMainScreen.ServerEntry($$3);
         }

         this.realmSelectionList.addEntry($$4);
         if ($$0 != null && $$0.id == $$3.id) {
            this.realmSelectionList.setSelected($$4);
         }
      }

      this.updateLayout();
      this.updateButtonStates();
   }

   private boolean addListEntriesForNotification(RealmsNotification pNotification) {
      if (!(pNotification instanceof RealmsNotification.VisitUrl $$1)) {
         return false;
      } else {
         Component $$2 = $$1.getMessage();
         int $$3 = this.font.wordWrapHeight($$2, 216);
         int $$4 = Mth.positiveCeilDiv($$3 + 7, 36) - 1;
         this.realmSelectionList.addEntry(new RealmsMainScreen.NotificationMessageEntry($$2, $$4 + 2, $$1));

         for (int $$5 = 0; $$5 < $$4; $$5++) {
            this.realmSelectionList.addEntry(new RealmsMainScreen.EmptyEntry());
         }

         this.realmSelectionList.addEntry(new RealmsMainScreen.ButtonEntry($$1.buildOpenLinkButton(this)));
         return true;
      }
   }

   private void pingRegions() {
      new Thread(() -> {
         List<RegionPingResult> $$0 = Ping.pingAllRegions();
         RealmsClient $$1 = RealmsClient.create();
         PingResult $$2 = new PingResult();
         $$2.pingResults = $$0;
         $$2.realmIds = this.getOwnedNonExpiredRealmIds();

         try {
            $$1.sendPingResults($$2);
         } catch (Throwable var5) {
            LOGGER.warn("Could not send ping result to Realms: ", var5);
         }
      }).start();
   }

   private List<Long> getOwnedNonExpiredRealmIds() {
      List<Long> $$0 = Lists.newArrayList();

      for (RealmsServer $$1 : this.serverList) {
         if (this.isSelfOwnedNonExpiredServer($$1)) {
            $$0.add($$1.id);
         }
      }

      return $$0;
   }

   private void onRenew(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null) {
         String $$1 = CommonLinks.extendRealms(pRealmsServer.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), pRealmsServer.expiredTrial);
         this.minecraft.keyboardHandler.setClipboard($$1);
         Util.getPlatform().openUri($$1);
      }
   }

   private void configureClicked(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null && this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, pRealmsServer.id));
      }
   }

   private void leaveClicked(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null && !this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
         Component $$1 = Component.translatable("mco.configure.world.leave.question.line1");
         this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, $$1, p_340701_ -> this.leaveServer(pRealmsServer)));
      }
   }

   @Nullable
   private RealmsServer getSelectedServer() {
      return this.realmSelectionList.getSelected() instanceof RealmsMainScreen.ServerEntry $$0 ? $$0.getServer() : null;
   }

   private void leaveServer(final RealmsServer pServer) {
      (new Thread("Realms-leave-server") {
            @Override
            public void run() {
               try {
                  RealmsClient $$0 = RealmsClient.create();
                  $$0.uninviteMyselfFrom(pServer.id);
                  RealmsMainScreen.this.minecraft.execute(RealmsMainScreen::refreshServerList);
               } catch (RealmsServiceException var2) {
                  RealmsMainScreen.LOGGER.error("Couldn't configure world", var2);
                  RealmsMainScreen.this.minecraft
                     .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(var2, RealmsMainScreen.this)));
               }
            }
         })
         .start();
      this.minecraft.setScreen(this);
   }

   void dismissNotification(UUID p_275349_) {
      callRealmsClient(p_274628_ -> {
         p_274628_.notificationsDismiss(List.of(p_275349_));
         return null;
      }, p_274632_ -> {
         this.notifications.removeIf(p_274621_ -> p_274621_.dismissable() && p_275349_.equals(p_274621_.uuid()));
         this.refreshListAndLayout();
      });
   }

   public void resetScreen() {
      this.realmSelectionList.setSelected(null);
      refreshServerList();
   }

   @Override
   public Component getNarrationMessage() {
      return (Component)(switch (this.activeLayoutState) {
         case LOADING -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
         case NO_REALMS -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
         case LIST -> super.getNarrationMessage();
      });
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      if (isSnapshot()) {
         pGuiGraphics.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().getName(), 2, this.height - 10, -1);
      }

      if (this.trialsAvailable && this.addRealmButton.active) {
         AddRealmPopupScreen.renderDiamond(pGuiGraphics, this.addRealmButton);
      }

      switch (RealmsClient.ENVIRONMENT) {
         case STAGE:
            this.renderEnvironment(pGuiGraphics, "STAGE!", -256);
            break;
         case LOCAL:
            this.renderEnvironment(pGuiGraphics, "LOCAL!", 8388479);
      }
   }

   private void openTrialAvailablePopup() {
      this.minecraft.setScreen(new AddRealmPopupScreen(this, this.trialsAvailable));
   }

   public static void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen) {
      play(pRealmsServer, pLastScreen, false);
   }

   public static void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen, boolean pAllowSnapshots) {
      if (pRealmsServer != null) {
         if (!isSnapshot() || pAllowSnapshots || pRealmsServer.isMinigameActive()) {
            Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
            return;
         }

         switch (pRealmsServer.compatibility) {
            case COMPATIBLE:
               Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
               break;
            case UNVERIFIABLE:
               confirmToPlay(
                  pRealmsServer,
                  pLastScreen,
                  Component.translatable("mco.compatibility.unverifiable.title").withColor(-171),
                  Component.translatable("mco.compatibility.unverifiable.message"),
                  CommonComponents.GUI_CONTINUE
               );
               break;
            case NEEDS_DOWNGRADE:
               confirmToPlay(
                  pRealmsServer,
                  pLastScreen,
                  Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128),
                  Component.translatable(
                     "mco.compatibility.downgrade.description",
                     Component.literal(pRealmsServer.activeVersion).withColor(-171),
                     Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
                  ),
                  Component.translatable("mco.compatibility.downgrade")
               );
               break;
            case NEEDS_UPGRADE:
               upgradeRealmAndPlay(pRealmsServer, pLastScreen);
               break;
            case INCOMPATIBLE:
               Minecraft.getInstance()
                  .setScreen(
                     new PopupScreen.Builder(pLastScreen, INCOMPATIBLE_POPUP_TITLE)
                        .setMessage(
                           Component.translatable(
                              "mco.compatibility.incompatible.series.popup.message",
                              Component.literal(pRealmsServer.activeVersion).withColor(-171),
                              Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
                           )
                        )
                        .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                        .build()
                  );
               break;
            case RELEASE_TYPE_INCOMPATIBLE:
               Minecraft.getInstance()
                  .setScreen(
                     new PopupScreen.Builder(pLastScreen, INCOMPATIBLE_POPUP_TITLE)
                        .setMessage(INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE)
                        .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                        .build()
                  );
         }
      }
   }

   private static void confirmToPlay(RealmsServer pRealmsServer, Screen pLastScreen, Component pTitle, Component pMessage, Component pConfirmButton) {
      Minecraft.getInstance().setScreen(new PopupScreen.Builder(pLastScreen, pTitle).setMessage(pMessage).addButton(pConfirmButton, p_340704_ -> {
         Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
         refreshServerList();
      }).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
   }

   private static void upgradeRealmAndPlay(RealmsServer pServer, Screen pLastScreen) {
      Component $$2 = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
      Component $$3 = Component.translatable("mco.compatibility.upgrade");
      Component $$4 = Component.literal(pServer.activeVersion).withColor(-171);
      Component $$5 = Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171);
      Component $$6 = isSelfOwnedServer(pServer)
         ? Component.translatable("mco.compatibility.upgrade.description", $$4, $$5)
         : Component.translatable("mco.compatibility.upgrade.friend.description", $$4, $$5);
      confirmToPlay(pServer, pLastScreen, $$2, $$6, $$3);
   }

   public static Component getVersionComponent(String pVersion, boolean pCompatible) {
      return getVersionComponent(pVersion, pCompatible ? -8355712 : -2142128);
   }

   public static Component getVersionComponent(String pVersion, int pColor) {
      return (Component)(StringUtils.isBlank(pVersion)
         ? CommonComponents.EMPTY
         : Component.translatable("mco.version", Component.literal(pVersion).withColor(pColor)));
   }

   static boolean isSelfOwnedServer(RealmsServer pServer) {
      return Minecraft.getInstance().isLocalPlayer(pServer.ownerUUID);
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer pServer) {
      return isSelfOwnedServer(pServer) && !pServer.expired;
   }

   private void renderEnvironment(GuiGraphics pGuiGraphics, String pText, int pColor) {
      pGuiGraphics.pose().pushPose();
      pGuiGraphics.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
      pGuiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
      pGuiGraphics.pose().scale(1.5F, 1.5F, 1.5F);
      pGuiGraphics.drawString(this.font, pText, 0, 0, pColor, false);
      pGuiGraphics.pose().popPose();
   }

   @OnlyIn(Dist.CLIENT)
   class AvailableSnapshotEntry extends RealmsMainScreen.Entry {
      private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
      private static final int TEXT_PADDING = 5;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
      private final RealmsServer parent;

      public AvailableSnapshotEntry(final RealmsServer pParent) {
         this.parent = pParent;
         this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.tooltip")));
      }

      @Override
      public void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      ) {
         pGuiGraphics.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, pLeft - 5, pTop + pHeight / 2 - 10, 40, 20);
         int $$10 = pTop + pHeight / 2 - 9 / 2;
         pGuiGraphics.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, pLeft + 40 - 2, $$10 - 5, 8388479);
         pGuiGraphics.drawString(
            RealmsMainScreen.this.font, Component.translatable("mco.snapshot.description", this.parent.name), pLeft + 40 - 2, $$10 + 5, -8355712
         );
         this.tooltip.refreshTooltipForNextRenderPass(pHovering, this.isFocused(), new ScreenRectangle(pLeft, pTop, pWidth, pHeight));
      }

      @Override
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         this.addSnapshotRealm();
         return true;
      }

      @Override
      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         if (CommonInputs.selected(pKeyCode)) {
            this.addSnapshotRealm();
            return true;
         } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
         }
      }

      private void addSnapshotRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.this.minecraft
            .setScreen(
               new PopupScreen.Builder(RealmsMainScreen.this, Component.translatable("mco.snapshot.createSnapshotPopup.title"))
                  .setMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text"))
                  .addButton(
                     Component.translatable("mco.selectServer.create"),
                     p_325099_ -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent.id))
                  )
                  .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
                  .build()
            );
      }

      @Override
      public Component getNarration() {
         return Component.translatable(
            "gui.narrate.button", CommonComponents.joinForNarration(START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", this.parent.name))
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ButtonEntry extends RealmsMainScreen.Entry {
      private final Button button;

      public ButtonEntry(final Button pButton) {
         this.button = pButton;
      }

      @Override
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         this.button.mouseClicked(pMouseX, pMouseY, pButton);
         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }

      @Override
      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         return this.button.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      @Override
      public void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      ) {
         this.button.setPosition(RealmsMainScreen.this.width / 2 - 75, pTop + 4);
         this.button.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      }

      @Override
      public void setFocused(boolean pFocused) {
         super.setFocused(pFocused);
         this.button.setFocused(pFocused);
      }

      @Override
      public Component getNarration() {
         return this.button.getMessage();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class CrossButton extends ImageButton {
      private static final WidgetSprites SPRITES = new WidgetSprites(
         ResourceLocation.withDefaultNamespace("widget/cross_button"), ResourceLocation.withDefaultNamespace("widget/cross_button_highlighted")
      );

      protected CrossButton(Button.OnPress pOnPress, Component pMessage) {
         super(0, 0, 14, 14, SPRITES, pOnPress);
         this.setTooltip(Tooltip.create(pMessage));
      }
   }

   @OnlyIn(Dist.CLIENT)
   class EmptyEntry extends RealmsMainScreen.Entry {
      @Override
      public void render(
         GuiGraphics p_301870_,
         int p_301858_,
         int p_301868_,
         int p_301866_,
         int p_301860_,
         int p_301859_,
         int p_301864_,
         int p_301865_,
         boolean p_301869_,
         float p_301861_
      ) {
      }

      @Override
      public Component getNarration() {
         return Component.empty();
      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      protected static final int STATUS_LIGHT_WIDTH = 10;
      private static final int STATUS_LIGHT_HEIGHT = 28;
      protected static final int PADDING_X = 7;
      protected static final int PADDING_Y = 2;

      protected void renderStatusLights(RealmsServer pRealmsServer, GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY) {
         int $$6 = pX - 10 - 7;
         int $$7 = pY + 2;
         if (pRealmsServer.expired) {
            this.drawRealmStatus(pGuiGraphics, $$6, $$7, pMouseX, pMouseY, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP);
         } else if (pRealmsServer.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(pGuiGraphics, $$6, $$7, pMouseX, pMouseY, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP);
         } else if (RealmsMainScreen.isSelfOwnedServer(pRealmsServer) && pRealmsServer.daysLeft < 7) {
            this.drawRealmStatus(
               pGuiGraphics,
               $$6,
               $$7,
               pMouseX,
               pMouseY,
               RealmsMainScreen.EXPIRES_SOON_SPRITE,
               () -> {
                  if (pRealmsServer.daysLeft <= 0) {
                     return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
                  } else {
                     return (Component)(pRealmsServer.daysLeft == 1
                        ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP
                        : Component.translatable("mco.selectServer.expires.days", pRealmsServer.daysLeft));
                  }
               }
            );
         } else if (pRealmsServer.state == RealmsServer.State.OPEN) {
            this.drawRealmStatus(pGuiGraphics, $$6, $$7, pMouseX, pMouseY, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP);
         }
      }

      private void drawRealmStatus(
         GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, ResourceLocation pSpriteLocation, Supplier<Component> pTooltipSupplier
      ) {
         pGuiGraphics.blitSprite(pSpriteLocation, pX, pY, 10, 28);
         if (RealmsMainScreen.this.realmSelectionList.isMouseOver((double)pMouseX, (double)pMouseY)
            && pMouseX >= pX
            && pMouseX <= pX + 10
            && pMouseY >= pY
            && pMouseY <= pY + 28) {
            RealmsMainScreen.this.setTooltipForNextRenderPass(pTooltipSupplier.get());
         }
      }

      protected void renderThirdLine(GuiGraphics pGuiGraphics, int pTop, int pLeft, RealmsServer pServer) {
         int $$4 = this.textX(pLeft);
         int $$5 = this.firstLineY(pTop);
         int $$6 = this.thirdLineY($$5);
         if (!RealmsMainScreen.isSelfOwnedServer(pServer)) {
            pGuiGraphics.drawString(RealmsMainScreen.this.font, pServer.owner, $$4, this.thirdLineY($$5), -8355712, false);
         } else if (pServer.expired) {
            Component $$7 = pServer.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
            pGuiGraphics.drawString(RealmsMainScreen.this.font, $$7, $$4, $$6, -2142128, false);
         }
      }

      protected void renderClampedString(GuiGraphics pGuiGraphics, String pText, int pMinX, int pY, int pMaxX, int pColor) {
         int $$6 = pMaxX - pMinX;
         if (RealmsMainScreen.this.font.width(pText) > $$6) {
            String $$7 = RealmsMainScreen.this.font.plainSubstrByWidth(pText, $$6 - RealmsMainScreen.this.font.width("... "));
            pGuiGraphics.drawString(RealmsMainScreen.this.font, $$7 + "...", pMinX, pY, pColor, false);
         } else {
            pGuiGraphics.drawString(RealmsMainScreen.this.font, pText, pMinX, pY, pColor, false);
         }
      }

      protected int versionTextX(int pLeft, int pWidth, Component pVersionComponent) {
         return pLeft + pWidth - RealmsMainScreen.this.font.width(pVersionComponent) - 20;
      }

      protected int firstLineY(int pTop) {
         return pTop + 1;
      }

      protected int lineHeight() {
         return 2 + 9;
      }

      protected int textX(int pLeft) {
         return pLeft + 36 + 2;
      }

      protected int secondLineY(int pFirstLineY) {
         return pFirstLineY + this.lineHeight();
      }

      protected int thirdLineY(int pFirstLineY) {
         return pFirstLineY + this.lineHeight() * 2;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum LayoutState {
      LOADING,
      NO_REALMS,
      LIST;
   }

   @OnlyIn(Dist.CLIENT)
   static class NotificationButton extends SpriteIconButton.CenteredIcon {
      private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{
         ResourceLocation.withDefaultNamespace("notification/1"),
         ResourceLocation.withDefaultNamespace("notification/2"),
         ResourceLocation.withDefaultNamespace("notification/3"),
         ResourceLocation.withDefaultNamespace("notification/4"),
         ResourceLocation.withDefaultNamespace("notification/5"),
         ResourceLocation.withDefaultNamespace("notification/more")
      };
      private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
      private static final int SIZE = 20;
      private static final int SPRITE_SIZE = 14;
      private int notificationCount;

      public NotificationButton(Component pMessage, ResourceLocation pSprite, Button.OnPress pOnPress) {
         super(20, 20, pMessage, 14, 14, pSprite, pOnPress, null);
      }

      int notificationCount() {
         return this.notificationCount;
      }

      public void setNotificationCount(int pNotificationCount) {
         this.notificationCount = pNotificationCount;
      }

      @Override
      public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
         super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         if (this.active && this.notificationCount != 0) {
            this.drawNotificationCounter(pGuiGraphics);
         }
      }

      private void drawNotificationCounter(GuiGraphics pGuiGraphics) {
         pGuiGraphics.blitSprite(NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class NotificationMessageEntry extends RealmsMainScreen.Entry {
      private static final int SIDE_MARGINS = 40;
      private static final int OUTLINE_COLOR = -12303292;
      private final Component text;
      private final int frameItemHeight;
      private final List<AbstractWidget> children = new ArrayList<>();
      @Nullable
      private final RealmsMainScreen.CrossButton dismissButton;
      private final MultiLineTextWidget textWidget;
      private final GridLayout gridLayout;
      private final FrameLayout textFrame;
      private int lastEntryWidth = -1;

      public NotificationMessageEntry(final Component pText, final int pFrameItemHeight, final RealmsNotification pNotification) {
         this.text = pText;
         this.frameItemHeight = pFrameItemHeight;
         this.gridLayout = new GridLayout();
         int $$3 = 7;
         this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
         this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
         this.textFrame = this.gridLayout.addChild(new FrameLayout(0, 9 * 3 * (pFrameItemHeight - 1)), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
         this.textWidget = this.textFrame
            .addChild(
               new MultiLineTextWidget(pText, RealmsMainScreen.this.font).setCentered(true),
               this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
            );
         this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
         if (pNotification.dismissable()) {
            this.dismissButton = this.gridLayout
               .addChild(
                  new RealmsMainScreen.CrossButton(
                     p_275478_ -> RealmsMainScreen.this.dismissNotification(pNotification.uuid()), Component.translatable("mco.notification.dismiss")
                  ),
                  0,
                  2,
                  this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
               );
         } else {
            this.dismissButton = null;
         }

         this.gridLayout.visitWidgets(this.children::add);
      }

      @Override
      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         return this.dismissButton != null && this.dismissButton.keyPressed(pKeyCode, pScanCode, pModifiers)
            ? true
            : super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      private void updateEntryWidth(int pEntryWidth) {
         if (this.lastEntryWidth != pEntryWidth) {
            this.refreshLayout(pEntryWidth);
            this.lastEntryWidth = pEntryWidth;
         }
      }

      private void refreshLayout(int pWidth) {
         int $$1 = pWidth - 80;
         this.textFrame.setMinWidth($$1);
         this.textWidget.setMaxWidth($$1);
         this.gridLayout.arrangeElements();
      }

      @Override
      public void renderBack(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick
      ) {
         super.renderBack(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, pIsMouseOver, pPartialTick);
         pGuiGraphics.renderOutline(pLeft - 2, pTop - 2, pWidth, 36 * this.frameItemHeight - 2, -12303292);
      }

      @Override
      public void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      ) {
         this.gridLayout.setPosition(pLeft, pTop);
         this.updateEntryWidth(pWidth - 4);
         this.children.forEach(p_280688_ -> p_280688_.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick));
      }

      @Override
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (this.dismissButton != null) {
            this.dismissButton.mouseClicked(pMouseX, pMouseY, pButton);
         }

         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }

      @Override
      public Component getNarration() {
         return this.text;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ParentEntry extends RealmsMainScreen.Entry {
      private final RealmsServer server;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

      public ParentEntry(final RealmsServer pServer) {
         this.server = pServer;
         if (!pServer.expired) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip")));
         }
      }

      @Override
      public void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      ) {
         int $$10 = this.textX(pLeft);
         int $$11 = this.firstLineY(pTop);
         RealmsUtil.renderPlayerFace(pGuiGraphics, pLeft, pTop, 32, this.server.ownerUUID);
         Component $$12 = RealmsMainScreen.getVersionComponent(this.server.activeVersion, -8355712);
         int $$13 = this.versionTextX(pLeft, pWidth, $$12);
         this.renderClampedString(pGuiGraphics, this.server.getName(), $$10, $$11, $$13, -8355712);
         if ($$12 != CommonComponents.EMPTY) {
            pGuiGraphics.drawString(RealmsMainScreen.this.font, $$12, $$13, $$11, -8355712, false);
         }

         pGuiGraphics.drawString(RealmsMainScreen.this.font, this.server.getDescription(), $$10, this.secondLineY($$11), -8355712, false);
         this.renderThirdLine(pGuiGraphics, pTop, pLeft, this.server);
         this.renderStatusLights(this.server, pGuiGraphics, pLeft + pWidth, pTop, pMouseX, pMouseY);
         this.tooltip.refreshTooltipForNextRenderPass(pHovering, this.isFocused(), new ScreenRectangle(pLeft, pTop, pWidth, pHeight));
      }

      @Override
      public Component getNarration() {
         return Component.literal(this.server.name);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
      }

      public void setSelected(@Nullable RealmsMainScreen.Entry p_86849_) {
         super.setSelected(p_86849_);
         RealmsMainScreen.this.updateButtonStates();
      }

      @Override
      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface RealmsCall<T> {
      T request(RealmsClient pRealmsClient) throws RealmsServiceException;
   }

   @OnlyIn(Dist.CLIENT)
   class ServerEntry extends RealmsMainScreen.Entry {
      private static final Component ONLINE_PLAYERS_TOOLTIP_HEADER = Component.translatable("mco.onlinePlayers");
      private static final int PLAYERS_ONLINE_SPRITE_SIZE = 9;
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      private final RealmsServer serverData;
      private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

      public ServerEntry(final RealmsServer pServerData) {
         this.serverData = pServerData;
         boolean $$1 = RealmsMainScreen.isSelfOwnedServer(pServerData);
         if (RealmsMainScreen.isSnapshot() && $$1 && pServerData.isSnapshotRealm()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", pServerData.parentWorldName)));
         } else if (!$$1 && pServerData.needsDowngrade()) {
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", pServerData.activeVersion)));
         }
      }

      @Override
      public void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      ) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            pGuiGraphics.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, pLeft - 5, pTop + pHeight / 2 - 10, 40, 20);
            int $$10 = pTop + pHeight / 2 - 9 / 2;
            pGuiGraphics.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, pLeft + 40 - 2, $$10, 8388479);
         } else {
            this.renderStatusLights(this.serverData, pGuiGraphics, pLeft + 36, pTop, pMouseX, pMouseY);
            RealmsUtil.renderPlayerFace(pGuiGraphics, pLeft, pTop, 32, this.serverData.ownerUUID);
            this.renderFirstLine(pGuiGraphics, pTop, pLeft, pWidth);
            this.renderSecondLine(pGuiGraphics, pTop, pLeft);
            this.renderThirdLine(pGuiGraphics, pTop, pLeft, this.serverData);
            boolean $$11 = this.renderOnlinePlayers(pGuiGraphics, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY);
            this.renderStatusLights(this.serverData, pGuiGraphics, pLeft + pWidth, pTop, pMouseX, pMouseY);
            if (!$$11) {
               this.tooltip.refreshTooltipForNextRenderPass(pHovering, this.isFocused(), new ScreenRectangle(pLeft, pTop, pWidth, pHeight));
            }
         }
      }

      private void renderFirstLine(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pWidth) {
         int $$4 = this.textX(pLeft);
         int $$5 = this.firstLineY(pTop);
         Component $$6 = RealmsMainScreen.getVersionComponent(this.serverData.activeVersion, this.serverData.isCompatible());
         int $$7 = this.versionTextX(pLeft, pWidth, $$6);
         this.renderClampedString(pGuiGraphics, this.serverData.getName(), $$4, $$5, $$7, -1);
         if ($$6 != CommonComponents.EMPTY && !this.serverData.isMinigameActive()) {
            pGuiGraphics.drawString(RealmsMainScreen.this.font, $$6, $$7, $$5, -8355712, false);
         }
      }

      private void renderSecondLine(GuiGraphics pGuiGraphics, int pTop, int pLeft) {
         int $$3 = this.textX(pLeft);
         int $$4 = this.firstLineY(pTop);
         int $$5 = this.secondLineY($$4);
         String $$6 = this.serverData.getMinigameName();
         if (this.serverData.isMinigameActive() && $$6 != null) {
            Component $$7 = Component.literal($$6).withStyle(ChatFormatting.GRAY);
            pGuiGraphics.drawString(
               RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", $$7).withColor(-171), $$3, $$5, -1, false
            );
         } else {
            pGuiGraphics.drawString(RealmsMainScreen.this.font, this.serverData.getDescription(), $$3, this.secondLineY($$4), -8355712, false);
         }
      }

      private boolean renderOnlinePlayers(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY) {
         List<ProfileResult> $$7 = RealmsMainScreen.this.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
         if (!$$7.isEmpty()) {
            int $$8 = pLeft + pWidth - 21;
            int $$9 = pTop + pHeight - 9 - 2;
            int $$10 = $$8;

            for (int $$11 = 0; $$11 < $$7.size(); $$11++) {
               $$10 -= 9 + ($$11 == 0 ? 0 : 3);
               PlayerFaceRenderer.draw(pGuiGraphics, Minecraft.getInstance().getSkinManager().getInsecureSkin($$7.get($$11).profile()), $$10, $$9, 9);
            }

            if (pMouseX >= $$10 && pMouseX <= $$8 && pMouseY >= $$9 && pMouseY <= $$9 + 9) {
               pGuiGraphics.renderTooltip(
                  RealmsMainScreen.this.font,
                  List.of(ONLINE_PLAYERS_TOOLTIP_HEADER),
                  Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip($$7)),
                  pMouseX,
                  pMouseY
               );
               return true;
            }
         }

         return false;
      }

      private void playRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
      }

      private void createUnitializedRealm() {
         RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         RealmsCreateRealmScreen $$0 = new RealmsCreateRealmScreen(RealmsMainScreen.this, this.serverData);
         RealmsMainScreen.this.minecraft.setScreen($$0);
      }

      @Override
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            this.createUnitializedRealm();
         } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
            if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
               this.playRealm();
            }

            RealmsMainScreen.this.lastClickTime = Util.getMillis();
         }

         return true;
      }

      @Override
      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         if (CommonInputs.selected(pKeyCode)) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
               this.createUnitializedRealm();
               return true;
            }

            if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
               this.playRealm();
               return true;
            }
         }

         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      @Override
      public Component getNarration() {
         return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED
            ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
            : Component.translatable("narrator.select", this.serverData.name));
      }

      public RealmsServer getServer() {
         return this.serverData;
      }
   }
}
