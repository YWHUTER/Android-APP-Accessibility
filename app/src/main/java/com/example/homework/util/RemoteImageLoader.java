package com.example.homework.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.homework.R;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final int CACHE_SIZE_KB = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
    private static final int DEFAULT_TARGET_SIZE_PX = 720;
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(CACHE_SIZE_KB) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return Math.max(1, value.getByteCount() / 1024);
                }
            };
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private RemoteImageLoader() {
    }

    public static void load(ImageView imageView, String imageUrl) {
        imageView.setImageResource(R.drawable.bg_image_placeholder);
        String resolvedSource = ItemImageCodec.getCoverImage(imageUrl);
        if (resolvedSource == null || resolvedSource.trim().isEmpty()) {
            imageView.setTag(null);
            return;
        }
        imageView.setTag(resolvedSource);

        Bitmap bitmap = CACHE.get(resolvedSource);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        int targetWidth = imageView.getWidth() > 0 ? imageView.getWidth() : DEFAULT_TARGET_SIZE_PX;
        int targetHeight = imageView.getHeight() > 0 ? imageView.getHeight() : DEFAULT_TARGET_SIZE_PX;
        EXECUTOR.execute(() -> {
            try {
                Bitmap downloaded = null;
                if (resolvedSource.startsWith("http://") || resolvedSource.startsWith("https://")) {
                    try (InputStream stream = new URL(resolvedSource).openStream()) {
                        downloaded = BitmapFactory.decodeStream(stream);
                    }
                } else {
                    java.io.File file = new java.io.File(resolvedSource);
                    if (file.exists()) {
                        downloaded = decodeSampledFile(file.getAbsolutePath(), targetWidth, targetHeight);
                    }
                }

                if (downloaded != null) {
                    CACHE.put(resolvedSource, downloaded);
                    Bitmap finalDownloaded = downloaded;
                    MAIN_HANDLER.post(() -> {
                        Object currentTag = imageView.getTag();
                        if (resolvedSource.equals(currentTag)) {
                            imageView.setImageBitmap(finalDownloaded);
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        });
    }

    public static void clearMemoryCache() {
        CACHE.evictAll();
    }

    private static Bitmap decodeSampledFile(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight);
        return BitmapFactory.decodeFile(path, decodeOptions);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }
}
