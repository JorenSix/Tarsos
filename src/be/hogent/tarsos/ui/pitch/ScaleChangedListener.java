/**
*
*  Tarsos is developed by Joren Six at 
*  The Royal Academy of Fine Arts & Royal Conservatory,
*  University College Ghent,
*  Hoogpoort 64, 9000 Ghent - Belgium
*
**/
package be.hogent.tarsos.ui.pitch;

public interface ScaleChangedListener {
	void scaleChanged(double[] newScale, boolean isChanging, boolean shiftHisto);
}
