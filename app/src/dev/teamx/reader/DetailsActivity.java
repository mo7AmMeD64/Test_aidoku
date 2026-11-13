package dev.teamx.reader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class DetailsActivity extends Activity {
	private String url;
	private String title;

	private TextView status;
	private LinearLayout chapters;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		url = getIntent().getStringExtra("url");
		title = getIntent().getStringExtra("title");
		setTitle(title == null ? "" : title);

		int pad = dp(14);
		ScrollView scroll = new ScrollView(this);
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(pad, pad, pad, pad);
		scroll.addView(root);

		ImageView cover = new ImageView(this);
		cover.setAdjustViewBounds(true);
		cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
		cover.setMaxHeight(dp(420));
		root.addView(cover, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView heading = new TextView(this);
		heading.setTextSize(22);
		heading.setPadding(0, pad, 0, 0);
		root.addView(heading);

		status = new TextView(this);
		status.setText("Loading…");
		status.setPadding(0, pad, 0, pad);
		root.addView(status);

		TextView description = new TextView(this);
		description.setTextSize(15);
		description.setPadding(0, pad, 0, pad);
		root.addView(description);

		chapters = new LinearLayout(this);
		chapters.setOrientation(LinearLayout.VERTICAL);
		root.addView(chapters);

		setContentView(scroll);
		if (url != null) load(cover, heading, description);
	}

	private void load(ImageView cover, TextView heading, TextView description) {
		new Thread(() -> {
			try {
				String html = Site.get(url);
				Site.Details details = Site.details(html, url);
				int lastPage = Site.lastChapterPage(html, url);
				List<Site.Chapter> first = Site.chapters(html, url);

				runOnUiThread(() -> {
					heading.setText(details.title.isEmpty() ? title : details.title);
					description.setText(details.description);
					if (!details.tags.isEmpty()) {
						TextView tags = new TextView(DetailsActivity.this);
						tags.setText(android.text.TextUtils.join(" · ", details.tags));
						tags.setTextSize(13);
						tags.setTextColor(Color.DKGRAY);
						tags.setPadding(0, 0, 0, dp(8));
						chapters.addView(tags, 0);
					}
					status.setText("Chapters");
					addChapters(first);
				});

				for (int page = 2; page <= lastPage; page++) {
					String nextUrl = url + "?page=" + page;
					List<Site.Chapter> more = Site.chapters(Site.get(nextUrl), nextUrl);
					runOnUiThread(() -> addChapters(more));
				}
				runOnUiThread(() -> ImageLoader.load(cover, details.cover, getResources()
					.getDisplayMetrics().widthPixels));
			} catch (Exception e) {
				runOnUiThread(() -> status.setText("Failed: " + e.getMessage()));
			}
		}).start();
	}

	private void addChapters(List<Site.Chapter> list) {
		for (Site.Chapter chapter : list) {
			TextView row = new TextView(this);
			row.setText(chapter.title);
			row.setTextSize(16);
			row.setPadding(dp(10), dp(12), dp(10), dp(12));
			row.setGravity(Gravity.CENTER_VERTICAL);
			row.setClickable(true);
			row.setOnClickListener(v -> {
				Intent intent = new Intent(this, ReaderActivity.class);
				intent.putExtra("url", chapter.url);
				intent.putExtra("title", chapter.title);
				startActivity(intent);
			});
			chapters.addView(row);
			View divider = new View(this);
			divider.setBackgroundColor(Color.LTGRAY);
			chapters.addView(divider, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, 1));
		}
	}

	private int dp(int value) {
		return (int) (value * getResources().getDisplayMetrics().density);
	}
}
