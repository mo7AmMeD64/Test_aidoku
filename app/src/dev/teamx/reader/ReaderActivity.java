package dev.teamx.reader;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** One page at a time, Prev/Next. Simple and memory-safe. */
public class ReaderActivity extends Activity {
	private final List<String> pages = new ArrayList<>();
	private int index;

	private ImageView image;
	private TextView status;
	private Button previous;
	private Button next;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		String url = getIntent().getStringExtra("url");
		setTitle(getIntent().getStringExtra("title"));

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);

		status = new TextView(this);
		status.setText("Loading…");
		status.setGravity(Gravity.CENTER);
		status.setPadding(0, dp(8), 0, dp(8));
		root.addView(status);

		image = new ImageView(this);
		image.setScaleType(ImageView.ScaleType.FIT_CENTER);
		image.setBackgroundColor(Color.BLACK);
		root.addView(image, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		LinearLayout controls = new LinearLayout(this);
		controls.setOrientation(LinearLayout.HORIZONTAL);
		previous = new Button(this);
		previous.setText("Previous");
		previous.setOnClickListener(v -> show(index - 1));
		controls.addView(previous, new LinearLayout.LayoutParams(0,
			ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		next = new Button(this);
		next.setText("Next");
		next.setOnClickListener(v -> show(index + 1));
		controls.addView(next, new LinearLayout.LayoutParams(0,
			ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		root.addView(controls);

		setContentView(root);
		if (url != null) load(url);
	}

	private void load(String url) {
		new Thread(() -> {
			try {
				List<String> loaded = Site.pages(Site.get(url), url);
				runOnUiThread(() -> {
					pages.clear();
					pages.addAll(loaded);
					if (pages.isEmpty()) {
						status.setText("No pages");
					} else {
						show(0);
					}
				});
			} catch (Exception e) {
				runOnUiThread(() -> status.setText("Failed: " + e.getMessage()));
			}
		}).start();
	}

	private void show(int target) {
		if (pages.isEmpty()) return;
		index = Math.max(0, Math.min(pages.size() - 1, target));
		status.setText((index + 1) + " / " + pages.size());
		previous.setEnabled(index > 0);
		next.setEnabled(index < pages.size() - 1);
		image.setImageBitmap(null);
		ImageLoader.load(image, pages.get(index),
			getResources().getDisplayMetrics().widthPixels);
	}

	private int dp(int value) {
		return (int) (value * getResources().getDisplayMetrics().density);
	}
}
