package de.idiotischer.bob.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.SharedCore;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public record Theme(String name, Button button, Scrollbar scrollbar, ColorPalette palette) {

    public record Button(Color textColor, Color bgColor, Color bgColorLight, Color borderColor, Color borderColorHover) {
        public static Color defaultTextColor() {
            return Color.WHITE;
        }
        public static Color defaultBGColor() {
            return Color.BLACK;
        }
        public static Color defaultBGColorLight() {
            return Color.DARK_GRAY;
        }
        public static Color defaultBorderColor() {
            return Color.DARK_GRAY.darker();
        }
        public static Color defaultBorderColorHover() {
            return Color.LIGHT_GRAY;
        }
        public static Button defaultButton() {
            return new Button(defaultTextColor(),defaultBGColor(),defaultBGColorLight(),defaultBorderColor(),defaultBorderColorHover());
        }
    }

    public record Scrollbar(Color bgColor, Color scrollerColor) {
        public static Color defaultBGColor() {
            return Color.GRAY.darker();
        }
        public static Color defaultScrollerColor() {
            return Color.DARK_GRAY;
        }
        public static Scrollbar defaultScrollbar() {
            return new Scrollbar(defaultBGColor(),defaultScrollerColor());
        }
    }

    public record ColorPalette(Color darkColor, Color defaultColor, Color lightColor) {
        public static Color defaultDarkColor() {
            return Color.DARK_GRAY;
        }

        public static Color defaultLightColor() {
            return Color.LIGHT_GRAY;
        }

        public static Color defaultDColor() {
            return Color.GRAY;
        }

        public static ColorPalette defaultColorPalette() {
            return new ColorPalette(defaultDarkColor(), defaultDColor(), defaultLightColor());
        }
    }

    public static Theme defaultTheme() {
        return new Theme("default",Button.defaultButton(),Scrollbar.defaultScrollbar(),ColorPalette.defaultColorPalette());
    }

    public static Theme fromJson(String name, Path path) {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            final Button[] button = {Button.defaultButton()};
            final Scrollbar[] scrollbar = {Scrollbar.defaultScrollbar()};
            final ColorPalette[] colorPalette = {ColorPalette.defaultColorPalette()};

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String sectionName = entry.getKey();
                JsonObject element = entry.getValue().getAsJsonObject();

                if(sectionName.equalsIgnoreCase("button")) {
                    String[] textColor = getElement(element,"textColor");

                    String[] bgColor = getElement(element,"bgColor");

                    String[] bgColorLight = getElement(element,"bgColorLight");

                    String[] borderColor = getElement(element,"borderColor");

                    String[] borderColorHover = getElement(element,"borderColorHover");

                    Color text = getColor(textColor, Button.defaultTextColor());
                    Color bg = getColor(bgColor, Button.defaultBGColor());
                    Color bgL = getColor(bgColorLight, Button.defaultBGColorLight());
                    Color border = getColor(borderColor, Button.defaultBorderColor());
                    Color borderHover = getColor(borderColorHover, Button.defaultBorderColorHover());

                    button[0] = new Button(text,bg,bgL,border,borderHover);
                } else if(sectionName.equalsIgnoreCase("scrollbar")) {
                    String[] bgColor = getElement(element,"bgColor");
                    String[] scrollerColor = getElement(element,"scrollerColor");

                    Color bg = getColor(bgColor, Scrollbar.defaultBGColor());
                    Color scroller = getColor(scrollerColor, Scrollbar.defaultScrollerColor());

                    scrollbar[0] = new Scrollbar(bg,scroller);
                } else if(sectionName.equalsIgnoreCase("colorPalette")) {
                    String[] darkS = getElement(element,"darkColor");
                    String[] lightS = getElement(element,"lightColor");
                    String[] defaultS = getElement(element,"defaultColor");

                    Color dark = getColor(darkS, ColorPalette.defaultDarkColor());
                    Color light = getColor(lightS, ColorPalette.defaultLightColor());
                    Color defaultC = getColor(defaultS, ColorPalette.defaultDColor());

                    colorPalette[0] = new ColorPalette(dark,defaultC,light);
                }
            });

            return new Theme(name,button[0],scrollbar[0],colorPalette[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String[] getElement(JsonObject element, String key) {
        if (!element.has(key) || element.get(key).isJsonNull()) return new String[]{""};

        String value = element.get(key).getAsString();

        if (value == null || value.isBlank()) return new String[]{""};

        return value.split("[;,]");
    }


    public static Color getColor(String[] array, Color defaultV) {
        if(array.length != 3) return defaultV;

        int r = Integer.parseInt(array[0]);
        int g = Integer.parseInt(array[1]);
        int b = Integer.parseInt(array[2]);

        return new Color(r,g,b);
    }

    public String serialize() {
        JsonObject root = new JsonObject();

        JsonObject buttonJson = new JsonObject();
        buttonJson.addProperty("textColor", colorToString(button.textColor()));
        buttonJson.addProperty("bgColor", colorToString(button.bgColor()));
        buttonJson.addProperty("bgColorLight", colorToString(button.bgColorLight()));
        buttonJson.addProperty("borderColor", colorToString(button.borderColor()));
        buttonJson.addProperty("borderColorHover", colorToString(button.borderColorHover()));
        root.add("button", buttonJson);

        JsonObject scrollbarJson = new JsonObject();
        scrollbarJson.addProperty("bgColor", colorToString(scrollbar.bgColor()));
        scrollbarJson.addProperty("scrollerColor", colorToString(scrollbar.scrollerColor()));
        root.add("scrollbar", scrollbarJson);

        JsonObject paletteJson = new JsonObject();
        paletteJson.addProperty("darkColor", colorToString(palette.darkColor()));
        paletteJson.addProperty("defaultColor", colorToString(palette.defaultColor()));
        paletteJson.addProperty("lightColor", colorToString(palette.lightColor()));
        root.add("colorPalette", paletteJson);

        return SharedCore.GSON.toJson(root);
    }

    public static Theme deserialize(String name, String json) {
        try {
            JsonObject root = SharedCore.GSON.fromJson(json, JsonObject.class);

            Button button = Button.defaultButton();
            Scrollbar scrollbar = Scrollbar.defaultScrollbar();
            ColorPalette palette = ColorPalette.defaultColorPalette();

            if (root.has("button") && root.get("button").isJsonObject()) {
                JsonObject obj = root.getAsJsonObject("button");

                Color text = getColor(getElement(obj, "textColor"), Button.defaultTextColor());
                Color bg = getColor(getElement(obj, "bgColor"), Button.defaultBGColor());
                Color bgL = getColor(getElement(obj, "bgColorLight"), Button.defaultBGColorLight());
                Color border = getColor(getElement(obj, "borderColor"), Button.defaultBorderColor());
                Color borderHover = getColor(getElement(obj, "borderColorHover"), Button.defaultBorderColorHover());

                button = new Button(text, bg, bgL, border, borderHover);
            }

            if (root.has("scrollbar") && root.get("scrollbar").isJsonObject()) {
                JsonObject obj = root.getAsJsonObject("scrollbar");

                Color bg = getColor(getElement(obj, "bgColor"), Scrollbar.defaultBGColor());
                Color scroller = getColor(getElement(obj, "scrollerColor"), Scrollbar.defaultScrollerColor());

                scrollbar = new Scrollbar(bg, scroller);
            }

            if (root.has("colorPalette") && root.get("colorPalette").isJsonObject()) {
                JsonObject obj = root.getAsJsonObject("colorPalette");

                Color dark = getColor(getElement(obj, "darkColor"), ColorPalette.defaultDarkColor());
                Color light = getColor(getElement(obj, "lightColor"), ColorPalette.defaultLightColor());
                Color defaultC = getColor(getElement(obj, "defaultColor"), ColorPalette.defaultDColor());

                palette = new ColorPalette(dark, defaultC, light);
            }

            return new Theme(name, button, scrollbar, palette);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String colorToString(Color color) {
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }
}
