package local.sise.core.mixin;

import local.sise.core.xp.OrbAccess;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperienceOrb.class)
public interface OrbAccessMixin extends OrbAccess {
    @Accessor("count") int sise$orbCount();
    @Accessor("age") int sise$orbAge();
    @Accessor("health") int sise$orbHealth();
    @Accessor("age") void sise$orbAge(int age);
    @Invoker("setValue") void sise$orbValue(int value);
}
