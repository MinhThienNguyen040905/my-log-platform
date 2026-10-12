import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Render one one-based PDF page for visual quality review. */
public final class PdfPagePreview {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: PdfPagePreview INPUT.pdf PAGE OUTPUT.png");
        try (var document = Loader.loadPDF(Path.of(args[0]).toFile())) {
            int page = Integer.parseInt(args[1]);
            if (page < 1 || page > document.getNumberOfPages()) throw new IllegalArgumentException("Page out of range");
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(page - 1, 72), "png", Path.of(args[2]).toFile());
        }
    }
}
