package de.hechler.cometchallenge;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hechler.cometchallenge.analyze.ImageAnalyzer;
import de.hechler.cometchallenge.analyze.SequenceAnalyzer;
import de.hechler.cometchallenge.gui.CometPathsController;
import de.hechler.cometchallenge.gui.ImageController;
import de.hechler.cometchallenge.gui.ShowControlledImages;
import de.hechler.cometchallenge.utils.Utils;

public class CometChallenge {


//	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\topcoder\\train-sample\\cmt0007";
//	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\NASA\\train-sample\\cmt0030";
//	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\NASA\\train-sample\\cmt0003";
//	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\NASA\\train-sample\\cmt0006";
	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\NASA\\train-sample\\cmt0009";

	
	private final static Logger logger;
	static {
		// System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s [%1$tc]%n");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
		Logger root = Logger.getLogger("");
	    root.setLevel(Level.FINE);
		for (Handler handler : root.getHandlers()) {
			handler.setLevel(Level.FINE);
		}
	    Logger.getLogger("de.hechler").setLevel(Level.FINE);
		Logger.getLogger("java.awt").setLevel(Level.OFF);
		Logger.getLogger("javax.swing").setLevel(Level.OFF);
	    Logger.getLogger("sun.awt").setLevel(Level.OFF);
	    
	    logger = Logger.getLogger(CometChallenge.class.getName());
	    logger.fine("FINE works");
	    Logger loggersw = Logger.getLogger("javax.swing.jbutton");
	    loggersw.fine("FINE should not work");
	}
	
	private SequenceAnalyzer analyzer;
	
	public void processFolder(Path folder) {
		
		analyzer = new SequenceAnalyzer();
		analyzer.readSequence(folder);

		analyzer.detectCometSpots();
		List<CometPath> detectedCometPaths = analyzer.detectCometPaths();

		CometPath labeledCometPath = extractLabeledCometPath();
		detectedCometPaths.add(0, labeledCometPath);

//		 showSpecialImage0009();

		
		ImageController controller = new CometPathsController(analyzer, detectedCometPaths);
		new ShowControlledImages(folder.getFileName().toString(), controller);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		
		for (int i=0; i<detectedCometPaths.size(); i++) {
			CometPath detectedCometPath = detectedCometPaths.get(i);
			logger.info("CP-"+i+": "+detectedCometPath.toSubmissionText(analyzer));
		}
		
	}


	private void showSpecialImage0009() {
		int steps = 9;
		int fromX = 880;
		int toX = 920;
		int fromY =10;
		int toY = 100;
		
		int width = toX - fromX + 1;
		int height = toY - fromY + 1;

		int totalWidth = steps*width;
		
        BufferedImage concatImage = new BufferedImage(totalWidth, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = concatImage.createGraphics();
        g2d.setColor(Color.BLACK);
		int currentWidth = 0;
		for (int i=0; i<steps; i++) {
			ImageAnalyzer ia = analyzer.getImageAnalyzer(i);
			BufferedImage bi = ia.createBufferedImage(fromX, fromY, toX, toY);
			g2d.drawImage(bi, currentWidth, 0, null);
			int mx = (int)ia.getLabeledCometPos().getX()-fromX+currentWidth;
			int my = (int)ia.getLabeledCometPos().getY()-fromY;
			g2d.drawRect(mx-10,my-10, 20, 20);
			currentWidth += width;
		}
        g2d.dispose();
        
        concatImage = Utils.scale(concatImage, 3.0);
        new ShowImage("cmt0003b", concatImage);
        return;
	}

	private void showSpecialImage0003() {
		int steps = 16;
		int fromX = 770;
		int toX = 860;
		int fromY = 330;
		int toY = 350;
		
		int wight = toX - fromX + 1;
		int height = toY - fromY + 1;

		int totalHeight = steps*height;
		
        BufferedImage concatImage = new BufferedImage(wight, totalHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = concatImage.createGraphics();

		int currentHeight = 0;
		for (int i=0; i<16; i++) {
			ImageAnalyzer ia = analyzer.getImageAnalyzer(i);
			BufferedImage bi = ia.createBufferedImage(fromX, fromY, toX, toY);
			g2d.drawImage(bi, 0, currentHeight, null);
			currentHeight += height;
		}
        g2d.dispose();
        
        concatImage = Utils.scale(concatImage, 3.0);
        
        new ShowImage("cmt0003b", concatImage);
        return;
	}


	private CometPath extractLabeledCometPath() {
		CometPath result = null;
		for (int i=0; i<analyzer.getLength(); i++) {
			ImageAnalyzer ia = analyzer.getImageAnalyzer(i);
			if (result == null) {
				result = new CometPath(ia.getTimestamp(), ia.getLabeledCometPos());
			}
			else {
				result.add(ia.getTimestamp(), ia.getLabeledCometPos());
			}
		}
		return result;
	}


	public static void main(String[] args) {
		logger.fine("START");
		String inputFolder = DEFAULT_INPUT_FOLDER;
		if (args.length > 0) {
			inputFolder = args[0];
		}
		CometChallenge challenge = new CometChallenge();
		challenge.processFolder(Paths.get(inputFolder));
	}

}
