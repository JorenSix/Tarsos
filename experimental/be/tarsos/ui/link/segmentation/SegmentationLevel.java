package be.tarsos.ui.link.segmentation;

public enum SegmentationLevel {
	
	BEAT ("Beat level"),
	MICRO ("Micro Level"),
	MESO ("Meso Level"),
	MACRO ("Macro Level"),
	CUSTOM ("Custom Segmentation");
	
	private String name;
	
	private SegmentationLevel(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public static SegmentationLevel getLevelByName(String name){
		if (name.equals(BEAT.getName())){
			return BEAT;
		}else if (name.equals(MICRO.getName())){
			return MICRO;
		} else if (name.equals(MESO.getName())){
			return MESO;
		} else if (name.equals(MACRO.getName())){
			return MACRO;
		} else if(name.equals(CUSTOM.getName())){
			return CUSTOM;
		} else {
			return null;
		}
	}
}
