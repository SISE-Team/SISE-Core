package local.sise.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import local.sise.core.xp.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerXpMixin {
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;touch(Lnet/minecraft/world/entity/Entity;)V",ordinal=1),require=1,allow=1)
    private void sise$touchBatch(Player player,Entity selected,Operation<Void> original,@Local(ordinal=1) List<Entity> xpOrbs) {
        original.call(player,selected);
        if (!XpFlags.enabled || !(player instanceof ServerPlayer)) return;
        // Reuse vanilla's collision list and random first choice, without another world query.
        for (var orb:xpOrbs) {
            if (XpPickupBudget.remaining(player)==0 || player.takeXpDelay!=0) break;
            if (orb!=selected && !orb.isRemoved()) original.call(player,orb);
        }
    }

    @WrapMethod(method="giveExperiencePoints",require=1,allow=1)
    private void sise$precisePoints(int amount,Operation<Void> original) {
        if (!XpFlags.enabled) { original.call(amount); return; }
        var player=(Player)(Object)this;
        double before=XpPrecision.before(player);
        int total=player.totalExperience;
        original.call(amount);
        XpPrecision.after(player,before,total);
    }

    @WrapOperation(method="giveExperiencePoints",at=@At(value="INVOKE",target="Lnet/minecraft/util/Mth;clamp(III)I"),require=1,allow=1)
    private int sise$saturateTotal(int sum,int min,int max,Operation<Integer> original,@Local(argsOnly=true) int amount) {
        if (!XpFlags.enabled) return original.call(sum,min,max);
        var player=(Player)(Object)this;
        return (int)Math.clamp((long)player.totalExperience+amount,(long)min,(long)max);
    }
}
