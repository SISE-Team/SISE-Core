package local.sise.core.xp;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;

public final class XpPickupBudget {
    private static final Map<Player,long[]> BUDGETS=new WeakHashMap<>();
    private XpPickupBudget() {}
    private static long[] state(Player player) {
        long time=player.level().getGameTime();
        var budget=BUDGETS.computeIfAbsent(player,k->new long[]{time,0});
        if (budget[0]!=time) { budget[0]=time;budget[1]=0; }
        return budget;
    }
    public static int remaining(Player player) { return XpFlags.PICKUPS_PER_TICK-(int)state(player)[1]; }
    public static boolean claim(Player player) {
        var budget=state(player);
        if (budget[1]>=XpFlags.PICKUPS_PER_TICK) return false;
        budget[1]++;return true;
    }
}
