package de.idiotischer.bob.troop;

public enum MoveStatus {
    SUCCESS(true),
    SUCCESS_PATHFIND(true),
    SUCCESS_NO_MOVE(true),
    FAILURE_FIGHT(false), // when we fight dont do anything the server handles
    FAILURE(false),
    FAILURE_STARTED_PATHFINDING(false), //tell the client that we started pathfinding and dont accept the move bc of that
    FAILURE_IN_COMBAT(false), //Cant move bc combat locked
    FAILURE_KICKED(false),
    FAILURE_NO_CONTROL(false);

    private final boolean booleanValue;

    MoveStatus(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public boolean getBooleanValue() {
        return booleanValue;
    }
}
