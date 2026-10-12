package com.mojang.math;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public record GivensParameters(float sinHalf, float cosHalf) {
   public static GivensParameters fromUnnormalized(float pSinHalf, float pCosHalf) {
      float $$2 = Math.invsqrt(pSinHalf * pSinHalf + pCosHalf * pCosHalf);
      return new GivensParameters($$2 * pSinHalf, $$2 * pCosHalf);
   }

   public static GivensParameters fromPositiveAngle(float pAngle) {
      float $$1 = Math.sin(pAngle / 2.0F);
      float $$2 = Math.cosFromSin($$1, pAngle / 2.0F);
      return new GivensParameters($$1, $$2);
   }

   public GivensParameters inverse() {
      return new GivensParameters(-this.sinHalf, this.cosHalf);
   }

   public Quaternionf aroundX(Quaternionf pQuaternion) {
      return pQuaternion.set(this.sinHalf, 0.0F, 0.0F, this.cosHalf);
   }

   public Quaternionf aroundY(Quaternionf pQuaternion) {
      return pQuaternion.set(0.0F, this.sinHalf, 0.0F, this.cosHalf);
   }

   public Quaternionf aroundZ(Quaternionf pQuaternion) {
      return pQuaternion.set(0.0F, 0.0F, this.sinHalf, this.cosHalf);
   }

   public float cos() {
      return this.cosHalf * this.cosHalf - this.sinHalf * this.sinHalf;
   }

   public float sin() {
      return 2.0F * this.sinHalf * this.cosHalf;
   }

   public Matrix3f aroundX(Matrix3f pMatrix) {
      pMatrix.m01 = 0.0F;
      pMatrix.m02 = 0.0F;
      pMatrix.m10 = 0.0F;
      pMatrix.m20 = 0.0F;
      float $$1 = this.cos();
      float $$2 = this.sin();
      pMatrix.m11 = $$1;
      pMatrix.m22 = $$1;
      pMatrix.m12 = $$2;
      pMatrix.m21 = -$$2;
      pMatrix.m00 = 1.0F;
      return pMatrix;
   }

   public Matrix3f aroundY(Matrix3f pMatrix) {
      pMatrix.m01 = 0.0F;
      pMatrix.m10 = 0.0F;
      pMatrix.m12 = 0.0F;
      pMatrix.m21 = 0.0F;
      float $$1 = this.cos();
      float $$2 = this.sin();
      pMatrix.m00 = $$1;
      pMatrix.m22 = $$1;
      pMatrix.m02 = -$$2;
      pMatrix.m20 = $$2;
      pMatrix.m11 = 1.0F;
      return pMatrix;
   }

   public Matrix3f aroundZ(Matrix3f pMatrix) {
      pMatrix.m02 = 0.0F;
      pMatrix.m12 = 0.0F;
      pMatrix.m20 = 0.0F;
      pMatrix.m21 = 0.0F;
      float $$1 = this.cos();
      float $$2 = this.sin();
      pMatrix.m00 = $$1;
      pMatrix.m11 = $$1;
      pMatrix.m01 = $$2;
      pMatrix.m10 = -$$2;
      pMatrix.m22 = 1.0F;
      return pMatrix;
   }
}
