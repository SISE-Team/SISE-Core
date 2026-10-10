package local.sise.core.space;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class SpaceTeleporterItem extends BlockItem {
    public SpaceTeleporterItem(Block block,Properties properties){super(block,properties);}
    public static UUID owner(ItemStack stack) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        String id=data.getStringOr("space_owner","");
        if(id.isEmpty())return null;
        try{return UUID.fromString(id);}catch(IllegalArgumentException bad){return new UUID(0,0);}
    }
    public static UUID bind(ItemStack stack,ServerPlayer player) {
        UUID owner=owner(stack);
        if(owner==null){owner=player.getUUID();var n=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            n.putString("space_owner",owner.toString());n.putString("space_owner_name",player.getGameProfile().name());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(n));}
        return owner;
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer p)SpaceRuntime.useHand(p,hand);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();if(player==null)return InteractionResult.FAIL;
        if(!player.isShiftKeyDown()){if(player instanceof ServerPlayer p)SpaceRuntime.useHand(p,context.getHand());return InteractionResult.SUCCESS;}
        if(player instanceof ServerPlayer p&&!bind(context.getItemInHand(),p).equals(p.getUUID())){SpaceRuntime.message(p,"space.sise_core.bound_elsewhere");return InteractionResult.FAIL;}
        return super.useOn(context);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag) {
        String name=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getStringOr("space_owner_name","");
        if(!name.isEmpty())lines.accept(Component.translatable("space.sise_core.owner",name).withStyle(ChatFormatting.AQUA));
        for(String key:new String[]{"use","place","settings_hint"})lines.accept(Component.translatable("space.sise_core."+key).withStyle(ChatFormatting.GRAY));
    }
}
