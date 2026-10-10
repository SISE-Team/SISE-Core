package local.sise.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Bound visual work before the vanilla per-particle loop, including packets from older servers. */
@Mixin(value=ClientPacketListener.class,remap=false)
public abstract class DamageParticleLimitMixin {
    @ModifyExpressionValue(method="handleParticleEvent",at=@At(value="INVOKE",
        target="Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;getCount()I"))
    private int sise$boundedDamageParticles(int count,ClientboundLevelParticlesPacket packet) {
        return packet.getParticle().getType()==ParticleTypes.DAMAGE_INDICATOR ? Math.min(count,64) : count;
    }
}
