package de.hechler.cometchallenge.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.logging.Logger;
import java.awt.Composite;

import de.hechler.cometchallenge.CometPath;
import de.hechler.cometchallenge.CometPos;
import de.hechler.cometchallenge.MinMaxCounter;
import de.hechler.cometchallenge.MinMaxStat;
import de.hechler.cometchallenge.analyze.ImageAnalyzer;
import de.hechler.cometchallenge.analyze.SequenceAnalyzer;
import de.hechler.cometchallenge.geometry.Pos;
import de.hechler.cometchallenge.utils.Utils;
import java.awt.AlphaComposite;

public class CometPathsController implements ImageController {

	private static final Logger logger = Logger.getLogger(CometPathsController.class.getName());

	private SequenceAnalyzer analyzer;
	
	private List<CometPath> cometPaths;
	private int currentCometPath;
	private int currentImage;
	
	private enum MODE { PATH, DETAIL, SPOT }
	private MODE overviewMode;
	
	private boolean showInfo;

	public CometPathsController(SequenceAnalyzer analyzer, List<CometPath> cometPaths) {
		this.analyzer = analyzer;
		this.cometPaths = cometPaths;
		this.overviewMode = MODE.PATH;
		this.currentImage = 0;
		this.currentCometPath = 0;
		this.showInfo = false;
	}

	private CometPath getCurrentCometPath() {
		return cometPaths.get(currentCometPath);
	}
	
	private ImageAnalyzer getCurrentImageAnalyzer() {
		return analyzer.getImageAnalyzer(currentImage);
	}
	
	public boolean hasLeft() {
		switch (overviewMode) {
		case PATH: {
			return currentCometPath > 0;
		}
		case DETAIL: {
			return currentImage > 0;
		}
		case SPOT: {
			return currentImage > 0;
		}
		default:
			throw new RuntimeException("invalid overview mode "+overviewMode);
		}
	}
	
	public boolean hasRight() {
		switch (overviewMode) {
		case PATH: {
			return currentCometPath < cometPaths.size()-1;
		}
		case DETAIL: {
			return currentImage < getCurrentCometPath().getLength()-1;
		}
		case SPOT: {
			return currentImage < getCurrentCometPath().getLength()-1;
		}
		default:
			throw new RuntimeException("invalid overview mode "+overviewMode);
		}
	}

	public String getCurrentInfo() {
		switch (overviewMode) {
		case PATH: {
			double distError = getCurrentCometPath().getDistError();
			double lineError = getCurrentCometPath().getLineError();
			String qual = " QD["+distError+"] QL["+lineError+"]";
			return "PATH: " + (currentCometPath+1) + "/" + cometPaths.size() + qual;  
		}
		case DETAIL: {
			double distError = getCurrentCometPath().getDistError();
			double lineError = getCurrentCometPath().getLineError();
			Pos pos = getCurrentCometPath().getCometPosition(currentImage).getPosition();
			String qual = "QD["+distError+"] QL["+lineError+"]";
			return "DETAIL: " + (currentImage+1) + "/" + getCurrentCometPath().getLength() + " "+ pos + " " + qual;
		}
		case SPOT: {
			return "SPOT: " + (currentImage+1) + "/" + getCurrentCometPath().getLength();
		}
		default:
			throw new RuntimeException("invalid overview mode "+overviewMode);
		}
	}

	public BufferedImage getCurrentImage() {
		switch (overviewMode) {
		case PATH: {
			return getOverviewImage();
		}
		case DETAIL: {
			return getDetailImage();
		}
		case SPOT: {
			return getAllSpotImage();
		}
		default:
			throw new RuntimeException("invalid overview mode "+overviewMode);
		}
	}
	
	public BufferedImage getOverviewImage() {
		if (showInfo) {
			return getOverviewImageBlack();
		}
		return getOverviewImageTransparent();
	}
	private BufferedImage getOverviewImageBlack() {
		BufferedImage bi = Utils.createSpots(getCurrentCometPath(), 1.0);	
		return bi;
	}
	private BufferedImage getOverviewImageTransparent() {
        BufferedImage biCometSpots = getOverviewImageBlack();

        BufferedImage result = getCurrentImageAnalyzer().createBufferedImage(0, 0, 1023, 1023);
        Graphics2D g2d = result.createGraphics();
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(makeComposite(0.5f));
		g2d.drawImage(biCometSpots, 0, 0, null);
        g2d.setComposite(originalComposite);        
		g2d.dispose();
		
		return result;
	}
	private AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}


	
	public BufferedImage getDetailImage() {
		if (showInfo) {
			return getDetailImageSequence();
		}
		return getDetailImage3x3();
	}
	
	
	public BufferedImage getDetailImageSequence() {
		CometPos cp = getCurrentCometPath().getCometPosition(currentImage);
		ImageAnalyzer iaThis = analyzer.getImageAnalyzerForTimestamp(cp.getTimestamp());

		int delta = 5;
		int cols = 3;

		int x = (int)cp.getPosition().getX();
		int y = (int)cp.getPosition().getY();
		int fromX = x-delta;
		int toX = x+delta;
		int fromY = y-delta;
		int toY = y+delta;
		
		int width = toX-fromX+1;
		int height = toY-fromY+1;

		int rows = (analyzer.getLength()+cols-1) / cols;
		
		int totalWidth = cols*width;
		int totalHeight = rows*width;

        BufferedImage concatImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_BYTE_GRAY);

        MinMaxCounter range = new MinMaxCounter();
        for (int i=0; i<analyzer.getLength(); i++) {
        	ImageAnalyzer ia = analyzer.getImageAnalyzer(i);
        	range.update(ia.calcMinMax(fromX, fromY, toX, toY));
        }
        
        range = iaThis.calcMinMax(fromX, fromY, toX, toY); 
        
        logger.info("RANGE: "+range);
        
        Graphics2D g2d = concatImage.createGraphics();
        for (int i=0; i<analyzer.getLength(); i++) {
        	ImageAnalyzer ia = analyzer.getImageAnalyzer(i);
        	BufferedImage bi = ia.createBufferedImage(range, fromX, fromY, toX, toY);
        	int c=i%cols;
        	int r=i/cols;
			g2d.drawImage(bi, c*width, r*height, null);
        }

//        BufferedImage biDiff = calcDiffImage(iaThis, fromX, toX, fromY, toY);
//		int c=currentImage%cols;
//    	int r=currentImage/cols;
//		g2d.drawImage(biDiff, c*width, r*height, null);
    	
        
        g2d.dispose();

    	logger.info("--- current "+(currentImage+1)+" (ExpTime="+getCurrentImageAnalyzer().getExpTime()+") ---");
        for (int i=0; i<analyzer.getLength(); i++) {
        	ImageAnalyzer ia = analyzer.getImageAnalyzer(i);
            double factor = 1.0/ia.getExpTime();
        	MinMaxStat allMM = ia.calcMinMaxStat(fromX, fromY, toX, toY);
            int centerDist = 1;
    		MinMaxStat centerMM = ia.calcMinMaxStat(x-centerDist, y-centerDist, x+centerDist, y+centerDist);
        	logger.info("IMG-"+(i+1)+".SIGMA:  center="+centerMM.getSigma()+"  all="+allMM.getSigma());
//        	logger.info("IMG-"+(i+1)+".STDERR: center="+centerMM.getStdErr()+"  all="+allMM.getStdErr());
//        	logger.info("--- IMAGE "+(i+1)+" ---");
//        	logger.info("EXPTIME: "+ia.getExpTime());
//        	logger.info("CENTER:  "+(ia.get(x, y)));
//    		MinMaxStat rangeMM = ia.calcMinMaxStat(fromX, fromY, toX, toY);
//        	logger.info("SIGMA:   "+(rangeMM.getSigma()));
//        	for (int d=1;d<=5; d++) {
//        		MinMaxStat surroundingMM = ia.surroundingStat(x, y, d);
//            	logger.info(" D"+d+"SIGMA: "+(surroundingMM.getSigma()));
//        	}
//        	for (int d=1;d<=5; d++) {
//        		MinMaxStat surroundingMM = ia.surroundingStat(x, y, d);
//            	logger.info("  D"+d+"AVG:   "+(surroundingMM.getAvg()));
//        	}
//        	for (int d=1;d<=5; d++) {
//        		MinMaxStat surroundingMM = ia.surroundingStat(x, y, d);
//            	logger.info(" D"+d+"MIN:   "+(surroundingMM.getMin()));
//        	}
//        	for (int d=1;d<=5; d++) {
//        		MinMaxStat surroundingMM = ia.surroundingStat(x, y, d);
//            	logger.info("  D"+d+"MAX:   "+(surroundingMM.getMax()));
//        	}
        }
        
        
        concatImage = Utils.scale(concatImage, 8.0);
		return concatImage;
	}

	private BufferedImage calcDiffImage(ImageAnalyzer iaThis, int fromX, int toX, int fromY, int toY) {
		int width = toX-fromX+1;
		int height = toY-fromY+1;
		double[][] matrixNC = new double[height][width]; 
        double[][] matrixC = new double[height][width];
        double cntOther = analyzer.getLength()-1;
        
        MinMaxCounter diffRange = new MinMaxCounter();
        for (int py=fromY; py<=toY; py++) {
            for (int px=fromX; px<=toX; px++) {
                for (int i=0; i<analyzer.getLength(); i++) {
                	ImageAnalyzer ia = analyzer.getImageAnalyzer(i);
                	if (ia == iaThis) {
                		matrixC[py-fromY][px-fromX] = 1.0*ia.get(px, py)/ia.getExpTime();
                	}
                	else {
                		matrixNC[py-fromY][px-fromX] += 1.0*ia.get(px, py)/ia.getExpTime();
                	}
                }    
                matrixC[py-fromY][px-fromX] -= matrixNC[py-fromY][px-fromX]/cntOther;
                diffRange.update((int)(100.0*matrixC[py-fromY][px-fromX]));
            }
        }

        double offset = 0.01*diffRange.getMin();
        double scale = 255.0/(0.01*(diffRange.getMax()-diffRange.getMin())+0.00001);
        int[] gray = new int[1];
        BufferedImage biDiff = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = biDiff.getRaster();
		for (int py=fromY; py<=toY; py++) {
			for (int px=fromX; px<=toX; px++) {
				gray[0] = Math.max(0, Math.min(255, (int) (scale*(matrixC[py-fromY][px-fromX]-offset))));
				raster.setPixel(px-fromX, py-fromY, gray);
			}
		}
		return biDiff;
	}
	
	

	public BufferedImage getDetailImage3x3() {
		CometPos cp = getCurrentCometPath().getCometPosition(currentImage);
		ImageAnalyzer iaThis = analyzer.getImageAnalyzerForTimestamp(cp.getTimestamp());
		ImageAnalyzer iaPrevious = analyzer.getPreviousImageAnalyzer(iaThis);
		ImageAnalyzer iaNext = analyzer.getNextImageAnalyzer(iaThis);
		int x = (int)cp.getPosition().getX();
		int y = (int)cp.getPosition().getY();
		
		int delta = 7;
		int fromX = x-delta;
		int toX = x+delta;
		int fromY = y-delta;
		int toY = y+delta;
		
		int width = toX-fromX+1;
		int height = toY-fromY+1;
		int totalWidth = 3*width;
		int totalHeight = 3*width;
		
        BufferedImage concatImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = concatImage.createGraphics();

        int currentHeight = 0;
        for (int row=0; row<3; row++) {
            MinMaxCounter rangeT = iaThis.calcMinMax(fromX, fromY, toX, toY);
            MinMaxCounter rangeN = iaNext == null ? null : iaNext.calcMinMax(fromX, fromY, toX, toY); 
            MinMaxCounter rangeP = iaPrevious == null ? null : iaPrevious.calcMinMax(fromX, fromY, toX, toY);
            if (row == 0) {
            	// calibrate over all images
                if (rangeN != null) {
                	rangeT.update(rangeT);
                	rangeN = rangeT;
                }
                if (rangeP != null) {
                	rangeT.update(rangeP);
                	rangeP = rangeT;
                }
            }
            else if (row == 1) {
            	// keep each image calibrated by its own values
            }
            else {
            	// keep the delta from min
            	int diff = rangeT.getMax()-rangeT.getMin();
                if (rangeN != null) {
                	rangeN = new MinMaxCounter(rangeN.getMin(), rangeN.getMin()+diff, 1,1);
                }
                if (rangeP != null) {
                	rangeP = new MinMaxCounter(rangeP.getMin(), rangeP.getMin()+diff, 1,1);
                }
            }
        
	        BufferedImage bi;
	        if (iaPrevious != null) {
	        	bi = iaPrevious.createBufferedImage(rangeP, fromX, fromY, toX, toY);
				g2d.drawImage(bi, 0, currentHeight, null);
	        }
	    	bi = iaThis.createBufferedImage(rangeT, fromX, fromY, toX, toY);
			g2d.drawImage(bi, width, currentHeight, null);
	        if (iaNext != null) {
	        	bi = iaNext.createBufferedImage(rangeN, fromX, fromY, toX, toY);
				g2d.drawImage(bi, 2*width, currentHeight, null);
	        }
	    	currentHeight += height;
	    }
        
        g2d.dispose();
        
        concatImage = Utils.scale(concatImage, 10.0);
		return concatImage;
	}
	
	public BufferedImage getSimpleDetailImage() {
		CometPos cp = getCurrentCometPath().getCometPosition(currentImage);
		ImageAnalyzer ia = analyzer.getImageAnalyzerForTimestamp(cp.getTimestamp());
		int x = (int)cp.getPosition().getX();
		int y = (int)cp.getPosition().getY();
		int delta = 10;
		BufferedImage bi = ia.createBufferedImage(x-delta,y-delta, x+delta, y+delta);
		bi = Utils.scale(bi, 10.0);
		return bi;
	}

	
	public BufferedImage getAllSpotImage() {
		if (showInfo) {
			return getAllSpotImageBlack();
		}
		return getAllSpotImageTransparent();
	}
	public BufferedImage getAllSpotImageBlack() {
		ImageAnalyzer ia = analyzer.getImageAnalyzerForTimestamp(getCurrentCometPath().getCometPosition(currentImage).getTimestamp());
		BufferedImage bi = Utils.createSpots(ia.getSpots(), 1.0);
		return bi;
	}
	private BufferedImage getAllSpotImageTransparent() {
        BufferedImage biSpots = getAllSpotImageBlack();

        BufferedImage result = getCurrentImageAnalyzer().createBufferedImage(0, 0, 1023, 1023);
        Graphics2D g2d = result.createGraphics();
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(makeComposite(0.5f));
		g2d.drawImage(biSpots, 0, 0, null);
        g2d.setComposite(originalComposite);        
		g2d.dispose();
		
		return result;
	}

	

	public void left(ImageWindow iw) {
		switch (overviewMode) {
		case PATH: {
			if (currentCometPath==0) {
				return;
			}
			currentCometPath -= 1;
			currentImage = 0;
			iw.updateControls();
			return;
		}
		case DETAIL: {
			if (currentImage==0) {
				return;
			}
			currentImage -= 1;
			iw.updateControls();
			return;
		}
		case SPOT: {
			if (currentImage==0) {
				return;
			}
			currentImage -= 1;
			iw.updateControls();
			return;
		}
		default:
			throw new RuntimeException("invalid overview mode "+overviewMode);
		}
	}

	
	public void right(ImageWindow iw) {
		switch (overviewMode) {
		case PATH: {
			if (currentCometPath==cometPaths.size()-1) {
				return;
			}
			currentCometPath += 1;
			currentImage = 0;
			iw.updateControls();
			return;
		}
		case DETAIL: {
			if (currentImage==getCurrentCometPath().getLength()-1) {
				return;
			}
			currentImage += 1;
			iw.updateControls();
			return;
		}
		case SPOT: {
			if (currentImage==getCurrentCometPath().getLength()-1) {
				return;
			}
			currentImage += 1;
			iw.updateControls();
			return;
		}
		default:
			throw new RuntimeException("invalid overview mode "+overviewMode);
		}
	}

	
	@Override
	public void switchMode(ImageWindow iw) {
		switch (overviewMode) {
		case PATH:
			overviewMode = MODE.DETAIL;
			break;
		case DETAIL:
			overviewMode = MODE.SPOT;
			break;
		case SPOT:
			overviewMode = MODE.PATH;
			break;
		default:
			throw new RuntimeException("invalid overview mode "+overviewMode);
		}
		iw.updateControls();
	}

	@Override
	public void info(ImageWindow iw) {
		showInfo = !showInfo;
		iw.updateControls();
	}


}
