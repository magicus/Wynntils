package com.wynntils.modules.core.events;

public class MusicNote {
    private final Instrument instr;
    private final float pitch;

    public MusicNote(Instrument instr, float pitch) {
        this.instr = instr;
        this.pitch = pitch;
    }

    public MusicNote(String instrName, float pitch) {
        this(Instrument.fromString(instrName), pitch);
    }

    public Instrument getInstrument() {
        return instr;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean match(String instr, float pitch) {
        return Instrument.fromString(instr) == this.instr && pitch == this.pitch;
    }

    public enum Instrument {
        BASEDRUM("basedrum"),
        BASS("bass"),
        BELL("bell"),
        CHIME("chime"),
        FLUTE("flute"),
        GUITAR("guitar"),
        HARP("harp"),
        HAT("hat"),
        PLING("pling"),
        SNARE("snare"),
        XYLOPHONE("xylophone");

        private final String name;

        Instrument(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Instrument fromString(String name) {
            return valueOf(name.toUpperCase());
        }
    }
}
