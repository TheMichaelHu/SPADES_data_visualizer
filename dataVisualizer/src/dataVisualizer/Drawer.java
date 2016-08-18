package dataVisualizer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.*;

class Drawer extends JPanel {
	private static final long serialVersionUID = 1L;

	// Preferences
	private static final int WIDTH = Utils.WIDTH;
	private static final int HEIGHT = Utils.HEIGHT;

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
	private int originY;

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

		drawPlot(g2, start, end, 10, HEIGHT);

		g2.setColor(LINE_COLOR);
		drawCenterString(g2, new SimpleDateFormat("MM-dd-yyyy").format(
				new Date(Visualizer.data.get(this.currentDay).date)), (start + end)/2, 0);
	}

	private void drawPlot(Graphics2D g2, int xStart, int xEnd, int yStart, int yEnd) {
		int xRange = xEnd - xStart;
		int yRange = yEnd - yStart;
		int originX = xStart + PADDING;
		originY = this.scalePoint(0, 0, xRange, yRange).y + yStart;
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
			int y0 = scalePoint(0, i * Y_HATCH_INTERVAL, xRange, yRange).y + yStart;
			if (y0 > PADDING + yStart) {
				g2.drawLine(x0, y0, x1, y0);
				drawCenterString(g2, String.format("%.1f", Y_HATCH_INTERVAL * i), originX, y0);
			}
		}

		// and for x axis
		for (int i = 0; i < 24; i++) {
			int x0 = scalePoint(minX + (i*60*60*1000), 0, xRange, yRange).x + xStart;
			int y0 = originY + HATCH_LENGTH;
			int y1 = originY - HATCH_LENGTH;

			g2.drawLine(x0, y0, x0, y1);
			drawCenterString(g2, ""+i, x0, originY);
		}

		// draw data
		graphPoints = dataToPoints(scaleBatteryData(Visualizer.battery.get(this.currentDay)), xRange, yRange);
		drawArea(graphPoints, g2, xStart, yStart, Visualizer.colors.get("phone battery").color);

		graphPoints = dataToPoints(scaleBatteryData(Visualizer.batteryW.get(this.currentDay)), xRange, yRange);
		drawArea(graphPoints, g2, xStart, yStart, Visualizer.colors.get("watch battery").color);

		graphPoints = dataToPoints(Visualizer.data.get(this.currentDay), xRange, yRange);
		drawLinePlot(graphPoints, g2, xStart, yStart, Visualizer.colors.get("phone data").color);

		graphPoints = dataToPoints(Visualizer.dataW.get(this.currentDay), xRange, yRange);
		drawLinePlot(graphPoints, g2, xStart, yStart,  Visualizer.colors.get("watch data").color);

		int count = 0;
		for(String sensorId : Visualizer.sensorLocations.keySet()) {
			graphPoints = dataToPoints(Visualizer.sensorData.get(sensorId).get(this.currentDay), xRange, yRange);
			drawLinePlot(graphPoints, g2, xStart, yStart, giveColor(sensorId, ++count, .9, LegendType.DATA));
		}

		Stroke dashed = new BasicStroke(GRAPH_POINT_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
				0, new float[]{9}, 0);
		g2.setStroke(dashed);

		for(Event event : Visualizer.calls) {
			int x = this.scalePoint(event.fromDate, 0, xRange, yRange).x;
			g2.setColor( Visualizer.colors.get("call").color);
			g2.drawLine(x + xStart, originY, x + xStart, PADDING + yStart);
		}

		for(Event event : Visualizer.texts) {
			int x = this.scalePoint(event.fromDate, 0, xRange, yRange).x;
			g2.setColor( Visualizer.colors.get("text").color);
			g2.drawLine(x + xStart, originY, x + xStart, PADDING + yStart);
		}

		for(Event annotation : Visualizer.annotations) {
			if(!Arrays.asList(Utils.IGNORED_ANNOTATIONS).contains(annotation.text.toLowerCase().trim())) {
				int x = this.scalePoint(annotation.fromDate, 0, xRange, yRange).x;
				g2.setColor(Color.gray);
				drawCenterString(g2, annotation.text, x + xStart + 3, annotation.getLabelHeight() + yStart + 3);
				g2.setColor(LINE_COLOR);
				drawCenterString(g2, annotation.text, x + xStart, annotation.getLabelHeight() + yStart);
			}
		}

		for(Prompt prompt : Visualizer.prompts) {
			int x = this.scalePoint(prompt.date, 0, xRange, yRange).x;
			if(prompt.completed) {
				g2.setColor(Visualizer.colors.get("answered prompt").color);
				drawCenterString(g2, prompt.posture, x + xStart, PADDING + yStart);
			} else {
				g2.setColor(Visualizer.colors.get("ignored prompt").color);
				drawCenterString(g2, prompt.activity, x + xStart, PADDING + yStart);
			}
			g2.drawLine(x + xStart, originY, x + xStart, PADDING + yStart);
		}
	}

	private void drawLinePlot(ArrayList<Point> points, Graphics2D g2, int xOffset, int yOffset, Color color) {
		g2.setColor(color);
		for (int i = 1; i < points.size(); i++) {
			Point point0 = points.get(i-1);
			Point point1 = points.get(i);
			g2.setStroke(new BasicStroke(GRAPH_POINT_WIDTH));
			g2.drawLine(point0.x + xOffset, point0.y + yOffset,
					point1.x + xOffset, point1.y + yOffset);
		}
	}

	private void drawArea(ArrayList<Point> points, Graphics2D g2, int xOffset, int yOffset, Color color) {
		g2.setColor(color);
		for (int i = 1; i < points.size(); i++) {
			Point point0 = points.get(i-1);
			Point point1 = points.get(i);

			Polygon p = new Polygon();
			p.addPoint(point0.x + xOffset, point0.y + yOffset);
			p.addPoint(point1.x + xOffset, point1.y + yOffset);
			p.addPoint(point1.x + xOffset, originY);
			p.addPoint(point0.x + xOffset, originY);

			g2.setColor(color);
			g2.fill(p);
		}
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
			}
		}
		return points;
	}

	private Day scaleBatteryData(Day data) {
		Day scaledDay = new Day();
		scaledDay.date = data.date;
		for(DataPoint dp: data.data) {
			if(dp.value != null) {
				DataPoint scaledPoint = new DataPoint(dp.date, dp.value * (Utils.MAX_Y - Utils.MIN_Y) / 100);
				scaledDay.data.add(scaledPoint);
			}
		}
		return scaledDay;
	}

	private Color giveColor(String name, int seed, double opacity, LegendType type) {
		if(!Visualizer.colors.containsKey(name)) {
			Color randColor = new Color(seed * 947 % 255, seed * 1013 % 255, seed * 1913 % 255, (int)(opacity * 255));
			Visualizer.colors.put(name, new LegendItem(name, randColor, type));
		}
		return Visualizer.colors.get(name).color;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
	}

	// Creates a JFrame and draws the graph on it
	void drawInFrame() {
		this.setBackground(BACKGROUND_COLOR);
		JFrame frame = new JFrame(Utils.TITLE);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
	
	BufferedImage getImages() throws IOException {
		this.currentDay = 0;
		this.calcRange();
		BufferedImage img = this.drawImage();

		ImageIO.write(img, "PNG", new File(Utils.TARGET_DIR + Utils.TITLE + ".png"));

		System.out.println(new Date(Visualizer.data.get(this.currentDay).date) + " drawn!");
		
		for(int i = 1; i < Visualizer.data.size(); i++) {
			this.currentDay = i;
			this.calcRange();

			if(i % 10 == 0) {
				ImageIO.write(img, "PNG", new File(Utils.TARGET_DIR + Utils.TITLE + ".png"));
				img = this.drawOnImage();
			} else {
				img = this.drawOnImage(img);
			}

			System.out.println(new Date(Visualizer.data.get(this.currentDay).date) + " drawn!");
		}
		return img;
	}

	private BufferedImage drawOnImage() throws IOException {
		return drawOnImage(ImageIO.read(new File(Utils.TARGET_DIR + Utils.TITLE + ".png")));
	}

	// Returns a bufferedimage containing the graphs
	private BufferedImage drawOnImage(BufferedImage background) {
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