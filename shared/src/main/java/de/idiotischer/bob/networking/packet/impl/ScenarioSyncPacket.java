package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.networking.packet.Packet;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.theme.Theme;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.ImageUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

//TODO: alle assets wie flaggen für das scenario etc syncen
public class ScenarioSyncPacket implements Packet {

    private Path focusesDir;

    private Theme theme;
    private String abbreviation;
    private String name;

    private List<Color> takenColors = new ArrayList<>();
    private List<Color> borderColors = new ArrayList<>();

    private BufferedImage mapImage;
    private BufferedImage backgroundImage;

    private byte[] unusableJson;
    private byte[] countriesJson;
    private byte[] tilesJson;
    private byte[] statesJson;
    private byte[] troopsJson;
    private byte[] warsJson;
    private byte[] ideologiesJson;
    private byte[] themeJson;
    private byte[] configJson;
    private byte[] modifiersJson;
    private byte[] factionsJson;
    private byte[] factionRulesJson;

    private List<FlagEntry> flagEntries = new ArrayList<>();
    private byte[] dirBytes;

    public ScenarioSyncPacket() {}

    public ScenarioSyncPacket(Scenario scenario) {
        this.abbreviation = scenario.getAbbreviation();
        this.name = scenario.getName();

        this.takenColors = new ArrayList<>(scenario.getTakenColors());
        this.borderColors = new ArrayList<>(scenario.getBorderColors());

        this.mapImage = scenario.isMapDefault() ? null : scenario.getMapImage();
        this.backgroundImage = scenario.isBackgroundDefault() ? null : scenario.getBackgroundImage();

        this.unusableJson = scenario.isUnusableDefault() ? null : FileUtil.readFile(scenario.getUnusable());
        this.countriesJson = scenario.isCountryConfigDefault() ? null : FileUtil.readFile(scenario.getCountryConfig());
        this.tilesJson = scenario.isTilesConfigDefault() ? null : FileUtil.readFile(scenario.getTilesConfig());
        this.statesJson = scenario.isStatesConfigDefault() ? null : FileUtil.readFile(scenario.getStatesConfig());
        this.troopsJson = scenario.isTroopConfigDefault() ? null : FileUtil.readFile(scenario.getTroopConfig());
        this.warsJson = scenario.isWarsDefault() ? null : FileUtil.readFile(scenario.getWarsConfig());
        this.themeJson = scenario.isThemesDefault() ? null : FileUtil.readFile(scenario.getThemeConfig());
        this.ideologiesJson = scenario.isIdeologiesDefault() ? null : FileUtil.readFile(scenario.getIdeologiesConfig());
        this.configJson = scenario.isConfigDefault() ? null : FileUtil.readFile(scenario.getConfig());
        this.modifiersJson = scenario.isModifiersConfigDefault() ? null : FileUtil.readFile(scenario.getModifierConfig());
        this.factionRulesJson = scenario.isFactionRulesConfigDefault() ? null : FileUtil.readFile(scenario.getFactionRulesConfig());
        this.factionsJson = scenario.isFactionsConfigDefault() ? null : FileUtil.readFile(scenario.getFactionConfig());
        this.focusesDir = scenario.isFocusesConfigDefault() ? null : scenario.getFocusesDir();

        this.theme = scenario.getTheme();

        Path flagFolder = FileUtil.getDefaultFlagsDir(scenario);

        if (Files.exists(flagFolder)) {
            try {
                flagEntries = readFlagFolder(flagFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private List<FlagEntry> readFlagFolder(Path root) throws IOException {
        List<FlagEntry> entries = new ArrayList<>();

        Files.walk(root).forEach(path -> {
            if (path.equals(root)) return;

            try {
                String relativePath = root.relativize(path).toString().replace(File.separatorChar, '/');

                if (Files.isDirectory(path)) entries.add(new FlagEntry(relativePath, true, null));
                else {
                    byte[] data = Files.readAllBytes(path);
                    entries.add(new FlagEntry(relativePath, false, data));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return entries;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);
        try {
            writeString(buffer, abbreviation);
            writeString(buffer, name);

            buffer.putInt(flagEntries.size());

            for (FlagEntry entry : flagEntries) {
                writeString(buffer, entry.path);
                buffer.put((byte) (entry.directory ? 1 : 0));

                if (!entry.directory) writeBytes(buffer, entry.data);
            }

            buffer.putInt(takenColors.size());
            for (Color c : takenColors) buffer.putInt(c.getRGB());

            buffer.putInt(borderColors.size());
            for (Color c : borderColors) buffer.putInt(c.getRGB());

            ImageUtil.writeImage(buffer, mapImage);
            ImageUtil.writeImage(buffer, backgroundImage);

            writeBytes(buffer, unusableJson);
            writeBytes(buffer, countriesJson);
            writeBytes(buffer, tilesJson);
            writeBytes(buffer, statesJson);
            writeBytes(buffer, troopsJson);
            writeBytes(buffer, warsJson);
            writeBytes(buffer, ideologiesJson);
            writeBytes(buffer, configJson);
            writeBytes(buffer, modifiersJson);
            writeBytes(buffer, factionRulesJson);
            writeBytes(buffer, factionsJson);

            writeBytes(buffer, themeJson);
            writeString(buffer, theme == null ? "" : theme.serialize());

            if(Files.notExists(focusesDir)) return;
            writeBytes(buffer,writeDir(focusesDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void read(ByteBuffer buffer) {
        this.abbreviation = readString(buffer);
        this.name = readString(buffer);

        takenColors.clear();
        borderColors.clear();
        flagEntries.clear();

        int flagCount = buffer.getInt();

        for (int i = 0; i < flagCount; i++) {
            String path = readString(buffer);
            boolean directory = buffer.get() == 1;

            byte[] data = null;

            if (!directory) data = readBytes(buffer);

            flagEntries.add(new FlagEntry(path, directory, data));
        }

        int takenSize = buffer.getInt();
        for (int i = 0; i < takenSize; i++) takenColors.add(new Color(buffer.getInt()));

        int borderSize = buffer.getInt();
        for (int i = 0; i < borderSize; i++) borderColors.add(new Color(buffer.getInt()));

        this.mapImage = ImageUtil.readImage(buffer);

        this.backgroundImage = ImageUtil.readImage(buffer);

        this.unusableJson = readBytes(buffer);
        this.countriesJson = readBytes(buffer);
        this.tilesJson = readBytes(buffer);
        this.statesJson = readBytes(buffer);
        this.troopsJson = readBytes(buffer);
        this.warsJson = readBytes(buffer);
        this.ideologiesJson = readBytes(buffer);
        this.configJson = readBytes(buffer);
        this.modifiersJson = readBytes(buffer);
        this.factionRulesJson = readBytes(buffer);
        this.factionsJson = readBytes(buffer);

        this.themeJson = readBytes(buffer);
        this.theme = Theme.deserialize(name + "-theme", readString(buffer));

        this.dirBytes = readBytes(buffer);
    }

    public void readDir(byte[] zipDat, Path output) {
        try {
            Path root = output.toAbsolutePath().normalize();

            Files.createDirectories(root);
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipDat))) {
                ZipEntry ent;

                while ((ent = zip.getNextEntry()) != null) {
                    Path target = root.resolve(ent.getName()).normalize();

                    if (!target.startsWith(root)) throw new IllegalStateException("ZIP ENTRY INVALID " + ent.getName());

                    if (ent.isDirectory()) Files.createDirectories(target);
                    else {
                        Files.createDirectories(target.getParent());
                        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] writeDir(Path p) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try(ZipOutputStream zip = new ZipOutputStream(os)) {
            try(var paths = Files.walk(p)) {
                paths.filter(Files::isRegularFile).forEach(file -> {
                    String name = p.relativize(file).toString().replace('\\','/');

                    try {
                        zip.putNextEntry(new ZipEntry(name));
                        Files.copy(file,zip);
                        zip.closeEntry();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return os.toByteArray();
    }

    private void writeFlagFolder(Path targetDir) throws IOException {
        Path flagsDir = targetDir.resolve("flags");

        if(!flagEntries.isEmpty()) Files.createDirectories(flagsDir);

        for (FlagEntry entry : flagEntries) {
            Path target = flagsDir.resolve(entry.path).normalize();

            if (!target.startsWith(flagsDir.normalize())) throw new IOException("Invalid flag path: " + entry.path);

            if (entry.directory) Files.createDirectories(target);
            else {
                Files.createDirectories(target.getParent());

                if (Files.notExists(target)) Files.write(target, entry.data);
            }
        }
    }


    public Path applyToDisk2() {
        return applyToDisk2(getAbbreviation());
    }

    public Path applyToDisk2(String baseScenarioDirName) {
        Path baseScenarioDir = Paths.get(baseScenarioDirName);
        Path targetDir = resolveScenarioDir(baseScenarioDir);

        try {
            if(Files.notExists(targetDir)) {
                targetDir = resolveFallbackScenarioDir(baseScenarioDir);
                if(Files.notExists(targetDir)) Files.createDirectories(targetDir);
            }

            writeIfMissing(targetDir.resolve("unusable.json"), unusableJson);
            writeIfMissing(targetDir.resolve("countries.json"), countriesJson);
            writeIfMissing(targetDir.resolve("states.json"), statesJson);
            writeIfMissing(targetDir.resolve("tiles.json"), tilesJson);
            writeIfMissing(targetDir.resolve("troops.json"), troopsJson);
            writeIfMissing(targetDir.resolve("wars.json"), warsJson);
            writeIfMissing(targetDir.resolve("theme.json"), themeJson);
            writeIfMissing(targetDir.resolve("ideologies.json"), ideologiesJson);
            writeIfMissing(targetDir.resolve("config.json"), configJson);
            writeIfMissing(targetDir.resolve("modifiers.json"), modifiersJson);
            writeIfMissing(targetDir.resolve("factionRules.json"), factionRulesJson);
            writeIfMissing(targetDir.resolve("factions.json"), factionsJson);

            writeFlagFolder(targetDir);

            if (mapImage != null) {
                Path mapPath = targetDir.resolve("map.png");
                if (Files.notExists(mapPath)) ImageIO.write(mapImage, "png", mapPath.toFile());
            }

            if (backgroundImage != null) {
                Path mapPath = targetDir.resolve("background.png");
                if (Files.notExists(mapPath)) ImageIO.write(backgroundImage, "png", mapPath.toFile());
            }

            readDir(dirBytes,targetDir);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return targetDir;
    }

    public boolean applyToDisk() {
        return applyToDisk(getAbbreviation());
    }

    public boolean applyToDisk(String baseScenarioDirName) {
        Path baseScenarioDir = Paths.get(baseScenarioDirName);
        Path targetDir = resolveScenarioDir(baseScenarioDir);

        boolean server = false;

        try {
            if(Files.notExists(targetDir)) {
                targetDir = resolveFallbackScenarioDir(baseScenarioDir);
                server = true;
                if(Files.notExists(targetDir)) Files.createDirectories(targetDir);
            }

            writeIfMissing(targetDir.resolve("unusable.json"), unusableJson);
            writeIfMissing(targetDir.resolve("countries.json"), countriesJson);
            writeIfMissing(targetDir.resolve("states.json"), statesJson);
            writeIfMissing(targetDir.resolve("tiles.json"), tilesJson);
            writeIfMissing(targetDir.resolve("troops.json"), troopsJson);
            writeIfMissing(targetDir.resolve("wars.json"), warsJson);
            writeIfMissing(targetDir.resolve("theme.json"), themeJson);
            writeIfMissing(targetDir.resolve("ideologies.json"), ideologiesJson);
            writeIfMissing(targetDir.resolve("config.json"), configJson);
            writeIfMissing(targetDir.resolve("modifiers.json"), modifiersJson);
            writeIfMissing(targetDir.resolve("factionRules.json"), factionRulesJson);
            writeIfMissing(targetDir.resolve("factions.json"), factionsJson);

            writeFlagFolder(targetDir);

            if (mapImage != null) {
                Path mapPath = targetDir.resolve("map.png");
                if (Files.notExists(mapPath)) ImageIO.write(mapImage, "png", mapPath.toFile());
            }

            if (backgroundImage != null) {
                Path mapPath = targetDir.resolve("background.png");
                if (Files.notExists(mapPath)) ImageIO.write(backgroundImage, "png", mapPath.toFile());
            }

            readDir(dirBytes,targetDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return server;
    }

    public Path resolveFallbackScenarioDir(Path baseScenarioDir) {
        //scenario dir: /scenario/scenario123
        //fallback (tote dir lol): /online/temp/scenario/scenario123
        return FileUtil.getScenarioDir().resolveSibling("online").resolve("temp").resolve("scenario").resolve(baseScenarioDir);
    }

    private Path resolveScenarioDir(Path baseScenarioDir) {
        return FileUtil.getScenarioDir().resolve(baseScenarioDir);
    }

    private void writeIfMissing(Path path, byte[] data) throws IOException {
        if (Files.exists(path)) return;

        if (data != null && data.length > 0) {
            Files.write(path, data);
        }
    }

    private void writeBytes(ByteBuffer buffer, byte[] data) {
        if (data != null) {
            buffer.putInt(data.length);
            buffer.put(data);
        } else {
            buffer.putInt(0);
        }
    }

    private byte[] readBytes(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length <= 0) return new byte[0];

        byte[] data = new byte[length];
        buffer.get(data);
        return data;
    }

    private void writeString(@NotNull ByteBuffer buffer, @NotNull String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }

    @Contract("_ -> new")
    private @NotNull String readString(@NotNull ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public Theme getTheme() {
        return theme;
    }

    private static class FlagEntry {
        private final String path;
        private final boolean directory;
        private final byte[] data;

        public FlagEntry(String path, boolean directory, byte[] data) {
            this.path = path;
            this.directory = directory;
            this.data = data;
        }
    }


    public String getAbbreviation() { return abbreviation; }
    public String getName() { return name; }
    public List<Color> getTakenColors() { return takenColors; }
    public List<Color> getBorderColors() { return borderColors; }
    public BufferedImage getMapImage() { return mapImage; }
}