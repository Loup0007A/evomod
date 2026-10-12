package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LightningBoltRenderer extends EntityRenderer<LightningBolt> {
   public LightningBoltRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
   }

   public void render(LightningBolt pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      float[] $$6 = new float[8];
      float[] $$7 = new float[8];
      float $$8 = 0.0F;
      float $$9 = 0.0F;
      RandomSource $$10 = RandomSource.create(pEntity.seed);

      for (int $$11 = 7; $$11 >= 0; $$11--) {
         $$6[$$11] = $$8;
         $$7[$$11] = $$9;
         $$8 += (float)($$10.nextInt(11) - 5);
         $$9 += (float)($$10.nextInt(11) - 5);
      }

      VertexConsumer $$12 = pBuffer.getBuffer(RenderType.lightning());
      Matrix4f $$13 = pPoseStack.last().pose();

      for (int $$14 = 0; $$14 < 4; $$14++) {
         RandomSource $$15 = RandomSource.create(pEntity.seed);

         for (int $$16 = 0; $$16 < 3; $$16++) {
            int $$17 = 7;
            int $$18 = 0;
            if ($$16 > 0) {
               $$17 = 7 - $$16;
            }

            if ($$16 > 0) {
               $$18 = $$17 - 2;
            }

            float $$19 = $$6[$$17] - $$8;
            float $$20 = $$7[$$17] - $$9;

            for (int $$21 = $$17; $$21 >= $$18; $$21--) {
               float $$22 = $$19;
               float $$23 = $$20;
               if ($$16 == 0) {
                  $$19 += (float)($$15.nextInt(11) - 5);
                  $$20 += (float)($$15.nextInt(11) - 5);
               } else {
                  $$19 += (float)($$15.nextInt(31) - 15);
                  $$20 += (float)($$15.nextInt(31) - 15);
               }

               float $$24 = 0.5F;
               float $$25 = 0.45F;
               float $$26 = 0.45F;
               float $$27 = 0.5F;
               float $$28 = 0.1F + (float)$$14 * 0.2F;
               if ($$16 == 0) {
                  $$28 *= (float)$$21 * 0.1F + 1.0F;
               }

               float $$29 = 0.1F + (float)$$14 * 0.2F;
               if ($$16 == 0) {
                  $$29 *= ((float)$$21 - 1.0F) * 0.1F + 1.0F;
               }

               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, false, false, true, false);
               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, true, false, true, true);
               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, true, true, false, true);
               quad($$13, $$12, $$19, $$20, $$21, $$22, $$23, 0.45F, 0.45F, 0.5F, $$28, $$29, false, true, false, false);
            }
         }
      }
   }

   private static void quad(
      Matrix4f pMatrix,
      VertexConsumer pConsumer,
      float pX1,
      float pZ1,
      int pIndex,
      float pX2,
      float pZ2,
      float pRed,
      float pGreen,
      float pBlue,
      float p_115283_,
      float p_115284_,
      boolean p_115285_,
      boolean p_115286_,
      boolean p_115287_,
      boolean p_115288_
   ) {
      pConsumer.addVertex(pMatrix, pX1 + (p_115285_ ? p_115284_ : -p_115284_), (float)(pIndex * 16), pZ1 + (p_115286_ ? p_115284_ : -p_115284_))
         .setColor(pRed, pGreen, pBlue, 0.3F);
      pConsumer.addVertex(pMatrix, pX2 + (p_115285_ ? p_115283_ : -p_115283_), (float)((pIndex + 1) * 16), pZ2 + (p_115286_ ? p_115283_ : -p_115283_))
         .setColor(pRed, pGreen, pBlue, 0.3F);
      pConsumer.addVertex(pMatrix, pX2 + (p_115287_ ? p_115283_ : -p_115283_), (float)((pIndex + 1) * 16), pZ2 + (p_115288_ ? p_115283_ : -p_115283_))
         .setColor(pRed, pGreen, pBlue, 0.3F);
      pConsumer.addVertex(pMatrix, pX1 + (p_115287_ ? p_115284_ : -p_115284_), (float)(pIndex * 16), pZ1 + (p_115288_ ? p_115284_ : -p_115284_))
         .setColor(pRed, pGreen, pBlue, 0.3F);
   }

   public ResourceLocation getTextureLocation(LightningBolt pEntity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}
