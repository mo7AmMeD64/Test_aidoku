package dev.teamx.reader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny bitmap loader: no cache, just background download + view-tag guard. */
final class ImageLoader {
	private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
	private static final Handler MAIN = new Handler(Looper.getMainLooper());

	private ImageLoader() {
	}

	static void load(ImageView view, String url, int targetWidth) {
		view.setTag(url);
		if (url == null || url.isEmpty()) return;
		POOL.execute(() -> {
			try {
				byte[] data = Site.getBytes(url);
				Bitmap bitmap = decode(data, targetWidth);
				if (bitmap == null) return;
				MAIN.post(() -> {
					if (url.equals(view.getTag())) view.setImageBitmap(bitmap);
				});
			} catch (Exception ignored) {
			}
		});
	}

	private static Bitmap decode(byte[] data, int targetWidth) {
		BitmapFactory.Options bounds = new BitmapFactory.Options();
		bounds.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
		int width = bounds.outWidth > 0 ? bounds.outWidth : targetWidth;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = Math.max(1, width / Math.max(1, targetWidth));
		return BitmapFactory.decodeByteArray(data, 0, data.length, options);
	}
}
