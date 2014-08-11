package be.tarsos.cli;

public class TimbreAndScaleRelation extends AbstractTarsosApp {
	
	
	public static void main(String[] args){
		String audioFile = args[0];
		String scalaFile = args[1];
	}

	@Override
	public void run(String... args) {
		
	}

	@Override
	public String description() {
		
		return "Returns how well a tone scale matches with the timbre of a piece of audio.";
	}
}
