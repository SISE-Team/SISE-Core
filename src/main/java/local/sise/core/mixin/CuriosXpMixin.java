package local.sise.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets="top.theillusivec4.curios.common.CuriosCommonEvents")
public abstract class CuriosXpMixin {
    @WrapOperation(method="handleMending",at=@At(value="INVOKE",target="Ljava/lang/Math;min(II)I"),require=1,allow=1)
    private static int sise$safeRepair(int xpRepair,int damage,Operation<Integer> original,@Local ExperienceOrb orb) {
        return original.call((int)Math.min((long)orb.getValue()*2,Integer.MAX_VALUE),damage);
    }
}
