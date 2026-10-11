import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

/** Emits one TSV row per PDF page: one-based page index and Base64 UTF-8 text. */
public final class PdfPageExtractor {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: PdfPageExtractor INPUT.pdf OUTPUT.tsv");
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());
        try (PDDocument document = Loader.loadPDF(Path.of(args[0]).toFile());
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                int images = 0;
                PDResources resources = document.getPage(page - 1).getResources();
                if (resources != null) {
                    for (var name : resources.getXObjectNames()) {
                        if (resources.getXObject(name) instanceof PDImageXObject) images++;
                    }
                }
                writer.write(page + "\t" + images + "\t" + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
                writer.newLine();
            }
            System.out.println(document.getNumberOfPages());
        }
    }
}
