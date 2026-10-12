package net.minecraft.client.resources.metadata.animation;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnimationMetadataSection {
   public static final AnimationMetadataSectionSerializer SERIALIZER = new AnimationMetadataSectionSerializer();
   public static final String SECTION_NAME = "animation";
   public static final int DEFAULT_FRAME_TIME = 1;
   public static final int UNKNOWN_SIZE = -1;
   public static final AnimationMetadataSection EMPTY = new AnimationMetadataSection(Lists.newArrayList(), -1, -1, 1, false) {
      @Override
      public FrameSize calculateFrameSize(int p_251622_, int p_252064_) {
         return new FrameSize(p_251622_, p_252064_);
      }
   };
   private final List<AnimationFrame> frames;
   private final int frameWidth;
   private final int frameHeight;
   private final int defaultFrameTime;
   private final boolean interpolatedFrames;

   public AnimationMetadataSection(List<AnimationFrame> pFrames, int pFrameWidth, int pFrameHeight, int pDefaultFrameTime, boolean pInterpolatedFrames) {
      this.frames = pFrames;
      this.frameWidth = pFrameWidth;
      this.frameHeight = pFrameHeight;
      this.defaultFrameTime = pDefaultFrameTime;
      this.interpolatedFrames = pInterpolatedFrames;
   }

   public FrameSize calculateFrameSize(int pWidth, int pHeight) {
      if (this.frameWidth != -1) {
         return this.frameHeight != -1 ? new FrameSize(this.frameWidth, this.frameHeight) : new FrameSize(this.frameWidth, pHeight);
      } else if (this.frameHeight != -1) {
         return new FrameSize(pWidth, this.frameHeight);
      } else {
         int $$2 = Math.min(pWidth, pHeight);
         return new FrameSize($$2, $$2);
      }
   }

   public int getDefaultFrameTime() {
      return this.defaultFrameTime;
   }

   public boolean isInterpolatedFrames() {
      return this.interpolatedFrames;
   }

   public void forEachFrame(AnimationMetadataSection.FrameOutput pOutput) {
      for (AnimationFrame $$1 : this.frames) {
         pOutput.accept($$1.getIndex(), $$1.getTime(this.defaultFrameTime));
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface FrameOutput {
      void accept(int pIndex, int pTime);
   }
}
