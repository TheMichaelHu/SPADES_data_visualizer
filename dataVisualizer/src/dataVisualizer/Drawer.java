package dataVisualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Drawer extends JPanel {
	private static final long serialVersionUID = 1L;

	// Preferences
	private static final boolean FIXED_DAYS_PER_ROW = true;
	private static final boolean DRAW_X_ACCEL = true;
	private static final boolean DRAW_Y_ACCEL = false;
	private static final boolean DRAW_Z_ACCEL = false;

	// Configs
	private static final int ACCEL_SCALE = 10000; // acceleration is scaled in
													// Data for more precise
													// calculations
	private static final int Y_HATCH_INTERVAL = (int) (1.2 * ACCEL_SCALE);
	private static final int DAYS_PER_ROW = 7; // only matters if
												// FIXED_DAYS_PER_ROW is true

	// Constants
	private static final int TEXT_OFFSET = 5;
	private static final int PREF_W = 1750;
	private static final int PREF_H = 900;
	private static final int PADDING = 30;
	private static final int GRAPH_POINT_WIDTH = 1;
	private static final int HATCH_LENGTH = 5;
	private static final Color BACKGROUND_COLOR = new Color(24, 24, 24);
	private static final Color LINE_COLOR = new Color(160, 160, 160);

	// private static final Color PHONE_X_COLOR = new Color(255, 96, 0);
	// private static final Color PHONE_Y_COLOR = new Color(255, 166, 0);
	// private static final Color PHONE_Z_COLOR = new Color(255, 60, 0);
	//
	// private static final Color WATCH_X_COLOR = new Color(0, 96, 255);
	// private static final Color WATCH_Y_COLOR = new Color(0, 166, 255);
	// private static final Color WATCH_Z_COLOR = new Color(0, 60, 255);

	// Default Colors
	private static final Color X_COLOR = new Color(255, 0, 0);
	private static final Color Y_COLOR = new Color(255, 128, 0);
	private static final Color Z_COLOR = new Color(255, 255, 0);

	// list of aggregated phone accelerometer data ordered by time
	private ArrayList<Data> phoneData;
	// list of aggregated watch accelerometer data ordered by time
	private ArrayList<Data> watchData;
	private double minX, maxX, minY, maxY, scaleX;
	private boolean isPhoneData, isWatchData;
	private int startDay;

	public Drawer(ArrayList<Data> phoneData, ArrayList<Data> watchData) {
		this(phoneData, watchData, 1);
	}

	public Drawer(ArrayList<Data> phoneData, ArrayList<Data> watchData,
			int startDay) {
		this.phoneData = phoneData;
		this.watchData = watchData;
		this.startDay = startDay;
		isPhoneData = phoneData != null && phoneData.size() > 0;
		isWatchData = watchData != null && watchData.size() > 0;
		this.calcRange();
	}

	private void calcRange() {
		double phoneMaxY, phoneMinY, watchMaxY, watchMinY, phoneMaxX, phoneMinX, watchMaxX, watchMinX;
		phoneMaxY = phoneMinY = watchMaxY = watchMinY = phoneMaxX = phoneMinX = watchMaxX = watchMinX = 0;

		if (isPhoneData) {
			Collections.sort(this.phoneData);
			for (Data d : phoneData) {
				phoneMaxY = Math.max(d.x_a, phoneMaxY);
				phoneMinY = Math.min(d.x_a, phoneMinY);
				phoneMaxY = Math.max(d.y_a, phoneMaxY);
				phoneMinY = Math.min(d.y_a, phoneMinY);
				phoneMaxY = Math.max(d.z_a, phoneMaxY);
				phoneMinY = Math.min(d.z_a, phoneMinY);
			}
		}
		if (isWatchData) {
			Collections.sort(this.watchData);
			for (Data d : watchData) {
				watchMaxY = Math.max(d.x_a, watchMaxY);
				watchMinY = Math.min(d.x_a, watchMinY);
				watchMaxY = Math.max(d.y_a, watchMaxY);
				watchMinY = Math.min(d.y_a, watchMinY);
				watchMaxY = Math.max(d.z_a, watchMaxY);
				watchMinY = Math.min(d.z_a, watchMinY);
			}
		}

		this.minY = Math.min(phoneMinY, watchMinY);
		this.maxY = Math.max(phoneMaxY, watchMaxY);

		if (isPhoneData && isWatchData) {
			phoneMinX = phoneData.get(0).getTime();
			watchMinX = watchData.get(0).getTime();
			phoneMaxX = phoneData.get(phoneData.size() - 1).getTime();
			watchMaxX = watchData.get(watchData.size() - 1).getTime();

			this.minX = Math.min(phoneMinX, watchMinX);
			this.maxX = Math.max(phoneMaxX, watchMaxX);
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
		if (FIXED_DAYS_PER_ROW) {
			this.maxX = this.minX + DAYS_PER_ROW;
		}

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
		int increment = PREF_H / 2;
		int end = increment;

		// if(isPhoneData != isWatchData) {
		// end = PREF_H;
		// }

		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (this.isPhoneData) {
			g2.setColor(LINE_COLOR);
			drawCenterString(g2, "Phone Data: Day " + startDay + " to Day "
					+ (startDay + 6), PREF_W / 2, start);
			drawPlot(g2, this.phoneData, start, end);
			start = end;
			end += increment;
		}
		if (this.isWatchData) {
			g2.setColor(LINE_COLOR);
			drawCenterString(g2, "Watch Data: Day " + startDay + " to Day "
					+ (startDay + 6), PREF_W / 2, start);
			drawPlot(g2, this.watchData, start, end);
			start = end;
			end += increment;
		}
	}

	private void drawPlot(Graphics2D g2, ArrayList<Data> data, int yStart,
			int yEnd) {
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
		g2.drawLine(PADDING, originY, PREF_W - PADDING, originY);

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
		if (FIXED_DAYS_PER_ROW) {
			for (int i = 0; i < DAYS_PER_ROW; i++) {
				int x0 = scalePoint((int) minX + i, 0, yEnd - yStart).x;
				int xText = x0
						+ (x0 - scalePoint((int) minX + i - 1, 0, yEnd - yStart).x)
						/ 2;
				int y0 = originY + HATCH_LENGTH;
				int y1 = originY - HATCH_LENGTH;
				g2.drawLine(x0, y0, x0, y1);
				drawCenterString(g2, "Day " + (i + startDay), xText, originY);
			}

		} else {
			for (int i = 0; i < (int) (maxX - minX + 1); i++) {
				int x0 = scalePoint((int) minX + i, 0, yEnd - yStart).x;
				int xText = x0
						+ (x0 - scalePoint((int) minX + i - 1, 0, yEnd - yStart).x)
						/ 2;
				int y0 = originY + HATCH_LENGTH;
				int y1 = originY - HATCH_LENGTH;
				g2.drawLine(x0, y0, x0, y1);
				drawCenterString(g2, "Day " + (i + startDay), xText, originY);
			}
		}

		graphPoints = dataToPoints(data, yEnd - yStart);
		if (DRAW_X_ACCEL) {
			g2.setColor(xc);
			drawPoints(graphPoints.get(0), g2, yStart);
		}
		if (DRAW_Y_ACCEL) {
			g2.setColor(yc);
			drawPoints(graphPoints.get(1), g2, yStart);
		}
		if (DRAW_Z_ACCEL) {
			g2.setColor(zc);
			drawPoints(graphPoints.get(2), g2, yStart);
		}
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
			double x1Shifted = data.get(i).getTime() - (startDay - 1);
			double y1 = data.get(i).getXAccel();
			if ((x1 - this.minX + 1 >= startDay && x1 - this.minX + 1 < startDay + 7)) {
				points.add(this.scalePoint(x1Shifted, y1, height));
			}
		}
		xyzPoints.add(points);
		points.clear();

		for (int i = 0; i < data.size(); i++) {
			double x1 = data.get(i).getTime();
			double x1Shifted = data.get(i).getTime() - (startDay - 1);
			double y1 = data.get(i).getYAccel();
			if ((x1 - this.minX + 1 >= startDay && x1 - this.minX + 1 < startDay + 7)) {
				points.add(this.scalePoint(x1Shifted, y1, height));
			}
		}
		xyzPoints.add(points);
		points.clear();

		for (int i = 0; i < data.size(); i++) {
			double x1 = data.get(i).getTime();
			double x1Shifted = data.get(i).getTime() - (startDay - 1);
			double y1 = data.get(i).getZAccel();
			if ((x1 - this.minX + 1 >= startDay && x1 - this.minX + 1 < startDay + 7)) {
				points.add(this.scalePoint(x1Shifted, y1, height));
			}
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
	public void drawInFrame() {
		this.setBackground(BACKGROUND_COLOR);
		JFrame frame = new JFrame("Activity Heat Map");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

	// Returns a bufferedimage containing the graphs
	private BufferedImage drawImage() {
		int h = this.isPhoneData != this.isWatchData ? PREF_H / 2 : PREF_H;
		BufferedImage img = new BufferedImage(PREF_W, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		this.paintComponent(g);
		return img;
	}
	
	public BufferedImage getImages(int weeks) {
		BufferedImage img = this.drawImage();
		int temp = startDay;
		
		for(int i = 1; i < weeks; i++) {
			this.startDay += 7;
			img = this.drawOnImage(img);
		}
		this.startDay = temp;
		return img;
	}

	// Returns a bufferedimage containing the graphs
	public BufferedImage drawOnImage(BufferedImage background) {
		BufferedImage img = this.drawImage();

		if (background == null) {
			return img;
		}

		int w = Math.max(background.getWidth(), img.getWidth());
		int h = background.getHeight() + img.getHeight();
		BufferedImage combined = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = combined.createGraphics();
		g.drawImage(background, 0, 0, null);
		g.drawImage(img, 0, background.getHeight(), null);

		return combined;
	}
}