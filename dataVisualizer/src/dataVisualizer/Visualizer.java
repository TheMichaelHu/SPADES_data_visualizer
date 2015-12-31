package dataVisualizer;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class Visualizer {
	public static void main(String[] args) {
		Parser par = new Parser("/home/michael/Desktop/SPADESStudy/p001/MasterSynced/");
		Drawer graph = new Drawer(par.getPhoneData(), par.getWatchData());
		
		try {
			ImageIO.write(graph.getImages(3), "PNG", new File("/home/michael/Desktop/combined.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
