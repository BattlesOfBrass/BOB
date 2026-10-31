package de.idiotischer.bob.effect;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.idiotischer.bob.SharedCore;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public record Effect(Map<String, Integer> popMap, Map<String, Set<String>> tilesTransferMap, Map<String, Set<String>> modsAdd, Map<String, Set<String>> modsRemove) {

    public static Effect fromJson(JsonObject obj) {
        Type mapStringSetType = new TypeToken<Map<String, Set<String>>>(){}.getType();
        Type popType = new TypeToken<Map<String, Integer>>(){}.getType();

        Map<String, Set<String>> tilesTransferMap = new HashMap<>();

        if(obj.has("tiles")) {
            JsonElement el = obj.get("tiles");
            if(el != null) tilesTransferMap = SharedCore.GSON.fromJson(el,mapStringSetType);
        }

        Map<String, Integer> popMap = new HashMap<>();

        if(obj.has("population")) {
            JsonElement el = obj.get("population");
            if(el != null) popMap = SharedCore.GSON.fromJson(el,popType);
        }

        Map<String, Set<String>> addModsMap = new HashMap<>();
        Map<String, Set<String>> rmModsMap = new HashMap<>();

        if(obj.has("modifiers")) {
            JsonObject el = obj.getAsJsonObject("modifiers");
            if(el != null) {
                if(el.has("add")) {
                    JsonObject addObj = el.getAsJsonObject("add");
                    if(addObj != null) addModsMap = SharedCore.GSON.fromJson(addObj,mapStringSetType);
                }

                if(el.has("remove")) {
                    JsonObject rmObj = el.getAsJsonObject("remove");
                    if(rmObj != null) rmModsMap = SharedCore.GSON.fromJson(rmObj,mapStringSetType);
                }
            }
        }

        return new Effect(popMap,tilesTransferMap,addModsMap,rmModsMap);
    }

    public String toDataString() {
        StringBuilder popBuilder = new StringBuilder();
        StringBuilder tilesBuilder = new StringBuilder();
        StringBuilder modsAddBuilder = new StringBuilder();
        StringBuilder modsRemoveBuilder = new StringBuilder();

        for(var entry : this.popMap.entrySet()) {
            if(!popBuilder.isEmpty()) popBuilder.append(",");

            //"...;key1:value11|value12,key2:value21|value22

            popBuilder.append(entry.getKey()).append(":");
            popBuilder.append(entry.getValue());
        }


        for(var entry : this.tilesTransferMap.entrySet()) {
            if(!tilesBuilder.isEmpty()) tilesBuilder.append(",");

            //"...;key1:value11|value12,key2:value21|value22

            tilesBuilder.append(entry.getKey()).append(":");
            tilesBuilder.append(String.join("|", entry.getValue()));
        }


        for(var entry : this.modsAdd.entrySet()) {
            if(!modsAddBuilder.isEmpty()) modsAddBuilder.append(",");

            //"...;key1:value11|value12,key2:value21|value22

            modsAddBuilder.append(entry.getKey()).append(":");
            modsAddBuilder.append(String.join("|", entry.getValue()));
        }


        for(var entry : this.modsRemove.entrySet()) {
            if(!modsRemoveBuilder.isEmpty()) modsRemoveBuilder.append(",");

            //"...;key1:value11|value12,key2:value21|value22

            modsRemoveBuilder.append(entry.getKey()).append(":");
            modsRemoveBuilder.append(String.join("|", entry.getValue()));
        }

        return  popBuilder + ";" + tilesBuilder + ";" + modsAddBuilder + ";" + modsRemoveBuilder;
    }

    public static Effect fromDataString(String data) {
        String[] strings = data.split(";");

        if(strings.length != 4) return null;

        String popString = strings[0];
        String tilesString = strings[1];
        String modsAddString = strings[2];
        String modsRmString = strings[3];

        Map<String, Set<String>> modifierAddMap = new HashMap<>();

        for(String entry : modsAddString.split(",")) {
            String[] keyV = entry.split(":",2);

            if(keyV.length != 2) continue;

            String key = keyV[0];

            Set<String> modifs = Arrays.stream(keyV[1].split("\\|")).collect(Collectors.toSet());
            modifierAddMap.put(key,modifs);
        }

        Map<String, Set<String>> modifierRmMap = new HashMap<>();

        for(String entry : modsRmString.split(",")) {
            String[] keyV = entry.split(":",2);

            if(keyV.length != 2) continue;

            String key = keyV[0];

            Set<String> modifs = Arrays.stream(keyV[1].split("\\|")).collect(Collectors.toSet());
            modifierRmMap.put(key,modifs);
        }

        Map<String, Set<String>> tilesTransferMap = new HashMap<>();

        for(String entry : tilesString.split(",")) {
            String[] keyV = entry.split(":",2);

            if(keyV.length != 2) continue;

            String key = keyV[0];

            Set<String> tiles = Arrays.stream(keyV[1].split("\\|")).collect(Collectors.toSet());
            tilesTransferMap.put(key,tiles);
        }


        Map<String, Integer> popAddMap = new HashMap<>();

        for(String entry : tilesString.split(",")) {
            String[] keyV = entry.split(":",2);

            if(keyV.length != 2) continue;

            String key = keyV[0];

            int i = Integer.parseInt(keyV[1]);
            popAddMap.put(key,i);
        }

        return new Effect(popAddMap,tilesTransferMap,modifierAddMap, modifierRmMap);
    }
}
