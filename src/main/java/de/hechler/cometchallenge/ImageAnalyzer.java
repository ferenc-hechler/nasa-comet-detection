package de.hechler.cometchallenge;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ImageAnalyzer {

	private final static Logger logger = Logger.getLogger(ImageAnalyzer.class.getName());
	
	private Path path;

	private int[][] matrix;
	private int width;
	private int height;
	
	
	
	public ImageAnalyzer(Path path, int[][] matrix) {
		this.path = path;
		this.matrix = matrix;
		this.height = matrix.length;
		this.width = matrix[0].length;
	}

	public Path getPath() {
		return path;
	}
	
	public String getFilename() {
		return path==null?"?":path.getFileName().toString();
	}
	
	public int get(int x, int y) {
		if (isInRange(x, y)) {
			return matrix[y][x];
		}
		int borderX = Math.max(0, Math.min(width-1, x));
		int borderY = Math.max(0, Math.min(height-1, y));
		return matrix[borderY][borderX];
	}

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

	public MinMaxCounter countRange(int fromX, int fromY, int toX, int toY) {
		MinMaxCounter result = new MinMaxCounter();
		for (int y=fromY; y<=toY; y++) {
			for (int x=fromX; x<=toX; x++) {
				result.update(get(x, y));
			}
		}
		return result;
	}


	public boolean checkCentrum(int x, int y) {
		int p = get(x,y);
		MinMaxCounter d1 = countNeighbours(x, y, 1);
		if (d1.getAvg()>p) {
			return false;
		}
		MinMaxCounter d2 = countNeighbours(x, y, 2);
		if ((d2.getMin() >= d1.getMin()) || 
			(d2.getMax() >= d1.getMax()) ||  
			(d2.getAvg() >= d1.getAvg())) {
			return false;
		}
		MinMaxCounter d3 = countNeighbours(x, y, 3);
		if ((d3.getMin() >= d2.getMin()) || 
			(d3.getMax() >= d2.getMax()) ||  
			(d3.getAvg() >= d2.getAvg())) {
			return false;
		}
		MinMaxCounter d4 = countNeighbours(x, y, 4);
		if ((d4.getMin() >= d3.getMin()) || 
			(d4.getMax() >= d3.getMax()) ||  
			(d4.getAvg() >= d3.getAvg())) {
			return false;
		}

		if (d4.getMax() > d1.getMin()) {
			return false;
		}
		
		logger.fine("("+x+","+y+")");
		logger.fine("P="+p);

		logger.fine("D1="+d1);
		logger.fine("D2="+d2);
		logger.fine("D3="+d3);
		logger.fine("D4="+d4);

		return true;
	}

	public List<Pos> findCenters() {
		List<Pos> result = new ArrayList<>();
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				if (checkCentrum(x, y)) {
					result.add(new Pos(x, y));
				}
			}
		}
		return result;
	}

	public void sub(int n) {
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				matrix[y][x] -= n;
			}
		}
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
				gray[0] = (int) (scale*(get(x,y)-offset));
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

}
