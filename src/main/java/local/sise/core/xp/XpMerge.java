package local.sise.core.xp;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Bounded spatial cache, containing weak references only. No world-wide entity scans. */
public final class XpMerge {
    private static final Map<ServerLevel,Cache> CACHES=new WeakHashMap<>();
    private static final class Cache extends LinkedHashMap<Long,WeakReference<ExperienceOrb>> {
        @Override protected boolean removeEldestEntry(Map.Entry<Long,WeakReference<ExperienceOrb>> e) {
            return size()>8192;
        }
    }
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,false,EntityJoinLevelEvent.class,XpMerge::join);
    }
    private static void join(EntityJoinLevelEvent e) {
        if (!XpFlags.enabled || e.loadedFromDisk() || e.isCanceled()) return;
        if (e.getEntity() instanceof ExperienceOrb orb && merge(orb)) {
            // All earlier non-canceled spawn hooks have seen the original incoming orb.
            e.setCanceled(true);
        }
    }
    private static boolean plain(ExperienceOrb orb) {
        var access=(OrbAccess)orb;
        return orb.getClass()==ExperienceOrb.class && orb.getType()==EntityType.EXPERIENCE_ORB
            && !orb.isRemoved() && orb.getValue()>0
            && access.sise$orbCount()==1 && access.sise$orbHealth()==5
            && access.sise$orbAge()<6000 && orb.entityTags().isEmpty()
            && orb.getPersistentData().isEmpty() && !orb.hasCustomName()
            && !orb.isNoGravity() && !orb.isInvulnerable();
    }
    /** Incoming points are transferred before the incoming orb is discarded. */
    public static boolean merge(ExperienceOrb incoming) {
        if (!XpFlags.enabled || !(incoming.level() instanceof ServerLevel level) || !plain(incoming)) return false;
        var cache=CACHES.computeIfAbsent(level,k->new Cache());
        var pos=incoming.blockPosition();
        long ownKey=pos.asLong();
        var own=cache.get(ownKey);
        if (transfer(level,incoming,own)) return true;
        // A new drop can straddle a block boundary. Bound the search to 27 cache entries.
        for (int dx=-1;dx<=1;dx++) for (int dy=-1;dy<=1;dy++) for (int dz=-1;dz<=1;dz++) {
            if ((dx|dy|dz)==0) continue;
            var ref=cache.get(BlockPos.asLong(pos.getX()+dx,pos.getY()+dy,pos.getZ()+dz));
            if (ref!=own && transfer(level,incoming,ref)) return true;
        }
        cache.put(ownKey,new WeakReference<>(incoming));
        return false;
    }
    private static boolean transfer(ServerLevel level,ExperienceOrb incoming,WeakReference<ExperienceOrb> ref) {
        var target=ref==null?null:ref.get();
        if (target==null || target==incoming || !plain(target)
            || level.getEntity(target.getId())!=target || target.distanceToSqr(incoming)>1.0
            || target.getValue()>XpFlags.MAX_VALUE-incoming.getValue()) return false;
        ((OrbAccess)target).sise$orbValue(target.getValue()+incoming.getValue());
        ((OrbAccess)target).sise$orbAge(Math.min(((OrbAccess)target).sise$orbAge(),((OrbAccess)incoming).sise$orbAge()));
        incoming.discard();
        return true;
    }
}
