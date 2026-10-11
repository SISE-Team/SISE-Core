package local.sise.core;

import local.sise.core.space.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import java.util.List;

@Mod(SiseCore.ID)
public final class SiseCore {
    public static final String ID = "sise_core";
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    public static final List<String> MATERIAL_IDS = List.of("lesser_technology_core", "basic_technology_core",
        "intermediate_technology_core", "advanced_technology_core", "terminal_hybrid_orb", "quantum_hybrid_orb",
        "paradox_hybrid_orb", "infinite_sanity_unit", "rare_mystic_crystal", "twilight_pioneer", "gun_soul_smithing_template");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final List<DeferredItem<Item>> MATERIALS = MATERIAL_IDS.stream().map(name -> {
        return ITEMS.registerSimpleItem(name);
    }).toList();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredBlock<SpaceGateBlock> GATE = BLOCKS.registerBlock("private_dimension_teleporter", SpaceGateBlock::new,
        props -> props.strength(3, 1200).lightLevel(s -> 7).noOcclusion());
    public static final DeferredItem<SpaceTeleporterItem> TELEPORTER = ITEMS.registerItem("private_dimension_teleporter",
        props -> new SpaceTeleporterItem(GATE.get(), props), props -> props.stacksTo(1));
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpaceGateEntity>> GATE_ENTITY = ENTITIES.register(
        "private_dimension_teleporter", () -> new BlockEntityType<>(SpaceGateEntity::new, GATE.get()));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    static {
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.sise_core"))
            .icon(() -> MATERIALS.get(3).get().getDefaultInstance())
            .displayItems((parameters, output) -> { MATERIALS.forEach(output::accept); output.accept(TELEPORTER); }).build());
    }
    public SiseCore(IEventBus bus) {
        ITEMS.register(bus); BLOCKS.register(bus); ENTITIES.register(bus); TABS.register(bus);
        SpaceRuntime.register(NeoForge.EVENT_BUS);
        local.sise.core.xp.XpMerge.register();
        bus.addListener(local.sise.core.localization.CoreLocalization::addPack);
        bus.addListener(local.sise.core.verification.CoreGameTests::register);
        bus.addListener(local.sise.core.verification.XpGameTests::register);
        if(net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            local.sise.core.verification.CoreClientProbe.register();
            local.sise.core.verification.LocalizationClientProbe.register();
        }
    }
}
