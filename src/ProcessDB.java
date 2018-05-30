/*
 * Author: Erik H
 * Class: CMPT450
 * Project
 */

import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.awt.*;

/**
 * Process input images, resizing them to be square while also calculating the
 * average RGB values of 4 square regions.
 */
public class ProcessDB {
	/**
	 * Prepare to process the given input images into the given output directory.
	 * @param in the input directory of images
	 * @param out the output directory where the DB image blobs will be placed
	 * @throws IllegalArgumentException if the given input directory is not a directory
	 */
	public ProcessDB(String in, String out, int width) throws IllegalArgumentException {
		//
		// Create a File for the input directory, ensuring that it actually is
		// a directory.
		//
		File inputDir = new File(in);
		if (!inputDir.exists()) {
			throw new IllegalArgumentException(in + " does not exist.");
		}

		//
		// Create the output directory where are DBImage blobs will be stored
		//
		new File(out).mkdir();

		//
		// We will write out all the database entries and their average RGB
		// regions to a file. This will make using the DB images faster/easier
		// later on in PhotoMosaic.java.
		//
		PrintWriter csvWriter = null;
		String dbCsvName = out + "/db" + width + "x" + width + ".csv";
		try {
			if (inputDir.exists() && !inputDir.isDirectory()) {
				// Append to the DB file if it already exists
				csvWriter = new PrintWriter(new FileOutputStream(new File(dbCsvName), true));
			}
			else {
				// The DB file doesn't exist, so we'll create it
				csvWriter = new PrintWriter(new File(dbCsvName));
			}

		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Error: unable to open/write to: " + dbCsvName);
		}

		//
		// Allow the user to have either provided a directory of images,
		// or a single image to add.
		//
		File[] inputImages;
		if (!inputDir.isDirectory()) {
			inputImages = new File[] { inputDir };
		}
		else {
			inputImages = inputDir.listFiles();
		}

		//
		// Process each image, resizing it and storing its average RGB values
		//
		for (File f : inputImages) {
			// Get the basename of the input image
			String inputBasename = f.getName().substring(0, f.getName().lastIndexOf("."));
			// Construct the output file path
			String outName = out + "/" + width + "x" + width + "_" + inputBasename + ".jpg";
			if (new File(outName).exists()) {
				System.err.println("[WARN] skipping already existing DB file: " + outName);
				continue;
			}
			BufferedImage image = null;
			try {
				image = ImageIO.read(f);
			}
			catch (Exception e) {
				System.err.println("[WARN] error reading image file: " + f.getName());
				e.printStackTrace();
				continue;
			}
			if (image == null) {
				System.err.println("[WARN] skipping non-image file: " + f.getName());
				continue;
			}

			// Resize the image to be square
			Image scaled = image.getScaledInstance(width, width, Image.SCALE_SMOOTH);
			BufferedImage dbBufferedImage = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
			// Draw the scaled image onto our DBImage
			dbBufferedImage.getGraphics().drawImage(scaled, 0, 0, null);

			//
			// Write the DB image to a file
			//
			try {
				writeDbJpg(outName, width, dbBufferedImage);
			}
			catch (Exception e) {
				System.err.println("[WARN] error writing processed image file: " + outName);
				System.err.println("\tSkipping...");
				e.printStackTrace();
				continue;
			}

			//
			// Write out the database entry to the CSV file.
			//
			// The first column is the resized filename, and the following
			// columns are the average RGB values of 4 square regions of the
			// patch image.
			//
			Color[] averageRgbs = PhotoMosaic.averageRegions(dbBufferedImage);
			StringBuilder sb = new StringBuilder(outName + ",");
			for (int i = 0; i < averageRgbs.length; i++) {
				sb.append(averageRgbs[i].getRGB());
				if (i != averageRgbs.length-1) {
					sb.append(",");
				}
			}

			csvWriter.println(sb.toString());
			csvWriter.flush();
		}
		csvWriter.close();
	}

	/**
	 * Write out a resized database image to a file.
	 * @param outName the output filename
	 * @param width the width of the image
	 * @param dbBufferedImage the BufferedImage instance for the resized image
	 */
	private void writeDbJpg(String outName, int width, BufferedImage dbBufferedImage) throws IOException {
		//
		// Write out the scaled image to a file
		//
		try {
			ImageIO.write(dbBufferedImage, "jpg", new File(outName));
		}
		catch (IOException e) {
			throw e;
		}
		System.out.println("[INFO] successfully created: " + outName);
	}

	public static void main(String[] args) {
		final String DBOUTPUTDIR = "./db";
		if (args.length != 2) {
			System.err.println("Usage: java ProcessDB <directory with images to put in DB> <downscale size>");
			System.err.println("e.g. java ProcessDB ./input/ 32");
			System.err.println("^-- This would process the images in ./input/ and resize them to 32x32.");
			System.err.println("    A CSV file '" + DBOUTPUTDIR + "/db32x32.csv' is created that will be used by the PhotoMosaic program.");
			System.exit(1);
		}
		ProcessDB pdb = new ProcessDB(args[0], DBOUTPUTDIR, Integer.parseInt(args[1]));
	}
}
