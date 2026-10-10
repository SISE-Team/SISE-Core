package local.sise.core.space;
import java.util.UUID;
import local.sise.core.SiseCore;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class SpaceGateEntity extends BlockEntity {
    public UUID owner;
    public String ownerName="";
    public SpaceGateEntity(BlockPos pos,BlockState state){super(SiseCore.GATE_ENTITY.get(),pos,state);}
    public void bind(UUID id,String name){owner=id;ownerName=name;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    private CompoundTag binding(){var n=new CompoundTag();if(owner!=null)n.putString("space_owner",owner.toString());n.putString("space_owner_name",ownerName);return n;}
    private void read(String id,String name){owner=id.isEmpty()?null:UUID.fromString(id);ownerName=name;}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(owner!=null)out.putString("space_owner",owner.toString());out.putString("space_owner_name",ownerName);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);read(in.getStringOr("space_owner",""),in.getStringOr("space_owner_name",""));}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder builder){super.collectImplicitComponents(builder);builder.set(DataComponents.CUSTOM_DATA,CustomData.of(binding()));}
    @Override protected void applyImplicitComponents(DataComponentGetter input){super.applyImplicitComponents(input);var data=input.get(DataComponents.CUSTOM_DATA);if(data!=null){var n=data.copyTag();read(n.getStringOr("space_owner",""),n.getStringOr("space_owner_name",""));}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return binding();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
