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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JPanel;

class Drawer extends JPanel {
	private static final long serialVersionUID = 1L;

	// Preferences
	private static final int WIDTH = 3000;
	private static final int HEIGHT = 500;

	// Configs
	private static final double Y_HATCH_INTERVAL = .5;

	// Constants
	private static final int TEXT_OFFSET = 5;
	private static final int PADDING = 30;
	private static final int GRAPH_POINT_WIDTH = 4;
	private static final int HATCH_LENGTH = 5;
	private static final Color BACKGROUND_COLOR = new Color(255, 255, 255);
	private static final Color LINE_COLOR = new Color(0, 0, 0);

	private long minX, maxX;
	private int currentDay;

	Drawer() {
		this.currentDay = 0;
		this.calcRange();
	}

	private void calcRange() {
		long phoneMinX, watchMinX;
		phoneMinX = watchMinX = Integer.MAX_VALUE;

		if (!Visualizer.data.isEmpty()) {
			phoneMinX = Visualizer.data.get(this.currentDay).date;
		}
		if (!Visualizer.dataW.isEmpty()) {
			watchMinX = Visualizer.dataW.get(this.currentDay).date;
		}

		this.minX = phoneMinX < watchMinX? phoneMinX : watchMinX;
		this.maxX = this.minX + 24 * 60 * 60 * 1000;
	}

	private Point scalePoint(long x, double y, int width, int height) {
		double scaleX = ((double)width - 2 * PADDING) / (this.maxX - this.minX);
		double scaleY = ((double)height - 2 * PADDING) / (Utils.MAX_Y - Utils.MIN_Y);
		int scaledX = PADDING + (int) ((x - this.minX) * scaleX);
		int scaledY = (height - PADDING) - (int) ((y - Utils.MIN_Y) * scaleY);
		return new Point(scaledX, scaledY);
	}

	@Override
	protected void paintComponent(Graphics g) {
		int start = 0;
		int end = (WIDTH - PADDING);

		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		drawPlot(g2, Visualizer.data, start, end, 10, HEIGHT, "phone data");
		drawPlot(g2, Visualizer.dataW, start, end, 10, HEIGHT, "watch data");

		drawCenterString(g2, new SimpleDateFormat("MM-dd-yyyy").format(new Date(Visualizer.data.get(this.currentDay).date)), start, 0);
	}

	private void drawPlot(Graphics2D g2, ArrayList<Day> data, int xStart, int xEnd, int yStart, int yEnd, String name) {
		int originX = xStart + PADDING;
		int originY = this.scalePoint(0, 0, xEnd - xStart, yEnd - yStart).y + yStart;
		ArrayList<Point> graphPoints;

		// draw x(time) and y(acceleration) axis
		g2.setColor(LINE_COLOR);
		g2.setStroke(new BasicStroke(2));
		g2.drawLine(originX, yEnd - PADDING, originX, PADDING + yStart);
		g2.drawLine(originX, originY, xEnd - PADDING, originY);

		// draw hatch marks for positive y axis.
		for (int i = 0; i < Utils.MAX_Y/Y_HATCH_INTERVAL; i++) {
			int x0 = originX - HATCH_LENGTH;
			int x1 = originX + HATCH_LENGTH;
			int y0 = scalePoint(0, i * Y_HATCH_INTERVAL, xEnd - xStart, yEnd - yStart).y + yStart;
			if (y0 > PADDING + yStart) {
				g2.drawLine(x0, y0, x1, y0);
				drawCenterString(g2, String.format("%.1f", Y_HATCH_INTERVAL * i), originX, y0);
			}
		}

		// and for x axis
		for (int i = 0; i < 24; i++) {
			int x0 = scalePoint(minX + (i*60*60*1000), 0, xEnd - xStart, yEnd - yStart).x + xStart;
//			int xText = x0
//					+ (x0 - (scalePoint(minX + i - 1, 0, xEnd - xStart, yEnd - yStart).x + xStart))
//					/ 2;
			int y0 = originY + HATCH_LENGTH;
			int y1 = originY - HATCH_LENGTH;

			g2.drawLine(x0, y0, x0, y1);
			drawCenterString(g2, ""+i, x0, originY);
		}

		g2.setColor(Visualizer.colors.get(name).color);
		graphPoints = dataToPoints(data.get(this.currentDay), xEnd - xStart, yEnd - yStart);
		drawPoints(graphPoints, g2, xStart, yStart);
	}

	private void drawCenterString(Graphics2D g2, String str, int x, int y) {
		FontMetrics fm = g2.getFontMetrics();
		int textWidth = fm.stringWidth(str);
		int textHeight = fm.getHeight() + fm.getAscent();
		g2.drawString(str, x - textWidth / 2, y + TEXT_OFFSET + textHeight / 2);
	}

	private ArrayList<Point> dataToPoints(Day data, int width, int height) {

		if (data.data.size() == 0) {
			return new ArrayList<>();
		}

		ArrayList<Point> points = new ArrayList<>();

		for(DataPoint dp : data.data) {
			if(dp.value != null) {
				points.add(this.scalePoint(dp.date, dp.value, width, height));
				System.out.println(height);
			}
		}
		return points;
	}

	private void drawPoints(ArrayList<Point> points, Graphics2D g2, int xOffset, int yOffset) {
		for (Point point : points) {
			int x = point.x + xOffset;
			int y = point.y + yOffset;

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
		JFrame frame = new JFrame(Utils.TITLE);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(this);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

	// Returns a bufferedimage containing the graphs
	private BufferedImage drawImage() {
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(BACKGROUND_COLOR);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		this.paintComponent(g);
		return img;
	}
	
	public BufferedImage getImages() {
		this.currentDay = 0;
		BufferedImage img = this.drawImage();
		System.out.println(new Date(Visualizer.data.get(this.currentDay).date) + " drawn!");
		
		for(int i = 1; i < Visualizer.data.size(); i++) {
			this.currentDay = i;
			img = this.drawOnImage(img);
			System.out.println(new Date(Visualizer.data.get(this.currentDay).date) + " drawn!");
		}
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