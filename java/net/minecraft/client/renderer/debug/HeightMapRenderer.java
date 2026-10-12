package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class HeightMapRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CHUNK_DIST = 2;
   private static final float BOX_HEIGHT = 0.09375F;

   public HeightMapRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   @Override
   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      LevelAccessor $$5 = this.minecraft.level;
      VertexConsumer $$6 = pBufferSource.getBuffer(RenderType.debugFilledBox());
      BlockPos $$7 = BlockPos.containing(pCamX, 0.0, pCamZ);

      for (int $$8 = -2; $$8 <= 2; $$8++) {
         for (int $$9 = -2; $$9 <= 2; $$9++) {
            ChunkAccess $$10 = $$5.getChunk($$7.offset($$8 * 16, 0, $$9 * 16));

            for (Entry<Heightmap.Types, Heightmap> $$11 : $$10.getHeightmaps()) {
               Heightmap.Types $$12 = $$11.getKey();
               ChunkPos $$13 = $$10.getPos();
               Vector3f $$14 = this.getColor($$12);

               for (int $$15 = 0; $$15 < 16; $$15++) {
                  for (int $$16 = 0; $$16 < 16; $$16++) {
                     int $$17 = SectionPos.sectionToBlockCoord($$13.x, $$15);
                     int $$18 = SectionPos.sectionToBlockCoord($$13.z, $$16);
                     float $$19 = (float)((double)((float)$$5.getHeight($$12, $$17, $$18) + (float)$$12.ordinal() * 0.09375F) - pCamY);
                     LevelRenderer.addChainedFilledBoxVertices(
                        pPoseStack,
                        $$6,
                        (double)((float)$$17 + 0.25F) - pCamX,
                        (double)$$19,
                        (double)((float)$$18 + 0.25F) - pCamZ,
                        (double)((float)$$17 + 0.75F) - pCamX,
                        (double)($$19 + 0.09375F),
                        (double)((float)$$18 + 0.75F) - pCamZ,
                        $$14.x(),
                        $$14.y(),
                        $$14.z(),
                        1.0F
                     );
                  }
               }
            }
         }
      }
   }

   private Vector3f getColor(Heightmap.Types pTypes) {
      return switch (pTypes) {
         case WORLD_SURFACE_WG -> new Vector3f(1.0F, 1.0F, 0.0F);
         case OCEAN_FLOOR_WG -> new Vector3f(1.0F, 0.0F, 1.0F);
         case WORLD_SURFACE -> new Vector3f(0.0F, 0.7F, 0.0F);
         case OCEAN_FLOOR -> new Vector3f(0.0F, 0.0F, 0.5F);
         case MOTION_BLOCKING -> new Vector3f(0.0F, 0.3F, 0.3F);
         case MOTION_BLOCKING_NO_LEAVES -> new Vector3f(0.0F, 0.5F, 0.5F);
      };
   }
}
