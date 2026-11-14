package de.idiotischer.bob.player.brain;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.player.tasks.Task;

//TODO: use country ai personalities
public abstract class BrainI {
    public String country;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public abstract void registerTask(Task task);

    public abstract Task getNextTask();
    public abstract Task getPreviousTask();
    public abstract Task getCurrentTask();
    public abstract void tick();
}
