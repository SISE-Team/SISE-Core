package local.sise.core.xp;

public interface OrbAccess {
    int sise$orbCount();
    int sise$orbAge();
    int sise$orbHealth();
    void sise$orbAge(int age);
    void sise$orbValue(int value);
}
