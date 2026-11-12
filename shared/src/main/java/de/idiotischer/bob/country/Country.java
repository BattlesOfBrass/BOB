package de.idiotischer.bob.country;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.ideology.Ideology;
import de.idiotischer.bob.ideology.IdeologyResolver;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;
import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.player.ServerPlayer;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.time.TimeResolver;
import de.idiotischer.bob.util.FileUtil;
import it.unimi.dsi.fastutil.Pair;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Country /*implements Comparable<Country>*/{

    private final String name;
    private final Color color;
    private final String abbreviation;
    private final boolean selectScreen;
    private boolean major;
    private Player player = null;
    private PuppetState puppetState;
    private int puppetProgress = 0; //in prozent (0-100) bzw maybe als float idk
    private Set<State> states = new HashSet<>();
    private boolean capitulated;
    private String overlord;
    private Set<String> milAccess = new HashSet<>();
    private double politicalPower = 0;

    private final Set<Modifier> modifiers;

    private Conscription conscription = Conscription.LIMITED_CONSCRIPTION;

    public Map<String, Consumer<Modifier>> modifierAddListeners = new HashMap<>();
    public Map<String, Consumer<Modifier>> modifierRemoveListeners = new HashMap<>();

    private Ideology rulingIdeology;
    private Ideology startIdeology;
    private Map<Ideology,Float> availableIdeologies = new HashMap<>(); //used for parties
    private long manpower;

    public Country(String abbreviation,String name, Color color, boolean major, boolean selectScreen, String overlord, Set<Modifier> modifiers) {
        this.abbreviation = abbreviation;
        this.name = name;
        this.color = color;
        this.major = major;
        this.selectScreen = selectScreen;
        this.capitulated = false;
        this.modifiers = modifiers;
        this.overlord = overlord;
    }

    //später nicht die default sondern die current flag returnen
    public Path getFlag(Scenario scenario) {
        return FileUtil.getFlag(scenario,this);
    }

    private final Map<Scenario, BufferedImage> flagCache = new HashMap<>();

    public BufferedImage getFlagImage(Scenario scenario) {
        if (scenario == null) return null;

        if (flagCache.containsKey(scenario)) return flagCache.get(scenario);

        var path = FileUtil.getFlag(scenario, this);
        if (path == null) return null;

        try {
            BufferedImage img = ImageIO.read(path.toFile());
            if (img != null) flagCache.put(scenario, img);
            return img;
        } catch (IOException e) {
            return null;
        }
    }

    public Path getDefaultFlag(Scenario scenario) {
        return FileUtil.getFlag(scenario, this);
    }

    public Color countryColor() {
        return color;
    }

    public String countryName() {
        return name;
    }

    // --> testweise immer gleich
    public Relations countryRelations() {
        return null;
    }

    //later add ideology thingy to the abbreviation (make a sanatized and unsanaitzed or smth that allows for civil wars betrween the same ideology and yeah, cosmetictags or by leader or smth)
    public String getAbbreviation() {
        return abbreviation;
    }

    public static Country fromJson(String json) {
        return null;
    }

    public PuppetState getPuppetState() {
        return puppetState;
    }

    /*wie nah oder nicht nah man am puppet werden, bzw am level demoten ist*/
    public int getPuppetProgress() {
        return puppetProgress; //nicht puppetable
    }

    public boolean isAutonomous() {
        return getPuppetState() == null || getPuppetState() == PuppetState.NONE;
    }

    public boolean isMajor() {
        return major;
    }

    public boolean isSelectScreen() {
        return selectScreen;
    }

    public void setMajor(boolean major) {
        this.major = major;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setOverlord(String country) {
        this.overlord = country;
    }

    public void setOverlordServer(String country, SendTool t, ChannelResolver r) {
        this.overlord = country;

        t.broadcast(r.channels(), new ReplyPacket(Type.COUNTRY_UPDATE, "id=" + abbreviation + ";" + "overlord=" + overlord));
    }

    public void setPuppetTile(PuppetState tile) {
        this.puppetState = tile;
    }

    public void setPuppetProgress(int progress) {
        this.puppetProgress = progress;
    }

    public void writeCountry(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);
        util.putUtf(name);
        util.putUtf(abbreviation);

        Color c = color;
        buffer.putLong(manpower);
        buffer.put((byte) c.getRed());
        buffer.put((byte) c.getGreen());
        buffer.put((byte) c.getBlue());

        buffer.put((byte) (major ? 1 : 0));
        buffer.put((byte) (selectScreen ? 1 : 0));

        if (getPlayer() == null || getPlayer().uuid() == null) util.putUuid(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        else util.putUuid(getPlayer().uuid());

        util.putEnum(getPuppetState() == null ? PuppetState.NONE : getPuppetState());
        //buffer.put((byte) (getPuppetTile() == null ? -1 : getPuppetTile().ordinal()));

        util.putUtf(overlord);
        buffer.putInt(getPuppetProgress());

        buffer.putInt(modifiers.size());
        for (Modifier mod : modifiers) util.putUtf(mod.getId());

        util.putBoolean(isCapitulated());
        util.putUtf(conscription.name().toUpperCase());
        util.putUtf(rulingIdeology == null ? "" : rulingIdeology.abbreviation());
        util.putUtf(startIdeology == null ? "" : startIdeology.abbreviation());

        buffer.putInt(milAccess.size());
        for (String country : milAccess) util.putUtf(country);

        buffer.putInt(availableIdeologies.size());

        for (Map.Entry<Ideology, Float> entry : availableIdeologies.entrySet()) {
            util.putUtf(entry.getKey().abbreviation());
            buffer.putFloat(entry.getValue());
        }
    }

    public static Country readCountry(ModifierResolver mr, IdeologyResolver r, ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        String name = util.getUtf();
        String abbreviation = util.getUtf();

        long manpower = buffer.getLong();

        Color color = new Color(buffer.get() & 0xFF, buffer.get() & 0xFF, buffer.get() & 0xFF);

        boolean major = buffer.get() == 1;
        boolean selectScreen = buffer.get() == 1;

        UUID uuid = util.getUuid();
        if (uuid != null && uuid.equals(new UUID(0L, 0L))) uuid = null;

        PuppetState puppetTile = util.getEnum(PuppetState.class);
        if (puppetTile == PuppetState.NONE) puppetTile = null;

        String overlord = util.getUtf();
        int puppetProgress = buffer.getInt();

        Set<Modifier> mods = new HashSet<>();
        int modsSize = buffer.getInt();
        for (int i = 0; i < modsSize; i++) mods.add(mr.by(util.getUtf()));

        Country country = new Country(abbreviation, name, color, major, selectScreen, overlord, mods);

        country.setManpower(manpower);
        country.setPlayer(Player.of(uuid));
        country.setPuppetTile(puppetTile);
        country.setPuppetProgress(puppetProgress);
        country.setCapitulated(util.getBoolean());
        Conscription conscription = Conscription.valueOf(util.getUtf());
        Ideology ideology1 = r.byAbbreviation(util.getUtf());
        Ideology ideology2 = r.byAbbreviation(util.getUtf());
        country.setConscription(conscription);

        int milAccessSize = buffer.getInt();
        for (int i = 0; i < milAccessSize; i++) country.addMilAccess(util.getUtf());

        int ideologySize = buffer.getInt();

        for (int i = 0; i < ideologySize; i++) {
            String ideology = util.getUtf();
            float weight = buffer.getFloat();

            country.getAvailableIdeologies().put(r.byAbbreviation(ideology), weight);
        }

        country.setRulingIdeologySimple(ideology1);
        country.setDefaultIdeologySimple(ideology2);

        return country;
    }

    public void setConscription(Conscription conscription) {
        this.conscription = conscription;
    }

    public void setConscriptionServer(SharedCore c, ChannelResolver r, Conscription conscription) {
        this.conscription = conscription;

        c.getTool().broadcast(r.channels(), new ReplyPacket(Type.COUNTRY_UPDATE, "id=" + this.abbreviation + ";" + "conscription=" + conscription.name().toUpperCase()));
    }

    public Set<String> getMilAccess() {
        return milAccess;
    }

    public Ideology getStartIdeology() {
        return startIdeology;
    }

    private void setDefaultIdeologySimple(Ideology ideology1) {
        this.startIdeology = ideology1;
    }

    @Override
    public String toString() {
        return "Country{" +
                "name='" + name + '\'' +
                ", color={" + "red=" + color.getRed() + ", green=" + color.getGreen() + ", blue=" + color.getBlue() + "}" +
                ", abbreviation='" + abbreviation + '\'' +
                ", major=" + major +
                ", selectScreen=" + selectScreen +
                ", player=" + (getPlayer() == null ? "null" : getPlayer().uuid() == null ? "null" : getPlayer().uuid().toString()) +
                ", getPuppetProgress=" + getPuppetProgress() +
                ", puppetTile=" + (getPuppetState() == null ? "null" : getPuppetState().name()) +
                ", capitulated=" + isCapitulated() +
                /*", autonomous=" + isAutonomous() + wird im client bestimmt*/
                '}';
    }

    public void addState(State state) {
        states.add(state);
    }

    public Set<State> getStates() {
        return states;
    }

    public boolean isCapitulated() {
        return capitulated;
    }

    public void setCapitulated(boolean b) {
        this.capitulated = b;
    }

    public void setCapitulated(boolean b, Consumer<Void> consumer) {
        this.capitulated = b;
        consumer.accept(null);
    }

    public Country getOverlord(CountryResolver r) {
        return r.byAbbreviation(overlord);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Country other)) return false;

        return Objects.equals(abbreviation, other.abbreviation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(abbreviation);
    }

    public void setStartIdeologySimple(Ideology startIdeology) {
        this.startIdeology = startIdeology;
    }

    public void setStartIdeology(SharedCore c, ChannelResolver r, Ideology startIdeology) {
        c.getTool().broadcast(r.channels(), new ReplyPacket(Type.IDEOLOGY_UPDATE, "start=" + this.abbreviation + ";" + startIdeology.abbreviation()));

        this.startIdeology = startIdeology;
    }

    public void addAvailableIdeologies(SharedCore c, ChannelResolver r, Ideology ideology) {
        c.getTool().broadcast(r.channels(), new ReplyPacket(Type.IDEOLOGY_UPDATE, "add=" + this.abbreviation + ";" + ideology.abbreviation()));

        availableIdeologies.put(ideology,0f);
    }

    public void addAvailableIdeologiesSimple(Ideology ideology) {
        availableIdeologies.put(ideology,0f);
    }

    public void rmAvailableIdeologies(SharedCore c, ChannelResolver r, Ideology ideology) {
        c.getTool().broadcast(r.channels(), new ReplyPacket(Type.IDEOLOGY_UPDATE, "remove=" + this.abbreviation + ";" + ideology.abbreviation()));

        availableIdeologies.remove(ideology,0f);
    }

    public void rmAvailableIdeologiesSimple(Ideology ideology) {
        availableIdeologies.remove(ideology,0f);
    }

    public Map<Ideology, Float> getAvailableIdeologies() {
        return availableIdeologies;
    }

    public Ideology getRulingIdeology() {
        return rulingIdeology;
    }

    public String getRulingIdeologyString() {
        return rulingIdeology == null ? "None" : rulingIdeology.name();
    }

    public void setRulingIdeology(SharedCore c, ChannelResolver r, Ideology rulingIdeology) {
        c.getTool().broadcast(r.channels(), new ReplyPacket(Type.IDEOLOGY_UPDATE, "ruling=" + this.abbreviation + ";" + rulingIdeology.abbreviation()));

        this.rulingIdeology = rulingIdeology;
    }

    public void setRulingIdeologySimple(Ideology rulingIdeology) {
        this.rulingIdeology = rulingIdeology;
    }

    public void removeMilAccess(String country) {
        milAccess.remove(country);
    }

    public void addMilAccess(String country) {
        milAccess.add(country);
    }

    public boolean hasCountryMilAccess(Country country) {
        return milAccess.contains(country.getAbbreviation()); //TODO: or is in faction
    }

    public String serializeAccessUpdate(String abbr, boolean added) {
        return this.getAbbreviation() + ";" +abbr + ";" + added;
    }

    public static Pair<String, Pair<String, Boolean>> getAccessUpdate(String s) {
        String[] strings = s.split(";");

        String cAbbr = strings[0];
        String milCAbbr = strings[1];
        boolean added = Boolean.parseBoolean(strings[2]);

        return Pair.of(cAbbr, Pair.of(milCAbbr,added));
    }

    //called on a new dawn (hihi sosand)
    public void newDay(ChannelResolver r, SendTool t, long dailyMP) {
        this.politicalPower += getDailyPP();

        addManpowerServer(t,r, dailyMP);

        t.broadcast(r.channels(), new ReplyPacket(Type.PP_UPDATE, this.abbreviation + ";" + politicalPower));
    }

    public double getDailyPP() {
        Set<Modifier> ppMods = modifiers.stream().filter(modifier -> modifier.getType().equals(Modifier.Types.POLITICAL_POWER.getMapping())).collect(Collectors.toSet());

        double base = 1;

        double modifier = ppMods.stream().flatMapToDouble(mod -> {
            try {
                double d = Double.parseDouble(mod.getModifier());
                return DoubleStream.of(d);
            } catch (Exception e) {
                return DoubleStream.of(0D);
            }
        }).sum();

        return base * (1 + modifier);
    }

    public void removeModifier(String id) {
        Modifier modifier = modifiers.stream().filter(mod -> mod.getId().equals(id)).findFirst().orElse(null);

        if(modifier == null) return;

        removeModifier(modifier);
    }

    public Set<String> getCountryTags() {
        return modifiers.stream().map(Modifier::getModifier).filter(this::isOnlyString).collect(Collectors.toSet());
    }

    public boolean isOnlyString(String s) {
        if(s == null || s.isBlank()) return false;

        String trim = s.trim();

        if(trim.equalsIgnoreCase("true") || trim.equalsIgnoreCase("false")) return false;

        try {
            Double.parseDouble(s);
            return false;
        } catch (NumberFormatException ignored) {}

        return true;
    }


    public void removeModifierServer(SendTool t, ChannelResolver r, String id) {
        Modifier modifier = modifiers.stream().filter(mod -> mod.getId().equals(id)).findFirst().orElse(null);

        if(modifier == null) return;

        removeModifierServer(t,r,modifier);
    }

    public void removeModifier(Modifier modifier) {
        this.modifiers.remove(modifier);

        for (var entry : modifierRemoveListeners.entrySet()) entry.getValue().accept(modifier);
    }

    public void removeModifierServer(SendTool t, ChannelResolver r, Modifier modifier) {
        this.modifiers.remove(modifier);

        for(var entry : modifierRemoveListeners.entrySet()) entry.getValue().accept(modifier);

        t.broadcast(r.channels(),new ReplyPacket(Type.MODIFIERS_UPDATE, "id=" + this.abbreviation + "remove=" + modifier.getId()));
    }

    public void addModifier(Modifier modifier, TimeResolver r) {
        if(modifier.getType().equals(Modifier.Types.REMOVE_MODIFIER.getMapping())) {
            removeModifier(modifier.getModifier());
            return;
        }

        r.addTimeHook("modifier-" + UUID.randomUUID(), modifier.getLasts(), integer -> removeModifier(modifier));

        this.modifiers.add(modifier);

        for(var entry : modifierAddListeners.entrySet()) entry.getValue().accept(modifier);
    }

    public void addModifierServer(SendTool t, ChannelResolver r, Modifier modifier, TimeResolver res) {
        if(modifier.getType().equals(Modifier.Types.REMOVE_MODIFIER.getMapping())) {
            removeModifierServer(t,r,modifier.getModifier());
            return;
        }

        res.addTimeHook("modifier-" + UUID.randomUUID(), modifier.getLasts(), integer -> removeModifier(modifier));

        this.modifiers.add(modifier);

        for(var entry : modifierAddListeners.entrySet()) entry.getValue().accept(modifier);

        t.broadcast(r.channels(),new ReplyPacket(Type.MODIFIERS_UPDATE, "id=" + this.abbreviation + "add=" + modifier.getId()));
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public double getPoliticalPower() {
        return politicalPower;
    }

    public void setPoliticalPower(double pp, Country c) {
        this.politicalPower = pp;
    }

    public Country(Country c, String abbr) {
        this.politicalPower = c.politicalPower;
        this.modifiers = c.modifiers;
        this.abbreviation = abbr;
        this.availableIdeologies = c.availableIdeologies;
        this.capitulated = c.capitulated; //False
        this.color = c.color;
        this.major = c.major;
        this.selectScreen = false; //FALSE FR
        this.milAccess = c.milAccess;
        this.name = c.name;
        this.overlord = c.overlord; //maybe remove him for both?
        this.player = c.player;
        this.puppetProgress = c.puppetProgress;
        this.puppetState = c.puppetState;
        this.rulingIdeology = c.rulingIdeology;
        this.startIdeology = c.startIdeology; //in civil wars start Ideology gets set to whatever start IDeology they have
        this.states = c.states;
        this.manpower = c.manpower;
    }


    public Country uniqueCopy(String abbreviation) {
        return new Country(this,abbreviation);
    }

    public void setManpowerServer(SendTool t, ChannelResolver r, long sum) {
        this.manpower = sum;
        t.broadcast(r.channels(),new ReplyPacket(Type.COUNTRY_UPDATE, "id=" + this.abbreviation + ";" + "manpower=" + sum));
    }

    public void setMajorServer(SendTool t, ChannelResolver r, boolean b) {
        this.major = b;
        t.broadcast(r.channels(),new ReplyPacket(Type.COUNTRY_UPDATE, "id=" + this.abbreviation + ";" + "major=" + b));
    }

    public void addManpowerServer(SendTool t, ChannelResolver r, long d) {
        long sum = getManpower() + d;
        this.manpower = sum;
        t.broadcast(r.channels(), new ReplyPacket(Type.COUNTRY_UPDATE, "id=" + this.abbreviation + ";" + "manpower=" + sum));
    }

    public void setManpower(long sum) {
        this.manpower = sum;
    }

    public long getManpower() {
        return manpower;
    }

    //@Override
    //public int compareTo(@NonNull Country o) {
    //    return this.abbreviation.compareTo(o.getAbbreviation());
    //}

    public long getDailyManpower(double totalPop) {
        if (conscription == null)  {
            System.out.println("conscription is null");
            return 0;
        }

        long extraPop = getModifiers().stream().filter(m -> m.getType().equals(Modifier.Types.DAILY_MANPOWER.getMapping()))
                .mapToLong(m -> {
                    try {
                        return Long.parseLong(m.getModifier());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }).sum();

        double extraPopPerc = getModifiers().stream().filter(m -> m.getType().equals(Modifier.Types.DAILY_MANPOWER_PERCENT.getMapping()))
                .mapToDouble(m -> {
                    try {
                        return Double.parseDouble(m.getModifier());
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                }).sum();

        double recruitable = getRecruitablePop();

        double baseManpower = (totalPop * recruitable) + extraPop;
        double finalManpower = baseManpower * (1.0 + extraPopPerc);

        return (long) Math.floor(finalManpower);
    }

    public double getRecruitablePop() {
        return getModifiers().stream().filter(m -> m.getType().equals(Modifier.Types.RECRUITABLE_POP.getMapping()))
                .mapToDouble(m -> {
                    try {
                        return Double.parseDouble(m.getModifier());
                    } catch (NumberFormatException e) {return 0.0;}
                }).sum() + conscription.getRecruitablePopulation();
    }

    public Conscription getConscription() {
        return conscription;
    }

    public enum Conscription {
        DISARMED_NATION(0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
        VOLUNTEER_ONLY(0.015, 0.0, 0.0, 0.0, 0.0, 0.0),
        LIMITED_CONSCRIPTION(0.025, 0.0, 0.0, 0.0, 0.0, 0.0),
        EXTENSIVE_CONSCRIPTION(0.05, 0.0, 0.10, 0.0, 0.0, 0.0),
        SERVICE_BY_REQUIREMENT(0.10, 0.0, 0.20, -0.10, -0.10, -0.10),
        ALL_ADULTS_SERVE(0.20, 0.0, 0.30, -0.30, -0.30, -0.30),
        SCRAPPING_THE_BARREL(0.25, 0.0, 0.50, -0.40, -0.40, -0.40);

        private final double recruitablePopulation;
        private final double mobilizationSpeed;
        private final double trainingTime;
        private final double factoryOutput;
        private final double dockyardOutput;
        private final double constructionSpeed;

        Conscription(double recruitablePopulation, double mobilizationSpeed, double trainingTime, double factoryOutput, double dockyardOutput, double constructionSpeed) {
            this.recruitablePopulation = recruitablePopulation;
            this.mobilizationSpeed = mobilizationSpeed;
            this.trainingTime = trainingTime;
            this.factoryOutput = factoryOutput;
            this.dockyardOutput = dockyardOutput;
            this.constructionSpeed = constructionSpeed;
        }

        public double getRecruitablePopulation() {
            return recruitablePopulation;
        }

        public double getMobilizationSpeed() {
            return mobilizationSpeed;
        }

        public double getTrainingTime() {
            return trainingTime;
        }

        public double getFactoryOutput() {
            return factoryOutput;
        }

        public double getDockyardOutput() {
            return dockyardOutput;
        }

        public double getConstructionSpeed() {
            return constructionSpeed;
        }
    }
}
