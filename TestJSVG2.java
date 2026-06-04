import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.attributes.ViewBox;
import java.io.File;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

public class TestJSVG2 {
    public static void main(String[] args) throws Exception {
        SVGLoader loader = new SVGLoader();
        // Just load any SVG file we know exists
        File svgFile = new File("src/main/resources/images/action.svg");
        SVGDocument doc = loader.load(svgFile.toURI().toURL());
        FloatSize size = doc.size();
        System.out.println("Size: " + size.width + "x" + size.height);
        
        int renderHeight = 32;
        float scale = (float) renderHeight / size.height;
        int width = Math.round(size.width * scale);
        
        BufferedImage img = new BufferedImage(width, renderHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        doc.render(null, g2, new ViewBox(0, 0, width, renderHeight));
        g2.dispose();
        System.out.println("Rendered SVG");
    }
}
