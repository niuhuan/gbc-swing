package gbc;

class Cartridge {

    protected final byte[][] rom;
    protected final int type;
    protected final boolean colored;
    protected final boolean hasBattery;
    protected final byte[][] ram;
    protected final byte[] rtcReg;
    protected int lastRtcUpdate;

    public Cartridge(byte[] bin) {
        this.rom = loadRom(bin);
        this.type = loadType(bin);
        this.colored = loadColored(bin);
        this.hasBattery = loadHasBattery(bin);
        this.ram = loadRam(bin);
        this.rtcReg = new byte[5];
        this.lastRtcUpdate = (int) System.currentTimeMillis();
    }

    private static final byte[][] loadRom(byte[] bin) {
        /** Translation between ROM size byte contained in the ROM header, and the number
         *  of 16kB ROM banks the cartridge will contain
         */
        int cartRomBankNumber;
        int sizeByte = bin[0x0148];
        if (sizeByte < 8)
            cartRomBankNumber = 2 << sizeByte;
        else if (sizeByte == 0x52)
            cartRomBankNumber = 72;
        else if (sizeByte == 0x53)
            cartRomBankNumber = 80;
        else if (sizeByte == 0x54)
            cartRomBankNumber = 96;
        else cartRomBankNumber = -1;
        //
        byte[][] rom = new byte[cartRomBankNumber * 2][0x2000];
        for (int i = 0; i < cartRomBankNumber * 2; i++) {
            if (0x2000 * i < bin.length)
                System.arraycopy(bin, 0x2000 * i, rom[i], 0, 0x2000);
        }
        return rom;
    }

    private static int loadType(byte[] bin) {
        return bin[0x0147] & 0xff;
    }

    private boolean loadColored(byte[] bin) {
        return ((bin[0x143] & 0x80) == 0x80);
    }

    private static boolean loadHasBattery(byte[] bin) {
        int type = loadType(bin);
        return (type == 3) || (type == 9) || (type == 0x1B)
                || (type == 0x1E) || (type == 6) || (type == 0x10)
                || (type == 0x13);
    }

    private byte[][] loadRam(byte[] bin) {
        /** Translation between ROM size byte contained in the ROM header, and the number
         *  of 16kB ROM banks the cartridge will contain
         */
        int ramBankNumber;
        switch (bin[0x149]) {
            case 1:
            case 2:
                ramBankNumber = 1;
                break;
            case 3:
                ramBankNumber = 4;
                break;
            case 4:
            case 5:
            case 6:
                ramBankNumber = 16;
                break;
            default:
                ramBankNumber = 0;
                break;
        }
        return new byte[ramBankNumber == 0 ? 1 : ramBankNumber][0x2000];
    }

    // Update the RTC registers before reading/writing (if active) with small delta
    protected final void rtcSync() {
        if ((rtcReg[4] & 0x40) == 0) {
            // active
            int now = (int) System.currentTimeMillis();
            while (now - lastRtcUpdate > 1000) {
                lastRtcUpdate += 1000;

                if (++rtcReg[0] == 60) {
                    rtcReg[0] = 0;

                    if (++rtcReg[1] == 60) {
                        rtcReg[1] = 0;

                        if (++rtcReg[2] == 24) {
                            rtcReg[2] = 0;

                            if (++rtcReg[3] == 0) {
                                rtcReg[4] = (byte) ((rtcReg[4] | (rtcReg[4] << 7)) ^ 1);
                            }
                        }
                    }
                }
            }
        }
    }

    // Update the RTC registers after resuming (large delta)
    protected final void rtcSkip(int s) {
        // seconds
        int sum = s + rtcReg[0];
        rtcReg[0] = (byte) (sum % 60);
        sum = sum / 60;
        if (sum == 0)
            return;

        // minutes
        sum = sum + rtcReg[1];
        rtcReg[1] = (byte) (sum % 60);
        sum = sum / 60;
        if (sum == 0)
            return;

        // hours
        sum = sum + rtcReg[2];
        rtcReg[2] = (byte) (sum % 24);
        sum = sum / 24;
        if (sum == 0)
            return;

        // days, bit 0-7
        sum = sum + (rtcReg[3] & 0xff) + ((rtcReg[4] & 1) << 8);
        rtcReg[3] = (byte) (sum);

        // overflow & day bit 8
        if (sum > 511)
            rtcReg[4] |= 0x80;
        rtcReg[4] = (byte) ((rtcReg[4] & 0xfe) + ((sum >> 8) & 1));
    }

    public byte[] dumpSram() {
        int bankCount = ram.length;
        int bankSize = ram[0].length;
        int size = bankCount * bankSize + 13;
        byte[] b = new byte[size];
        for (int i = 0; i < bankCount; i++)
            System.arraycopy(ram[i], 0, b, i * bankSize, bankSize);
        System.arraycopy(rtcReg, 0, b, bankCount * bankSize, 5);
        long now = System.currentTimeMillis();
        Common.setInt(b, bankCount * bankSize + 5, (int) (now >> 32));
        Common.setInt(b, bankCount * bankSize + 9, (int) now);
        return b;
    }

    public void setSram(byte[] b) {
        int bankCount = ram.length;
        int bankSize = ram[0].length;
        for (int i = 0; i < bankCount; i++)
            System.arraycopy(b, i * bankSize, ram[i], 0, bankSize);
        if (b.length == bankCount * bankSize + 13) {
            // load real time clock
            System.arraycopy(b, bankCount * bankSize, rtcReg, 0, 5);
            long time = Common.getInt(b, bankCount * bankSize + 5);
            time = (time << 32) + ((long) Common.getInt(b, bankCount * bankSize + 9) & 0xffffffffL);
            time = System.currentTimeMillis() - time;
            this.rtcSkip((int) (time / 1000));
        }
    }

}
