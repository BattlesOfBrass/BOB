package de.idiotischer.bob.util;

import com.google.common.reflect.ClassPath;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

//TODO: placeholder aus github asets fetchen
public class FileUtil {

    public static Path getJarDir() {
        try {
            return Path.of(FileUtil.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .getParent();
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

    public static void extractFolder(ClassLoader resourceRoot, @NotNull String folderName) throws Exception {
        ClassPath classPath = ClassPath.from(resourceRoot);

        //TODO wenn internet zugang dann von github assets holen

        String prefix = folderName.startsWith("/") ? folderName.substring(1) : folderName;
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }

        for (ClassPath.ResourceInfo resource : classPath.getResources()) {
            String resourceName = resource.getResourceName();

            if (resourceName.startsWith(prefix) &&
                    !resourceName.endsWith(".class") &&
                    !resourceName.startsWith("META-INF/")) {

                Path destination = Paths.get(resourceName);

                try {
                    if (Files.notExists(destination)) {
                        if (destination.getParent() != null) {
                            Files.createDirectories(destination.getParent());
                        }

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

    @ApiStatus.Obsolete
    public static Path getRunningDir() {
       return getJarDir().toAbsolutePath();
    }

    public static Path getConfigDir() {
        Path configDir = getJarDir().toAbsolutePath().resolve("config/");

        if(Files.notExists(configDir)) {
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

        if(Files.notExists(scenarioDir)) {
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

        if(Files.notExists(scenarioDir)) {
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

        if(Files.notExists(scenarioDir)) {
            try {
                Files.createDirectory(scenarioDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return scenarioDir;
    }

    public static Path getIconPath() {
        Path scenarioDir = getRunningDir();

        //TODO: check this ig
        if(Files.notExists(scenarioDir)) {
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
            return stream
                    .filter(Files::isDirectory)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list scenarios", e);
        }
    }

    public static Path getDefaultConfig() {
        Path configPath = getConfigDir().resolve("config.json");

        if(Files.notExists(configPath)) {
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
        if(Files.notExists(scenarioDir)) {
            try {
                Files.createFile(scenarioDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return scenarioDir;
    }
}
