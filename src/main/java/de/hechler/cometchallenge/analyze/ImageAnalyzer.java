package de.hechler.cometchallenge.analyze;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import de.hechler.cometchallenge.MinMaxCounter;
import de.hechler.cometchallenge.geometry.Pos;

public class ImageAnalyzer {

	private Path path;
	private Date timestamp;
	private Pos labeledCometPos;
	private Double vmag;
	
	private int[][] matrix;
	private int width;
	private int height;
	
	private Properties fitsProperties;
	private double expTime;   // exposure time, normalize by dividing all pixel values by expTime. 
	
	// calculated data
	private List<Pos> spots;
	
	
	
	public ImageAnalyzer(Path path, Date timestamp, Pos labeledCometPos, Double vmag, int[][] matrix, Properties fitsProperties) {
		this.path = path;
		this.timestamp = timestamp;
		this.labeledCometPos = labeledCometPos;
		this.vmag = vmag;
		this.matrix = matrix;
		this.height = matrix.length;
		this.width = matrix[0].length;
		this.fitsProperties = fitsProperties;
		this.expTime = Double.parseDouble(((String)fitsProperties.get("Info")).replace("\r", " ").replace("\n", " ").replaceFirst(".*EXPTIME\\s*=([^/]*)/.*", "$1").trim());
	}

	public Path getPath() { return path; }
	public String getFilename() { return path==null?"?":path.getFileName().toString(); }
	public Date getTimestamp() { return timestamp; }
	public Pos getLabeledCometPos() { return labeledCometPos; }
	public double getVmag() { return vmag; }
	public int[][] getMatrix() { return matrix; }
	public String getProperty(String key) { return fitsProperties.getProperty(key); }
	public double getExpTime() { return expTime; }
	public void setSpots(List<Pos> spots) { this.spots = spots; }
	public List<Pos> getSpots() { return spots; }

	
	public int get(int x, int y) {
		if (isInRange(x, y)) {
			return matrix[y][x];
		}
		int borderX = Math.max(0, Math.min(width-1, x));
		int borderY = Math.max(0, Math.min(height-1, y));
		return matrix[borderY][borderX];
	}

	/**
	 * count neighbours on surroundg square with distance dist from the center. 
	 * @param x
	 * @param y
	 * @param dist
	 * @return
	 */
	public MinMaxCounter countNeighbours(int x, int y, int dist) {
		MinMaxCounter result = new MinMaxCounter();
		for (int n=-dist; n<=dist; n++) {
			result.update(get(x+n, y-dist));
			result.update(get(x+n, y+dist));
		}
		for (int n=-dist+1; n<dist; n++) {
			result.update(get(x-dist, y+n));
			result.update(get(x+dist, y+n));
		}
		return result;
	}

	/**
	 * count every pixel in range (including fromXY, toXy).
	 * @param fromX
	 * @param fromY
	 * @param toX
	 * @param toY
	 * @return
	 */
	public MinMaxCounter countRange(int fromX, int fromY, int toX, int toY) {
		MinMaxCounter result = new MinMaxCounter();
		for (int y=fromY; y<=toY; y++) {
			for (int x=fromX; x<=toX; x++) {
				result.update(get(x, y));
			}
		}
		return result;
	}

	public BufferedImage createBufferedImage(int fromX, int fromY, int toX, int toY) {
		MinMaxCounter range = calcMinMax(fromX, fromY, toX, toY);
		return createBufferedImage(range, fromX, fromY, toX, toY);
	}
	
	public BufferedImage createBufferedImage(MinMaxCounter range, int fromX, int fromY, int toX, int toY) {
		int offset = range.getMin(); 
		double scale = 255.0/(range.getMax() - range.getMin() + 0.0001);
		int[] gray = new int[1];
		BufferedImage result = new BufferedImage(toX-fromX+1, toY-fromY+1, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = result.getRaster();
		for (int y=fromY; y<=toY; y++) {
			for (int x=fromX; x<=toX; x++) {
				gray[0] = Math.max(0, Math.min(255, (int) (scale*(get(x,y)-offset))));
				raster.setPixel(x-fromX, y-fromY, gray);
			}
		}
		return result;
	}

	private boolean isInRange(int x, int y) {
		return (x>=0) && (x<width) & (y>=0) && (y<height);
	}

	public MinMaxCounter calcMinMax(int fromX, int fromY, int toX, int toY) {
		MinMaxCounter result = new MinMaxCounter();
		for (int y=fromY; y<=toY; y++) {
			for (int x=fromX; x<=toX; x++) {
				result.update(get(x,y));
			}
		}
		return result;
	}

	public double checkComet(int x, int y) {
		MinMaxCounter center1MinMax = calcMinMax(x-1,y-1, x+1,y+1);
		MinMaxCounter outer4MinMax = countNeighbours(x, y,  4);
//		logger.info("CENTER1: "+center1MinMax);
//		logger.info("OUTER4: "+outer4MinMax);
		double centerDist = center1MinMax.getAvg()-outer4MinMax.getMin();
		double outerDist = outer4MinMax.getAvg()-outer4MinMax.getMin();
		double factorCenter2Outer = outerDist==0.0?centerDist : centerDist/outerDist;
		return factorCenter2Outer;
	}

}
