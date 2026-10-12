package com.mojang.math;

import org.apache.commons.lang3.tuple.Triple;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MatrixUtil {
   private static final float G = 3.0F + 2.0F * Math.sqrt(2.0F);
   private static final GivensParameters PI_4 = GivensParameters.fromPositiveAngle((float) (java.lang.Math.PI / 4));

   private MatrixUtil() {
   }

   public static Matrix4f mulComponentWise(Matrix4f pMatrix, float pScalar) {
      return pMatrix.set(
         pMatrix.m00() * pScalar,
         pMatrix.m01() * pScalar,
         pMatrix.m02() * pScalar,
         pMatrix.m03() * pScalar,
         pMatrix.m10() * pScalar,
         pMatrix.m11() * pScalar,
         pMatrix.m12() * pScalar,
         pMatrix.m13() * pScalar,
         pMatrix.m20() * pScalar,
         pMatrix.m21() * pScalar,
         pMatrix.m22() * pScalar,
         pMatrix.m23() * pScalar,
         pMatrix.m30() * pScalar,
         pMatrix.m31() * pScalar,
         pMatrix.m32() * pScalar,
         pMatrix.m33() * pScalar
      );
   }

   private static GivensParameters approxGivensQuat(float p_276275_, float p_276276_, float p_276282_) {
      float $$3 = 2.0F * (p_276275_ - p_276282_);
      return G * p_276276_ * p_276276_ < $$3 * $$3 ? GivensParameters.fromUnnormalized(p_276276_, $$3) : PI_4;
   }

   private static GivensParameters qrGivensQuat(float pInput1, float pInput2) {
      float $$2 = (float)java.lang.Math.hypot((double)pInput1, (double)pInput2);
      float $$3 = $$2 > 1.0E-6F ? pInput2 : 0.0F;
      float $$4 = Math.abs(pInput1) + Math.max($$2, 1.0E-6F);
      if (pInput1 < 0.0F) {
         float $$5 = $$3;
         $$3 = $$4;
         $$4 = $$5;
      }

      return GivensParameters.fromUnnormalized($$3, $$4);
   }

   private static void similarityTransform(Matrix3f pInput, Matrix3f pTempStorage) {
      pInput.mul(pTempStorage);
      pTempStorage.transpose();
      pTempStorage.mul(pInput);
      pInput.set(pTempStorage);
   }

   private static void stepJacobi(Matrix3f pInput, Matrix3f pTempStorage, Quaternionf pResultEigenvector, Quaternionf pResultEigenvalue) {
      if (pInput.m01 * pInput.m01 + pInput.m10 * pInput.m10 > 1.0E-6F) {
         GivensParameters $$4 = approxGivensQuat(pInput.m00, 0.5F * (pInput.m01 + pInput.m10), pInput.m11);
         Quaternionf $$5 = $$4.aroundZ(pResultEigenvector);
         pResultEigenvalue.mul($$5);
         $$4.aroundZ(pTempStorage);
         similarityTransform(pInput, pTempStorage);
      }

      if (pInput.m02 * pInput.m02 + pInput.m20 * pInput.m20 > 1.0E-6F) {
         GivensParameters $$6 = approxGivensQuat(pInput.m00, 0.5F * (pInput.m02 + pInput.m20), pInput.m22).inverse();
         Quaternionf $$7 = $$6.aroundY(pResultEigenvector);
         pResultEigenvalue.mul($$7);
         $$6.aroundY(pTempStorage);
         similarityTransform(pInput, pTempStorage);
      }

      if (pInput.m12 * pInput.m12 + pInput.m21 * pInput.m21 > 1.0E-6F) {
         GivensParameters $$8 = approxGivensQuat(pInput.m11, 0.5F * (pInput.m12 + pInput.m21), pInput.m22);
         Quaternionf $$9 = $$8.aroundX(pResultEigenvector);
         pResultEigenvalue.mul($$9);
         $$8.aroundX(pTempStorage);
         similarityTransform(pInput, pTempStorage);
      }
   }

   public static Quaternionf eigenvalueJacobi(Matrix3f pInput, int pIterations) {
      Quaternionf $$2 = new Quaternionf();
      Matrix3f $$3 = new Matrix3f();
      Quaternionf $$4 = new Quaternionf();

      for (int $$5 = 0; $$5 < pIterations; $$5++) {
         stepJacobi(pInput, $$3, $$4, $$2);
      }

      $$2.normalize();
      return $$2;
   }

   public static Triple<Quaternionf, Vector3f, Quaternionf> svdDecompose(Matrix3f pMatrix) {
      Matrix3f $$1 = new Matrix3f(pMatrix);
      $$1.transpose();
      $$1.mul(pMatrix);
      Quaternionf $$2 = eigenvalueJacobi($$1, 5);
      float $$3 = $$1.m00;
      float $$4 = $$1.m11;
      boolean $$5 = (double)$$3 < 1.0E-6;
      boolean $$6 = (double)$$4 < 1.0E-6;
      Matrix3f $$8 = pMatrix.rotate($$2);
      Quaternionf $$9 = new Quaternionf();
      Quaternionf $$10 = new Quaternionf();
      GivensParameters $$11;
      if ($$5) {
         $$11 = qrGivensQuat($$8.m11, -$$8.m10);
      } else {
         $$11 = qrGivensQuat($$8.m00, $$8.m01);
      }

      Quaternionf $$13 = $$11.aroundZ($$10);
      Matrix3f $$14 = $$11.aroundZ($$1);
      $$9.mul($$13);
      $$14.transpose().mul($$8);
      if ($$5) {
         $$11 = qrGivensQuat($$14.m22, -$$14.m20);
      } else {
         $$11 = qrGivensQuat($$14.m00, $$14.m02);
      }

      $$11 = $$11.inverse();
      Quaternionf $$15 = $$11.aroundY($$10);
      Matrix3f $$16 = $$11.aroundY($$8);
      $$9.mul($$15);
      $$16.transpose().mul($$14);
      if ($$6) {
         $$11 = qrGivensQuat($$16.m22, -$$16.m21);
      } else {
         $$11 = qrGivensQuat($$16.m11, $$16.m12);
      }

      Quaternionf $$17 = $$11.aroundX($$10);
      Matrix3f $$18 = $$11.aroundX($$14);
      $$9.mul($$17);
      $$18.transpose().mul($$16);
      Vector3f $$19 = new Vector3f($$18.m00, $$18.m11, $$18.m22);
      return Triple.of($$9, $$19, $$2.conjugate());
   }

   public static boolean isPureTranslation(Matrix4f pMatrix) {
      return (pMatrix.properties() & 8) != 0;
   }

   public static boolean isOrthonormal(Matrix4f pMatrix) {
      return (pMatrix.properties() & 16) != 0;
   }
}
