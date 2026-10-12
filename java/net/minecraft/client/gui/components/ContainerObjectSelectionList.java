package net.minecraft.client.gui.components;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ContainerObjectSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   public ContainerObjectSelectionList(Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
      super(pMinecraft, pWidth, pHeight, pY, pItemHeight);
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (!(pEvent instanceof FocusNavigationEvent.ArrowNavigation $$1)) {
         return super.nextFocusPath(pEvent);
      } else {
         E $$2 = this.getFocused();
         if ($$1.direction().getAxis() == ScreenAxis.HORIZONTAL && $$2 != null) {
            return ComponentPath.path(this, $$2.nextFocusPath(pEvent));
         } else {
            int $$3 = -1;
            ScreenDirection $$4 = $$1.direction();
            if ($$2 != null) {
               $$3 = $$2.children().indexOf($$2.getFocused());
            }

            if ($$3 == -1) {
               switch ($$4) {
                  case LEFT:
                     $$3 = Integer.MAX_VALUE;
                     $$4 = ScreenDirection.DOWN;
                     break;
                  case RIGHT:
                     $$3 = 0;
                     $$4 = ScreenDirection.DOWN;
                     break;
                  default:
                     $$3 = 0;
               }
            }

            E $$5 = $$2;

            ComponentPath $$6;
            do {
               $$5 = this.nextEntry($$4, p_340774_ -> !p_340774_.children().isEmpty(), $$5);
               if ($$5 == null) {
                  return null;
               }

               $$6 = $$5.focusPathAtIndex($$1, $$3);
            } while ($$6 == null);

            return ComponentPath.path(this, $$6);
         }
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener pFocused) {
      if (this.getFocused() != pFocused) {
         super.setFocused(pFocused);
         if (pFocused == null) {
            this.setSelected(null);
         }
      }
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : super.narrationPriority();
   }

   @Override
   protected boolean isSelectedItem(int pIndex) {
      return false;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
      E $$1 = this.getHovered();
      if ($$1 != null) {
         $$1.updateNarration(pNarrationElementOutput.nest());
         this.narrateListElementPosition(pNarrationElementOutput, $$1);
      } else {
         E $$2 = this.getFocused();
         if ($$2 != null) {
            $$2.updateNarration(pNarrationElementOutput.nest());
            this.narrateListElementPosition(pNarrationElementOutput, $$2);
         }
      }

      pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements ContainerEventHandler {
      @Nullable
      private GuiEventListener focused;
      @Nullable
      private NarratableEntry lastNarratable;
      private boolean dragging;

      @Override
      public boolean isDragging() {
         return this.dragging;
      }

      @Override
      public void setDragging(boolean pDragging) {
         this.dragging = pDragging;
      }

      @Override
      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         return ContainerEventHandler.super.mouseClicked(pMouseX, pMouseY, pButton);
      }

      @Override
      public void setFocused(@Nullable GuiEventListener pListener) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         if (pListener != null) {
            pListener.setFocused(true);
         }

         this.focused = pListener;
      }

      @Nullable
      @Override
      public GuiEventListener getFocused() {
         return this.focused;
      }

      @Nullable
      public ComponentPath focusPathAtIndex(FocusNavigationEvent pEvent, int pIndex) {
         if (this.children().isEmpty()) {
            return null;
         } else {
            ComponentPath $$2 = this.children().get(Math.min(pIndex, this.children().size() - 1)).nextFocusPath(pEvent);
            return ComponentPath.path(this, $$2);
         }
      }

      @Nullable
      @Override
      public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
         if (pEvent instanceof FocusNavigationEvent.ArrowNavigation $$1) {
            int $$2 = switch ($$1.direction()) {
               case LEFT -> -1;
               case RIGHT -> 1;
               case UP, DOWN -> 0;
            };
            if ($$2 == 0) {
               return null;
            }

            int $$3 = Mth.clamp($$2 + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

            for (int $$4 = $$3; $$4 >= 0 && $$4 < this.children().size(); $$4 += $$2) {
               GuiEventListener $$5 = this.children().get($$4);
               ComponentPath $$6 = $$5.nextFocusPath(pEvent);
               if ($$6 != null) {
                  return ComponentPath.path(this, $$6);
               }
            }
         }

         return ContainerEventHandler.super.nextFocusPath(pEvent);
      }

      public abstract List<? extends NarratableEntry> narratables();

      void updateNarration(NarrationElementOutput pNarrationElementOutput) {
         List<? extends NarratableEntry> $$1 = this.narratables();
         Screen.NarratableSearchResult $$2 = Screen.findNarratableWidget($$1, this.lastNarratable);
         if ($$2 != null) {
            if ($$2.priority.isTerminal()) {
               this.lastNarratable = $$2.entry;
            }

            if ($$1.size() > 1) {
               pNarrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.object_list", $$2.index + 1, $$1.size()));
               if ($$2.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                  pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
               }
            }

            $$2.entry.updateNarration(pNarrationElementOutput.nest());
         }
      }
   }
}
