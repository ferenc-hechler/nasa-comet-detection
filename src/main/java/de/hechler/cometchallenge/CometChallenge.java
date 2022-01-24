package de.hechler.cometchallenge;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ij.ImagePlus;
import ij.plugin.FITS_Reader;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class CometChallenge {


	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\topcoder\\train-sample\\cmt0007";

	
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
	
	private Map<String, CometPos> cometPositionForFilename;
	
	private final static String COMETPOS_RX = "^\\s*([-0-9]+\\s+[0-9:]+)\\s+([0-9]+[.]fts)\\s+([0-9.]+)\\s+([0-9.]+)\\s+([0-9.an]+)\\s*$";
	private final static String TIMESTAMP_FORMAT = "YYYY-MM-dd HH:mm:ss";
	
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
				Date timestamp = parseTimestamp(time); 
				String filename = cometLine.replaceFirst(COMETPOS_RX, "$2");
				double xPos = Double.parseDouble(cometLine.replaceFirst(COMETPOS_RX, "$3"));
				double yPos = Double.parseDouble(cometLine.replaceFirst(COMETPOS_RX, "$4"));
				cometPositionForFilename.put(filename, new CometPos(timestamp, new Pos((int)(xPos+0.5), (int)(yPos+0.5))));
			}
			System.out.println(cometPositionForFilename);
			
			
			List<Path> files = Files.list(folder).filter(p -> p.getFileName().toString().endsWith(".fts"))
					.collect(Collectors.toList());
			ImageAnalyzer lastImage = null;
			ImageAnalyzer thisImage = null;
			ImageAnalyzer nextImage = null;
			// edge case first image: use next image as previous image
			lastImage = readFile(files.get(1), cometPositionForFilename);
			thisImage = readFile(files.get(0), cometPositionForFilename);
			
			List<CometPath> cometPaths = new ArrayList<>();
			List<CometPath> finishedCometPaths = new ArrayList<>();
			
			for (int i=0; i<files.size(); i++) {
				if (i==files.size()-1) {
					// edge case last image: use previous image as next image
					nextImage = lastImage;
				}
				else {
					nextImage = readFile(files.get(i+1), cometPositionForFilename);
				}
				
				List<Pos> spots = runAnalysis(lastImage, thisImage, nextImage);
				String csvFilename = files.get(i).toString().replace(".fts", "")+"-spots.csv";
				writeCSV(csvFilename, spots);
//				showSpots(thisImage.getFilename(), spots);
				
				Date timestamp = thisImage.getTimestamp();
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
						finishedCometPaths.add(lastCometPath);
					}
				}
				
				lastImage = thisImage;
				thisImage = nextImage;
			}
			finishedCometPaths.addAll(cometPaths);
			for (CometPath finishedCometPath:finishedCometPaths) {
				if (finishedCometPath.getLength()>5) {
					logger.info("FOUND COMET PATH: "+finishedCometPath);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private Date parseTimestamp(String time) {
		try {
			return new SimpleDateFormat(TIMESTAMP_FORMAT).parse(time);
		} catch (ParseException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	private void showSpots(String filename, List<Pos> spots) {
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
			raster.setPixel(spot.getX()/scale, spot.getY()/scale, gray);
		}
		new ShowImage(filename, image);
	}

	private List<Pos> runAnalysis(ImageAnalyzer lastImage, ImageAnalyzer thisImage, ImageAnalyzer nextImage) {
		CometPos labeledCometPos = cometPositionForFilename.get(thisImage.getFilename());
		Pos cometPos = labeledCometPos.getPosition();
		Pos correctedCometPos = searchMax(thisImage, cometPos.getX()-5, cometPos.getY()-5, cometPos.getX()+5, cometPos.getY()+5);
//		correctedCometPos = cometPos;
//		logger.info(cometPos + " -> " + correctedCometPos);
//		showCompare9Details(lastImage, thisImage, nextImage, correctedCometPos);
//		thisImage.checkCentrum(correctedCometPos.getX(), correctedCometPos.getY());
		List<Pos> centers = searchAllComets(lastImage, thisImage, nextImage);
		logger.info("---- "+thisImage.getFilename()+" ---");
		logger.info("TO BE FOUND: "+cometPos);
		logger.info("DETECTED:    "+centers);
		if (centers.contains(cometPos)) {
			logger.info("!!! FOUND ORIGINAL !!!");
		}
		if (centers.contains(correctedCometPos)) {
			logger.info("!!! FOUND CORRECTED POSITION !!!");
		}
		logger.info("-----------------------------------");
		logger.info("#centers: "+ centers.size());
		centers = concentrate(centers, 2);
		logger.info("#centers(concentrated): "+ centers.size());
		logger.info("CONTRACTED:    "+centers);
		logger.info("-----------------------------------");
		return centers;
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

	private ImageAnalyzer readFile(Path path, Map<String, CometPos> cometPositionForFilename) {
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
		return new ImageAnalyzer(path, matrix, cometPositionForFilename.get(path.getFileName().toString()));
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

	
	private void showCompare9Details(ImageAnalyzer iaPrevious, ImageAnalyzer iaThis, ImageAnalyzer iaNext, Pos center) {
		logger.fine("");
		logger.fine("------------ CENTER "+center);
		showRange(iaThis, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);

		int fromX = center.getX()-15;
		int toX = center.getX()+15;
		int fromY = center.getY()-15;
		int toY = center.getY()+15;

		MinMaxCounter rangePrevious = iaPrevious.calcMinMax(fromX,fromY, toX,toY);
		MinMaxCounter rangeThis = iaThis.calcMinMax(fromX,fromY, toX,toY);
		MinMaxCounter rangeNext = iaNext.calcMinMax(fromX,fromY, toX,toY);
		BufferedImage biPrevious = iaPrevious.createBufferedImage(rangePrevious, fromX,fromY, toX, toY);
		BufferedImage biThis = iaThis.createBufferedImage(rangeThis, fromX,fromY, toX, toY);
		BufferedImage biNext = iaNext.createBufferedImage(rangeNext, fromX,fromY, toX, toY);
		biPrevious = scale(biPrevious, 10.0);
		biThis = scale(biThis, 10.0);
		biNext = scale(biNext , 10.0);

		MinMaxCounter rangeG = new MinMaxCounter(rangePrevious).update(rangeThis).update(rangeNext);
		BufferedImage biPreviousG = iaPrevious.createBufferedImage(rangeG, fromX,fromY, toX, toY);
		BufferedImage biThisG = iaThis.createBufferedImage(rangeG, fromX,fromY, toX, toY);
		BufferedImage biNextG = iaNext.createBufferedImage(rangeG, fromX,fromY, toX, toY);
		biPreviousG = scale(biPreviousG, 10.0);
		biThisG = scale(biThisG, 10.0);
		biNextG = scale(biNextG, 10.0);

		int deltaP = rangePrevious.getMax()-rangePrevious.getMin();
		int deltaT = rangeThis.getMax()-rangeThis.getMin();
		int deltaN = rangeNext.getMax()-rangeNext.getMin();
		int delta = Math.max(deltaP, Math.max(deltaT, deltaN));
		MinMaxCounter rangePreviousA = new MinMaxCounter(rangePrevious.getMin(), rangePrevious.getMin()+delta, 1, 1);
		MinMaxCounter rangeThisA = new MinMaxCounter(rangeThis.getMin(), rangeThis.getMin()+delta, 1, 1);
		MinMaxCounter rangeNextA = new MinMaxCounter(rangeNext.getMin(), rangeNext.getMin()+delta, 1, 1);
		BufferedImage biPreviousA = iaPrevious.createBufferedImage(rangePreviousA, fromX,fromY, toX, toY);
		BufferedImage biThisA = iaThis.createBufferedImage(rangeThisA, fromX,fromY, toX, toY);
		BufferedImage biNextA = iaNext.createBufferedImage(rangeNextA, fromX,fromY, toX, toY);
		biPreviousA = scale(biPreviousA, 10.0);
		biThisA = scale(biThisA, 10.0);
		biNextA = scale(biNextA , 10.0);
		
		BufferedImage[] images = {
				biPreviousG, biThisG, biNextG, 
				biPrevious, biThis, biNext,
				biPreviousA, biThisA, biNextA
			};
		new ShowMultipleCompImage(iaThis.getFilename(), 3, 3, images);

		logger.info("--- "+iaThis.getFilename()+" ---");
		double probaIsComet = iaThis.checkComet(center.getX(),center.getY());
		logger.info("ISCOMTPROBA: "+probaIsComet);
		logger.info("--------------------------------");
		double previosProbaIsComet = iaPrevious.checkComet(center.getX(),center.getY());
		logger.info("PREVIOUS: "+previosProbaIsComet);
		logger.info("--------------------------------");
		double nextProbaIsComet = iaNext.checkComet(center.getX(),center.getY());
		logger.info("NEXT: "+nextProbaIsComet);
		logger.info("--------------------------------");
		if ((probaIsComet > 2.0) && (previosProbaIsComet <2.0) && (nextProbaIsComet <2.0)) {
			logger.info("THIS IS A COMET");
		}
		return;
		
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
