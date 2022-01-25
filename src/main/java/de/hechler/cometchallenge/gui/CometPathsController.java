package de.hechler.cometchallenge.gui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import de.hechler.cometchallenge.CometPath;
import de.hechler.cometchallenge.CometPos;
import de.hechler.cometchallenge.MinMaxCounter;
import de.hechler.cometchallenge.analyze.ImageAnalyzer;
import de.hechler.cometchallenge.analyze.SequenceAnalyzer;
import de.hechler.cometchallenge.geometry.Pos;
import de.hechler.cometchallenge.utils.Utils;

public class CometPathsController implements ImageController {

	private SequenceAnalyzer analyzer;
	
	private List<CometPath> cometPaths;
	private int currentCometPath;
	private int currentImage;
	
	private enum MODE { PATH, DETAIL, SPOT }
	private MODE overviewMode;

	public CometPathsController(SequenceAnalyzer analyzer, List<CometPath> cometPaths) {
		this.analyzer = analyzer;
		this.cometPaths = cometPaths;
		this.overviewMode = MODE.PATH;
		this.currentImage = 0;
		this.currentCometPath = 0;
	}

	private CometPath getCurrentCometPath() {
		return cometPaths.get(currentCometPath);
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
		BufferedImage bi = Utils.createSpots(getCurrentCometPath(), 1.0);
		return bi;
	}
	
	public BufferedImage getDetailImage() {
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
		ImageAnalyzer ia = analyzer.getImageAnalyzerForTimestamp(getCurrentCometPath().getCometPosition(currentImage).getTimestamp());
		BufferedImage bi = Utils.createSpots(ia.getSpots(), 1.0);
		return bi;
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

}
