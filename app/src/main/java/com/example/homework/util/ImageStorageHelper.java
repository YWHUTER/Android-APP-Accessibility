package com.example.homework.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * 将相册选择的图片复制到应用内部存储，避免 content URI 在卸载后失效。
 */
public class ImageStorageHelper {
    private static final String IMAGE_DIR = "item_images";

    private ImageStorageHelper() {
    }

    /**
     * 将 content URI 指向的图片复制到应用内部存储并返回文件的绝对路径。
     *
     * @param context 上下文
     * @param sourceUri 从相册选择器获得的 content URI
     * @return 复制后的文件绝对路径，失败时返回 null
     */
    public static String copyToInternalStorage(Context context, Uri sourceUri) {
        try {
            File imageDir = new File(context.getFilesDir(), IMAGE_DIR);
            if (!imageDir.exists() && !imageDir.mkdirs()) {
                return null;
            }

            String fileName = UUID.randomUUID().toString() + ".jpg";
            File destFile = new File(imageDir, fileName);

            try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                 OutputStream outputStream = new FileOutputStream(destFile)) {
                if (inputStream == null) {
                    return null;
                }
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return destFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除应用内部存储中的图片文件。
     *
     * @param absolutePath 图片的绝对路径
     */
    public static void deleteImage(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return;
        }
        try {
            File file = new File(absolutePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }
}
