package de.idiotischer.bob.util;

import com.google.common.reflect.ClassPath;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import de.idiotischer.bob.scenario.Scenario;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

//TODO: placeholder aus github asets fetchen
public class FileUtil {
    private static final Set<String> EXCLUDED_PREFIXES = Set.of("native/", "net/","META-INF/", "assets/native/", "lib/native/");

    private static final Set<String> EXCLUDED_FILES = Set.of(".DS_Store");

    public static Path getJarDir() {
        try {
            Path path = Path.of(FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            Path p = Files.isRegularFile(path) ? path.getParent() : path;

            //System.out.println(p.toAbsolutePath().toString());

            return p;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static CompletableFuture<Void> replaceIfNotExistingAsync(ClassLoader resourceRoot) {
        return CompletableFuture.runAsync(() -> replaceIfNotExisting(resourceRoot));
    }

    private static void replaceIfNotExisting(ClassLoader resourceRoot) {
        try {
            extractFolder(resourceRoot, "/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //TODO: copy empty folders too (urgent)
    //TODO: fix wrong files are created (ibnstead of just map.png for mdoernday it creates tiles etc)
    public static void extractFolder(ClassLoader resourceRoot, @NotNull String folderName) throws Exception {
        ClassPath classPath = ClassPath.from(resourceRoot);

        //TODO wenn internet zugang dann von github assets holen
        String prefix = folderName.startsWith("/") ? folderName.substring(1) : folderName;
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";

        for (ClassPath.ResourceInfo resource : classPath.getResources()) {
            String resourceName = resource.getResourceName();

            if (isExcluded(resourceName)) continue;

            if (resourceName.startsWith(prefix) && !resourceName.endsWith(".class") && !resourceName.startsWith("META-INF/")) {
                Path destination = getJarDir().resolve(resourceName);

                try {
                    if (Files.notExists(destination)) {
                        if (destination.getParent() != null) Files.createDirectories(destination.getParent());

                        try (InputStream is = resource.url().openStream()) {
                            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("Extracted: " + resourceName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to extract " + resourceName + ": " + e.getMessage());
                }
            }
        }
    }

    private static boolean isExcluded(String resourceName) {
        for (String prefix : EXCLUDED_PREFIXES) if (resourceName.startsWith(prefix)) return true;

        return EXCLUDED_FILES.contains(resourceName);
    }

    @ApiStatus.Obsolete
    public static Path getRunningDir() {
        return getJarDir().toAbsolutePath();
    }

    public static Path getConfigDir() {
        Path configDir = getJarDir().toAbsolutePath().resolve("config/");

        if (Files.notExists(configDir)) {
            try {
                Files.createDirectory(configDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return configDir;
    }

    public static Path getScenarioDir() {
        Path scenarioDir = getJarDir().toAbsolutePath().resolve("scenario/");

        //System.out.println(scenarioDir.toAbsolutePath().toString());

        if (Files.notExists(scenarioDir)) {
            try {
                Files.createDirectory(scenarioDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return scenarioDir;
    }

    public static Path getScenarioDir(String scenarioName) {
        Path scenarioDir = getScenarioDir().resolve(scenarioName);

        if (Files.notExists(scenarioDir)) {
            try {
                Files.createDirectory(scenarioDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return scenarioDir;
    }

    public static Path getDefaultScenarioDir() {
        Path scenarioDir = getScenarioDir().resolve("default/");

        if (Files.notExists(scenarioDir)) {
            try {
                Files.createDirectory(scenarioDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return scenarioDir;
    }

    public static Path getIconPath() {
        Path scenarioDir = getJarDir().resolve("icons/icon.png");

        //TODO: check this ig
        if (Files.notExists(scenarioDir)) {
            try {
                Files.createFile(scenarioDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return scenarioDir;
    }

    public static List<Path> getAllScenarios() {
        Path scenarioDir = getScenarioDir();

        if (!Files.isDirectory(scenarioDir)) return List.of();

        try (Stream<Path> stream = Files.list(scenarioDir)) {
            return stream.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list scenarios", e);
        }
    }

    public static Path getDefaultConfig() {
        Path configPath = getConfigDir().resolve("config.json");

        if (Files.notExists(configPath)) {
            try {
                Files.createFile(configPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return configPath;
    }

    public static Path getHostConfig() {
        Path scenarioDir = getJarDir().toAbsolutePath().resolve("config/host.json");
        if (Files.notExists(scenarioDir)) {
            try {
                Files.createFile(scenarioDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return scenarioDir;
    }

    public static Font getFont() {
        Path fontDir = getJarDir().toAbsolutePath().resolve("font/font.ttf");
        if (Files.notExists(fontDir)) {
            try {
                Files.createFile(fontDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return Font.createFont(Font.TRUETYPE_FONT,fontDir.toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getDefaultFlagsDir() {
        return getFlagsDir(false);
    }

    public static Path getDefaultFlagsDir(Scenario sc) {
        Path flagsDir = getScenarioDir(sc.getAbbreviation()).resolve("flags/");

        if(Files.notExists(flagsDir)) {
            try {
                Files.createDirectory(flagsDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return flagsDir;
    }

    public static Path getFlag(Scenario scenario, String abbreviation) {
        Path countryFlagsDir = getDefaultFlagsDir().resolve(abbreviation).resolve(abbreviation + ".png");
        Path countryFlagsDirSc = getDefaultFlagsDir(scenario).resolve(abbreviation).resolve(abbreviation + ".png");

        if(Files.exists(countryFlagsDir)) return countryFlagsDir;
        else if(Files.exists(countryFlagsDirSc)) return countryFlagsDirSc;

        return null;
    }

    public static Path getFlagsDir(boolean server) {
        Path flagsDir = getJarDir().toAbsolutePath().resolve(server ? "flags/temp/server" : "flags/");

        if(Files.notExists(flagsDir)) {
            try {
                Files.createDirectory(flagsDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return flagsDir;
    }

    public static byte[] readFile(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    public static byte[] readBytes(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length <= 0) return new byte[0];

        byte[] data = new byte[length];
        buffer.get(data);
        return data;
    }
}
