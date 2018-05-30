/*
 * Author: Erik H
 * Class: CMPT450
 * Project
 */

import java.awt.image.*;
import java.io.Serializable;
import java.awt.Color;

/**
 * A database image used for creating photomosaics.
 * This class contains a path to a database image, as well as 4 tuples
 * representing the average RGB values for 4 square regions of the image.
 */
public class DBImage {
	/**
	 * The average RGB values for 4 square regions of the image.
	 */
	private Color[] averageRGBs;
	/**
	 * The path to the image file that this object is representing.
	 */
	private String imagePath;

	/**
	 * Represent a database image given a path and average RGB values.
	 */
	public DBImage(String imagePath, Color[] averageRGBs) {
		this.imagePath = imagePath;
		this.averageRGBs = averageRGBs;
	}

	/**
	 * @return the average RGB values for the 4 square regions of this image
	 */
	public Color[] getAverageRGBs() {
		return averageRGBs;
	}

	/**
	 * @return the path to the image file represented by this object.
	 */
	public String getImagePath() {
		return imagePath;
	}
}
