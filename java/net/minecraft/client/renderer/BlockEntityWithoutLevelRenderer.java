package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockEntityWithoutLevelRenderer implements ResourceManagerReloadListener {
   private static final ShulkerBoxBlockEntity[] SHULKER_BOXES = Arrays.stream(DyeColor.values())
      .sorted(Comparator.comparingInt(DyeColor::getId))
      .map(p_172557_ -> new ShulkerBoxBlockEntity(p_172557_, BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState()))
      .toArray(ShulkerBoxBlockEntity[]::new);
   private static final ShulkerBoxBlockEntity DEFAULT_SHULKER_BOX = new ShulkerBoxBlockEntity(BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
   private final ChestBlockEntity chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
   private final ChestBlockEntity trappedChest = new TrappedChestBlockEntity(BlockPos.ZERO, Blocks.TRAPPED_CHEST.defaultBlockState());
   private final EnderChestBlockEntity enderChest = new EnderChestBlockEntity(BlockPos.ZERO, Blocks.ENDER_CHEST.defaultBlockState());
   private final BannerBlockEntity banner = new BannerBlockEntity(BlockPos.ZERO, Blocks.WHITE_BANNER.defaultBlockState());
   private final BedBlockEntity bed = new BedBlockEntity(BlockPos.ZERO, Blocks.RED_BED.defaultBlockState());
   private final ConduitBlockEntity conduit = new ConduitBlockEntity(BlockPos.ZERO, Blocks.CONDUIT.defaultBlockState());
   private final DecoratedPotBlockEntity decoratedPot = new DecoratedPotBlockEntity(BlockPos.ZERO, Blocks.DECORATED_POT.defaultBlockState());
   private ShieldModel shieldModel;
   private TridentModel tridentModel;
   private Map<SkullBlock.Type, SkullModelBase> skullModels;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final EntityModelSet entityModelSet;

   public BlockEntityWithoutLevelRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
      this.blockEntityRenderDispatcher = pBlockEntityRenderDispatcher;
      this.entityModelSet = pEntityModelSet;
   }

   @Override
   public void onResourceManagerReload(ResourceManager pResourceManager) {
      this.shieldModel = new ShieldModel(this.entityModelSet.bakeLayer(ModelLayers.SHIELD));
      this.tridentModel = new TridentModel(this.entityModelSet.bakeLayer(ModelLayers.TRIDENT));
      this.skullModels = SkullBlockRenderer.createSkullRenderers(this.entityModelSet);
   }

   public void renderByItem(
      ItemStack pStack, ItemDisplayContext pDisplayContext, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay
   ) {
      Item $$6 = pStack.getItem();
      if ($$6 instanceof BlockItem) {
         Block $$7 = ((BlockItem)$$6).getBlock();
         if ($$7 instanceof AbstractSkullBlock $$8) {
            ResolvableProfile $$9 = pStack.get(DataComponents.PROFILE);
            if ($$9 != null && !$$9.isResolved()) {
               pStack.remove(DataComponents.PROFILE);
               $$9.resolve().thenAcceptAsync(p_325494_ -> pStack.set(DataComponents.PROFILE, p_325494_), Minecraft.getInstance());
               $$9 = null;
            }

            SkullModelBase $$10 = this.skullModels.get($$8.getType());
            RenderType $$11 = SkullBlockRenderer.getRenderType($$8.getType(), $$9);
            SkullBlockRenderer.renderSkull(null, 180.0F, 0.0F, pPoseStack, pBuffer, pPackedLight, $$10, $$11);
         } else {
            BlockState $$12 = $$7.defaultBlockState();
            BlockEntity $$13;
            if ($$7 instanceof AbstractBannerBlock) {
               this.banner.fromItem(pStack, ((AbstractBannerBlock)$$7).getColor());
               $$13 = this.banner;
            } else if ($$7 instanceof BedBlock) {
               this.bed.setColor(((BedBlock)$$7).getColor());
               $$13 = this.bed;
            } else if ($$12.is(Blocks.CONDUIT)) {
               $$13 = this.conduit;
            } else if ($$12.is(Blocks.CHEST)) {
               $$13 = this.chest;
            } else if ($$12.is(Blocks.ENDER_CHEST)) {
               $$13 = this.enderChest;
            } else if ($$12.is(Blocks.TRAPPED_CHEST)) {
               $$13 = this.trappedChest;
            } else if ($$12.is(Blocks.DECORATED_POT)) {
               this.decoratedPot.setFromItem(pStack);
               $$13 = this.decoratedPot;
            } else {
               if (!($$7 instanceof ShulkerBoxBlock)) {
                  return;
               }

               DyeColor $$20 = ShulkerBoxBlock.getColorFromItem($$6);
               if ($$20 == null) {
                  $$13 = DEFAULT_SHULKER_BOX;
               } else {
                  $$13 = SHULKER_BOXES[$$20.getId()];
               }
            }

            this.blockEntityRenderDispatcher.renderItem($$13, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
         }
      } else {
         if (pStack.is(Items.SHIELD)) {
            BannerPatternLayers $$24 = pStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
            DyeColor $$25 = pStack.get(DataComponents.BASE_COLOR);
            boolean $$26 = !$$24.layers().isEmpty() || $$25 != null;
            pPoseStack.pushPose();
            pPoseStack.scale(1.0F, -1.0F, -1.0F);
            Material $$27 = $$26 ? ModelBakery.SHIELD_BASE : ModelBakery.NO_PATTERN_SHIELD;
            VertexConsumer $$28 = $$27.sprite()
               .wrap(ItemRenderer.getFoilBufferDirect(pBuffer, this.shieldModel.renderType($$27.atlasLocation()), true, pStack.hasFoil()));
            this.shieldModel.handle().render(pPoseStack, $$28, pPackedLight, pPackedOverlay);
            if ($$26) {
               BannerRenderer.renderPatterns(
                  pPoseStack,
                  pBuffer,
                  pPackedLight,
                  pPackedOverlay,
                  this.shieldModel.plate(),
                  $$27,
                  false,
                  Objects.requireNonNullElse($$25, DyeColor.WHITE),
                  $$24,
                  pStack.hasFoil()
               );
            } else {
               this.shieldModel.plate().render(pPoseStack, $$28, pPackedLight, pPackedOverlay);
            }

            pPoseStack.popPose();
         } else if (pStack.is(Items.TRIDENT)) {
            pPoseStack.pushPose();
            pPoseStack.scale(1.0F, -1.0F, -1.0F);
            VertexConsumer $$29 = ItemRenderer.getFoilBufferDirect(pBuffer, this.tridentModel.renderType(TridentModel.TEXTURE), false, pStack.hasFoil());
            this.tridentModel.renderToBuffer(pPoseStack, $$29, pPackedLight, pPackedOverlay);
            pPoseStack.popPose();
         }
      }
   }
}
