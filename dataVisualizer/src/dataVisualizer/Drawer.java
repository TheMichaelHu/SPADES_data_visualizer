package dataVisualizer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

class Drawer extends JPanel {
	private static final long serialVersionUID = 1L;

	// Preferences
	private final int WIDTH = Utils.WIDTH;
	private final int HEIGHT = Utils.HEIGHT;
	private final int LEGEND_WIDTH = WIDTH/3;

	// Constants
	private final int PADDING = 30;
	private final int GRAPH_POINT_WIDTH = 4;
	private final Color BACKGROUND_COLOR = new Color(255, 255, 255);
	private final Color LINE_COLOR = new Color(0, 0, 0);

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
		int start = PADDING;
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
		int hatchLength = 5;
		double yHatchInterval = .5;
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
		for (int i = 0; i < Utils.MAX_Y/yHatchInterval; i++) {
			int x0 = originX - hatchLength;
			int x1 = originX + hatchLength;
			int y0 = scalePoint(0, i * yHatchInterval, xRange, yRange).y + yStart;
			if (y0 > PADDING + yStart) {
				g2.drawLine(x0, y0, x1, y0);
				drawCenterString(g2, String.format("%.1f", yHatchInterval * i), originX, y0);
			}
		}

		// and for x axis
		for (int i = 0; i < 24; i++) {
			int x0 = scalePoint(minX + (i*60*60*1000), 0, xRange, yRange).x + xStart;
			int y0 = originY + hatchLength;
			int y1 = originY - hatchLength;

			g2.drawLine(x0, y0, x0, y1);
			drawCenterString(g2, ""+i, x0, originY);
		}

		// draw data
		long currentTime = Visualizer.data.get(this.currentDay).date;

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
			if(currentTime < event.fromDate && event.fromDate < currentTime + 24 * 60 * 60 *1000) {
				int x = this.scalePoint(event.fromDate, 0, xRange, yRange).x;
				g2.setColor(Visualizer.colors.get("call").color);
				g2.drawLine(x + xStart, originY, x + xStart, PADDING + yStart);
			}
		}

		for(Event event : Visualizer.texts) {
			if(currentTime < event.fromDate && event.fromDate < currentTime + 24 * 60 * 60 *1000) {
				int x = this.scalePoint(event.fromDate, 0, xRange, yRange).x;
				g2.setColor(Visualizer.colors.get("text").color);
				g2.drawLine(x + xStart, originY, x + xStart, PADDING + yStart);
			}
		}

		HashMap<String, Long> recentAnnotations = new HashMap<>();
		for(Event annotation : Visualizer.annotations) {
			if(!Arrays.asList(Utils.IGNORED_ANNOTATIONS).contains(annotation.text.toLowerCase().trim())) {
				if(currentTime < annotation.fromDate && annotation.fromDate < currentTime + 24 * 60 * 60 *1000) {
					int x = this.scalePoint(annotation.fromDate, 0, xRange, yRange).x;
					g2.setColor(LINE_COLOR);
					if(recentAnnotations.get(annotation.text) == null ||
							annotation.fromDate - recentAnnotations.get(annotation.text) >= 60 * 60 * 1000) {
						drawCenterString(g2, annotation.text, x + xStart, annotation.getLabelHeight() + yStart);
						recentAnnotations.put(annotation.text, annotation.fromDate);
					}
					g2.setColor(Visualizer.colors.get("annotation").color);
					g2.fillOval(x + xStart, annotation.getLabelHeight() + yStart,
							GRAPH_POINT_WIDTH, GRAPH_POINT_WIDTH - 2);
				}
			}
		}

		for(Prompt prompt : Visualizer.prompts) {
			if(currentTime < prompt.date && prompt.date < currentTime + 24 * 60 * 60 *1000) {
				int x = this.scalePoint(prompt.date, 0, xRange, yRange).x + xStart;
				if (prompt.completed) {
					g2.setColor(Visualizer.colors.get("answered prompt").color);
					drawCenterString(g2, prompt.posture, x, PADDING + yStart);
				} else {
					g2.setColor(Visualizer.colors.get("ignored prompt").color);
					drawCenterString(g2, prompt.activity, x, PADDING + yStart);
				}
				g2.drawLine(x, originY, x, PADDING + yStart);
			}
		}

		count = 0;
		for(Event session : Visualizer.sessions) {
			int fromX = this.scalePoint(session.fromDate, 0, xRange, yRange).x + xStart;
			int toX = this.scalePoint(session.toDate, 0, xRange, yRange).x + xStart;

			if(session.fromDate < currentTime) {
				fromX = this.scalePoint(Visualizer.data.get(this.currentDay).date, 0, xRange, yRange).x + xStart;
			}
			if(session.toDate > currentTime + 24 * 60 * 60 * 1000) {
				toX = this.scalePoint(Visualizer.data.get(this.currentDay).date + 24 * 60 * 60 * 1000, 0,
						xRange, yRange).x + xStart;
			}

			if(fromX < toX) {
				Polygon p = new Polygon();
				p.addPoint(fromX, PADDING + yStart);
				p.addPoint(toX, PADDING + yStart);
				p.addPoint(toX, originY);
				p.addPoint(fromX, originY);

				g2.setColor(giveColor(session.text, (count++) + 51, .5, LegendType.SESSION));
				g2.fill(p);
			}
		}
	}

	private void drawLinePlot(ArrayList<Point> points, Graphics2D g2, int xOffset, int yOffset, Color color) {
		g2.setColor(color);
		for (int i = 1; i < points.size(); i++) {
			Point point0 = points.get(i-1);
			Point point1 = points.get(i);

			int scaledChunk = scalePoint(Visualizer.data.get(this.currentDay).date +
					(long)(Utils.CHUNK * 60 * 60 * 1000), 0, (WIDTH - 2 * PADDING), HEIGHT).x -
					scalePoint(Visualizer.data.get(this.currentDay).date, 0, (WIDTH - 2 * PADDING), HEIGHT).x;

			// chunk is in hours, who designed this?
			if(point1.x - point0.x <= scaledChunk) {
				g2.setStroke(new BasicStroke(GRAPH_POINT_WIDTH));
				g2.drawLine(point0.x + xOffset, point0.y + yOffset,
						point1.x + xOffset, point1.y + yOffset);
			}
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
		int textOffset = 5;
		FontMetrics fm = g2.getFontMetrics();
		int textWidth = fm.stringWidth(str);
		int textHeight = fm.getHeight() + fm.getAscent();
		g2.drawString(str, x - textWidth / 2, y + textOffset + textHeight / 2);
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

	// Returns a bufferedimage containing the graphs
	private BufferedImage drawImage() {
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();
		g2.setBackground(BACKGROUND_COLOR);
		g2.clearRect(0, 0, WIDTH, HEIGHT);
		this.paintComponent(g2);
		return img;
	}

	private BufferedImage drawSpacing(int num) {
		BufferedImage img = new BufferedImage((WIDTH * num) + 1, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();
		g2.setBackground(BACKGROUND_COLOR);
		g2.clearRect(0, 0, (WIDTH * num) + 1, HEIGHT);
		return img;
	}

	private BufferedImage drawLegend() {
		int titleFontSize = 30;
		int normalFontSize = 20;
		Font titleFont = new Font("sansserif", Font.BOLD, titleFontSize);
		Font normalFont = new Font("sansserif", Font.PLAIN, normalFontSize);

		int yStart = 10;
		int padding = 30 + normalFontSize;
		int legendHeight = 0;
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();
		g2.setBackground(BACKGROUND_COLOR);
		g2.clearRect(0, 0, WIDTH, HEIGHT);
		g2.setColor(LINE_COLOR);
		g2.setFont(titleFont);

		drawCenterString(g2, Utils.TITLE, WIDTH/2, yStart);
		for(int i = 0; i < LegendType.values().length; i++) {
			int count = 2;
			int x = (WIDTH - LEGEND_WIDTH)/2 + i * LEGEND_WIDTH/LegendType.values().length
					+ LEGEND_WIDTH/LegendType.values().length/2;
			g2.setFont(normalFont.deriveFont(Font.BOLD));
			drawCenterString(g2, LegendType.values()[i].name(), x, yStart + count++ * padding);

			g2.setFont(normalFont);
			for(String name : Visualizer.colors.keySet()) {
				if(Visualizer.colors.get(name).type == LegendType.values()[i]) {
					g2.setColor(Visualizer.colors.get(name).color);
					g2.fillOval(x - LEGEND_WIDTH/LegendType.values().length/4 -20, yStart + count * padding - 6, 6, 6);
					g2.setColor(LINE_COLOR);
					g2.drawString(Visualizer.colors.get(name).name, x - LEGEND_WIDTH/LegendType.values().length/4,
							yStart + count++ * padding);
				}
			}

			legendHeight = Math.max(legendHeight, count * padding);
		}

		return img.getSubimage(0,0,WIDTH, yStart + legendHeight);
	}

	BufferedImage exportImage(String fileName) throws IOException {
		this.currentDay = 0;
		this.calcRange();
		BufferedImage img = this.drawImage();
		System.out.println(new Date(Visualizer.data.get(this.currentDay).date) + " drawn!");
		
		for(int i = 1; i < Visualizer.data.size(); i++) {
			this.currentDay = i;
			this.calcRange();
			img = this.drawBelowImage(img, this.drawImage());
			System.out.println(new Date(Visualizer.data.get(this.currentDay).date) + " drawn!");
		}
		ImageIO.write(img, Utils.IMAGE_EXT, new File(fileName));
		return img;
	}

	private void exportImagesByWeek() throws IOException {
		int weekCount = 1;
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(Visualizer.data.get(0).date));
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		BufferedImage img = this.drawSpacing(dayOfWeek-1);

		for(int i = 0; i < Visualizer.data.size(); i++) {
			this.currentDay = i;
			this.calcRange();
			img = this.drawRightOfImage(img, this.drawImage());
			System.out.println(new Date(Visualizer.data.get(this.currentDay).date) + " drawn!");

			cal.setTime(new Date(Visualizer.data.get(i).date));
			dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			if(dayOfWeek == 7) {
				String fileName = String.format("%s%s_week_%d.%s",
						Utils.TARGET_DIR, Utils.TITLE, weekCount++, Utils.IMAGE_EXT);
				ImageIO.write(img, Utils.IMAGE_EXT, new File(fileName));
				img = this.drawSpacing(0);
			}
		}

		img = this.drawRightOfImage(img, this.drawSpacing(7 - dayOfWeek));
		String fileName = String.format("%s%s_week_%d.%s",
				Utils.TARGET_DIR, Utils.TITLE, weekCount, Utils.IMAGE_EXT);
		ImageIO.write(img, Utils.IMAGE_EXT, new File(fileName));
	}

	// Returns a bufferedimage containing the graphs
	private BufferedImage drawBelowImage(BufferedImage background, BufferedImage img) {
		return drawBelowImage(background, img, 0, 0);
	}

	// Returns a bufferedimage containing the graphs
	private BufferedImage drawBelowImage(BufferedImage background, BufferedImage img, int xOffset1, int xOffset2) {
		if (background == null) {
			return img;
		}

		int w = Math.max(background.getWidth(), img.getWidth());
		int h = background.getHeight() + img.getHeight();
		BufferedImage combined = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = combined.createGraphics();
		g2.setBackground(BACKGROUND_COLOR);
		g2.clearRect(0, 0, w, h);
		g2.drawImage(background, xOffset1, 0, null);
		g2.drawImage(img, xOffset2, background.getHeight(), null);

		return combined;
	}

	private BufferedImage drawRightOfImage(BufferedImage background, BufferedImage img) {
		if (background == null) {
			return img;
		}

		int w = background.getWidth() + img.getWidth();
		int h = Math.max(background.getHeight(), img.getHeight());
		BufferedImage combined = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = combined.createGraphics();
		g2.setBackground(BACKGROUND_COLOR);
		g2.clearRect(0, 0, w, h);
		g2.drawImage(background, 0, 0, null);
		g2.drawImage(img, background.getWidth(), 0, null);

		return combined;
	}

	boolean exportFullPlot(ArrayList<String> paths, String fileName) throws IOException {
		BufferedImage img = null;
		switch(Utils.CHART_TYPE) {
			case byDay:
				img = drawLegend();
				for (String path : paths) {
					BufferedImage newImg = ImageIO.read(new File(path));
					img = drawBelowImage(img, newImg);
				}
				break;

			case byWeek:
				File chart;
				int weekCount = 1;
				exportImagesByWeek();
				while((chart = new File(String.format("%s%s_week_%s.%s",
					Utils.TARGET_DIR, Utils.TITLE, weekCount++, Utils.IMAGE_EXT))).exists()) {
					BufferedImage newImg = ImageIO.read(chart);
					img = drawBelowImage(img, newImg);
				}
				img = drawBelowImage(drawLegend(), img, (int)(WIDTH*3.5 - .5 * LEGEND_WIDTH), 0);
				break;

			default:
				return false;
		}
		try {
			ImageIO.write(img, Utils.IMAGE_EXT, new File(fileName));

			if(Utils.CHART_TYPE == Utils.ChartType.byWeek) {
				File chart;
				int weekCount = 1;
				while ((chart = new File(String.format("%s%s_week_%s.%s",
						Utils.TARGET_DIR, Utils.TITLE, weekCount++, Utils.IMAGE_EXT))).exists()) {
					chart.delete();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}