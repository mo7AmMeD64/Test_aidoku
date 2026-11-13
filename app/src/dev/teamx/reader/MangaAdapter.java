package dev.teamx.reader;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Two-line manga row: cover + title. */
final class MangaAdapter extends BaseAdapter {
	private final Context context;
	private final List<Site.Manga> items;
	private final int coverWidth;

	MangaAdapter(Context context, List<Site.Manga> items, int coverWidth) {
		this.context = context;
		this.items = items;
		this.coverWidth = coverWidth;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Site.Manga getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View recycled, ViewGroup parent) {
		LinearLayout row = new LinearLayout(context);
		row.setOrientation(LinearLayout.HORIZONTAL);
		int pad = dp(10);
		row.setPadding(pad, pad, pad, pad);

		ImageView cover = new ImageView(context);
		cover.setLayoutParams(new LinearLayout.LayoutParams(coverWidth, dp(96)));
		cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
		cover.setBackgroundColor(Color.LTGRAY);
		row.addView(cover);

		TextView title = new TextView(context);
		title.setTextSize(16);
		title.setPadding(dp(12), 0, 0, 0);
		title.setMaxLines(3);
		title.setText(items.get(position).title);
		row.addView(title, new LinearLayout.LayoutParams(0,
			ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		Site.Manga manga = items.get(position);
		ImageLoader.load(cover, manga.cover, coverWidth);
		return row;
	}

	private int dp(int value) {
		return (int) (value * context.getResources().getDisplayMetrics().density);
	}
}
