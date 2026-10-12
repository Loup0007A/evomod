package net.minecraft.client.renderer.debug;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BrainDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final boolean SHOW_NAME_FOR_ALL = true;
   private static final boolean SHOW_PROFESSION_FOR_ALL = false;
   private static final boolean SHOW_BEHAVIORS_FOR_ALL = false;
   private static final boolean SHOW_ACTIVITIES_FOR_ALL = false;
   private static final boolean SHOW_INVENTORY_FOR_ALL = false;
   private static final boolean SHOW_GOSSIPS_FOR_ALL = false;
   private static final boolean SHOW_PATH_FOR_ALL = false;
   private static final boolean SHOW_HEALTH_FOR_ALL = false;
   private static final boolean SHOW_WANTS_GOLEM_FOR_ALL = true;
   private static final boolean SHOW_ANGER_LEVEL_FOR_ALL = false;
   private static final boolean SHOW_NAME_FOR_SELECTED = true;
   private static final boolean SHOW_PROFESSION_FOR_SELECTED = true;
   private static final boolean SHOW_BEHAVIORS_FOR_SELECTED = true;
   private static final boolean SHOW_ACTIVITIES_FOR_SELECTED = true;
   private static final boolean SHOW_MEMORIES_FOR_SELECTED = true;
   private static final boolean SHOW_INVENTORY_FOR_SELECTED = true;
   private static final boolean SHOW_GOSSIPS_FOR_SELECTED = true;
   private static final boolean SHOW_PATH_FOR_SELECTED = true;
   private static final boolean SHOW_HEALTH_FOR_SELECTED = true;
   private static final boolean SHOW_WANTS_GOLEM_FOR_SELECTED = true;
   private static final boolean SHOW_ANGER_LEVEL_FOR_SELECTED = true;
   private static final boolean SHOW_POI_INFO = true;
   private static final int MAX_RENDER_DIST_FOR_BRAIN_INFO = 30;
   private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
   private static final int MAX_TARGETING_DIST = 8;
   private static final float TEXT_SCALE = 0.02F;
   private static final int WHITE = -1;
   private static final int YELLOW = -256;
   private static final int CYAN = -16711681;
   private static final int GREEN = -16711936;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private static final int RED = -65536;
   private static final int ORANGE = -23296;
   private final Minecraft minecraft;
   private final Map<BlockPos, BrainDebugRenderer.PoiInfo> pois = Maps.newHashMap();
   private final Map<UUID, BrainDebugPayload.BrainDump> brainDumpsPerEntity = Maps.newHashMap();
   @Nullable
   private UUID lastLookedAtUuid;

   public BrainDebugRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void clear() {
      this.pois.clear();
      this.brainDumpsPerEntity.clear();
      this.lastLookedAtUuid = null;
   }

   public void addPoi(BrainDebugRenderer.PoiInfo pPoiInfo) {
      this.pois.put(pPoiInfo.pos, pPoiInfo);
   }

   public void removePoi(BlockPos pPos) {
      this.pois.remove(pPos);
   }

   public void setFreeTicketCount(BlockPos pPos, int pFreeTicketCount) {
      BrainDebugRenderer.PoiInfo $$2 = this.pois.get(pPos);
      if ($$2 == null) {
         LOGGER.warn("Strange, setFreeTicketCount was called for an unknown POI: {}", pPos);
      } else {
         $$2.freeTicketCount = pFreeTicketCount;
      }
   }

   public void addOrUpdateBrainDump(BrainDebugPayload.BrainDump pBrainDump) {
      this.brainDumpsPerEntity.put(pBrainDump.uuid(), pBrainDump);
   }

   public void removeBrainDump(int pId) {
      this.brainDumpsPerEntity.values().removeIf(p_296278_ -> p_296278_.id() == pId);
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      this.clearRemovedEntities();
      this.doRender(pPoseStack, pBufferSource, pCamX, pCamY, pCamZ);
      if (!this.minecraft.player.isSpectator()) {
         this.updateLastLookedAtUuid();
      }
   }

   private void clearRemovedEntities() {
      this.brainDumpsPerEntity.entrySet().removeIf(p_296287_ -> {
         Entity $$1 = this.minecraft.level.getEntity(p_296287_.getValue().id());
         return $$1 == null || $$1.isRemoved();
      });
   }

   private void doRender(PoseStack pPoseStack, MultiBufferSource pBuffer, double pX, double pY, double pZ) {
      BlockPos $$5 = BlockPos.containing(pX, pY, pZ);
      this.brainDumpsPerEntity.values().forEach(p_296286_ -> {
         if (this.isPlayerCloseEnoughToMob(p_296286_)) {
            this.renderBrainInfo(pPoseStack, pBuffer, p_296286_, pX, pY, pZ);
         }
      });

      for (BlockPos $$6 : this.pois.keySet()) {
         if ($$5.closerThan($$6, 30.0)) {
            highlightPoi(pPoseStack, pBuffer, $$6);
         }
      }

      this.pois.values().forEach(p_269718_ -> {
         if ($$5.closerThan(p_269718_.pos, 30.0)) {
            this.renderPoiInfo(pPoseStack, pBuffer, p_269718_);
         }
      });
      this.getGhostPois().forEach((p_269707_, p_269708_) -> {
         if ($$5.closerThan(p_269707_, 30.0)) {
            this.renderGhostPoi(pPoseStack, pBuffer, p_269707_, (List<String>)p_269708_);
         }
      });
   }

   private static void highlightPoi(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pPos) {
      float $$3 = 0.05F;
      DebugRenderer.renderFilledBox(pPoseStack, pBuffer, pPos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
   }

   private void renderGhostPoi(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pPos, List<String> pPoiName) {
      float $$4 = 0.05F;
      DebugRenderer.renderFilledBox(pPoseStack, pBuffer, pPos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
      renderTextOverPos(pPoseStack, pBuffer, pPoiName + "", pPos, 0, -256);
      renderTextOverPos(pPoseStack, pBuffer, "Ghost POI", pPos, 1, -65536);
   }

   private void renderPoiInfo(PoseStack pPoseStack, MultiBufferSource pBuffer, BrainDebugRenderer.PoiInfo pPoiInfo) {
      int $$3 = 0;
      Set<String> $$4 = this.getTicketHolderNames(pPoiInfo);
      if ($$4.size() < 4) {
         renderTextOverPoi(pPoseStack, pBuffer, "Owners: " + $$4, pPoiInfo, $$3, -256);
      } else {
         renderTextOverPoi(pPoseStack, pBuffer, $$4.size() + " ticket holders", pPoiInfo, $$3, -256);
      }

      $$3++;
      Set<String> $$5 = this.getPotentialTicketHolderNames(pPoiInfo);
      if ($$5.size() < 4) {
         renderTextOverPoi(pPoseStack, pBuffer, "Candidates: " + $$5, pPoiInfo, $$3, -23296);
      } else {
         renderTextOverPoi(pPoseStack, pBuffer, $$5.size() + " potential owners", pPoiInfo, $$3, -23296);
      }

      renderTextOverPoi(pPoseStack, pBuffer, "Free tickets: " + pPoiInfo.freeTicketCount, pPoiInfo, ++$$3, -256);
      renderTextOverPoi(pPoseStack, pBuffer, pPoiInfo.type, pPoiInfo, ++$$3, -1);
   }

   private void renderPath(PoseStack pPoseStack, MultiBufferSource pBuffer, BrainDebugPayload.BrainDump pBrainDump, double pX, double pY, double pZ) {
      if (pBrainDump.path() != null) {
         PathfindingRenderer.renderPath(pPoseStack, pBuffer, pBrainDump.path(), 0.5F, false, false, pX, pY, pZ);
      }
   }

   private void renderBrainInfo(PoseStack pPoseStack, MultiBufferSource pBuffer, BrainDebugPayload.BrainDump pBrainDump, double pX, double pY, double pZ) {
      boolean $$6 = this.isMobSelected(pBrainDump);
      int $$7 = 0;
      renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, pBrainDump.name(), -1, 0.03F);
      $$7++;
      if ($$6) {
         renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, pBrainDump.profession() + " " + pBrainDump.xp() + " xp", -1, 0.02F);
         $$7++;
      }

      if ($$6) {
         int $$8 = pBrainDump.health() < pBrainDump.maxHealth() ? -23296 : -1;
         renderTextOverMob(
            pPoseStack,
            pBuffer,
            pBrainDump.pos(),
            $$7,
            "health: " + String.format(Locale.ROOT, "%.1f", pBrainDump.health()) + " / " + String.format(Locale.ROOT, "%.1f", pBrainDump.maxHealth()),
            $$8,
            0.02F
         );
         $$7++;
      }

      if ($$6 && !pBrainDump.inventory().equals("")) {
         renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, pBrainDump.inventory(), -98404, 0.02F);
         $$7++;
      }

      if ($$6) {
         for (String $$9 : pBrainDump.behaviors()) {
            renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, $$9, -16711681, 0.02F);
            $$7++;
         }
      }

      if ($$6) {
         for (String $$10 : pBrainDump.activities()) {
            renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, $$10, -16711936, 0.02F);
            $$7++;
         }
      }

      if (pBrainDump.wantsGolem()) {
         renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, "Wants Golem", -23296, 0.02F);
         $$7++;
      }

      if ($$6 && pBrainDump.angerLevel() != -1) {
         renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, "Anger Level: " + pBrainDump.angerLevel(), -98404, 0.02F);
         $$7++;
      }

      if ($$6) {
         for (String $$11 : pBrainDump.gossips()) {
            if ($$11.startsWith(pBrainDump.name())) {
               renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, $$11, -1, 0.02F);
            } else {
               renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, $$11, -23296, 0.02F);
            }

            $$7++;
         }
      }

      if ($$6) {
         for (String $$12 : Lists.reverse(pBrainDump.memories())) {
            renderTextOverMob(pPoseStack, pBuffer, pBrainDump.pos(), $$7, $$12, -3355444, 0.02F);
            $$7++;
         }
      }

      if ($$6) {
         this.renderPath(pPoseStack, pBuffer, pBrainDump, pX, pY, pZ);
      }
   }

   private static void renderTextOverPoi(
      PoseStack pPoseStack, MultiBufferSource pBuffer, String pText, BrainDebugRenderer.PoiInfo pPoiInfo, int pLayer, int pColor
   ) {
      renderTextOverPos(pPoseStack, pBuffer, pText, pPoiInfo.pos, pLayer, pColor);
   }

   private static void renderTextOverPos(PoseStack pPoseStack, MultiBufferSource pBuffer, String pText, BlockPos pPos, int pLayer, int pColor) {
      double $$6 = 1.3;
      double $$7 = 0.2;
      double $$8 = (double)pPos.getX() + 0.5;
      double $$9 = (double)pPos.getY() + 1.3 + (double)pLayer * 0.2;
      double $$10 = (double)pPos.getZ() + 0.5;
      DebugRenderer.renderFloatingText(pPoseStack, pBuffer, pText, $$8, $$9, $$10, pColor, 0.02F, true, 0.0F, true);
   }

   private static void renderTextOverMob(PoseStack pPoseStack, MultiBufferSource pBuffer, Position pPos, int pLayer, String pText, int pColor, float pScale) {
      double $$7 = 2.4;
      double $$8 = 0.25;
      BlockPos $$9 = BlockPos.containing(pPos);
      double $$10 = (double)$$9.getX() + 0.5;
      double $$11 = pPos.y() + 2.4 + (double)pLayer * 0.25;
      double $$12 = (double)$$9.getZ() + 0.5;
      float $$13 = 0.5F;
      DebugRenderer.renderFloatingText(pPoseStack, pBuffer, pText, $$10, $$11, $$12, pColor, pScale, false, 0.5F, true);
   }

   private Set<String> getTicketHolderNames(BrainDebugRenderer.PoiInfo pPoiInfo) {
      return this.getTicketHolders(pPoiInfo.pos).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private Set<String> getPotentialTicketHolderNames(BrainDebugRenderer.PoiInfo pPoiInfo) {
      return this.getPotentialTicketHolders(pPoiInfo.pos).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private boolean isMobSelected(BrainDebugPayload.BrainDump pBrainDump) {
      return Objects.equals(this.lastLookedAtUuid, pBrainDump.uuid());
   }

   private boolean isPlayerCloseEnoughToMob(BrainDebugPayload.BrainDump pBrainDump) {
      Player $$1 = this.minecraft.player;
      BlockPos $$2 = BlockPos.containing($$1.getX(), pBrainDump.pos().y(), $$1.getZ());
      BlockPos $$3 = BlockPos.containing(pBrainDump.pos());
      return $$2.closerThan($$3, 30.0);
   }

   private Collection<UUID> getTicketHolders(BlockPos pPos) {
      return this.brainDumpsPerEntity
         .values()
         .stream()
         .filter(p_296276_ -> p_296276_.hasPoi(pPos))
         .map(BrainDebugPayload.BrainDump::uuid)
         .collect(Collectors.toSet());
   }

   private Collection<UUID> getPotentialTicketHolders(BlockPos pPos) {
      return this.brainDumpsPerEntity
         .values()
         .stream()
         .filter(p_296280_ -> p_296280_.hasPotentialPoi(pPos))
         .map(BrainDebugPayload.BrainDump::uuid)
         .collect(Collectors.toSet());
   }

   private Map<BlockPos, List<String>> getGhostPois() {
      Map<BlockPos, List<String>> $$0 = Maps.newHashMap();

      for (BrainDebugPayload.BrainDump $$1 : this.brainDumpsPerEntity.values()) {
         for (BlockPos $$2 : Iterables.concat($$1.pois(), $$1.potentialPois())) {
            if (!this.pois.containsKey($$2)) {
               $$0.computeIfAbsent($$2, p_113292_ -> Lists.newArrayList()).add($$1.name());
            }
         }
      }

      return $$0;
   }

   private void updateLastLookedAtUuid() {
      DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113212_ -> this.lastLookedAtUuid = p_113212_.getUUID());
   }

   @OnlyIn(Dist.CLIENT)
   public static class PoiInfo {
      public final BlockPos pos;
      public final String type;
      public int freeTicketCount;

      public PoiInfo(BlockPos pPos, String pType, int pFreeTicketCount) {
         this.pos = pPos;
         this.type = pType;
         this.freeTicketCount = pFreeTicketCount;
      }
   }
}
