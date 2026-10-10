package local.sise.core.space;

import local.sise.core.SiseCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Vanilla chest protocol: clients cannot supply an owner, dimension or arbitrary terrain. */
public final class SpaceMenu extends ChestMenu {
    private final ServerPlayer owner;
    private final BlockPos gate;
    private final InteractionHand hand;
    private final ResourceKey<Level> dimension;
    private final SimpleContainer buttons;
    public static void open(ServerPlayer p,BlockPos gate,InteractionHand hand) {
        p.openMenu(new SimpleMenuProvider((id,inv,ignored)->new SpaceMenu(id,inv,p,gate,hand),Component.translatable("space.sise_core.title")));
    }
    public SpaceMenu(int id,Inventory inventory,ServerPlayer owner,BlockPos gate,InteractionHand hand) {
        this(id,inventory,owner,gate,hand,new SimpleContainer(27));
    }
    private SpaceMenu(int id,Inventory inventory,ServerPlayer owner,BlockPos gate,InteractionHand hand,SimpleContainer buttons) {
        super(MenuType.GENERIC_9x3,id,inventory,buttons,3);this.owner=owner;this.gate=gate==null?null:gate.immutable();this.hand=hand;this.dimension=owner.level().dimension();this.buttons=buttons;
        refresh();
    }
    private void button(int slot,Item item,String key) {var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.translatable(key));buttons.setItem(slot,stack);}
    private void refresh() {
        buttons.clearContent();var s=SpaceData.get(owner.level().getServer()).spaces.get(owner.getUUID());
        if(s==null){button(11,Items.GLASS,"space.sise_core.create_void");button(15,Items.GRASS_BLOCK,"space.sise_core.create_flat");}
        else {button(10,SiseCore.TELEPORTER.get(),"space.sise_core.enter");button(12,Items.IRON_DOOR,"space.sise_core.access_"+s.access.name().toLowerCase(java.util.Locale.ROOT));button(14,Items.OAK_DOOR,"space.sise_core.leave");button(16,Items.PAPER,"space.sise_core.members_hint");}
    }
    @Override public boolean stillValid(Player player) {
        if(player!=owner||!dimension.equals(player.level().dimension()))return false;
        if(gate!=null)return player.distanceToSqr(gate.getCenter())<=64&&player.level().getBlockEntity(gate) instanceof SpaceGateEntity block&&player.getUUID().equals(block.owner);
        if(hand!=null)return player.getItemInHand(hand).is(SiseCore.TELEPORTER.get())&&player.getUUID().equals(SpaceTeleporterItem.owner(player.getItemInHand(hand)));
        return player.level().dimension().equals(SpaceData.dimension(player.getUUID()));
    }
    @Override public void clicked(int slot,int mouseButton,ContainerInput type,Player player) {
        // This is an action menu. Suppress every inventory mutation, including hotbar swaps,
        // quick-craft, clone, throw and pickup-all; icons must never become real items.
        if(!stillValid(player)||type!=ContainerInput.PICKUP||mouseButton!=0||!getCarried().isEmpty())return;
        var data=SpaceData.get(owner.level().getServer());var space=data.spaces.get(owner.getUUID());
        if(space==null&&(slot==11||slot==15)){space=SpaceRuntime.create(owner,slot==11?SpaceData.Terrain.VOID:SpaceData.Terrain.FLAT);SpaceRuntime.enter(owner,space);return;}
        if(space==null)return;
        if(slot==10)SpaceRuntime.enter(owner,space);
        else if(slot==14)SpaceRuntime.leave(owner,false);
        else if(slot==12){space.access=SpaceData.Access.values()[(space.access.ordinal()+1)%3];data.setDirty();refresh();broadcastChanges();}
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
