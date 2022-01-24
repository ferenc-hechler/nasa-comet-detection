package de.hechler.cometchallenge;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hechler.cometchallenge.analyze.ImageAnalyzer;
import de.hechler.cometchallenge.analyze.SequenceAnalyzer;
import de.hechler.cometchallenge.gui.CometPathsController;
import de.hechler.cometchallenge.gui.ImageController;
import de.hechler.cometchallenge.gui.ShowControlledImages;

public class CometChallenge {


//	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\topcoder\\train-sample\\cmt0007";
//	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\NASA\\train-sample\\cmt0030";
	public final static String DEFAULT_INPUT_FOLDER = "C:\\DEV\\NASA\\train-sample\\cmt0003";

	
	private final static Logger logger = Logger.getLogger(CometChallenge.class.getName());
	static {
		// System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s [%1$tc]%n");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%n");
		Logger root = Logger.getLogger("");
//	    root.setLevel(Level.FINE);
//		for (Handler handler : root.getHandlers()) {
//			handler.setLevel(Level.FINE);
//		}
		Logger.getLogger("javax.swing").setLevel(Level.OFF);
		Logger.getLogger("javax.awt").setLevel(Level.OFF);
	    Logger.getLogger("sun.awt").setLevel(Level.OFF);
	    Logger.getLogger("de.hechler").setLevel(Level.FINE);
	}
	
	private SequenceAnalyzer analyzer;
	
	public void processFolder(Path folder) {
		
		analyzer = new SequenceAnalyzer();
		analyzer.readSequence(folder);

		analyzer.detectCometSpots();
		List<CometPath> detectedCometPaths = analyzer.detectCometPaths();

		CometPath labeledCometPath = extractLabeledCometPath();
		detectedCometPaths.add(0, labeledCometPath);
		
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
		String inputFolder = DEFAULT_INPUT_FOLDER;
		if (args.length > 0) {
			inputFolder = args[0];
		}
		CometChallenge challenge = new CometChallenge();
		challenge.processFolder(Paths.get(inputFolder));
	}

}
