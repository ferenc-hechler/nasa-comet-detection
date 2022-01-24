package de.hechler.cometchallenge.gui;

import java.util.List;
import java.awt.image.BufferedImage;

public class ImageWindowUtils {

	private ImageWindowUtils() {}
	
	public static void showImages(String title, List<BufferedImage> images) {
		ImageController controller = new ImageListController(images);
		new ShowControlledImages(title, controller);
	}

}
