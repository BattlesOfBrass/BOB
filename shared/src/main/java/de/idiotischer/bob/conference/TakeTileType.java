//package de.idiotischer.bob.conference;
//
//public class TakeTileType {
//
//    public void register(String string) {
//        this.types.add(string);
//    }
//
//    public void unRegister(String string) {
//        this.types.add(string);
//    }
//}

package de.idiotischer.bob.conference;

//TODO: gonna use an enum for now till someone needs this xD
public enum TakeTileType {
    GIVE("GVE"),
    PUPPET("PPT"),
    TAKE("TKE"),
    LIBERATE("LIB");

    private final String charr;

    TakeTileType(String charr) {
        this.charr = charr;
    }

    public String getDefaultCharr() {
        return charr;
    }
}