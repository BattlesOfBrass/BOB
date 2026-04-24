package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.cnet.modules.packets.common.packet.Packet;
import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.util.FileUtil;
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
public class ScenarioSyncPacket implements Packet, de.idiotischer.bob.networking.packet.Packet {

    private String abbreviation;
    private String name;

    private List<Color> takenColors = new ArrayList<>();
    private List<Color> borderColors = new ArrayList<>();

    private BufferedImage mapImage;

    private byte[] unusableJson;
    private byte[] countriesJson;
    private byte[] statesJson;

    public ScenarioSyncPacket() {}

    public ScenarioSyncPacket(Scenario scenario) {
        this.abbreviation = scenario.getAbbreviation();
        this.name = scenario.getName();

        this.takenColors = new ArrayList<>(scenario.getTakenColors());
        this.borderColors = new ArrayList<>(scenario.getBorderColors());

        this.mapImage = scenario.getMapImage();

        this.unusableJson = scenario.isUnusableDefault() ? null : FileUtil.readFile(scenario.getUnusable());
        this.countriesJson = scenario.isCountryConfigDefault() ? null : FileUtil.readFile(scenario.getCountryConfig());
        this.statesJson = scenario.isStatesConfigDefault() ? null : FileUtil.readFile(scenario.getStatesConfig());
    }

    @Override
    public void write(BufferUtil buffer) {
        try {
            buffer.putUtf(abbreviation);
            buffer.putUtf(name);

            buffer.getRaw().putInt(takenColors.size());
            for (Color c : takenColors) buffer.getRaw().putInt(c.getRGB());

            buffer.getRaw().putInt(borderColors.size());
            for (Color c : borderColors) buffer.getRaw().putInt(c.getRGB());

            if (mapImage != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(mapImage, "png", baos); //add support for svg and webp etc which is better
                byte[] imageData = baos.toByteArray();

                buffer.getRaw().putInt((imageData.length));
                buffer.getRaw().put(imageData);
            } else {
                buffer.getRaw().putInt(0);
            }

            writeBytes(buffer.getRaw(), unusableJson);
            writeBytes(buffer.getRaw(), countriesJson);
            writeBytes(buffer.getRaw(), statesJson);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(ByteBuffer buffer) {
        try {
            writeString(buffer, abbreviation);
            writeString(buffer, name);

            buffer.putInt(takenColors.size());
            for (Color c : takenColors) buffer.putInt(c.getRGB());

            buffer.putInt(borderColors.size());
            for (Color c : borderColors) buffer.putInt(c.getRGB());

            if (mapImage != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(mapImage, "png", baos); //add support for svg and webp etc which is better
                byte[] imageData = baos.toByteArray();

                buffer.putInt((imageData.length));
                buffer.put(imageData);
            } else {
                buffer.putInt(0);
            }

            writeBytes(buffer, unusableJson);
            writeBytes(buffer, countriesJson);
            writeBytes(buffer, statesJson);

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

        int takenSize = buffer.getInt();
        for (int i = 0; i < takenSize; i++) {
            takenColors.add(new Color(buffer.getInt()));
        }

        int borderSize = buffer.getInt();
        for (int i = 0; i < borderSize; i++) {
            borderColors.add(new Color(buffer.getInt()));
        }

        int imageLength = buffer.getInt();
        if (imageLength > 0) {
            byte[] imageData = new byte[imageLength];
            buffer.get(imageData);
            try {
                this.mapImage = ImageIO.read(new ByteArrayInputStream(imageData));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.unusableJson = readBytes(buffer);
        this.countriesJson = readBytes(buffer);
        this.statesJson = readBytes(buffer);
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

            if (mapImage != null) {
                Path mapPath = targetDir.resolve("map.png");
                if (Files.notExists(mapPath)) {
                    ImageIO.write(mapImage, "png", mapPath.toFile());
                }
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
            writeIfMissing(targetDir.resolve("states.json"), statesJson);

            if (mapImage != null) {
                Path mapPath = targetDir.resolve("map.png");
                if (Files.notExists(mapPath)) {
                    ImageIO.write(mapImage, "png", mapPath.toFile());
                }
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

    @Override
    public void handle(Networker networker) {

    }
}