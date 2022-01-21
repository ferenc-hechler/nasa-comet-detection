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
import java.util.List;
import java.util.stream.Collectors;

import ij.ImagePlus;
import ij.plugin.FITS_Reader;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class CometChallenge {

	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\topcoder\\train-sample\\cmt0030";

	public void processFolder(Path folder) {
		try {

			List<Path> files = Files.list(folder).filter(p -> p.getFileName().toString().endsWith(".fts"))
					.collect(Collectors.toList());
			for (Path file : files) {
				List<Pos> spots = processFile(file);
				String csvFilename = file.toString().replace(".fts", "")+"-spots.csv";
				writeCSV(csvFilename, spots);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
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

	private List<Pos> processFile(Path file) {
		System.out.println("processing "+file);
		FITS_Reader reader = new FITS_Reader();
		reader.run(file.toString());
		ImagePlus imp = (ImagePlus)reader;
		ImageProcessor imgProc = imp.getProcessor();
		ShortProcessor shortProc = (ShortProcessor)imgProc.convertToShort(false);
		BufferedImage gray16img = shortProc.get16BitBufferedImage();
		Raster raster = gray16img.getData();
		DataBuffer dataBuffer = raster.getDataBuffer();
		
		int[][] matrix = new int[1024][1024];
		for (int y = 0; y<1024; y++) {
			for (int x = 0; x<1024; x++) {
				matrix[1023-y][x] = dataBuffer.getElem(1024*y+x);
			}
		}
		ImageAnalyzer ia = new ImageAnalyzer(matrix);
		
		//ia.sub(33826);
		
		//ia.checkCentrum(908, 1018);
		//ia.checkCentrum(909, 1018);
		List<Pos> centers = ia.findCenters();
		System.out.println(centers);

//		showDetails(ia, new Pos(909,1018));
		
		for (Pos center:centers) {
//			showDetails(ia, center);
		}
		
		// 909.10 1018.10
		// showRange(ia, 900,1010, 920, 1023);
		return centers;
	}

	private void showDetails(ImageAnalyzer ia, Pos center) {
		System.out.println("");
		System.out.println("------------ CENTER "+center);
		showRange(ia, center.getX()-5,center.getY()-5, center.getX()+5, center.getY()+5);
		BufferedImage bi = ia.createBufferedImage(center.getX()-10,center.getY()-10, center.getX()+10, center.getY()+10);
		bi = scale(bi, 10.0);
		new ShowImage(bi);
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
		System.out.println("minP="+minP+", maxP="+maxP);
		int len = Integer.toString(maxP-minP).length();
		for (int y = fromY; y<=toY; y++) {
			for (int x = fromX; x<=toX; x++) {
				int p = ia.get(x, y);
				System.out.print(num(p-minP, len)+" ");
			}
			System.out.println();
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
