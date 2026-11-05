package de.idiotischer.bob.troop;

import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TrainEntry {

    private final long startTime;
    private double availablePop;
    private int progress;
    private final UUID uuid;
    private final int troopTrainDays;
    private TroopStack finalStack;

    private double lastAvailablePop;

    public long extra = 0;

    //progress is as long as troopTrainDays, so NOT 100 xD
    public TrainEntry(long startTime, int progress, UUID uuid, double availablePop, int troopTrainDays, TroopStack finalStack) {
        this.startTime = startTime;
        this.progress = progress;
        this.uuid = uuid;
        this.troopTrainDays = troopTrainDays;
        this.finalStack = finalStack;
        this.availablePop = availablePop;
        this.lastAvailablePop = availablePop;
    }

    public TroopStack getFinalStack() {
        return finalStack;
    }

    public int getExpIfDeployedNow(long now) {
        long elapsedTicks = Math.max(0, now - startTime - extra);

        double elapsedDays = elapsedTicks / 1440.0;
        double timePercentage = troopTrainDays <= 0 ? 100 : ((progress + elapsedDays) / troopTrainDays) * 100;

        long requiredPop = finalStack.getPop();

        double populationPercentage = requiredPop <= 0 ? 100 : Math.min(1.0, (double) availablePop / requiredPop);

        double effectivePercentage = timePercentage * populationPercentage;

        return Math.clamp((int) Math.floor(effectivePercentage), 0, 100);
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getTroopTrainDays() {
        return troopTrainDays;
    }

    public double getStartTime() {
        return startTime;
    }

    public String getTroopName() {
        return finalStack.getName();
    }

    public String getTroopTemplate() {
        return finalStack.getTemplate();
    }

    public boolean canTrain() {
        //blabla

        boolean result = true;

        if(availablePop >= finalStack.getPop()) result = true;

        double lastAvailablePopStamp = lastAvailablePop;

        this.lastAvailablePop = availablePop;

        if(lastAvailablePopStamp >= availablePop) result = false;

        if(!result) extra++;

        return result;
    }

    private void addAvailablePop(long availablePop) {
        this.availablePop += availablePop;
    }

    public boolean canTrainServer(ChannelResolver r, SendTool t, double availablePop) {
        //blabla
        boolean result = true;
        //for some reason percentage still goes up but on server its correct

        if(availablePop >= finalStack.getPop()) result = true;

        if(availablePop >= 0) result = false;

        //TODO: add a conscription cap etc so you cant just conscript all POP and pop aint just taken away but instead manpower is created
        addAvailablePopServer(r,t,availablePop);

        if(!result) extra++;

        return result;
    }

    private void addAvailablePopServer(ChannelResolver r, SendTool t, double availablePop) {
        this.availablePop += availablePop;
        finalStack.setPopulation(availablePop);
        t.broadcast(r.channels(), new ReplyPacket(Type.TROOP_TRAIN_UPDATE, "id=" + this.uuid.toString() + ";pop=" + this.availablePop));
    }

    public double getEndTick() {
        return startTime + TimeUnit.DAYS.toMinutes(troopTrainDays) + extra;
    }

    public void setPop(long full) {
        this.availablePop = full;
    }
}
