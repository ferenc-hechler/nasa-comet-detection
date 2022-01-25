package de.hechler.cometchallenge.analyze;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import de.hechler.cometchallenge.CometPath;
import de.hechler.cometchallenge.geometry.Pos;
import de.hechler.cometchallenge.utils.Utils;
import ij.ImagePlus;
import ij.plugin.FITS_Reader;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class SequenceAnalyzer {

	private static final Logger logger = Logger.getLogger(SequenceAnalyzer.class.getName());

	private List<ImageAnalyzer> images;
	private Map<Date, ImageAnalyzer> timestamp2imageMap;
	
	private static final String COMETPOS_RX = "^\\s*([-0-9]+\\s+[0-9:]+)\\s+([0-9]+[.]fts)\\s+([0-9.]+)\\s+([0-9.]+)\\s+([0-9.an]+)\\s*$";
	private static final String TIMESTAMP_FORMAT = "YYYY-MM-dd HH:mm:ss";
	
	public void readSequence(Path folder) {
		
        // List<Path> files = Files.list(folder).filter(p -> p.getFileName().toString().endsWith(".fts"))
        //         .collect(Collectors.toList());
	
		images = new ArrayList<>();
		timestamp2imageMap = new HashMap<>();
		String labeledDataFilename = folder.getFileName().toString()+".txt";
		List<String> sequenceInfo = Utils.readAllLines(folder.resolve(labeledDataFilename));
		for (String line:sequenceInfo) {
			if (line.trim().startsWith("#") || line.trim().isEmpty()) {
				continue;
			}
			if (!line.matches(COMETPOS_RX)) {
				throw new RuntimeException("invalid line '"+line+"'");
			}
			String time = line.replaceFirst(COMETPOS_RX, "$1");
			Date timestamp = parseTimestamp(time); 
			String filename = line.replaceFirst(COMETPOS_RX, "$2");
			Path path = folder.resolve(filename);
			double xPos = Double.parseDouble(line.replaceFirst(COMETPOS_RX, "$3"));
			double yPos = Double.parseDouble(line.replaceFirst(COMETPOS_RX, "$4"));
			String vMagText = line.replaceFirst(COMETPOS_RX, "$5");
			Double vMag = vMagText.equals("nan") ? null : Double.parseDouble(vMagText);
			
			int[][] matrix = readRawGrayscale(path);
			
			ImageAnalyzer ia = new ImageAnalyzer(path, timestamp, new Pos(xPos, yPos), vMag, matrix);
			timestamp2imageMap.put(ia.getTimestamp(), ia);
			images.add(ia);
		}
	}

	public ImageAnalyzer getImageAnalyzerForTimestamp(Date timestamp) {
		return timestamp2imageMap.get(timestamp);
	}
	
	public ImageAnalyzer getPreviousImageAnalyzer(ImageAnalyzer ia) {
		int idx = images.indexOf(ia);
		if (idx==0) {
			return null;
		}
		return images.get(idx-1);
	}
	
	public ImageAnalyzer getNextImageAnalyzer(ImageAnalyzer ia) {
		int idx = images.indexOf(ia);
		if (idx==images.size()-1) {
			return null;
		}
		return images.get(idx+1);
	}
	
	public void detectCometSpots() {
		ImageAnalyzer lastImage = null;
		ImageAnalyzer thisImage = null;
		ImageAnalyzer nextImage = null;
		// edge case first image: use next image as previous image
		lastImage = images.get(1);
		thisImage = images.get(0);
		
		for (int i=0; i<images.size(); i++) {
			if (i==images.size()-1) {
				// edge case last image: use previous image as next image
				nextImage = lastImage;
			}
			else {
				nextImage = images.get(i+1);
			}
			
			List<Pos> spots = searchAllComets(lastImage, thisImage, nextImage);
			spots = concentrate(spots, 2);
			thisImage.setSpots(spots);
			String csvFilename = thisImage.getPath().toString().replace(".fts", "")+"-spots.csv";
			writeCSV(csvFilename, spots);
			
			lastImage = thisImage;
			thisImage = nextImage;
		}
	}
	
	public List<CometPath> detectCometPaths() {
		
		List<CometPath> cometPaths = new ArrayList<>();
		List<CometPath> finishedCometPaths = new ArrayList<>();
		
		for (ImageAnalyzer image:images) {
			List<Pos> spots = image.getSpots();
			Date timestamp = image.getTimestamp();
			
			List<CometPath> lastCometPaths = cometPaths;
			cometPaths = new ArrayList<>();
			for (Pos spot:spots) {
				cometPaths.add(new CometPath(timestamp, spot));
			}
			for (CometPath lastCometPath:lastCometPaths) {
				boolean found = false;
				for (Pos spot:spots) {
					CometPath next = lastCometPath.createNewIfInRange(timestamp, spot);
					if (next != null) {
						found = true;
						cometPaths.add(next);
					}
				}
				if (!found) {
					if (lastCometPath.getLength()>=5) {
						finishedCometPaths.add(lastCometPath);
					}
				}
			}
		}
		cometPaths.forEach(cp -> {
			if (cp.getLength()>=5) {
				finishedCometPaths.add(cp);
			}
		});
		return finishedCometPaths;
	}


	private Date parseTimestamp(String time) {
		try {
			return new SimpleDateFormat(TIMESTAMP_FORMAT).parse(time);
		} catch (ParseException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private BufferedImage createSpots(String filename, List<Pos> spots) {
		int[] gray = new int[1];
		int scale = 2;
		int imgWH = 1024/scale;
		BufferedImage image = new BufferedImage(imgWH, imgWH, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image.getRaster();
		gray[0] = 0;
		for (int y=0; y<imgWH; y++) {
			for (int x=0; x<imgWH; x++) {
				raster.setPixel(x, y, gray);
			}
		}
		gray[0] = 255;
		for (Pos spot:spots) {
			raster.setPixel((int)(spot.getX()/scale), (int)(spot.getY()/scale), gray);
		}
		return image;
	}

	private List<Pos> concentrate(List<Pos> centers, int mhDist) {
		Set<Pos> centersPos = new LinkedHashSet<>(centers);
		final Map<Pos, Set<Pos>> clusters = new LinkedHashMap<>();
		centersPos.forEach(p -> {
			Set<Pos> sp = new HashSet<>();
			sp.add(p);
			clusters.put(p, sp);
		});	
		while (!centersPos.isEmpty()) {
			Pos centerPos = centersPos.iterator().next();
			centersPos.remove(centerPos);
			Iterator<Pos> otherCenterIter = centersPos.iterator();
			while (otherCenterIter.hasNext()) {
				Pos otherCenterPos = otherCenterIter.next();
				if (centerPos.getManhattenDist(otherCenterPos) <= 2) {
					final Set<Pos> sp1 = clusters.get(centerPos);
					Set<Pos> sp2 = clusters.get(otherCenterPos);
					sp1.addAll(sp2);
					sp2.forEach(p -> clusters.put(p, sp1));
				}
			}
		}
		Set<Pos> result = new LinkedHashSet<>();
		clusters.values().forEach(sp -> result.add(calcAveragePos(sp)));
		return new ArrayList<>(result);
	}

	private Pos calcAveragePos(Set<Pos> posList) {
		if (posList.size() == 1) {
			return posList.iterator().next();
		}
		final Pos sum = new Pos(0,0);
		posList.forEach(p -> sum.add(p));
		int cnt = posList.size(); 
		return new Pos((sum.getX()+cnt/2)/cnt,(sum.getY()+cnt/2)/cnt);
	}

	private int[][] readRawGrayscale(Path path) {
		logger.info("reading "+path.getFileName().toString());
		FITS_Reader reader = new FITS_Reader();
		reader.run(path.toString());
		ImagePlus imp = (ImagePlus)reader;
		ImageProcessor imgProc = imp.getProcessor();
		ShortProcessor shortProc = (ShortProcessor)imgProc.convertToShort(false);
		BufferedImage gray16img = shortProc.get16BitBufferedImage();
		Raster raster = gray16img.getData();
		DataBuffer dataBuffer = raster.getDataBuffer();
		int w = imp.getWidth();
		int h = imp.getHeight();
		int[][] matrix = new int[h][w];
		for (int y = 0; y<h; y++) {
			for (int x = 0; x<w; x++) {
				matrix[h-1-y][x] = dataBuffer.getElem(h*y+x);
			}
		}
		return matrix;
	}


	private void writeCSV(String csvFilename, List<Pos> spots) {
		if (spots.isEmpty()) {
			return;
		}
		try (PrintStream out = new PrintStream(csvFilename)) {
			for (Pos spot:spots) {
				out.println(spot.getX()+";"+spot.getY());
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e.toString(), e);
		}
		
	}


	private Pos searchMax(ImageAnalyzer ia, int fromX, int fromY, int toX, int toY) {
		int maxLight = -1;
		Pos result = null;
		for (int y=fromY; y<=toY; y++) {
			for (int x=fromX; x<=toX; x++) {
				int p = ia.get(x, y);
				if (p>maxLight) {
					maxLight = p;
					result = new Pos(x, y);
				}
			}
		}
		return result;
	}

	private List<Pos> searchAllComets(ImageAnalyzer iaPrevious, ImageAnalyzer iaThis, ImageAnalyzer iaNext) {
		List<Pos> result = new ArrayList<>();
		for (int y=0; y<1024; y++) {
			for (int x=0; x<1024; x++) {
				double probaIsComet = iaThis.checkComet(x,y);
				if (probaIsComet<4.0) {
					continue;
				}
				double previosProbaIsComet = iaPrevious.checkComet(x,y);
				if (previosProbaIsComet>=1.6) {
					continue;
				}
				double nextProbaIsComet = iaNext.checkComet(x,y);
				if (nextProbaIsComet>=1.6) {
					continue;
				}
				result.add(new Pos(x,y));
			}
		}
		return result;
	}

	

	private BufferedImage scale(BufferedImage bi, double factor) {
		BufferedImage result = new BufferedImage((int)(bi.getWidth()*factor), (int)(bi.getHeight()*factor), BufferedImage.TYPE_BYTE_GRAY);
		AffineTransform at = new AffineTransform();
		at.scale(factor, factor);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		result = scaleOp.filter(bi, result);
		return result;
	}

	private void showRange(ImageAnalyzer ia, int fromX, int fromY, int toX, int toY) {
		int minP = 65536;
		int maxP = -1;
		for (int y = fromY; y<=toY; y++) {
			for (int x = fromX; x<=toX; x++) {
				int p = ia.get(x, y);
				minP = Math.min(minP, p);
				maxP = Math.max(maxP, p);
			}
		}
		logger.fine("minP="+minP+", maxP="+maxP);
		int len = Integer.toString(maxP-minP).length();
		for (int y = fromY; y<=toY; y++) {
			StringBuilder line = new StringBuilder();
			for (int x = fromX; x<=toX; x++) {
				int p = ia.get(x, y);
				line.append(num(p-minP, len)+" ");
			}
			logger.fine(line.toString());
		}
	}

	private String num(int n, int len) {
		String result = Integer.toString(n);
		int spaces = len - result.length();
		if (spaces > 0) {
			result = "                  ".substring(0, spaces)+result; 
		}
		return result;
	}

	public int getLength() {
		return images.size();
	}

	public ImageAnalyzer getImageAnalyzer(int i) {
		return images.get(i);
	}

}
