package de.idiotischer.bob.networking;

import java.util.ArrayList;
import java.util.List;

public class BasicsFuerLukas {
    private String string = "op<kxjhf#";

    static void main(String[] args) {
        BasicsFuerLukas basics = new BasicsFuerLukas();
        IO.println(basics.string);
        // i wird auf 0 gesetzt, solange i < 6 i um 1 erhöhen udn code ausführen (läuft 5 mal)
        for(int i = 0; i < 6; i++) {}

        List<?> strings1 = new ArrayList<>();

        for(Object obj : strings1) {}

        List<String> strings = new ArrayList<>(List.of("a", "b", "c"));
        strings.add("d");

        for(int i = 0; i < 6; i++) {
            if(i > strings1.size()) continue;

            String s = strings.get(i);

            if(s == null) continue;

            IO.println(s);
        }

        //for(String s : strings){
        //   IO.println(s);
        //}
    }

    public BasicsFuerLukas() {
        //IO.println("Test");
    }

    public String getString() {
        return string;
    }
}
