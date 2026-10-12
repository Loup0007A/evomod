package net.minecraft.client.gui.spectator.categories;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TeleportToTeamMenuCategory implements SpectatorMenuCategory, SpectatorMenuItem {
   private static final ResourceLocation TELEPORT_TO_TEAM_SPRITE = ResourceLocation.withDefaultNamespace("spectator/teleport_to_team");
   private static final Component TELEPORT_TEXT = Component.translatable("spectatorMenu.team_teleport");
   private static final Component TELEPORT_PROMPT = Component.translatable("spectatorMenu.team_teleport.prompt");
   private final List<SpectatorMenuItem> items;

   public TeleportToTeamMenuCategory() {
      Minecraft $$0 = Minecraft.getInstance();
      this.items = createTeamEntries($$0, $$0.level.getScoreboard());
   }

   private static List<SpectatorMenuItem> createTeamEntries(Minecraft pMinecraft, Scoreboard pScoreboard) {
      return pScoreboard.getPlayerTeams()
         .stream()
         .flatMap(p_260025_ -> TeleportToTeamMenuCategory.TeamSelectionItem.create(pMinecraft, p_260025_).stream())
         .toList();
   }

   @Override
   public List<SpectatorMenuItem> getItems() {
      return this.items;
   }

   @Override
   public Component getPrompt() {
      return TELEPORT_PROMPT;
   }

   @Override
   public void selectItem(SpectatorMenu pMenu) {
      pMenu.selectCategory(this);
   }

   @Override
   public Component getName() {
      return TELEPORT_TEXT;
   }

   @Override
   public void renderIcon(GuiGraphics pGuiGraphics, float pShadeColor, int pAlpha) {
      pGuiGraphics.blitSprite(TELEPORT_TO_TEAM_SPRITE, 0, 0, 16, 16);
   }

   @Override
   public boolean isEnabled() {
      return !this.items.isEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   static class TeamSelectionItem implements SpectatorMenuItem {
      private final PlayerTeam team;
      private final Supplier<PlayerSkin> iconSkin;
      private final List<PlayerInfo> players;

      private TeamSelectionItem(PlayerTeam pTeam, List<PlayerInfo> pPlayers, Supplier<PlayerSkin> pIconSkin) {
         this.team = pTeam;
         this.players = pPlayers;
         this.iconSkin = pIconSkin;
      }

      public static Optional<SpectatorMenuItem> create(Minecraft pMinecraft, PlayerTeam pTeam) {
         List<PlayerInfo> $$2 = new ArrayList<>();

         for (String $$3 : pTeam.getPlayers()) {
            PlayerInfo $$4 = pMinecraft.getConnection().getPlayerInfo($$3);
            if ($$4 != null && $$4.getGameMode() != GameType.SPECTATOR) {
               $$2.add($$4);
            }
         }

         if ($$2.isEmpty()) {
            return Optional.empty();
         } else {
            GameProfile $$5 = $$2.get(RandomSource.create().nextInt($$2.size())).getProfile();
            Supplier<PlayerSkin> $$6 = pMinecraft.getSkinManager().lookupInsecure($$5);
            return Optional.of(new TeleportToTeamMenuCategory.TeamSelectionItem(pTeam, $$2, $$6));
         }
      }

      @Override
      public void selectItem(SpectatorMenu pMenu) {
         pMenu.selectCategory(new TeleportToPlayerMenuCategory(this.players));
      }

      @Override
      public Component getName() {
         return this.team.getDisplayName();
      }

      @Override
      public void renderIcon(GuiGraphics pGuiGraphics, float pShadeColor, int pAlpha) {
         Integer $$3 = this.team.getColor().getColor();
         if ($$3 != null) {
            float $$4 = (float)($$3 >> 16 & 0xFF) / 255.0F;
            float $$5 = (float)($$3 >> 8 & 0xFF) / 255.0F;
            float $$6 = (float)($$3 & 0xFF) / 255.0F;
            pGuiGraphics.fill(1, 1, 15, 15, Mth.color($$4 * pShadeColor, $$5 * pShadeColor, $$6 * pShadeColor) | pAlpha << 24);
         }

         pGuiGraphics.setColor(pShadeColor, pShadeColor, pShadeColor, (float)pAlpha / 255.0F);
         PlayerFaceRenderer.draw(pGuiGraphics, this.iconSkin.get(), 2, 2, 12);
         pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      @Override
      public boolean isEnabled() {
         return true;
      }
   }
}
