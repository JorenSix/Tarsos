/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.ui.link.io;

import be.tarsos.tarsossegmenter.model.AASModel;
import be.tarsos.tarsossegmenter.model.segmentation.Segment;
import be.tarsos.ui.link.LinkedFrame;
import be.tarsos.ui.link.LinkedPanel;
import be.tarsos.ui.link.coordinatessystems.Quantity;
import be.tarsos.ui.link.layers.Layer;
import be.tarsos.ui.link.layers.LayerBuilder;
import be.tarsos.ui.link.layers.LayerProperty;
import be.tarsos.ui.link.layers.LayerType;
import be.tarsos.ui.link.layers.segmentationlayers.SegmentationLayer;
import be.tarsos.ui.link.segmentation.Segmentation;
import be.tarsos.ui.link.segmentation.SegmentationLevel;
import be.tarsos.ui.link.segmentation.SegmentationList;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 
 * @author Thomas Stubbe
 */
public class SegmentationFileParser {

	static public void parseFile(String file) {
		Segmentation.getInstance().deleteAll();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String strLine = in.readLine();
			while (strLine != null && strLine.trim().contains("item []:")) {
				strLine = in.readLine();
			}
			SegmentationList l = null;
			while ((strLine = in.readLine()) != null) { // lijn per lijn inlezen
				strLine = strLine.trim().toLowerCase();
				if (strLine.contains("name = ")) {
					String label = strLine.split("\"")[1];

					SegmentationLevel level = SegmentationLevel
							.getLevelByName(label);
					if (level == null) {
						level = SegmentationLevel.CUSTOM;
					}
					if (label == null) {
						l = Segmentation.getInstance()
								.constructNewSegmentationList(level);
					} else {
						l = Segmentation.getInstance()
								.constructNewSegmentationList(
										SegmentationLevel.CUSTOM, label);
					}
					LinkedPanel p = LinkedFrame.getInstance().addPanel(
							Quantity.TIME, Quantity.NONE, Color.WHITE);
					ArrayList<LayerProperty> properties = new ArrayList<LayerProperty>();
					properties.add(new LayerProperty<String>("Level", level
							.getName()));
					if (label != null) {
						properties
								.add(new LayerProperty<String>("Label", label));
					}
					Layer layer = LayerBuilder.buildLayer(p,
							LayerType.SEGMENTATION, properties);
					p.addLayer(layer);
				} else if (strLine.contains("intervals [")) {
					float begin = Float
							.valueOf(in.readLine().trim().split(" ")[2]); // begin
					float end = Float
							.valueOf(in.readLine().trim().split(" ")[2]);

					String label = null;
					String[] labelTable = in.readLine().trim().split("\"");
					if (labelTable.length >= 2) {
						String[] temp = labelTable[1].split("@:@");
						if (!temp[0].isEmpty() && !" ".equals(temp[0])) {
							label = temp[0];
						}
					}
					if (label == null) {
						label = "";
					}
					Segment sp = new Segment(begin, end, label, Color.WHITE);
					l.add(sp);
				}
			}
			in.close();
		} catch (IOException e) {
			System.out
					.println("No segmentationfile was found or error while parsing");
		}

	}

	static public void writeToFile(String file, Segmentation segmentation) {
		// FileWriter fstream;
		// BufferedWriter out;
		// try {
		// File f = new File(file);
		// f.delete();
		// f.createNewFile();
		// fstream = new FileWriter(f);
		// out = new BufferedWriter(fstream);
		// out.write("File type = \"ooTextFile\"\n");
		// out.write("Object class = \"TextGrid\"\n\n");
		//
		// ArrayList<SegmentationList> macroSegmentation = segmentation
		// .getSegmentationLists(AASModel.MACRO_LEVEL);
		// ArrayList<SegmentationList> mesoSegmentation = segmentation
		// .getSegmentationLists(AASModel.MESO_LEVEL);
		// ArrayList<SegmentationList> microSegmentation = segmentation
		// .getSegmentationLists(AASModel.MICRO_LEVEL);
		//
		// out.write("xmin = " + segmentation.getBegin() + " \n");
		// out.write("xmax = " + segmentation.getEnd() + " \n");
		// out.write("tiers? <exists> \n");
		//
		// int size = 0;
		// if (macroSegmentation != null && !macroSegmentation.isEmpty()) {
		// size++;
		// if (mesoSegmentation != null && !mesoSegmentation.isEmpty()) {
		// size++;
		// if (microSegmentation != null
		// && !microSegmentation.isEmpty()) {
		// size++;
		// }
		// }
		// }
		//
		// out.write("size = " + size + " \n");
		// out.write("item []: \n");
		//
		// // min en max bepalen
		//
		// if (microSegmentation != null && !microSegmentation.isEmpty()) {
		// out.write("\titem [1]:\n");
		// out.write("\t\tclass = \"IntervalTier\" \n");
		// out.write("\t\tname = \"micro\" \n");
		// out.write("\t\txmin = " + microSegmentation.get(0).getBegin()
		// + " \n");
		// out.write("\t\txmax = "
		// + microSegmentation.get(microSegmentation.size() - 1)
		// .getEnd() + " \n");
		// out.write("\t\tintervals: size = "
		// + segmentation.getActiveMicroSize() + " \n");
		// int count = 0;
		// for (int i = 0; i < microSegmentation.size(); i++) {
		// for (int j = 0; j < microSegmentation.get(i).size(); j++) {
		// SegmentationPart sp = microSegmentation.get(i).get(j);
		// count++;
		// out.write("\t\tintervals [" + (count) + "]:\n");
		// out.write("\t\t\txmin = " + sp.getBegin() + " \n");
		// out.write("\t\t\txmax = " + sp.getEnd() + " \n");
		// out.write("\t\t\ttext = \"" + sp.getLabel());
		// if (sp.getComment() != null
		// && !sp.getComment().isEmpty()) {
		// out.write("@:@" + sp.getComment());
		// }
		// out.write("\" \n");
		// }
		// }
		// }
		//
		// if (mesoSegmentation != null && !mesoSegmentation.isEmpty()) {
		// out.write("\titem [2]:\n");
		// out.write("\t\tclass = \"IntervalTier\" \n");
		// out.write("\t\tname = \"meso\" \n");
		// out.write("\t\txmin = " + mesoSegmentation.get(0).getBegin()
		// + " \n");
		// out.write("\t\txmax = "
		// + mesoSegmentation.get(mesoSegmentation.size() - 1)
		// .getEnd() + " \n");
		// out.write("\t\tintervals: size = "
		// + segmentation.getActiveMesoSize() + " \n");
		// int count = 0;
		// for (int i = 0; i < mesoSegmentation.size(); i++) {
		// for (int j = 0; j < mesoSegmentation.get(i).size(); j++) {
		// SegmentationPart sp = mesoSegmentation.get(i).get(j);
		// count++;
		// out.write("\t\tintervals [" + (count) + "]:\n");
		// out.write("\t\t\txmin = " + sp.getBegin() + " \n");
		// out.write("\t\t\txmax = " + sp.getEnd() + " \n");
		// out.write("\t\t\ttext = \"" + sp.getLabel());
		// if (sp.getComment() != null
		// && !sp.getComment().isEmpty()) {
		// out.write("@:@" + sp.getComment());
		// }
		// out.write("\" \n");
		// }
		// }
		// }
		// if (macroSegmentation != null && !macroSegmentation.isEmpty()) {
		// out.write("\titem [3]:\n");
		// out.write("\t\tclass = \"IntervalTier\" \n");
		// out.write("\t\tname = \"macro\" \n");
		// out.write("\t\txmin = " + macroSegmentation.get(0).getBegin()
		// + " \n");
		// out.write("\t\txmax = "
		// + macroSegmentation.get(macroSegmentation.size() - 1)
		// .getEnd() + " \n");
		// out.write("\t\tintervals: size = "
		// + segmentation.getActiveMacroSize() + " \n");
		// int count = 0;
		// for (int i = 0; i < macroSegmentation.size(); i++) {
		// for (int j = 0; j < macroSegmentation.get(i).size(); j++) {
		// SegmentationPart sp = macroSegmentation.get(i).get(j);
		// count++;
		// out.write("\t\tintervals [" + (count) + "]:\n");
		// out.write("\t\t\txmin = " + sp.getBegin() + " \n");
		// out.write("\t\t\txmax = " + sp.getEnd() + " \n");
		// out.write("\t\t\ttext = \"" + sp.getLabel());
		// if (sp.getComment() != null
		// && !sp.getComment().isEmpty()) {
		// out.write("@:@" + sp.getComment());
		// }
		// out.write("\" \n");
		// }
		// }
		// }
		// out.close();
		// } catch (IOException ex) {
		// System.out.println("No access to file/dir or error while parsing");
		// }
	}

	static public void parseCSVFile(String file) {
		Segmentation.getInstance().deleteAll();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));

			SegmentationLevel level = null;
			String label = null;

			SegmentationList l = null;
			Segment s = null;

			String strLine;

			while ((strLine = in.readLine().trim()) != null) {
				String[] values = strLine.split(";");

				if (values[0].equals("Level")) {
					level = SegmentationLevel.getLevelByName(values[1]);
					if (level == null) {
						level = SegmentationLevel.CUSTOM;
					}
					s = null;
					label = null;
					l = null;
				} else if (values[0].equals("Label")) {
					if (values.length > 1) {
						label = values[1];
					} else {
						label = null;
					}

					LinkedPanel p = LinkedFrame.getInstance().addPanel(
							Quantity.TIME, Quantity.NONE, Color.WHITE);
					ArrayList<LayerProperty> properties = new ArrayList<LayerProperty>();
					properties.add(new LayerProperty<String>("Level", level
							.getName()));
					if (label != null) {
						properties
								.add(new LayerProperty<String>("Label", label));
					}
					Layer layer = LayerBuilder.buildLayer(p,
							LayerType.SEGMENTATION, properties);
					p.addLayer(layer);
					if (label == null) {
						l = Segmentation.getInstance()
								.constructNewSegmentationList(level);
					} else {
						l = Segmentation.getInstance()
								.constructNewSegmentationList(level, label);
					}
				} else if (values[0].equals("Segment")) {
					if (values.length > 3) {
						s = new Segment(Float.valueOf(values[1]),
								Float.valueOf(values[2]), values[3],
								Color.WHITE);
					} else {
						s = new Segment(Float.valueOf(values[1]),
								Float.valueOf(values[2]), "", Color.WHITE);
					}
					l.add(s);
				}
			}
			in.close();
		} catch (IOException e) {
			System.out
					.println("No segmentationfile was found or error while parsing");
		}
	}

	static public void writeToCSVFile(String file, Segmentation segmentation) {
		// FileWriter fstream;
		// BufferedWriter out;
		// try {
		// File f = new File(file);
		// f.delete();
		// f.createNewFile();
		// fstream = new FileWriter(f);
		// out = new BufferedWriter(fstream);
		// ArrayList<SegmentationList> segmentationLists;
		//
		// for (int i = AASModel.MACRO_LEVEL; i <= AASModel.MICRO_LEVEL; i++) {
		// segmentationLists = segmentation.getSegmentationLists(i);
		// if (segmentationLists != null && !segmentationLists.isEmpty()) {
		// switch (i) {
		// case AASModel.MACRO_LEVEL:
		// out.write("MACRO\n");
		// break;
		// case AASModel.MESO_LEVEL:
		// out.write("MESO\n");
		// break;
		// case AASModel.MICRO_LEVEL:
		// out.write("MICRO\n");
		// break;
		// }
		// for (int j = 0; j < segmentationLists.size(); j++) {
		// for (int k = 0; k < segmentationLists.get(j).size(); k++) {
		// SegmentationPart sp = segmentationLists.get(j).get(
		// k);
		// if (sp.getComment() == null) {
		// out.write(sp.getBegin() + ";" + sp.getEnd()
		// + ";" + sp.getLabel() + "\n");
		// } else {
		// out.write(sp.getBegin() + ";" + sp.getEnd()
		// + ";" + sp.getLabel() + ";"
		// + sp.getComment() + "\n");
		// }
		// }
		// }
		// }
		// }
		// out.close();
		// } catch (IOException e) {
		// System.out.println("No access to file/dir or error while parsing");
		// }
	}

}
