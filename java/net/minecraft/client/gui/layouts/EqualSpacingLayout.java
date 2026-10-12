package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EqualSpacingLayout extends AbstractLayout {
   private final EqualSpacingLayout.Orientation orientation;
   private final List<EqualSpacingLayout.ChildContainer> children = new ArrayList<>();
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();

   public EqualSpacingLayout(int pWidth, int pHeight, EqualSpacingLayout.Orientation pOrientation) {
      this(0, 0, pWidth, pHeight, pOrientation);
   }

   public EqualSpacingLayout(int pX, int pY, int pWidth, int pHeight, EqualSpacingLayout.Orientation pOrientation) {
      super(pX, pY, pWidth, pHeight);
      this.orientation = pOrientation;
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int $$0 = 0;
         int $$1 = this.orientation.getSecondaryLength(this);

         for (EqualSpacingLayout.ChildContainer $$2 : this.children) {
            $$0 += this.orientation.getPrimaryLength($$2);
            $$1 = Math.max($$1, this.orientation.getSecondaryLength($$2));
         }

         int $$3 = this.orientation.getPrimaryLength(this) - $$0;
         int $$4 = this.orientation.getPrimaryPosition(this);
         Iterator<EqualSpacingLayout.ChildContainer> $$5 = this.children.iterator();
         EqualSpacingLayout.ChildContainer $$6 = $$5.next();
         this.orientation.setPrimaryPosition($$6, $$4);
         $$4 += this.orientation.getPrimaryLength($$6);
         if (this.children.size() >= 2) {
            Divisor $$7 = new Divisor($$3, this.children.size() - 1);

            while ($$7.hasNext()) {
               $$4 += $$7.nextInt();
               EqualSpacingLayout.ChildContainer $$8 = $$5.next();
               this.orientation.setPrimaryPosition($$8, $$4);
               $$4 += this.orientation.getPrimaryLength($$8);
            }
         }

         int $$9 = this.orientation.getSecondaryPosition(this);

         for (EqualSpacingLayout.ChildContainer $$10 : this.children) {
            this.orientation.setSecondaryPosition($$10, $$9, $$1);
         }

         switch (this.orientation) {
            case HORIZONTAL:
               this.height = $$1;
               break;
            case VERTICAL:
               this.width = $$1;
         }
      }
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> pVisitor) {
      this.children.forEach(p_298818_ -> pVisitor.accept(p_298818_.child));
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(T pChild) {
      return this.addChild(pChild, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T pChild, LayoutSettings pLayoutSettings) {
      this.children.add(new EqualSpacingLayout.ChildContainer(pChild, pLayoutSettings));
      return pChild;
   }

   public <T extends LayoutElement> T addChild(T pChild, Consumer<LayoutSettings> pLayoutSettingsCreator) {
      return this.addChild(pChild, Util.make(this.newChildLayoutSettings(), pLayoutSettingsCreator));
   }

   @OnlyIn(Dist.CLIENT)
   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement p_298955_, LayoutSettings p_298136_) {
         super(p_298955_, p_298136_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Orientation {
      HORIZONTAL,
      VERTICAL;

      int getPrimaryLength(LayoutElement pElement) {
         return switch (this) {
            case HORIZONTAL -> pElement.getWidth();
            case VERTICAL -> pElement.getHeight();
         };
      }

      int getPrimaryLength(EqualSpacingLayout.ChildContainer pContainer) {
         return switch (this) {
            case HORIZONTAL -> pContainer.getWidth();
            case VERTICAL -> pContainer.getHeight();
         };
      }

      int getSecondaryLength(LayoutElement pElement) {
         return switch (this) {
            case HORIZONTAL -> pElement.getHeight();
            case VERTICAL -> pElement.getWidth();
         };
      }

      int getSecondaryLength(EqualSpacingLayout.ChildContainer pContainer) {
         return switch (this) {
            case HORIZONTAL -> pContainer.getHeight();
            case VERTICAL -> pContainer.getWidth();
         };
      }

      void setPrimaryPosition(EqualSpacingLayout.ChildContainer pContainer, int pPosition) {
         switch (this) {
            case HORIZONTAL:
               pContainer.setX(pPosition, pContainer.getWidth());
               break;
            case VERTICAL:
               pContainer.setY(pPosition, pContainer.getHeight());
         }
      }

      void setSecondaryPosition(EqualSpacingLayout.ChildContainer pContainer, int pPosition, int pLength) {
         switch (this) {
            case HORIZONTAL:
               pContainer.setY(pPosition, pLength);
               break;
            case VERTICAL:
               pContainer.setX(pPosition, pLength);
         }
      }

      int getPrimaryPosition(LayoutElement pElement) {
         return switch (this) {
            case HORIZONTAL -> pElement.getX();
            case VERTICAL -> pElement.getY();
         };
      }

      int getSecondaryPosition(LayoutElement pElement) {
         return switch (this) {
            case HORIZONTAL -> pElement.getY();
            case VERTICAL -> pElement.getX();
         };
      }
   }
}
