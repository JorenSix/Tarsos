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
* Tarsos is developed by Joren Six at IPEM, University Ghent
*  
* -----------------------------------------------------------
*
*  Info: http://tarsos.0110.be
*  Github: https://github.com/JorenSix/Tarsos
*  Releases: http://0110.be/releases/Tarsos/
*  
*  Tarsos includes some source code by various authors,
*  for credits, license and info: see README.
* 
*/



package be.tarsos.util.histogram;

/**
 * Defines an implementation of a histogram correlation or distance measure.
 * 
 * @author Joren Six
 */
public interface HistogramCorrelation {
    /**
     * The implementation of a histogram correlation or distance measure.
     * 
     * @param thisHistogram
     *            the first histogram
     * @param displacement
     *            the value to displace the otherHistogram (e.g. for optimal
     *            correlation, minimum distance between the two)
     * @param otherHistogram
     *            the second (not displaced) histogram
     * @return the correlation between a histogram and a displaced histogram
     */
    double correlation(Histogram thisHistogram, int displacement, Histogram otherHistogram);


}
