package io.github.zekerzhayard.forgewrapper.installer.detector;

import com.mojang.logging.LogUtils;
import io.github.zekerzhayard.forgewrapper.installer.Main;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.StringUtils;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class NeoForgeModLoader implements IModFileCandidateLocator {
    private static final String SUFFIX = ".jar";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path modFolder;
    private final String customName;

    public NeoForgeModLoader() {
        this(getDirectoryRequired());
    }

    private static Path getDirectoryRequired() {
        File file = new File(Main.gameDir);
        Path path;
        if (Main.coreModsOnly) {
            path = file.getParentFile().getParentFile().toPath()
                    .resolve("shared/neoforge/"+Main.mcVersion+"/mods/core/");
        } else {
            path = file.getParentFile().getParentFile().toPath()
                    .resolve("shared/neoforge/"+Main.mcVersion+"/mods/");
        }
        return path;
    }

    NeoForgeModLoader(Path modFolder) {
        this(modFolder, "DL shared Mod Directory");
    }

    public NeoForgeModLoader(Path modFolder, String name) {
        this.modFolder = Objects.requireNonNull(modFolder, "modFolder");
        this.customName = Objects.requireNonNull(name, "name");
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        LOGGER.debug(LogMarkers.SCAN, "Scanning mods dir {} for mods", this.modFolder);

        List<Path> directoryContent;
        if (!Main.coreModsOnly) {
            try (var files = Stream.concat(Files.list(this.modFolder), Files.list(this.modFolder.getParent()))) {
                directoryContent = files
                        .filter(p -> StringUtils.toLowerCase(p.getFileName().toString()).endsWith(SUFFIX))
                        .sorted(Comparator.comparing(path -> StringUtils.toLowerCase(path.getFileName().toString())))
                        .toList();
            } catch (UncheckedIOException | IOException e) {
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failed_to_list_folder_content", this.modFolder).withAffectedPath(this.modFolder).withCause(e));
            }
        } else {
            try (var files = Files.list(this.modFolder)) {
                directoryContent = files
                        .filter(p -> StringUtils.toLowerCase(p.getFileName().toString()).endsWith(SUFFIX))
                        .sorted(Comparator.comparing(path -> StringUtils.toLowerCase(path.getFileName().toString())))
                        .toList();
            } catch (UncheckedIOException | IOException e) {
                throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failed_to_list_folder_content", this.modFolder).withAffectedPath(this.modFolder).withCause(e));
            }
        }

        for (var file : directoryContent) {
            if (!Files.isRegularFile(file)) {
                pipeline.addIssue(ModLoadingIssue.warning("fml.modloadingissue.brokenfile.unknown").withAffectedPath(file));
                continue;
            }

            pipeline.addPath(file, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
        }
    }

    @Override
    public String toString() {
        return "{" + customName + " locator at " + this.modFolder + "}";
    }
}