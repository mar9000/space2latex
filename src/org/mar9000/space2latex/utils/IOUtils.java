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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class IOUtils {

	public static void writeDataToFile(byte[] data, File f) throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(data);
		fos.close();
	}

	public static String readFileAsString(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String inputLine;
		StringBuffer buffer = new StringBuffer();
		while ((inputLine = reader.readLine()) != null) {
			buffer.append(inputLine).append("\n");
		}
		reader.close();
		return buffer.toString();
	}

	public static String readResourceAsString(String resourceName) throws IOException, URISyntaxException {
		URL resourceURL = IOUtils.class.getResource(resourceName);
		File resource = new File(resourceURL.toURI());

		BufferedReader reader = new BufferedReader(new FileReader(resource));
		String inputLine;
		StringBuffer buffer = new StringBuffer();
		while ((inputLine = reader.readLine()) != null) {
			buffer.append(inputLine).append("\n");
		}
		reader.close();
		return buffer.toString();
	}

	public static void saveStringToFile(String content, File destFile) throws IOException {
		FileWriter writer = new FileWriter(destFile);
		writer.write(content);
		writer.close();
	}
	
	private static final byte[] MISSING_IMAGE_DATA = "MISSING_IMAGE_DATA".getBytes();
	public static byte[] getImageFromURL(String urlString) throws MalformedURLException {
		URL url = new URL(urlString);
		//
		URLConnection urlConn = null;
		int count = ConfluenceRESTUtils.MAX_REQUEST_TRY;
		while (urlConn == null && count > 0) {
			try {
				urlConn = url.openConnection();
			} catch (IOException e) {
				count--;
				urlConn = null;
			}
		}
		if (urlConn == null) {
			System.err.println("Max retry reached to open connection for URL: " + urlString);
			return MISSING_IMAGE_DATA;
		}
		//
		if (!urlConn.getHeaderField("Content-Type").startsWith("image")) {
			// Try Location returned in case of HTTP error 301.
			String location = urlConn.getHeaderField("Location");
			if (location == null) {
				System.err.println("Content-Type returned is not an image and does not have a Location header field.");
				return MISSING_IMAGE_DATA;
			}
			url = new URL(location);
		}
		//
		count = ConfluenceRESTUtils.MAX_REQUEST_TRY;
		InputStream in = null;
		while (in == null && count > 0) {
			try {
				in = new BufferedInputStream(url.openStream());
			} catch (IOException e) {
				count --;
				in = null;
			}
		}
		if (in == null) {
			System.err.println("Max retry reached to open input stream for URL: " + urlString);
			return MISSING_IMAGE_DATA;
		}
		//
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int n = 0;
		try {
			while (-1 != (n = in.read(buf))) {
				out.write(buf, 0, n);
			}
			out.close();
			in.close();
		} catch (IOException e) {
			System.err.println("Error during read line from input stream of URL: " + urlString);
			return MISSING_IMAGE_DATA;
		}
		byte[] response = out.toByteArray();
		return response;
	}

}
