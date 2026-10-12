package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   @Nullable
   private String filter;

   public SocialInteractionsPlayerList(
      SocialInteractionsScreen pSocialInteractionsScreen, Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight
   ) {
      super(pMinecraft, pWidth, pHeight, pY, pItemHeight);
      this.socialInteractionsScreen = pSocialInteractionsScreen;
   }

   @Override
   protected void renderListBackground(GuiGraphics pGuiGraphics) {
   }

   @Override
   protected void renderListSeparators(GuiGraphics pGuiGraphics) {
   }

   @Override
   protected void enableScissor(GuiGraphics pGuiGraphics) {
      pGuiGraphics.enableScissor(this.getX(), this.getY() + 4, this.getRight(), this.getBottom());
   }

   public void updatePlayerList(Collection<UUID> pIds, double pScrollAmount, boolean pAddChatLogPlayers) {
      Map<UUID, PlayerEntry> $$3 = new HashMap<>();
      this.addOnlinePlayers(pIds, $$3);
      this.updatePlayersFromChatLog($$3, pAddChatLogPlayers);
      this.updateFiltersAndScroll($$3.values(), pScrollAmount);
   }

   private void addOnlinePlayers(Collection<UUID> pIds, Map<UUID, PlayerEntry> pPlayerMap) {
      ClientPacketListener $$2 = this.minecraft.player.connection;

      for (UUID $$3 : pIds) {
         PlayerInfo $$4 = $$2.getPlayerInfo($$3);
         if ($$4 != null) {
            boolean $$5 = $$4.hasVerifiableChat();
            pPlayerMap.put($$3, new PlayerEntry(this.minecraft, this.socialInteractionsScreen, $$3, $$4.getProfile().getName(), $$4::getSkin, $$5));
         }
      }
   }

   private void updatePlayersFromChatLog(Map<UUID, PlayerEntry> pPlayerMap, boolean pAddPlayers) {
      for (GameProfile $$3 : collectProfilesFromChatLog(this.minecraft.getReportingContext().chatLog())) {
         PlayerEntry $$4;
         if (pAddPlayers) {
            $$4 = pPlayerMap.computeIfAbsent(
               $$3.getId(),
               p_296219_ -> {
                  PlayerEntry $$2 = new PlayerEntry(
                     this.minecraft, this.socialInteractionsScreen, $$3.getId(), $$3.getName(), this.minecraft.getSkinManager().lookupInsecure($$3), true
                  );
                  $$2.setRemoved(true);
                  return $$2;
               }
            );
         } else {
            $$4 = pPlayerMap.get($$3.getId());
            if ($$4 == null) {
               continue;
            }
         }

         $$4.setHasRecentMessages(true);
      }
   }

   private static Collection<GameProfile> collectProfilesFromChatLog(ChatLog pChatLog) {
      Set<GameProfile> $$1 = new ObjectLinkedOpenHashSet();

      for (int $$2 = pChatLog.end(); $$2 >= pChatLog.start(); $$2--) {
         LoggedChatEvent $$3 = pChatLog.lookup($$2);
         if ($$3 instanceof LoggedChatMessage.Player) {
            LoggedChatMessage.Player $$4 = (LoggedChatMessage.Player)$$3;
            if ($$4.message().hasSignature()) {
               $$1.add($$4.profile());
            }
         }
      }

      return $$1;
   }

   private void sortPlayerEntries() {
      this.players.sort(Comparator.<PlayerEntry, Integer>comparing(p_240745_ -> {
         if (this.minecraft.isLocalPlayer(p_240745_.getPlayerId())) {
            return 0;
         } else if (this.minecraft.getReportingContext().hasDraftReportFor(p_240745_.getPlayerId())) {
            return 1;
         } else if (p_240745_.getPlayerId().version() == 2) {
            return 4;
         } else {
            return p_240745_.hasRecentMessages() ? 2 : 3;
         }
      }).thenComparing(p_240744_ -> {
         if (!p_240744_.getPlayerName().isBlank()) {
            int $$1 = p_240744_.getPlayerName().codePointAt(0);
            if ($$1 == 95 || $$1 >= 97 && $$1 <= 122 || $$1 >= 65 && $$1 <= 90 || $$1 >= 48 && $$1 <= 57) {
               return 0;
            }
         }

         return 1;
      }).thenComparing(PlayerEntry::getPlayerName, String::compareToIgnoreCase));
   }

   private void updateFiltersAndScroll(Collection<PlayerEntry> pPlayers, double pScrollAmount) {
      this.players.clear();
      this.players.addAll(pPlayers);
      this.sortPlayerEntries();
      this.updateFilteredPlayers();
      this.replaceEntries(this.players);
      this.setScrollAmount(pScrollAmount);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf(p_100710_ -> !p_100710_.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter));
         this.replaceEntries(this.players);
      }
   }

   public void setFilter(String pFilter) {
      this.filter = pFilter;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo pPlayerInfo, SocialInteractionsScreen.Page pPage) {
      UUID $$2 = pPlayerInfo.getProfile().getId();

      for (PlayerEntry $$3 : this.players) {
         if ($$3.getPlayerId().equals($$2)) {
            $$3.setRemoved(false);
            return;
         }
      }

      if ((pPage == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom($$2))
         && (Strings.isNullOrEmpty(this.filter) || pPlayerInfo.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.filter))) {
         boolean $$4 = pPlayerInfo.hasVerifiableChat();
         PlayerEntry $$5 = new PlayerEntry(
            this.minecraft, this.socialInteractionsScreen, pPlayerInfo.getProfile().getId(), pPlayerInfo.getProfile().getName(), pPlayerInfo::getSkin, $$4
         );
         this.addEntry($$5);
         this.players.add($$5);
      }
   }

   public void removePlayer(UUID pId) {
      for (PlayerEntry $$1 : this.players) {
         if ($$1.getPlayerId().equals(pId)) {
            $$1.setRemoved(true);
            return;
         }
      }
   }
}
