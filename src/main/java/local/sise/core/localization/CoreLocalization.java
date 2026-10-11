package local.sise.core.localization;

import local.sise.core.SiseCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/** Ships the pack inside Core and enables it on every client resource reload. */
public final class CoreLocalization {
    public static final String PACK_ID = "mod/sise_core:localization";

    public static void addPack(AddPackFindersEvent event) {
        event.addPackFinders(SiseCore.id("localization"), PackType.CLIENT_RESOURCES,
            Component.translatable("pack.sise_core.localization"), PackSource.BUILT_IN,
            true, Pack.Position.TOP);
    }

    private CoreLocalization() {}
}
