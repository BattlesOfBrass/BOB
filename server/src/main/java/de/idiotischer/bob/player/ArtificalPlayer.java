package de.idiotischer.bob.player;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.game.GameState;
import de.idiotischer.bob.player.brain.BrainI;
import de.idiotischer.bob.player.brain.impl.BasicBrain;
import de.idiotischer.bob.player.tasks.impl.TroopStationTask;
import de.idiotischer.bob.player.tasks.impl.TroopTrainTask;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// TODO: implement brain AND make it use the game speed in the period for the scheduler
public class ArtificalPlayer implements Player {

    private final CountryResolver r;

    private String country;

    private BrainI brain = new BasicBrain();

    public ArtificalPlayer(CountryResolver r) {
        this.r = r;

        brain.registerTask(new TroopStationTask());
        brain.registerTask(new TroopTrainTask());
    }

    public void think() {
        if(this.country == null) return;
        if(Server.getInstance().getGameManager().getState() == GameState.PAUSED || Server.getInstance().getGameManager().getState() == GameState.LOBBY) return;

        brain.setCountry(country);

        brain.tick();
    }

    @Override
    public UUID uuid() {
        return UUID.randomUUID();
    }

    @Override
    public void uuid(UUID uuid) {}

    @Override
    public Country country() {
        return r.byAbbreviation(country);
    }

    @Override
    public boolean isAI() {return true;}

    @Override
    public void country(String country) {
        this.country = country;

        Country c = country();
        if (c != null) c.setPlayer(this);
    }

    @Override
    public boolean hasOP() {
        return false;
    }

    public BrainI getBrain() {
        return brain;
    }

    public void setBrain(BrainI brain) {
        this.brain = brain;
    }
}
