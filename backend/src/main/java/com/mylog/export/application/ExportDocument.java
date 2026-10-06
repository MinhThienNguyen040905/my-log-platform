package com.mylog.export.application;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
class ExportDocument {
    private final ObjectMapper mapper;
    ExportDocument(ObjectMapper mapper) { this.mapper=mapper; }
    byte[] render(String format, List<ExportSnapshot.Row> rows) {
        if ("CSV".equals(format)) return csv(rows);
        if ("PDF".equals(format)) return pdf(rows);
        throw new IllegalArgumentException("format");
    }
    private byte[] csv(List<ExportSnapshot.Row> rows) {
        StringBuilder csv = new StringBuilder("section,id,dataJson\r\n");
        for (var row : rows) {
            csv.append(cell(row.section())).append(',').append(cell(row.id())).append(',')
                    .append(cell(mapper.writeValueAsString(row.value()))).append("\r\n");
        }
        return ("\uFEFF"+csv).getBytes(StandardCharsets.UTF_8);
    }
    private static String cell(String value) {
        // Quote all cells; the tab prefix prevents spreadsheet formula execution when opened interactively.
        String safe = value.replace("\r", " ").replace("\n", " ");
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0))>=0) safe="'"+safe;
        return '"'+safe.replace("\"","\"\"")+'"';
    }
    private byte[] pdf(List<ExportSnapshot.Row> rows) {
        try (PDDocument document = new PDDocument();
             InputStream fontInput = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            if (fontInput == null) throw new IllegalStateException("PDF font missing");
            PDType0Font font = PDType0Font.load(document,fontInput);
            PDPage page = new PDPage(PDRectangle.A4); document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document,page);
            float y=790;
            y = line(stream,font,y,"Mylog - personal data export",true);
            for (var row : rows) {
                String value = mapper.writeValueAsString(row.value());
                String content = printable(font,row.section()+"  "+row.id()+"  "+value);
                for (String part:wrap(font,content)) {
                    if (y<45) { stream.close(); page=new PDPage(PDRectangle.A4); document.addPage(page);
                        stream=new PDPageContentStream(document,page); y=790; }
                    y=line(stream,font,y,part,false);
                }
                y-=8;
            }
            stream.close();
            ByteArrayOutputStream output = new ByteArrayOutputStream(); document.save(output);
            return output.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("PDF export failed",e); }
    }
    private float line(PDPageContentStream stream, PDType0Font font, float y, String value, boolean heading) throws java.io.IOException {
        stream.beginText(); stream.setFont(font,heading?14:8); stream.newLineAtOffset(35,y);
        stream.showText(value); stream.endText();
        return y-(heading?25:13);
    }
    private String printable(PDType0Font font,String value) {
        StringBuilder safe=new StringBuilder();
        value.codePoints().forEach(code -> {
            if (Character.isISOControl(code)) {safe.append(' ');return;}
            String glyph=new String(Character.toChars(code));
            try {font.getStringWidth(glyph);safe.append(glyph);}
            catch (Exception e) {safe.append('?');}
        });
        return safe.toString();
    }
    private List<String> wrap(PDType0Font font,String value) throws java.io.IOException {
        java.util.ArrayList<String> lines=new java.util.ArrayList<>();
        StringBuilder line=new StringBuilder();float width=0;
        for (int code:value.codePoints().toArray()) {
            String glyph=new String(Character.toChars(code));
            float next=font.getStringWidth(glyph)*8/1000;
            if (width+next>520 && !line.isEmpty()) {
                lines.add(line.toString());line.setLength(0);width=0;
            }
            line.append(glyph);width+=next;
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
