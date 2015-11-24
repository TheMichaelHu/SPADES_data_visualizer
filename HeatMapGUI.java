package dataVisualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class HeatMapGUI extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int Y_HATCH_INTERVAL = 4000; // .4 m/s (acceleration is
														// scaled up by 10000 in
														// Data)
	private static final int ACCEL_SCALE = 10000; // acceleration is scaled in
													// Data for more precise
													// calculations
	private static final int TEXT_OFFSET = 5;
	private static final int PREF_W = 1750;
	private static final int PREF_H = 900;
	private static final int PADDING = 30;
	private static final int GRAPH_POINT_WIDTH = 5;
	private static final int HATCH_LENGTH = 5;
	private static final Color BACKGROUND_COLOR = new Color(24, 24, 24);
	private static final Color LINE_COLOR = new Color(160, 160, 160);

	private static final Color PHONE_X_COLOR = new Color(255, 96, 0, 20);
	private static final Color PHONE_Y_COLOR = new Color(255, 166, 0, 20);
	private static final Color PHONE_Z_COLOR = new Color(255, 60, 0, 20);

	private static final Color WATCH_X_COLOR = new Color(0, 96, 255, 10);
	private static final Color WATCH_Y_COLOR = new Color(0, 166, 255, 10);
	private static final Color WATCH_Z_COLOR = new Color(0, 60, 255, 10);
	
	// Default Colors
	private static final Color X_COLOR = new Color(255, 0, 0, 20);
	private static final Color Y_COLOR = new Color(255, 128, 0, 20);
	private static final Color Z_COLOR = new Color(255, 255, 0, 20);

	// list of aggregated phone accelerometer data ordered by time
	private ArrayList<Data> phoneData;
	// list of aggregated watch accelerometer data ordered by time
	private ArrayList<Data> watchData;
	private double minX, maxX, minY, maxY, scaleX;
	private boolean isPhoneData, isWatchData;

	public HeatMapGUI(ArrayList<Data> phoneData, ArrayList<Data> watchData) {
		this.phoneData = phoneData;
		this.watchData = watchData;
		isPhoneData = phoneData != null && phoneData.size() > 0;
		isWatchData = watchData != null && watchData.size() > 0;
		this.calcRange();
	}

	private void calcRange() {
		double phoneMaxY, phoneMinY, watchMaxY, watchMinY, phoneMaxX, phoneMinX, watchMaxX, watchMinX;
		phoneMaxY = phoneMinY = watchMaxY = watchMinY = phoneMaxX = phoneMinX = watchMaxX = watchMinX = 0;

		if(isPhoneData) {
			Collections.sort(this.phoneData);
			for (Data d : phoneData) {
				phoneMaxY = d.x_a > phoneMaxY ? d.x_a : phoneMaxY;
				phoneMinY = d.x_a < phoneMinY ? d.x_a : phoneMinY;
				phoneMaxY = d.y_a > phoneMaxY ? d.y_a : phoneMaxY;
				phoneMinY = d.y_a < phoneMinY ? d.y_a : phoneMinY;
				phoneMaxY = d.z_a > phoneMaxY ? d.z_a : phoneMaxY;
				phoneMinY = d.z_a < phoneMinY ? d.z_a : phoneMinY;
			}
		}
		if(isWatchData) {
			Collections.sort(this.watchData);
			for (Data d : watchData) {
				watchMaxY = d.x_a > watchMaxY ? d.x_a : watchMaxY;
				watchMinY = d.x_a < watchMinY ? d.x_a : watchMinY;
				watchMaxY = d.y_a > watchMaxY ? d.y_a : watchMaxY;
				watchMinY = d.y_a < watchMinY ? d.y_a : watchMinY;
				watchMaxY = d.z_a > watchMaxY ? d.z_a : watchMaxY;
				watchMinY = d.z_a < watchMinY ? d.z_a : watchMinY;
			}
		}

		this.minY = phoneMinY < watchMinY ? phoneMinY : watchMinY;
		this.maxY = phoneMaxY > watchMaxY ? phoneMaxY : watchMaxY;

		if (isPhoneData && isWatchData) {
			phoneMinX = phoneData.get(0).getTime();
			watchMinX = watchData.get(0).getTime();
			phoneMaxX = phoneData.get(phoneData.size() - 1).getTime();
			watchMaxX = watchData.get(watchData.size() - 1).getTime();

			this.minX = phoneMinX < watchMinX ? phoneMinX : watchMinX;
			this.maxX = phoneMaxX > watchMaxX ? phoneMaxX : watchMaxX;
		} else if (isPhoneData) {
			phoneMinX = phoneData.get(0).getTime();
			phoneMaxX = phoneData.get(phoneData.size() - 1).getTime();

			this.minX = phoneMinX;
			this.maxX = phoneMaxX;
		} else if (isWatchData) {
			watchMinX = watchData.get(0).getTime();
			watchMaxX = watchData.get(watchData.size() - 1).getTime();

			this.minX = watchMinX;
			this.maxX = watchMaxX;
		}

		this.minX += .001; // stupid lazy fix for stupid time bug

		this.scaleX = (PREF_W - 2 * PADDING) / (this.maxX - this.minX);
	}

	private Point scalePoint(double x, double y, int height) {
		double scaleY = (height - 2 * PADDING) / (this.maxY - this.minY);
		int scaledX = PADDING + (int) ((x - this.minX) * scaleX);
		int scaledY = (height - PADDING) - (int) ((y - this.minY) * scaleY);
		return new Point(scaledX, scaledY);
	}

	@Override
	protected void paintComponent(Graphics g) {
		int start = 10;
		int increment = getHeight() / 2;
		int end = increment;
		
		if(isPhoneData != isWatchData) {
			end = getHeight();
		}

		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (this.isPhoneData) {
			g2.setColor(LINE_COLOR);
			drawCenterString(g2, "Phone Data", getWidth()/2, start);
			drawPlot(g2, this.phoneData, start, end);
			start = end;
			end += increment;
		}
		if (this.isWatchData) {
			g2.setColor(LINE_COLOR);
			drawCenterString(g2, "Watch Data", getWidth()/2, start);
			drawPlot(g2, this.watchData, start, end);
			start = end;
			end += increment;
		}
	}

	private void drawPlot(Graphics2D g2, ArrayList<Data> data, int yStart, int yEnd) {
		drawPlot(g2, data, X_COLOR, Y_COLOR, Z_COLOR, yStart, yEnd);
	}
	
	private void drawPlot(Graphics2D g2, ArrayList<Data> data, Color xc,
			Color yc, Color zc, int yStart, int yEnd) {
		int originY = this.scalePoint(0, 0, yEnd - yStart).y + yStart;
		ArrayList<ArrayList<Point>> graphPoints;

		// draw x(time) and y(acceleration) axis
		g2.setColor(LINE_COLOR);
		g2.setStroke(new BasicStroke(2));
		g2.drawLine(PADDING, yEnd - PADDING, PADDING, PADDING + yStart);
		g2.drawLine(PADDING, originY, getWidth() - PADDING, originY);

		// draw hatch marks for positive y axis.
		for (int i = 0; i < (int) (maxY); i++) {
			int x0 = PADDING - HATCH_LENGTH;
			int x1 = PADDING + HATCH_LENGTH;
			int y0 = scalePoint(0, i * Y_HATCH_INTERVAL, yEnd - yStart).y
					+ yStart;
			if (y0 > PADDING + yStart) {
				g2.drawLine(x0, y0, x1, y0);
				drawCenterString(
						g2,
						String.format("%.1f", ((double) Y_HATCH_INTERVAL
								/ ACCEL_SCALE * i))
								+ " m/s", PADDING, y0);
			}
		}

		// draw hatch marks for negative y axis.
		for (int i = 0; i > (int) (minY); i--) {
			int x0 = PADDING - HATCH_LENGTH;
			int x1 = PADDING + HATCH_LENGTH;
			int y0 = scalePoint(0, i * Y_HATCH_INTERVAL, yEnd - yStart).y
					+ yStart;
			if (y0 < yEnd - PADDING - TEXT_OFFSET) {
				g2.drawLine(x0, y0, x1, y0);
				drawCenterString(
						g2,
						String.format("%.1f", ((double) Y_HATCH_INTERVAL
								/ ACCEL_SCALE * i))
								+ " m/s", PADDING, y0);
			}
		}

		// and for x axis
		for (int i = 0; i < (int) (maxX - minX + 1); i++) {
			int x0 = scalePoint((int) minX + i, 0, yEnd - yStart).x;
			int xText = x0
					+ (x0 - scalePoint((int) minX + i - 1, 0, yEnd - yStart).x)
					/ 2;
			int y0 = originY + HATCH_LENGTH;
			int y1 = originY - HATCH_LENGTH;
			g2.drawLine(x0, y0, x0, y1);
			drawCenterString(g2, "Day " + (i + 1), xText, originY);
		}

		graphPoints = dataToPoints(data, yEnd - yStart);
		g2.setColor(xc);
		drawPoints(graphPoints.get(0), g2, yStart);
		g2.setColor(yc);
		drawPoints(graphPoints.get(1), g2, yStart);
		g2.setColor(zc);
		drawPoints(graphPoints.get(2), g2, yStart);
	}

	private void drawCenterString(Graphics2D g2, String str, int x, int y) {
		FontMetrics fm = g2.getFontMetrics();
		int textWidth = fm.stringWidth(str);
		int textHeight = fm.getHeight() + fm.getAscent();
		g2.drawString(str, x - textWidth / 2, y + TEXT_OFFSET + textHeight / 2);
	}

	private ArrayList<ArrayList<Point>> dataToPoints(ArrayList<Data> data,
			int height) {

		if (data.size() == 0) {
			return new ArrayList<ArrayList<Point>>();
		}

		ArrayList<ArrayList<Point>> xyzPoints = new ArrayList<ArrayList<Point>>();
		ArrayList<Point> points = new ArrayList<Point>();

		for (int i = 0; i < data.size(); i++) {
			double x1 = data.get(i).getTime();
			double y1 = data.get(i).getXAccel();
			points.add(this.scalePoint(x1, y1, height));
		}
		xyzPoints.add(points);
		points = new ArrayList<Point>();

		for (int i = 0; i < data.size(); i++) {
			double x1 = data.get(i).getTime();
			double y1 = data.get(i).getYAccel();
			points.add(this.scalePoint(x1, y1, height));
		}
		xyzPoints.add(points);
		points = new ArrayList<Point>();

		for (int i = 0; i < data.size(); i++) {
			double x1 = data.get(i).getTime();
			double y1 = data.get(i).getZAccel();
			points.add(this.scalePoint(x1, y1, height));
		}
		xyzPoints.add(points);
		return xyzPoints;
	}

	private void drawPoints(ArrayList<Point> points, Graphics2D g2, int offset) {
		for (int i = 0; i < points.size(); i++) {
			int x = points.get(i).x - GRAPH_POINT_WIDTH / 2;
			int y = points.get(i).y - GRAPH_POINT_WIDTH / 2 + offset;

			int ovalW = GRAPH_POINT_WIDTH;
			int ovalH = GRAPH_POINT_WIDTH;
			g2.fillOval(x, y, ovalW, ovalH);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(PREF_W, PREF_H);
	}

	// Creates a JFrame and draws the graph on it
	public void draw() {
		this.setBackground(BACKGROUND_COLOR);
		JFrame frame = new JFrame("Activity Heat Map");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}
}