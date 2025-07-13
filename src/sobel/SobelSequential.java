package sobel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

public class SobelSequential {

    public static BufferedImage process(File inputFile) throws IOException {
        BufferedImage inputImage = ImageIO.read(inputFile);

        BufferedImage grayImage = toGrayscale(inputImage);
        BufferedImage edgeImage = applySobel(grayImage);

        return edgeImage;
    }

    public static BufferedImage toGrayscale(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();

        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = gray.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int grayVal = (int)(0.3 * r + 0.59 * g + 0.11 * b);
                raster.setSample(x, y, 0, grayVal);
            }
        }
        return gray;
    }

    public static BufferedImage applySobel(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster in = gray.getRaster();
        WritableRaster out = output.getRaster();

        int[][] gx = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };

        int[][] gy = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int sumX = 0;
                int sumY = 0;

                for (int j = -1; j <= 1; j++) {
                    for (int i = -1; i <= 1; i++) {
                        int val = in.getSample(x + i, y + j, 0);
                        sumX += gx[j + 1][i + 1] * val;
                        sumY += gy[j + 1][i + 1] * val;
                    }
                }

                int mag = Math.min(255, Math.abs(sumX) + Math.abs(sumY));
                out.setSample(x, y, 0, mag);
            }
        }

        return output;
    }
}
