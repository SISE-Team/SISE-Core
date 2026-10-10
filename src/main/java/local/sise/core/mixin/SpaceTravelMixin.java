package local.sise.core.mixin;
import local.sise.core.space.SpaceRuntime;
import net.minecraft.server.level.*;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class SpaceTravelMixin {
    @Inject(method="teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",at=@At("HEAD"),cancellable=true)
    private void sise$permission(TeleportTransition transition,CallbackInfoReturnable<ServerPlayer> ci) {
        var p=(ServerPlayer)(Object)this;
        if(!SpaceRuntime.permitted(p.level().getServer(),transition.newLevel().dimension(),p.getUUID())) {
            SpaceRuntime.message(p,"space.sise_core.denied");ci.setReturnValue(null);
        }
    }
}
