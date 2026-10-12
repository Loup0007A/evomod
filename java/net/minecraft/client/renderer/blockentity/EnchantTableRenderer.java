package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnchantTableRenderer implements BlockEntityRenderer<EnchantingTableBlockEntity> {
   public static final Material BOOK_LOCATION = new Material(
      TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("entity/enchanting_table_book")
   );
   private final BookModel bookModel;

   public EnchantTableRenderer(BlockEntityRendererProvider.Context pContext) {
      this.bookModel = new BookModel(pContext.bakeLayer(ModelLayers.BOOK));
   }

   public void render(
      EnchantingTableBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay
   ) {
      pPoseStack.pushPose();
      pPoseStack.translate(0.5F, 0.75F, 0.5F);
      float $$6 = (float)pBlockEntity.time + pPartialTick;
      pPoseStack.translate(0.0F, 0.1F + Mth.sin($$6 * 0.1F) * 0.01F, 0.0F);
      float $$7 = pBlockEntity.rot - pBlockEntity.oRot;

      while ($$7 >= (float) Math.PI) {
         $$7 -= (float) (Math.PI * 2);
      }

      while ($$7 < (float) -Math.PI) {
         $$7 += (float) (Math.PI * 2);
      }

      float $$8 = pBlockEntity.oRot + $$7 * pPartialTick;
      pPoseStack.mulPose(Axis.YP.rotation(-$$8));
      pPoseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
      float $$9 = Mth.lerp(pPartialTick, pBlockEntity.oFlip, pBlockEntity.flip);
      float $$10 = Mth.frac($$9 + 0.25F) * 1.6F - 0.3F;
      float $$11 = Mth.frac($$9 + 0.75F) * 1.6F - 0.3F;
      float $$12 = Mth.lerp(pPartialTick, pBlockEntity.oOpen, pBlockEntity.open);
      this.bookModel.setupAnim($$6, Mth.clamp($$10, 0.0F, 1.0F), Mth.clamp($$11, 0.0F, 1.0F), $$12);
      VertexConsumer $$13 = BOOK_LOCATION.buffer(pBufferSource, RenderType::entitySolid);
      this.bookModel.render(pPoseStack, $$13, pPackedLight, pPackedOverlay, -1);
      pPoseStack.popPose();
   }
}
