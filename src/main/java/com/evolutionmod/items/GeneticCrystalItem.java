package com.evolutionmod.items;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.core.Phenotype;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Cristal génomique : stocke un génome complet.
 *
 * <ul>
 *   <li>Vide + clic droit sur une créature : séquence (copie) son génome.</li>
 *   <li>Plein + Maj + clic droit sur une créature de la même espèce : injection
 *       (croisement du génome stocké avec celui de la créature).</li>
 *   <li>Au laboratoire : fusion, clonage, mutagenèse.</li>
 * </ul>
 */
public class GeneticCrystalItem extends Item {

    private static final String KEY = "StoredGenome";

    public GeneticCrystalItem() {
        super(new Item.Properties().stacksTo(16).fireResistant().rarity(Rarity.UNCOMMON));
    }

    // ───────────────────────── Données ─────────────────────────

    public static boolean hasGenome(ItemStack stack) {
        if (!(stack.getItem() instanceof GeneticCrystalItem)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(KEY);
    }

    @Nullable
    public static Genome getGenome(ItemStack stack) {
        if (!hasGenome(stack)) return null;
        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompound(KEY);
        return Genome.isValid(tag) ? Genome.load(tag) : null;
    }

    public static ItemStack withGenome(ItemStack stack, Genome genome) {
        CompoundTag root = new CompoundTag();
        root.put(KEY, genome.save());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    public static void clear(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    // ───────────────────────── Interaction ─────────────────────────

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        Genome targetGenome = EvolutionCapability.genome(target);
        if (targetGenome == null) {
            player.displayClientMessage(Component.translatable("evolutionmod.crystal.no_genome")
                .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && hasGenome(held)) {
            return inject(held, player, target, targetGenome);
        }
        if (hasGenome(held)) {
            player.displayClientMessage(Component.translatable("evolutionmod.crystal.full")
                .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }

        // Séquençage : un cristal vide de la pile devient un cristal plein
        ItemStack filled = withGenome(new ItemStack(this), targetGenome);
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, filled);
        } else if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        effects(target, true);
        player.displayClientMessage(Component.translatable("evolutionmod.crystal.sequenced",
            target.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult inject(ItemStack held, Player player, LivingEntity target, Genome targetGenome) {
        Genome stored = getGenome(held);
        if (stored == null) return InteractionResult.FAIL;
        if (!stored.species().equals(targetGenome.species())) {
            player.displayClientMessage(Component.translatable("evolutionmod.crystal.incompatible")
                .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        Genome hybrid = Genome.crossover(targetGenome, stored, target.getRandom());
        EvolutionCapability.get(target).ifPresent(cap -> cap.setGenome(hybrid));
        Phenotype.apply(target, hybrid, false);
        EvolutionManager.sync(target);
        effects(target, false);
        player.displayClientMessage(Component.translatable("evolutionmod.crystal.injected",
            target.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static void effects(LivingEntity target, boolean sequence) {
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(sequence ? ParticleTypes.ENCHANT : ParticleTypes.WITCH,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                30, target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, 0.2);
            level.playSound(null, target.blockPosition(),
                sequence ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.BREWING_STAND_BREW,
                SoundSource.PLAYERS, 1.0f, sequence ? 1.4f : 1.0f);
        }
    }

    // ───────────────────────── Affichage ─────────────────────────

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasGenome(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Genome g = getGenome(stack);
        if (g == null) return Component.translatable("item.evolutionmod.genetic_crystal.empty");
        return Component.translatable("item.evolutionmod.genetic_crystal.filled",
            EvolutionManager.speciesComponent(g.species()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        Genome g = getGenome(stack);
        if (g == null) {
            tooltip.add(Component.translatable("evolutionmod.crystal.tip.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!g.lineage().isEmpty()) {
            tooltip.add(Component.translatable("evolutionmod.tip.lineage", g.lineage())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("evolutionmod.tip.mutations", g.totalMutations(), g.generation())
            .withStyle(ChatFormatting.GRAY));
        g.genes().stream()
            .filter(t -> t != GeneType.HUE)
            .sorted(Comparator.comparingDouble((GeneType t) -> -t.distance(g.get(t), t.neutral)))
            .limit(6)
            .forEach(t -> tooltip.add(Component.literal(" • ")
                .append(Component.translatable(t.translationKey()))
                .append(Component.literal(" " + t.format(g.get(t))).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.DARK_AQUA)));
        tooltip.add(Component.translatable("evolutionmod.crystal.tip.full").withStyle(ChatFormatting.DARK_GRAY));
    }
}
