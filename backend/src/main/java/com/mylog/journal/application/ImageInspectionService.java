package com.mylog.journal.application;

import com.mylog.platform.web.InvalidRequestException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

@Service
public class ImageInspectionService {
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final long MAX_PIXELS = 25_000_000L; // 25 Megapixels

    public SanitizedImage inspectAndSanitize(byte[] inputBytes, String declaredMime) {
        if (inputBytes == null || inputBytes.length == 0) {
            throw new InvalidRequestException("Tệp tải lên không được để trống.");
        }
        if (inputBytes.length > MAX_FILE_SIZE) {
            throw new InvalidRequestException("Kích thước tệp vượt quá giới hạn 5MB.");
        }

        String detectedFormat = detectFormatByMagicBytes(inputBytes);
        if (detectedFormat == null) {
            throw new InvalidRequestException("Định dạng tệp không được hỗ trợ hoặc bị giả mạo. Chỉ hỗ trợ JPEG, PNG, WebP.");
        }

        String mimeType = switch (detectedFormat) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new InvalidRequestException("Định dạng tệp không hợp lệ.");
        };

        if ("webp".equals(detectedFormat)) {
            return sanitizeWebP(inputBytes, mimeType);
        }

        return sanitizeRasterImage(inputBytes, detectedFormat, mimeType);
    }

    private SanitizedImage sanitizeRasterImage(byte[] inputBytes, String format, String mimeType) {
        int width;
        int height;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(inputBytes))) {
            if (iis == null) throw new InvalidRequestException("Không thể đọc cấu trúc tệp hình ảnh.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) throw new InvalidRequestException("Không thể đọc định dạng hình ảnh.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                width = reader.getWidth(0);
                height = reader.getHeight(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidRequestException("Tệp hình ảnh bị lỗi hoặc không thể đọc cấu trúc.");
        }

        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
            throw new InvalidRequestException("Kích thước hình ảnh vượt quá giới hạn cho phép (tối đa 25 Megapixel).");
        }

        int orientation = 1;
        if ("jpg".equals(format)) {
            orientation = readJpegExifOrientation(inputBytes);
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(inputBytes));
            if (original == null) throw new InvalidRequestException("Không thể giải mã dữ liệu hình ảnh.");

            BufferedImage processed = applyOrientation(original, orientation);
            int finalWidth = processed.getWidth();
            int finalHeight = processed.getHeight();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean written = ImageIO.write(processed, format.equals("jpg") ? "jpeg" : "png", baos);
            if (!written) throw new InvalidRequestException("Không thể xử lý hình ảnh.");

            byte[] cleanBytes = baos.toByteArray();
            byte[] sha256 = sha256(cleanBytes);

            return new SanitizedImage(cleanBytes, format, mimeType, finalWidth, finalHeight, sha256);
        } catch (IOException e) {
            throw new InvalidRequestException("Lỗi khi xử lý làm sạch hình ảnh.");
        }
    }

    private SanitizedImage sanitizeWebP(byte[] inputBytes, String mimeType) {
        int[] dimensions = readWebpDimensions(inputBytes);
        int width = dimensions[0];
        int height = dimensions[1];

        if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
            throw new InvalidRequestException("Kích thước hình ảnh WebP vượt quá giới hạn 25 Megapixel.");
        }

        byte[] sha256 = sha256(inputBytes);
        return new SanitizedImage(inputBytes, "webp", mimeType, width, height, sha256);
    }

    private static String detectFormatByMagicBytes(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private static int[] readWebpDimensions(byte[] bytes) {
        if (bytes.length < 30) return new int[]{100, 100};
        try {
            if (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == 'X') {
                int width = 1 + ((bytes[24] & 0xFF) | ((bytes[25] & 0xFF) << 8) | ((bytes[26] & 0xFF) << 16));
                int height = 1 + ((bytes[27] & 0xFF) | ((bytes[28] & 0xFF) << 8) | ((bytes[29] & 0xFF) << 16));
                return new int[]{width, height};
            }
            if (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == 'L') {
                int b1 = bytes[21] & 0xFF;
                int b2 = bytes[22] & 0xFF;
                int b3 = bytes[23] & 0xFF;
                int b4 = bytes[24] & 0xFF;
                int width = 1 + (((b2 & 0x3F) << 8) | b1);
                int height = 1 + (((b4 & 0x0F) << 10) | (b3 << 2) | ((b2 & 0xC0) >> 6));
                return new int[]{width, height};
            }
            if (bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8' && bytes[15] == ' ') {
                if (bytes.length >= 26) {
                    int width = ((bytes[26] & 0xFF) | ((bytes[27] & 0xFF) << 8)) & 0x3FFF;
                    int height = ((bytes[28] & 0xFF) | ((bytes[29] & 0xFF) << 8)) & 0x3FFF;
                    return new int[]{width, height};
                }
            }
        } catch (Exception ignored) {
        }
        return new int[]{800, 600};
    }

    private static int readJpegExifOrientation(byte[] bytes) {
        int offset = 2;
        while (offset + 4 < bytes.length) {
            if ((bytes[offset] & 0xFF) != 0xFF) break;
            int marker = bytes[offset + 1] & 0xFF;
            if (marker == 0xDA || marker == 0xD9) break; // SOS or EOI
            int length = ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
            if (marker == 0xE1 && length >= 14) { // APP1 Exif
                int exifHeader = offset + 4;
                if (bytes[exifHeader] == 'E' && bytes[exifHeader + 1] == 'x' && bytes[exifHeader + 2] == 'i'
                        && bytes[exifHeader + 3] == 'f' && bytes[exifHeader + 4] == 0 && bytes[exifHeader + 5] == 0) {
                    int tiff = exifHeader + 6;
                    boolean bigEndian = bytes[tiff] == 'M' && bytes[tiff + 1] == 'M';
                    int ifd0Offset = readInt(bytes, tiff + 4, bigEndian);
                    int ifd0 = tiff + ifd0Offset;
                    if (ifd0 + 2 <= bytes.length) {
                        int entries = readShort(bytes, ifd0, bigEndian);
                        int p = ifd0 + 2;
                        for (int i = 0; i < entries && p + 12 <= bytes.length; i++) {
                            int tag = readShort(bytes, p, bigEndian);
                            if (tag == 0x0112) { // Orientation tag
                                return readShort(bytes, p + 8, bigEndian);
                            }
                            p += 12;
                        }
                    }
                }
            }
            offset += 2 + length;
        }
        return 1;
    }

    private static int readShort(byte[] b, int offset, boolean bigEndian) {
        int b1 = b[offset] & 0xFF;
        int b2 = b[offset + 1] & 0xFF;
        return bigEndian ? (b1 << 8) | b2 : (b2 << 8) | b1;
    }

    private static int readInt(byte[] b, int offset, boolean bigEndian) {
        int s1 = readShort(b, offset, bigEndian);
        int s2 = readShort(b, offset + 2, bigEndian);
        return bigEndian ? (s1 << 16) | s2 : (s2 << 16) | s1;
    }

    private static BufferedImage applyOrientation(BufferedImage image, int orientation) {
        if (orientation <= 1) return image;
        int width = image.getWidth();
        int height = image.getHeight();
        AffineTransform tx = new AffineTransform();

        int targetWidth = width;
        int targetHeight = height;

        switch (orientation) {
            case 3: // 180 rotate
                tx.translate(width, height);
                tx.rotate(Math.PI);
                break;
            case 6: // 90 rotate CW
                targetWidth = height;
                targetHeight = width;
                tx.translate(height, 0);
                tx.rotate(Math.PI / 2);
                break;
            case 8: // 270 rotate CW (90 CCW)
                targetWidth = height;
                targetHeight = width;
                tx.translate(0, width);
                tx.rotate(-Math.PI / 2);
                break;
            default:
                return image;
        }

        BufferedImage rotated = new BufferedImage(targetWidth, targetHeight,
                image.getTransparency() == BufferedImage.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.drawImage(image, tx, null);
        g2d.dispose();
        return rotated;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}

