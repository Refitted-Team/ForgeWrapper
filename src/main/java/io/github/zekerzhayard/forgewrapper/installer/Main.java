package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.installer.detector.DetectorLoader;
import io.github.zekerzhayard.forgewrapper.installer.detector.IFileDetector;
import io.github.zekerzhayard.forgewrapper.installer.util.ModuleUtil;

public class Main {
    public static String mcVersion; // DL: Make Minecraft version public
    public static String gameDir; // DL: Grant access to the game directory
    public static String modDirectory;

    public static void main(String[] args) throws Throwable {
        // --fml.neoForgeVersion 20.2.20-beta --fml.fmlVersion 1.0.2 --fml.mcVersion 1.20.2 --fml.neoFormVersion 20231019.002635 --launchTarget forgeclient

        List<String> argsList = Stream.of(args).toList();

        gameDir = argsList.get(argsList.indexOf("--gameDir") + 1); // DL: Grant access to the game directory

        // NOTE: this is only true for NeoForge versions past 20.2.x
        // early versions of NeoForge (for 1.20.1) are not supposed to be covered here
        boolean isNeoForge = argsList.contains("--fml.neoForgeVersion");
        modDirectory = System.getProperty("fabric.addMods");

        mcVersion = argsList.get(argsList.indexOf("--fml.mcVersion") + 1); // DL: Make Minecraft version public
        String forgeGroup = argsList.contains("--fml.forgeGroup") ? argsList.get(argsList.indexOf("--fml.forgeGroup") + 1) : "net.neoforged";
        String forgeArtifact = isNeoForge ? "neoforge" : "forge";
        String forgeVersionKey = isNeoForge ? "--fml.neoForgeVersion" : "--fml.forgeVersion";
        String forgeVersion = argsList.get(argsList.indexOf(forgeVersionKey) + 1);
        String forgeFullVersion = isNeoForge ? forgeVersion : mcVersion + "-" + forgeVersion;

        IFileDetector detector = DetectorLoader.loadDetector();
        try {
            Bootstrap.bootstrap(detector.getJvmArgs(forgeGroup, forgeArtifact, forgeFullVersion), detector.getMinecraftJar(mcVersion).getFileName().toString(), detector.getLibraryDir().toAbsolutePath().toString());
        } catch (Throwable ignored) {
            // Avoid this bunch of hacks that nuke the whole wrapper.
        }
        if (!detector.checkExtraFiles(forgeGroup, forgeArtifact, forgeFullVersion)) {
            System.out.println("Some extra libraries are missing! Running the installer to generate them now.");

            // Check installer jar.
            Path installerJar = detector.getInstallerJar(forgeGroup, forgeArtifact, forgeFullVersion);
            if (!IFileDetector.isFile(installerJar)) {
                throw new RuntimeException("Unable to detect the forge installer!");
            }

            // Check vanilla Minecraft jar.
            Path minecraftJar = detector.getMinecraftJar(mcVersion);
            if (!IFileDetector.isFile(minecraftJar)) {
                throw new RuntimeException("Unable to detect the Minecraft jar!");
            }

            try (URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
                Main.class.getProtectionDomain().getCodeSource().getLocation(),
                Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
                installerJar.toUri().toURL()
            }, ModuleUtil.getPlatformClassLoader())) {
                Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");
                if (!(boolean) installer.getMethod("install", File.class, File.class, File.class).invoke(null, detector.getLibraryDir().toFile(), minecraftJar.toFile(), installerJar.toFile())) {
                    return;
                }
            }
        }

        ModuleUtil.setupClassPath(detector.getLibraryDir(), detector.getExtraLibraries(forgeGroup, forgeArtifact, forgeFullVersion));
        Class<?> mainClass = ModuleUtil.setupBootstrapLauncher(Class.forName(detector.getMainClass(forgeGroup, forgeArtifact, forgeFullVersion)));
        mainClass.getMethod("main", String[].class).invoke(null, new Object[] { args });
    }
}
