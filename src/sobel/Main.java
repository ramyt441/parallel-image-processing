package sobel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;

public class Main {
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Please specify a mode: single OR batch");
            return;
        }

        String mode = args[0].toLowerCase();

        if (mode.equals("single")) {
            runSingleImageMode(args);
        } else if (mode.equals("batch")) {
            runBatchMode();
        } else {
            System.out.println("Unknown mode. Use 'single' or 'batch'.");
        }
    }

    private static void runSingleImageMode(String[] args) throws Exception {
        System.out.println("--- SINGLE IMAGE MODE ---");

        String filename = args.length > 1 ? args[1] : "sample4K.jpg";
        File inputFile = new File("input/" + filename);
        String baseName = filename.replaceAll("\\.[^.]+$", "");

        BufferedImage inputImage = ImageIO.read(inputFile);
        System.out.println("Loaded image: " + filename +
                " [" + inputImage.getWidth() + " x " + inputImage.getHeight() + "]");

        System.out.println("Press Enter to start...");
        new Scanner(System.in).nextLine();

        // --- Sequential processing ---
        long t1 = System.nanoTime();
        BufferedImage graySeq = SobelSequential.toGrayscale(inputImage);
        BufferedImage resultSeq = SobelSequential.applySobel(graySeq);
        long t2 = System.nanoTime();
        double timeSeq = (t2 - t1) / 1_000_000_000.0;

        ImageIO.write(resultSeq, "png", new File("output/single_sequential_" + baseName + ".png"));

        System.out.printf("Sequential Time: %.3f sec%n", timeSeq);

        // --- Parallel processing ---
        int threads = 4;
        long t3 = System.nanoTime();
        BufferedImage resultPar = SobelParallel.process(inputImage, threads);
        long t4 = System.nanoTime();
        double timePar = (t4 - t3) / 1_000_000_000.0;

        ImageIO.write(resultPar, "png",
                new File(String.format("output/single_parallel_%dthreads_%s.png", threads, baseName)));

        double speedup = timeSeq / timePar;
        System.out.printf("Parallel Time: %.3f sec%n", timePar);
        System.out.printf("Speedup: %.2fx%n", speedup);

        System.out.println("\nPress Enter to exit...");
        new Scanner(System.in).nextLine();
    }

    private static void runBatchMode() throws Exception {
        System.out.println("--- BATCH MODE ---");

        List<String> filenames = List.of(
                "sample720.jpg",
                "sample2K.jpg",
                "sample4K.jpg",
                "sample8K.jpg"
        );

        int threads = 4;

        System.out.println("Starting parallel batch processing with " + threads + " threads.");
        System.out.println("Press Enter to begin...");
        new Scanner(System.in).nextLine();

        long tBatchStart = System.nanoTime();

        // Accumulate total times:
        final double[] totalSeqTime = {0.0};
        final double[] totalParTime = {0.0};

        ForkJoinPool pool = new ForkJoinPool(threads);

        pool.submit(() -> filenames.parallelStream().forEach(filename -> {
            try {
                System.out.println("Processing " + filename);

                File inputFile = new File("input/" + filename);
                BufferedImage inputImage = ImageIO.read(inputFile);

                String baseName = filename.replaceAll("\\.[^.]+$", "");

                // --- Sequential processing ---
                long t1 = System.nanoTime();
                BufferedImage graySeq = SobelSequential.toGrayscale(inputImage);
                BufferedImage resultSeq = SobelSequential.applySobel(graySeq);
                long t2 = System.nanoTime();
                double timeSeq = (t2 - t1) / 1_000_000_000.0;

                ImageIO.write(resultSeq, "png",
                        new File("output/batch_sequential_" + baseName + ".png"));

                // --- Parallel processing ---
                long t3 = System.nanoTime();
                BufferedImage resultPar = SobelParallel.process(inputImage, threads);
                long t4 = System.nanoTime();
                double timePar = (t4 - t3) / 1_000_000_000.0;

                ImageIO.write(resultPar, "png",
                        new File("output/batch_parallel_" + baseName + ".png"));

                double speedup = timeSeq / timePar;

                // Accumulate totals
                synchronized (totalSeqTime) {
                    totalSeqTime[0] += timeSeq;
                    totalParTime[0] += timePar;
                }

                System.out.printf("Done: %-15s | Seq: %.3f s | Par: %.3f s | Speedup: %.2fx%n",
                        filename, timeSeq, timePar, speedup);

            } catch (Exception e) {
                e.printStackTrace();
            }
        })).get();

        long tBatchEnd = System.nanoTime();
        double batchElapsed = (tBatchEnd - tBatchStart) / 1_000_000_000.0;

        double overallSpeedup = totalSeqTime[0] / totalParTime[0];

        System.out.printf("\nTotal Sequential Time: %.3f seconds\n", totalSeqTime[0]);
        System.out.printf("Total Parallel Time:   %.3f seconds\n", totalParTime[0]);
        System.out.printf("Total Speedup:         %.2fx\n", overallSpeedup);
        System.out.printf("\nTotal wall-clock time for batch parallel processing: %.3f seconds\n", batchElapsed);

        System.out.println("\nPress Enter to exit...");
        new Scanner(System.in).nextLine();
    }
}
