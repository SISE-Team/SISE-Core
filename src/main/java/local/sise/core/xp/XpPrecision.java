package local.sise.core.xp;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;

/** Carries exact points between bundled pickups instead of accumulating float error. */
public final class XpPrecision {
    private static final Map<Player,Ledger> LEDGERS=new WeakHashMap<>();
    private static final class Ledger {
        double points;int level,progress,total;
        boolean matches(Player p) {
            return level==p.experienceLevel && progress==Float.floatToIntBits(p.experienceProgress) && total==p.totalExperience;
        }
        void remember(Player p) {level=p.experienceLevel;progress=Float.floatToIntBits(p.experienceProgress);total=p.totalExperience;}
    }
    private XpPrecision() {}
    private static long base(int level) {
        long l=level;
        if(level<=16)return l*l+6*l;
        if(level<=31)return (5*l*l-81*l+720)/2;
        return (9*l*l-325*l+4440)/2;
    }
    private static int needed(int l) {return l>=30?112+(l-30)*9:l>=15?37+(l-15)*5:7+l*2;}
    private static boolean standard(Player p) {
        return p.experienceLevel>=0&&p.experienceLevel<=100000 && p.getXpNeededForNextLevel()==needed(p.experienceLevel);
    }
    public static double before(Player p) {
        if(!standard(p))return Double.NaN;
        var ledger=LEDGERS.computeIfAbsent(p,k->new Ledger());
        if(!ledger.matches(p))ledger.points=base(p.experienceLevel)+(double)p.experienceProgress*p.getXpNeededForNextLevel();
        ledger.remember(p);return ledger.points;
    }
    private static boolean matchesVanilla(Ledger before,Player p,long delta) {
        if(delta<=0 || delta>Integer.MAX_VALUE)return false;
        int level=before.level;
        float progress=Float.intBitsToFloat(before.progress);
        progress+=(float)delta/(float)needed(level);
        // Check the unmodified vanilla float result, rather than imposing an arbitrary
        // point-error tolerance. A single large award accumulates error across many levels.
        // Changed/canceled LevelChange hooks produce a different result and stay untouched.
        while(progress>=1.0F && level<100000) {
            progress=(progress-1.0F)*(float)needed(level);
            level++;
            progress/=(float)needed(level);
        }
        return level==p.experienceLevel && Float.floatToIntBits(progress)==Float.floatToIntBits(p.experienceProgress);
    }
    public static void after(Player p,double before,int oldTotal) {
        if(!Double.isFinite(before)||!standard(p))return;
        var ledger=LEDGERS.computeIfAbsent(p,k->new Ledger());
        long delta=(long)p.totalExperience-oldTotal;
        double ideal=before+delta;
        if(matchesVanilla(ledger,p,delta)) {
            if(ideal>=base(p.experienceLevel+1))p.giveExperienceLevels(1);
            else if(ideal<base(p.experienceLevel)&&p.experienceLevel>0)p.giveExperienceLevels(-1);
            double fraction=(ideal-base(p.experienceLevel))/p.getXpNeededForNextLevel();
            if(fraction>=0&&fraction<1) {
                p.experienceProgress=(float)fraction;ledger.points=ideal;ledger.remember(p);return;
            }
        }
        ledger.points=base(p.experienceLevel)+(double)p.experienceProgress*p.getXpNeededForNextLevel();
        ledger.remember(p);
    }
}
