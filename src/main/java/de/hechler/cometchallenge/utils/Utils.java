package de.hechler.cometchallenge.utils;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import de.hechler.cometchallenge.CometPath;
import de.hechler.cometchallenge.geometry.Pos;

public class Utils {
	
	private Utils() {}
	
	public static void sleep() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
	
	public static List<String> readAllLines(Path file) {
		try {
			return Files.readAllLines(file);
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}
	}
		
	public static BufferedImage scale(BufferedImage bi, double factor) {
		if (factor == 0.0) {
			return bi;
		}
		BufferedImage result = new BufferedImage((int)(bi.getWidth()*factor), (int)(bi.getHeight()*factor), BufferedImage.TYPE_BYTE_GRAY);
		AffineTransform at = new AffineTransform();
		at.scale(factor, factor);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		result = scaleOp.filter(bi, result);
		return result;
	}

	public static BufferedImage createSpots(CometPath cometPath, double scale) {
		int[] gray = new int[1];
		int imgWH = (int)(1024/scale);
		BufferedImage image = new BufferedImage(imgWH, imgWH, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image.getRaster();
		gray[0] = 0;
		for (int y=0; y<imgWH; y++) {
			for (int x=0; x<imgWH; x++) {
				raster.setPixel(x, y, gray);
			}
		}
		gray[0] = 255;
		for (int i=0; i<cometPath.getLength(); i++) {
			Pos spot=cometPath.getCometPosition(i).getPosition();
			raster.setPixel((int)(spot.getX()/scale), (int)(spot.getY()/scale), gray);
		}
		return image;
	}

	public static BufferedImage createSpots(List<Pos> spots, double scale) {
		int[] gray = new int[1];
		int imgWH = (int)(1024/scale);
		BufferedImage image = new BufferedImage(imgWH, imgWH, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image.getRaster();
		gray[0] = 0;
		for (int y=0; y<imgWH; y++) {
			for (int x=0; x<imgWH; x++) {
				raster.setPixel(x, y, gray);
			}
		}
		gray[0] = 255;
		for (Pos spot:spots) {
			raster.setPixel((int)(spot.getX()/scale), (int)(spot.getY()/scale), gray);
		}
		return image;
	}




}
