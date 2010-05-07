/**
 *
 */
package be.hogent.tarsos.util.histogram;

/**
 * 
 * Defines a correlation measure for histograms. See On measuring the distance
 * between histograms: Sung-Hyuk Chaa, Sargur N. Sriharib (2002)
 * 
 * CorrelationMeasure is a bit of a strange name: also distance measures are
 * defined.
 * 
 * Side note: java enums are nice.
 * 
 * @author Joren Six
 */
public enum CorrelationMeasure {
    /**
     * Is a distance measure using city block distances.
     */
    CITY_BLOCK(new CityBlockDistance()),
    /**
     * A direct euclidean distance.
     */
    EUCLIDEAN(new EuclideanDistance()),
    /**
     * The intersection computes the matching areas under the curve. It is a
     * symmetric correlation measure: 1.0 is perfect correlation, 0.0 means no
     * correlation. Symmetric: f(a,b) = f(b,a)
     */
    INTERSECTION(new Intersection()),
    /**
     * Another distance measure, with an impressive sounding name... TODO Write
     * better description
     */
    BHATTACHARYA(new BhattacharyaDistance()),
    /**
     * Another correlation measure, seems to work also, TODO Write better
     * description.
     */
    CROSSCORRELATION(new CrossCorrelation());

    private final HistogramCorrelation histogramCorrelation;

    private CorrelationMeasure(HistogramCorrelation histogramCorrelation) {
        this.histogramCorrelation = histogramCorrelation;
    }

    /**
     * @return the HistogramCorrelation instance used to caluculate the distance
     *         / correlation
     */
    public HistogramCorrelation getHistogramCorrelation() {
        return this.histogramCorrelation;
    }
}