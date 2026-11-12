package com.mylog.journal.application;

import java.nio.file.Path;
import java.util.UUID;

/** Files kept on the local disk when Cloudinary is off: signed-link check and file location for raw serving. */
public interface LocalAssetFiles {
    boolean verifyToken(UUID assetId, long expires, String sig);
    Path resolveLocalFile(UUID userId, UUID assetId, String format);
}
