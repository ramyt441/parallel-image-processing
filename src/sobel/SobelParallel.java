package sobel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

public class SobelParallel {

    private static class SobelTask extends RecursiveAction {
        private final WritableRaster in;
        private final WritableRaster out;
        private final int yStart;
        private final int yEnd;
        private final int width;
        private final int threshold;

        public SobelTask(WritableRaster in, WritableRaster out, int yStart, int yEnd, int width, int threshold) {
            this.in = in;
            this.out = out;
            this.yStart = yStart;
            this.yEnd = yEnd;
            this.width = width;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            if (yEnd - yStart <= threshold) {
                processStrip();
            } else {
                int mid = yStart + (yEnd - yStart) / 2;
                SobelTask top = new SobelTask(in, out, yStart, mid, width, threshold);
                SobelTask bottom = new SobelTask(in, out, mid, yEnd, width, threshold);
                invokeAll(top, bottom);
            }
        }

        private void processStrip() {
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

            for (int y = Math.max(1, yStart); y < Math.min(yEnd, in.getHeight() - 1); y++) {
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
        }
    }

    public static BufferedImage process(BufferedImage inputImage, int parallelism) throws IOException {
        BufferedImage grayImage = SobelSequential.toGrayscale(inputImage);

        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster in = grayImage.getRaster();
        WritableRaster out = output.getRaster();

        int threshold = 2000; // large tiles for less overhead

        ForkJoinPool pool = new ForkJoinPool(parallelism);
        SobelTask mainTask = new SobelTask(in, out, 0, height, width, threshold);
        pool.invoke(mainTask);

        return output;
    }
}
