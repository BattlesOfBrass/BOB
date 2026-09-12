package de.idiotischer.bob.player;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// TODO: implement brain AND make it use the game speed in the period for the scheduler
public class ArtificalPlayer implements Player {
    private Country country;

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    private void think() {
        if(this.country == null) return;

        if(Server.getInstance().getWarManager().isAtWar(country)) {
            doWarLogic();
        } else {
            doPassiveDefense();
        }

        doTrainAndDeploy();
        doBuildIndustry();
        doFocuses();
        doDiplomacy();
    }

    private void doDiplomacy() {
    }

    private void doFocuses() {
    }

    private void doBuildIndustry() {
    }

    private void doTrainAndDeploy() {
    }

    private void doPassiveDefense() {
        //TODO: get neighboring countries andf mvoe all troops there and some to the beaches
    }

    private void doWarLogic() {
        //TODO: move troops to the enemy, but try to keep some at other borders
    }

    @Override
    public UUID uuid() {
        return UUID.randomUUID();
    }

    @Override
    public void uuid(UUID uuid) {}

    @Override
    public Country country() {
        return country;
    }

    @Override
    public void country(Country country) {
        this.country = country;
        if (country != null) {
            service.scheduleAtFixedRate(this::think, 0, 2, TimeUnit.SECONDS);
            country.setPlayer(this);
        }
        else service.shutdownNow();
    }
}
