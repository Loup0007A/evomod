package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.common.custom.BeeDebugPayload;
import net.minecraft.network.protocol.common.custom.HiveDebugPayload;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final boolean SHOW_GOAL_FOR_ALL_BEES = true;
   private static final boolean SHOW_NAME_FOR_ALL_BEES = true;
   private static final boolean SHOW_HIVE_FOR_ALL_BEES = true;
   private static final boolean SHOW_FLOWER_POS_FOR_ALL_BEES = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_ALL_BEES = true;
   private static final boolean SHOW_PATH_FOR_ALL_BEES = false;
   private static final boolean SHOW_GOAL_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_NAME_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_FLOWER_POS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_TRAVEL_TICKS_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_PATH_FOR_SELECTED_BEE = true;
   private static final boolean SHOW_HIVE_MEMBERS = true;
   private static final boolean SHOW_BLACKLISTS = true;
   private static final int MAX_RENDER_DIST_FOR_HIVE_OVERLAY = 30;
   private static final int MAX_RENDER_DIST_FOR_BEE_OVERLAY = 30;
   private static final int MAX_TARGETING_DIST = 8;
   private static final int HIVE_TIMEOUT = 20;
   private static final float TEXT_SCALE = 0.02F;
   private static final int WHITE = -1;
   private static final int YELLOW = -256;
   private static final int ORANGE = -23296;
   private static final int GREEN = -16711936;
   private static final int GRAY = -3355444;
   private static final int PINK = -98404;
   private static final int RED = -65536;
   private final Minecraft minecraft;
   private final Map<BlockPos, BeeDebugRenderer.HiveDebugInfo> hives = new HashMap<>();
   private final Map<UUID, BeeDebugPayload.BeeInfo> beeInfosPerEntity = new HashMap<>();
   @Nullable
   private UUID lastLookedAtUuid;

   public BeeDebugRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void clear() {
      this.hives.clear();
      this.beeInfosPerEntity.clear();
      this.lastLookedAtUuid = null;
   }

   public void addOrUpdateHiveInfo(HiveDebugPayload.HiveInfo pHiveInfo, long pLastSeen) {
      this.hives.put(pHiveInfo.pos(), new BeeDebugRenderer.HiveDebugInfo(pHiveInfo, pLastSeen));
   }

   public void addOrUpdateBeeInfo(BeeDebugPayload.BeeInfo pBeeInfo) {
      this.beeInfosPerEntity.put(pBeeInfo.uuid(), pBeeInfo);
   }

   public void removeBeeInfo(int pId) {
      this.beeInfosPerEntity.values().removeIf(p_296269_ -> p_296269_.id() == pId);
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      this.clearRemovedHives();
      this.clearRemovedBees();
      this.doRender(pPoseStack, pBufferSource);
      if (!this.minecraft.player.isSpectator()) {
         this.updateLastLookedAtUuid();
      }
   }

   private void clearRemovedBees() {
      this.beeInfosPerEntity.entrySet().removeIf(p_296260_ -> this.minecraft.level.getEntity(p_296260_.getValue().id()) == null);
   }

   private void clearRemovedHives() {
      long $$0 = this.minecraft.level.getGameTime() - 20L;
      this.hives.entrySet().removeIf(p_296254_ -> p_296254_.getValue().lastSeen() < $$0);
   }

   private void doRender(PoseStack pPoseStack, MultiBufferSource pBuffer) {
      BlockPos $$2 = this.getCamera().getBlockPosition();
      this.beeInfosPerEntity.values().forEach(p_296263_ -> {
         if (this.isPlayerCloseEnoughToMob(p_296263_)) {
            this.renderBeeInfo(pPoseStack, pBuffer, p_296263_);
         }
      });
      this.renderFlowerInfos(pPoseStack, pBuffer);

      for (BlockPos $$3 : this.hives.keySet()) {
         if ($$2.closerThan($$3, 30.0)) {
            highlightHive(pPoseStack, pBuffer, $$3);
         }
      }

      Map<BlockPos, Set<UUID>> $$4 = this.createHiveBlacklistMap();
      this.hives.values().forEach(p_296259_ -> {
         if ($$2.closerThan(p_296259_.info.pos(), 30.0)) {
            Set<UUID> $$5 = $$4.get(p_296259_.info.pos());
            this.renderHiveInfo(pPoseStack, pBuffer, p_296259_.info, (Collection<UUID>)($$5 == null ? Sets.newHashSet() : $$5));
         }
      });
      this.getGhostHives().forEach((p_269699_, p_269700_) -> {
         if ($$2.closerThan(p_269699_, 30.0)) {
            this.renderGhostHive(pPoseStack, pBuffer, p_269699_, (List<String>)p_269700_);
         }
      });
   }

   private Map<BlockPos, Set<UUID>> createHiveBlacklistMap() {
      Map<BlockPos, Set<UUID>> $$0 = Maps.newHashMap();
      this.beeInfosPerEntity
         .values()
         .forEach(
            p_296271_ -> p_296271_.blacklistedHives()
                  .forEach(p_296274_ -> $$0.computeIfAbsent(p_296274_, p_173777_ -> Sets.newHashSet()).add(p_296271_.uuid()))
         );
      return $$0;
   }

   private void renderFlowerInfos(PoseStack pPoseStack, MultiBufferSource pBuffer) {
      Map<BlockPos, Set<UUID>> $$2 = Maps.newHashMap();
      this.beeInfosPerEntity.values().forEach(p_296251_ -> {
         if (p_296251_.flowerPos() != null) {
            $$2.computeIfAbsent(p_296251_.flowerPos(), p_296252_ -> new HashSet<>()).add(p_296251_.uuid());
         }
      });
      $$2.forEach((p_325526_, p_325527_) -> {
         Set<String> $$4 = p_325527_.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
         int $$5 = 1;
         renderTextOverPos(pPoseStack, pBuffer, $$4.toString(), p_325526_, $$5++, -256);
         renderTextOverPos(pPoseStack, pBuffer, "Flower", p_325526_, $$5++, -1);
         float $$6 = 0.05F;
         DebugRenderer.renderFilledBox(pPoseStack, pBuffer, p_325526_, 0.05F, 0.8F, 0.8F, 0.0F, 0.3F);
      });
   }

   private static String getBeeUuidsAsString(Collection<UUID> pBeeUuids) {
      if (pBeeUuids.isEmpty()) {
         return "-";
      } else {
         return pBeeUuids.size() > 3
            ? pBeeUuids.size() + " bees"
            : pBeeUuids.stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet()).toString();
      }
   }

   private static void highlightHive(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pHivePos) {
      float $$3 = 0.05F;
      DebugRenderer.renderFilledBox(pPoseStack, pBuffer, pHivePos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
   }

   private void renderGhostHive(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pHivePos, List<String> pGhostHives) {
      float $$4 = 0.05F;
      DebugRenderer.renderFilledBox(pPoseStack, pBuffer, pHivePos, 0.05F, 0.2F, 0.2F, 1.0F, 0.3F);
      renderTextOverPos(pPoseStack, pBuffer, pGhostHives + "", pHivePos, 0, -256);
      renderTextOverPos(pPoseStack, pBuffer, "Ghost Hive", pHivePos, 1, -65536);
   }

   private void renderHiveInfo(PoseStack pPoseStack, MultiBufferSource pBuffer, HiveDebugPayload.HiveInfo pHiveInfo, Collection<UUID> pBeeUuids) {
      int $$4 = 0;
      if (!pBeeUuids.isEmpty()) {
         renderTextOverHive(pPoseStack, pBuffer, "Blacklisted by " + getBeeUuidsAsString(pBeeUuids), pHiveInfo, $$4++, -65536);
      }

      renderTextOverHive(pPoseStack, pBuffer, "Out: " + getBeeUuidsAsString(this.getHiveMembers(pHiveInfo.pos())), pHiveInfo, $$4++, -3355444);
      if (pHiveInfo.occupantCount() == 0) {
         renderTextOverHive(pPoseStack, pBuffer, "In: -", pHiveInfo, $$4++, -256);
      } else if (pHiveInfo.occupantCount() == 1) {
         renderTextOverHive(pPoseStack, pBuffer, "In: 1 bee", pHiveInfo, $$4++, -256);
      } else {
         renderTextOverHive(pPoseStack, pBuffer, "In: " + pHiveInfo.occupantCount() + " bees", pHiveInfo, $$4++, -256);
      }

      renderTextOverHive(pPoseStack, pBuffer, "Honey: " + pHiveInfo.honeyLevel(), pHiveInfo, $$4++, -23296);
      renderTextOverHive(pPoseStack, pBuffer, pHiveInfo.hiveType() + (pHiveInfo.sedated() ? " (sedated)" : ""), pHiveInfo, $$4++, -1);
   }

   private void renderPath(PoseStack pPoseStack, MultiBufferSource pBuffer, BeeDebugPayload.BeeInfo pBeeInfo) {
      if (pBeeInfo.path() != null) {
         PathfindingRenderer.renderPath(
            pPoseStack,
            pBuffer,
            pBeeInfo.path(),
            0.5F,
            false,
            false,
            this.getCamera().getPosition().x(),
            this.getCamera().getPosition().y(),
            this.getCamera().getPosition().z()
         );
      }
   }

   private void renderBeeInfo(PoseStack pPoseStack, MultiBufferSource pBuffer, BeeDebugPayload.BeeInfo pBeeInfo) {
      boolean $$3 = this.isBeeSelected(pBeeInfo);
      int $$4 = 0;
      renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), $$4++, pBeeInfo.toString(), -1, 0.03F);
      if (pBeeInfo.hivePos() == null) {
         renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), $$4++, "No hive", -98404, 0.02F);
      } else {
         renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), $$4++, "Hive: " + this.getPosDescription(pBeeInfo, pBeeInfo.hivePos()), -256, 0.02F);
      }

      if (pBeeInfo.flowerPos() == null) {
         renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), $$4++, "No flower", -98404, 0.02F);
      } else {
         renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), $$4++, "Flower: " + this.getPosDescription(pBeeInfo, pBeeInfo.flowerPos()), -256, 0.02F);
      }

      for (String $$5 : pBeeInfo.goals()) {
         renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), $$4++, $$5, -16711936, 0.02F);
      }

      if ($$3) {
         this.renderPath(pPoseStack, pBuffer, pBeeInfo);
      }

      if (pBeeInfo.travelTicks() > 0) {
         int $$6 = pBeeInfo.travelTicks() < 600 ? -3355444 : -23296;
         renderTextOverMob(pPoseStack, pBuffer, pBeeInfo.pos(), $$4++, "Travelling: " + pBeeInfo.travelTicks() + " ticks", $$6, 0.02F);
      }
   }

   private static void renderTextOverHive(
      PoseStack pPoseStack, MultiBufferSource pBuffer, String pText, HiveDebugPayload.HiveInfo pHiveInfo, int pLayer, int pColor
   ) {
      renderTextOverPos(pPoseStack, pBuffer, pText, pHiveInfo.pos(), pLayer, pColor);
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

   private Camera getCamera() {
      return this.minecraft.gameRenderer.getMainCamera();
   }

   private Set<String> getHiveMemberNames(HiveDebugPayload.HiveInfo pHiveInfo) {
      return this.getHiveMembers(pHiveInfo.pos()).stream().map(DebugEntityNameGenerator::getEntityName).collect(Collectors.toSet());
   }

   private String getPosDescription(BeeDebugPayload.BeeInfo pBeeInfo, BlockPos pPos) {
      double $$2 = Math.sqrt(pPos.distToCenterSqr(pBeeInfo.pos()));
      double $$3 = (double)Math.round($$2 * 10.0) / 10.0;
      return pPos.toShortString() + " (dist " + $$3 + ")";
   }

   private boolean isBeeSelected(BeeDebugPayload.BeeInfo pBeeInfo) {
      return Objects.equals(this.lastLookedAtUuid, pBeeInfo.uuid());
   }

   private boolean isPlayerCloseEnoughToMob(BeeDebugPayload.BeeInfo pBeeInfo) {
      Player $$1 = this.minecraft.player;
      BlockPos $$2 = BlockPos.containing($$1.getX(), pBeeInfo.pos().y(), $$1.getZ());
      BlockPos $$3 = BlockPos.containing(pBeeInfo.pos());
      return $$2.closerThan($$3, 30.0);
   }

   private Collection<UUID> getHiveMembers(BlockPos pPos) {
      return this.beeInfosPerEntity
         .values()
         .stream()
         .filter(p_296249_ -> p_296249_.hasHive(pPos))
         .map(BeeDebugPayload.BeeInfo::uuid)
         .collect(Collectors.toSet());
   }

   private Map<BlockPos, List<String>> getGhostHives() {
      Map<BlockPos, List<String>> $$0 = Maps.newHashMap();

      for (BeeDebugPayload.BeeInfo $$1 : this.beeInfosPerEntity.values()) {
         if ($$1.hivePos() != null && !this.hives.containsKey($$1.hivePos())) {
            $$0.computeIfAbsent($$1.hivePos(), p_113140_ -> Lists.newArrayList()).add($$1.generateName());
         }
      }

      return $$0;
   }

   private void updateLastLookedAtUuid() {
      DebugRenderer.getTargetedEntity(this.minecraft.getCameraEntity(), 8).ifPresent(p_113059_ -> this.lastLookedAtUuid = p_113059_.getUUID());
   }

   @OnlyIn(Dist.CLIENT)
   static record HiveDebugInfo(HiveDebugPayload.HiveInfo info, long lastSeen) {
   }
}
