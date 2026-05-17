package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//gets its own thread
public class ServerTroopManager {

    private final List<Troop> troops = new ArrayList<>();
    private final List<TroopStack> troopStacks = new ArrayList<>();

    public void addTroop(Troop troop) {
        troops.add(troop);

        updateStacks();
    }

    private void updateStacks() {
        troopStacks.clear();

        for (Troop troop : troops) {

            TroopStack stack = troopStacks.stream()
                    .filter(s ->
                            s.getTemplate().equals(troop.getTemplate())
                                    && s.getController().equals(troop.getController())
                                    && s.getState().equals(troop.getState()))
                    .findFirst()
                    .orElseGet(() -> {
                        TroopStack newStack = new TroopStack(new ArrayList<>());
                        troopStacks.add(newStack);
                        return newStack;
                    });

            stack.getTroops().add(troop);
        }
    }

    public List<TroopStack> getEnemy(Country country) {
        return List.of();
    }

    public List<TroopStack> getVisible(Country country) {
        return troopStacks.stream().filter(s -> s.getController() != null && s.getController().equals(country) && s.isVisible()).toList();
    }

    public List<TroopStack> getForController(Country country) {
        return troopStacks.stream().filter(s -> s.getController() != null && s.getController().equals(country)).toList();
    }

    public List<TroopStack> getTroopStacks() {
        return troopStacks;
    }

    public List<Troop> getTroops() {
        return troops;
    }
}
