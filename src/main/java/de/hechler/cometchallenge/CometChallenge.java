package de.hechler.cometchallenge;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ij.ImagePlus;
import ij.plugin.FITS_Reader;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class CometChallenge {


	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\topcoder\\train-sample\\cmt0030";

	
	private final static Logger logger = Logger.getLogger(CometChallenge.class.getName());
	static {
		// System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s [%1$tc]%n");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
		Logger root = Logger.getLogger("");
	    root.setLevel(Level.FINE);
		for (Handler handler : root.getHandlers()) {
			handler.setLevel(Level.FINE);
		}
	}
	
	private Map<String, Pos> cometPositionForFilename;
	
	private final static String COMETPOS_RX = "^\\s*[-0-9]+\\s+([0-9:]+)\\s+([0-9]+[.]fts)\\s+([0-9.]+)\\s+([0-9.]+)\\s+([0-9.an]+)\\s*$";
	
	public void processFolder(Path folder) {
		try {
			cometPositionForFilename = new LinkedHashMap<>();
			List<String> cometLines = Files.readAllLines(folder.resolve(folder.getFileName().toString()+".txt"));
			for (String cometLine:cometLines) {
				if (cometLine.trim().startsWith("#") || cometLine.trim().isEmpty()) {
					continue;
				}
				if (!cometLine.matches(COMETPOS_RX)) {
					throw new RuntimeException("invalid line '"+cometLine+"'");
				}
				String time = cometLine.replaceFirst(COMETPOS_RX, "$1");
				String filename = cometLine.replaceFirst(COMETPOS_RX, "$2");
				double xPos = Double.parseDouble(cometLine.replaceFirst(COMETPOS_RX, "$3"));
				double yPos = Double.parseDouble(cometLine.replaceFirst(COMETPOS_RX, "$4"));
				cometPositionForFilename.put(filename, new Pos((int)(xPos+0.5), (int)(yPos+0.5)));
			}
			System.out.println(cometPositionForFilename);
			
			
			List<Path> files = Files.list(folder).filter(p -> p.getFileName().toString().endsWith(".fts"))
					.collect(Collectors.toList());
			ImageAnalyzer lastImage = null;
			ImageAnalyzer thisImage = null;
			ImageAnalyzer nextImage = null;
			// edge case first image: use next image as previous image
			lastImage = readFile(files.get(1));
			thisImage = readFile(files.get(0));
			for (int i=0; i<files.size(); i++) {
				if (i==files.size()-1) {
					// edge case last image: use previous image as next image
					nextImage = lastImage;
				}
				else {
					nextImage = readFile(files.get(i+1));
				}
				
				List<Pos> spots = runAnalysis(lastImage, thisImage, nextImage);
				String csvFilename = files.get(i).toString().replace(".fts", "")+"-spots.csv";
				writeCSV(csvFilename, spots);
				
				lastImage = thisImage;
				thisImage = nextImage;
			}
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private List<Pos> runAnalysis(ImageAnalyzer lastImage, ImageAnalyzer thisImage, ImageAnalyzer nextImage) {
		Pos cometPos = cometPositionForFilename.get(thisImage.getFilename());
		Pos correctedCometPos = searchMax(thisImage, cometPos.getX()-5, cometPos.getY()-5, cometPos.getX()+5, cometPos.getY()+5);
		logger.info(cometPos + " -> " + correctedCometPos);
		showCompare3Details(lastImage, thisImage, nextImage, correctedCometPos);
		thisImage.checkCentrum(correctedCometPos.getX(), correctedCometPos.getY());
		List<Pos> centers = new ArrayList<>();
		centers.add(correctedCometPos);
		return centers;
	}

	private ImageAnalyzer readFile(Path path) {
		String filename = path.getFileName().toString();
		logger.info("reading "+filename);
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
		return new ImageAnalyzer(path, matrix);
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

	private int[][] lastMatrix = new int[1024][1024];
	
//	private List<Pos> processFile(Path file) {
//		String filename = file.getFileName().toString();
//		logger.info("processing "+filename);
//		FITS_Reader reader = new FITS_Reader();
//		reader.run(file.toString());
//		ImagePlus imp = (ImagePlus)reader;
//		ImageProcessor imgProc = imp.getProcessor();
//		ShortProcessor shortProc = (ShortProcessor)imgProc.convertToShort(false);
//		BufferedImage gray16img = shortProc.get16BitBufferedImage();
//		Raster raster = gray16img.getData();
//		DataBuffer dataBuffer = raster.getDataBuffer();
//		
//		int[][] matrix = new int[1024][1024];
//		for (int y = 0; y<1024; y++) {
//			for (int x = 0; x<1024; x++) {
//				matrix[1023-y][x] = dataBuffer.getElem(1024*y+x);
//			}
//		}
//		ImageAnalyzer ia = new ImageAnalyzer(matrix);
//		ImageAnalyzer iaLast = new ImageAnalyzer(lastMatrix);
//		
//		Pos cometPos = cometPositionForFilename.get(filename);
////		if (filename.equals("22721520.fts")) {
////			Pos correctedCometPos = searchMax(ia, cometPos.getX()-10, cometPos.getY()-10, cometPos.getX()+10, cometPos.getY()+10);
////		}
//		Pos correctedCometPos = searchMax(ia, cometPos.getX()-5, cometPos.getY()-5, cometPos.getX()+5, cometPos.getY()+5);
//		//Pos correctedCometPos = searchMax(ia, cometPos.getX()-5, cometPos.getY()-5, cometPos.getX()+5, cometPos.getY()+5);
//		logger.info(cometPos + " -> " + correctedCometPos);
//		showCompareDetails(filename, ia, iaLast, correctedCometPos);
//		ia.checkCentrum(correctedCometPos.getX(), correctedCometPos.getY());
//		// showRange(ia, 900,1010, 920, 1023);
//
//		//List<Pos> centers = ia.findCenters();
//		//logger.info(centers.toString());
//		//for (Pos center:centers) {
//		//	showDetails(ia, center);
//		//}
//		
//		List<Pos> centers = new ArrayList<>();
//		centers.add(correctedCometPos);
//		
//		lastMatrix = matrix;
//		
//		return centers;
//	}

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

	private void showCompare3Details(ImageAnalyzer iaPrevious, ImageAnalyzer iaThis, ImageAnalyzer iaNext, Pos center) {
		int fromX = center.getX()-5;
		int toX = center.getX()+5;
		int fromY = center.getY()-5;
		int toY = center.getY()+5;
		MinMaxCounter rangePrevious = iaPrevious.calcMinMax(fromX,fromY, toX,toY);
		MinMaxCounter rangeThis = iaThis.calcMinMax(fromX,fromY, toX,toY);
		MinMaxCounter rangeNext = iaNext.calcMinMax(fromX,fromY, toX,toY);
		MinMaxCounter range = new MinMaxCounter(rangePrevious).update(rangeThis).update(rangeNext);
		logger.fine("");
		logger.fine("------------ CENTER "+center);
		showRange(iaThis, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);
		logger.fine("------------ PREVIOUS "+center);
		showRange(iaPrevious, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);
		logger.fine("------------ NEXT "+center);
		showRange(iaNext, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);
		BufferedImage biPrevious = iaPrevious.createBufferedImage(range, center.getX()-10,center.getY()-10, center.getX()+10, center.getY()+10);
		BufferedImage biThis = iaThis.createBufferedImage(range, center.getX()-10,center.getY()-10, center.getX()+10, center.getY()+10);
		BufferedImage biNext = iaNext.createBufferedImage(range, center.getX()-10,center.getY()-10, center.getX()+10, center.getY()+10);
		biPrevious = scale(biPrevious, 10.0);
		biThis = scale(biThis, 10.0);
		biNext = scale(biNext , 10.0);
		new Show3CompImage(iaThis.getFilename(), biPrevious, biThis, biNext);
	}

	private void showCompareDetails(String filename, ImageAnalyzer ia, ImageAnalyzer iaComp, Pos center) {
		logger.fine("");
		logger.fine("------------ CENTER "+center);
		showRange(ia, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);
		logger.fine("------------ COMP "+center);
		showRange(iaComp, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);
		BufferedImage bi = ia.createBufferedImage(center.getX()-10,center.getY()-10, center.getX()+10, center.getY()+10);
		BufferedImage biComp = iaComp.createBufferedImage(center.getX()-10,center.getY()-10, center.getX()+10, center.getY()+10);
		bi = scale(bi, 10.0);
		biComp = scale(biComp, 10.0);
		new ShowCompImage(filename, bi, biComp);
	}

	private void showDetails(String filename, ImageAnalyzer ia, Pos center) {
		logger.fine("");
		logger.fine("------------ CENTER "+center);
		showRange(ia, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);
		BufferedImage bi = ia.createBufferedImage(center.getX()-10,center.getY()-10, center.getX()+10, center.getY()+10);
		bi = scale(bi, 10.0);
		new ShowImage(filename, bi);
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

	public static void main(String[] args) {
		String inputFolder = DEFAULT_INPUT_FOLDER;
		if (args.length > 0) {
			inputFolder = args[0];
		}
		CometChallenge challenge = new CometChallenge();
		challenge.processFolder(Paths.get(inputFolder));
	}

}
