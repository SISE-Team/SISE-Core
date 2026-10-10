package local.sise.core.space;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class SpaceGateBlock extends BaseEntityBlock {
    public static final MapCodec<SpaceGateBlock> CODEC=simpleCodec(SpaceGateBlock::new);
    private static final VoxelShape SHAPE=Shapes.or(Block.box(0,0,0,16,4,16),Block.box(4,4,4,12,14,12));
    public SpaceGateBlock(BlockBehaviour.Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SpaceGateEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer p&&level.getBlockEntity(pos) instanceof SpaceGateEntity gate)SpaceRuntime.useGate(p,gate);
        return InteractionResult.SUCCESS;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        if(placer instanceof ServerPlayer p&&level.getBlockEntity(pos) instanceof SpaceGateEntity gate)gate.bind(p.getUUID(),p.getGameProfile().name());
    }
}
