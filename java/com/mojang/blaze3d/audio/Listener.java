package com.mojang.blaze3d.audio;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;

@OnlyIn(Dist.CLIENT)
public class Listener {
   private float gain = 1.0F;
   private ListenerTransform transform = ListenerTransform.INITIAL;

   public void setTransform(ListenerTransform pTransform) {
      this.transform = pTransform;
      Vec3 $$1 = pTransform.position();
      Vec3 $$2 = pTransform.forward();
      Vec3 $$3 = pTransform.up();
      AL10.alListener3f(4100, (float)$$1.x, (float)$$1.y, (float)$$1.z);
      AL10.alListenerfv(4111, new float[]{(float)$$2.x, (float)$$2.y, (float)$$2.z, (float)$$3.x(), (float)$$3.y(), (float)$$3.z()});
   }

   public void setGain(float pGain) {
      AL10.alListenerf(4106, pGain);
      this.gain = pGain;
   }

   public float getGain() {
      return this.gain;
   }

   public void reset() {
      this.setTransform(ListenerTransform.INITIAL);
   }

   public ListenerTransform getTransform() {
      return this.transform;
   }
}
