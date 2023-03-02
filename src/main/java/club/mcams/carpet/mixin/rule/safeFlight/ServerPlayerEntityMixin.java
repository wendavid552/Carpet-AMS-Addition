package club.mcams.carpet.mixin.rule.safeFlight;

import club.mcams.carpet.AmsServerSettings;

import net.minecraft.entity.damage.DamageSource;
//#if MC>=11940
//$$ import net.minecraft.entity.damage.DamageSource;
//#endif
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "isInvulnerableTo",at = @At("TAIL"), cancellable = true)
    public void isInvulnerableTo(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (AmsServerSettings.safeFlight && damageSource.equals(
                //#if MC<=11920
                DamageSource.FLY_INTO_WALL
                //#else
                //$$ DamageSources.flyIntoWall
                //#endif
        )) {
            cir.setReturnValue(true);
        }
    }
}