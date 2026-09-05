import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Crops the same region out of several frames and tiles them. Usage: java tools/Crop.java out.png x y w h scale file1 file2 ... */
public final class Crop {
    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        int x = Integer.parseInt(args[1]), y = Integer.parseInt(args[2]), w = Integer.parseInt(args[3]), h = Integer.parseInt(args[4]);
        double scale = Double.parseDouble(args[5]);
        List<File> files = new ArrayList<>();
        for (int i = 6; i < args.length; i++) files.add(new File(args[i]));
        int tw = (int) (w * scale), th = (int) (h * scale);
        BufferedImage sheet = new BufferedImage(tw * files.size(), th, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int i = 0; i < files.size(); i++) {
            BufferedImage img = ImageIO.read(files.get(i)).getSubimage(x, y, w, h);
            g.drawImage(img, i * tw, 0, tw, th, null);
        }
        g.dispose();
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out);
    }
}
