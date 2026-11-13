package dev.teamx.reader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
	private final List<Site.Manga> entries = new ArrayList<>();
	private MangaAdapter adapter;
	private EditText searchInput;
	private TextView status;
	private Button more;

	private boolean searching;
	private String query = "";
	private int page = 1;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		int pad = dp(12);

		LinearLayout root = new LinearLayout(this);
		root.setOrientation(LinearLayout.VERTICAL);
		root.setPadding(pad, pad, pad, 0);

		LinearLayout searchRow = new LinearLayout(this);
		searchRow.setOrientation(LinearLayout.HORIZONTAL);
		searchInput = new EditText(this);
		searchInput.setHint("Search manga");
		searchInput.setSingleLine(true);
		searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		searchRow.addView(searchInput, new LinearLayout.LayoutParams(0,
			ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		Button search = new Button(this);
		search.setText("Search");
		searchRow.addView(search);
		root.addView(searchRow);

		status = new TextView(this);
		status.setText("Loading popular…");
		status.setPadding(0, pad, 0, pad);
		root.addView(status);

		adapter = new MangaAdapter(this, entries, dp(64));
		ListView list = new ListView(this);
		list.setAdapter(adapter);
		list.setOnItemClickListener((parent, view, position, id) -> {
			Site.Manga manga = entries.get(position);
			Intent intent = new Intent(this, DetailsActivity.class);
			intent.putExtra("url", manga.url);
			intent.putExtra("title", manga.title);
			startActivity(intent);
		});
		root.addView(list, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		more = new Button(this);
		more.setText("Load more");
		more.setVisibility(View.GONE);
		more.setOnClickListener(v -> {
			page++;
			loadPopular();
		});
		root.addView(more);
		setContentView(root);

		search.setOnClickListener(v -> startSearch());
		searchInput.setOnEditorActionListener((v, actionId, event) -> {
			startSearch();
			return true;
		});
		loadPopular();
	}

	private void startSearch() {
		String text = searchInput.getText().toString().trim();
		searching = !text.isEmpty();
		query = text;
		page = 1;
		entries.clear();
		adapter.notifyDataSetChanged();
		more.setVisibility(View.GONE);
		if (searching) {
			loadSearch();
		} else {
			loadPopular();
		}
	}

	private void loadPopular() {
		status.setText("Loading popular…");
		new Thread(() -> {
			try {
				String url = Site.BASE + "/series" + (page > 1 ? "?page=" + page : "");
				String html = Site.get(url);
				List<Site.Manga> loaded = Site.popular(html);
				boolean next = Site.hasNextPage(html, url);
				runOnUiThread(() -> show(loaded, next));
			} catch (Exception e) {
				fail(e);
			}
		}).start();
	}

	private void loadSearch() {
		status.setText("Searching…");
		new Thread(() -> {
			try {
				String url = Site.BASE + "/search?keyword=" + URLEncoder.encode(query, "UTF-8");
				List<Site.Manga> loaded = Site.search(Site.get(url), url);
				runOnUiThread(() -> show(loaded, false));
			} catch (Exception e) {
				fail(e);
			}
		}).start();
	}

	private void show(List<Site.Manga> loaded, boolean hasNext) {
		entries.addAll(loaded);
		adapter.notifyDataSetChanged();
		status.setText(entries.isEmpty() ? "No results" : entries.size() + " manga");
		more.setVisibility(searching || !hasNext ? View.GONE : View.VISIBLE);
	}

	private void fail(Exception e) {
		runOnUiThread(() -> {
			status.setText("Failed: " + e.getMessage());
			Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
		});
	}

	private int dp(int value) {
		return (int) (value * getResources().getDisplayMetrics().density);
	}
}
