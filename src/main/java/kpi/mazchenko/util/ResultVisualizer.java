package kpi.mazchenko.util;

import kpi.mazchenko.model.BenchmarkResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class ResultVisualizer {

    private static final int W = 860;
    private static final int H = 560;
    private static final int PAD_LEFT = 80;
    private static final int PAD_RIGHT = 40;
    private static final int PAD_TOP = 50;
    private static final int PAD_BOTTOM = 70;

    private static final Color COLOR_IDEAL = new Color(180, 180, 180);
    private static final Color COLOR_PC1 = new Color(0, 0, 255);
    private static final Color COLOR_DATA = new Color(128, 0, 128);

    public static void saveCharts(List<BenchmarkResult> threadResults, List<BenchmarkResult> dataResults,
            int maxThreads) throws Exception {
        File dir = new File("results");
        if (!dir.exists())
            dir.mkdirs();

        drawSpeedupChart(threadResults, maxThreads);
        drawEfficiencyChart(threadResults, maxThreads);
        drawMetricsTable(threadResults);
        drawDataScalabilityChart(dataResults);
    }

    private static void drawSpeedupChart(List<BenchmarkResult> results, int maxThreads) throws Exception {
        BufferedImage img = newImage();
        Graphics2D g = setup(img);

        int chartW = W - PAD_LEFT - PAD_RIGHT;
        int chartH = H - PAD_TOP - PAD_BOTTOM;
        double maxY = maxThreads;

        drawAxes(g, maxThreads, maxY, "Speedup (S)", "Кількість потоків (p)",
                "Залежність прискорення від кількості потоків");

        g.setColor(COLOR_IDEAL);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 6, 4 }, 0));
        int ix1 = PAD_LEFT;
        int iy1 = PAD_TOP + chartH - (int) (1.0 / maxY * chartH);
        int ix2 = PAD_LEFT + chartW;
        int iy2 = PAD_TOP + chartH - (int) (maxThreads / maxY * chartH);
        g.drawLine(ix1, iy1, ix2, iy2);

        g.setStroke(new BasicStroke(2.5f));
        g.setColor(COLOR_PC1);
        drawLine(g, results, maxThreads, maxY, chartW, chartH, r -> r.realSpeedup);

        drawLegend(g, new String[] { "Ідеальне прискорення", "Реальне прискорення" },
                new Color[] { COLOR_IDEAL, COLOR_PC1 }, new boolean[] { true, false }, PAD_LEFT + 20, PAD_TOP + 20);

        g.dispose();
        ImageIO.write(img, "png", new File("results/speedup.png"));
    }

    private static void drawEfficiencyChart(List<BenchmarkResult> results, int maxThreads) throws Exception {
        BufferedImage img = newImage();
        Graphics2D g = setup(img);
        int legendX = W - PAD_RIGHT - 190;
        int legendY = PAD_TOP + 30;

        int chartH = H - PAD_TOP - PAD_BOTTOM;
        int chartW = W - PAD_LEFT - PAD_RIGHT;
        double maxY = 1.1;

        drawAxes(g, maxThreads, maxY, "Ефективність (E = S/p)", "Кількість потоків (p)", "Ефективність паралелізму");

        g.setColor(COLOR_IDEAL);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 6, 4 }, 0));
        g.drawLine(PAD_LEFT, PAD_TOP + chartH - (int) (1.0 / maxY * chartH),
                PAD_LEFT + chartW, PAD_TOP + chartH - (int) (1.0 / maxY * chartH));

        g.setStroke(new BasicStroke(2.5f));
        g.setColor(COLOR_PC1);
        drawLine(g, results, maxThreads, maxY, chartW, chartH, r -> r.realEfficiency);

        drawLegend(g, new String[] { "Ідеальна ефективність (E=1)", "Реальна ефективність" },
                new Color[] { COLOR_IDEAL, COLOR_PC1 }, new boolean[] { true, false }, legendX, legendY);

        g.dispose();
        ImageIO.write(img, "png", new File("results/efficiency.png"));
    }

    private static void drawDataScalabilityChart(List<BenchmarkResult> results) throws Exception {
        BufferedImage img = newImage();
        Graphics2D g = setup(img);

        int maxDocs = results.get(results.size() - 1).documents;
        double maxSpeedup = results.stream().mapToDouble(r -> r.realSpeedup).max().orElse(1.0) * 1.15;
        int chartW = W - PAD_LEFT - PAD_RIGHT;
        int chartH = H - PAD_TOP - PAD_BOTTOM;

        g.setColor(new Color(240, 240, 240));
        for (int tick = 0; tick <= 5; tick++) {
            int y = PAD_TOP + chartH - (int) (tick / 5.0 * chartH);
            g.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);
        g.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Залежність прискорення від обсягу даних", PAD_LEFT + chartW / 2 - 160, PAD_TOP - 20);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));

        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult r = results.get(i);
            int x = PAD_LEFT + (int) ((double) r.documents / maxDocs * chartW);
            g.setColor(new Color(200, 200, 200));
            g.drawLine(x, PAD_TOP, x, PAD_TOP + chartH);
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(r.documents), x - 15, PAD_TOP + chartH + 18);
        }

        for (int tick = 0; tick <= 5; tick++) {
            double val = tick / 5.0 * maxSpeedup;
            int y = PAD_TOP + chartH - (int) (val / maxSpeedup * chartH);
            g.setColor(Color.BLACK);
            g.drawString(String.format("%.1f", val), PAD_LEFT - 38, y + 4);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        drawRotatedLabel(g, "Прискорення (S)", PAD_LEFT - 55, PAD_TOP + chartH / 2);
        g.drawString("Кількість документів (N)", PAD_LEFT + chartW / 2 - 70, PAD_TOP + chartH + 45);

        g.setStroke(new BasicStroke(2.5f));
        g.setColor(COLOR_DATA);
        for (int i = 1; i < results.size(); i++) {
            BenchmarkResult r1 = results.get(i - 1);
            BenchmarkResult r2 = results.get(i);
            int x1 = PAD_LEFT + (int) ((double) r1.documents / maxDocs * chartW);
            int y1 = PAD_TOP + chartH - (int) (r1.realSpeedup / maxSpeedup * chartH);
            int x2 = PAD_LEFT + (int) ((double) r2.documents / maxDocs * chartW);
            int y2 = PAD_TOP + chartH - (int) (r2.realSpeedup / maxSpeedup * chartH);
            g.drawLine(x1, y1, x2, y2);
        }
        for (BenchmarkResult r : results) {
            int x = PAD_LEFT + (int) ((double) r.documents / maxDocs * chartW);
            int y = PAD_TOP + chartH - (int) (r.realSpeedup / maxSpeedup * chartH);
            g.fillOval(x - 4, y - 4, 8, 8);
        }

        drawLegend(g, new String[] { "Прискорення від обсягу даних" }, new Color[] { COLOR_DATA },
                new boolean[] { false }, PAD_LEFT + 20, PAD_TOP + 20);

        g.dispose();
        ImageIO.write(img, "png", new File("results/data_scalability.png"));
    }

    private static void drawMetricsTable(List<BenchmarkResult> results) throws Exception {
        int rowH = 28;
        int tableH = 45 + results.size() * rowH;
        BufferedImage img = new BufferedImage(820, tableH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 820, tableH);

        int[] colX = { 10, 110, 230, 360, 490 };
        String[] headers = { "Потоки", "Час (мс)", "Прискорення", "Ефективність", "Вартість" };

        g.setColor(new Color(230, 230, 230));
        g.fillRect(0, 0, 820, 32);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        for (int i = 0; i < headers.length; i++) {
            g.drawString(headers[i], colX[i], 20);
        }

        g.setFont(new Font("Monospaced", Font.PLAIN, 13));
        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult r = results.get(i);
            if (i % 2 == 0) {
                g.setColor(new Color(245, 245, 255));
                g.fillRect(0, 32 + i * rowH, 820, rowH);
            }
            g.setColor(Color.BLACK);
            double cost = r.avgParallelMs * r.maxThreads;
            String[] vals = {
                    String.valueOf(r.parallelism),
                    String.format("%.2f", r.avgParallelMs),
                    String.format("%.4f", r.realSpeedup),
                    String.format("%.4f", r.realEfficiency),
                    String.format("%.2f", cost)
            };
            for (int j = 0; j < vals.length; j++) {
                g.drawString(vals[j], colX[j], 32 + i * rowH + 19);
            }
        }

        g.setColor(new Color(180, 180, 180));
        g.drawLine(0, 32, 820, 32);
        for (int x : colX) {
            g.drawLine(x - 5, 0, x - 5, tableH);
        }

        g.dispose();
        ImageIO.write(img, "png", new File("results/metrics_table.png"));
    }

    private static void drawAxes(Graphics2D g, int maxThreads, double maxY,
            String yLabel, String xLabel, String title) {
        int chartW = W - PAD_LEFT - PAD_RIGHT;
        int chartH = H - PAD_TOP - PAD_BOTTOM;

        g.setColor(new Color(235, 235, 235));
        int gridLines = 5;
        for (int i = 1; i <= gridLines; i++) {
            int y = PAD_TOP + chartH - i * chartH / gridLines;
            g.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);
        }
        for (int p = 1; p <= maxThreads; p++) {
            int x = PAD_LEFT + (int) ((p - 1.0) / (maxThreads - 1) * chartW);
            g.drawLine(x, PAD_TOP, x, PAD_TOP + chartH);
        }

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(PAD_LEFT, PAD_TOP + chartH, PAD_LEFT + chartW, PAD_TOP + chartH);
        g.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT, PAD_TOP + chartH);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int p = 1; p <= maxThreads; p++) {
            if (maxThreads <= 12 || p % 2 == 0 || p == 1) {
                int x = PAD_LEFT + (int) ((p - 1.0) / (maxThreads - 1) * chartW);
                g.drawString(String.valueOf(p), x - 4, PAD_TOP + chartH + 18);
            }
        }

        for (int i = 0; i <= gridLines; i++) {
            double val = i / (double) gridLines * maxY;
            int y = PAD_TOP + chartH - (int) (val / maxY * chartH);
            g.drawString(String.format("%.1f", val), PAD_LEFT - 38, y + 4);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(title, PAD_LEFT + chartW / 2 - title.length() * 4, PAD_TOP - 20);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(xLabel, PAD_LEFT + chartW / 2 - 60, PAD_TOP + chartH + 45);
        drawRotatedLabel(g, yLabel, PAD_LEFT - 55, PAD_TOP + chartH / 2);
    }

    private static void drawLine(Graphics2D g, List<BenchmarkResult> results, int maxThreads, double maxY,
            int chartW, int chartH, java.util.function.Function<BenchmarkResult, Double> valueExtractor) {
        for (int i = 1; i < results.size(); i++) {
            BenchmarkResult r1 = results.get(i - 1);
            BenchmarkResult r2 = results.get(i);
            int x1 = PAD_LEFT + (int) ((r1.parallelism - 1.0) / (maxThreads - 1) * chartW);
            int y1 = PAD_TOP + chartH - (int) (valueExtractor.apply(r1) / maxY * chartH);
            int x2 = PAD_LEFT + (int) ((r2.parallelism - 1.0) / (maxThreads - 1) * chartW);
            int y2 = PAD_TOP + chartH - (int) (valueExtractor.apply(r2) / maxY * chartH);
            g.drawLine(x1, y1, x2, y2);
        }
        for (BenchmarkResult r : results) {
            int x = PAD_LEFT + (int) ((r.parallelism - 1.0) / (maxThreads - 1) * chartW);
            int y = PAD_TOP + chartH - (int) (valueExtractor.apply(r) / maxY * chartH);
            g.fillOval(x - 4, y - 4, 8, 8);
        }
    }

    private static void drawLegend(Graphics2D g, String[] labels, Color[] colors, boolean[] dashed, int x, int y) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 0; i < labels.length; i++) {
            g.setColor(colors[i]);
            if (dashed[i]) {
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 6, 4 },
                        0));
            } else {
                g.setStroke(new BasicStroke(2.5f));
            }
            g.drawLine(x, y + i * 20, x + 30, y + i * 20);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1f));
            g.drawString(labels[i], x + 38, y + i * 20 + 4);
        }
    }

    private static void drawRotatedLabel(Graphics2D g, String text, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.rotate(-Math.PI / 2);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.drawString(text, -text.length() * 3, 0);
        g2.dispose();
    }

    private static BufferedImage newImage() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        g.dispose();
        return img;
    }

    private static Graphics2D setup(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }
}