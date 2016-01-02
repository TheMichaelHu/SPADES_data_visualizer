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

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Drawer extends JPanel {
	private static final long serialVersionUID = 1L;

	// Preferences
	private static final int WIDTH = 3000;
	private static final int HEIGHT = 500;
	
	private static final boolean DRAW_X_ACCEL = false;
	private static final boolean DRAW_Y_ACCEL = true;
	private static final boolean DRAW_Z_ACCEL = false;

	// Configs
	private static final int ACCEL_SCALE = 10000; // acceleration is scaled in
													// Data for more precise
													// calculations
	private static final int Y_HATCH_INTERVAL = (int) (1 * ACCEL_SCALE);
	private static final int DAYS_PER_ROW = 7;

	// Constants
	private static final int TEXT_OFFSET = 5;
	private static final int PADDING = 30;
	private static final int GRAPH_POINT_WIDTH = 1;
	private static final int HATCH_LENGTH = 5;
	private static final Color BACKGROUND_COLOR = new Color(24, 24, 24);
	private static final Color LINE_COLOR = new Color(160, 160, 160);

	 private static final Color PHONE_X_COLOR = new Color(255, 96, 0);
	 private static final Color PHONE_Y_COLOR = new Color(255, 166, 0);
	 private static final Color PHONE_Z_COLOR = new Color(255, 60, 0);
	
	 private static final Color WATCH_X_COLOR = new Color(0, 96, 255);
	 private static final Color WATCH_Y_COLOR = new Color(0, 166, 255);
	 private static final Color WATCH_Z_COLOR = new Color(0, 60, 255);

	// Default Colors
	private static final Color X_COLOR = new Color(255, 0, 0);
	private static final Color Y_COLOR = new Color(255, 128, 0);
	private static final Color Z_COLOR = new Color(255, 255, 0);

	// list of aggregated phone accelerometer data ordered by time
	private ArrayList<Data> phoneData;
	// list of aggregated watch accelerometer data ordered by time
	private ArrayList<Data> watchData;
	private double minX, maxX, minY, maxY;
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
		double phoneMaxY, phoneMinY, watchMaxY, watchMinY, phoneMinX, watchMinX;
		phoneMaxY = watchMaxY = Integer.MIN_VALUE;
		phoneMinY = watchMinY = phoneMinX = watchMinX = Integer.MAX_VALUE;

		if (isPhoneData) {
			phoneMinX = this.phoneData.get(0).getTime();
			for (Data d : phoneData) {
				phoneMaxY = Math.max(d.x_a, phoneMaxY);
				phoneMinY = Math.min(d.x_a, phoneMinY);
				phoneMaxY = Math.max(d.y_a, phoneMaxY);
				phoneMinY = Math.min(d.y_a, phoneMinY);
				phoneMaxY = Math.max(d.z_a, phoneMaxY);
				phoneMinY = Math.min(d.z_a, phoneMinY);
				phoneMinX = Math.min(d.getTime(), phoneMinX);
			}
		}
		if (isWatchData) {
			watchMinX = this.watchData.get(0).getTime();
			for (Data d : watchData) {
				watchMaxY = Math.max(d.x_a, watchMaxY);
				watchMinY = Math.min(d.x_a, watchMinY);
				watchMaxY = Math.max(d.y_a, watchMaxY);
				watchMinY = Math.min(d.y_a, watchMinY);
				watchMaxY = Math.max(d.z_a, watchMaxY);
				watchMinY = Math.min(d.z_a, watchMinY);
				watchMinX = Math.min(d.getTime(), watchMinX);
			}
		}

		this.minY = Math.min(phoneMinY, watchMinY);
		this.maxY = Math.max(phoneMaxY, watchMaxY);
		this.minX = Math.min(phoneMinX, watchMinX);

		this.minX = Math.floor(this.minX);
		this.maxX = this.minX + DAYS_PER_ROW;
	}

	private Point scalePoint(double x, double y, int width, int height) {
		double scaleX = (width - 2 * PADDING) / (this.maxX - this.minX);
		double scaleY = (height - 2 * PADDING) / (this.maxY - this.minY);
		int scaledX = PADDING + (int) ((x - this.minX) * scaleX);
		int scaledY = (height - PADDING) - (int) ((y - this.minY) * scaleY);
		return new Point(scaledX, scaledY);
	}

	@Override
	protected void paintComponent(Graphics g) {
		int start = 0;
		int increment = (WIDTH - PADDING) / 2;
		int end = increment;

		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (this.isPhoneData) {
			g2.setColor(LINE_COLOR);
			drawCenterString(g2, "Phone Data: Week " + (startDay / 7 + 1), start + (increment / 2), 0);
			drawPlot(g2, this.phoneData, PHONE_X_COLOR, PHONE_Y_COLOR, PHONE_Z_COLOR, start, end, 10, HEIGHT);
		}
		
		start = end + PADDING/2;
		end = start + increment;
		
		if (this.isWatchData) {
			g2.setColor(LINE_COLOR);
			drawCenterString(g2, "Watch Data: Week " + (startDay / 7 + 1), start + (increment / 2), 0);
			drawPlot(g2, this.watchData, WATCH_X_COLOR, WATCH_Y_COLOR, WATCH_Z_COLOR, start, end, 10, HEIGHT);
		}
	}

	@SuppressWarnings("unused")
	private void drawPlot(Graphics2D g2, ArrayList<Data> data, int xStart,
			int xEnd, int yStart, int yEnd) {
		drawPlot(g2, data, X_COLOR, Y_COLOR, Z_COLOR, xStart, xEnd, yStart, yEnd);
	}

	private void drawPlot(Graphics2D g2, ArrayList<Data> data, Color xc,
			Color yc, Color zc, int xStart, int xEnd, int yStart, int yEnd) {
		int originX = xStart + PADDING;
		int originY = this.scalePoint(0, 0, xEnd - xStart, yEnd - yStart).y + yStart;
		ArrayList<ArrayList<Point>> graphPoints;

		// draw x(time) and y(acceleration) axis
		g2.setColor(LINE_COLOR);
		g2.setStroke(new BasicStroke(2));
		g2.drawLine(originX, yEnd - PADDING, originX, PADDING + yStart);
		g2.drawLine(originX, originY, xEnd - PADDING, originY);

		// draw hatch marks for positive y axis.
		for (int i = 0; i < (int) (maxY); i++) {
			int x0 = originX - HATCH_LENGTH;
			int x1 = originX + HATCH_LENGTH;
			int y0 = scalePoint(0, i * Y_HATCH_INTERVAL, xEnd - xStart, yEnd - yStart).y
					+ yStart;
			if (y0 > PADDING + yStart) {
				g2.drawLine(x0, y0, x1, y0);
				drawCenterString(
						g2,
						String.format("%.1f", ((double) Y_HATCH_INTERVAL
								/ ACCEL_SCALE * i))
								+ " m/s", originX, y0);
			}
		}

		// draw hatch marks for negative y axis.
		for (int i = 0; i > (int) (minY); i--) {
			int x0 = originX - HATCH_LENGTH;
			int x1 = originX + HATCH_LENGTH;
			int y0 = scalePoint(0, i * Y_HATCH_INTERVAL, xEnd - xStart, yEnd - yStart).y
					+ yStart;
			if (y0 < yEnd - PADDING - TEXT_OFFSET) {
				g2.drawLine(x0, y0, x1, y0);
				drawCenterString(
						g2,
						String.format("%.1f", ((double) Y_HATCH_INTERVAL
								/ ACCEL_SCALE * i))
								+ " m/s", originX, y0);
			}
		}

		// and for x axis
		for (int i = 0; i < DAYS_PER_ROW; i++) {
			int x0 = scalePoint((int) (minX + i), 0, xEnd - xStart, yEnd - yStart).x + xStart;
			int xText = x0
					+ (x0 - (scalePoint(minX + i - 1, 0, xEnd - xStart, yEnd - yStart).x + xStart))
					/ 2;
			int y0 = originY + HATCH_LENGTH;
			int y1 = originY - HATCH_LENGTH;
			g2.drawLine(x0, y0, x0, y1);
			drawCenterString(g2, "Day " + (i + startDay), xText, originY);
		}

		graphPoints = dataToPoints(data, xEnd - xStart, yEnd - yStart);
		if (DRAW_X_ACCEL) {
			g2.setColor(xc);
			drawPoints(graphPoints.get(0), g2, xStart, yStart);
		}
		if (DRAW_Y_ACCEL) {
			g2.setColor(yc);
			drawPoints(graphPoints.get(1), g2, xStart, yStart);
		}
		if (DRAW_Z_ACCEL) {
			g2.setColor(zc);
			drawPoints(graphPoints.get(2), g2, xStart, yStart);
		}
	}

	private void drawCenterString(Graphics2D g2, String str, int x, int y) {
		FontMetrics fm = g2.getFontMetrics();
		int textWidth = fm.stringWidth(str);
		int textHeight = fm.getHeight() + fm.getAscent();
		g2.drawString(str, x - textWidth / 2, y + TEXT_OFFSET + textHeight / 2);
	}

	private ArrayList<ArrayList<Point>> dataToPoints(ArrayList<Data> data, int width,
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
				points.add(this.scalePoint(x1Shifted, y1, width, height));
			}
		}
		xyzPoints.add(points);
		points = new ArrayList<Point>();

		for (int i = 0; i < data.size(); i++) {
			double x1 = data.get(i).getTime();
			double x1Shifted = data.get(i).getTime() - (startDay - 1);
			double y1 = data.get(i).getYAccel();
			if ((x1 - this.minX + 1 >= startDay && x1 - this.minX + 1 < startDay + 7)) {
				points.add(this.scalePoint(x1Shifted, y1, width, height));
			}
		}
		xyzPoints.add(points);
		points = new ArrayList<Point>();

		for (int i = 0; i < data.size(); i++) {
			double x1 = data.get(i).getTime();
			double x1Shifted = data.get(i).getTime() - (startDay - 1);
			double y1 = data.get(i).getZAccel();
			if ((x1 - this.minX + 1 >= startDay && x1 - this.minX + 1 < startDay + 7)) {
				points.add(this.scalePoint(x1Shifted, y1, width, height));
			}
		}
		xyzPoints.add(points);
		return xyzPoints;
	}

	private void drawPoints(ArrayList<Point> points, Graphics2D g2, int xOffset, int yOffset) {
		for (int i = 0; i < points.size(); i++) {
			int x = points.get(i).x - GRAPH_POINT_WIDTH / 2 + xOffset;
			int y = points.get(i).y - GRAPH_POINT_WIDTH / 2 + yOffset;

			int ovalW = GRAPH_POINT_WIDTH;
			int ovalH = GRAPH_POINT_WIDTH;
			g2.fillOval(x, y, ovalW, ovalH);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
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
		int h = this.isPhoneData != this.isWatchData ? HEIGHT / 2 : HEIGHT;
		BufferedImage img = new BufferedImage(WIDTH, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		this.paintComponent(g);
		return img;
	}
	
	public BufferedImage getImages(int weeks) {
		BufferedImage img = this.drawImage();
		System.out.println("Week 1 drawn!");
		int temp = startDay;
		
		for(int i = 1; i < weeks; i++) {
			this.startDay += 7;
			img = this.drawOnImage(img);
			System.out.println("Week " + (this.startDay/7 + 1) + " drawn!");
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