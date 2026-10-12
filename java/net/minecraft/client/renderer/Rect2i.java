package net.minecraft.client.renderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Rect2i {
   private int xPos;
   private int yPos;
   private int width;
   private int height;

   public Rect2i(int pXPos, int pYPos, int pWidth, int pHeight) {
      this.xPos = pXPos;
      this.yPos = pYPos;
      this.width = pWidth;
      this.height = pHeight;
   }

   public Rect2i intersect(Rect2i pOther) {
      int $$1 = this.xPos;
      int $$2 = this.yPos;
      int $$3 = this.xPos + this.width;
      int $$4 = this.yPos + this.height;
      int $$5 = pOther.getX();
      int $$6 = pOther.getY();
      int $$7 = $$5 + pOther.getWidth();
      int $$8 = $$6 + pOther.getHeight();
      this.xPos = Math.max($$1, $$5);
      this.yPos = Math.max($$2, $$6);
      this.width = Math.max(0, Math.min($$3, $$7) - this.xPos);
      this.height = Math.max(0, Math.min($$4, $$8) - this.yPos);
      return this;
   }

   public int getX() {
      return this.xPos;
   }

   public int getY() {
      return this.yPos;
   }

   public void setX(int pXPos) {
      this.xPos = pXPos;
   }

   public void setY(int pYPos) {
      this.yPos = pYPos;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setWidth(int pWidth) {
      this.width = pWidth;
   }

   public void setHeight(int pHeight) {
      this.height = pHeight;
   }

   public void setPosition(int pXPos, int pYPos) {
      this.xPos = pXPos;
      this.yPos = pYPos;
   }

   public boolean contains(int pX, int pY) {
      return pX >= this.xPos && pX <= this.xPos + this.width && pY >= this.yPos && pY <= this.yPos + this.height;
   }
}
