package be.hogent.tarsos.midi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

/**
 * MidiUtils provides a lot of MIDI messages. Also it can be used to build and
 * send tuning messages to a receiver. Uses a lot of unmodified code from the
 * gervill software package, licensed under the GPL with the classpath
 * exception. <a
 * href="https://gervill.dev.java.net/source/browse/gervill/src.demos/"> Gervill
 * source code</a>
 * 
 * @author Karl Helgason
 */
public class MidiUtils {

    private MidiUtils() {
    }

    /**
     * Enables a tuning preset.
     * 
     * @param recv
     *            the receiver to send the message to
     * @param channel
     *            the channel to send the message on
     * @param tuningpreset
     *            the index of the tuning preset
     * @throws InvalidMidiDataException
     *             if something goes awry.
     */
    public static void sendTuningChange(Receiver recv, int channel, int tuningpreset)
            throws InvalidMidiDataException {
        // Data Entry
        ShortMessage sm1 = new ShortMessage();
        sm1.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x64, 03);
        ShortMessage sm2 = new ShortMessage();
        sm2.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x65, 00);
        // Tuning program 19
        ShortMessage sm3 = new ShortMessage();
        sm3.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x06, tuningpreset);

        // Data Increment
        ShortMessage sm4 = new ShortMessage();
        sm4.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x60, 0x7F);
        // Data Decrement
        ShortMessage sm5 = new ShortMessage();
        sm5.setMessage(ShortMessage.CONTROL_CHANGE, channel, 0x61, 0x7F);

        recv.send(sm1, -1);
        recv.send(sm2, -1);
        recv.send(sm3, -1);
        recv.send(sm4, -1);
        recv.send(sm5, -1);
    }

    /**
     * Sends a {@link MidiUtils.MidiTuningStandard.keyBasedTuningDump} MIDI
     * message to a receiver using the specified tuning in cents.
     * 
     * @param recv
     * @param bank
     * @param preset
     * @param name
     * @param tunings
     * @throws IOException
     * @throws InvalidMidiDataException
     */
    public static void sendTunings(Receiver recv, int bank, int preset, String name, double[] tunings)
            throws IOException, InvalidMidiDataException {
        assert tunings.length == 128;
        int[] itunings = new int[128];
        for (int i = 0; i < itunings.length; i++) {
            itunings[i] = (int) (tunings[i] * 16384.0 / 100.0);
        }

        SysexMessage msg = MidiUtils.MidiTuningStandard.keyBasedTuningDump(MidiUtils.ALL_DEVICES, bank,
                preset, name, itunings);
        recv.send(msg, -1);
    }

    // ------------- MIDI MESSAGES ------------------

    public static final int ALL_DEVICES = 0x7F;

    private static final byte[] UNIVERSAL_NON_REALTIME_SYSEX_HEADER = new byte[] { (byte) 0xF0, (byte) 0x7E };

    private static final byte[] UNIVERSAL_REALTIME_SYSEX_HEADER = new byte[] { (byte) 0xF0, (byte) 0x7F };

    private static final byte[] EOX = new byte[] { (byte) 0xF7 };

    public static class GeneralMidiMessages {
        private GeneralMidiMessages() {
        }

        private static final byte[] GENERAL_MIDI_MESSAGES = new byte[] { (byte) 0x09 };

        private static final byte GENERAL_MIDI_1_ON = 0x01;

        private static final byte GENERAL_MIDI_OFF = 0x02;

        private static final byte GENERAL_MIDI_2_ON = 0x03;

        private static SysexMessage setGeneralMidiMessage(int targetDevice, byte type) throws IOException,
                InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(GENERAL_MIDI_MESSAGES);
            baos.write(type);
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;

        }

        public static SysexMessage gmSystemOff(int targetDevice) throws IOException, InvalidMidiDataException {
            return setGeneralMidiMessage(targetDevice, GENERAL_MIDI_OFF);
        }

        public static SysexMessage gmSystemOn(int targetDevice) throws IOException, InvalidMidiDataException {
            return setGeneralMidiMessage(targetDevice, GENERAL_MIDI_1_ON);
        }

        public static SysexMessage gm1SystemOn(int targetDevice) throws IOException, InvalidMidiDataException {
            return setGeneralMidiMessage(targetDevice, GENERAL_MIDI_1_ON);
        }

        public static SysexMessage gm2SystemOn(int targetDevice) throws IOException, InvalidMidiDataException {
            return setGeneralMidiMessage(targetDevice, GENERAL_MIDI_2_ON);
        }
    }

    public static class DeviceControl {

        private static final byte[] DEVICE_CONTROL = new byte[] { (byte) 0x04 };

        private static final byte MASTER_VOLUME = (byte) 0x01;

        private static final byte MASTER_BALANCE = (byte) 0x02;

        private static final byte MASTER_FINE_TUNING = (byte) 0x03;

        private static final byte MASTER_COARSE_TUNING = (byte) 0x04;

        private static final byte[] GLOBAL_PARAMETER_CONTROL = new byte[] { (byte) 0x05 };

        private static SysexMessage setDeviceControl(int targetDevice, int control, int value)
                throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(DEVICE_CONTROL);
            baos.write((byte) control);
            baos.write((byte) (value % 128));
            baos.write((byte) (value / 128));
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;

        }

        public static SysexMessage setMasterVolume(int targetDevice, int value) throws IOException,
                InvalidMidiDataException {
            return setDeviceControl(targetDevice, MASTER_VOLUME, value);
        }

        public static SysexMessage setMasterBalance(int targetDevice, int value) throws IOException,
                InvalidMidiDataException {
            return setDeviceControl(targetDevice, MASTER_BALANCE, value);
        }

        public static SysexMessage setMasterFineTuning(int targetDevice, int value) throws IOException,
                InvalidMidiDataException {
            return setDeviceControl(targetDevice, MASTER_FINE_TUNING, value);
        }

        public static SysexMessage setMasterCoarseTuning(int targetDevice, int value) throws IOException,
                InvalidMidiDataException {
            return setDeviceControl(targetDevice, MASTER_COARSE_TUNING, value);
        }

        public static SysexMessage setGlobalParameter(int targetDevice, short[] slotpath, byte[] parameter,
                int value) throws IOException, InvalidMidiDataException {
            return setGlobalParameter(targetDevice, slotpath, parameter, new byte[] { (byte) value });
        }

        public static SysexMessage setGlobalParameter(int targetDevice, short[] slotpath, byte[] parameter,
                byte[] value) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(DEVICE_CONTROL);
            baos.write(GLOBAL_PARAMETER_CONTROL);
            baos.write((byte) slotpath.length);
            baos.write((byte) parameter.length);
            baos.write((byte) value.length);
            for (int i = 0; i < slotpath.length; i++) {
                short x = slotpath[0];
                baos.write((byte) (x >>> 8));
                baos.write((byte) x);
            }
            baos.write(parameter);
            baos.write(value);
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static class ReverbEffect {

            public static final int REVERB_TYPE_SMALL_ROOM = 0;

            public static final int REVERB_TYPE_MEDIUM_ROOM = 1;

            public static final int REVERB_TYPE_LARGE_ROOM = 2;

            public static final int REVERB_TYPE_MEDIUM_HALL = 3;

            public static final int REVERB_TYPE_LARGE_HALL = 4;

            public static final int REVERB_TYPE_PLATE = 8;

            private static final short[] SLOTPATH_EFFECT_REVERB = new short[] { (short) 0x0101 };

            private static final byte[] REVERB_TYPE = new byte[] { (byte) 0x00 };

            private static final byte[] REVERB_TIME = new byte[] { (byte) 0x01 };

            public static SysexMessage setReverbType(int targetDevice, int reverbType) throws IOException,
                    InvalidMidiDataException {
                return setGlobalParameter(targetDevice, SLOTPATH_EFFECT_REVERB, REVERB_TYPE, reverbType);
            }

            public static SysexMessage setReverbTime(int targetDevice, int reverbTime) throws IOException,
                    InvalidMidiDataException {
                return setGlobalParameter(targetDevice, SLOTPATH_EFFECT_REVERB, REVERB_TIME, reverbTime);
            }
        }

        public static class ChorusEffect {

            public static final int CHORUS_TYPE_CHORUS1 = 0;

            public static final int CHORUS_TYPE_CHORUS2 = 1;

            public static final int CHORUS_TYPE_CHORUS3 = 2;

            public static final int CHORUS_TYPE_CHORUS4 = 3;

            public static final int CHORUS_TYPE_FB_CHORUS = 4;

            public static final int CHORUS_TYPE_FLANGER = 5;

            private static final short[] SLOTPATH_EFFECT_CHORUS = new short[] { (short) 0x0102 };

            private static final byte[] CHORUS_TYPE = new byte[] { (byte) 0x00 };

            private static final byte[] CHORUS_MOD_RATE = new byte[] { (byte) 0x01 };

            private static final byte[] CHORUS_MOD_DEPTH = new byte[] { (byte) 0x02 };

            private static final byte[] CHORUS_FEEDBACK = new byte[] { (byte) 0x03 };

            private static final byte[] CHORUS_SEND_TO_REVERB = new byte[] { (byte) 0x04 };

            public static SysexMessage setChorusType(int targetDevice, int reverbType) throws IOException,
                    InvalidMidiDataException {
                return setGlobalParameter(targetDevice, SLOTPATH_EFFECT_CHORUS, CHORUS_TYPE, reverbType);
            }

            public static SysexMessage setChorusModRate(int targetDevice, int reverbType) throws IOException,
                    InvalidMidiDataException {
                return setGlobalParameter(targetDevice, SLOTPATH_EFFECT_CHORUS, CHORUS_MOD_RATE, reverbType);
            }

            public static SysexMessage setChorusModDepth(int targetDevice, int reverbType)
                    throws IOException, InvalidMidiDataException {
                return setGlobalParameter(targetDevice, SLOTPATH_EFFECT_CHORUS, CHORUS_MOD_DEPTH, reverbType);
            }

            public static SysexMessage setChorusFeedback(int targetDevice, int reverbType)
                    throws IOException, InvalidMidiDataException {
                return setGlobalParameter(targetDevice, SLOTPATH_EFFECT_CHORUS, CHORUS_FEEDBACK, reverbType);
            }

            public static SysexMessage setChorusSendToReverb(int targetDevice, int reverbType)
                    throws IOException, InvalidMidiDataException {
                return setGlobalParameter(targetDevice, SLOTPATH_EFFECT_CHORUS, CHORUS_SEND_TO_REVERB,
                        reverbType);
            }
        }
    }

    public static class KeyBasedInstrumentControl {

        public static final int KEY_BASED_CONTORL_FINE_TUNING = 0x78;

        public static final int KEY_BASED_CONTORL_COARSE_TUNING = 0x79;

        private static final byte[] KEY_BASED_INSTRUMENT_CONTROL = new byte[] { (byte) 0x0A };

        private static final byte[] BASIC_MESSAGE = new byte[] { (byte) 0x01 };

        public static SysexMessage setKeyBasedControl(int targetDevice, int midi_channel, int key_number,
                int control, int value) throws IOException, InvalidMidiDataException {
            return setKeyBasedControl(targetDevice, midi_channel, key_number, new int[] { control },
                    new int[] { value });
        }

        public static SysexMessage setKeyBasedControl(int targetDevice, int midi_channel, int key_number,
                int[] controls, int[] values) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(KEY_BASED_INSTRUMENT_CONTROL);
            baos.write(BASIC_MESSAGE);
            baos.write((byte) midi_channel);
            baos.write((byte) key_number);
            for (int i = 0; i < controls.length; i++) {
                baos.write((byte) controls[i]);
                baos.write((byte) values[i]);
            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }
    }

    public static class DestinationSettings {

        private static final byte[] CONTROLLER_DESTINATION_SETTINGS = new byte[] { (byte) 0x09 };

        private static final byte[] CONTROLLER_CHANNEL_PRESSURE = new byte[] { (byte) 0x01 };

        private static final byte[] CONTROLLER_POLY_PRESSURE = new byte[] { (byte) 0x02 };

        private static final byte[] CONTROLLER_CONTROL_CHANGE = new byte[] { (byte) 0x03 };

        public static SysexMessage setControllerDestinationForChannelPressure(int targetDevice, int channel,
                int[] controls, int[] ranges) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(CONTROLLER_DESTINATION_SETTINGS);
            baos.write(CONTROLLER_CHANNEL_PRESSURE);
            baos.write((byte) channel);
            for (int i = 0; i < controls.length; i++) {
                baos.write((byte) controls[i]);
                baos.write((byte) ranges[i]);
            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage setControllerDestinationForPolyPressure(int targetDevice, int channel,
                int[] controls, int[] ranges) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(CONTROLLER_DESTINATION_SETTINGS);
            baos.write(CONTROLLER_POLY_PRESSURE);
            baos.write((byte) channel);
            for (int i = 0; i < controls.length; i++) {
                baos.write((byte) controls[i]);
                baos.write((byte) ranges[i]);
            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage setControllerDestinationForControlChange(int targetDevice, int channel,
                byte control, int[] controls, int[] ranges) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(CONTROLLER_DESTINATION_SETTINGS);
            baos.write(CONTROLLER_CONTROL_CHANGE);
            baos.write((byte) channel);
            baos.write(control);
            for (int i = 0; i < controls.length; i++) {
                baos.write((byte) controls[i]);
                baos.write((byte) ranges[i]);
            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }
    }

    /**
     * See: <a href="http://www.midi.org/techspecs/midituning.php">the MIDI
     * Tuning Messages specification</a>.
     */
    public static class MidiTuningStandard {

        public static final int TUNING_A440 = 45 * 128 * 128;

        public static final int TUNING_NO_CHANGE = 2097151;

        private static final byte[] MIDI_TUNING_STANDARD = new byte[] { (byte) 0x08 };

        private static final byte[] BULK_TUNING_DUMP = new byte[] { (byte) 0x01 };

        private static final byte[] SINGLE_NOTE_TUNING_CHANGE = new byte[] { (byte) 0x02 };

        private static final byte[] KEY_BASED_TUNING_DUMP = new byte[] { (byte) 0x04 };

        private static final byte[] SCALE_OCTAVE_TUNING_DUMP_1BYTE_FORM = new byte[] { (byte) 0x05 };

        private static final byte[] SCALE_OCTAVE_TUNING_DUMP_2BYTE_FORM = new byte[] { (byte) 0x06 };

        private static final byte[] SINGLE_NOTE_TUNING_CHANGE_BANK = new byte[] { (byte) 0x07 };

        private static final byte[] SCALE_OCTAVE_TUNING_1BYTE_FORM = new byte[] { (byte) 0x08 };

        private static final byte[] SCALE_OCTAVE_TUNING_2BYTE_FORM = new byte[] { (byte) 0x09 };

        public static SysexMessage scaleOctaveTuning1ByteForm(int targetDevice, boolean realtime,
                boolean[] channels, int[] tuning) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (realtime) {
                baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            } else {
                baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            }
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(SCALE_OCTAVE_TUNING_1BYTE_FORM);
            int channelmask = 0;
            for (int i = 0; i < 2; i++) {
                if (channels[i + 14]) {
                    channelmask += 1 << i;
                }
            }
            baos.write((byte) channelmask);
            channelmask = 0;
            for (int i = 0; i < 7; i++) {
                if (channels[i + 7]) {
                    channelmask += 1 << i;
                }
            }
            baos.write((byte) channelmask);
            channelmask = 0;
            for (int i = 0; i < 7; i++) {
                if (channels[i]) {
                    channelmask += 1 << i;
                }
            }
            baos.write((byte) channelmask);
            for (int i = 0; i < 12; i++) {
                baos.write((byte) (tuning[i] + 64));
            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage scaleOctaveTuning2ByteForm(int targetDevice, boolean realtime,
                boolean[] channels, int[] tuning) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (realtime) {
                baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            } else {
                baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            }
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(SCALE_OCTAVE_TUNING_2BYTE_FORM);
            int channelmask = 0;
            for (int i = 0; i < 2; i++) {
                if (channels[i + 14]) {
                    channelmask += 1 << i;
                }
            }
            baos.write((byte) channelmask);
            channelmask = 0;
            for (int i = 0; i < 7; i++) {
                if (channels[i + 7]) {
                    channelmask += 1 << i;
                }
            }
            baos.write((byte) channelmask);
            channelmask = 0;
            for (int i = 0; i < 7; i++) {
                if (channels[i]) {
                    channelmask += 1 << i;
                }
            }
            baos.write((byte) channelmask);
            for (int i = 0; i < 12; i++) {
                int t = tuning[i] + 8192;
                baos.write((byte) (t / 128));
                baos.write((byte) (t % 128));
            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }

        private static void setTuningChecksum(byte[] data) {
            int x = data[1] & 0xFF;
            for (int i = 2; i < data.length - 2; i++) {
                x = x ^ (data[i] & 0xFF);
            }
            data[data.length - 2] = (byte) (x & 127);
        }

        public static SysexMessage scaleOctaveTuningDump1ByteForm(int targetDevice, int bank, int preset,
                String name, int[] tuning) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(SCALE_OCTAVE_TUNING_DUMP_1BYTE_FORM);
            baos.write((byte) bank);
            baos.write((byte) preset);
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            byte[] namebytes = name.getBytes("ASCII");
            baos.write(namebytes);
            byte space_char = " ".getBytes()[0];
            for (int i = namebytes.length; i < 16; i++) {
                baos.write(space_char);
            }
            for (int i = 0; i < 12; i++) {
                baos.write((byte) (tuning[i] + 64));
            }
            baos.write(0);
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            setTuningChecksum(data);
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage scaleOctaveTuningDump2ByteForm(int targetDevice, int bank, int preset,
                String name, int[] tuning) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(SCALE_OCTAVE_TUNING_DUMP_2BYTE_FORM);
            baos.write((byte) bank);
            baos.write((byte) preset);
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            byte[] namebytes = name.getBytes("ASCII");
            baos.write(namebytes);
            byte space_char = " ".getBytes()[0];
            for (int i = namebytes.length; i < 16; i++) {
                baos.write(space_char);
            }
            for (int i = 0; i < 12; i++) {
                int t = tuning[i] + 8192;
                baos.write((byte) (t / 128));
                baos.write((byte) (t % 128));
            }
            baos.write(0);
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            setTuningChecksum(data);
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage singleNoteTuningChange(int targetDevice, boolean realtime, int bank,
                int preset, int[] key_numbers, int[] key_tunings) throws IOException,
                InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (realtime) {
                baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            } else {
                baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            }
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(SINGLE_NOTE_TUNING_CHANGE_BANK);
            baos.write((byte) bank);
            baos.write((byte) preset);
            baos.write((byte) key_numbers.length);
            for (int i = 0; i < key_numbers.length; i++) {

                baos.write((byte) key_numbers[i]);
                int t = key_tunings[i];
                baos.write((byte) ((t / 16384) % 128));
                baos.write((byte) ((t / 128) % 128));
                baos.write((byte) (t % 128));

            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage singleNoteTuningChange(int targetDevice, int preset, int[] key_numbers,
                int[] key_tunings) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(SINGLE_NOTE_TUNING_CHANGE);
            baos.write((byte) preset);
            baos.write((byte) key_numbers.length);
            for (int i = 0; i < key_numbers.length; i++) {

                baos.write((byte) key_numbers[i]);
                int t = key_tunings[i];
                baos.write((byte) ((t / 16384) % 128));
                baos.write((byte) ((t / 128) % 128));
                baos.write((byte) (t % 128));

            }
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage keyBasedTuningDump(int targetDevice, int preset, String name, int[] tunings)
                throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(BULK_TUNING_DUMP);
            baos.write((byte) preset);
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            byte[] namebytes = name.getBytes("ASCII");
            baos.write(namebytes);
            byte space_char = " ".getBytes()[0];
            for (int i = namebytes.length; i < 16; i++) {
                baos.write(space_char);
            }
            for (int i = 0; i < 128; i++) {
                int t = tunings[i];
                baos.write((byte) ((t / 16384) % 128));
                baos.write((byte) ((t / 128) % 128));
                baos.write((byte) (t % 128));
            }
            baos.write(0);
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            setTuningChecksum(data);
            sysex.setMessage(data, data.length);
            return sysex;
        }

        public static SysexMessage keyBasedTuningDump(int targetDevice, int bank, int preset, String name,
                int[] tunings) throws IOException, InvalidMidiDataException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UNIVERSAL_NON_REALTIME_SYSEX_HEADER);
            baos.write((byte) targetDevice);
            baos.write(MIDI_TUNING_STANDARD);
            baos.write(KEY_BASED_TUNING_DUMP);
            baos.write((byte) bank);
            baos.write((byte) preset);
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            byte[] namebytes = name.getBytes("ASCII");
            baos.write(namebytes);
            byte space_char = " ".getBytes()[0];
            for (int i = namebytes.length; i < 16; i++) {
                baos.write(space_char);
            }
            for (int i = 0; i < 128; i++) {
                int t = tunings[i];
                baos.write((byte) ((t / 16384) % 128));
                baos.write((byte) ((t / 128) % 128));
                baos.write((byte) (t % 128));
            }
            baos.write(0);
            baos.write(EOX);
            SysexMessage sysex = new SysexMessage();
            byte[] data = baos.toByteArray();
            setTuningChecksum(data);
            sysex.setMessage(data, data.length);
            return sysex;
        }

    }

}
