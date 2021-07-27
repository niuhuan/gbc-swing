package gbc;

public class Cheat {

    public String code;
    public Integer address;
    public Byte ifIs;
    public Byte changeTo;

    public static Cheat newCheat(String code) {
        if (code == null) return null;
        /* II:IF  DD:DATA LL:LOW HH::HIGH */
        // RAW HHLLL:DD
        if (code.matches("^[0-9A-Fa-f]{4}:[0-9A-Fa-f]{2}$")) {
            Cheat cheat = new Cheat();
            cheat.code = code;
            cheat.address = Integer.parseInt(code.substring(0, 4), 16);
            cheat.changeTo = (byte) Integer.parseInt(code.substring(5, 7), 16);
            return cheat;
        }
        // RAW HHLL?II:DD
        if (code.matches("^[0-9A-Fa-f]{4}\\?[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}$")) {
            Cheat cheat = new Cheat();
            cheat.code = code;
            cheat.address = Integer.parseInt(code.substring(0, 4), 16);
            cheat.ifIs = (byte) Integer.parseInt(code.substring(5, 7), 16);
            cheat.changeTo = (byte) Integer.parseInt(code.substring(8, 10), 16);
            return cheat;
        }
        // GameShark 01DDLLHH
        if (code.matches("^01[0-9A-Fa-f]{6}$")) {
            Cheat cheat = new Cheat();
            cheat.code = code;
            cheat.address = Integer.parseInt(code.substring(6, 8) + code.substring(4, 6), 16);
            cheat.changeTo = (byte) Integer.parseInt(code.substring(2, 4), 16);
            return cheat;
        }
        // Codebreaker 00HHLL-DD
        if (code.matches("^00[0-9A-Fa-f]{4}-[0-9A-Fa-f]{2}$")) {
            Cheat cheat = new Cheat();
            cheat.address = Integer.parseInt(code.substring(2, 6), 16);
            cheat.changeTo = (byte) Integer.parseInt(code.substring(7, 9), 16);
            return cheat;
        }
        return null;
    }

}
