package com.mylog.journal.application;

import com.mylog.platform.web.InvalidRequestException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageInspectionServiceTest {
    private final ImageInspectionService service = new ImageInspectionService();

    @Test
    void rejectsEmptyOrNullInput() {
        assertThrows(InvalidRequestException.class, () -> service.inspectAndSanitize(null, "image/jpeg"));
        assertThrows(InvalidRequestException.class, () -> service.inspectAndSanitize(new byte[0], "image/jpeg"));
    }

    @Test
    void rejectsSpoofedExecutableFiles() {
        byte[] fakeExe = "MZ fake executable binary content".getBytes();
        assertThrows(InvalidRequestException.class, () -> service.inspectAndSanitize(fakeExe, "image/jpeg"));
    }

    @Test
    void acceptsAndSanitizesValidPngImage() throws IOException {
        BufferedImage image = new BufferedImage(100, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 80);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        SanitizedImage result = service.inspectAndSanitize(pngBytes, "image/png");
        assertNotNull(result);
        assertEquals("png", result.format());
        assertEquals("image/png", result.mimeType());
        assertEquals(100, result.width());
        assertEquals(80, result.height());
        assertNotNull(result.sha256());
        assertTrue(result.bytes().length > 0);
    }

    @Test
    void acceptsAndSanitizesValidJpegImage() throws IOException {
        BufferedImage image = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 120, 90);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", baos);
        byte[] jpegBytes = baos.toByteArray();

        SanitizedImage result = service.inspectAndSanitize(jpegBytes, "image/jpeg");
        assertNotNull(result);
        assertEquals("jpg", result.format());
        assertEquals("image/jpeg", result.mimeType());
        assertEquals(120, result.width());
        assertEquals(90, result.height());
        assertNotNull(result.sha256());
        assertTrue(result.bytes().length > 0);
    }
}

