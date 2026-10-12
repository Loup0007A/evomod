package net.minecraft.client.gui.navigation;

import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ScreenRectangle(ScreenPosition position, int width, int height) {
   private static final ScreenRectangle EMPTY = new ScreenRectangle(0, 0, 0, 0);

   public ScreenRectangle(int pX, int pY, int pWidth, int pHeight) {
      this(new ScreenPosition(pX, pY), pWidth, pHeight);
   }

   public static ScreenRectangle empty() {
      return EMPTY;
   }

   public static ScreenRectangle of(ScreenAxis pAxis, int pPrimaryPosition, int pSecondaryPosition, int pPrimaryLength, int pSecondaryLength) {
      return switch (pAxis) {
         case HORIZONTAL -> new ScreenRectangle(pPrimaryPosition, pSecondaryPosition, pPrimaryLength, pSecondaryLength);
         case VERTICAL -> new ScreenRectangle(pSecondaryPosition, pPrimaryPosition, pSecondaryLength, pPrimaryLength);
      };
   }

   public ScreenRectangle step(ScreenDirection pDirection) {
      return new ScreenRectangle(this.position.step(pDirection), this.width, this.height);
   }

   public int getLength(ScreenAxis pAxis) {
      return switch (pAxis) {
         case HORIZONTAL -> this.width;
         case VERTICAL -> this.height;
      };
   }

   public int getBoundInDirection(ScreenDirection pDirection) {
      ScreenAxis $$1 = pDirection.getAxis();
      return pDirection.isPositive() ? this.position.getCoordinate($$1) + this.getLength($$1) - 1 : this.position.getCoordinate($$1);
   }

   public ScreenRectangle getBorder(ScreenDirection pDirection) {
      int $$1 = this.getBoundInDirection(pDirection);
      ScreenAxis $$2 = pDirection.getAxis().orthogonal();
      int $$3 = this.getBoundInDirection($$2.getNegative());
      int $$4 = this.getLength($$2);
      return of(pDirection.getAxis(), $$1, $$3, 1, $$4).step(pDirection);
   }

   public boolean overlaps(ScreenRectangle pRectangle) {
      return this.overlapsInAxis(pRectangle, ScreenAxis.HORIZONTAL) && this.overlapsInAxis(pRectangle, ScreenAxis.VERTICAL);
   }

   public boolean overlapsInAxis(ScreenRectangle pRectangle, ScreenAxis pAxis) {
      int $$2 = this.getBoundInDirection(pAxis.getNegative());
      int $$3 = pRectangle.getBoundInDirection(pAxis.getNegative());
      int $$4 = this.getBoundInDirection(pAxis.getPositive());
      int $$5 = pRectangle.getBoundInDirection(pAxis.getPositive());
      return Math.max($$2, $$3) <= Math.min($$4, $$5);
   }

   public int getCenterInAxis(ScreenAxis pAxis) {
      return (this.getBoundInDirection(pAxis.getPositive()) + this.getBoundInDirection(pAxis.getNegative())) / 2;
   }

   @Nullable
   public ScreenRectangle intersection(ScreenRectangle pRectangle) {
      int $$1 = Math.max(this.left(), pRectangle.left());
      int $$2 = Math.max(this.top(), pRectangle.top());
      int $$3 = Math.min(this.right(), pRectangle.right());
      int $$4 = Math.min(this.bottom(), pRectangle.bottom());
      return $$1 < $$3 && $$2 < $$4 ? new ScreenRectangle($$1, $$2, $$3 - $$1, $$4 - $$2) : null;
   }

   public int top() {
      return this.position.y();
   }

   public int bottom() {
      return this.position.y() + this.height;
   }

   public int left() {
      return this.position.x();
   }

   public int right() {
      return this.position.x() + this.width;
   }

   public boolean containsPoint(int pX, int pY) {
      return pX >= this.left() && pX < this.right() && pY >= this.top() && pY < this.bottom();
   }
}
