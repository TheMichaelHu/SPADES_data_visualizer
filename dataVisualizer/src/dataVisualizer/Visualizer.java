package dataVisualizer;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class Visualizer {
	public static void main(String[] args) throws IOException {
		// Directory where data can be found
		String inputPath = "/home/michael/Desktop/SPADESTEST_07/MasterSynced/2015/";
		// File to output visualization to
		String outputPath = "/home/michael/Desktop/visualizaton.png";
		
		// Parse csvs found at input path (includes sub directories)
		Parser par = new Parser(inputPath);
		System.out.println(par.getPhoneData().size());
		
		System.out.println("Parser Initialized!");
		// Feed parsed data to Drawer
		Drawer graph = new Drawer(par.getPhoneData(), par.getWatchData());
		System.out.println("Drawer Initialized!");
		
		// Output visualization to output path
		ImageIO.write(graph.getImages(13), "PNG", new File(outputPath));
		System.out.println("Visualization Generated!");
	}
}
