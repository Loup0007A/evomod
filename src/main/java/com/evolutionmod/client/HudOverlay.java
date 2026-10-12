package com.evolutionmod.client;

import com.evolutionmod.client.gui.GuiDraw;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.Traits;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.network.NotifyMsg;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Éléments de HUD du mod : notifications animées (cycles, spéciations,
 * mutations du joueur) et lecture rapide du génome avec le scanner en main.
 */
public final class HudOverlay {

    private HudOverlay() {}

    private record Notice(NotifyMsg.Kind kind, Component title, Component body, long start) {}

    private static final List<Notice> NOTICES = new ArrayList<>();
    private static final long DURATION = 6000;
    private static final long SLIDE = 250;
    private static LivingEntity scanned;

    public static void push(NotifyMsg msg) {
        Object[] args = msg.args().stream().map(HudOverlay::resolveArg).toArray();
        Component body = Component.translatable(msg.key(), args);
        Component title = Component.translatable("evolutionmod.notify.title." + msg.kind().name().toLowerCase(java.util.Locale.ROOT));
        NOTICES.add(new Notice(msg.kind(), title, body, Util.getMillis()));
        while (NOTICES.size() > 4) NOTICES.remove(0);
    }

    private static Object resolveArg(String a) {
        if (a.startsWith("#species:")) return EvolutionManager.speciesComponent(a.substring(9));
        if (a.startsWith("#reason:")) return Component.translatable("evolutionmod.reason." + a.substring(8));
        if (a.startsWith("#item:")) {
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                net.minecraft.resources.ResourceLocation.parse(a.substring(6)));
            return item == null ? a : item.getDescription();
        }
        if (a.startsWith("#gene:")) {
            GeneType t = GeneType.byName(a.substring(6));
            return t == null ? a : Component.translatable(t.translationKey());
        }
        return a;
    }

    public static void clear() {
        NOTICES.clear();
        scanned = null;
    }

    /** Appelé à chaque tick client : recherche la créature visée par le scanner. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        scanned = null;
        if (player == null || mc.level == null) return;
        boolean holding = player.getMainHandItem().is(ModItems.EVOLUTION_SCANNER.get())
            || player.getOffhandItem().is(ModItems.EVOLUTION_SCANNER.get());
        if (!holding) return;
        double reach = 20;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(reach));
        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
            e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable(), reach * reach);
        if (hit != null) {
            Entity e = hit.getEntity();
            // ne pas « voir » à travers les murs
            Vec3 hitPos = hit.getLocation();
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && mc.hitResult.getLocation().distanceToSqr(eye) < hitPos.distanceToSqr(eye)) {
                return;
            }
            if (e instanceof LivingEntity l) scanned = l;
        }
    }

    public static void render(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        renderNotices(g, mc);
        renderScanner(g, mc);
    }

    private static void renderNotices(GuiGraphics g, Minecraft mc) {
        long now = Util.getMillis();
        Iterator<Notice> it = NOTICES.iterator();
        while (it.hasNext()) if (now - it.next().start() > DURATION) it.remove();
        if (NOTICES.isEmpty()) return;

        Font font = mc.font;
        int screenW = g.guiWidth();
        int w = 180;
        int y = 6;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        for (Notice n : NOTICES) {
            long age = now - n.start();
            float slide = Math.min(1f, age / (float) SLIDE);
            float fade = age > DURATION - 600 ? (DURATION - age) / 600f : 1f;
            slide = 1f - (1f - slide) * (1f - slide);
            int alpha = (int) (Math.max(0, fade) * 230);
            if (alpha < 8) continue;
            List<FormattedCharSequence> lines = font.split(n.body(), w - 16);
            int h = 16 + lines.size() * 9 + 3;
            int x = 6 - (int) (w * (1f - slide)); // en haut à gauche : les toasts vanilla occupent la droite
            g.fill(x, y, x + w, y + h, GuiDraw.withAlpha(0x0E161C, alpha));
            g.fill(x, y, x + 2, y + h, GuiDraw.withAlpha(n.kind().color, alpha));
            g.renderOutline(x, y, w, h, GuiDraw.withAlpha(0x2F4A5C, alpha));
            // barre de durée
            int bw = (int) ((w - 2) * (1f - age / (float) DURATION));
            g.fill(x + 2, y + h - 1, x + 2 + Math.max(0, bw), y + h, GuiDraw.withAlpha(n.kind().color, alpha / 2));
            int textAlpha = Math.max(alpha, 10);
            g.drawString(font, n.title(), x + 7, y + 4, GuiDraw.withAlpha(n.kind().color, textAlpha), false);
            int ly = y + 15;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, x + 7, ly, GuiDraw.withAlpha(0xE6F1F5, textAlpha), false);
                ly += 9;
            }
            y += h + 4;
        }
        g.pose().popPose();
    }

    private static void renderScanner(GuiGraphics g, Minecraft mc) {
        LivingEntity e = scanned;
        if (e == null || !e.isAlive() || mc.screen != null) return;
        Font font = mc.font;
        Genome genome = EvolutionCapability.genome(e);
        int cx = g.guiWidth() / 2 + 14;
        int cy = g.guiHeight() / 2 - 10;

        List<Component> lines = new ArrayList<>();
        lines.add(e.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
        if (genome == null) {
            lines.add(Component.translatable("evolutionmod.hud.no_genome").withStyle(ChatFormatting.GRAY));
        } else {
            if (!genome.lineage().isEmpty()) {
                lines.add(Component.literal(genome.lineage()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            }
            lines.add(Component.translatable("evolutionmod.hud.mutations", genome.totalMutations(), genome.generation())
                .withStyle(ChatFormatting.GRAY));
            List<Traits.Trait> traits = Traits.of(genome);
            if (!traits.isEmpty()) {
                MutableComponent t = Component.empty();
                for (int i = 0; i < Math.min(3, traits.size()); i++) {
                    if (i > 0) t.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
                    int color = traits.get(i).color();
                    t.append(Component.translatable(traits.get(i).translationKey()).withStyle(s -> s.withColor(color & 0xFFFFFF)));
                }
                if (traits.size() > 3) t.append(Component.literal(" +" + (traits.size() - 3)).withStyle(ChatFormatting.DARK_GRAY));
                lines.add(t);
            }
            lines.add(Component.translatable("evolutionmod.hud.click").withStyle(ChatFormatting.DARK_AQUA));
        }

        int w = 0;
        for (Component c : lines) w = Math.max(w, font.width(c));
        w += 12;
        int h = lines.size() * 10 + 6;
        int x = cx, y = cy;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(x, y, x + w, y + h, 0xC00B1218);
        g.renderOutline(x, y, w, h, 0xFF2F4A5C);
        g.fill(x, y, x + 2, y + h, GuiDraw.ACCENT);
        // petit trait vers le réticule
        g.hLine(x - 8, x - 1, y + 6, 0x8057E3C2);
        int ly = y + 4;
        for (Component c : lines) {
            g.drawString(font, c, x + 7, ly, 0xFFFFFFFF, false);
            ly += 10;
        }
        g.pose().popPose();
    }
}
