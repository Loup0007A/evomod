package com.evolutionmod.client;

import com.evolutionmod.client.gui.JournalScreen;
import com.evolutionmod.client.gui.ScanScreen;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.network.JournalDataMsg;
import com.evolutionmod.network.NotifyMsg;
import com.evolutionmod.network.ScanResultMsg;
import com.evolutionmod.network.SyncGenomeMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Traitement des paquets côté client. Cette classe n'est jamais chargée sur un serveur dédié. */
public final class ClientPacketHandler {

    private ClientPacketHandler() {}

    public static void onSyncGenome(SyncGenomeMsg msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(msg.entityId());
        if (e == null) return;
        Genome g = Genome.isValid(msg.genome()) ? Genome.load(msg.genome()) : null;
        EvolutionCapability.get(e).ifPresent(cap -> cap.setGenome(g));
    }

    public static void onJournal(JournalDataMsg msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof JournalScreen journal) {
            journal.setData(msg.data());
        } else {
            mc.setScreen(new JournalScreen(msg.data()));
        }
    }

    public static void onScan(ScanResultMsg msg) {
        Minecraft mc = Minecraft.getInstance();
        if (!Genome.isValid(msg.genome())) return;
        mc.setScreen(new ScanScreen(msg.entityId(), msg.genome(), msg.species()));
    }

    public static void onNotify(NotifyMsg msg) {
        HudOverlay.push(msg);
    }
}
