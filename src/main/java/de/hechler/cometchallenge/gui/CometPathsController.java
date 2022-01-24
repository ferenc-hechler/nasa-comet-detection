package de.hechler.cometchallenge.gui;

import java.awt.image.BufferedImage;
import java.util.List;

import de.hechler.cometchallenge.CometPath;
import de.hechler.cometchallenge.CometPos;
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
