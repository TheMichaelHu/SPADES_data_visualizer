package dataVisualizer;


public class Visualizer {
	public static void main(String[] args) {
		Parser par = new Parser("/home/michael/Desktop/SPADESStudy/p001/MasterSynced/");
//		Parser par = new Parser("/home/michael/Desktop/SPADESStudy/p001/MasterSynced/2015/08/03");
		Drawer graph = new Drawer(par.getPhoneData(), par.getWatchData());
//		HeatMapGUI graph = new HeatMapGUI(null, par.getWatchData());
		graph.draw();	
	}
}
