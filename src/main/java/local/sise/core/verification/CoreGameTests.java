package local.sise.core.verification;

import com.mojang.serialization.MapCodec;
import java.util.*;
import java.util.function.Consumer;
import local.sise.core.SiseCore;
import local.sise.core.space.*;
import local.sise.core.performance.ZipDirectoryIndex;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class CoreGameTests {
    public static void register(RegisterGameTestsEvent event) {
        var environment=event.registerEnvironment(SiseCore.id("verification"));
        Map<String,Consumer<GameTestHelper>> tests=new LinkedHashMap<>();
        tests.put("content_and_recipe", h->{
            h.assertValueEqual(SiseCore.MATERIALS.size(),11,"material count");
            for(var item:SiseCore.MATERIALS){h.assertValueEqual(item.get().getDefaultInstance().getMaxStackSize(),64,"material stack size");
                h.assertTrue(h.getLevel().getServer().getResourceManager().getResource(SiseCore.id("items/"+item.getId().getPath()+".json")).isEmpty(),"assets are not server data");}
            h.assertValueEqual(SiseCore.TELEPORTER.get().getDefaultInstance().getMaxStackSize(),1,"teleporter stack size");
            h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,SiseCore.id("private_dimension_teleporter"))).isPresent(),"teleporter recipe loaded");
        });
        tests.put("binding_and_copy",h->{var p=player(h,"Owner");var q=player(h,"Visitor");var item=new ItemStack(SiseCore.TELEPORTER.get());
            h.assertValueEqual(SpaceTeleporterItem.bind(item,p),p.getUUID(),"initial owner");
            h.assertValueEqual(SpaceTeleporterItem.bind(item.copy(),q),p.getUUID(),"copy cannot steal ownership");
            var data=item.get(DataComponents.CUSTOM_DATA).copyTag();data.putString("space_owner","corrupt");item.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            h.assertFalse(SpaceTeleporterItem.bind(item,q).equals(q.getUUID()),"invalid binding cannot be claimed");});
        tests.put("ownership_and_routes_codec",h->{var data=new SpaceData();var id=UUID.randomUUID();var visitor=UUID.randomUUID();var s=new SpaceData.Space(id,"Owner",SpaceData.Terrain.VOID);s.members.add(visitor);s.access=SpaceData.Access.MEMBERS;s.entranceReady=true;data.spaces.put(id,s);
            data.push(visitor,new SpaceData.ReturnPoint(Level.OVERWORLD,3.5,64,8.5,90,12));
            var copy=SpaceData.CODEC.parse(NbtOps.INSTANCE,SpaceData.CODEC.encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
            h.assertTrue(copy.spaces.get(id).permits(visitor),"members survive reload");h.assertFalse(copy.spaces.get(id).permits(UUID.randomUUID()),"stranger denied");
            h.assertValueEqual(copy.peek(visitor),data.peek(visitor),"return location round trip");copy.pop(visitor);h.assertTrue(copy.peek(visitor)==null,"return consumed");
            var broken=data.encode();broken.putInt("schema",999);h.assertTrue(SpaceData.CODEC.parse(NbtOps.INSTANCE,broken).error().isPresent(),"unknown schema rejected");});
        tests.put("return_stack_bound",h->{var data=new SpaceData();var id=UUID.randomUUID();var point=new SpaceData.ReturnPoint(Level.OVERWORLD,0,64,0,0,0);for(int i=0;i<8;i++)data.push(id,point);boolean rejected=false;try{data.push(id,point);}catch(IllegalStateException expected){rejected=true;}h.assertTrue(rejected,"nested travel bound");h.assertValueEqual(data.returns.get(id).size(),8,"overflow preserves routes");});
        tests.put("gate_drop_and_save_binding",h->{var pos=h.absolutePos(new BlockPos(3,3,3));var level=h.getLevel();level.setBlock(pos,SiseCore.GATE.get().defaultBlockState(),3);var gate=(SpaceGateEntity)level.getBlockEntity(pos);var owner=UUID.randomUUID();gate.bind(owner,"Owner");
            var drops=net.minecraft.world.level.block.Block.getDrops(gate.getBlockState(),level,pos,gate);h.assertValueEqual(drops.size(),1,"one teleporter drop");h.assertValueEqual(SpaceTeleporterItem.owner(drops.getFirst()),owner,"drop keeps owner");
            var restored=new SpaceGateEntity(pos,gate.getBlockState());restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),gate.saveWithoutMetadata(level.registryAccess())));
            h.assertValueEqual(restored.owner,owner,"block entity ownership persists");
        });
        tests.put("menu_owner_and_dimension_guard",h->{var p=player(h,"Owner");var q=player(h,"Stranger");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SiseCore.TELEPORTER.get()));SpaceTeleporterItem.bind(p.getMainHandItem(),p);var menu=new SpaceMenu(1,p.getInventory(),p,null,InteractionHand.MAIN_HAND);
            h.assertFalse(menu.stillValid(q),"different player denied");menu.clicked(11,0,ContainerInput.PICKUP,q);h.assertFalse(SpaceData.get(p.level().getServer()).spaces.containsKey(p.getUUID()),"other player cannot trigger creation");
            var corrupt=new CompoundTag();corrupt.putInt("schema",1);h.assertTrue(SpaceData.CODEC.parse(NbtOps.INSTANCE,corrupt).error().isPresent(),"missing ownership list cannot become empty save");
        });
        for(var terrain:SpaceData.Terrain.values())tests.put("dimension_"+terrain.name().toLowerCase(Locale.ROOT),h->{
            var p=player(h,"Builder");var server=h.getLevel().getServer();var before=server.overworld().getRespawnData();
            var space=SpaceRuntime.create(p,terrain);var level=server.getLevel(space.key());
            h.assertTrue(level!=null,"dynamic dimension exists");h.assertTrue(level.getBlockEntity(new BlockPos(8,space.y,8)) instanceof SpaceGateEntity,"return gate");
            h.assertTrue(SpaceRuntime.safe(level,new Vec3(8.5,space.y,10.5),p)!=null,"safe spawn");
            var mark=new BlockPos(10,space.y,10);level.setBlock(mark,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            SpaceRuntime.initialize(server,space);h.assertTrue(level.getBlockState(mark).is(Blocks.GOLD_BLOCK),"re-entry preserves builds");
            h.assertValueEqual(SpaceRuntime.create(p,terrain==SpaceData.Terrain.VOID?SpaceData.Terrain.FLAT:SpaceData.Terrain.VOID),space,"terrain immutable");
            h.assertValueEqual(server.overworld().getRespawnData(),before,"overworld spawn unchanged");
            h.assertFalse(SpaceRuntime.permitted(server,space.key(),UUID.randomUUID()),"private dimension");
            if(terrain==SpaceData.Terrain.VOID)h.assertTrue(level.getBlockState(new BlockPos(24,64,24)).isAir(),"void outside platform");
            else h.assertTrue(level.getBlockState(new BlockPos(24,3,24)).is(Blocks.GRASS_BLOCK),"flat continues beyond platform");
            server.overworld().getDataStorage().saveAndJoin();
        });
        tests.put("menu_no_icon_duplication",h->{var p=player(h,"Menu");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SiseCore.TELEPORTER.get()));SpaceTeleporterItem.bind(p.getMainHandItem(),p);
            var menu=new SpaceMenu(1,p.getInventory(),p,null,InteractionHand.MAIN_HAND);
            for(var click:ContainerInput.values())if(click!=ContainerInput.PICKUP)for(int slot:new int[]{11,15,27,-999})menu.clicked(slot,0,click,p);
            h.assertTrue(menu.getCarried().isEmpty(),"icons not copied to cursor");h.assertTrue(menu.quickMoveStack(p,11).isEmpty(),"shift click blocked");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.assertFalse(menu.stillValid(p),"menu invalid after teleporter removed");
            menu.clicked(11,0,ContainerInput.PICKUP,p);h.assertFalse(SpaceData.get(p.level().getServer()).spaces.containsKey(p.getUUID()),"stale menu cannot create");});
        tests.put("zip_index_matches_enumeration",h->{
            try{var path=java.nio.file.Files.createTempFile("sise-pack-index-",".zip");
                try{try(var out=new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(path))){for(var name:List.of("assets/sise_core/models/a.json","assets/sise_core/models/nested/b.json","assets/sise_core/models2/c.json","overlay/assets/sise_core/models/d.json")){out.putNextEntry(new java.util.zip.ZipEntry(name));out.write(1);out.closeEntry();}}
                    try(var zip=new java.util.zip.ZipFile(path.toFile())){for(String prefix:List.of("assets/sise_core/models/","overlay/assets/sise_core/models/","missing/")){var expected=zip.stream().filter(e->!e.isDirectory()&&e.getName().startsWith(prefix)).map(java.util.zip.ZipEntry::getName).toList();var actual=Collections.list(ZipDirectoryIndex.entries(zip,prefix)).stream().map(java.util.zip.ZipEntry::getName).toList();h.assertValueEqual(actual,expected,"same order and contents");}}
                }finally{java.nio.file.Files.deleteIfExists(path);}
            }catch(java.io.IOException error){throw new RuntimeException(error);}
        });
        tests.forEach((name,test)->event.registerTest(SiseCore.id(name),new GameTestInstance(new TestData<>(environment,SiseCore.id("core_test"),200,0,true)){
            @Override public void run(GameTestHelper helper){test.accept(helper);helper.succeed();}
            @Override public MapCodec<? extends GameTestInstance> codec(){return FunctionGameTestInstance.CODEC;}
            @Override protected net.minecraft.network.chat.MutableComponent typeDescription(){return Component.literal("SISE Core verification");}
        }));
    }
    public static FakePlayer player(GameTestHelper h,String name){return new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),name));}
    private CoreGameTests(){}
}
