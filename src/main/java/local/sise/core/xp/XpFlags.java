package local.sise.core.xp;

public final class XpFlags {
    public static volatile boolean enabled=!Boolean.getBoolean("sise.core.xp.disabled");
    // Vanilla synchronizes an int; Core also preserves it through the short-valued save format.
    public static final int MAX_VALUE=Integer.MAX_VALUE;
    public static final int PICKUPS_PER_TICK=32;
    private XpFlags() {}
}
