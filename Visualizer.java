package dataVisualizer;


public class Visualizer {
	public static void main(String[] args) {
		Parser par = new Parser("/home/michael/Desktop/SPADESStudy/p001/MasterSynced/");
//		Parser par = new Parser("/home/michael/Desktop/SPADESStudy/p001/MasterSynced/2015/08/03");
		HeatMapGUI graph = new HeatMapGUI(par.getPhoneData(), par.getWatchData());
//		HeatMapGUI graph = new HeatMapGUI(null, par.getWatchData());
		graph.draw();	
	}
}
