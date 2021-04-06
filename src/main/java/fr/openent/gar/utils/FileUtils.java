package fr.openent.gar.utils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(org.entcore.common.utils.FileUtils.class);

    private FileUtils(){}

    public static String appendPath(final String path, final String path2) {
        return Paths.get(path, path2).toString();
    }

    public static void mkdirs(final String path) {
        if (path != null && !path.isEmpty()) {
            if (Files.notExists(Paths.get(path))) {
                new File(path).mkdirs();
            }
        }
    }

    public static void deleteFiles(String path) {
        if (path != null && !path.isEmpty()) {
            File index = new File(path);
            if (index.exists()) {
                File[] entries = index.listFiles();
                if (entries != null) {
                    for (File f : entries) {
                        f.delete();
                    }
                }
            }
        }
    }
}