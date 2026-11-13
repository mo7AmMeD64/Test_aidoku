package dev.teamx.reader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Scraping + network for olympustaff.com (the Team X source). */
public final class Site {
	public static final String BASE = "https://olympustaff.com";
	private static final String USER_AGENT =
		"Mozilla/5.0 (Linux; Android 14; Mobile) TeamXReader/1.0";

	private Site() {
	}

	public static class Manga {
		public String url = "";
		public String title = "";
		public String cover = "";
	}

	public static class Details {
		public String title = "";
		public String cover = "";
		public String description = "";
		public List<String> tags = new ArrayList<>();
	}

	public static class Chapter {
		public String url = "";
		public String title = "";
		public long date;
	}

	public static String get(String address) throws IOException {
		return new String(getBytes(address), StandardCharsets.UTF_8);
	}

	public static byte[] getBytes(String address) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
		connection.setConnectTimeout(20000);
		connection.setReadTimeout(30000);
		connection.setInstanceFollowRedirects(true);
		connection.setRequestProperty("User-Agent", USER_AGENT);
		try (InputStream in = connection.getInputStream()) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[16384];
			int read;
			while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}

	public static List<Manga> popular(String html) {
		Document document = Jsoup.parse(html, BASE + "/series");
		List<Manga> entries = new ArrayList<>();
		for (Element element : document.select("div.listupd div.bsx")) {
			Element link = element.selectFirst("a");
			if (link == null) continue;
			Manga manga = new Manga();
			manga.url = link.attr("abs:href");
			manga.title = link.attr("title");
			if (manga.title.isEmpty()) manga.title = link.text().trim();
			Element image = element.selectFirst("img");
			if (image != null) manga.cover = firstUrl(image.attr("abs:data-src"), image.attr("abs:src"));
			if (!manga.url.isEmpty()) entries.add(manga);
		}
		return entries;
	}

	public static List<Manga> search(String html, String queryUrl) {
		Document document = Jsoup.parse(html, queryUrl);
		List<Manga> entries = new ArrayList<>();
		for (Element element : document.select("div.tx-grid a.tx-card")) {
			Manga manga = new Manga();
			manga.url = element.attr("abs:href");
			Element title = element.selectFirst("h3");
			manga.title = title != null ? title.text().trim() : element.attr("title");
			Element image = element.selectFirst("img");
			if (image != null) manga.cover = firstUrl(image.attr("abs:src"), image.attr("abs:data-src"));
			if (!manga.url.isEmpty() && !manga.title.isEmpty()) entries.add(manga);
		}
		return entries;
	}

	public static Details details(String html, String pageUrl) {
		Document document = Jsoup.parse(html, pageUrl);
		Details details = new Details();

		Element title = document.selectFirst("div.author-info-title h1");
		if (title == null) title = document.selectFirst("h1");
		if (title != null) details.title = title.text().trim();

		Element cover = document.selectFirst("div.text-right img");
		if (cover != null) details.cover = firstUrl(cover.attr("abs:src"), cover.attr("abs:data-src"));

		Element description = document.selectFirst("div.review-content");
		if (description != null) details.description = description.text().trim();

		for (Element tag : document.select("div.review-author-info a")) {
			String text = tag.text().trim();
			if (!text.isEmpty()) details.tags.add(text);
		}
		return details;
	}

	public static List<Chapter> chapters(String html, String pageUrl) {
		Document document = Jsoup.parse(html, pageUrl);
		List<Chapter> chapters = new ArrayList<>();
		for (Element element : document.select("div.chapter-card")) {
			if (element.selectFirst("span.locked") != null) continue;
			Element link = element.selectFirst("a");
			if (link == null) continue;
			String url = link.attr("abs:href");
			if (url.isEmpty()) continue;

			String number = element.attr("data-number");
			String extra = "";
			Element extraTitle = element.selectFirst("div.chapter-info div.chapter-title");
			if (extraTitle != null) extra = extraTitle.text().trim();

			String title = number.isEmpty() ? "Chapter" : "الفصل " + number;
			if (!extra.isEmpty()
				&& !extra.equals(number)
				&& !extra.equals("الفصل " + number)
				&& !extra.equals("الفصل رقم " + number)) {
				title += " - " + extra;
			}

			Chapter chapter = new Chapter();
			chapter.url = url;
			chapter.title = title;
			try {
				chapter.date = Long.parseLong(element.attr("data-date")) * 1000L;
			} catch (NumberFormatException ignored) {
			}
			chapters.add(chapter);
		}
		return chapters;
	}

	public static boolean hasNextPage(String html, String pageUrl) {
		return Jsoup.parse(html, pageUrl).selectFirst("a[rel=next]") != null;
	}

	public static int lastChapterPage(String html, String pageUrl) {		Document document = Jsoup.parse(html, pageUrl);
		int last = 1;
		for (Element link : document.select("ul.pagination a.page-link")) {
			try {
				last = Math.max(last, Integer.parseInt(link.text().trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return last;
	}

	public static List<String> pages(String html, String pageUrl) {
		Document document = Jsoup.parse(html, pageUrl);
		List<String> pages = new ArrayList<>();
		for (Element element : document.select("div.image_list img, div.image_list canvas")) {
			String url = firstUrl(element.attr("abs:src"), element.attr("abs:data-src"));
			if (!url.isEmpty()) pages.add(url);
		}
		return pages;
	}

	public static String chapterPageUrl(String chapterUrl, int page) {
		return page <= 1 ? chapterUrl : chapterUrl + "?page=" + page;
	}

	private static String firstUrl(String first, String second) {
		return first == null || first.isEmpty() ? (second == null ? "" : second) : first;
	}
}
