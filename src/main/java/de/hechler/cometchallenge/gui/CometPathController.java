package de.hechler.cometchallenge.gui;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;

import de.hechler.cometchallenge.CometPath;
import de.hechler.cometchallenge.CometPos;
import de.hechler.cometchallenge.analyze.ImageAnalyzer;
import de.hechler.cometchallenge.analyze.SequenceAnalyzer;
import de.hechler.cometchallenge.geometry.Pos;
import de.hechler.cometchallenge.utils.Utils;

public class CometPathController implements ImageController {

	private SequenceAnalyzer analyzer;
	
	private CometPath cometPath;
	private int currentImage;
	
	private boolean overviewMode;

	public CometPathController(SequenceAnalyzer analyzer, CometPath cometPath) {
		this.analyzer = analyzer;
		this.cometPath = cometPath;
		this.overviewMode = false;
		this.currentImage = 0;
	}
	
	public boolean hasLeft() {
		return currentImage > 0;
	}
	
	public boolean hasRight() {
		return currentImage < cometPath.getLength()-1;
	}

	public String getCurrentInfo() {
		return (currentImage+1) + "/" + cometPath.getLength();
	}

	public BufferedImage getCurrentImage() {
		if (overviewMode) {
			return getOverviewImage();
		}
		return getDetailImage();
	}
	
	public BufferedImage getOverviewImage() {
		BufferedImage bi = Utils.createSpots(cometPath, 2.0);
		return bi;
	}
	
	public BufferedImage getDetailImage() {
		CometPos cp = cometPath.getCometPosition(currentImage);
		ImageAnalyzer ia = analyzer.getImageAnalyzerForTimestamp(cp.getTimestamp());
		int x = (int)cp.getPosition().getX();
		int y = (int)cp.getPosition().getY();
		int delta = 10;
		BufferedImage bi = ia.createBufferedImage(x-delta,y-delta, x+delta, y+delta);
		bi = Utils.scale(bi, 10.0);
		return bi;
	}
	

	public void left(ImageWindow iw) {
		if (currentImage==0) {
			return;
		}
		currentImage -= 1;
		iw.updateControls();
	}

	
	public void right(ImageWindow iw) {
		if (currentImage==cometPath.getLength()-1) {
			return;
		}
		currentImage += 1;
		iw.updateControls();
	}

	@Override
	public void switchMode(ImageWindow iw) {
		overviewMode = !overviewMode;
		iw.updateControls();
	}

	@Override public void info(ImageWindow iw) {}
	@Override public void special(ImageWindow iw) {}

}
