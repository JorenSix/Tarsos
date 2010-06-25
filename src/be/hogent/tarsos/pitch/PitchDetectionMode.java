package be.hogent.tarsos.pitch;

import be.hogent.tarsos.pitch.pure.TarsosPitchDetection;
import be.hogent.tarsos.util.AudioFile;

/**
 * The pitch detection mode defines which algorithm is used to detect pitch.
 * @author Joren Six
 */
public enum PitchDetectionMode {
    /**
     * The AUBIO_YIN algorithm.
     */
    AUBIO_YIN("yin"),
    /**
     * A faster version of AUBIO_YIN: spectral AUBIO_YIN. It should yield very similar
     * results as AUBIO_YIN, only faster.
     */
    AUBIO_YINFFT("yinfft"),
    /**
     * Fast spectral comb.
     */
    AUBIO_FCOMB("fcomb"),
    /**
     * Multi comb with spectral smoothing.
     */
    AUBIO_MCOMB("mcomb"),
    /**
     * Schmitt trigger.
     */
    AUBIO_SCHMITT("schmitt"),

    /**
     * The IPEM pitch tracker outputs six weighted pitch candidates.
     */
    IPEM_SIX("ipem_six"),

    /**
     * The IPEM pitch tracker outputs only one pitch candidates.
     */
    IPEM_ONE("ipem_one"),

    /**
     * The pure java YIN implementation of Tarsos.
     */
    TARSOS_YIN("tarsos_yin"),

    /**
     * The pure java MPM (Tartini pitch tracker) implementation of Tarsos.
     */
    TARSOS_MPM("tarsos_yin"),

    /**
     * The pure java Meta pitch tracker, uses MPM and YIN in the background.
     */
    TARSOS_META("tarsos_meta");


    /**
     * The name of the parameter.
     */
    private final String detectionModeName;

    /**
     * Initialize a pitch detection mode with a name.
     * @param name
     *            The name (e.g. command line parameter) for the mode.
     */
    private PitchDetectionMode(final String name) {
        this.detectionModeName = name;
    }

    /**
     * @return The name used in the aubio command.
     */
    public String getParametername() {
        return this.getDetectionModeName();
    }

    /**
     * Returns a pitch detector for an audio file.
     * @param audioFile
     *            the audioFile to detect pitch for.
     * @return A pitch detector for the audio file.
     */
    public PitchDetector getPitchDetector(final AudioFile audioFile) {
        PitchDetector detector;
        switch (this) {
        case IPEM_SIX:
            detector = new IPEMPitchDetection(audioFile, this);
            break;
        case IPEM_ONE:
            detector = new IPEMPitchDetection(audioFile, this);
            break;
        case TARSOS_YIN:
            detector = new TarsosPitchDetection(audioFile, this);
            break;
        case TARSOS_MPM:
            detector = new TarsosPitchDetection(audioFile, this);
            break;
        default:
            detector = new AubioPitchDetection(audioFile, this);
            break;
        }
        return detector;
    }

    public String getDetectionModeName() {
        return detectionModeName;
    }
}
