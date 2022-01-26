package de.hechler.cometchallenge.gui;

import java.awt.image.BufferedImage;

public interface ImageController {

	public boolean hasLeft();
	public void left(ImageWindow iw);

	public boolean hasRight();
	public void right(ImageWindow iw);

	public void switchMode(ImageWindow iw);
	public void info(ImageWindow iw);

	public String getCurrentInfo();

	public BufferedImage getCurrentImage();
	
}
