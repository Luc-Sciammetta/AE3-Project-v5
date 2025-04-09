package uk.ac.nulondon;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class Image {
    private final List<Pixel> rows;

    private int width;
    private int height;

    public Image(BufferedImage img) {
        width = img.getWidth();
        height = img.getHeight();
        rows = new ArrayList<>();
        Pixel current = null;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Pixel pixel = new Pixel(img.getRGB(col, row));
                if (col == 0) {
                    rows.add(pixel);
                } else {
                    current.right = pixel;
                    pixel.left = current;
                }
                current = pixel;
            }
        }
    }

    public BufferedImage toBufferedImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < height; row++) {
            Pixel pixel = rows.get(row);
            int col = 0;
            while (pixel != null) {
                image.setRGB(col++, row, pixel.color.getRGB());
                pixel = pixel.right;
            }
        }
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Find the energy for the pixel
     * @param above the above pixel
     * @param current the current pixel
     * @param below the below pixel
     * @return the energy of the seam
     */
    double energy(Pixel above, Pixel current, Pixel below) {
        if(above == null || below == null || current.left == null || current.right == null) {
            return current.brightness();
        }

        double a = above.left.brightness();
        double b = above.brightness();
        double c = above.right.brightness();

        double d = current.left.brightness();
        double f = current.right.brightness();

        double g = below.left.brightness();
        double h = below.brightness();
        double i = below.right.brightness();

        // find the horizontal energy
        double HorizEnergy = (a + (2 * (d)) + g) - (c + (2 * (f)) + i);
        // find the vertical energy
        double VertEnergy = (a + (2 * (b)) + c) - (g + (2 * (h)) + i);
        // find and return the energy
        return Math.sqrt(HorizEnergy*HorizEnergy + VertEnergy*VertEnergy);
    }

    /**
     * calculates the energy for the pixel
     */
    public void calculateEnergy() {
        for (int row = 0; row < rows.size(); row++){
            Pixel above = null;
            Pixel below = null;
            if(row != 0){ //we then change the value of above and below if we are not at the top or bottom of the image
                 above = rows.get(row - 1);
            }
            if (row != rows.size() - 1){
                below = rows.get(row + 1);
            }
            Pixel current = rows.get(row);
            while (current != null){
                current.energy = energy(above, current, below); //set the energy of the pixel
                //change the above, below, and current to be the next pixel
                if (above != null){
                    above = above.right;
                }
                if (below != null){
                    below = below.right;
                }
                current = current.right;
            }
        }
    }

    /**
     * highlights the seam
     * @param seam the seam to highlight
     * @param color the color to highlight it
     * @return return the original seam
     */
    public List<Pixel> higlightSeam(List<Pixel> seam, Color color) {
        List<Pixel> originalSeam = new ArrayList<>(); //deep copy here
        for (Pixel pixel : seam) {
            Pixel newPixel = new Pixel(pixel.color);
            newPixel.left = pixel.left;
            newPixel.right = pixel.right;
            originalSeam.add(newPixel);
        }

        int row = height - 1;
        for (Pixel pixel : seam) {
            Pixel newPixel = new Pixel(color);
            if (pixel.left != null) {
                pixel.left.right = newPixel;
                newPixel.left = pixel.left;
            } else {
                // left most pixel
                rows.set(row, newPixel);
            }
            if (pixel.right != null) {
                pixel.right.left = newPixel;
                newPixel.right = pixel.right;
            }
            row--;
        }
        return originalSeam;
    }

    /**
     * removes the seam given
     * @param seam the given seam
     */
    public void removeSeam(List<Pixel> seam) {
        width--;
        int row = height - 1;
        for(Pixel pixel : seam){
            if(pixel.left != null){
                pixel.left.right = pixel.right; // jump the pixel from the left
            } else {
                // left most pixel
                rows.set(row, pixel.right);
            }
            if(pixel.right != null){
                pixel.right.left = pixel.left; // jump the pixel from the right
            }
            row--;
        }
    }

    /**
     * adds the current seam
     * @param seam to add
     */
    public void addSeam(List<Pixel> seam) {
        List<Pixel> newSeam = seam.reversed(); //reverse the seam because the seam comes in reversed
        int row = 0;
        for (Pixel pixel : newSeam){
            if (pixel.left != null){
                pixel.left.right = pixel;
            }else{
                rows.set(row, pixel); //if the pixel needs to be added to be the front of the linked list of pixels
            }
            if (pixel.right != null){
                pixel.right.left = pixel;
            }

            row++;
        }
        width++;
    }

    /**
     * Finds the max seam for the function
     * @param valueGetter used to find the value for the pixel
     * @return returns the seam found
     */
    private List<Pixel> getSeamMaximizing(Function<Pixel, Double> valueGetter) {
        double[] previousValues = new double[width];
        double[] currentValues = new double[width];

        // Lists to store the seam paths for the previous and current row
        List<List<Pixel>> previousSeams = new ArrayList<>();
        List<List<Pixel>> currentSeams = new ArrayList<>();

        // Start processing from the first row
        Pixel currentPixel = rows.getFirst();
        int col = 0;
        // Initialize the first row values and corresponding seams
        while (currentPixel != null){
            previousValues[col] = valueGetter.apply(currentPixel);
            previousSeams.add(List.of(currentPixel));
            currentPixel = currentPixel.right;
            col++;
        }

        // Process all rows to compute the max-value seams
        for (int row = 1; row < height; row++){
            currentPixel = rows.get(row); // get the first pixel of the current row
            col = 0;
            while (currentPixel != null){
                double max = previousValues[col]; // find the above value
                int ref = col;

                // Check the left diagonal pixel
                if(col > 0 && previousValues[col-1] > max){
                    max = previousValues[col - 1];
                    ref = col - 1;
                }

                // Check the right diagonal pixel
                if(col < width - 1 && previousValues[col+1] > max){
                    max = previousValues[col+1];
                    ref = col + 1;
                }

                // Update the current pixel's value with the max sum path so far
                currentValues[col] = max + valueGetter.apply(currentPixel);

                // Store the best seam path leading to this pixel
                currentSeams.add(concat(currentPixel, previousSeams.get(ref)));

                // Move to the next pixel in the row
                col++;
                currentPixel = currentPixel.right;
            }

            // Prepare for the next row by updating references
            previousValues = currentValues;
            currentValues = new double[width];
            previousSeams = currentSeams;
            currentSeams = new ArrayList<>();
        }

        // find the seam with the max value
        double maxVal = previousValues[0];
        int maxValIndex = 0;

        for(int i = 0 ; i < width ; i++){
            if(previousValues[i] > maxVal){
                maxVal = previousValues[i];
                maxValIndex = i;
            }
        }

        // Return the seam with the highest total value
        return previousSeams.get(maxValIndex);
    }

    /**
     * concat to append the pixel to start of the seam
     * @param currentPixel the pixel to add
     * @param previousSeams the old seam
     * @return new seam
     */
    public List<Pixel> concat(Pixel currentPixel, List<Pixel> previousSeams){
        // create a new Seams
        List<Pixel> newSeams = new ArrayList<>();
        // add current pixel to the front
        newSeams.add(currentPixel);
        // add all values of the previous seam
        newSeams.addAll(previousSeams);
        return newSeams;
    }


    public List<Pixel> getGreenestSeam() {
        return getSeamMaximizing(Pixel::getGreen);
        /*Or, since we haven't lectured on lambda syntax in Java, this can be
        return getSeamMaximizing(new Function<Pixel, Double>() {
            @Override
            public Double apply(Pixel pixel) {
                return pixel.getGreen();
            }
        });*/

    }

    public List<Pixel> getLowestEnergySeam() {
        calculateEnergy();
        /*
        Maximizing negation of energy is the same as minimizing the energy.
         */
        return getSeamMaximizing(pixel -> -pixel.energy);

        /*Or, since we haven't lectured on lambda syntax in Java, this can be
        return getSeamMaximizing(new Function<Pixel, Double>() {
            @Override
            public Double apply(Pixel pixel) {
                return -pixel.energy;
            }
        });
        */
    }
}
