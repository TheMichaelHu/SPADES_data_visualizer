package dataVisualizer;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class Visualizer {
	public static void main(String[] args) throws IOException {
		// Directory where data can be found
		String inputPath = "/home/michael/Desktop/SPADESTEST_07/MasterSynced/2015";
		// File to output visualization to
		String outputPath = "/home/michael/Desktop/combined.png";
		
		// Parse csvs found at input path (includes sub directories)
		Parser par = new Parser(inputPath);
		// Feed parsed data to Drawer
//		Drawer graph = new Drawer(par.getPhoneData(), par.getWatchData());
		Drawer graph = new Drawer(par.getPhoneData(), null);
		
		// Output visualization to output path
		ImageIO.write(graph.getImages(13), "PNG", new File(outputPath));
	}
}
