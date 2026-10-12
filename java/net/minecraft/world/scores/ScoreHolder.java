package net.minecraft.world.scores;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public interface ScoreHolder {
   String WILDCARD_NAME = "*";
   ScoreHolder WILDCARD = new ScoreHolder() {
      @Override
      public String getScoreboardName() {
         return "*";
      }
   };

   String getScoreboardName();

   @Nullable
   default Component getDisplayName() {
      return null;
   }

   default Component getFeedbackDisplayName() {
      Component $$0 = this.getDisplayName();
      return $$0 != null
         ? $$0.copy()
            .withStyle(p_312428_ -> p_312428_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.getScoreboardName()))))
         : Component.literal(this.getScoreboardName());
   }

   static ScoreHolder forNameOnly(final String pName) {
      if (pName.equals("*")) {
         return WILDCARD;
      } else {
         final Component $$1 = Component.literal(pName);
         return new ScoreHolder() {
            @Override
            public String getScoreboardName() {
               return pName;
            }

            @Override
            public Component getFeedbackDisplayName() {
               return $$1;
            }
         };
      }
   }

   static ScoreHolder fromGameProfile(GameProfile pGameProfile) {
      final String $$1 = pGameProfile.getName();
      return new ScoreHolder() {
         @Override
         public String getScoreboardName() {
            return $$1;
         }
      };
   }
}
