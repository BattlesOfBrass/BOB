package de.idiotischer.bob.focus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import it.unimi.dsi.fastutil.Pair;
import org.graalvm.collections.EconomicMap;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerFocusManager implements FocusResolver {

    private final Set<FocusTree> trees = new HashSet<>();
    private Map<String,Pair<String,String>> activeFocuses = new HashMap<>();

    public void reload() {
        Path focusTrees = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getFocusesDir();

        //if(Files.exists(globalFocuses)) loadFocuses(Set.of(globalFocuses));

        if(Files.exists(focusTrees)) {
            Set<Path> focusJsons = new HashSet<>();

            try {
                Files.walk(focusTrees).forEach(path -> {
                    if(!Files.isRegularFile(path)) return;

                    if(path.endsWith(".json")) focusJsons.add(path);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            loadFocuses(focusJsons);
            loadTrees(focusJsons);
        }
    }

    private void loadTrees(Set<Path> focusJsons) {
        focusJsons.forEach(path -> {
            if(Files.notExists(path)) return;

            try (JsonReader reader = new JsonReader(Files.newBufferedReader(Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getCountryConfig()))) {
                JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

                root.getAsJsonObject().entrySet().forEach(entry -> {
                    String id = entry.getKey();
                    JsonObject element = entry.getValue().getAsJsonObject();

                    String requiredTag = getAsStringSafe(element,"requiredTag");

                    Set<String> countries = new HashSet<>();

                    JsonElement modsElement = element.get("countries");

                    if(modsElement != null && modsElement.isJsonArray()) {
                        JsonArray array = modsElement.getAsJsonArray();

                        array.forEach(modifier -> countries.add(modifier.getAsString()));
                    }

                    Map<String, Set<String>> alreadyDone = new HashMap<>();

                    JsonElement alreadyDoneElement = element.get("alreadyDone");

                    if(alreadyDoneElement != null) {
                        alreadyDoneElement.getAsJsonObject().entrySet().forEach(ent -> {
                            String focus = ent.getKey();

                            Set<String> countriesS = new HashSet<>();

                            if(ent.getValue() != null && ent.getValue().isJsonArray()) {
                                JsonArray array = ent.getValue().getAsJsonArray();

                                array.forEach(el -> countriesS.add(el.getAsString()));
                            }

                            alreadyDone.put(focus,countriesS);
                        });
                    }

                    Set<Focus> focuses = new HashSet<>();

                    JsonElement focusesElement = element.get("focuses");

                    if(element.has("focuses") && !element.get("focuses").isJsonNull()) {
                        focuses = loadFocuses(focusesElement,alreadyDone);
                    }

                    FocusTree tree = new FocusTree(requiredTag,countries,focuses,id);

                    trees.add(tree);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadFocuses(Set<Path> focusJsons) {

    }

    private Set<Focus> loadFocuses(JsonElement jsonElement, Map<String, Set<String>> alreadyDone) {
        Set<Focus> focuses = new HashSet<>();
        jsonElement.getAsJsonObject().entrySet().forEach(el -> {
            String id = el.getKey();
            JsonObject element = el.getValue().getAsJsonObject();

            String prerequisite = getAsStringSafe(element, "prerequisite");
            String description = getAsStringSafe(element, "description");
            String name = getAsStringSafe(element, "name");
            int time = getAsIntSafe(element,"time");
            float ppDebuff = getAsFloatSafe(element,"ppLoss");

            int x = 0;
            int y = 0;

            if(element.has("x")) {
                String s = element.get("x").getAsString();

                x = Integer.parseInt(s);
            }

            if(element.has("y")) {
                String s = element.get("y").getAsString();

                y = Integer.parseInt(s);
            }

            if(element.has("point")) {
                String[] strings = element.get("point").getAsString().split("[,;]");

                x = Integer.parseInt(strings[0]);
                y = Integer.parseInt(strings[1]);
            }

            Set<Modifier> mods = new HashSet<>();

            JsonElement modsElement = element.get("effects");

            if(modsElement != null && modsElement.isJsonArray()) {
                JsonArray array = modsElement.getAsJsonArray();

                array.forEach(modifier -> mods.add(Server.getInstance().getModifierManager().by(modifier.getAsString())));
            }

            Set<Modifier> visibleWhen = new HashSet<>();

            JsonElement visibleWhenElement = element.get("visibleWhen");

            if(visibleWhenElement != null && visibleWhenElement.isJsonArray()) {
                JsonArray array = visibleWhenElement.getAsJsonArray();

                array.forEach(modifier -> visibleWhen.add(Server.getInstance().getModifierManager().by(modifier.getAsString())));
            }

            Map<String, Set<Modifier>> effectsFor = new HashMap<>();

            JsonElement effectsForElement = element.get("effectsFor");

            if(effectsForElement != null) {
                effectsForElement.getAsJsonObject().entrySet().forEach(ent -> {
                    String country = ent.getKey();

                    Set<Modifier> countryMods = new HashSet<>();

                    if(ent.getValue() != null && ent.getValue().isJsonArray()) {
                        JsonArray array = ent.getValue().getAsJsonArray();

                        array.forEach(modifier -> countryMods.add(Server.getInstance().getModifierManager().by(modifier.getAsString())));
                    }

                    effectsFor.put(country,countryMods);
                });
            }

            Set<String> countries = alreadyDone.get(id);

            Focus focus = new Focus(id,name,countries,effectsFor,visibleWhen,time,ppDebuff, new Point(x,y), prerequisite,mods,description);

            focuses.add(focus);
        });

        return focuses;
    }

    public boolean takeFocus(Country c, String id) {
        FocusTree tree = getFocusTree(c);

        Focus focus = focusBy(tree.id(),id);

        return true;
    }

    private String getAsStringSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        return el.getAsString();
    }

    private int getAsIntSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
        return el.getAsInt();
    }


    private float getAsFloatSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
        return el.getAsFloat();
    }

    public Set<FocusTree> getTrees() {
        return trees;
    }

    @Override
    public FocusTree getFocusTree(Country c) {
        //sorted may throw
        return trees.stream().filter(tr -> tr.hasTree(c)).sorted().findFirst().orElse(null);
    }

    @Override
    public Set<Country> getWithFocusTree(FocusTree f) {
        return Server.getInstance().getCountryManager().getCountries().stream().filter(f::hasTree).collect(Collectors.toSet());
    }

    @Override
    public Focus focusBy(String focusTree, String id) {
        return treeBy(focusTree).focuses().stream().filter(f -> f.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public FocusTree treeBy(String id) {
        return trees.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public Set<Focus> getFor(FocusTree tree) {
        return tree.focuses();
    }

    @Override
    public Focus getActiveFocus(Country c) {
        var pair = activeFocuses.get(c.getAbbreviation());
        return focusBy(pair.key(),pair.value());
    }

    public void remove(FocusTree tree) {
        this.trees.remove(tree);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().channels(), new ReplyPacket(Type.FOCUSTREE_REMOVE, tree.id()));
    }

    public void startFocus(String country, String id, String treeId) {
        Focus foc = focusBy(treeId,id);
        foc.startFocusServer(Server.getInstance().getServerSocket(), Server.getInstance().getSendTool(),treeId,country);

        activeFocuses.put(country, Pair.of(id,treeId));

        Server.getInstance().getGameManager().addTimeHook(treeId + "-" + id + "-" + country, foc.getTime(), (i) -> {
            endFocus(country, id, treeId);
        });
    }

    public void cancelFocus(String country, String id, String treeId) {
        Focus foc = focusBy(treeId,id);
        foc.cancelFocusServer(Server.getInstance().getServerSocket(), Server.getInstance().getSendTool(),treeId,country);

        activeFocuses.remove(country);

        Server.getInstance().getGameManager().removeTimeHook(treeId + "-" + id + "-" + country);
    }

    public void endFocus(String country, String id, String treeId) {
        focusBy(treeId,id).endFocusServer(Server.getInstance().getCountryManager(), Server.getInstance().getServerSocket(), Server.getInstance().getSendTool(),treeId,country, Server.getInstance().getGameManager());

        activeFocuses.remove(country);
    }
}
