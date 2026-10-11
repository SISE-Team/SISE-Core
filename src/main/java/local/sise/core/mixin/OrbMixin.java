package local.sise.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.sise.core.xp.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class OrbMixin {
    @Shadow private int count;

    @WrapMethod(method="awardWithDirection",require=1,allow=1)
    private static void sise$award(ServerLevel level,Vec3 pos,Vec3 direction,int amount,Operation<Void> original) {
        if (!XpFlags.enabled) { original.call(level,pos,direction,amount); return; }
        if (amount>0) level.addFreshEntity(new ExperienceOrb(level,pos,direction,amount));
    }

    @Inject(method="addAdditionalSaveData",at=@At("TAIL"),require=1,allow=1)
    private void sise$saveBulk(ValueOutput output,CallbackInfo ci) {
        var orb=(ExperienceOrb)(Object)this;
        if (orb.getValue()>Short.MAX_VALUE && count==1) {
            output.putInt("SISEBulkXp",orb.getValue());
            // Vanilla without Core can still read the exact total as counted one-point units.
            output.putShort("Value",(short)1);
            output.putInt("Count",orb.getValue());
        }
    }

    @Inject(method="readAdditionalSaveData",at=@At("TAIL"),require=1,allow=1)
    private void sise$loadBulk(ValueInput input,CallbackInfo ci) {
        int total=input.getIntOr("SISEBulkXp",0);
        if (total>Short.MAX_VALUE) {
            ((OrbAccess)this).sise$orbValue(total);
            count=1;
        }
    }

    @WrapMethod(method="scanForMerges",require=1,allow=1)
    private void sise$merge(Operation<Void> original) {
        if (!XpFlags.enabled) { original.call(); return; }
        XpMerge.merge((ExperienceOrb)(Object)this);
    }

    @WrapMethod(method="playerTouch",require=1,allow=1)
    private void sise$pickup(Player player,Operation<Void> original) {
        var orb=(ExperienceOrb)(Object)this;
        if (!XpFlags.enabled || !(player instanceof ServerPlayer)) { original.call(player); return; }
        if (orb.isRemoved() || player.takeXpDelay!=0) return;
        while (!orb.isRemoved() && XpPickupBudget.claim(player)) {
            int before=count;
            // Keep PickupXp cancellation, Curios, Mending, XpChange and LevelChange hooks.
            original.call(player);
            if (orb.isRemoved() && player.takeXpDelay==2) player.takeXpDelay=0;
            if (count>=before) return;
            player.takeXpDelay=0;
        }
    }

    @WrapMethod(method="repairPlayerItems",require=1,allow=1)
    private int sise$safeMending(ServerPlayer player,int amount,Operation<Integer> original) {
        if (!XpFlags.enabled || amount<=Short.MAX_VALUE) return original.call(player,amount);
        // Retain vanilla equipment selection and repair effects while bounding int products.
        int remaining=amount;
        while (remaining>0) {
            int batch=Math.min(remaining,Short.MAX_VALUE);
            int unused=original.call(player,batch);
            remaining-=batch;
            if (unused>0) return remaining+unused;
        }
        return 0;
    }
}
