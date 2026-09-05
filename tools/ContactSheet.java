import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Tiles animation frames into one labelled contact sheet.
 * Usage: java tools/ContactSheet.java <screenshot dir> <name prefix> <out.png> [columns] [scale]
 * Example: java tools/ContactSheet.java sb_combat/run/screenshots anim_sword_sweep_ltr_back_ sheet.png 4 0.33
 */
public final class ContactSheet {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        String prefix = args[1];
        File out = new File(args[2]);
        int columns = args.length > 3 ? Integer.parseInt(args[3]) : 4;
        double scale = args.length > 4 ? Double.parseDouble(args[4]) : 0.33;
        List<File> frames = new ArrayList<>(Arrays.asList(dir.listFiles((d, n) -> n.startsWith(prefix) && n.endsWith(".png"))));
        frames.sort(Comparator.comparing(File::getName));
        if (frames.isEmpty()) {
            System.err.println("no frames for prefix " + prefix);
            System.exit(1);
        }
        BufferedImage first = ImageIO.read(frames.get(0));
        int w = (int) Math.round(first.getWidth() * scale);
        int h = (int) Math.round(first.getHeight() * scale);
        int rows = (frames.size() + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(w * columns, h * rows, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, h / 12)));
        for (int i = 0; i < frames.size(); i++) {
            BufferedImage img = ImageIO.read(frames.get(i));
            int x = (i % columns) * w;
            int y = (i / columns) * h;
            g.drawImage(img, x, y, w, h, null);
            String label = frames.get(i).getName().replace(prefix, "").replace(".png", "");
            g.setColor(Color.BLACK);
            g.drawString(label, x + 6, y + h - 6);
            g.setColor(Color.YELLOW);
            g.drawString(label, x + 4, y + h - 8);
        }
        g.dispose();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out + " (" + frames.size() + " frames)");
    }
}
