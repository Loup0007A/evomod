package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public interface ContainerEventHandler extends GuiEventListener {
   List<? extends GuiEventListener> children();

   default Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
      for (GuiEventListener $$2 : this.children()) {
         if ($$2.isMouseOver(pMouseX, pMouseY)) {
            return Optional.of($$2);
         }
      }

      return Optional.empty();
   }

   @Override
   default boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      for (GuiEventListener $$3 : this.children()) {
         if ($$3.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.setFocused($$3);
            if (pButton == 0) {
               this.setDragging(true);
            }

            return true;
         }
      }

      return false;
   }

   @Override
   default boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0 && this.isDragging()) {
         this.setDragging(false);
         if (this.getFocused() != null) {
            return this.getFocused().mouseReleased(pMouseX, pMouseY, pButton);
         }
      }

      return this.getChildAt(pMouseX, pMouseY).filter(p_94708_ -> p_94708_.mouseReleased(pMouseX, pMouseY, pButton)).isPresent();
   }

   @Override
   default boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      return this.getFocused() != null && this.isDragging() && pButton == 0 ? this.getFocused().mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY) : false;
   }

   boolean isDragging();

   void setDragging(boolean pIsDragging);

   @Override
   default boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      return this.getChildAt(pMouseX, pMouseY).filter(p_296182_ -> p_296182_.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY)).isPresent();
   }

   @Override
   default boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return this.getFocused() != null && this.getFocused().keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   @Override
   default boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
      return this.getFocused() != null && this.getFocused().keyReleased(pKeyCode, pScanCode, pModifiers);
   }

   @Override
   default boolean charTyped(char pCodePoint, int pModifiers) {
      return this.getFocused() != null && this.getFocused().charTyped(pCodePoint, pModifiers);
   }

   @Nullable
   GuiEventListener getFocused();

   void setFocused(@Nullable GuiEventListener pFocused);

   @Override
   default void setFocused(boolean pFocused) {
   }

   @Override
   default boolean isFocused() {
      return this.getFocused() != null;
   }

   @Nullable
   @Override
   default ComponentPath getCurrentFocusPath() {
      GuiEventListener $$0 = this.getFocused();
      return $$0 != null ? ComponentPath.path(this, $$0.getCurrentFocusPath()) : null;
   }

   @Nullable
   @Override
   default ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
      GuiEventListener $$1 = this.getFocused();
      if ($$1 != null) {
         ComponentPath $$2 = $$1.nextFocusPath(pEvent);
         if ($$2 != null) {
            return ComponentPath.path(this, $$2);
         }
      }

      if (pEvent instanceof FocusNavigationEvent.TabNavigation $$3) {
         return this.handleTabNavigation($$3);
      } else {
         return pEvent instanceof FocusNavigationEvent.ArrowNavigation $$4 ? this.handleArrowNavigation($$4) : null;
      }
   }

   @Nullable
   private ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation pTabNavigation) {
      boolean $$1 = pTabNavigation.forward();
      GuiEventListener $$2 = this.getFocused();
      List<? extends GuiEventListener> $$3 = new ArrayList<>(this.children());
      Collections.sort($$3, Comparator.comparingInt(p_340777_ -> p_340777_.getTabOrderGroup()));
      int $$4 = $$3.indexOf($$2);
      int $$5;
      if ($$2 != null && $$4 >= 0) {
         $$5 = $$4 + ($$1 ? 1 : 0);
      } else if ($$1) {
         $$5 = 0;
      } else {
         $$5 = $$3.size();
      }

      ListIterator<? extends GuiEventListener> $$8 = $$3.listIterator($$5);
      BooleanSupplier $$9 = $$1 ? $$8::hasNext : $$8::hasPrevious;
      Supplier<? extends GuiEventListener> $$10 = $$1 ? $$8::next : $$8::previous;

      while ($$9.getAsBoolean()) {
         GuiEventListener $$11 = $$10.get();
         ComponentPath $$12 = $$11.nextFocusPath(pTabNavigation);
         if ($$12 != null) {
            return ComponentPath.path(this, $$12);
         }
      }

      return null;
   }

   @Nullable
   private ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation pArrowNavigation) {
      GuiEventListener $$1 = this.getFocused();
      if ($$1 == null) {
         ScreenDirection $$2 = pArrowNavigation.direction();
         ScreenRectangle $$3 = this.getRectangle().getBorder($$2.getOpposite());
         return ComponentPath.path(this, this.nextFocusPathInDirection($$3, $$2, null, pArrowNavigation));
      } else {
         ScreenRectangle $$4 = $$1.getRectangle();
         return ComponentPath.path(this, this.nextFocusPathInDirection($$4, pArrowNavigation.direction(), $$1, pArrowNavigation));
      }
   }

   @Nullable
   private ComponentPath nextFocusPathInDirection(
      ScreenRectangle pRectangle, ScreenDirection pDirection, @Nullable GuiEventListener pListener, FocusNavigationEvent pEvent
   ) {
      ScreenAxis $$4 = pDirection.getAxis();
      ScreenAxis $$5 = $$4.orthogonal();
      ScreenDirection $$6 = $$5.getPositive();
      int $$7 = pRectangle.getBoundInDirection(pDirection.getOpposite());
      List<GuiEventListener> $$8 = new ArrayList<>();

      for (GuiEventListener $$9 : this.children()) {
         if ($$9 != pListener) {
            ScreenRectangle $$10 = $$9.getRectangle();
            if ($$10.overlapsInAxis(pRectangle, $$5)) {
               int $$11 = $$10.getBoundInDirection(pDirection.getOpposite());
               if (pDirection.isAfter($$11, $$7)) {
                  $$8.add($$9);
               } else if ($$11 == $$7 && pDirection.isAfter($$10.getBoundInDirection(pDirection), pRectangle.getBoundInDirection(pDirection))) {
                  $$8.add($$9);
               }
            }
         }
      }

      Comparator<GuiEventListener> $$12 = Comparator.comparing(
         p_264674_ -> p_264674_.getRectangle().getBoundInDirection(pDirection.getOpposite()), pDirection.coordinateValueComparator()
      );
      Comparator<GuiEventListener> $$13 = Comparator.comparing(
         p_264676_ -> p_264676_.getRectangle().getBoundInDirection($$6.getOpposite()), $$6.coordinateValueComparator()
      );
      $$8.sort($$12.thenComparing($$13));

      for (GuiEventListener $$14 : $$8) {
         ComponentPath $$15 = $$14.nextFocusPath(pEvent);
         if ($$15 != null) {
            return $$15;
         }
      }

      return this.nextFocusPathVaguelyInDirection(pRectangle, pDirection, pListener, pEvent);
   }

   @Nullable
   private ComponentPath nextFocusPathVaguelyInDirection(
      ScreenRectangle pRectangle, ScreenDirection pDirection, @Nullable GuiEventListener pListener, FocusNavigationEvent pEvent
   ) {
      ScreenAxis $$4 = pDirection.getAxis();
      ScreenAxis $$5 = $$4.orthogonal();
      List<Pair<GuiEventListener, Long>> $$6 = new ArrayList<>();
      ScreenPosition $$7 = ScreenPosition.of($$4, pRectangle.getBoundInDirection(pDirection), pRectangle.getCenterInAxis($$5));

      for (GuiEventListener $$8 : this.children()) {
         if ($$8 != pListener) {
            ScreenRectangle $$9 = $$8.getRectangle();
            ScreenPosition $$10 = ScreenPosition.of($$4, $$9.getBoundInDirection(pDirection.getOpposite()), $$9.getCenterInAxis($$5));
            if (pDirection.isAfter($$10.getCoordinate($$4), $$7.getCoordinate($$4))) {
               long $$11 = Vector2i.distanceSquared($$7.x(), $$7.y(), $$10.x(), $$10.y());
               $$6.add(Pair.of($$8, $$11));
            }
         }
      }

      $$6.sort(Comparator.comparingDouble(Pair::getSecond));

      for (Pair<GuiEventListener, Long> $$12 : $$6) {
         ComponentPath $$13 = ((GuiEventListener)$$12.getFirst()).nextFocusPath(pEvent);
         if ($$13 != null) {
            return $$13;
         }
      }

      return null;
   }
}
