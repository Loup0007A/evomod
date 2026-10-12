package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ModelPart {
   public static final float DEFAULT_SCALE = 1.0F;
   public float x;
   public float y;
   public float z;
   public float xRot;
   public float yRot;
   public float zRot;
   public float xScale = 1.0F;
   public float yScale = 1.0F;
   public float zScale = 1.0F;
   public boolean visible = true;
   public boolean skipDraw;
   private final List<ModelPart.Cube> cubes;
   private final Map<String, ModelPart> children;
   private PartPose initialPose = PartPose.ZERO;

   public ModelPart(List<ModelPart.Cube> pCubes, Map<String, ModelPart> pChildren) {
      this.cubes = pCubes;
      this.children = pChildren;
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public PartPose getInitialPose() {
      return this.initialPose;
   }

   public void setInitialPose(PartPose pInitialPose) {
      this.initialPose = pInitialPose;
   }

   public void resetPose() {
      this.loadPose(this.initialPose);
   }

   public void loadPose(PartPose pPartPose) {
      this.x = pPartPose.x;
      this.y = pPartPose.y;
      this.z = pPartPose.z;
      this.xRot = pPartPose.xRot;
      this.yRot = pPartPose.yRot;
      this.zRot = pPartPose.zRot;
      this.xScale = 1.0F;
      this.yScale = 1.0F;
      this.zScale = 1.0F;
   }

   public void copyFrom(ModelPart pModelPart) {
      this.xScale = pModelPart.xScale;
      this.yScale = pModelPart.yScale;
      this.zScale = pModelPart.zScale;
      this.xRot = pModelPart.xRot;
      this.yRot = pModelPart.yRot;
      this.zRot = pModelPart.zRot;
      this.x = pModelPart.x;
      this.y = pModelPart.y;
      this.z = pModelPart.z;
   }

   public boolean hasChild(String pName) {
      return this.children.containsKey(pName);
   }

   public ModelPart getChild(String pName) {
      ModelPart $$1 = this.children.get(pName);
      if ($$1 == null) {
         throw new NoSuchElementException("Can't find part " + pName);
      } else {
         return $$1;
      }
   }

   public void setPos(float pX, float pY, float pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   public void setRotation(float pXRot, float pYRot, float pZRot) {
      this.xRot = pXRot;
      this.yRot = pYRot;
      this.zRot = pZRot;
   }

   public void render(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay) {
      this.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, -1);
   }

   public void render(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
      if (this.visible) {
         if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            pPoseStack.pushPose();
            this.translateAndRotate(pPoseStack);
            if (!this.skipDraw) {
               this.compile(pPoseStack.last(), pBuffer, pPackedLight, pPackedOverlay, pColor);
            }

            for (ModelPart $$5 : this.children.values()) {
               $$5.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor);
            }

            pPoseStack.popPose();
         }
      }
   }

   public void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor) {
      this.visit(pPoseStack, pVisitor, "");
   }

   private void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor, String pPath) {
      if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
         pPoseStack.pushPose();
         this.translateAndRotate(pPoseStack);
         PoseStack.Pose $$3 = pPoseStack.last();

         for (int $$4 = 0; $$4 < this.cubes.size(); $$4++) {
            pVisitor.visit($$3, pPath, $$4, this.cubes.get($$4));
         }

         String $$5 = pPath + "/";
         this.children.forEach((p_171320_, p_171321_) -> p_171321_.visit(pPoseStack, pVisitor, $$5 + p_171320_));
         pPoseStack.popPose();
      }
   }

   public void translateAndRotate(PoseStack pPoseStack) {
      pPoseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         pPoseStack.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         pPoseStack.scale(this.xScale, this.yScale, this.zScale);
      }
   }

   private void compile(PoseStack.Pose pPose, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
      for (ModelPart.Cube $$5 : this.cubes) {
         $$5.compile(pPose, pBuffer, pPackedLight, pPackedOverlay, pColor);
      }
   }

   public ModelPart.Cube getRandomCube(RandomSource pRandom) {
      return this.cubes.get(pRandom.nextInt(this.cubes.size()));
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public void offsetPos(Vector3f pOffset) {
      this.x = this.x + pOffset.x();
      this.y = this.y + pOffset.y();
      this.z = this.z + pOffset.z();
   }

   public void offsetRotation(Vector3f pOffset) {
      this.xRot = this.xRot + pOffset.x();
      this.yRot = this.yRot + pOffset.y();
      this.zRot = this.zRot + pOffset.z();
   }

   public void offsetScale(Vector3f pOffset) {
      this.xScale = this.xScale + pOffset.x();
      this.yScale = this.yScale + pOffset.y();
      this.zScale = this.zScale + pOffset.z();
   }

   public Stream<ModelPart> getAllParts() {
      return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
   }

   @OnlyIn(Dist.CLIENT)
   public static class Cube {
      private final ModelPart.Polygon[] polygons;
      public final float minX;
      public final float minY;
      public final float minZ;
      public final float maxX;
      public final float maxY;
      public final float maxZ;

      public Cube(
         int pTexCoordU,
         int pTexCoordV,
         float pOriginX,
         float pOriginY,
         float pOriginZ,
         float pDimensionX,
         float pDimensionY,
         float pDimensionZ,
         float pGtowX,
         float pGrowY,
         float pGrowZ,
         boolean pMirror,
         float pTexScaleU,
         float pTexScaleV,
         Set<Direction> pVisibleFaces
      ) {
         this.minX = pOriginX;
         this.minY = pOriginY;
         this.minZ = pOriginZ;
         this.maxX = pOriginX + pDimensionX;
         this.maxY = pOriginY + pDimensionY;
         this.maxZ = pOriginZ + pDimensionZ;
         this.polygons = new ModelPart.Polygon[pVisibleFaces.size()];
         float $$15 = pOriginX + pDimensionX;
         float $$16 = pOriginY + pDimensionY;
         float $$17 = pOriginZ + pDimensionZ;
         pOriginX -= pGtowX;
         pOriginY -= pGrowY;
         pOriginZ -= pGrowZ;
         $$15 += pGtowX;
         $$16 += pGrowY;
         $$17 += pGrowZ;
         if (pMirror) {
            float $$18 = $$15;
            $$15 = pOriginX;
            pOriginX = $$18;
         }

         ModelPart.Vertex $$19 = new ModelPart.Vertex(pOriginX, pOriginY, pOriginZ, 0.0F, 0.0F);
         ModelPart.Vertex $$20 = new ModelPart.Vertex($$15, pOriginY, pOriginZ, 0.0F, 8.0F);
         ModelPart.Vertex $$21 = new ModelPart.Vertex($$15, $$16, pOriginZ, 8.0F, 8.0F);
         ModelPart.Vertex $$22 = new ModelPart.Vertex(pOriginX, $$16, pOriginZ, 8.0F, 0.0F);
         ModelPart.Vertex $$23 = new ModelPart.Vertex(pOriginX, pOriginY, $$17, 0.0F, 0.0F);
         ModelPart.Vertex $$24 = new ModelPart.Vertex($$15, pOriginY, $$17, 0.0F, 8.0F);
         ModelPart.Vertex $$25 = new ModelPart.Vertex($$15, $$16, $$17, 8.0F, 8.0F);
         ModelPart.Vertex $$26 = new ModelPart.Vertex(pOriginX, $$16, $$17, 8.0F, 0.0F);
         float $$27 = (float)pTexCoordU;
         float $$28 = (float)pTexCoordU + pDimensionZ;
         float $$29 = (float)pTexCoordU + pDimensionZ + pDimensionX;
         float $$30 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionX;
         float $$31 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionZ;
         float $$32 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionZ + pDimensionX;
         float $$33 = (float)pTexCoordV;
         float $$34 = (float)pTexCoordV + pDimensionZ;
         float $$35 = (float)pTexCoordV + pDimensionZ + pDimensionY;
         int $$36 = 0;
         if (pVisibleFaces.contains(Direction.DOWN)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$24, $$23, $$19, $$20}, $$28, $$33, $$29, $$34, pTexScaleU, pTexScaleV, pMirror, Direction.DOWN
            );
         }

         if (pVisibleFaces.contains(Direction.UP)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$21, $$22, $$26, $$25}, $$29, $$34, $$30, $$33, pTexScaleU, pTexScaleV, pMirror, Direction.UP
            );
         }

         if (pVisibleFaces.contains(Direction.WEST)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$19, $$23, $$26, $$22}, $$27, $$34, $$28, $$35, pTexScaleU, pTexScaleV, pMirror, Direction.WEST
            );
         }

         if (pVisibleFaces.contains(Direction.NORTH)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$20, $$19, $$22, $$21}, $$28, $$34, $$29, $$35, pTexScaleU, pTexScaleV, pMirror, Direction.NORTH
            );
         }

         if (pVisibleFaces.contains(Direction.EAST)) {
            this.polygons[$$36++] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$24, $$20, $$21, $$25}, $$29, $$34, $$31, $$35, pTexScaleU, pTexScaleV, pMirror, Direction.EAST
            );
         }

         if (pVisibleFaces.contains(Direction.SOUTH)) {
            this.polygons[$$36] = new ModelPart.Polygon(
               new ModelPart.Vertex[]{$$23, $$24, $$25, $$26}, $$31, $$34, $$32, $$35, pTexScaleU, pTexScaleV, pMirror, Direction.SOUTH
            );
         }
      }

      public void compile(PoseStack.Pose pPose, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
         Matrix4f $$5 = pPose.pose();
         Vector3f $$6 = new Vector3f();

         for (ModelPart.Polygon $$7 : this.polygons) {
            Vector3f $$8 = pPose.transformNormal($$7.normal, $$6);
            float $$9 = $$8.x();
            float $$10 = $$8.y();
            float $$11 = $$8.z();

            for (ModelPart.Vertex $$12 : $$7.vertices) {
               float $$13 = $$12.pos.x() / 16.0F;
               float $$14 = $$12.pos.y() / 16.0F;
               float $$15 = $$12.pos.z() / 16.0F;
               Vector3f $$16 = $$5.transformPosition($$13, $$14, $$15, $$6);
               pBuffer.addVertex($$16.x(), $$16.y(), $$16.z(), pColor, $$12.u, $$12.v, pPackedOverlay, pPackedLight, $$9, $$10, $$11);
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Polygon {
      public final ModelPart.Vertex[] vertices;
      public final Vector3f normal;

      public Polygon(
         ModelPart.Vertex[] pVertices,
         float pU1,
         float pV1,
         float pU2,
         float pV2,
         float pTextureWidth,
         float pTextureHeight,
         boolean pMirror,
         Direction pDirection
      ) {
         this.vertices = pVertices;
         float $$9 = 0.0F / pTextureWidth;
         float $$10 = 0.0F / pTextureHeight;
         pVertices[0] = pVertices[0].remap(pU2 / pTextureWidth - $$9, pV1 / pTextureHeight + $$10);
         pVertices[1] = pVertices[1].remap(pU1 / pTextureWidth + $$9, pV1 / pTextureHeight + $$10);
         pVertices[2] = pVertices[2].remap(pU1 / pTextureWidth + $$9, pV2 / pTextureHeight - $$10);
         pVertices[3] = pVertices[3].remap(pU2 / pTextureWidth - $$9, pV2 / pTextureHeight - $$10);
         if (pMirror) {
            int $$11 = pVertices.length;

            for (int $$12 = 0; $$12 < $$11 / 2; $$12++) {
               ModelPart.Vertex $$13 = pVertices[$$12];
               pVertices[$$12] = pVertices[$$11 - 1 - $$12];
               pVertices[$$11 - 1 - $$12] = $$13;
            }
         }

         this.normal = pDirection.step();
         if (pMirror) {
            this.normal.mul(-1.0F, 1.0F, 1.0F);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Vertex {
      public final Vector3f pos;
      public final float u;
      public final float v;

      public Vertex(float pX, float pY, float pZ, float pU, float pV) {
         this(new Vector3f(pX, pY, pZ), pU, pV);
      }

      public ModelPart.Vertex remap(float pU, float pV) {
         return new ModelPart.Vertex(this.pos, pU, pV);
      }

      public Vertex(Vector3f pPos, float pU, float pV) {
         this.pos = pPos;
         this.u = pU;
         this.v = pV;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Visitor {
      void visit(PoseStack.Pose pPose, String pPath, int pIndex, ModelPart.Cube pCube);
   }
}
