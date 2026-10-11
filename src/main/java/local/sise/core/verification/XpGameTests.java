package local.sise.core.verification;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Consumer;
import local.sise.core.SiseCore;
import local.sise.core.xp.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.transfer.fluid.*;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Server behavior checks. Only registered by NeoForge when GameTest is enabled. */
public final class XpGameTests {
    public static void register(RegisterGameTestsEvent event) {
        var environment=event.registerEnvironment(SiseCore.id("xp_verification"));
        Map<String,Consumer<GameTestHelper>> tests=new LinkedHashMap<>();
        tests.put("xp_single_large_award",h->{
            ExperienceOrb.awardWithDirection(h.getLevel(),pos(h),new Vec3(1,0,0),1_000_000);
            var orbs=orbs(h);
            h.assertValueEqual(orbs.size(),1,"one entity for a million points");
            h.assertValueEqual(orbs.getFirst().getValue(),1_000_000,"full award retained");
            var p=CoreGameTests.player(h,"Bulk");
            orbs.getFirst().playerTouch(p);
            h.assertValueEqual(p.totalExperience,1_000_000,"full award picked up");
            h.assertTrue(orbs.getFirst().isRemoved(),"consumed only once");
            orbs.getFirst().playerTouch(p);
            h.assertValueEqual(p.totalExperience,1_000_000,"removed orb cannot pay twice");
        });
        tests.put("xp_spawn_merge_and_overflow",h->{
            var first=orb(h,1);
            h.getLevel().addFreshEntity(first);
            for(int i=1;i<100_000;i++)h.getLevel().addFreshEntity(orb(h,1));
            h.assertValueEqual(orbs(h).size(),1,"100000 small drops share one entity");
            h.assertValueEqual(first.getValue(),100_000,"no points lost on merge");
            ((OrbAccess)first).sise$orbValue(Integer.MAX_VALUE);
            h.getLevel().addFreshEntity(orb(h,1));
            h.assertValueEqual(orbs(h).size(),2,"overflow leaves a separate orb");
            h.assertValueEqual(first.getValue(),Integer.MAX_VALUE,"int value never wraps");
        });
        tests.put("xp_special_orbs_remain_separate",h->{
            h.getLevel().addFreshEntity(orb(h,7));
            var named=orb(h,9);named.setCustomName(Component.literal("Special"));h.getLevel().addFreshEntity(named);
            var data=orb(h,11);data.getPersistentData().putString("owner","test");h.getLevel().addFreshEntity(data);
            var tagged=orb(h,13);tagged.addTag("special");h.getLevel().addFreshEntity(tagged);
            var gravity=orb(h,15);gravity.setNoGravity(true);h.getLevel().addFreshEntity(gravity);
            h.assertValueEqual(orbs(h).size(),5,"custom metadata and physics protected");
            h.assertFalse(XpMerge.merge(named),"named orb excluded from periodic merge");
        });
        tests.put("xp_save_and_sync_full_int",h->{
            for(int points:new int[]{1,32767,32768,1_000_000,Integer.MAX_VALUE}) {
                var original=orb(h,points);
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
                h.assertTrue(original.save(output),"orb saved");
                var tag=output.buildResult();
                var restored=orb(h,0);
                restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
                h.assertValueEqual(restored.getValue(),points,"full int survives load");
                h.assertValueEqual(((OrbAccess)restored).sise$orbCount(),1,"one logical pickup after reload");
                if(points>32767)h.assertValueEqual(tag.getIntOr("Count",0),points,"vanilla fallback retains total");
                var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
                try {
                    var values=original.getEntityData().getNonDefaultValues();
                    var decoded=new ArrayList<SynchedEntityData.DataValue<?>>();
                    for(var value:values){value.write(buffer);decoded.add(SynchedEntityData.DataValue.read(buffer,buffer.readUnsignedByte()));}
                    var receiving=orb(h,0);receiving.getEntityData().assignValues(decoded);
                    h.assertValueEqual(receiving.getValue(),points,"vanilla entity codec retains full int");
                } finally {buffer.release();}
            }
        });
        tests.put("xp_precision_at_level_boundary",h->{
            var p=CoreGameTests.player(h,"Exact");
            orb(h,4_339_720).playerTouch(p);
            h.assertValueEqual(p.totalExperience,4_339_720,"exact total");
            h.assertValueEqual(p.experienceLevel,1000,"exact level boundary");
            h.assertValueEqual(p.experienceProgress,0.0F,"no float residue");
        });
        tests.put("xp_mending_retains_excess",h->{
            var p=CoreGameTests.player(h,"Mending");var item=mending(h);
            p.setItemSlot(EquipmentSlot.MAINHAND,item);
            orb(h,1_000_000).playerTouch(p);
            h.assertValueEqual(item.getDamageValue(),0,"damaged equipment repaired");
            h.assertValueEqual(p.totalExperience,999_400,"only repair cost consumed");
        });
        tests.put("xp_events_and_player_isolation",h->{
            var p=CoreGameTests.player(h,"Canceled");var q=CoreGameTests.player(h,"Receiver");var orb=orb(h,100);
            Consumer<PlayerXpEvent.PickupXp> cancel=e->{if(e.getEntity()==p)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancel);
            try {orb.playerTouch(p);h.assertValueEqual(p.totalExperience,0,"canceled pickup untouched");h.assertFalse(orb.isRemoved(),"canceled orb retained");}
            finally {NeoForge.EVENT_BUS.unregister(cancel);}
            orb.playerTouch(q);orb.playerTouch(p);
            h.assertValueEqual(q.totalExperience,100,"second player receives once");h.assertValueEqual(p.totalExperience,0,"no duplicate payment");
            Consumer<PlayerXpEvent.XpChange> modify=e->{if(e.getEntity()==p)e.setAmount(50);};
            NeoForge.EVENT_BUS.addListener(modify);
            try {orb(h,100).playerTouch(p);h.assertValueEqual(p.totalExperience,50,"XpChange amount respected");}
            finally {NeoForge.EVENT_BUS.unregister(modify);}
            Consumer<PlayerXpEvent.LevelChange> levels=e->{if(e.getEntity()==p)e.setCanceled(true);};
            int previousLevel=p.experienceLevel;
            NeoForge.EVENT_BUS.addListener(levels);
            try {p.giveExperiencePoints(100);h.assertValueEqual(p.experienceLevel,previousLevel,"canceled level changes respected");}
            finally {NeoForge.EVENT_BUS.unregister(levels);}
        });
        tests.put("xp_total_saturates_without_wrap",h->{
            var p=CoreGameTests.player(h,"Saturation");p.giveExperiencePoints(100);
            p.giveExperiencePoints(Integer.MAX_VALUE);
            h.assertValueEqual(p.totalExperience,Integer.MAX_VALUE,"large awards saturate instead of wrapping to zero");
        });
        tests.put("xp_fluid_accepts_whole_points",h->{
            var fluid=FluidResource.of(Fluids.WATER);
            var tank=new FluidStacksResourceHandler(1,39);
            h.assertValueEqual(XpFluidTransfer.insert(tank,fluid,1_000_000,20),1,"partial capacity accepts one whole point");
            h.assertValueEqual(tank.getAmountAsInt(0),20,"fractional point rolled back");
            h.assertValueEqual(XpFluidTransfer.insert(tank,fluid,10,20),0,"under one point does not consume experience");
            var large=new FluidStacksResourceHandler(1,Integer.MAX_VALUE);
            h.assertValueEqual(XpFluidTransfer.insert(large,fluid,Integer.MAX_VALUE,20),Integer.MAX_VALUE/20,"large conversion bounded");
            h.assertValueEqual(large.getAmountAsInt(0),Integer.MAX_VALUE/20*20,"fluid multiplication never overflows");
        });
        tests.put("xp_disabled_uses_vanilla_award",h->{
            boolean enabled=XpFlags.enabled;
            try {
                XpFlags.enabled=false;ExperienceOrb.award(h.getLevel(),pos(h),100);
                h.assertTrue(orbs(h).stream().allMatch(o->o.getValue()<100),"vanilla denominations restored");
                h.assertValueEqual(orbs(h).stream().mapToInt(o->o.getValue()*((OrbAccess)o).sise$orbCount()).sum(),100,"disabled path keeps total");
            } finally {XpFlags.enabled=enabled;}
        });
        if(ModList.get().isLoaded("curios"))tests.put("xp_curios_mending_large_int",XpGameTests::curios);
        if(ModList.get().isLoaded("enderio"))tests.put("xp_enderio_partial_capacity",XpGameTests::enderio);
        tests.forEach((name,test)->event.registerTest(SiseCore.id(name),new GameTestInstance(new TestData<>(environment,SiseCore.id("core_test"),400,0,true)){
            @Override public void run(GameTestHelper helper){test.accept(helper);helper.succeed();}
            @Override public MapCodec<? extends GameTestInstance> codec(){return FunctionGameTestInstance.CODEC;}
            @Override protected net.minecraft.network.chat.MutableComponent typeDescription(){return Component.literal("SISE XP verification");}
        }));
        event.registerTest(SiseCore.id("xp_collision_pickup_budget"),new GameTestInstance(new TestData<>(environment,SiseCore.id("core_test"),100,0,true)){
            @Override public void run(GameTestHelper h){
                var p=CoreGameTests.player(h,"Batch");p.setPos(pos(h));
                for(int i=0;i<40;i++){var orb=orb(h,1);orb.setCustomName(Component.literal("Separate"+i));h.getLevel().addFreshEntity(orb);}
                p.aiStep();h.assertValueEqual(p.totalExperience,32,"pickup budget caps first tick at 32");
                var far=orb(h,99);far.setPos(pos(h).add(4,0,0));h.getLevel().addFreshEntity(far);
                h.runAfterDelay(1,()->{p.aiStep();h.assertValueEqual(p.totalExperience,40,"budget renews next tick");h.assertFalse(far.isRemoved(),"pickup range unchanged");h.succeed();});
            }
            @Override public MapCodec<? extends GameTestInstance> codec(){return FunctionGameTestInstance.CODEC;}
            @Override protected net.minecraft.network.chat.MutableComponent typeDescription(){return Component.literal("SISE XP pickup budget");}
        });
    }
    private static Vec3 pos(GameTestHelper h){return h.absoluteVec(new Vec3(4.5,4,4.5));}
    private static ExperienceOrb orb(GameTestHelper h,int points){var orb=new ExperienceOrb(h.getLevel(),0,0,0,points);orb.setPos(pos(h));return orb;}
    private static List<ExperienceOrb> orbs(GameTestHelper h){return h.getLevel().getEntitiesOfClass(ExperienceOrb.class,AABB.ofSize(pos(h),4,4,4),Entity::isAlive);}
    private static ItemStack mending(GameTestHelper h){var item=new ItemStack(Items.DIAMOND_SWORD);item.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING),1);item.setDamageValue(1200);return item;}
    private static void curios(GameTestHelper h){
        try {
            var p=CoreGameTests.player(h,"Curios");var item=mending(h);var orb=orb(h,Integer.MAX_VALUE);
            var handlerClass=Class.forName("top.theillusivec4.curios.common.inventory.DynamicStackHandler");
            var handler=handlerClass.getConstructor(int.class,java.util.function.Function.class).newInstance(1,(java.util.function.Function<Integer,Object>)(slot->null));
            handlerClass.getMethod("setStackInSlot",int.class,ItemStack.class).invoke(handler,0,item);
            var inventory=Class.forName("top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");
            var method=Class.forName("top.theillusivec4.curios.common.CuriosCommonEvents").getDeclaredMethod("handleMending",Player.class,inventory,PlayerXpEvent.PickupXp.class);method.setAccessible(true);
            method.invoke(null,p,handler,new PlayerXpEvent.PickupXp(p,orb));
            h.assertValueEqual(item.getDamageValue(),0,"Curios repair multiplication does not overflow");
            h.assertValueEqual(p.totalExperience,Integer.MAX_VALUE-600,"Curios keeps remaining full experience");
            h.assertTrue(orb.isRemoved(),"Curios consumes only once");
        } catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private static void enderio(GameTestHelper h){
        try {
            var pos=h.absolutePos(new BlockPos(4,3,4));
            var block=BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("enderio","xp_vacuum"));
            h.getLevel().setBlock(pos,block.defaultBlockState(),3);
            var machine=h.getLevel().getBlockEntity(pos);
            var tank=h.getLevel().getCapability(Capabilities.Fluid.BLOCK,pos,machine.getBlockState(),machine,null);
            var fluid=FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.fromNamespaceAndPath("enderio","fluid_xp_juice_still")));
            try(var transaction=Transaction.openRoot()){tank.insert(fluid,Integer.MAX_VALUE-39,transaction);transaction.commit();}
            var orb=orb(h,1_000_000);
            machine.getClass().getMethod("handleEntity",ExperienceOrb.class).invoke(machine,orb);
            h.assertValueEqual(orb.getValue(),999_999,"EnderIO leaves excess in the original orb");
            h.assertFalse(orb.isRemoved(),"partial fill cannot discard excess");
            h.assertValueEqual(tank.getAmountAsInt(0),Integer.MAX_VALUE-19,"only whole points stored");
            var emptyPos=h.absolutePos(new BlockPos(6,3,4));
            h.getLevel().setBlock(emptyPos,block.defaultBlockState(),3);
            var emptyMachine=h.getLevel().getBlockEntity(emptyPos);
            var emptyTank=h.getLevel().getCapability(Capabilities.Fluid.BLOCK,emptyPos,emptyMachine.getBlockState(),emptyMachine,null);
            var complete=orb(h,10);
            var collect=emptyMachine.getClass().getMethod("handleEntity",ExperienceOrb.class);
            collect.invoke(emptyMachine,complete);collect.invoke(emptyMachine,complete);
            h.assertTrue(complete.isRemoved(),"full collection consumes orb");
            h.assertValueEqual(emptyTank.getAmountAsInt(0),200,"removed orb cannot fill twice");
            var large=orb(h,Integer.MAX_VALUE);collect.invoke(emptyMachine,large);
            h.assertValueEqual(large.getValue(),Integer.MAX_VALUE-(Integer.MAX_VALUE-200)/20,"maximum-value orb retains excess");
            h.assertValueEqual(emptyTank.getAmountAsInt(0),Integer.MAX_VALUE/20*20,"EnderIO fluid count never wraps");
        } catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private XpGameTests(){}
}
