package local.sise.core.verification;

import java.nio.file.*;
import java.util.*;
import local.sise.core.SiseCore;
import local.sise.core.space.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Explicit opt-in only, and refuses any game directory without its fixture marker. */
public final class CoreClientProbe {
    private static boolean started,finished,reopen;
    private static int stage,ticks,worldTicks;
    private static Path directory;
    private static final List<String> checks=new ArrayList<>();
    public static void register() {
        if(!Boolean.getBoolean("sise.core.verify"))return;
        NeoForge.EVENT_BUS.addListener(CoreClientProbe::tick);
    }
    private static void require(boolean value,String description){if(!value)throw new IllegalStateException(description);checks.add(description);}
    private static void tick(ClientTickEvent.Post event) {
        if(finished)return;var mc=Minecraft.getInstance();
        try {
            if(!started) {
                if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
                directory=mc.gameDirectory.toPath().toAbsolutePath().normalize();
                if(!directory.equals(Path.of(System.getProperty("sise.core.fixture","")).toAbsolutePath().normalize())||!Files.isRegularFile(directory.resolve("SISE_AUTOTEST_ONLY")))throw new IllegalStateException("Not a SISE fixture");
                started=true;reopen=Boolean.getBoolean("sise.core.reopen");
                if(reopen)mc.createWorldOpenFlows().openWorld("sise-verification",()->finish(false,"World reopen cancelled"));
                else mc.createWorldOpenFlows().createFreshLevel("sise-verification",new LevelSettings("SISE verification",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(602,false,false),WorldPresets::createFlatWorldDimensions,mc.screen);
                return;
            }
            if(++ticks>6000)throw new IllegalStateException("Client verification timed out");
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.player.tickCount<30)return;
            if(++worldTicks%80!=0)return;
            var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
            if(reopen) {
                if(stage==0){require(mc.level.dimension().equals(SpaceData.dimension(id)),"player reloads inside personal dimension");server.submit(()->{var s=SpaceData.get(server).spaces.get(id);require(s!=null&&s.entranceReady,"ownership persisted");require(server.getLevel(s.key()).getBlockState(new BlockPos(10,65,10)).is(Blocks.GOLD_BLOCK),"placed block persists across restart");require(SpaceData.get(server).peek(id)!=null,"return point survives restart");}).join();shot("04-restored-space");}
                else if(stage==1)mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
                else {require(mc.level.dimension().equals(Level.OVERWORLD),"return after restart");finish(true,"");}
                stage++;return;
            }
            switch(stage++) {
                case 0 -> {
                    // Render all ported items in a vanilla container, using actual item models.
                    server.submit(()->{var p=server.getPlayerList().getPlayer(id);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SiseCore.TELEPORTER.get()));
                        var inventory=new SimpleContainer(27);int i=0;for(var item:SiseCore.MATERIALS)inventory.setItem(i++,new ItemStack(item.get()));inventory.setItem(i,new ItemStack(SiseCore.TELEPORTER.get()));
                        p.openMenu(new SimpleMenuProvider((container,inv,player)->new ChestMenu(MenuType.GENERIC_9x3,container,inv,inventory,3),Component.literal("SISE Core · 26.1.2")));}).join();
                    for(var name:SiseCore.MATERIAL_IDS){require(mc.getResourceManager().getResource(SiseCore.id("items/"+name+".json")).isPresent(),"item definition "+name);require(mc.getResourceManager().getResource(SiseCore.id("textures/item/"+name+".png")).isPresent(),"texture "+name);}
                }
                case 1 -> {shot("01-items");server.submit(()->{var p=server.getPlayerList().getPlayer(id);p.closeContainer();SpaceRuntime.useHand(p,InteractionHand.MAIN_HAND);}).join();}
                case 2 -> {require(mc.player.containerMenu.getType()==MenuType.GENERIC_9x3,"configuration menu synced");shot("02-create-space");}
                case 3 -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,11,0,ContainerInput.PICKUP,mc.player);
                case 4 -> {require(mc.level.dimension().equals(SpaceData.dimension(id)),"real client enters newly created private dimension");
                    server.submit(()->{var p=server.getPlayerList().getPlayer(id);require(p.level().dimension().equals(SpaceData.dimension(id)),"server dimension matches client");p.level().setBlock(new BlockPos(10,65,10),Blocks.GOLD_BLOCK.defaultBlockState(),3);}).join();shot("03-private-space");}
                case 5 -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
                case 6 -> {require(mc.level.dimension().equals(Level.OVERWORLD),"real client returns to overworld");server.submit(()->require(SpaceData.get(server).peek(id)==null,"completed return consumes route")).join();}
                case 7 -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
                case 8 -> {require(mc.level.dimension().equals(SpaceData.dimension(id)),"second entry reuses same dimension");finish(true,"");}
                default -> throw new IllegalStateException("Unexpected stage");
            }
        }catch(Throwable error){finish(false,error.toString());}
    }
    private static void shot(String name) {
        var mc=Minecraft.getInstance();Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{try(image){var path=directory.resolve("screenshots/"+name+".png");Files.createDirectories(path.getParent());image.writeToFile(path);}catch(Exception e){finish(false,e.toString());}});
    }
    private static void finish(boolean passed,String error) {
        if(finished)return;finished=true;
        try{var report=Map.of("passed",passed,"checks",checks,"error",error,"reopen",reopen);Files.writeString(directory.resolve(reopen?"sise-reopen.json":"sise-client.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception ignored){}
        Minecraft.getInstance().stop();
    }
    private CoreClientProbe(){}
}
