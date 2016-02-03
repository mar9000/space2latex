/*
 * Copyright 2016 Marco Lombardo
 * https://github.com/mar9000
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mar9000.space2latex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mar9000.space2latex.log.S2LLogUtils;
import org.mar9000.space2latex.utils.ConfluenceRESTUtils;
import org.mar9000.space2latex.utils.IOUtils;

public class WikiPage {
	
	private static Logger LOGGER = S2LLogUtils.getLogger(WikiPage.class.getName());
	
	public static final String JSON_TYPE_ATTR = "type";
	public static final String JSON_TYPE_VALUE_PAGE = "page";
	public static final String JSON_PAGE_ATTR = "page";
	public static final String JSON_SIZE_ATTR = "size";
	public static final String JSON_LIMIT_ATTR = "limit";
	public static final String JSON_RESULTS_ATTR = "results";
	public static final String JSON_ID_ATTR = "id";
	public static final String JSON_TITLE_ATTR = "title";
	public static final String JSON_STORAGE_ATTR = "storage";
	public static final String JSON_BODY_ATTR = "body";
	public static final String JSON_LINKS_ATTR = "_links";
	public static final String JSON_SELF_ATTR = "self";
	public static final String JSON_VALUE_ATTR = "value";
	
	public JSONObject json = null;
	public String title = null;
	public String id = null;
	public String storage = null;
	public Map<String, WikiImage> images = new HashMap<String, WikiImage>();
	public Element pageContent = null;
	public boolean alreadyIncluded = false;
	public boolean isExcluded = false;
	
	public WikiPage(JSONObject json, String title, String id, String storage) {
		this.json = json;
		this.title = title;
		this.id = id;
		this.storage = storage;
	}
	
	public static WikiPage getWikiPage(JSONObject jsonPage) throws MalformedURLException {
		String title = jsonPage.getString(JSON_TITLE_ATTR);
		String id = jsonPage.getString(JSON_ID_ATTR);
		String storage = null;
		JSONObject bodyObj = jsonPage.optJSONObject(JSON_BODY_ATTR);
		if (bodyObj == null) {
			String url = jsonPage.getJSONObject(JSON_LINKS_ATTR).getString(JSON_SELF_ATTR);
			JSONObject pageExpanded = ConfluenceRESTUtils.getURLResponse(url + "?expand="
					+ JSON_BODY_ATTR + "." + JSON_STORAGE_ATTR);
			storage = pageExpanded.getJSONObject(JSON_BODY_ATTR).getJSONObject(JSON_STORAGE_ATTR)
					.getString(JSON_VALUE_ATTR);
		} else {
			storage = bodyObj.getJSONObject(JSON_STORAGE_ATTR).getString(JSON_VALUE_ATTR);
		}
		WikiPage page = new WikiPage(jsonPage, title, id, storage);
		LOGGER.log(Level.INFO, "  Page downloaded: {0}", title);
		downloadWikiPageImages(page);
		return page;
	}
	
	public static void downloadWikiPageImages(WikiPage page) throws MalformedURLException {
		String pageUrl = page.json.getJSONObject(JSON_LINKS_ATTR).getString(JSON_SELF_ATTR);
		Document document = Jsoup.parseBodyFragment(page.storage);
		document.outputSettings().prettyPrint(false);
		Elements images = document.select("ac|image");
		if (images.size() > 0)
			LOGGER.info("  Download images:");
		for (Element element : images) {
			String downloadURL = null;
			String imageKey = null;
			// Attachment?
			Elements refs = element.select("ri|attachment");
			WikiImage image = new WikiImage();
			image.pageId = page.id;
			image.acImage = element.outerHtml();
			//
			if (refs.size() > 0) {   // Attachment.
				Element riAttachment = refs.get(0);
				imageKey = riAttachment.attr("ri:filename");
				Elements riPages = riAttachment.select("ri|page");
				// Thumbnails are not found with "child/attachment" URL schema.
				boolean isThumbnail = "true".equals(element.attr("ac:thumbnail"));
				String queryURL = null;
				if (!isThumbnail) {
					queryURL = pageUrl + "/child/attachment?filename=" + URLEncoder.encode(imageKey);
				} else {
					// For thumbnail we construct directly the downloadURL without queryURL.
					downloadURL = pageUrl.substring(0, pageUrl.indexOf("/rest/api"))
							+ "/download/thumbnails/" + page.id + "/" + URLEncoder.encode(imageKey);
				}
				if (riPages.size() > 0) {
					// The attachment is related with another page.
					Element riPage = riPages.get(0);
					String space = riPage.attr("ri:space-key");
					String contentTitle = riPage.attr("ri:content-title").replaceAll(" ", "%20");					
					String self = page.json.getJSONObject(JSON_LINKS_ATTR).getString(JSON_SELF_ATTR);
					String newQueryURL = self.substring(0, self.lastIndexOf('/'))
							+ "?title=" + contentTitle + "&spaceKey=" + space;
					JSONObject jsonNewQuery = ConfluenceRESTUtils.getURLResponse(newQueryURL);
					if (jsonNewQuery.getInt(JSON_SIZE_ATTR) == 0)
						throw new RuntimeException("Page \"" + contentTitle + "\" in space " + space + " not found.");
					JSONObject jsonNewPage = (JSONObject)jsonNewQuery.getJSONArray(JSON_RESULTS_ATTR).get(0);
					image.pageId = jsonNewPage.getString(JSON_ID_ATTR);
					// Overwrite queryURL.
					String newPageUrl = jsonNewPage.getJSONObject(JSON_LINKS_ATTR).getString(JSON_SELF_ATTR);
					queryURL = newPageUrl + "/child/attachment?filename=" + URLEncoder.encode(imageKey);
				}
				if (!isThumbnail)
					downloadURL = getAttachmentDownloadURL(queryURL);
			} else {
				refs = element.select("ri|url");
				if (refs.size() > 0) {   // URL.
					downloadURL = refs.get(0).attr("ri:value");
					URL tempURL = new URL(downloadURL);
					String urlPath = tempURL.getPath();
					imageKey = urlPath.substring(urlPath.lastIndexOf('/')+1);
				} else {
					throw new RuntimeException("Image format unknown: " + element.toString());
				}
			}
			// Download the image data.
			image.filename = imageKey.replace(' ', '_');   // Space are not handled by LaTeX.
			if (downloadURL != null) {
				LOGGER.log(Level.INFO, "    about to download image {0}/{1}", new Object[]{image.pageId, image.filename});
				image.data = IOUtils.getImageFromURL(downloadURL);
			} else {
				LOGGER.log(Level.SEVERE, "    NULL download URL for page/image: {0}/{1}"
						, new Object[]{image.pageId, image.filename});
			}
			page.images.put(imageKey, image);
		}
	}

	private static String getAttachmentDownloadURL(String queryURL) throws MalformedURLException {
		JSONObject response = ConfluenceRESTUtils.getURLResponse(queryURL);
		if (response.getInt(JSON_SIZE_ATTR) == 0) {
			LOGGER.log(Level.SEVERE, "Image at URL non found: {0}", queryURL);
			LOGGER.log(Level.SEVERE, "Response: {0}", response.toString());
			return null;
		}
		JSONObject firstResult = (JSONObject)response.getJSONArray(JSON_RESULTS_ATTR).get(0);
		String self = firstResult.getJSONObject(JSON_LINKS_ATTR).getString(JSON_SELF_ATTR);
		String protocol = self.substring(0, self.indexOf("/rest/api"));
		return protocol + firstResult.getJSONObject(JSON_LINKS_ATTR).getString("download");
	}
	
	public static final String PAGE_PREFIX = "page-";
	public static final String PAGE_EXTENSION = ".html";
	public void save(File destDir) throws IOException {
		File destFile = new File(destDir, PAGE_PREFIX + getPageName() + PAGE_EXTENSION);
		System.out.println("  About to save page \"" + title + "\" to " + destFile.getAbsolutePath());
		FileWriter writer = new FileWriter(destFile);
		writer.write("<page title=\"" + this.title + "\" id=\"" +this.id + "\">\n");
		// Save images references to be used in format.
		writer.write("<wikiimages>\n");
		for (String imageKey : this.images.keySet()) {
			WikiImage image = this.images.get(imageKey);
			// Write the image element.
			writer.write("<wikiimage pageid=\"" + image.pageId+"\" filename=\""+image.filename + "\">\n");
			writer.write(image.acImage);
			writer.write("\n");
			writer.write("</wikiimage>\n");
			// Write image to disk.
			// Images can reference images from other page/space so I have to check this for every image.
			File imagesDir = new File(destDir, image.pageId);
			if (!imagesDir.exists())
				imagesDir.mkdir();
			//
			if (image.data == null)
				continue;
			File imageFile = new File(imagesDir, image.filename);
			System.out.println("    about to save image \"" + image.filename + "\" to " + imageFile.getAbsolutePath());
			IOUtils.writeDataToFile(image.data, imageFile);
		}
		writer.write("</wikiimages>\n");
		// Content.
		writer.write("<content>");
		writer.write(this.storage);
		writer.write("</content>\n</page>\n");
		writer.close();
	}
	
	public static WikiPage loadForFormat(File file) throws IOException {
		String fileContent = IOUtils.readFileAsString(file);
		Document doc = Jsoup.parseBodyFragment(fileContent);
		// Maintain input string.
		doc.outputSettings().prettyPrint(false);
		Element body = doc.body();
		Element pageElement = body.select("page").first();
		String title = pageElement.attr("title");
		String id = pageElement.attr("id");
		Element pageContent = pageElement.select("content").first();
		WikiPage page = new WikiPage(null, title, id, pageContent.html());
		page.pageContent = pageContent;
		// Images.
		Elements images = body.select("wikiimages").first().select("wikiimage");
		for (Element imageElement : images) {
			WikiImage image = new WikiImage();
			String acKey = imageElement.select("ac|image").first().outerHtml();
			image.filename = imageElement.attr("pageid") + "/" + imageElement.attr("filename");
			page.images.put(acKey, image);
		}
		return page;
	}
	
	// ---------- Utilities.   ----------
	
	public String getPageName() {
		return getPageName(title);
	}
	
	public static String getPageName(String title) {
		return title.replaceAll(" ", "+");
	}
	
}
