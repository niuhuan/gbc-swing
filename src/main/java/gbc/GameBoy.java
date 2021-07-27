package gbc;

import java.util.Map;
import java.util.TreeMap;

/**
 * Dmg ProcessingChip
 */
public class GameBoy implements Runnable {
    // Constants for flags register

    /**
     * Zero flag
     */
    private final int F_ZERO = 0x80;
    /**
     * Subtract/negative flag
     */
    private final int F_SUBTRACT = 0x40;
    /**
     * Half carry flag
     */
    private final int F_HALFCARRY = 0x20;
    /**
     * Carry flag
     */
    private final int F_CARRY = 0x10;

    // same in single and double speed:
    protected final int INSTRS_PER_DIV = 64;

    // single speed values:
    protected final int BASE_INSTRS_IN_MODE_0 = 51;
    protected final int BASE_INSTRS_IN_MODE_2 = 20;
    protected final int BASE_INSTRS_IN_MODE_3 = 43;

    /**
     * Used to set the speed of the emulator.  This controls how
     * many instructions are executed for each horizontal line scanned
     * on the gbc.  Multiply by 154 to find out how many instructions
     * per frame.
     */
    protected int INSTRS_IN_MODE_0 = BASE_INSTRS_IN_MODE_0;
    protected int INSTRS_IN_MODE_2 = BASE_INSTRS_IN_MODE_2;
    protected int INSTRS_IN_MODE_3 = BASE_INSTRS_IN_MODE_3;

    // Constants for interrupts

    /**
     * Vertical blank interrupt
     */
    public final int INT_VBLANK = 0x01;

    /**
     * LCD Coincidence interrupt
     */
    public final int INT_LCDC = 0x02;

    /**
     * TIMA (programmable timer) interrupt
     */
    public final int INT_TIMA = 0x04;

    /**
     * Serial interrupt
     */
    public final int INT_SER = 0x08;

    /**
     * P10 - P13 (Joypad) interrupt
     */
    public final int INT_P10 = 0x10;


    /**
     * Registers: 8-bit
     */
    private int a, b, c, d, e, f;
    /**
     * Registers: 16-bit
     */
    private int sp, hl;

    // decoder variables
    private byte[] decoderMemory;
    private int localPC;
    private int globalPC;
    private int decoderMaxCruise; // if localPC exceeds this, a new (half)bank should be found

    /**
     * The number of instructions that have been executed since the last reset
     */
    private int instrCount;

    private int graphicsChipMode; // takes values 0,2,3 -- mode 1 is signaled by line>=144

    private int nextModeTime;
    private int nextTimaOverflow;
    private int nextTimedInterrupt;

    public boolean interruptsEnabled;
    public boolean interruptsArmed;
    private boolean timaActive;
    private boolean interruptEnableRequested;
    public boolean gbcFeatures;
    protected int gbcRamBank;
    protected boolean hdmaRunning;


    // 0,1 = rom bank 0
    // 2,3 = mapped rom bank
    // 4 = vram (read only)
    // 5 = mapped cartram
    // 6 = main ram
    // 7 = main ram again (+ oam+reg)
    public byte[][] memory = new byte[8][];

    // 8kB main system RAM appears at 0xC000 in address space
    // 32kB for GBC
    private byte[] mainRam;

    // sprite ram, at 0xfe00
    public byte[] oam = new byte[0x100];

    /**
     * registers, at 0xff00
     */
    public byte[] registers = new byte[0x100];

    /**
     * instrCount at the time register[4] was reset
     */
    private int divReset;

    private int instrsPerTima = 256;

    public boolean terminate;

    /**
     * The bank number which is currently mapped at 0x4000 in ProcessingChip address space
     */
    private int currentRomBank = 1;

    /**
     * The RAM bank number which is currently mapped at 0xA000 in ProcessingChip address space
     */
    private int currentRamBank;

    private boolean mbc1LargeRamMode;
    private boolean cartRamEnabled;

    private int[] incflags = new int[256];
    private int[] decflags = new int[256];

    private static final int cyclesPerInstr[] = {
            1, 3, 2, 2, 1, 1, 2, 1, 5, 2, 2, 2, 1, 1, 2, 1,
            1, 3, 2, 2, 1, 1, 2, 1, 3, 2, 2, 2, 1, 1, 2, 1,
            3, 3, 2, 2, 1, 1, 2, 1, 3, 2, 2, 2, 1, 1, 2, 1,
            3, 3, 2, 2, 3, 3, 3, 1, 3, 2, 2, 2, 1, 1, 2, 1,

            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1,
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1,
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1,
            2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 1, 1, 1, 1, 2, 1,

            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1,
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1,
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1,
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1,

            5, 3, 4, 4, 6, 4, 2, 4, 5, 4, 4, 0, 6, 6, 2, 4,
            5, 3, 4, 0, 6, 4, 2, 4, 5, 4, 4, 0, 6, 0, 2, 4,
            3, 3, 2, 0, 0, 4, 2, 4, 4, 1, 4, 0, 0, 0, 2, 4,
            3, 3, 2, 1, 0, 4, 2, 4, 3, 2, 4, 1, 0, 0, 2, 4,
    };


    private static int cyclesPerInstrShift[] = {
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,

            2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 3, 2,
            2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 3, 2,
            2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 3, 2,
            2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 3, 2,

            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,

            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
            2, 2, 2, 2, 2, 2, 4, 2, 2, 2, 2, 2, 2, 2, 4, 2,
    };

    private final Controller controller;
    private final Cartridge cartridge;
    private ScreenAbstract screen;
    private final Speaker speaker;

    public GameBoy(boolean gbcFeatures, int[] palette, byte[] cartridgeBin, Controller controller, ScreenListener screenListener) {
        this.gbcFeatures = gbcFeatures;
        this.cartridge = new Cartridge(cartridgeBin);
        this.controller = controller;
        this.speaker = new Speaker(registers);
        this.screen = new ScreenImplement(registers, memory, oam, gbcFeatures, palette == null ? Palette.GB : palette, screenListener, 0);
        if (gbcFeatures) {
            mainRam = new byte[0x8000]; // 32 kB
        } else {
            mainRam = new byte[0x2000]; // 8 kB
        }
        init();
    }

    private void init() {
        gbcRamBank = 1;
        memory[0] = cartridge.rom[0];
        memory[1] = cartridge.rom[1];
        mapRom(1);
        memory[5] = cartridge.ram[0];
        memory[6] = mainRam;
        memory[7] = mainRam;
        interruptsEnabled = false;
        a = gbcFeatures ? 0x11 : 0x01;
        b = 0x00;
        c = 0x13;
        d = 0x00;
        e = 0xd8;
        f = 0xB0;
        hl = 0x014D;
        setPC(0x0100);
        sp = 0xFFFE;
        graphicsChipMode = 0;
        nextModeTime = 0;
        timaActive = false;
        interruptEnableRequested = false;
        nextTimedInterrupt = 0;
        initIncDecFlags();
        ioHandlerReset();
    }

    private void initIncDecFlags() {
        incflags[0] = F_ZERO + F_HALFCARRY;
        for (int i = 0x10; i < 0x100; i += 0x10)
            incflags[i] = F_HALFCARRY;

        decflags[0] = F_ZERO + F_SUBTRACT;
        for (int i = 1; i < 0x100; i++)
            decflags[i] = F_SUBTRACT + (((i & 0x0f) == 0x0f) ? F_HALFCARRY : 0);
    }

    /**
     * Perform a ProcessingChip address space read.  This maps all the relevant objects into the correct parts of
     * the memory
     */
    public final int addressRead1(int addr) {
        if (addr < 0xa000) {
            return memory[addr >> 13][addr & 0x1fff];
        } else if (addr < 0xc000) {
            if (currentRamBank >= 8) { // real time clock
                cartridge.rtcSync();
                return cartridge.rtcReg[currentRamBank - 8];
            } else
                return memory[addr >> 13][addr & 0x1fff];
        } else if ((addr & 0x1000) == 0) {
            return mainRam[addr & 0x0fff];
        } else if (addr < 0xfe00) {
            return mainRam[(addr & 0x0fff) + gbcRamBank * 0x1000];
        } else if (addr < 0xFF00) {
            if (addr > 0xFEA0) // outside OAM range
                return 0xff;
            return (oam[addr - 0xFE00] & 0xFF);
        } else {
            return ioRead(addr - 0xFF00);
        }
    }

    /**
     * Performs a ProcessingChip address space write.  Maps all of the relevant object into the right parts of
     * memory.
     */
    public final void addressWrite(int addr, int data) {
        int bank = addr >> 12;
        switch (bank) {
            case 0x0:
            case 0x1:
            case 0x2:
            case 0x3:
            case 0x4:
            case 0x5:
            case 0x6:
            case 0x7:
                cartridgeWrite(addr, data);
                break;

            case 0x8:
            case 0x9:
                screen.addressWrite(addr - 0x8000, (byte) data);
                break;

            case 0xA:
            case 0xB:
                cartridgeWrite(addr, data);
                break;

            case 0xC:
                mainRam[addr - 0xC000] = (byte) data;
                break;

            case 0xD:
                mainRam[addr - 0xD000 + gbcRamBank * 0x1000] = (byte) data;
                break;

            case 0xE:
                mainRam[addr - 0xE000] = (byte) data;
                break;

            case 0xF:
                if (addr < 0xFE00) {
                    mainRam[addr - 0xF000 + gbcRamBank * 0x1000] = (byte) data;
                } else if (addr < 0xFF00) {
                    oam[addr - 0xFE00] = (byte) data;
                } else {
                    ioWrite(addr - 0xFF00, data);
                }
                break;
        }
    }

    private final void pushPC() {
        int pc = globalPC + localPC;
        if ((sp >> 13) == 6) {
            mainRam[--sp - 0xC000] = (byte) (pc >> 8);
            mainRam[--sp - 0xC000] = (byte) (pc);
        } else {
            addressWrite(--sp, pc >> 8);
            addressWrite(--sp, pc & 0xFF);
        }
    }

    private final void popPC() {
        if (sp >> 13 == 6) {
            setPC((mainRam[sp++ - 0xc000] & 0xff) + ((mainRam[sp++ - 0xc000] & 0xff) << 8));
        } else {
            setPC((addressRead(sp++) & 0xff) + ((addressRead(sp++) & 0xff) << 8));
        }
    }

    /**
     * Performs a read of a register by internal register number
     */
    private final int registerRead(int regNum) {
        switch (regNum) {
            case 0:
                return b;
            case 1:
                return c;
            case 2:
                return d;
            case 3:
                return e;
            case 4:
                return (hl >> 8);
            case 5:
                return (hl & 0xFF);
            case 6:
                return addressRead(hl) & 0xff;
            case 7:
                return a;
            default:
                return -1;
        }
    }

    /**
     * Performs a write of a register by internal register number
     */
    private final void registerWrite(int regNum, int data) {
        switch (regNum) {
            case 0:
                b = data;
                return;
            case 1:
                c = data;
                return;
            case 2:
                d = data;
                return;
            case 3:
                e = data;
                return;
            case 4:
                // h
                hl = (hl & 0x00FF) | (data << 8);
                return;
            case 5:
                // l
                hl = (hl & 0xFF00) | data;
                return;
            case 6:
                // (hl)
                addressWrite(hl, data);
                return;
            case 7:
                a = data;
                return;
            default:
                return;
        }
    }

    private void performHdma() {
        int dmaSrc = ((registers[0x51] & 0xff) << 8) + ((registers[0x52] & 0xff) & 0xF0);
        int dmaDst = ((registers[0x53] & 0x1F) << 8) + (registers[0x54] & 0xF0) + 0x8000;

        for (int r = 0; r < 16; r++) {
            addressWrite(dmaDst + r, addressRead(dmaSrc + r));
        }

        dmaSrc += 16;
        dmaDst += 16;
        registers[0x51] = (byte) ((dmaSrc & 0xFF00) >> 8);
        registers[0x52] = (byte) (dmaSrc & 0x00F0);
        registers[0x53] = (byte) ((dmaDst & 0x1F00) >> 8);
        registers[0x54] = (byte) (dmaDst & 0x00F0);

        if (registers[0x55] == 0) {
            hdmaRunning = false;
        }
        registers[0x55]--;
    }

    /**
     * If an interrupt is enabled an the interrupt register shows that it has occured, jump to
     * the relevant interrupt vector address
     */
    private final void checkInterrupts() {
        pushPC();

        int mask = registers[0xff] & registers[0x0f];

        if ((mask & INT_VBLANK) != 0) {
            setPC(0x40);
            registers[0x0f] -= INT_VBLANK;
        } else if ((mask & INT_LCDC) != 0) {
            setPC(0x48);
            registers[0x0f] -= INT_LCDC;
        } else if ((mask & INT_TIMA) != 0) {
            setPC(0x50);
            registers[0x0f] -= INT_TIMA;
        } else if ((mask & INT_SER) != 0) {
            setPC(0x58);
            registers[0x0f] -= INT_SER;
        } else if ((mask & INT_P10) != 0) {
            setPC(0x60);
            registers[0x0f] -= INT_P10;
        } else {
            // throw new RuntimeException("concurrent modification exception: " + mask + " "
            //		+ registers[0xff] + " " + registers[0x0f]);
        }

        interruptsEnabled = false;
        interruptsArmed = (registers[0xff] & registers[0x0f]) != 0;
    }

    /**
     * Check for interrupts that need to be initiated
     */
    private final void initiateInterrupts() {
        if (instrCount - nextModeTime >= 0) {
            // changed graphics chip mode
            if (graphicsChipMode == 3) {
                // entered mode 0 (unless in vblank)
                graphicsChipMode = 0;
                nextModeTime += INSTRS_IN_MODE_0;

                int line = registers[0x44] & 0xff;

                if (line < 144) {
                    if (gbcFeatures && hdmaRunning) {
                        performHdma();
                    }

                    if (((registers[0x40] & 0x80) != 0) && ((registers[0xff] & INT_LCDC) != 0)) {
                        if (((registers[0x41] & 0x08) != 0)) {
                            // trigger "mode 0 entered" interrupt
                            interruptsArmed = true;
                            registers[0x0f] |= INT_LCDC;
                        }
                    }
                }
            } else if (graphicsChipMode == 0) {
                // entered mode 2 (or mode 1, i.e. vblank)
                graphicsChipMode = 2;
                nextModeTime += INSTRS_IN_MODE_2;

                registers[0x44]++;
                if ((registers[0x44] & 0xff) == 154) {
                    registers[0x44] = 0;
                }

                int line = registers[0x44] & 0xff;

                // check for mode 2 interrupt
                if (line < 144) {
                    if (((registers[0x41] & 0x20) != 0)) {
                        // trigger "mode 2 entered" interrupt
                        interruptsArmed = true;
                        registers[0x0f] |= INT_LCDC;
                    }
                }

                // check for lyc coincidence interrupt
                if (((registers[0x40] & 0x80) != 0) && ((registers[0xff] & INT_LCDC) != 0)) {
                    if (((registers[0x41] & 0x40) != 0) && ((registers[0x45] & 0xff) == line)) {
                        // trigger "lyc coincidence" interrupt
                        interruptsArmed = true;
                        registers[0x0f] |= INT_LCDC;
                    }
                }

                if (line == 144) {
                    // whole frame done, draw buffer and start vblank
                    screen.vBlank();

                    if (((registers[0x40] & 0x80) != 0) && ((registers[0xff] & INT_VBLANK) != 0)) {
                        interruptsArmed = true;
                        registers[0x0f] |= INT_VBLANK;

                        if (((registers[0x41] & 0x10) != 0) && ((registers[0xff] & INT_LCDC) != 0)) {
                            // VBLANK LCDC
                            // armed is already set
                            registers[0x0f] |= INT_LCDC;
                        }
                    }
                    if (speaker != null) {
                        speaker.outputSound();
                    }
                }

                if (line == 0) {
                    if (controller.p10Requested) {
                        controller.p10Requested = false;
                        if ((registers[0xff] & INT_P10) != 0) {
                            registers[0x0f] |= INT_P10;
                        }
                        interruptsArmed = (registers[0xff] & registers[0x0f]) != 0;
                    }
                }
            } else {
                // entered mode 3 (unless in vblank)
                graphicsChipMode = 3;
                nextModeTime += INSTRS_IN_MODE_3;

                int line = registers[0x44] & 0xff;
                if (line < 144) {
                    // send the line to graphic chip
                    screen.notifyScanline(line);
                }
            }
        }

        if (timaActive && instrCount - nextTimaOverflow >= 0) {
            nextTimaOverflow += instrsPerTima * (0x100 - (registers[0x06] & 0xff));

            if ((registers[0xff] & INT_TIMA) != 0) {
                interruptsArmed = true;
                registers[0x0f] |= INT_TIMA;
            }
        }

        if (interruptEnableRequested) {
            interruptsEnabled = true;
            interruptEnableRequested = false;
        }

        nextTimedInterrupt = nextModeTime;
        if (timaActive && nextTimaOverflow < nextTimedInterrupt)
            nextTimedInterrupt = nextTimaOverflow;
    }

    public final void setPC(int pc) {
        if (pc < 0xff00) {
            decoderMemory = memory[pc >> 13];
            localPC = pc & 0x1fff;
            globalPC = pc & 0xe000;
            decoderMaxCruise = (pc < 0xe000) ? 0x1ffd : 0x1dfd;
            if (gbcFeatures) {
                if (gbcRamBank > 1 && pc >= 0xC000)
                    decoderMaxCruise &= 0x0fff; // can't cruise in switched ram bank
            }
        } else {
            decoderMemory = registers;
            localPC = pc & 0xff;
            globalPC = 0xff00;
            decoderMaxCruise = 0xfd;
        }
    }

    private final void executeShift(int b2) {
        int regNum = b2 & 0x07;
        int data = registerRead(regNum);
        int newf;

        instrCount += cyclesPerInstrShift[b2];

		/*
        00ooorrr = operation ooo on register rrr
		01bbbrrr = test bit bbb of register rrr
		10bbbrrr = reset bit bbb of register rrr
		11bbbrrr = set bit bbb of register rrr
		*/

        if ((b2 & 0xC0) == 0) {
            switch ((b2 & 0xF8)) {
                case 0x00: // RLC A
                    f = 0;
                    if (data >= 0x80) {
                        f = F_CARRY;
                    }
                    data = (data << 1) & 0xff;
                    if ((f & F_CARRY) != 0) {
                        data |= 1;
                    }

                    break;
                case 0x08: // RRC A
                    f = 0;
                    if ((data & 0x01) != 0) {
                        f = F_CARRY;
                    }
                    data >>= 1;
                    if ((f & F_CARRY) != 0) {
                        data |= 0x80;
                    }
                    break;
                case 0x10: // RL r
                    if (data >= 0x80) {
                        newf = F_CARRY;
                    } else {
                        newf = 0;
                    }
                    data = (data << 1) & 0xff;

                    if ((f & F_CARRY) != 0) {
                        data |= 1;
                    }
                    f = newf;
                    break;

                case 0x18: // RR r
                    if ((data & 0x01) != 0) {
                        newf = F_CARRY;
                    } else {
                        newf = 0;
                    }
                    data >>= 1;

                    if ((f & F_CARRY) != 0) {
                        data |= 0x80;
                    }

                    f = newf;
                    break;

                case 0x20: // SLA r
                    f = 0;
                    if ((data & 0x80) != 0) {
                        f = F_CARRY;
                    }

                    data = (data << 1) & 0xff;
                    break;
                case 0x28: // SRA r
                    f = 0;
                    if ((data & 0x01) != 0) {
                        f = F_CARRY;
                    }

                    data = (data & 0x80) + (data >> 1); // i.e. duplicate high bit=sign
                    break;
                case 0x30: // SWAP r
                    data = (((data & 0x0F) << 4) | (data >> 4));
                    f = 0;

                    break;
                case 0x38: // SRL r
                    f = 0;
                    if ((data & 0x01) != 0) {
                        f = F_CARRY;
                    }

                    data >>= 1;
                    break;
            }

            if (data == 0) {
                f |= F_ZERO;
            }
            registerWrite(regNum, data);
        } else {
            int bitMask = 1 << ((b2 & 0x38) >> 3);

            if ((b2 & 0xC0) == 0x40) { // BIT n, r
                f = ((f & F_CARRY) | F_HALFCARRY);
                if ((data & bitMask) == 0) {
                    f |= F_ZERO;
                }
            } else if ((b2 & 0xC0) == 0x80) { // RES n, r
                registerWrite(regNum, (data & (0xFF - bitMask)));
            } else if ((b2 & 0xC0) == 0xC0) { // SET n, r
                registerWrite(regNum, (data | bitMask));
            }
        }
    }

    private final void executeDAA() {
        int upperNibble = (a >> 4) & 0x0f;
        int lowerNibble = a & 0x0f;

        int newf = f & (F_SUBTRACT | F_CARRY);

        if ((f & F_SUBTRACT) == 0) {
            if ((f & F_CARRY) == 0) {
                if ((upperNibble <= 8) && (lowerNibble >= 0xA) && ((f & F_HALFCARRY) == 0)) {
                    a += 0x06;
                }

                if ((upperNibble <= 9) && (lowerNibble <= 0x3) && ((f & F_HALFCARRY) != 0)) {
                    a += 0x06;
                }

                if ((upperNibble >= 0xA) && (lowerNibble <= 0x9) && ((f & F_HALFCARRY) == 0)) {
                    a += 0x60;
                    newf |= F_CARRY;
                }

                if ((upperNibble >= 0x9) && (lowerNibble >= 0xA) && ((f & F_HALFCARRY) == 0)) {
                    a += 0x66;
                    newf |= F_CARRY;
                }

                if ((upperNibble >= 0xA) && (lowerNibble <= 0x3) && ((f & F_HALFCARRY) != 0)) {
                    a += 0x66;
                    newf |= F_CARRY;
                }

            } else { // carry is set

                if ((upperNibble <= 0x2) && (lowerNibble <= 0x9) && ((f & F_HALFCARRY) == 0)) {
                    a += 0x60;
                }

                if ((upperNibble <= 0x2) && (lowerNibble >= 0xA) && ((f & F_HALFCARRY) == 0)) {
                    a += 0x66;
                }

                if ((upperNibble <= 0x3) && (lowerNibble <= 0x3) && ((f & F_HALFCARRY) != 0)) {
                    a += 0x66;
                }
            }

        } else { // subtract is set

            if ((f & F_CARRY) == 0) {
                if ((upperNibble <= 0x8) && (lowerNibble >= 0x6) && ((f & F_HALFCARRY) != 0)) {
                    a += 0xFA;
                }
            } else { // Carry is set
                if ((upperNibble >= 0x7) && (lowerNibble <= 0x9) && ((f & F_HALFCARRY) == 0)) {
                    a += 0xA0;
                }

                if ((upperNibble >= 0x6) && (lowerNibble >= 0x6) && ((f & F_HALFCARRY) != 0)) {
                    a += 0x9A;
                }
            }
        }

        a &= 0xff;
        if (a == 0)
            newf |= F_ZERO;

        // halfcarry is wrong
        f = newf;
    }

    private final void executeALU(int b1) {
        int operand = registerRead(b1 & 0x07);
        switch ((b1 & 0x38) >> 3) {
            case 1: // ADC A, r
                if ((f & F_CARRY) != 0) {
                    operand++;
                }
                // Note!  No break!
            case 0: // ADD A, r

                if ((a & 0x0F) + (operand & 0x0F) >= 0x10) {
                    f = F_HALFCARRY;
                } else {
                    f = 0;
                }

                a += operand;

                if (a > 0xff) {
                    f |= F_CARRY;
                    a &= 0xff;
                }

                if (a == 0) {
                    f |= F_ZERO;
                }
                break;
            case 3: // SBC A, r
                if ((f & F_CARRY) != 0) {
                    operand++;
                }
                // Note! No break!
            case 2: // SUB A, r

                f = F_SUBTRACT;

                if ((a & 0x0F) < (operand & 0x0F)) {
                    f |= F_HALFCARRY;
                }

                a -= operand;

                if (a < 0) {
                    f |= F_CARRY;
                    a &= 0xff;
                }
                if (a == 0) {
                    f |= F_ZERO;
                }

                break;
            case 4: // AND A, r
                a &= operand;
                if (a == 0) {
                    f = F_HALFCARRY + F_ZERO;
                } else {
                    f = F_HALFCARRY;
                }
                break;
            case 5: // XOR A, r
                a ^= operand;
                f = (a == 0) ? F_ZERO : 0;
                break;
            case 6: // OR A, r
                a |= operand;
                f = (a == 0) ? F_ZERO : 0;
                break;
            case 7: // CP A, r (compare)
                f = F_SUBTRACT;
                if (a == operand) {
                    f |= F_ZERO;
                } else if (a < operand) {
                    f |= F_CARRY;
                }
                if ((a & 0x0F) < (operand & 0x0F)) {
                    f |= F_HALFCARRY;
                }
                break;
        }
    }

    public final void run() {
        try {
            terminate = false;

            int newf = 0;
            int b1, b2, offset, b3;

            System.gc();

            screen.fixTimer();

            while (!terminate) {
                if (localPC <= decoderMaxCruise) {
                    b1 = decoderMemory[localPC++] & 0xff;
                    offset = decoderMemory[localPC];
                    b2 = offset & 0xff;
                    b3 = decoderMemory[localPC + 1];
                } else {
                    int pc = localPC + globalPC;
                    b1 = addressRead(pc++) & 0xff;
                    offset = addressRead(pc);
                    b2 = offset & 0xff;
                    b3 = addressRead(pc + 1);
                    setPC(pc);
                }

                switch (b1) {
                    case 0x00: // NOP
                        break;
                    case 0x01: // LD BC, nn
                        localPC += 2;
                        b = b3 & 0xff;
                        c = b2;
                        break;
                    case 0x02: // LD (BC), A
                        addressWrite((b << 8) | c, a);
                        break;
                    case 0x03: // INC BC
                        c++;
                        if (c == 0x0100) {
                            c = 0;
                            b = (b + 1) & 0xff;
                        }
                        break;
                    case 0x04: // INC B
                        b = (b + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[b];
                        break;
                    case 0x05: // DEC B
                        b = (b - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[b];
                        break;
                    case 0x06: // LD B, nn
                        localPC++;
                        b = b2;
                        break;
                    case 0x07: // RLC A
                        if (a >= 0x80) {
                            f = F_CARRY;
                            a = ((a << 1) + 1) & 0xff;
                        } else if (a == 0) {
                            f = F_ZERO;
                        } else {
                            a <<= 1;
                            f = 0;
                        }
                        break;
                    case 0x08: // LD (nnnn), SP
                        localPC += 2;
                        newf = ((b3 & 0xff) << 8) + b2;
                        addressWrite(newf, sp);
                        addressWrite(newf + 1, sp >> 8);
                        break;
                    case 0x09: // ADD HL, BC
                        hl = (hl + ((b << 8) + c));
                        if ((hl & 0xffff0000) != 0) {
                            f = (f & F_ZERO) | F_CARRY; // halfcarry is wrong
                            hl &= 0xFFFF;
                        } else {
                            f &= F_ZERO; // halfcarry is wrong
                        }
                        break;
                    case 0x0A: // LD A, (BC)
                        a = addressRead((b << 8) + c) & 0xff;
                        break;
                    case 0x0B: // DEC BC
                        c--;
                        if (c < 0) {
                            c = 0xFF;
                            b = (b - 1) & 0xff;
                        }
                        break;
                    case 0x0C: // INC C
                        c = (c + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[c];
                        break;
                    case 0x0D: // DEC C
                        c = (c - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[c];
                        break;
                    case 0x0E: // LD C, nn
                        localPC++;
                        c = b2;
                        break;
                    case 0x0F: // RRC A
                        if ((a & 0x01) == 0x01) {
                            f = F_CARRY;
                        } else {
                            f = 0;
                        }
                        a >>= 1;
                        if ((f & F_CARRY) != 0) {
                            a |= 0x80;
                        }
                        if (a == 0) {
                            f |= F_ZERO;
                        }
                        break;
                    case 0x10: // STOP
                        localPC++;

                        if (gbcFeatures) {
                            if ((registers[0x4D] & 0x01) != 0) {
                                int newKey1Reg = registers[0x4D] & 0xFE;
                                int multiplier = 1;
                                if ((newKey1Reg & 0x80) != 0) {
                                    newKey1Reg &= 0x7F;
                                } else {
                                    multiplier = 2;
                                    newKey1Reg |= 0x80;
                                }

                                INSTRS_IN_MODE_0 = BASE_INSTRS_IN_MODE_0 * multiplier;
                                INSTRS_IN_MODE_2 = BASE_INSTRS_IN_MODE_2 * multiplier;
                                INSTRS_IN_MODE_3 = BASE_INSTRS_IN_MODE_3 * multiplier;

                                registers[0x4D] = (byte) newKey1Reg;
                            }
                        }

                        break;
                    case 0x11: // LD DE, nnnn
                        localPC += 2;
                        d = b3 & 0xff;
                        e = b2;
                        break;
                    case 0x12: // LD (DE), A
                        addressWrite((d << 8) + e, a);
                        break;
                    case 0x13: // INC DE
                        e++;
                        if (e == 0x0100) {
                            e = 0;
                            d = (d + 1) & 0xff;
                        }
                        break;
                    case 0x14: // INC D
                        d = (d + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[d];
                        break;
                    case 0x15: // DEC D
                        d = (d - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[d];
                        break;
                    case 0x16: // LD D, nn
                        localPC++;
                        d = b2;
                        break;
                    case 0x17: // RL A
                        if ((a & 0x80) != 0) {
                            newf = F_CARRY;
                        } else {
                            newf = 0;
                        }
                        a <<= 1;

                        if ((f & F_CARRY) != 0) {
                            a |= 1;
                        }

                        a &= 0xFF;
                        if (a == 0) {
                            newf |= F_ZERO;
                        }
                        f = newf;
                        break;
                    case 0x18: // JR nn
                        localPC += 1 + offset;
                        if (localPC < 0 || localPC > decoderMaxCruise) {
                            // switch bank
                            setPC(localPC + globalPC);
                        }

                        break;
                    case 0x19: // ADD HL, DE
                        hl += ((d << 8) + e);
                        if ((hl & 0xFFFF0000) != 0) {
                            f = ((f & F_ZERO) | F_CARRY); // halfcarry is wrong
                            hl &= 0xFFFF;
                        } else {
                            f &= F_ZERO; // halfcarry is wrong
                        }
                        break;
                    case 0x1A: // LD A, (DE)
                        a = (addressRead((d << 8) + e)) & 0xff;
                        break;
                    case 0x1B: // DEC DE
                        e--;
                        if (e < 0) {
                            e = 0xFF;
                            d = (d - 1) & 0xff;
                        }
                        break;
                    case 0x1C: // INC E
                        e = (e + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[e];
                        break;
                    case 0x1D: // DEC E
                        e = (e - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[e];
                        break;
                    case 0x1E: // LD E, nn
                        localPC++;
                        e = b2;
                        break;
                    case 0x1F: // RR A
                        if ((a & 0x01) != 0) {
                            newf = F_CARRY;
                        } else {
                            newf = 0;
                        }
                        a >>= 1;

                        if ((f & F_CARRY) != 0) {
                            a |= 0x80;
                        }

                        if (a == 0) {
                            newf |= F_ZERO;
                        }
                        f = newf;
                        break;
                    case 0x20: // JR NZ, nn
                        if (f < F_ZERO) {
                            localPC += 1 + offset;
                            if (localPC < 0 || localPC > decoderMaxCruise) {
                                // switch bank
                                setPC(localPC + globalPC);
                            }
                        } else {
                            localPC++;
                        }
                        break;
                    case 0x21: // LD HL, nnnn
                        localPC += 2;
                        hl = ((b3 & 0xff) << 8) + b2;
                        break;
                    case 0x22: // LD (HL+), A
                        addressWrite(hl++, a);
                        break;
                    case 0x23: // INC HL
                        hl = (hl + 1) & 0xFFFF;
                        break;
                    case 0x24: // INC H
                        b2 = ((hl >> 8) + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[b2];
                        hl = (hl & 0xff) + (b2 << 8);
                        break;
                    case 0x25: // DEC H
                        b2 = ((hl >> 8) - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[b2];
                        hl = (hl & 0xff) + (b2 << 8);
                        break;
                    case 0x26: // LD H, nn
                        localPC++;
                        hl = (hl & 0xFF) | (b2 << 8);
                        break;
                    case 0x27: // DAA
                        executeDAA();
                        break;
                    case 0x28: // JR Z, nn
                        if (f >= F_ZERO) {
                            localPC += 1 + offset;
                            if (localPC < 0 || localPC > decoderMaxCruise) {
                                // switch bank
                                setPC(localPC + globalPC);
                            }
                        } else {
                            localPC++;
                        }
                        break;
                    case 0x29: // ADD HL, HL
                        hl *= 2;
                        if ((hl & 0xFFFF0000) != 0) {
                            f = (f & F_ZERO) | F_CARRY; // halfcarry is wrong
                            hl &= 0xFFFF;
                        } else {
                            f &= F_ZERO; // halfcarry is wrong
                        }
                        break;
                    case 0x2A: // LDI A, (HL)
                        a = addressRead(hl++) & 0xff;
                        break;
                    case 0x2B: // DEC HL
                        hl = (hl - 1) & 0xffff;
                        break;
                    case 0x2C: // INC L
                        b2 = (hl + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[b2];
                        hl = (hl & 0xff00) + b2;
                        break;
                    case 0x2D: // DEC L
                        b2 = (hl - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[b2];
                        hl = (hl & 0xff00) + b2;
                        break;
                    case 0x2E: // LD L, nn
                        localPC++;
                        hl = (hl & 0xFF00) | b2;
                        break;
                    case 0x2F: // CPL A
                        a = ((~a) & 0x00FF);
                        f |= F_SUBTRACT | F_HALFCARRY;
                        break;
                    case 0x30: // JR NC, nn
                        if ((f & F_CARRY) == 0) {
                            localPC += 1 + offset;
                            if (localPC < 0 || localPC > decoderMaxCruise) {
                                // switch bank
                                setPC(localPC + globalPC);
                            }
                        } else {
                            localPC++;
                        }
                        break;
                    case 0x31: // LD SP, nnnn
                        localPC += 2;
                        sp = ((b3 & 0xff) << 8) + b2;
                        break;
                    case 0x32:
                        addressWrite(hl--, a); // LD (HL-), A
                        break;
                    case 0x33: // INC SP
                        sp = (sp + 1) & 0xFFFF;
                        break;
                    case 0x34: // INC (HL)
                        b2 = (addressRead(hl) + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[b2];
                        addressWrite(hl, b2);
                        break;
                    case 0x35: // DEC (HL)
                        b2 = (addressRead(hl) - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[b2];
                        addressWrite(hl, b2);
                        break;
                    case 0x36: // LD (HL), nn
                        localPC++;
                        addressWrite(hl, b2);
                        break;
                    case 0x37: // SCF
                        f = (f & F_ZERO) | F_CARRY;
                        break;
                    case 0x38: // JR C, nn
                        if ((f & F_CARRY) != 0) {
                            localPC += 1 + offset;
                            if (localPC < 0 || localPC > decoderMaxCruise) {
                                // switch bank
                                setPC(localPC + globalPC);
                            }
                        } else {
                            localPC += 1;
                        }
                        break;
                    case 0x39: // ADD HL, SP
                        hl += sp;
                        if (hl > 0x0000FFFF) {
                            f = (f & F_ZERO) | F_CARRY; // halfcarry is wrong
                            hl &= 0xFFFF;
                        } else {
                            f &= F_ZERO; // halfcarry is wrong
                        }
                        break;
                    case 0x3A: // LD A, (HL-)
                        a = addressRead(hl--) & 0xff;
                        break;
                    case 0x3B: // DEC SP
                        sp = (sp - 1) & 0xFFFF;
                        break;
                    case 0x3C: // INC A
                        a = (a + 1) & 0xff;
                        f = (f & F_CARRY) | incflags[a];
                        break;
                    case 0x3D: // DEC A
                        a = (a - 1) & 0xff;
                        f = (f & F_CARRY) | decflags[a];
                        break;
                    case 0x3E: // LD A, nn
                        localPC++;
                        a = b2;
                        break;
                    case 0x3F: // CCF
                        f = (f & (F_CARRY | F_ZERO)) ^ F_CARRY;
                        break;

                    // B = r
                    case 0x40:
                        break;
                    case 0x41:
                        b = c;
                        break;
                    case 0x42:
                        b = d;
                        break;
                    case 0x43:
                        b = e;
                        break;
                    case 0x44:
                        b = hl >> 8;
                        break;
                    case 0x45:
                        b = hl & 0xFF;
                        break;
                    case 0x46:
                        b = addressRead(hl) & 0xff;
                        break;
                    case 0x47:
                        b = a;
                        break;

                    // C = r
                    case 0x48:
                        c = b;
                        break;
                    case 0x49:
                        break;
                    case 0x4a:
                        c = d;
                        break;
                    case 0x4b:
                        c = e;
                        break;
                    case 0x4c:
                        c = hl >> 8;
                        break;
                    case 0x4d:
                        c = hl & 0xFF;
                        break;
                    case 0x4e:
                        c = addressRead(hl) & 0xff;
                        break;
                    case 0x4f:
                        c = a;
                        break;

                    // D = r
                    case 0x50:
                        d = b;
                        break;
                    case 0x51:
                        d = c;
                        break;
                    case 0x52:
                        break;
                    case 0x53:
                        d = e;
                        break;
                    case 0x54:
                        d = hl >> 8;
                        break;
                    case 0x55:
                        d = hl & 0xFF;
                        break;
                    case 0x56:
                        d = addressRead(hl) & 0xff;
                        break;
                    case 0x57:
                        d = a;
                        break;

                    // E = r
                    case 0x58:
                        e = b;
                        break;
                    case 0x59:
                        e = c;
                        break;
                    case 0x5a:
                        e = d;
                        break;
                    case 0x5b:
                        break;
                    case 0x5c:
                        e = hl >> 8;
                        break;
                    case 0x5d:
                        e = hl & 0xFF;
                        break;
                    case 0x5e:
                        e = addressRead(hl) & 0xff;
                        break;
                    case 0x5f:
                        e = a;
                        break;

                    // h = r
                    case 0x60:
                        hl = (hl & 0xFF) | (b << 8);
                        break;
                    case 0x61:
                        hl = (hl & 0xFF) | (c << 8);
                        break;
                    case 0x62:
                        hl = (hl & 0xFF) | (d << 8);
                        break;
                    case 0x63:
                        hl = (hl & 0xFF) | (e << 8);
                        break;
                    case 0x64:
                        break;
                    case 0x65:
                        hl = (hl & 0xFF) * 0x0101;
                        break;
                    case 0x66:
                        hl = (hl & 0xFF) | ((addressRead(hl) & 0xff) << 8);
                        break;
                    case 0x67:
                        hl = (hl & 0xFF) | (a << 8);
                        break;

                    // l = r
                    case 0x68:
                        hl = (hl & 0xFF00) | b;
                        break;
                    case 0x69:
                        hl = (hl & 0xFF00) | c;
                        break;
                    case 0x6a:
                        hl = (hl & 0xFF00) | d;
                        break;
                    case 0x6b:
                        hl = (hl & 0xFF00) | e;
                        break;
                    case 0x6c:
                        hl = (hl >> 8) * 0x0101;
                        break;
                    case 0x6d:
                        break;
                    case 0x6e:
                        hl = (hl & 0xFF00) | (addressRead(hl) & 0xff);
                        break;
                    case 0x6f:
                        hl = (hl & 0xFF00) | a;
                        break;

                    // (hl) = r
                    case 0x70:
                        addressWrite(hl, b);
                        break;
                    case 0x71:
                        addressWrite(hl, c);
                        break;
                    case 0x72:
                        addressWrite(hl, d);
                        break;
                    case 0x73:
                        addressWrite(hl, e);
                        break;
                    case 0x74:
                        addressWrite(hl, hl >> 8);
                        break;
                    case 0x75:
                        addressWrite(hl, hl);
                        break;
                    case 0x76: // HALT
                        interruptsEnabled = true;

                        if (interruptsArmed) {
                            nextTimedInterrupt = instrCount;
                        } else {
                            while (!interruptsArmed) {
                                instrCount = nextTimedInterrupt;
                                initiateInterrupts();
                            }
                            instrCount++;
                            nextTimedInterrupt = instrCount;
                        }

                        break;
                    case 0x77:
                        addressWrite(hl, a);
                        break;

                    // LD A, n:
                    case 0x78:
                        a = b;
                        break;
                    case 0x79:
                        a = c;
                        break;
                    case 0x7a:
                        a = d;
                        break;
                    case 0x7b:
                        a = e;
                        break;
                    case 0x7c:
                        a = (hl >> 8);
                        break;
                    case 0x7d:
                        a = (hl & 0xFF);
                        break;
                    case 0x7e:
                        a = addressRead(hl) & 0xff;
                        break;
                    case 0x7f:
                        break;

                    // ALU, 0x80 - 0xbf

                    case 0xA7: // AND A, A
                        if (a == 0) {
                            f = F_HALFCARRY + F_ZERO;
                        } else {
                            f = F_HALFCARRY;
                        }
                        break;

                    case 0xAF: // XOR A, A (== LD A, 0)
                        a = 0;
                        f = F_ZERO;
                        break;
                    case 0xC0: // RET NZ
                        if (f < F_ZERO) {
                            popPC();
                        }
                        break;
                    case 0xC1: // POP BC
                        c = addressRead(sp++) & 0xff;
                        b = addressRead(sp++) & 0xff;
                        break;
                    case 0xC2: // JP NZ, nnnn
                        if (f < F_ZERO) {
                            setPC(((b3 & 0xff) << 8) + b2);
                        } else {
                            localPC += 2;
                        }
                        break;
                    case 0xC3: // JP nnnn
                        setPC(((b3 & 0xff) << 8) + b2);
                        break;
                    case 0xC4: // CALL NZ, nnnn
                        localPC += 2;
                        if (f < F_ZERO) {
                            pushPC();
                            setPC(((b3 & 0xff) << 8) + b2);
                        }
                        break;
                    case 0xC5: // PUSH BC
                        addressWrite(--sp, b);
                        addressWrite(--sp, c);
                        break;
                    case 0xC6: // ADD A, nn
                        localPC++;
                        if ((a & 0x0F) + (b2 & 0x0F) >= 0x10) {
                            f = F_HALFCARRY;
                        } else {
                            f = 0;
                        }

                        a += b2;

                        if (a > 0xff) {
                            f |= F_CARRY;
                            a &= 0xff;
                        }

                        if (a == 0) {
                            f |= F_ZERO;
                        }
                        break;
                    case 0xC7: // RST 00
                        pushPC();
                        setPC(0x00);
                        break;
                    case 0xC8: // RET Z
                        if (f >= F_ZERO) {
                            popPC();
                        }
                        break;
                    case 0xC9: // RET
                        popPC();
                        break;
                    case 0xCA: // JP Z, nnnn
                        if (f >= F_ZERO) {
                            setPC(((b3 & 0xff) << 8) + b2);
                        } else {
                            localPC += 2;
                        }
                        break;
                    case 0xCB: // Shift/bit test
                        localPC++;
                        executeShift(b2);
                        break;
                    case 0xCC: // CALL Z, nnnn
                        localPC += 2;
                        if (f >= F_ZERO) {
                            pushPC();
                            setPC(((b3 & 0xff) << 8) + b2);
                        }
                        break;
                    case 0xCD: // CALL nnnn
                        localPC += 2;
                        pushPC();
                        setPC(((b3 & 0xff) << 8) + b2);
                        break;
                    case 0xCE: // ADC A, nn
                        localPC++;

                        if ((f & F_CARRY) != 0) {
                            b2++;
                        }
                        if ((a & 0x0F) + (b2 & 0x0F) >= 0x10) {
                            f = F_HALFCARRY;
                        } else {
                            f = 0;
                        }

                        a += b2;

                        if (a > 0xff) {
                            f |= F_CARRY;
                            a &= 0xff;
                        }

                        if (a == 0) {
                            f |= F_ZERO;
                        }
                        break;
                    case 0xCF: // RST 08
                        pushPC();
                        setPC(0x08);
                        break;
                    case 0xD0: // RET NC
                        if ((f & F_CARRY) == 0) {
                            popPC();
                        }
                        break;
                    case 0xD1: // POP DE
                        e = addressRead(sp++) & 0xff;
                        d = addressRead(sp++) & 0xff;
                        break;
                    case 0xD2: // JP NC, nnnn
                        if ((f & F_CARRY) == 0) {
                            setPC(((b3 & 0xff) << 8) + b2);
                        } else {
                            localPC += 2;
                        }
                        break;
                    case 0xD4: // CALL NC, nnnn
                        localPC += 2;
                        if ((f & F_CARRY) == 0) {
                            pushPC();
                            setPC(((b3 & 0xff) << 8) + b2);
                        }
                        break;
                    case 0xD5: // PUSH DE
                        addressWrite(--sp, d);
                        addressWrite(--sp, e);
                        break;
                    case 0xD6: // SUB A, nn
                        localPC++;

                        f = F_SUBTRACT;

                        if ((a & 0x0F) < (b2 & 0x0F)) {
                            f |= F_HALFCARRY;
                        }

                        a -= b2;

                        if (a < 0) {
                            f |= F_CARRY;
                            a &= 0xff;
                        } else if (a == 0) {
                            f |= F_ZERO;
                        }
                        break;
                    case 0xD7: // RST 10
                        pushPC();
                        setPC(0x10);
                        break;
                    case 0xD8: // RET C
                        if ((f & F_CARRY) != 0) {
                            popPC();
                        }
                        break;
                    case 0xD9: // RETI
                        interruptsEnabled = true;
                        if (interruptsArmed) {
                            nextTimedInterrupt = instrCount;
                        }
                        popPC();
                        break;
                    case 0xDA: // JP C, nnnn
                        if ((f & F_CARRY) != 0) {
                            setPC(((b3 & 0xff) << 8) + b2);
                        } else {
                            localPC += 2;
                        }
                        break;
                    case 0xDC: // CALL C, nnnn
                        localPC += 2;
                        if ((f & F_CARRY) != 0) {
                            pushPC();
                            setPC(((b3 & 0xff) << 8) + b2);
                        }
                        break;
                    case 0xDE: // SBC A, nn
                        localPC++;
                        if ((f & F_CARRY) != 0) {
                            b2++;
                        }

                        f = F_SUBTRACT;
                        if ((a & 0x0F) < (b2 & 0x0F)) {
                            f |= F_HALFCARRY;
                        }

                        a -= b2;

                        if (a < 0) {
                            f |= F_CARRY;
                            a &= 0xff;
                        } else if (a == 0) {
                            f |= F_ZERO;
                        }
                        break;
                    case 0xDF: // RST 18
                        pushPC();
                        setPC(0x18);
                        break;
                    case 0xE0: // LDH (FFnn), A
                        localPC++;
                        ioWrite(b2, a);
                        break;
                    case 0xE1: // POP HL
                        hl = ((addressRead(sp + 1) & 0xff) << 8) + (addressRead(sp) & 0xff);
                        sp += 2;
                        break;
                    case 0xE2: // LDH (FF00 + C), A
                        ioWrite(c, a);
                        break;
                    case 0xE5: // PUSH HL
                        addressWrite(--sp, hl >> 8);
                        addressWrite(--sp, hl);
                        break;
                    case 0xE6: // AND nn
                        localPC++;
                        a &= b2;
                        if (a == 0)
                            f = F_ZERO;
                        else
                            f = 0;
                        break;
                    case 0xE7: // RST 20
                        pushPC();
                        setPC(0x20);
                        break;
                    case 0xE8: // ADD SP, nn
                        localPC++;

                        sp += offset;
                        f = 0;
                        if (sp > 0xffff || sp < 0) {
                            sp &= 0xffff;
                            f = F_CARRY;
                        }
                        break;
                    case 0xE9: // JP (HL)
                        setPC(hl);
                        break;
                    case 0xEA: // LD (nnnn), A
                        localPC += 2;
                        addressWrite(((b3 & 0xff) << 8) + b2, a);
                        break;
                    case 0xEE: // XOR A, nn
                        localPC++;
                        a ^= b2;
                        f = 0;
                        if (a == 0)
                            f = F_ZERO;
                        break;
                    case 0xEF: // RST 28
                        pushPC();
                        setPC(0x28);
                        break;
                    case 0xF0: // LDH A, (FFnn)
                        localPC++;
                        a = ioRead(b2) & 0xff; // fixme, direct access?

                        break;
                    case 0xF1: // POP AF
                        f = addressRead(sp++) & 0xff; // fixme, f0 or ff?
                        a = addressRead(sp++) & 0xff;
                        break;
                    case 0xF2: // LD A, (FF00 + C)
                        a = ioRead(c) & 0xff; // fixme, direct access?
                        break;
                    case 0xF3: // DI
                        interruptsEnabled = false;
                        break;
                    case 0xF5: // PUSH AF
                        addressWrite(--sp, a);
                        addressWrite(--sp, f);
                        break;
                    case 0xF6: // OR A, nn
                        localPC++;
                        a |= b2;
                        f = 0;
                        if (a == 0) {
                            f = F_ZERO;
                        }
                        break;
                    case 0xF7: // RST 30
                        pushPC();
                        setPC(0x30);
                        break;
                    case 0xF8: // LD HL, SP + nn  ** HALFCARRY FLAG NOT SET ***
                        localPC++;
                        hl = (sp + offset);
                        f = 0;
                        if ((hl & 0xffff0000) != 0) {
                            f = F_CARRY;
                            hl &= 0xFFFF;
                        }
                        break;
                    case 0xF9: // LD SP, HL
                        sp = hl;
                        break;
                    case 0xFA: // LD A, (nnnn)
                        localPC += 2;
                        a = addressRead(((b3 & 0xff) << 8) + b2) & 0xff;
                        break;
                    case 0xFB: // EI
                        interruptEnableRequested = true;
                        nextTimedInterrupt = instrCount + cyclesPerInstr[b1] + 1; // fixme, this is an ugly hack
                        break;
                    case 0xFE: // CP nn
                        localPC++;
                        f = ((a & 0x0F) < (b2 & 0x0F)) ? F_HALFCARRY | F_SUBTRACT : F_SUBTRACT;
                        if (a == b2) {
                            f |= F_ZERO;
                        } else if (a < b2) {
                            f |= F_CARRY;
                        }
                        break;
                    case 0xFF: // RST 38
                        pushPC();
                        setPC(0x38);
                        break;

                    default:
                        if ((b1 & 0xC0) == 0x80) { // Byte 0x10?????? indicates ALU op, i.e. 0x80 - 0xbf
                            executeALU(b1);
                        } else {
                            throw new RuntimeException(Integer.toHexString(b1));
                        }
                }

                instrCount += cyclesPerInstr[b1];

                if (instrCount - nextTimedInterrupt >= 0) {
                    initiateInterrupts();

                    if (interruptsArmed && interruptsEnabled) {
                        checkInterrupts();
                    }
                }
            }
        } catch (Exception ex) {
            terminate();
            ex.printStackTrace();
            Common.showError(null, "error#20", ex);
        }
    }

// IOHandler

    /**
     * Initialize IO to initial power on state
     */
    private final void ioHandlerReset() {
        ioWrite(0x0F, 0x01);
        ioWrite(0x26, 0xf1);
        ioWrite(0x40, 0x91);
        ioWrite(0x47, 0xFC);
        ioWrite(0x48, 0xFF);
        ioWrite(0x49, 0xFF);
        registers[0x55] = (byte) 0x80;
        hdmaRunning = false;
    }

    /**
     * Read data from IO Ram
     */
    private final int ioRead(int num) {
        if (num == 0x41) {
            // LCDSTAT
            int output = registers[0x41];

            if (registers[0x44] == registers[0x45]) {
                output |= 4;
            }

            if ((registers[0x44] & 0xff) >= 144) {
                output |= 1; // mode 1
            } else {
                output |= graphicsChipMode;
            }

            return output;
        } else if (num == 0x04) {
            // DIV
            return (byte) ((instrCount - divReset - 1) / INSTRS_PER_DIV);
        } else if (num == 0x05) {
            // TIMA
            if (!timaActive)
                return registers[num];

            return ((instrCount + instrsPerTima * 0x100 - nextTimaOverflow) / instrsPerTima);
        }

        return registers[num];
    }

    /**
     * Write data to IO Ram
     */
    public void ioWrite(int num, int data) {
        switch (num) {
            case 0x00: // FF00 - Joypad
                int output = 0;
                if ((data & 0x10) == 0) {
                    // P14
                    output |= controller.buttonState & 0x0f;
                }

                if ((data & 0x20) == 0) {
                    // P15
                    output |= controller.buttonState >> 4;
                }
                // the high nybble is unused for reading (according to gbcpuman), but Japanese Pokemon
                // seems to require it to be set to f
                registers[0x00] = (byte) (0xf0 | (~output & 0x0f));

                break;

            case 0x02: // Serial
                registers[0x02] = (byte) data;

                if ((registers[0x02] & 0x01) == 1) {
                    registers[0x01] = (byte) 0xFF; // when no LAN connection, always receive 0xFF from port.  Simulates empty socket.
                    if ((registers[0xff] & INT_SER) != 0) {
                        interruptsArmed = true;
                        registers[0x0f] |= INT_SER;

                        if (interruptsEnabled)
                            nextTimedInterrupt = instrCount;
                    }
                    registers[0x02] &= 0x7F;
                }

                break;

            case 0x04: // DIV
                divReset = instrCount;
                break;

            case 0x05: // TIMA
                if (timaActive)
                    nextTimaOverflow = instrCount + instrsPerTima * (0x100 - (data & 0xff));
                break;

            case 0x07: // TAC
                if ((data & 0x04) != 0) {
                    if (!timaActive) {
                        timaActive = true;
                        nextTimaOverflow = instrCount + instrsPerTima * (0x100 - (registers[0x05] & 0xff));
                    }

                    instrsPerTima = 4 << (2 * ((data - 1) & 3));
                    // 0-3 -> {256, 4, 16, 64}
                } else {
                    if (timaActive) {
                        timaActive = false;
                        registers[0x05] = (byte) ((instrCount + instrsPerTima * 0x100 - nextTimaOverflow) / instrsPerTima);
                    }
                }
                registers[num] = (byte) data;
                break;

            case 0x0f:
                registers[num] = (byte) data;
                interruptsArmed = (registers[0xff] & registers[0x0f]) != 0;

                if (interruptsArmed && interruptsEnabled)
                    nextTimedInterrupt = instrCount;
                break;

            // sound registers: 10 11 12 13 14 16 17 18 19 1a 1b 1c 1d 1e 20 21 22 23 24 25 26 30-3f
            // 0x12: Sound 1 volume + volume sweep
            // 0x13: Sound 1 freq 0-7
            // 0x14: Sound 1 frequency 8-10 + control
            // 0x15: Unused
            // 0x16: Sound 2 length
            // 0x17: Sound 2 volume + volume sweep
            // 0x18: Sound 2 freq 0-7
            // 0x19: Sound 2 frequency 8-10 + control
            // 0x1a: sound mode 3, on/off
            // 0x1b: Sound 3 length
            // 0x1c: Sound 3 volume
            // 0x1d: Sound 3 freq 0-7
            // 0x1e: Sound 3 frequency 8-10 + control
            // 0x20-0x23: Sound 4
            // 0x24: Channel control
            // 0x25: Selection of sound terminal
            // 0x26: Sound on/off
            // 0x30-0x3f: Sound 3 wave pattern

            case 0x11:
            case 0x12:
            case 0x13:
            case 0x14:

            case 0x17:
            case 0x18:
            case 0x19:

            case 0x1C:
            case 0x1D:
            case 0x1E:

            case 0x20:
            case 0x21:
            case 0x22:
            case 0x23:
                registers[num] = (byte) data;
                if (speaker != null) {
                    speaker.ioWrite(num, data);
                }
                break;

            case 0x40: // LCDC
                screen.UpdateLCDCFlags(data);
                registers[num] = (byte) data;
                break;

            case 0x41: // LCDC
                registers[num] = (byte) (data & 0xf8);
                break;

            case 0x46: // DMA
                System.arraycopy(memory[data >> 5], (data << 8) & 0x1f00, oam, 0, 0xa0);
                // This is meant to be run at the same time as the ProcessingChip is executing
                // instructions, but I don't think it's crucial.
                break;

            case 0x47: // FF47 - BKG and WIN palette
                screen.decodePalette(0, data);
                if (registers[num] != (byte) data) {
                    registers[num] = (byte) data;
                    screen.invalidateAll(0);
                }
                break;

            case 0x48: // FF48 - OBJ1 palette
                screen.decodePalette(4, data);
                if (registers[num] != (byte) data) {
                    registers[num] = (byte) data;
                    screen.invalidateAll(1);
                }
                break;

            case 0x49: // FF49 - OBJ2 palette
                screen.decodePalette(8, data);
                if (registers[num] != (byte) data) {
                    registers[num] = (byte) data;
                    screen.invalidateAll(2);
                }
                break;

            case 0x4A: // FF4A - Window Position Y
                if ((data & 0xff) >= 144)
                    screen.stopWindowFromLine();
                registers[num] = (byte) data;
                break;

            case 0x4B: // FF4B - Window Position X
                if ((data & 0xff) >= 167)
                    screen.stopWindowFromLine();
                registers[num] = (byte) data;
                break;

            case 0x4D: // FF4D - KEY1 - CGB Mode Only - Prepare Speed Switch
                if (gbcFeatures) {
                    // high bit is read only
                    registers[num] = (byte) ((data & 0x7f) + (registers[num] & 0x80));
                } else {
                    registers[num] = (byte) data;
                }
                break;

            case 0x4F: // FF4F - VRAM Bank - GBC only
                if (gbcFeatures) {
                    screen.setVRamBank(data & 0x01);
                }
                registers[num] = (byte) data;
                break;

            case 0x55: // FF55 - HDMA5 - GBC only
                if (gbcFeatures) {
                    if (!hdmaRunning && (data & 0x80) == 0) {
                        int dmaSrc = ((registers[0x51] & 0xff) << 8) + (registers[0x52] & 0xF0);
                        int dmaDst = ((registers[0x53] & 0x1F) << 8) + (registers[0x54] & 0xF0);
                        int dmaLen = ((data & 0x7F) * 16) + 16;

                        for (int r = 0; r < dmaLen; r++) {
                            screen.addressWrite(dmaDst + r, (byte) addressRead(dmaSrc + r));
                        }
                        // fixme, move instrCount?

                        registers[0x55] = (byte) 0xff;
                    } else if ((data & 0x80) != 0) {
                        // start hdma
                        hdmaRunning = true;
                        registers[0x55] = (byte) (data & 0x7F);
                    } else {
                        // stop hdma
                        hdmaRunning = false;
                        registers[0x55] |= 0x80;
                    }
                } else {
                    registers[num] = (byte) data;
                }
                break;

            case 0x68: // FF68 - Background Palette Index - GBC only
                if (gbcFeatures) {
                    registers[0x69] = (byte) screen.getGBCPalette(data & 0x3f);
                }
                registers[num] = (byte) data;
                break;

            case 0x69: // FF69 - Background Palette Data - GBC only
                if (gbcFeatures) {
                    screen.setGBCPalette(registers[0x68] & 0x3f, data & 0xff);

                    if (registers[0x68] < 0) { // high bit = autoincrement
                        int next = ((registers[0x68] + 1) & 0x3f);
                        registers[0x68] = (byte) (next + 0x80);
                        registers[0x69] = (byte) screen.getGBCPalette(next);
                    } else {
                        registers[num] = (byte) data;
                    }
                } else {
                    registers[num] = (byte) data;
                }
                break;

            case 0x6A: // FF6A - Sprite Palette Index - GBC only
                if (gbcFeatures) {
                    registers[0x6B] = (byte) screen.getGBCPalette((data & 0x3f) + 0x40);
                }
                registers[0x6A] = (byte) data;
                break;

            case 0x6B: // FF6B - Sprite Palette Data - GBC only
                if (gbcFeatures) {
                    screen.setGBCPalette((registers[0x6A] & 0x3f) + 0x40, data & 0xff);

                    if (registers[0x6A] < 0) { // high bit = autoincrement
                        int next = ((registers[0x6A] + 1) & 0x3f);
                        registers[0x6A] = (byte) (next + 0x80);
                        registers[0x6B] = (byte) screen.getGBCPalette(next + 0x40);
                    } else {
                        registers[num] = (byte) data;
                    }
                } else {
                    registers[num] = (byte) data;
                }
                break;

            case 0x70: // FF70 - GBC Work RAM bank
                if (gbcFeatures) {
                    if ((data & 0x07) < 2) {
                        gbcRamBank = 1;
                    } else {
                        gbcRamBank = data & 0x07;
                    }

                    if (globalPC >= 0xC000) {
                        // verify cruising if executing in RAM
                        setPC(globalPC + localPC);
                    }
                }
                registers[num] = (byte) data;
                break;

            case 0xff:
                registers[num] = (byte) data;
                interruptsArmed = (registers[0xff] & registers[0x0f]) != 0;
                if (interruptsArmed && interruptsEnabled)
                    nextTimedInterrupt = instrCount;
                break;

            default:
                registers[num] = (byte) data;
                break;
        }
    }


    /**
     * Maps a ROM bank into the ProcessingChip address space at 0x4000
     */
    private final void mapRom(int bankNo) {
        bankNo = bankNo & ((cartridge.rom.length >> 1) - 1);
        currentRomBank = bankNo;
        memory[2] = cartridge.rom[bankNo * 2];
        memory[3] = cartridge.rom[bankNo * 2 + 1];
        if ((globalPC & 0xC000) == 0x4000) {
            setPC(localPC + globalPC);
        }
    }

    private final void mapRam(int bankNo) {
        currentRamBank = bankNo;
        if (currentRamBank < cartridge.ram.length)
            memory[5] = cartridge.ram[currentRamBank];
    }

    /**
     * Writes to an address in ProcessingChip address space.  Writes to ROM may cause a mapping change.
     */
    private final void cartridgeWrite(int addr, int data) {
        int halfbank = addr >> 13;
        int subaddr = addr & 0x1fff;

        switch (cartridge.type) {
            case 0:
                // ROM Only
                break;
            case 1:
            case 2:
            case 3:
                // MBC1
                if (halfbank == 0) {
                    cartRamEnabled = ((data & 0x0F) == 0x0A);
                } else if (halfbank == 1) {
                    int bankNo = data & 0x1F;
                    if (bankNo == 0)
                        bankNo = 1;
                    mapRom((currentRomBank & 0x60) | bankNo);
                } else if (halfbank == 2) {
                    if (mbc1LargeRamMode) {
                        mapRam(data & 0x03);
                    } else {
                        mapRom((currentRomBank & 0x1F) | ((data & 0x03) << 5));
                    }
                } else if (halfbank == 3) {
                    mbc1LargeRamMode = ((data & 1) == 1);
                } else if (halfbank == 5 && memory[halfbank] != null) {
                    // fixme, we should check cartRamEnabled, but that seems
                    // to break Pokemon yellow... (which uses MBC5, but I'm erring
                    // on the side of caution).
                    memory[halfbank][subaddr] = (byte) data;
                }
                break;
            case 5:
            case 6:
                // MBC2
                if ((halfbank == 1)) {
                    if ((addr & 0x0100) != 0) {
                        int bankNo = data & 0x0F;
                        if (bankNo == 0)
                            bankNo = 1;
                        mapRom(bankNo);
                    } else {
                        cartRamEnabled = ((data & 0x0F) == 0x0A);
                    }
                } else if (halfbank == 5 && memory[halfbank] != null) {
                    // fixme, we should check cartRamEnabled, but that seems
                    // to break Pokemon yellow... (which uses MBC5, but I'm erring
                    // on the side of caution).
                    memory[halfbank][subaddr] = (byte) data;
                }

                break;

            case 0x0F:
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
                // MBC3
                if (halfbank == 0) {
                    cartRamEnabled = ((data & 0x0F) == 0x0A);
                } else if (halfbank == 1) {
                    // Select ROM bank
                    int bankNo = data & 0x7F;
                    if (bankNo == 0)
                        bankNo = 1;
                    mapRom(bankNo);
                } else if (halfbank == 2) {
                    // Select RAM bank
                    if (cartridge.ram.length > 0)
                        mapRam(data & 0x0f); // only 0-3 for ram banks, 8+ for RTC
                } else if (halfbank == 3) {
                    // fixme, rtc latch
                } else if (halfbank == 5) {
                    // memory write
                    if (currentRamBank >= 8) {
                        // rtc register
                        cartridge.rtcSync();
                        cartridge.rtcReg[currentRamBank - 8] = (byte) data;
                    } else if (memory[halfbank] != null) {
                        // normal memory
                        // fixme, we should check cartRamEnabled, but that seems
                        // to break Pokemon yellow... (which uses MBC5, but I'm erring
                        // on the side of caution).
                        memory[halfbank][subaddr] = (byte) data;
                    }
                }
                break;
            case 0x19:
            case 0x1A:
            case 0x1B:
            case 0x1C:
            case 0x1D:
            case 0x1E:
                // MBC5
                if (addr >> 12 == 1) {
                    cartRamEnabled = ((data & 0x0F) == 0x0A);
                } else if (addr >> 12 == 2) {
                    int bankNo = (currentRomBank & 0xFF00) | data;
                    // note: bank 0 can be mapped to 0x4000
                    mapRom(bankNo);
                } else if (addr >> 12 == 3) {
                    int bankNo = (currentRomBank & 0x00FF) | ((data & 0x01) << 8);
                    // note: bank 0 can be mapped to 0x4000
                    mapRom(bankNo);
                } else if (halfbank == 2) {
                    if (cartridge.ram.length > 0)
                        mapRam(data & 0x0f);
                } else if (halfbank == 5) {
                    if (memory[halfbank] != null) {
                        // fixme, we should check cartRamEnabled, but that seems
                        // to break Pokemon yellow...
                        memory[halfbank][subaddr] = (byte) data;
                    }
                }
                break;
        }
    }

    public boolean isTerminated() {
        return terminate;
    }

    public void terminate() {
        terminate = true;
    }

    // STATUS

    public byte[] sarm() {
        if (cartridge.hasBattery) return cartridge.dumpSram();
        return null;
    }

    public void setSarm(byte[] sarm) {
        if (sarm != null) {
            cartridge.setSram(sarm);
        }
    }

    public void unflatten(byte[] flatState) {
        int offset = 0;

        int version = flatState[offset++];
        boolean flatGbcFeatures = flatState[offset++] != 0;
        if (version != 1 || flatGbcFeatures != gbcFeatures)
            throw new RuntimeException("Can't unflatten");

        a = flatState[offset++] & 0xff;
        b = flatState[offset++] & 0xff;
        c = flatState[offset++] & 0xff;
        d = flatState[offset++] & 0xff;
        e = flatState[offset++] & 0xff;
        f = flatState[offset++] & 0xff;
        sp = flatState[offset++] & 0xff;
        sp = (sp << 8) + (flatState[offset++] & 0xff);
        hl = flatState[offset++] & 0xff;
        hl = (hl << 8) + (flatState[offset++] & 0xff);
        int pc = flatState[offset++] & 0xff;
        pc = (pc << 8) + (flatState[offset++] & 0xff);
        // setPC() will be called below to set the fields.

        instrCount = Common.getInt(flatState, offset);
        offset += 4;
        nextModeTime = Common.getInt(flatState, offset);
        offset += 4;
        nextTimaOverflow = Common.getInt(flatState, offset);
        offset += 4;
        nextTimedInterrupt = Common.getInt(flatState, offset);
        offset += 4;

        timaActive = flatState[offset++] != 0;
        graphicsChipMode = flatState[offset++];
        interruptsEnabled = flatState[offset++] != 0;
        interruptsArmed = flatState[offset++] != 0;
        interruptEnableRequested = flatState[offset++] != 0;

        System.arraycopy(flatState, offset, mainRam, 0, mainRam.length);
        offset += mainRam.length;
        System.arraycopy(flatState, offset, oam, 0, 0x00A0);
        offset += 0x00A0;
        System.arraycopy(flatState, offset, registers, 0, 0x0100);
        offset += 0x0100;

        divReset = Common.getInt(flatState, offset);
        offset += 4;
        instrsPerTima = Common.getInt(flatState, offset);
        offset += 4;

        // cartridge
        for (int i = 0; i < cartridge.ram.length; i++) {
            System.arraycopy(flatState, offset, cartridge.ram[i], 0, 0x2000);
            offset += 0x2000;
        }

        currentRomBank = Common.getInt(flatState, offset);
        offset += 4;
        mapRom(currentRomBank);

        currentRamBank = Common.getInt(flatState, offset);
        offset += 4;
        if (currentRamBank != 0)
            mapRam(currentRamBank);

        mbc1LargeRamMode = (flatState[offset++] != 0);
        cartRamEnabled = (flatState[offset++] != 0);

        // realtime clock
        System.arraycopy(flatState, offset, cartridge.rtcReg, 0, cartridge.rtcReg.length);
        offset += cartridge.rtcReg.length;

        offset = screen.unflatten(flatState, offset);

        if (gbcFeatures) {
            gbcRamBank = flatState[offset++] & 0xff;
            hdmaRunning = flatState[offset++] != 0;

            if ((registers[0x4D] & 0x80) != 0) {
                // double speed
                INSTRS_IN_MODE_0 = BASE_INSTRS_IN_MODE_0;
                INSTRS_IN_MODE_2 = BASE_INSTRS_IN_MODE_2;
                INSTRS_IN_MODE_3 = BASE_INSTRS_IN_MODE_3;
            } else {
                // instrs_in_mode are set correctly already
            }
        }

        setPC(pc);

        if (offset != flatState.length)
            throw new RuntimeException("Loading the game failed" + ": " + offset + ", " + flatState.length);
    }

    public byte[] flatten() {
        int size = 53 + mainRam.length + 0x01A0 + 0x2000 * cartridge.ram.length + cartridge.rtcReg.length
                + 0x2000 + 48;
        if (gbcFeatures) {
            size += 2 + 129 + 0x2000;
        }

        byte[] flatState = new byte[size];
        int offset = 0;

        flatState[offset++] = (byte) 1; // version
        flatState[offset++] = (byte) (gbcFeatures ? 1 : 0);

        flatState[offset++] = (byte) a;
        flatState[offset++] = (byte) b;
        flatState[offset++] = (byte) c;
        flatState[offset++] = (byte) d;
        flatState[offset++] = (byte) e;
        flatState[offset++] = (byte) f;
        flatState[offset++] = (byte) (sp >> 8);
        flatState[offset++] = (byte) sp;
        flatState[offset++] = (byte) (hl >> 8);
        flatState[offset++] = (byte) hl;
        int pc = localPC + globalPC;
        flatState[offset++] = (byte) (pc >> 8);
        flatState[offset++] = (byte) pc;

        Common.setInt(flatState, offset, instrCount);
        offset += 4;
        Common.setInt(flatState, offset, nextModeTime);
        offset += 4;
        Common.setInt(flatState, offset, nextTimaOverflow);
        offset += 4;
        Common.setInt(flatState, offset, nextTimedInterrupt);
        offset += 4;

        flatState[offset++] = (byte) (timaActive ? 1 : 0);
        flatState[offset++] = (byte) (graphicsChipMode);
        flatState[offset++] = (byte) (interruptsEnabled ? 1 : 0);
        flatState[offset++] = (byte) (interruptsArmed ? 1 : 0);
        flatState[offset++] = (byte) (interruptEnableRequested ? 1 : 0);

        System.arraycopy(mainRam, 0, flatState, offset, mainRam.length);
        offset += mainRam.length;
        System.arraycopy(oam, 0, flatState, offset, 0x00A0);
        offset += 0x00A0;
        System.arraycopy(registers, 0, flatState, offset, 0x0100);
        offset += 0x0100;

        Common.setInt(flatState, offset, divReset);
        offset += 4;
        Common.setInt(flatState, offset, instrsPerTima);
        offset += 4;

        for (int j = 0; j < cartridge.ram.length; j++) {
            System.arraycopy(cartridge.ram[j], 0, flatState, offset, 0x2000);
            offset += 0x2000;
        }

        Common.setInt(flatState, offset, currentRomBank);
        offset += 4;
        Common.setInt(flatState, offset, currentRamBank);
        offset += 4;

        flatState[offset++] = (byte) (mbc1LargeRamMode ? 1 : 0);
        flatState[offset++] = (byte) (cartRamEnabled ? 1 : 0);

        System.arraycopy(cartridge.rtcReg, 0, flatState, offset, cartridge.rtcReg.length);
        offset += cartridge.rtcReg.length;

        offset = screen.flatten(flatState, offset);

        if (gbcFeatures) {
            flatState[offset++] = (byte) gbcRamBank;
            flatState[offset++] = (byte) (hdmaRunning ? 1 : 0);
        }

        if (offset != flatState.length)
            throw new RuntimeException("error#21: " + offset + ", " + flatState.length);

        return flatState;
    }

    // CHEAT
    // HashMap Multithreading put unsafe, but ...

    private Map<Integer, Cheat> cheatMap = new TreeMap<Integer, Cheat>();

    public void setCheat(Cheat cheat) {
        if (cheat == null) return;
        cheatMap.put(cheat.address, cheat);
    }

    public void setCheat(String code) {
        setCheat(Cheat.newCheat(code));
    }

    public void remoteAllCheat() {
        cheatMap.clear();
    }

    public final int addressRead(int addr) {
        int result = addressRead1(addr);
        Cheat cheat = cheatMap.get(addr);
        if (cheat != null) {
             if (cheat.ifIs == null || cheat.ifIs == result) {
                return cheat.changeTo & 0xFF;
            }
        }
        return result;
    }

    // OPTIONS

    public void setSoundEnable(boolean channelEnable) {
        if (speaker != null) {
            speaker.setSoundEnabled(channelEnable);
        }
    }

    public void setChannelEnable(int channel, boolean enable) {
        if (speaker != null) {
            speaker.setChannelEnable(channel, enable);
        }
    }

    public void setSpeed(int i) {
        speaker.setSpeed(i);
        screen.setSpeed(i);
    }

    // THREAD

    private Thread thread;

    public boolean running() {
        return thread != null && thread.isAlive();
    }

    public void startup() {
        if (running()) {
            throw new RuntimeException("Started");
        } else {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void shutdown() {
        if (running()) {
            terminate();
            while (thread.isAlive()) {
                Thread.yield();
            }
        } else {
            throw new RuntimeException("");
        }
    }

}