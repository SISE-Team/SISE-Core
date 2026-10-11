package local.sise.core.verification;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import local.sise.core.localization.CoreLocalization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.config.ModConfigs;

/** Opt-in probe restricted to marked disposable fixtures, never normal play. */
public final class LocalizationClientProbe {
    private static boolean started;
    private static Path directory;

    public static void register() {
        if (Boolean.getBoolean("sise.localization.verify"))
            NeoForge.EVENT_BUS.addListener(LocalizationClientProbe::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (started || !(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
        started = true;
        try {
            directory = mc.gameDirectory.toPath().toAbsolutePath().normalize();
            if (!directory.equals(Path.of(System.getProperty("sise.localization.fixture", "")).toAbsolutePath().normalize())
                || !Files.isRegularFile(directory.resolve("SISE_AUTOTEST_ONLY")))
                throw new IllegalStateException("Not a localization fixture");
            verify("startup");
            // No resource pack selected by the player: Core must stay enabled by itself.
            mc.getResourcePackRepository().setSelected(List.of());
            mc.reloadResourcePacks().thenRunAsync(() -> {
                try { verify("reload"); finish(true, ""); }
                catch (Throwable error) { finish(false, error.toString()); }
            }, mc);
        } catch (Throwable error) { finish(false, error.toString()); }
    }

    private static void verify(String stage) throws Exception {
        var mc = Minecraft.getInstance();
        var packs = mc.getResourcePackRepository().getSelectedPacks();
        var pack = packs.stream().filter(p -> p.getId().equals(CoreLocalization.PACK_ID)).findFirst().orElseThrow();
        if (!pack.isRequired() || !pack.getCompatibility().isCompatible())
            throw new IllegalStateException("Core localization pack is not required and compatible");
        if (!mc.getLanguageManager().getSelected().equals("zh_cn"))
            throw new IllegalStateException("Fixture language is not zh_cn");
        Map<String, String> expected = new TreeMap<>();
        try (var stream = LocalizationClientProbe.class.getResourceAsStream("/META-INF/sise-localization/namespaces.json")) {
            var namespaces = JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)).getAsJsonArray();
            for (var namespace : namespaces) {
                String path = "/localization/assets/" + namespace.getAsString() + "/lang/zh_cn.json";
                try (var lang = LocalizationClientProbe.class.getResourceAsStream(path)) {
                    Language.loadFromJson(Objects.requireNonNull(lang), expected::put);
                }
            }
        }
        Map<String, Object> mismatches = new TreeMap<>();
        expected.forEach((key, value) -> {
            String actual = Language.getInstance().getOrDefault(key);
            if (!value.equals(actual)) mismatches.put(key, Map.of("expected", value, "actual", actual));
        });
        Map<String, String> missingNames = new TreeMap<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            String key = item.getDescriptionId();
            if (!Language.getInstance().has(key)) missingNames.put(BuiltInRegistries.ITEM.getKey(item).toString(), key);
        });
        var report = new LinkedHashMap<String, Object>();
        report.put("pack", pack.getId()); report.put("required", pack.isRequired());
        report.put("compatible", pack.getCompatibility().isCompatible());
        report.put("selected", mc.getResourcePackRepository().getSelectedIds());
        report.put("language", mc.getLanguageManager().getSelected());
        report.put("checkedTranslations", expected.size()); report.put("mismatches", mismatches);
        report.put("missingItemNames", missingNames);
        Map<String, String> missingBlocks = new TreeMap<>();
        BuiltInRegistries.BLOCK.forEach(block -> {
            String key = block.getDescriptionId();
            if (!Language.getInstance().has(key)) missingBlocks.put(BuiltInRegistries.BLOCK.getKey(block).toString(), key);
        });
        report.put("missingBlockNames", missingBlocks);
        Map<String, String> missingEntities = new TreeMap<>();
        BuiltInRegistries.ENTITY_TYPE.forEach(entity -> {
            String key = entity.getDescriptionId();
            if (!Language.getInstance().has(key)) missingEntities.put(BuiltInRegistries.ENTITY_TYPE.getKey(entity).toString(), key);
        });
        report.put("missingEntityNames", missingEntities);
        Map<String, String> missingEffects = new TreeMap<>();
        BuiltInRegistries.MOB_EFFECT.forEach(effect -> {
            String key = effect.getDescriptionId();
            if (!Language.getInstance().has(key)) missingEffects.put(BuiltInRegistries.MOB_EFFECT.getKey(effect).toString(), key);
        });
        report.put("missingEffectNames", missingEffects);
        Map<String, String> missingAttributes = new TreeMap<>();
        BuiltInRegistries.ATTRIBUTE.forEach(attribute -> {
            String key = attribute.getDescriptionId();
            if (!Language.getInstance().has(key)) missingAttributes.put(BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString(), key);
        });
        report.put("missingAttributeNames", missingAttributes);
        Map<String, Map<String, String>> missingConfigText = new TreeMap<>();
        for (var config : ModConfigs.getFileMap().values()) {
            if (config.getSpec() instanceof ModConfigSpec spec)
                collectConfigText(config.getModId(), spec, spec.getSpec(), List.of(), missingConfigText);
        }
        report.put("missingConfigText", missingConfigText);
        Files.writeString(directory.resolve("localization-" + stage + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        if (!mismatches.isEmpty()) throw new IllegalStateException(mismatches.size() + " language values did not load");
        if (!missingNames.isEmpty() || !missingBlocks.isEmpty() || !missingEntities.isEmpty()
            || !missingEffects.isEmpty() || !missingAttributes.isEmpty() || !missingConfigText.isEmpty())
            throw new IllegalStateException("Missing registered names or declared configuration texts");
    }

    private static void collectConfigText(String modId, ModConfigSpec spec, UnmodifiableConfig node,
                                          List<String> parent, Map<String, Map<String, String>> output) {
        node.valueMap().forEach((name, value) -> {
            var path = new ArrayList<>(parent); path.add(name);
            String key; String comment;
            if (value instanceof ModConfigSpec.ValueSpec option) {
                key = option.getTranslationKey(); comment = option.getComment();
            } else {
                key = spec.getLevelTranslationKey(path); comment = spec.getLevelComment(path);
            }
            // Configured uses only declared translation keys and otherwise renders
            // literal labels/comments. Do not manufacture unused resource-pack keys.
            if (key == null) {
                if (value instanceof UnmodifiableConfig section) collectConfigText(modId, spec, section, path, output);
                return;
            }
            if (!Language.getInstance().has(key)) {
                String fallback = name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ');
                output.put(key, Map.of("namespace", modId, "english", fallback, "kind", "label"));
            }
            if (comment != null && !comment.isBlank() && !Language.getInstance().has(key + ".tooltip"))
                output.put(key + ".tooltip", Map.of("namespace", modId, "english", comment, "kind", "tooltip"));
            if (value instanceof UnmodifiableConfig section) collectConfigText(modId, spec, section, path, output);
        });
    }

    private static void finish(boolean passed, String error) {
        try {
            if (directory != null && Files.isRegularFile(directory.resolve("SISE_AUTOTEST_ONLY")))
                Files.writeString(directory.resolve("localization-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed", passed, "error", error)));
        } catch (Exception ignored) {}
        Minecraft.getInstance().stop();
    }

    private LocalizationClientProbe() {}
}
