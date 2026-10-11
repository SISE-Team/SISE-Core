package local.sise.core.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.sise.core.xp.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets="com.enderio.enderio.content.machines.vacuum.xp.XPVacuumBlockEntity")
public abstract class EnderIoXpMixin {
    @WrapMethod(method="handleEntity(Lnet/minecraft/world/entity/ExperienceOrb;)V",require=1,allow=1)
    private void sise$wholePoints(ExperienceOrb orb,Operation<Void> original) {
        if (orb.isRemoved()) return;
        // Existing bulk orbs still need overflow protection after the optimization is disabled.
        if ((!XpFlags.enabled && orb.getValue()<=Short.MAX_VALUE) || ((OrbAccess)orb).sise$orbCount()!=1 || orb.getValue()<=0) { original.call(orb); return; }
        var machine=(BlockEntity)(Object)this;
        var level=machine.getLevel();
        if (level==null) return;
        var tank=level.getCapability(Capabilities.Fluid.BLOCK,machine.getBlockPos(),machine.getBlockState(),machine,null);
        var fluid=BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("enderio","fluid_xp_juice_still"));
        if (tank==null || fluid==null || fluid==Fluids.EMPTY) return;
        int used=XpFluidTransfer.insert(tank,FluidResource.of(fluid),orb.getValue(),20);
        if (used==0) return;
        ((OrbAccess)orb).sise$orbValue(orb.getValue()-used);
        if (orb.getValue()==0) orb.discard();
    }
}
