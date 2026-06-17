package de.idiotischer.bob.networking.packet.impl.pp;

public enum Type {
    TILE_CHANGE,
    ERROR,
    TILES_SYNC,
    COUNTRIES_SYNC,
    SCENARIO_LOAD,
    SCENARIO_SYNC,
    GAMESTATE_SYNC,
    PLAYER_CHANGE,
    SCENARIOS,
    STATES_SYNC,
    STATE_SYNC,
    TROOPS_SYNC,
    TROOPS_MOVE,
    TROOP_REMOVE,
    START_WAR,
    END_WAR,
    CAPITULATE_COUNTRY,
    COMBAT_OVER,
    CLEAR_COMBATS, WARS_SYNC
}