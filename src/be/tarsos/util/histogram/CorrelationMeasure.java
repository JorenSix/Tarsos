/*
*              _______                      
*             |__   __|                     
*                | | __ _ _ __ ___  ___  ___
*                | |/ _` | '__/ __|/ _ \/ __| 
*                | | (_| | |  \__ \ (_) \__ \    
*                |_|\__,_|_|  |___/\___/|___/    
*                                                         
* -----------------------------------------------------------
*
*  Tarsos is developed by Joren Six at 
*  The School of Arts,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://tarsos.0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits and info, see README.
* 
*/

package be.tarsos.util.histogram;

/**
 * Defines a correlation measure for histograms. See On measuring the distance
 * between histograms: Sung-Hyuk Chaa, Sargur N. Sriharib (2002)
 * CorrelationMeasure is a bit of a strange name: also distance measures are
 * defined. Side note: java enums are nice.
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

    /**
     * The underlying histogram correlation.
     */
    private final HistogramCorrelation histoCorrelation;

    /**
     * Create a new correlation measure.
     * @param histoCor
     *            The underlying histogram correlation.
     */
    private CorrelationMeasure(final HistogramCorrelation histoCor) {
        this.histoCorrelation = histoCor;
    }

    /**
     * @return the HistogramCorrelation instance used to caluculate the distance
     *         / correlation
     */
    public HistogramCorrelation getHistogramCorrelation() {
        return this.histoCorrelation;
    }
}

