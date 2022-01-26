package de.hechler.cometchallenge.gui;

import java.awt.image.BufferedImage;
import java.util.List;

public class ImageListController implements ImageController {

	private List<BufferedImage> images;
	private int currentImage;

	public ImageListController(List<BufferedImage> images) {
		this.images = images;
		currentImage = 0;
	}
	
	public boolean hasLeft() {
		return currentImage > 0;
	}
	
	public boolean hasRight() {
		return currentImage < images.size()-1;
	}

	public String getCurrentInfo() {
		return currentImage + "/" + images.size();
	}

	public BufferedImage getCurrentImage() {
		return images.get(currentImage);
	}
	

	public void left(ImageWindow iw) {
		if (currentImage==0) {
			return;
		}
		currentImage -= 1;
		iw.updateControls();
	}

	
	public void right(ImageWindow iw) {
		if (currentImage==images.size()-1) {
			return;
		}
		currentImage += 1;
		iw.updateControls();
	}

	@Override public void switchMode(ImageWindow iw) {}
	@Override public void info(ImageWindow iw) {}
	@Override public void special(ImageWindow iw) {}



}
