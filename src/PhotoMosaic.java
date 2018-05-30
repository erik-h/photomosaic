/*
 * Author: Erik H
 * Class: CMPT450
 * Project
 */

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.awt.*;
import java.util.*;

/**
 * Create a photomosaic given an input image and a database of patch images.
 */
public class PhotoMosaic {
	/**
	 * The input image, edited in place to become the photomosaic image.
	 */
	private BufferedImage image;
	/**
	 * A cache for database images, allowing us to not have to keep re-reading
	 * images into BufferedImage objects.
	 * This saves a small amount of processing time, but it is not extreme.
	 */
	private Map<String, BufferedImage> dbCache;
	/**
	 * An array representing the image as a set of database image patches.
	 * This array is used in determening whether a given patch has already
	 * been used in a region.
	 */
	private String[][] placedTiles;
	/**
	 * The size for the database patches.
	 * This value will be used in loading the corresponding "dbNxN.csv" file
	 * that represents the database.
	 */
	private int patchSize;
	/**
	 * The box size to use in searching for duplicate patches.
	 * For each patch, a box of this size around the patch will be searched to
	 * see if a candidate database image has already been used in the area.
	 */
	private int uniqueBox;
	/**
	 * The default uniqueBox size.
	 */
	public static final int DEFAULT_UNIQUE_BOX = 21;
	public static final String SCALED_INPUT_PREFIX = "SCALED_ORIGINAL_";
	public static final String MOSAIC_OUTPUT_PREFIX = "MOSAIC_OUTPUT_";

	/**
	 * Create a photomosaic for the given image using the given database of images, using the default unique box size.
	 * @param filename the file we'll create a photomosaic of
	 * @param width the width to scale the input image to
	 * @param dbFilePath the path to the DB csv file
	 * @param patchSize the square DB image width
	 * @throws IOException if something went wrong while reading the image
	 * @throws IllegalArgumentException if the provided filename is not a valid image or the DB file doesn't exist
	 */
	public PhotoMosaic(String filename, int width, String dbFilePath, int patchSize) throws IOException, IllegalArgumentException {
		this(filename, width, dbFilePath, patchSize, DEFAULT_UNIQUE_BOX);
	}

	/**
	 * Create a photomosaic for the given image using the given database of images.
	 * @param filename the file we'll create a photomosaic of
	 * @param width the width to scale the input image to
	 * @param dbFilePath the path to the DB csv file
	 * @param patchSize the square DB image width
	 * @param uniqueBox the box size to use when searching for duplicates
	 * @throws IOException if something went wrong while reading the image
	 * @throws IllegalArgumentException if the provided filename is not a valid image or the DB file doesn't exist
	 */
	public PhotoMosaic(String filename, int width, String dbFilePath, int patchSize, int uniqueBox) throws IOException, IllegalArgumentException {
		this.patchSize = patchSize;
		this.uniqueBox = uniqueBox;

		//
		// Read in the original image
		//
		try {
			image = ImageIO.read(new File(filename));
		}
		catch (IOException e) {
			throw e;
		}
		if (image == null) {
			throw new IllegalArgumentException(filename + " is not a valid image.");
		}

		// Set up the tile representation of our image; this will let us
		// determine if a tile has already been placed in a region
		placedTiles = new String[width/patchSize][width/patchSize];

		// Resize the input image to width x width
		image = scaleImage(image, width);

		//
		// Write out the scaled image to a file
		//
		String fileBaseName = new File(filename).getName();
		String outName = SCALED_INPUT_PREFIX + fileBaseName;
		ImageIO.write(image, "jpg", new File(outName));

		//
		// Load up the database image paths into memory
		//
		java.util.List<DBImage> dbImages = loadDbImages(dbFilePath);
		// Set up a map for caching BufferedImage instances for each database
		// image
		dbCache = new HashMap<String, BufferedImage>();

		//
		// Loop through each patchSize by patchSize region of the original image.
		// We will swap out each of these regions with the most appropriate
		// database image.
		//
		for (int j = 0; j < image.getHeight(); j+=patchSize) {
			for (int i = 0; i < image.getWidth(); i+=patchSize) {
				System.out.print("."); // Print a dot to show that something's happening
				BufferedImage region = image.getSubimage(
					i, j, patchSize, patchSize
				);

				// Find the closest matching DB image for this region
				String closestMatch = findClosest(region, dbImages, i/patchSize, j/patchSize);

				// We've placed the tile down, so update the array representing
				// the placed down tiles.
				placedTiles[i/patchSize][j/patchSize] = closestMatch;

				// The patch that we will paste into our original image
				BufferedImage closestMatchImg = null;
				if (!dbCache.containsKey(closestMatch)) {
					// We don't have a BufferedImage cached for this image,
					// so let's load up a new instance.
					closestMatchImg = ImageIO.read(new File(closestMatch));
					dbCache.put(closestMatch, closestMatchImg);
				}
				else {
					// We have a cached BufferedImage for this DB image, so use it.
					closestMatchImg = dbCache.get(closestMatch);
				}

				//
				// Turn the closest matching database image into an array of RGB values
				//
				int[] closestRGBs = closestMatchImg.getRGB(
					0, 0, closestMatchImg.getWidth(), closestMatchImg.getHeight(),
					null, 0, closestMatchImg.getWidth()
				);

				//
				// Apply the DB image to the patch on the original image
				//
				image.setRGB(
					i, j, patchSize, patchSize, closestRGBs, 0, patchSize
				);
			}
		}
		System.out.println();

		ImageIO.write(image, "jpg", new File(MOSAIC_OUTPUT_PREFIX + fileBaseName));
	}

	/**
	 * Find the closest matching image to the given image region.
	 * @param region the image to match a database image to
	 * @param dbImages the list of database images we will test against
	 * @param x the x coordinate of our patch in "patch space"
	 * @param y the y coordinate of our patch in "patch space"
	 */
	private String findClosest(BufferedImage region, java.util.List<DBImage> dbImages, int x, int y) {
		// Default the closest distance to be impossibly large
		double closestDistance = Double.POSITIVE_INFINITY;
		DBImage closestDbImage = null;
		for (DBImage dbImage : dbImages) {
			// We grab the DB image's already calculated average RGBs and
			// compare them with the average regions of the current section
			// we're looking at.
			double currentDistance = regionDistance(dbImage.getAverageRGBs(), averageRegions(region)) * uniquenessScore(dbImage.getImagePath(), x, y);

			// Set this image as the new closest if it has the smallest distance
			if (currentDistance < closestDistance) {
				closestDistance = currentDistance;
				closestDbImage = dbImage;
			}
		}

		// Return the path to the closest matching image file
		return closestDbImage.getImagePath();
	}

	/**
	 * Determine whether a given location in "patch space" is valid.
	 * @param x the x coordinate in "patch space"
	 * @param y the y coordinate in "patch space"
	 * @return if the location is valid
	 */
	private boolean validNeighbour(int x, int y) {
		return x >= 0 && y >= 0 && x < placedTiles.length && y < placedTiles[x].length;
	}

	/**
	 * Determine how unique a database image is in a certain region.
	 * @param imagePath the image whose uniqueness we want to check
	 * @param tileX the x coordinate in "patch space"
	 * @param tileY the y coordinate in "patch space"
	 * @return the double score representing the uniqueness of the imagePath
	 */
	private double uniquenessScore(String imagePath, int tileX, int tileY) {
		int occurances = 0;
		for (int y = -uniqueBox/2; y <= uniqueBox/2; y++) {
			for (int x = -uniqueBox/2; x <= uniqueBox/2; x++) {
				if (!validNeighbour(tileX+x, tileY+y)) {
					continue;
				}
				else if (placedTiles[tileX+x][tileY+y] != null && placedTiles[tileX+x][tileY+y].equals(imagePath)) {
					occurances++;
				}
			}
		}
		return 1.0 + 0.5*occurances;
		//
		// DEBUG: Use the line below to nullify the effect of this method and just
		// repeat images all over
		//
		// return 1.0;
	}

	/**
	 * Determine the distance between two images in 12 dimensional space.
	 * The 12 dimensions come from the 4 RGB tuples representing the average RGB
	 * values in 4 square regions of the image.
	 * @param first the first array of 4 RGB tuples
	 * @param second the second array of 4 RGB tuples
	 */
	private double regionDistance(Color[] first, Color[] second) {
		// This should never happen but it's included incase something messes
		// up in the future.
		if (first.length != second.length) {
			System.err.println("[FATAL] sanity check failed; comparing two different length sets of tuples!");
			System.exit(1);
		}

		//
		// Sum up all of the squared differences for all 12 dimensions to attain
		// the distance between `first` and `second`.
		//
		double distance = 0;
		for (int i = 0; i < first.length; i++) {
			distance +=
				Math.pow(first[i].getRed()-second[i].getRed(), 2) +
				Math.pow(first[i].getGreen()-second[i].getGreen(), 2) +
				Math.pow(first[i].getBlue()-second[i].getBlue(), 2);
		}

		return distance;
	}

	/**
	 * Get the average colours for four square regions of a given image.
	 * @param image the input image
	 * @return an int array with the four regions' average colours
	 */
	public static Color[] averageRegions(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		//
		// Get the average colour for the 4 square regions
		//
		return new Color[] {
			calculateAverage(image, 0, 0, height/2, width/2),
			calculateAverage(image, 0, width/2, height/2, width),
			calculateAverage(image, height/2, 0, height, width/2),
			calculateAverage(image, height/2, width/2, height, width)
		};
	}

	/**
	 * Get the average colour for a specified region of an image.
	 * @param image the input image
	 * @param xStart the starting x pixel location
	 * @param xEnd the ending x pixel location
	 * @param yStart the starting y pixel location
	 * @param yEnd the ending y pixel location
	 * @return the average colour for the region
	 */
	private static Color calculateAverage(BufferedImage image, int yStart, int xStart, int yEnd, int xEnd) {
		int totalPixels = (yEnd-yStart)*(xEnd-xStart); // The total number of pixels in this region

		int totalRed = 0;
		int totalGreen = 0;
		int totalBlue = 0;
		// Sum all of the red, green, and blue values in the region separately
		for (int y = yStart; y < yEnd; y++) {
			for (int x = xStart; x < xEnd; x++) {
				Color c = new Color(image.getRGB(x, y));
				totalRed += c.getRed();
				totalGreen += c.getGreen();
				totalBlue += c.getBlue();
			}
		}

		// Return the average colour
		return new Color(totalRed/totalPixels, totalGreen/totalPixels, totalBlue/totalPixels);
	}

	/**
	 * Scale an image to the given square dimensions.
	 * @param image the image to scale
	 * @param width the image is scaled so its height and width equal this value
	 * @return the scaled image
	 */
	private BufferedImage scaleImage(BufferedImage image, int width) {
		// Scale the image
		Image scaled = image.getScaledInstance(width, width, Image.SCALE_SMOOTH);
		// Create a new image to plop the scaled version on to
		BufferedImage newImage = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
		// Draw the scaled image onto the new image
		newImage.getGraphics().drawImage(scaled, 0, 0, null);

		return newImage;
	}

	/**
	 * Load up references to the database images specified in the given CSV file.
	 * @param dbFilePath the path to the CSV file containing database entries
	 * @throws IllegalArgumentException if the provided path doesn't exist, or could not be loaded
	 */
	private java.util.List<DBImage> loadDbImages(String dbFilePath) throws IllegalArgumentException {
		// The number of entries on each line of the CSV file
		final int NUMENTRIES = 5;
		// Load up the database file
		File dbFile = new File(dbFilePath);
		if (!dbFile.exists()) {
			throw new IllegalArgumentException(dbFilePath + " does not exist.");
		}

		//
		// Iterate over each line of the database CSV, populating a List with
		// the entries.
		//
		java.util.List<DBImage> dbImages = new ArrayList<DBImage>();
		Scanner scan = null;
		try {
			scan = new Scanner(dbFile);
		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Could not find DB file: " + dbFilePath);
		}
		while (scan.hasNextLine()) {
			String[] entry = scan.nextLine().split(",");
			if (entry.length != NUMENTRIES) {
				// Skip lines that don't have the right number of entries (columns)
				System.err.println("[WARN] invalid entry: " + entry + ". Skipping...");
				continue;
			}

			Color[] averageRGBs = new Color[] {
				// Entries 1 through 4 are the average RGB values for the 4 regions
				new Color(Integer.parseInt(entry[1])),
				new Color(Integer.parseInt(entry[2])),
				new Color(Integer.parseInt(entry[3])),
				new Color(Integer.parseInt(entry[4])),
			};

			// Add the new DB image.
			// Entry 0 is the path to the image.
			dbImages.add(new DBImage(entry[0], averageRGBs));
		}

		return dbImages;
	}

	/**
	 * Create a photomosaic given an input image and some parameters.
	 * @param args is used for the input image path, input downscale size, patch size for DB images, and unique box search size
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			// The input image path, downscale size, and patch size are required
			System.err.println("Usage java PhotoMosaic <input image> <size to downscale input image> <patch size for DB images> [unique box size (default: 21)]");
			System.err.println("e.g. java PhotoMosaic ./img/schnauzer.jpg 512 8 11");
			System.exit(1);
		}

		int imageSize = Integer.parseInt(args[1]);
		int patchSize = Integer.parseInt(args[2]);
		try {
			if (args.length >= 4) {
				// If the user provided a unique box search value then use it
				int uniqueBox = Integer.parseInt(args[3]);
				PhotoMosaic pm = new PhotoMosaic(args[0], imageSize, "./db/db"+patchSize+"x"+patchSize+".csv", patchSize, uniqueBox);
			}
			else {
				// The user didn't provide a unique box size, so we'll just let PhotoMosaic use its default
				PhotoMosaic pm = new PhotoMosaic(args[0], imageSize, "./db/db"+patchSize+"x"+patchSize+".csv", patchSize);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
