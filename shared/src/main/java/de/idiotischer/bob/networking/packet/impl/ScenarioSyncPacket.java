package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.cnet.modules.packets.common.packet.Packet;
import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.LZ4Util;
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
import java.util.ArrayList;
import java.util.List;

//TODO: alle assets wie flaggen für das scenario etc syncen
//TODO: optimize data usage
public class ScenarioSyncPacket implements de.idiotischer.bob.networking.packet.Packet {

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

    private transient byte[] cachedMapImage;
    private transient byte[] cachedBackgroundImage;

    public ScenarioSyncPacket() {}

    public ScenarioSyncPacket(Scenario scenario) {
        this.abbreviation = scenario.getAbbreviation();
        this.name = scenario.getName();

        this.takenColors = new ArrayList<>(scenario.getTakenColors());
        this.borderColors = new ArrayList<>(scenario.getBorderColors());

        this.mapImage = scenario.getMapImage();
        this.backgroundImage = scenario.getBackgroundImage();

        this.unusableJson = scenario.isUnusableDefault() ? null : FileUtil.readFile(scenario.getUnusable());
        this.countriesJson = scenario.isCountryConfigDefault() ? null : FileUtil.readFile(scenario.getCountryConfig());
        this.tilesJson = scenario.isTilesConfigDefault() ? null : FileUtil.readFile(scenario.getTilesConfig());
        this.statesJson = scenario.isStatesConfigDefault() ? null : FileUtil.readFile(scenario.getStatesConfig());
        this.troopsJson = scenario.isTroopConfigDefault() ? null : FileUtil.readFile(scenario.getTroopConfig());

        buildCache();
    }

    @Override
    public void write(ByteBuffer buffer) {
        writeString(buffer, abbreviation);
        writeString(buffer, name);

        buffer.putInt(takenColors.size());
        for (Color c : takenColors) buffer.putInt(c.getRGB());

        buffer.putInt(borderColors.size());
        for (Color c : borderColors) buffer.putInt(c.getRGB());

        writeBytes(buffer, cachedMapImage);
        writeBytes(buffer, cachedBackgroundImage);

        writeBytes(buffer, unusableJson);
        writeBytes(buffer, countriesJson);
        writeBytes(buffer, tilesJson);
        writeBytes(buffer, statesJson);
        writeBytes(buffer, troopsJson);
    }

    @Override
    public void read(ByteBuffer buffer) {
        this.abbreviation = readString(buffer);
        this.name = readString(buffer);

        takenColors.clear();
        borderColors.clear();

        int takenSize = buffer.getInt();
        for (int i = 0; i < takenSize; i++) {
            takenColors.add(new Color(buffer.getInt()));
        }

        int borderSize = buffer.getInt();
        for (int i = 0; i < borderSize; i++) {
            borderColors.add(new Color(buffer.getInt()));
        }

        byte[] mapBytes = readBytes(buffer);
        if (mapBytes.length > 0) {
            try {
                this.mapImage = ImageIO.read(new ByteArrayInputStream(mapBytes));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.mapImage = null;
        }

        byte[] bgBytes = readBytes(buffer);
        if (bgBytes.length > 0) {
            try {
                this.backgroundImage = ImageIO.read(new ByteArrayInputStream(bgBytes));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.backgroundImage = null;
        }

        this.unusableJson = readBytes(buffer);
        this.countriesJson = readBytes(buffer);
        this.tilesJson = readBytes(buffer);
        this.statesJson = readBytes(buffer);
        this.troopsJson = readBytes(buffer);
    }

    public void buildCache() {
        try {
            cachedMapImage = mapImage == null ? null : encode(mapImage);
            cachedBackgroundImage = backgroundImage == null ? null : encode(backgroundImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] encode(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "webp", baos);
        return baos.toByteArray();
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
            writeIfMissing(targetDir.resolve("tiles.json"), tilesJson);
            writeIfMissing(targetDir.resolve("states.json"), statesJson);
            writeIfMissing(targetDir.resolve("troops.json"), troopsJson);

            if (mapImage != null) {
                Path mapPath = targetDir.resolve("map.png");
                if (Files.notExists(mapPath)) {
                    ImageIO.write(mapImage, "png", mapPath.toFile());
                }
            }

            Path backgroundPath = targetDir.resolve("background.png");

            if (backgroundImage != null) {
                ImageIO.write(backgroundImage, "png", backgroundPath.toFile());
            } else {
                Files.deleteIfExists(backgroundPath);
            }

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
            writeIfMissing(targetDir.resolve("tiles.json"), tilesJson);
            writeIfMissing(targetDir.resolve("states.json"), statesJson);
            writeIfMissing(targetDir.resolve("troops.json"), troopsJson);

            if (mapImage != null) {
                Path mapPath = targetDir.resolve("map.png");
                if (Files.notExists(mapPath)) {
                    ImageIO.write(mapImage, "png", mapPath.toFile());
                }
            }

            Path backgroundPath = targetDir.resolve("background.png");

            if (backgroundImage != null) {
                ImageIO.write(backgroundImage, "png", backgroundPath.toFile());
            } else {
                Files.deleteIfExists(backgroundPath);
            }
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

    public String getAbbreviation() { return abbreviation; }
    public String getName() { return name; }
    public List<Color> getTakenColors() { return takenColors; }
    public List<Color> getBorderColors() { return borderColors; }
    public BufferedImage getMapImage() { return mapImage; }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public byte[] getTroopsJson() {
        return troopsJson;
    }

    public byte[] getUnusableJson() {
        return unusableJson;
    }

    public byte[] getTilesJson() {
        return tilesJson;
    }

    public byte[] getStatesJson() {
        return statesJson;
    }

    public byte[] getCountriesJson() {
        return countriesJson;
    }

}