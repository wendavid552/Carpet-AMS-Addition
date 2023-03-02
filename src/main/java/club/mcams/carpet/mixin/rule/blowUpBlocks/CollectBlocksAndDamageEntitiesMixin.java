package club.mcams.carpet.mixin.rule.blowUpBlocks;

import club.mcams.carpet.AmsServerSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Optional;

//1. Blast resistance, change or not?
//2. Check breakable by resistance or?
@Mixin(value = Explosion.class, priority = 888)
public class CollectBlocksAndDamageEntitiesMixin {

    final static private Map<Block, Float> blockBlast = Map.of(
            Blocks.BEDROCK, AmsServerSettings.blowUpBedRock,
            Blocks.OBSIDIAN, AmsServerSettings.blowUpObsidian,
            Blocks.CRYING_OBSIDIAN, AmsServerSettings.blowUpCryingObsidian
            //#if MC>=11900
            //$$ ,Blocks.REINFORCED_DEEPSLATE, AmsServerSettings.blowUpReinforcedDeepslate
            //#endif
    );
    @Shadow
    @Final
    private ExplosionBehavior behavior;

    @Redirect(
            method = "collectBlocksAndDamageEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/explosion/ExplosionBehavior;getBlastResistance(Lnet/minecraft/world/explosion/Explosion;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Ljava/util/Optional;"
            )
    )
    public Optional<Float> getModifiedBlastResistance(ExplosionBehavior instance, Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (blockBlast.containsKey(blockState.getBlock())) {
            return Optional.of(blockBlast.get(blockState.getBlock()));
        }
        return this.behavior.getBlastResistance(explosion, world, pos, blockState, fluidState);
    }
}