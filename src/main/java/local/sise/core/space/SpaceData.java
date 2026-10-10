package local.sise.core.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.*;
import local.sise.core.SiseCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.*;

/** Server-owned descriptors, independent of item copies, names and entrance locations. */
public final class SpaceData extends SavedData {
    public enum Terrain { VOID, FLAT }
    public enum Access { PRIVATE, MEMBERS, PUBLIC }
    public static final class Space {
        public final UUID owner;
        public String name;
        public final Terrain terrain;
        public Access access = Access.PRIVATE;
        public final Set<UUID> members = new HashSet<>();
        public boolean entranceReady;
        public int x = 8, y, z = 10;
        public Space(UUID owner, String name, Terrain terrain) {
            this.owner=owner; this.name=name; this.terrain=terrain; y=terrain==Terrain.VOID?65:4;
        }
        public ResourceKey<Level> key() { return dimension(owner); }
        public boolean permits(UUID visitor) { return owner.equals(visitor) || access==Access.PUBLIC || access==Access.MEMBERS && members.contains(visitor); }
    }
    public record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        public ReturnPoint {
            if (!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Float.isFinite(yaw)||!Float.isFinite(pitch)
                ||Math.abs(x)>29999984||Math.abs(z)>29999984) throw new IllegalArgumentException("Invalid return location");
        }
        public static ReturnPoint at(ServerPlayer p) { return new ReturnPoint(p.level().dimension(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()); }
    }
    public final Map<UUID,Space> spaces = new LinkedHashMap<>();
    public final Map<UUID,List<ReturnPoint>> returns = new HashMap<>();
    public static final Codec<SpaceData> CODEC = CompoundTag.CODEC.comapFlatMap(tag -> {
        try { return DataResult.success(decode(tag)); } catch (RuntimeException error) { return DataResult.error(() -> "Invalid SISE space index: "+error.getMessage()); }
    }, SpaceData::encode);
    public static final SavedDataType<SpaceData> TYPE = new SavedDataType<>(SiseCore.id("private_spaces"), level -> {
        // Never silently replace an unreadable ownership index with an empty one.
        if (level!=null && java.nio.file.Files.exists(level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("data/sise_core/private_spaces.dat"))) throw new IllegalStateException("Cannot read SISE private space ownership data");
        return new SpaceData();
    }, level -> CODEC);
    public static SpaceData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public static ResourceKey<Level> dimension(UUID owner) { return ResourceKey.create(Registries.DIMENSION,SiseCore.id("personal_"+owner.toString().replace("-",""))); }
    public static UUID owner(ResourceKey<Level> key) {
        var id=key.identifier(); String p=id.getPath();
        if(!id.getNamespace().equals(SiseCore.ID)||!p.matches("personal_[0-9a-f]{32}"))return null;
        String s=p.substring(9);return UUID.fromString(s.substring(0,8)+"-"+s.substring(8,12)+"-"+s.substring(12,16)+"-"+s.substring(16,20)+"-"+s.substring(20));
    }
    public void push(UUID player, ReturnPoint point) {
        var stack=returns.computeIfAbsent(player, k->new ArrayList<>());
        if(stack.size()>=8)throw new IllegalStateException("Return stack is full");
        stack.add(point);setDirty();
    }
    public ReturnPoint peek(UUID player) { var stack=returns.get(player);return stack==null||stack.isEmpty()?null:stack.getLast(); }
    public void pop(UUID player) { var stack=returns.get(player);if(stack!=null&&!stack.isEmpty()){stack.removeLast();if(stack.isEmpty())returns.remove(player);setDirty();} }
    public CompoundTag encode() {
        var root=new CompoundTag();root.putInt("schema",1);var all=new ListTag();
        for(var s:spaces.values()) {
            var n=new CompoundTag();n.putString("owner",s.owner.toString());n.putString("name",s.name);n.putString("terrain",s.terrain.name());
            n.putString("access",s.access.name());n.putBoolean("ready",s.entranceReady);n.putInt("x",s.x);n.putInt("y",s.y);n.putInt("z",s.z);
            var members=new ListTag();s.members.stream().sorted().forEach(id->members.add(StringTag.valueOf(id.toString())));n.put("members",members);all.add(n);
        }
        root.put("spaces",all);var routes=new ListTag();
        returns.forEach((id,points)->{var n=new CompoundTag();n.putString("player",id.toString());var stack=new ListTag();
            for(var p:points){var v=new CompoundTag();v.putString("dimension",p.dimension.identifier().toString());v.putDouble("x",p.x);v.putDouble("y",p.y);v.putDouble("z",p.z);v.putFloat("yaw",p.yaw);v.putFloat("pitch",p.pitch);stack.add(v);}
            n.put("stack",stack);routes.add(n);});root.put("returns",routes);return root;
    }
    public static SpaceData decode(CompoundTag root) {
        if(root.getIntOr("schema",0)!=1)throw new IllegalArgumentException("schema");
        var data=new SpaceData();
        for(var tag:root.getList("spaces").orElseThrow()) {
            var n=(CompoundTag)tag;var id=UUID.fromString(n.getString("owner").orElseThrow());
            var s=new Space(id,n.getString("name").orElseThrow(),Terrain.valueOf(n.getString("terrain").orElseThrow()));
            s.access=Access.valueOf(n.getString("access").orElseThrow());s.entranceReady=n.getBooleanOr("ready",false);
            s.x=n.getInt("x").orElseThrow();s.y=n.getInt("y").orElseThrow();s.z=n.getInt("z").orElseThrow();
            if(Math.abs((long)s.x)>29999984||Math.abs((long)s.z)>29999984||s.y<1||s.y>254)throw new IllegalArgumentException("spawn");
            for(var m:n.getListOrEmpty("members"))s.members.add(UUID.fromString(m.asString().orElseThrow()));
            if(data.spaces.putIfAbsent(id,s)!=null)throw new IllegalArgumentException("duplicate owner");
        }
        for(var tag:root.getList("returns").orElseThrow()) {
            var n=(CompoundTag)tag;var points=new ArrayList<ReturnPoint>();
            for(var entry:n.getListOrEmpty("stack")){var v=(CompoundTag)entry;points.add(new ReturnPoint(ResourceKey.create(Registries.DIMENSION,Identifier.parse(v.getString("dimension").orElseThrow())),v.getDouble("x").orElseThrow(),v.getDouble("y").orElseThrow(),v.getDouble("z").orElseThrow(),v.getFloat("yaw").orElseThrow(),v.getFloat("pitch").orElseThrow()));}
            if(points.size()>8)throw new IllegalArgumentException("return stack");
            if(data.returns.putIfAbsent(UUID.fromString(n.getString("player").orElseThrow()),points)!=null)throw new IllegalArgumentException("duplicate return route");
        }
        return data;
    }
}
