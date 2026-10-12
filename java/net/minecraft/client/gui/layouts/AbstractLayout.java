package net.minecraft.client.gui.layouts;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractLayout implements Layout {
   private int x;
   private int y;
   protected int width;
   protected int height;

   public AbstractLayout(int pX, int pY, int pWidth, int pHeight) {
      this.x = pX;
      this.y = pY;
      this.width = pWidth;
      this.height = pHeight;
   }

   @Override
   public void setX(int pX) {
      this.visitChildren(p_265043_ -> {
         int $$2 = p_265043_.getX() + (pX - this.getX());
         p_265043_.setX($$2);
      });
      this.x = pX;
   }

   @Override
   public void setY(int pY) {
      this.visitChildren(p_265586_ -> {
         int $$2 = p_265586_.getY() + (pY - this.getY());
         p_265586_.setY($$2);
      });
      this.y = pY;
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract static class AbstractChildWrapper {
      public final LayoutElement child;
      public final LayoutSettings.LayoutSettingsImpl layoutSettings;

      protected AbstractChildWrapper(LayoutElement pChild, LayoutSettings pLayoutSettings) {
         this.child = pChild;
         this.layoutSettings = pLayoutSettings.getExposed();
      }

      public int getHeight() {
         return this.child.getHeight() + this.layoutSettings.paddingTop + this.layoutSettings.paddingBottom;
      }

      public int getWidth() {
         return this.child.getWidth() + this.layoutSettings.paddingLeft + this.layoutSettings.paddingRight;
      }

      public void setX(int pX, int pWidth) {
         float $$2 = (float)this.layoutSettings.paddingLeft;
         float $$3 = (float)(pWidth - this.child.getWidth() - this.layoutSettings.paddingRight);
         int $$4 = (int)Mth.lerp(this.layoutSettings.xAlignment, $$2, $$3);
         this.child.setX($$4 + pX);
      }

      public void setY(int pY, int pHeight) {
         float $$2 = (float)this.layoutSettings.paddingTop;
         float $$3 = (float)(pHeight - this.child.getHeight() - this.layoutSettings.paddingBottom);
         int $$4 = Math.round(Mth.lerp(this.layoutSettings.yAlignment, $$2, $$3));
         this.child.setY($$4 + pY);
      }
   }
}
