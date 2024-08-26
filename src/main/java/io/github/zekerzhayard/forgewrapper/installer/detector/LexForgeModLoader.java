package io.github.zekerzhayard.forgewrapper.installer.detector;

import com.mojang.logging.LogUtils;
import io.github.zekerzhayard.forgewrapper.installer.Main;
import net.minecraftforge.fml.loading.LogMarkers;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import net.minecraftforge.fml.loading.moddiscovery.AbstractModProvider;
import net.minecraftforge.forgespi.locating.IModLocator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LexForgeModLoader extends AbstractModProvider implements IModLocator {
    private static final String SUFFIX = ".jar";
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private final Path modFolder;
    private final String customName;

    public LexForgeModLoader() {
        this(getDirectoryRequired(), "DL shared Mod Directory");
    }

    private static Path getDirectoryRequired() {
        File file = new File(Main.gameDir);
        Path path;
        if (Main.coreModsOnly) {
            path = file.getParentFile().getParentFile().toPath()
                    .resolve("shared/lexforge/"+Main.mcVersion+"/mods/core/");
        } else {
            path = file.getParentFile().getParentFile().toPath()
                    .resolve("shared/lexforge/"+Main.mcVersion+"/mods/");
        }
        return path;
    }

    LexForgeModLoader(Path modFolder, String name) {
        this.modFolder = modFolder;
        this.customName = name;
    }

    @Override
    public List<ModFileOrException> scanMods() {
        LOGGER.debug(LogMarkers.SCAN, "Scanning mods dir {} for mods", this.modFolder);
        var excluded = ModDirTransformerDiscoverer.allExcluded();
        try {
            var ret = new ArrayList<ModFileOrException>();
            var files = Files.list(this.modFolder).toList();
            for (var file : files) {
                var name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (excluded.contains(file) || !name.endsWith(SUFFIX))
                    continue;
                var mod = this.createMod(file, true);
                if (mod == null) {
                    LOGGER.debug(LogMarkers.SCAN, "Found unknown jar file {} ignoring", file);
                } else {
                    ret.add(mod);
                }
            }
            return ret;
        } catch (IOException e) {
            return sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }

    @Override
    public String name() {
        return customName;
    }

    @Override
    public String toString() {
        return "{"+customName+" locator at "+this.modFolder+"}";
    }
}
