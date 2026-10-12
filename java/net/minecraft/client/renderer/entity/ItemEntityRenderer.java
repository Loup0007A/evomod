package net.minecraft.client.renderer.entity;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemEntityRenderer extends EntityRenderer<ItemEntity> {
   private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_X = 0.0F;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_Y = 0.0F;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_Z = 0.09375F;
   private final ItemRenderer itemRenderer;
   private final RandomSource random = RandomSource.create();

   public ItemEntityRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.itemRenderer = pContext.getItemRenderer();
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   public ResourceLocation getTextureLocation(ItemEntity pEntity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   public void render(ItemEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      pPoseStack.pushPose();
      ItemStack $$6 = pEntity.getItem();
      this.random.setSeed((long)getSeedForItemStack($$6));
      BakedModel $$7 = this.itemRenderer.getModel($$6, pEntity.level(), null, pEntity.getId());
      boolean $$8 = $$7.isGui3d();
      float $$9 = 0.25F;
      float $$10 = Mth.sin(((float)pEntity.getAge() + pPartialTicks) / 10.0F + pEntity.bobOffs) * 0.1F + 0.1F;
      float $$11 = $$7.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
      pPoseStack.translate(0.0F, $$10 + 0.25F * $$11, 0.0F);
      float $$12 = pEntity.getSpin(pPartialTicks);
      pPoseStack.mulPose(Axis.YP.rotation($$12));
      renderMultipleFromCount(this.itemRenderer, pPoseStack, pBuffer, pPackedLight, $$6, $$7, $$8, this.random);
      pPoseStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
   }

   public static int getSeedForItemStack(ItemStack pStack) {
      return pStack.isEmpty() ? 187 : Item.getId(pStack.getItem()) + pStack.getDamageValue();
   }

   @VisibleForTesting
   static int getRenderedAmount(int pCount) {
      if (pCount <= 1) {
         return 1;
      } else if (pCount <= 16) {
         return 2;
      } else if (pCount <= 32) {
         return 3;
      } else {
         return pCount <= 48 ? 4 : 5;
      }
   }

   public static void renderMultipleFromCount(
      ItemRenderer pItemRenderer, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, ItemStack pItem, RandomSource pRandom, Level pLevel
   ) {
      BakedModel $$7 = pItemRenderer.getModel(pItem, pLevel, null, 0);
      renderMultipleFromCount(pItemRenderer, pPoseStack, pBuffer, pPackedLight, pItem, $$7, $$7.isGui3d(), pRandom);
   }

   public static void renderMultipleFromCount(
      ItemRenderer pItemRenderer,
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      ItemStack pItem,
      BakedModel pModel,
      boolean pIsGui3d,
      RandomSource pRandom
   ) {
      int $$8 = getRenderedAmount(pItem.getCount());
      float $$9 = pModel.getTransforms().ground.scale.x();
      float $$10 = pModel.getTransforms().ground.scale.y();
      float $$11 = pModel.getTransforms().ground.scale.z();
      if (!pIsGui3d) {
         float $$12 = -0.0F * (float)($$8 - 1) * 0.5F * $$9;
         float $$13 = -0.0F * (float)($$8 - 1) * 0.5F * $$10;
         float $$14 = -0.09375F * (float)($$8 - 1) * 0.5F * $$11;
         pPoseStack.translate($$12, $$13, $$14);
      }

      for (int $$15 = 0; $$15 < $$8; $$15++) {
         pPoseStack.pushPose();
         if ($$15 > 0) {
            if (pIsGui3d) {
               float $$16 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float $$17 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float $$18 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F;
               pPoseStack.translate($$16, $$17, $$18);
            } else {
               float $$19 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float $$20 = (pRandom.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               pPoseStack.translate($$19, $$20, 0.0F);
            }
         }

         pItemRenderer.render(pItem, ItemDisplayContext.GROUND, false, pPoseStack, pBuffer, pPackedLight, OverlayTexture.NO_OVERLAY, pModel);
         pPoseStack.popPose();
         if (!pIsGui3d) {
            pPoseStack.translate(0.0F * $$9, 0.0F * $$10, 0.09375F * $$11);
         }
      }
   }
}
