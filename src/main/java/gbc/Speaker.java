package gbc;

import java.lang.*;
import java.util.Random;
import javax.sound.sampled.*;

/**
 * This is the central controlling class for the sound.
 * It interfaces with the Java Sound API, and handles the
 * calsses for each sound channel.
 */
class Speaker {
    private final Speed speed = new Speed();

    private final byte[] registers;
    /**
     * The DataLine for outputting the sound
     */
    SourceDataLine soundLine;

    SquareWaveGenerator channel1;
    SquareWaveGenerator channel2;
    VoluntaryWaveGenerator channel3;
    NoiseGenerator channel4;

    boolean soundEnabled = false;

    /**
     * If true, channel is enabled
     */
    boolean channel1Enable = true, channel2Enable = true,
            channel3Enable = true, channel4Enable = true;

    /**
     * Current sampling rate that sound is output at
     */
    int sampleRate = 44100;

    /**
     * Amount of sound data to buffer before playback
     */
    int bufferLengthMsec = 200;

    /**
     * Initialize sound emulation, and allocate sound hardware
     */
    public Speaker(byte[] registers) {
        this.registers = registers;
        soundLine = initSoundHardware();
        channel1 = new SquareWaveGenerator(sampleRate);
        channel2 = new SquareWaveGenerator(sampleRate);
        channel3 = new VoluntaryWaveGenerator(sampleRate);
        channel4 = new NoiseGenerator(sampleRate);
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public void setChannelEnable(int channel, boolean enable) {
        switch (channel) {
            case 1:
                this.channel1Enable = enable;
                break;
            case 2:
                this.channel2Enable = enable;
                break;
            case 3:
                this.channel3Enable = enable;
                break;
            case 4:
                this.channel4Enable = enable;
                break;
        }
    }

    public void ioWrite(int num, int data) {

        switch (num) {
            // 0x10: Sound 1 freq sweep
            // 0x11: Sound 1 length

            // 0x20-0x23: Sound 4

            // 0x24: Channel control

            // 0x25: Selection of sound terminal

            // 0x26: Sound on/off

            // 0x30-0x3f: Sound 3 wave pattern

            case 0x10: // RL r
                this.channel1.setSweep(
                        (Common.unsign((byte) data) & 0x70) >> 4,
                        (Common.unsign((byte) data) & 0x07),
                        (Common.unsign((byte) data) & 0x08) == 1);
                break;

            case 0x11:           // Sound channel 1, length and wave duty
                this.channel1.setDutyCycle((Common.unsign((byte) data) & 0xC0) >> 6);
                this.channel1.setLength(Common.unsign((byte) data) & 0x3F);
                break;

            case 0x12:           // Sound channel 1, volume envelope
                this.channel1.setEnvelope(
                        (Common.unsign((byte) data) & 0xF0) >> 4,
                        (Common.unsign((byte) data) & 0x07),
                        (Common.unsign((byte) data) & 0x08) == 8);
                break;

            case 0x13:           // Sound channel 1, frequency low
                this.channel1.setFrequency(
                        ((int) (Common.unsign(registers[0x14]) & 0x07) << 8) + Common.unsign(registers[0x13]));
                break;

            case 0x14:           // Sound channel 1, frequency high
                if ((registers[0x14] & 0x80) != 0) {
                    this.channel1.setLength(Common.unsign(registers[0x11]) & 0x3F);
                    this.channel1.setEnvelope(
                            (Common.unsign(registers[0x12]) & 0xF0) >> 4,
                            (Common.unsign(registers[0x12]) & 0x07),
                            (Common.unsign(registers[0x12]) & 0x08) == 8);
                }

                if ((registers[0x14] & 0x40) == 0) {
                    this.channel1.setLength(-1);
                }

                this.channel1.setFrequency(
                        ((int) (Common.unsign(registers[0x14]) & 0x07) << 8) + Common.unsign(registers[0x13]));

                break;

            case 0x16:           // Sound channel 2, length and wave duty
                this.channel2.setDutyCycle((Common.unsign((byte) data) & 0xC0) >> 6);
                this.channel2.setLength(Common.unsign((byte) data) & 0x3F);
                break;

            case 0x17:           // Sound channel 2, volume envelope
                this.channel2.setEnvelope(
                        (Common.unsign((byte) data) & 0xF0) >> 4,
                        (Common.unsign((byte) data) & 0x07),
                        (Common.unsign((byte) data) & 0x08) == 8);
                break;

            case 0x18:
                this.channel2.setFrequency(
                        ((int) (Common.unsign(registers[0x19]) & 0x07) << 8) + Common.unsign(registers[0x18]));
                break;

            case 0x19:           // Sound channel 2, frequency high
                if ((registers[0x19] & 0x80) != 0) {
                    this.channel2.setLength(Common.unsign(registers[0x21]) & 0x3F);
                    this.channel2.setEnvelope(
                            (Common.unsign(registers[0x17]) & 0xF0) >> 4,
                            (Common.unsign(registers[0x17]) & 0x07),
                            (Common.unsign(registers[0x17]) & 0x08) == 8);
                }
                if ((registers[0x19] & 0x40) == 0) {
                    this.channel2.setLength(-1);
                }
                this.channel2.setFrequency(
                        ((int) (Common.unsign(registers[0x19]) & 0x07) << 8) + Common.unsign(registers[0x18]));

                break;

            case 0x1A:           // Sound channel 3, on/off
                if ((Common.unsign((byte) data) & 0x80) != 0) {
                    this.channel3.setVolume((Common.unsign(registers[0x1C]) & 0x60) >> 5);
                } else {
                    this.channel3.setVolume(0);
                }

                break;

            case 0x1B:           // Sound channel 3, length
                this.channel3.setLength(Common.unsign((byte) data));
                break;

            case 0x1C:           // Sound channel 3, volume
                this.channel3.setVolume((Common.unsign(registers[0x1C]) & 0x60) >> 5);
                break;

            case 0x1D:           // Sound channel 3, frequency lower 8-bit
                this.channel3.setFrequency(
                        ((int) (Common.unsign(registers[0x1E]) & 0x07) << 8) + Common.unsign(registers[0x1D]));
                break;

            case 0x1E:           // Sound channel 3, frequency higher 3-bit
            {
                if ((registers[0x19] & 0x80) != 0) {
                    this.channel3.setLength(Common.unsign(registers[0x1B]));
                }
                this.channel3.setFrequency(
                        ((int) (Common.unsign(registers[0x1E]) & 0x07) << 8) + Common.unsign(registers[0x1D]));
            }
            break;

            case 0x20:           // Sound channel 4, length
                this.channel4.setLength(Common.unsign((byte) data) & 0x3F);
                break;


            case 0x21:           // Sound channel 4, volume envelope
                this.channel4.setEnvelope(
                        (Common.unsign((byte) data) & 0xF0) >> 4,
                        (Common.unsign((byte) data) & 0x07),
                        (Common.unsign((byte) data) & 0x08) == 8);
                break;

            case 0x22:           // Sound channel 4, polynomial parameters
                this.channel4.setParameters(
                        (Common.unsign((byte) data) & 0x07),
                        (Common.unsign((byte) data) & 0x08) == 8,
                        (Common.unsign((byte) data) & 0xF0) >> 4);
                break;

            case 0x23:          // Sound channel 4, initial/consecutive
            {
                if ((registers[0x23] & 0x80) != 0) {
                    this.channel4.setLength(Common.unsign(registers[0x20]) & 0x3F);
                }
                if ((registers[0x23] & 0x40) == 0) {
                    this.channel4.setLength(-1);
                }
            }
            break;

            case 0x25:           // Stereo select
                int chanData;
            {
                chanData = 0;
                if ((Common.unsign((byte) data) & 0x01) != 0) {
                    chanData |= SquareWaveGenerator.CHAN_LEFT;
                }
                if ((Common.unsign((byte) data) & 0x10) != 0) {
                    chanData |= SquareWaveGenerator.CHAN_RIGHT;
                }
                this.channel1.setChannel(chanData);

                chanData = 0;
                if ((Common.unsign((byte) data) & 0x02) != 0) {
                    chanData |= SquareWaveGenerator.CHAN_LEFT;
                }
                if ((Common.unsign((byte) data) & 0x20) != 0) {
                    chanData |= SquareWaveGenerator.CHAN_RIGHT;
                }
                this.channel2.setChannel(chanData);

                chanData = 0;
                if ((Common.unsign((byte) data) & 0x04) != 0) {
                    chanData |= SquareWaveGenerator.CHAN_LEFT;
                }
                if ((Common.unsign((byte) data) & 0x40) != 0) {
                    chanData |= SquareWaveGenerator.CHAN_RIGHT;
                }
                this.channel3.setChannel(chanData);
            }

            break;
        }
    }

    /**
     * Adds a single frame of sound data to the buffer
     */
    public void outputSound() {
        if (soundEnabled && speed.output()) {
            int numSamples;

            if (sampleRate / 28 >= soundLine.available() * 2) {
                numSamples = soundLine.available() * 2;
            } else {
                numSamples = (sampleRate / 28) & 0xFFFE;
            }

            byte[] b = new byte[numSamples];
            if (channel1Enable) channel1.play(b, numSamples / 2, 0);
            if (channel2Enable) channel2.play(b, numSamples / 2, 0);
            if (channel3Enable) channel3.play(b, numSamples / 2, 0);
            if (channel4Enable) channel4.play(b, numSamples / 2, 0);
            soundLine.write(b, 0, numSamples);
        }
    }

    /**
     * Initialize sound hardware if available
     */
    SourceDataLine initSoundHardware() {

        try {
            AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate, 8, 2, 2, sampleRate, true);
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(lineInfo)) {
                System.out.println("Error: Can't find audio output system!");
                soundEnabled = false;
            } else {
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(lineInfo);

                int bufferLength = (sampleRate / 1000) * bufferLengthMsec;
                line.open(format, bufferLength);
                line.start();
//    System.out.println("Initialized audio successfully.");
                soundEnabled = true;
                return line;
            }
        } catch (Exception e) {
            System.out.println("Error: Audio system busy!");
            soundEnabled = false;
        }

        return null;
    }

    /**
     * Change the sample rate of the playback
     */
    void setSampleRate(int sr) {
        sampleRate = sr;

        soundLine.flush();
        soundLine.close();

        soundLine = initSoundHardware();

        channel1.setSampleRate(sr);
        channel2.setSampleRate(sr);
        channel3.setSampleRate(sr);
        channel4.setSampleRate(sr);
    }

    /**
     * Change the sound buffer length
     */
    void setBufferLength(int time) {
        bufferLengthMsec = time;

        soundLine.flush();
        soundLine.close();

        soundLine = initSoundHardware();
    }


    /**
     * This class can mix a square wave signal with a sound buffer.
     * It supports all features of the Gameboys sound channels 1 and 2.
     */
    private class SquareWaveGenerator {
        /**
         * Sound is to be played on the left channel of a stereo sound
         */
        static final int CHAN_LEFT = 1;

        /**
         * Sound is to be played on the right channel of a stereo sound
         */
        static final int CHAN_RIGHT = 2;

        /**
         * Sound is to be played back in mono
         */
        static final int CHAN_MONO = 4;

        /**
         * Length of the sound (in frames)
         */
        int totalLength;

        /**
         * Current position in the waveform (in samples)
         */
        int cyclePos;

        /**
         * Length of the waveform (in samples)
         */
        int cycleLength;

        /**
         * Amplitude of the waveform
         */
        int amplitude;

        /**
         * Amount of time the sample stays high in a single waveform (in eighths)
         */
        int dutyCycle;

        /**
         * The channel that the sound is to be played back on
         */
        int channel;

        /**
         * Sample rate of the sound buffer
         */
        int sampleRate;

        /**
         * Initial amplitude
         */
        int initialEnvelope;

        /**
         * Number of envelope steps
         */
        int numStepsEnvelope;

        /**
         * If true, envelope will increase amplitude of sound, false indicates decrease
         */
        boolean increaseEnvelope;

        /**
         * Current position in the envelope
         */
        int counterEnvelope;

        /**
         * Frequency of the sound in internal GB format
         */
        int gbFrequency;

        /**
         * Amount of time between sweep steps.
         */
        int timeSweep;

        /**
         * Number of sweep steps
         */
        int numSweep;

        /**
         * If true, sweep will decrease the sound frequency, otherwise, it will increase
         */
        boolean decreaseSweep;

        /**
         * Current position in the sweep
         */
        int counterSweep;

        /**
         * Create a square wave generator with the supplied parameters
         */
        SquareWaveGenerator(int waveLength, int ampl, int duty, int chan, int rate) {
            cycleLength = waveLength;
            amplitude = ampl;
            cyclePos = 0;
            dutyCycle = duty;
            channel = chan;
            sampleRate = rate;
        }

        /**
         * Create a square wave generator at the specified sample rate
         */
        SquareWaveGenerator(int rate) {
            dutyCycle = 4;
            cyclePos = 0;
            channel = CHAN_LEFT | CHAN_RIGHT;
            cycleLength = 2;
            totalLength = 0;
            sampleRate = rate;
            amplitude = 32;
            counterSweep = 0;
        }

        /**
         * Set the sound buffer sample rate
         */
        void setSampleRate(int sr) {
            sampleRate = sr;
        }

        /**
         * Set the duty cycle
         */
        void setDutyCycle(int duty) {
            switch (duty) {
                case 0:
                    dutyCycle = 1;
                    break;
                case 1:
                    dutyCycle = 2;
                    break;
                case 2:
                    dutyCycle = 4;
                    break;
                case 3:
                    dutyCycle = 6;
                    break;
            }
//  System.out.println(dutyCycle);
        }

        /**
         * Set the sound frequency, in internal GB format
         */
        void setFrequency(int gbFrequency) {
            try {
                float frequency = 131072 / 2048;

                if (gbFrequency != 2048) {
                    frequency = ((float) 131072 / (float) (2048 - gbFrequency));
                }
//  System.out.println("gbFrequency: " + gbFrequency + "");
                this.gbFrequency = gbFrequency;
                if (frequency != 0) {
                    cycleLength = (256 * sampleRate) / (int) frequency;
                } else {
                    cycleLength = 65535;
                }
                if (cycleLength == 0) cycleLength = 1;
//  System.out.println("Cycle length : " + cycleLength + " samples");
            } catch (ArithmeticException e) {
                // Skip ip
            }
        }

        /**
         * Set the channel for playback
         */
        void setChannel(int chan) {
            channel = chan;
        }

        /**
         * Set the envelope parameters
         */
        void setEnvelope(int initialValue, int numSteps, boolean increase) {
            initialEnvelope = initialValue;
            numStepsEnvelope = numSteps;
            increaseEnvelope = increase;
            amplitude = initialValue * 2;
        }

        /**
         * Set the frequency sweep parameters
         */
        void setSweep(int time, int num, boolean decrease) {
            timeSweep = (time + 1) / 2;
            numSweep = num;
            decreaseSweep = decrease;
            counterSweep = 0;
//  System.out.println("Sweep: " + time + ", " + num + ", " + decrease);
        }

        void setLength(int gbLength) {
            if (gbLength == -1) {
                totalLength = -1;
            } else {
                totalLength = (64 - gbLength) / 4;
            }
        }

        void setLength3(int gbLength) {
            if (gbLength == -1) {
                totalLength = -1;
            } else {
                totalLength = (256 - gbLength) / 4;
            }
        }

        void setVolume3(int volume) {
            switch (volume) {
                case 0:
                    amplitude = 0;
                    break;
                case 1:
                    amplitude = 32;
                    break;
                case 2:
                    amplitude = 16;
                    break;
                case 3:
                    amplitude = 8;
                    break;
            }
//  System.out.println("A:"+volume);
        }

        /**
         * Output a frame of sound data into the buffer using the supplied frame length and array offset.
         */
        void play(byte[] b, int length, int offset) {
            int val = 0;

            if (totalLength != 0) {
                totalLength--;

                if (timeSweep != 0) {
                    counterSweep++;
                    if (counterSweep > timeSweep) {
                        if (decreaseSweep) {
                            setFrequency(gbFrequency - (gbFrequency >> numSweep));
                        } else {
                            setFrequency(gbFrequency + (gbFrequency >> numSweep));
                        }
                        counterSweep = 0;
                    }
                }

                counterEnvelope++;
                if (numStepsEnvelope != 0) {
                    if (((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0)) {
                        if (!increaseEnvelope) {
                            if (amplitude > 0) amplitude -= 2;
                        } else {
                            if (amplitude < 16) amplitude += 2;
                        }
                    }
                }
                for (int r = offset; r < offset + length; r++) {

                    if (cycleLength != 0) {
                        if (((8 * cyclePos) / cycleLength) >= dutyCycle) {
                            val = amplitude;
                        } else {
                            val = -amplitude;
                        }
                    }

/*    if (cyclePos >= (cycleLength / 2)) {
     val = amplitude;
    } else {
     val = -amplitude;
    }*/


                    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
                    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
                    if ((channel & CHAN_MONO) != 0) b[r] += val;

                    //   System.out.print(val + " ");

                    cyclePos = (cyclePos + 256) % cycleLength;
                }
            }
        }

    }


    private class VoluntaryWaveGenerator {
        static final int CHAN_LEFT = 1;
        static final int CHAN_RIGHT = 2;
        static final int CHAN_MONO = 4;

        int totalLength;
        int cyclePos;
        int cycleLength;
        int amplitude;
        int channel;
        int sampleRate;
        int volumeShift;

        byte[] waveform = new byte[32];

        VoluntaryWaveGenerator(int waveLength, int ampl, int duty, int chan, int rate) {
            cycleLength = waveLength;
            amplitude = ampl;
            cyclePos = 0;
            channel = chan;
            sampleRate = rate;
        }

        VoluntaryWaveGenerator(int rate) {
            cyclePos = 0;
            channel = CHAN_LEFT | CHAN_RIGHT;
            cycleLength = 2;
            totalLength = 0;
            sampleRate = rate;
            amplitude = 32;
        }

        void setSampleRate(int sr) {
            sampleRate = sr;
        }

        void setFrequency(int gbFrequency) {
//  cyclePos = 0;
            float frequency = (int) ((float) 65536 / (float) (2048 - gbFrequency));
//  System.out.println("gbFrequency: " + gbFrequency + "");
            cycleLength = (int) ((float) (256f * sampleRate) / (float) frequency);
            if (cycleLength == 0) cycleLength = 1;
//  System.out.println("Cycle length : " + cycleLength + " samples");
        }

        void setChannel(int chan) {
            channel = chan;
        }

        void setLength(int gbLength) {
            if (gbLength == -1) {
                totalLength = -1;
            } else {
                totalLength = (256 - gbLength) / 4;
            }
        }

        void setSamplePair(int address, int value) {
            waveform[address * 2] = (byte) ((value & 0xF0) >> 4);
            waveform[address * 2 + 1] = (byte) ((value & 0x0F));
        }

        void setVolume(int volume) {
            switch (volume) {
                case 0:
                    volumeShift = 5;
                    break;
                case 1:
                    volumeShift = 0;
                    break;
                case 2:
                    volumeShift = 1;
                    break;
                case 3:
                    volumeShift = 2;
                    break;
            }
//  System.out.println("A:"+volume);
        }

        void play(byte[] b, int length, int offset) {
            int val;

            if (totalLength != 0) {
                totalLength--;

                for (int r = offset; r < offset + length; r++) {

                    int samplePos = (31 * cyclePos) / cycleLength;
                    val = Common.unsign(waveform[samplePos % 32]) >> volumeShift << 1;
//    System.out.print(" " + val);

                    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
                    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
                    if ((channel & CHAN_MONO) != 0) b[r] += val;

                    //   System.out.print(val + " ");
                    cyclePos = (cyclePos + 256) % cycleLength;
                }
            }
        }

    }


    /**
     * This is a white noise generator.  It is used to emulate
     * channel 4.
     */

    private class NoiseGenerator {
        /**
         * Indicates sound is to be played on the left channel of a stereo sound
         */
        static final int CHAN_LEFT = 1;

        /**
         * Indictaes sound is to be played on the right channel of a stereo sound
         */
        static final int CHAN_RIGHT = 2;

        /**
         * Indicates that sound is mono
         */
        static final int CHAN_MONO = 4;

        /**
         * Indicates the length of the sound in frames
         */
        int totalLength;
        int cyclePos;

        /**
         * The length of one cycle, in samples
         */
        int cycleLength;

        /**
         * Amplitude of the wave function
         */
        int amplitude;

        /**
         * Channel being played on.  Combination of CHAN_LEFT and CHAN_RIGHT, or CHAN_MONO
         */
        int channel;

        /**
         * Sampling rate of the output channel
         */
        int sampleRate;

        /**
         * Initial value of the envelope
         */
        int initialEnvelope;

        int numStepsEnvelope;

        /**
         * Whether the envelope is an increase/decrease in amplitude
         */
        boolean increaseEnvelope;

        int counterEnvelope;

        /**
         * Stores the random values emulating the polynomial generator (badly!)
         */
        boolean randomValues[];

        int dividingRatio;
        int polynomialSteps;
        int shiftClockFreq;
        int finalFreq;
        int cycleOffset;

        /**
         * Creates a white noise generator with the specified wavelength, amplitude, channel, and sample rate
         */
        NoiseGenerator(int waveLength, int ampl, int chan, int rate) {
            cycleLength = waveLength;
            amplitude = ampl;
            cyclePos = 0;
            channel = chan;
            sampleRate = rate;
            cycleOffset = 0;

            randomValues = new boolean[32767];

            Random rand = new Random();


            for (int r = 0; r < 32767; r++) {
                randomValues[r] = rand.nextBoolean();
            }

            cycleOffset = 0;
        }

        /**
         * Creates a white noise generator with the specified sample rate
         */
        NoiseGenerator(int rate) {
            cyclePos = 0;
            channel = CHAN_LEFT | CHAN_RIGHT;
            cycleLength = 2;
            totalLength = 0;
            sampleRate = rate;
            amplitude = 32;

            randomValues = new boolean[32767];

            Random rand = new Random();


            for (int r = 0; r < 32767; r++) {
                randomValues[r] = rand.nextBoolean();
            }

            cycleOffset = 0;
        }


        void setSampleRate(int sr) {
            sampleRate = sr;
        }

        /**
         * Set the channel that the white noise is playing on
         */
        void setChannel(int chan) {
            channel = chan;
        }

        /**
         * Setup the envelope, and restart it from the beginning
         */
        void setEnvelope(int initialValue, int numSteps, boolean increase) {
            initialEnvelope = initialValue;
            numStepsEnvelope = numSteps;
            increaseEnvelope = increase;
            amplitude = initialValue * 2;
        }

        /**
         * Set the length of the sound
         */
        void setLength(int gbLength) {
            if (gbLength == -1) {
                totalLength = -1;
            } else {
                totalLength = (64 - gbLength) / 4;
            }
        }

        void setParameters(float dividingRatio, boolean polynomialSteps, int shiftClockFreq) {
            this.dividingRatio = (int) dividingRatio;
            if (!polynomialSteps) {
                this.polynomialSteps = 32767;
                cycleLength = 32767 << 8;
                cycleOffset = 0;
            } else {
                this.polynomialSteps = 63;
                cycleLength = 63 << 8;

                Random rand = new Random();

                cycleOffset = (int) (rand.nextFloat() * 1000);
            }
            this.shiftClockFreq = shiftClockFreq;

            if (dividingRatio == 0) dividingRatio = 0.5f;

            finalFreq = ((int) (4194304 / 8 / dividingRatio)) >> (shiftClockFreq + 1);
//  System.out.println("dr:" + dividingRatio + "  steps: " + this.polynomialSteps + "  shift:" + shiftClockFreq + "  = Freq:" + finalFreq);
        }

        /**
         * Output a single frame of samples, of specified length.  Start at position indicated in the
         * output array.
         */
        void play(byte[] b, int length, int offset) {
            int val;

            if (totalLength != 0) {
                totalLength--;

                counterEnvelope++;
                if (numStepsEnvelope != 0) {
                    if (((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0)) {
                        if (!increaseEnvelope) {
                            if (amplitude > 0) amplitude -= 2;
                        } else {
                            if (amplitude < 16) amplitude += 2;
                        }
                    }
                }


                int step = ((finalFreq) / (sampleRate >> 8));
                // System.out.println("Step=" + step);

                for (int r = offset; r < offset + length; r++) {
                    boolean value = randomValues[((cycleOffset) + (cyclePos >> 8)) & 0x7FFF];
                    int v = value ? (amplitude / 2) : (-amplitude / 2);

                    if ((channel & CHAN_LEFT) != 0) b[r * 2] += v;
                    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += v;
                    if ((channel & CHAN_MONO) != 0) b[r] += v;

                    cyclePos = (cyclePos + step) % cycleLength;
                }

            }
        }

    }

    // SPEED
    public void setSpeed(int i) {
        speed.setSpeed(i);
    }

}



