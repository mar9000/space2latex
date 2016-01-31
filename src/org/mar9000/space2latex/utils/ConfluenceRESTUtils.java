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
package org.mar9000.space2latex.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mar9000.space2latex.WikiPage;

public class ConfluenceRESTUtils {

	public static final int LIMIT_FOR_REQUEST = 25;
	public static final int MAX_REQUEST_TRY = 3;
	
	public static void getPages(String urlString, int start, int limit, File destDir) throws MalformedURLException {
		int count = 0;
		int currentLimit = limit < LIMIT_FOR_REQUEST ? limit : LIMIT_FOR_REQUEST;
		while (true) {
			System.out.println("Request pages: start=" + start + ", limit=" + currentLimit);
			JSONObject json = getURLResponse(urlString, start, currentLimit);
			String type = json.optString(WikiPage.JSON_TYPE_ATTR);
			if (WikiPage.JSON_TYPE_VALUE_PAGE.equals(type)) {
				// Single page query,
				WikiPage page = WikiPage.getWikiPage(json);
				try {
					page.save(destDir);
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Error saving page " + page.title + "(" + page.id + ")");
				}
				return;
			} else if (json.optJSONObject(WikiPage.JSON_PAGE_ATTR) != null) {
				// A space query.
				JSONObject pageArray = json.getJSONObject(WikiPage.JSON_PAGE_ATTR);
				JSONArray results = pageArray.getJSONArray(WikiPage.JSON_RESULTS_ATTR);
				if (results.length() == 0)
					return;
				// Save pages returned by last REST request.
				for (int p = 0; p < results.length(); p++) {
					WikiPage page = WikiPage.getWikiPage((JSONObject)results.get(p));
					try {
						page.save(destDir);
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Error saving page " + page.title + "(" + page.id + ")");
					}
				}
				// Finish?
				// Less records than requested.
				if (results.length() < currentLimit) {
					return;
				}
				// Download next chunk.
				count += results.length();
				if (count >= limit)   // Greater than should never happen.
					return;
				start += results.length();
				currentLimit = (limit-count) < LIMIT_FOR_REQUEST ? limit-count : LIMIT_FOR_REQUEST;
			} else {
				throw new RuntimeException("Unknown response from server:" + json.toString());
			}
		}
	}

	public static JSONObject getURLResponse(String urlString) throws MalformedURLException {
		return getURLResponse(urlString, 0, LIMIT_FOR_REQUEST);
	}

	public static JSONObject getURLResponse(String urlString, int start, int limit) throws MalformedURLException {
		StringBuffer buffer = new StringBuffer();
		// Always add limit.
		URL receivedURL = new URL(urlString);
		if (receivedURL.getQuery() == null)
			urlString += "?";
		else
			urlString += "&";
		urlString += "limit=" + limit;
		if (start != 0) {
			urlString += "&start=" + start;
		}
		URL requestURL = new URL(urlString);
		URLConnection urlConn = null;
		int count = MAX_REQUEST_TRY;
		while (urlConn == null && count > 0) {
			try {
				urlConn = requestURL.openConnection();
			} catch (IOException e) {
				try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {}
				count--;
				urlConn = null;
			}
		}
		if (urlConn == null)
			throw new RuntimeException("Max retry count reached for open URL: " + urlString);
		//
		BufferedReader reader = null;
		count = MAX_REQUEST_TRY;
		while (reader == null && count > 0) {
			try {
				reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			} catch (IOException e) {
				reader = null;
				count--;
				try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {}
			}
		}
		if (reader == null)
			throw new RuntimeException("Max retry count reached for open InputStream: " + urlString);
		//
		try {
			String inputLine;
			while ((inputLine = reader.readLine()) != null) {
				buffer.append(inputLine);
			}
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException("Error reading line from URL: " + urlString);
		}
		// Return.
		return new JSONObject(buffer.toString());
	}

}
