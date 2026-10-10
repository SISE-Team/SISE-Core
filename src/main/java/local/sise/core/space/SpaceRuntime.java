package local.sise.core.space;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;
import local.sise.core.SiseCore;
import local.sise.core.mixin.SpaceServerAccess;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** All ownership, creation and travel decisions run on the authoritative server. */
public final class SpaceRuntime {
    private static final Map<UUID,Long> COOLDOWNS=new HashMap<>();
    public static void message(ServerPlayer p,String key,Object...args){p.sendOverlayMessage(Component.translatable(key,args));}
    public static void register(IEventBus bus) {
        bus.addListener((ServerStartedEvent e)->SpaceData.get(e.getServer()).spaces.values().forEach(s->load(e.getServer(),s)));
        bus.addListener((ServerStoppedEvent e)->COOLDOWNS.clear());
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e)->COOLDOWNS.remove(e.getEntity().getUUID()));
        bus.addListener((PlayerTickEvent.Post e)->{
            if(e.getEntity() instanceof ServerPlayer p&&p.tickCount%20==0&&!permitted(p.level().getServer(),p.level().dimension(),p.getUUID()))leave(p,true);
        });
        bus.addListener(EventPriority.HIGHEST,(PlayerInteractEvent.RightClickBlock e)->{
            if(e.getItemStack().is(SiseCore.TELEPORTER.get())&&!e.getEntity().isShiftKeyDown())e.setUseBlock(TriState.FALSE);
        });
        bus.addListener((net.neoforged.neoforge.event.level.block.BreakBlockEvent e)->{
            if(e.getLevel().getBlockEntity(e.getPos()) instanceof SpaceGateEntity gate&&gate.owner!=null&&!gate.owner.equals(e.getPlayer().getUUID())){e.setCanceled(true);e.setNotifyClient(true);}
        });
        bus.addListener(SpaceRuntime::commands);
    }
    public static boolean permitted(MinecraftServer server,ResourceKey<Level> key,UUID visitor) {
        UUID owner=SpaceData.owner(key);if(owner==null)return true;
        var space=SpaceData.get(server).spaces.get(owner);return space!=null&&space.permits(visitor);
    }
    public static ServerLevel load(MinecraftServer server,SpaceData.Space space) {
        var existing=server.getLevel(space.key());if(existing!=null)return existing;
        var type=server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE,SiseCore.id("private_space")));
        var biome=server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        var settings=new FlatLevelGeneratorSettings(Optional.of(HolderSet.empty()),biome,List.of());
        if(space.terrain==SpaceData.Terrain.FLAT)settings.getLayersInfo().addAll(List.of(new FlatLayerInfo(1,Blocks.BEDROCK),new FlatLayerInfo(2,Blocks.DIRT),new FlatLayerInfo(1,Blocks.GRASS_BLOCK)));
        settings.updateLayers();
        var levelData=new DerivedLevelData(server.getWorldData(),(ServerLevelData)server.overworld().getLevelData()) {
            @Override public LevelData.RespawnData getRespawnData(){return LevelData.RespawnData.of(space.key(),new BlockPos(space.x,space.y,space.z),180,0);}
            @Override public void setSpawn(LevelData.RespawnData ignored) {} // Never change the overworld spawn.
            @Override public boolean isInitialized(){return true;}
            @Override public String getLevelName(){return space.name+" / SISE";}
        };
        var level=new ServerLevel(server,net.minecraft.util.Util.backgroundExecutor(),((SpaceServerAccess)server).sise$storage(),levelData,space.key(),new LevelStem(type,new FlatLevelSource(settings)),false,space.owner.getMostSignificantBits(),List.of(),false);
        server.forgeGetWorldMap().put(space.key(),level);server.markWorldsDirty();NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }
    public static SpaceData.Space create(ServerPlayer player,SpaceData.Terrain terrain) {
        var server=player.level().getServer();var data=SpaceData.get(server);var existing=data.spaces.get(player.getUUID());
        if(existing!=null)return existing;
        var space=new SpaceData.Space(player.getUUID(),player.getGameProfile().name(),terrain);
        data.spaces.put(space.owner,space);data.setDirty();
        server.overworld().getDataStorage().saveAndJoin(); // Commit the terrain choice before creating chunks.
        initialize(server,space);return space;
    }
    public static ServerLevel initialize(MinecraftServer server,SpaceData.Space space) {
        var level=load(server,space);
        if(!space.entranceReady) {
            // Only a new descriptor may build the platform. Re-entry never replaces player builds.
            if(space.terrain==SpaceData.Terrain.VOID)for(int x=0;x<16;x++)for(int z=0;z<16;z++)level.setBlock(new BlockPos(x,64,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);
            var pos=new BlockPos(8,space.terrain==SpaceData.Terrain.VOID?65:4,8);
            level.setBlock(pos,SiseCore.GATE.get().defaultBlockState(),3);
            if(level.getBlockEntity(pos) instanceof SpaceGateEntity gate)gate.bind(space.owner,space.name);
            level.save(null,true,false);
            space.entranceReady=true;SpaceData.get(server).setDirty();server.overworld().getDataStorage().saveAndJoin();
        }
        return level;
    }
    public static void useHand(ServerPlayer p,InteractionHand hand) {
        if(!SpaceTeleporterItem.bind(p.getItemInHand(hand),p).equals(p.getUUID())){message(p,"space.sise_core.bound_elsewhere");return;}
        var space=SpaceData.get(p.level().getServer()).spaces.get(p.getUUID());
        if(p.isShiftKeyDown()||space==null){SpaceMenu.open(p,null,hand);return;}
        if(SpaceData.owner(p.level().dimension())!=null)leave(p,false);else enter(p,space);
    }
    public static void useGate(ServerPlayer p,SpaceGateEntity gate) {
        if(gate.owner==null){message(p,"space.sise_core.unbound");return;}
        var space=SpaceData.get(p.level().getServer()).spaces.get(gate.owner);
        if(p.isShiftKeyDown()||space==null){if(gate.owner.equals(p.getUUID()))SpaceMenu.open(p,gate.getBlockPos(),null);else message(p,"space.sise_core.denied");return;}
        if(p.level().dimension().equals(space.key()))leave(p,false);else enter(p,space);
    }
    private static boolean ready(ServerPlayer p){long now=p.level().getServer().overworld().getGameTime();if(now<COOLDOWNS.getOrDefault(p.getUUID(),0L))return false;COOLDOWNS.put(p.getUUID(),now+20);return true;}
    public static boolean enter(ServerPlayer p,SpaceData.Space space) {
        var server=p.level().getServer();
        if(!space.permits(p.getUUID())){message(p,"space.sise_core.denied");return false;}
        if(p.isPassenger()||p.isVehicle()){message(p,"space.sise_core.dismount");return false;}
        if(p.level().dimension().equals(space.key())||!ready(p))return false;
        var target=initialize(server,space);var pos=safe(target,new Vec3(space.x+.5,space.y,space.z+.5),p);
        if(pos==null){message(p,"space.sise_core.blocked");return false;}
        var data=SpaceData.get(server);
        if(data.returns.getOrDefault(p.getUUID(),List.of()).size()>=8){message(p,"space.sise_core.return_first");return false;}
        data.push(p.getUUID(),SpaceData.ReturnPoint.at(p));server.overworld().getDataStorage().saveAndJoin();
        p.closeContainer();boolean moved=p.teleportTo(target,pos.x,pos.y,pos.z,Set.of(),180,0,true);
        if(!moved)data.pop(p.getUUID());else{p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);}
        return moved;
    }
    public static boolean leave(ServerPlayer p,boolean forced) {
        if(SpaceData.owner(p.level().dimension())==null)return false;
        if(!forced&&!ready(p))return false;
        var server=p.level().getServer();var data=SpaceData.get(server);var route=data.peek(p.getUUID());
        ServerLevel target=null;Vec3 pos=null;float yaw=180,pitch=0;
        if(route!=null&&permitted(server,route.dimension(),p.getUUID())) {
            target=server.getLevel(route.dimension());if(target!=null)pos=safe(target,new Vec3(route.x(),route.y(),route.z()),p);yaw=route.yaw();pitch=route.pitch();
        }
        if(pos==null){target=server.overworld();pos=safe(target,target.getRespawnData().pos().getBottomCenter(),p);}
        if(pos==null){message(p,"space.sise_core.blocked_return");return false;}
        p.closeContainer();boolean moved=p.teleportTo(target,pos.x,pos.y,pos.z,Set.of(),yaw,pitch,true);
        if(moved){data.pop(p.getUUID());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);server.overworld().getDataStorage().saveAndJoin();}
        return moved;
    }
    public static Vec3 safe(ServerLevel level,Vec3 wanted,ServerPlayer player) {
        var base=BlockPos.containing(wanted);
        for(int r=0;r<=4;r++)for(int dy=0;dy<=8;dy++)for(int sign:dy==0?new int[]{1}:new int[]{1,-1})for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;var at=base.offset(dx,dy*sign,dz);
            if(at.getY()<level.getMinY()+1||at.getY()>level.getMaxY()-2||!level.getWorldBorder().isWithinBounds(at))continue;
            level.getChunk(at);
            if(!level.getFluidState(at).isEmpty()||!level.getFluidState(at.above()).isEmpty()||!level.getFluidState(at.below()).isEmpty())continue;
            var floor=level.getBlockState(at.below());if(!floor.isFaceSturdy(level,at.below(),Direction.UP)||floor.is(Blocks.MAGMA_BLOCK)||floor.is(Blocks.CACTUS)||floor.is(Blocks.CAMPFIRE)||floor.is(Blocks.SOUL_CAMPFIRE))continue;
            var pos=r==0&&dy==0?wanted:at.getBottomCenter();
            if(level.noCollision(player,player.getDimensions(player.getPose()).makeBoundingBox(pos)))return pos;
        }
        return null;
    }
    public static void commands(RegisterCommandsEvent event) {
        var root=Commands.literal("sise").then(Commands.literal("space")
            .then(Commands.literal("leave").executes(c->leave(c.getSource().getPlayerOrException(),false)?1:0))
            .then(Commands.literal("settings").executes(c->{var p=c.getSource().getPlayerOrException();for(var hand:InteractionHand.values())if(p.getItemInHand(hand).is(SiseCore.TELEPORTER.get())&&SpaceTeleporterItem.bind(p.getItemInHand(hand),p).equals(p.getUUID())){SpaceMenu.open(p,null,hand);return 1;}if(p.level().dimension().equals(SpaceData.dimension(p.getUUID()))){SpaceMenu.open(p,null,null);return 1;}message(p,"space.sise_core.hold");return 0;}))
            .then(Commands.literal("set_spawn").executes(c->{var p=c.getSource().getPlayerOrException();var data=SpaceData.get(p.level().getServer());var s=data.spaces.get(p.getUUID());if(s==null||!p.level().dimension().equals(s.key()))return 0;var safe=safe(p.level(),p.position(),p);if(safe==null)return 0;s.x=BlockPos.containing(safe).getX();s.y=BlockPos.containing(safe).getY();s.z=BlockPos.containing(safe).getZ();data.setDirty();message(p,"space.sise_core.spawn_set");return 1;}))
            .then(Commands.literal("invite").then(Commands.argument("player",EntityArgument.player()).executes(c->{var p=c.getSource().getPlayerOrException();var s=SpaceData.get(p.level().getServer()).spaces.get(p.getUUID());if(s==null)return 0;s.members.add(EntityArgument.getPlayer(c,"player").getUUID());s.access=SpaceData.Access.MEMBERS;SpaceData.get(p.level().getServer()).setDirty();message(p,"space.sise_core.invited");return 1;})))
            .then(Commands.literal("revoke").then(Commands.argument("uuid",StringArgumentType.word()).executes(c->{var p=c.getSource().getPlayerOrException();var s=SpaceData.get(p.level().getServer()).spaces.get(p.getUUID());if(s==null)return 0;try{s.members.remove(UUID.fromString(StringArgumentType.getString(c,"uuid")));}catch(IllegalArgumentException bad){return 0;}SpaceData.get(p.level().getServer()).setDirty();return 1;}))));
        event.getDispatcher().register(root);
    }
    private SpaceRuntime(){}
}
