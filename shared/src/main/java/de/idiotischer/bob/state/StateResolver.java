package de.idiotischer.bob.state;

public interface StateResolver {
    State byAbbreviation(String abbreviation);

    State fromPos(int x, int y);
}
