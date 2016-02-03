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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mar9000.space2latex.latex.Chapter;
import org.mar9000.space2latex.latex.Formatter;
import org.mar9000.space2latex.latex.LatexDocument;
import org.mar9000.space2latex.latex.Part;
import org.mar9000.space2latex.latex.TOC;
import org.mar9000.space2latex.utils.ConfluenceRESTUtils;
import org.mar9000.space2latex.utils.IOUtils;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

public class Space2Latex {
	
	private static Logger LOGGER = Logger.getLogger("Space2Latex");

	public static void main(String[] args) throws Exception {
		Space2Latex s2l = new Space2Latex(args);
		s2l.execute();
	}
	
	public final static String SWITCH_URL = "--url";
	public final static String SWITCH_DEST_DIR = "--dest-dir";
	public final static String SWITCH_LATEX_DIR = "--latex-dir";
	public final static String SWITCH_COMMAND = "--command";
	public final static String SWITCH_EXCLUDE = "--exclude";
	public final static String SWITCH_START = "--start";
	public final static String SWITCH_LIMIT = "--limit";
	public final static String SWITCH_INCLUDE_ALL = "--include-all";
	public final static String SWITCH_DOCUMENTS_DEF = "--documents-def";
	public final static String COMMAND_DOWNLOAD = "download";
	public final static String COMMAND_FORMAT = "format";
	private HashMap<String, String> params = new HashMap<String, String>();
	private List<String> excludes = new ArrayList<String>();
	public Space2Latex(String[] args) {
		// Default parameters.
		params.put(SWITCH_INCLUDE_ALL, "true");
		// Load command line parameters.
		int i = 0;
		while (i < args.length) {
			if (args[i].startsWith(SWITCH_COMMAND)) {
				params.put(SWITCH_COMMAND, getParameterValue(args[i]));
			} else if (args[i].startsWith(SWITCH_URL)) {
				params.put(SWITCH_URL, getParameterValue(args[i]));
			} else if (args[i].startsWith(SWITCH_DEST_DIR)) {
				params.put(SWITCH_DEST_DIR, getParameterValue(args[i]));
			} else if (args[i].startsWith(SWITCH_LATEX_DIR)) {
				params.put(SWITCH_LATEX_DIR, getParameterValue(args[i]));
			} else if (args[i].startsWith(SWITCH_DOCUMENTS_DEF)) {
				params.put(SWITCH_DOCUMENTS_DEF, getParameterValue(args[i]));
			} else if (args[i].startsWith(SWITCH_EXCLUDE)) {
				excludes.add(WikiPage.PAGE_PREFIX + getParameterValue(args[i]) + WikiPage.PAGE_EXTENSION);
			} else if (args[i].startsWith(SWITCH_START)) {
				params.put(SWITCH_START, getParameterValue(args[i]));
			} else if (args[i].startsWith(SWITCH_INCLUDE_ALL)) {
				params.put(SWITCH_INCLUDE_ALL, getParameterValue(args[i]));
			} else if (args[i].startsWith(SWITCH_LIMIT)) {
				params.put(SWITCH_LIMIT, getParameterValue(args[i]));
			}
			i++;
		}
	}
	
	private void execute() {
		String command = params.get(SWITCH_COMMAND);
		if (command != null) {
			if (command.equals(COMMAND_DOWNLOAD)) {
				executeDownload();
			} else if (command.equals(COMMAND_FORMAT)) {
				executeFormat();
			} else {
				showUsage();
				showError("Command " + command + " unknown.");
			}
		} else {
			showUsage();
			showError(SWITCH_COMMAND + " parameter is mandatory.");
		}
	}
	
	private void executeDownload() {
		String url = params.get(SWITCH_URL);
		if (url == null) {
			showError(SWITCH_URL + " parameter is mandatory.");
		} else {
			LOGGER.log(Level.INFO, "Download URL: {}", url);
		}
		//
		String destDirName = params.get(SWITCH_DEST_DIR);
		if (destDirName == null)
			destDirName = ".";
		File destDir = new File(destDirName);
		if (!destDir.isDirectory()) {
			showError("Destination directory " + destDirName + " is not a directory.");
		} else {
			LOGGER.log(Level.INFO, "Destination directory: {}", destDir.getAbsolutePath());
		}
		// Download pages returned by the passed URL.
		int start = params.get(SWITCH_START) != null ?
				Integer.parseInt(params.get(SWITCH_START)) : 0;
		int limit = params.get(SWITCH_LIMIT) != null ?
				Integer.parseInt(params.get(SWITCH_LIMIT)) : ConfluenceRESTUtils.LIMIT_FOR_REQUEST;
		System.out.println("Download limits: start=" + start + ", limit=" + limit);
		try {
			ConfluenceRESTUtils.getPages(url, start, limit, destDir);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			showError("MalformedURLException for URL: " + url);
		}
	}
	
	private boolean createMissingChapters = false;
	private File[] pageFiles = null;
	private File destDir = null;
	private File latexDir = null;
	private Map<String, WikiPage> pages = null;
	private void executeFormat() {
		// Get source (dest-dir) directory and target (latex-dir) directory.
		String destDirName = params.get(SWITCH_DEST_DIR);
		if (destDirName == null)
			destDirName = ".";
		destDir = new File(destDirName);
		if (!destDir.isDirectory()) {
			showError("Destination directory " + destDirName + " is not a directory.");
		} else {
			LOGGER.log(Level.INFO, "Destination directory used during download: {0}", destDir);
		}
		// Latex dir.
		String latexDirName = params.get(SWITCH_LATEX_DIR);
		if (latexDirName == null)
			latexDirName = ".";
		latexDir = new File(latexDirName);
		if (!latexDir.isDirectory()) {
			showError("Destination directory for latex files " + latexDirName + " is not a directory.");
		} else {
			System.out.println("Generated files will go to: " + latexDir);
		}
		// Document definition.
		String documentsDefPath = params.get(SWITCH_DOCUMENTS_DEF);
		if (documentsDefPath == null)
			documentsDefPath = "/templates/documents-def.xml";
		File documentsDef = new File(documentsDefPath);
		if (!documentsDef.exists()) {
			showError("Documents definition does not exists: " + documentsDefPath);
		} else if (documentsDef.isDirectory()) {
			showError("Documents definition " + documentsDefPath + " is a directory.");
		} else {
			System.out.println("Pages will be processed accordly to: " + documentsDefPath);
		}
		// Create missing chapter?
		createMissingChapters = params.get(SWITCH_INCLUDE_ALL).equals("true");
		
		// Load page files from dir. used during download.
		pageFiles = destDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(WikiPage.PAGE_EXTENSION);
			}
		});
		// First load all pages, needed to resolve "include" macros.
		pages = new HashMap<String, WikiPage>();
		for (int f = 0; f < pageFiles.length; f++) {
			try {
				WikiPage page = WikiPage.loadForFormat(pageFiles[f]);
				pages.put(page.title, page);
				// Exclude?
				if (excludes.contains(pageFiles[f].getName())) {
					page.isExcluded = true;
					System.out.println("Page will be excluded as requested: " + page.title);
				}
			} catch (Exception e) {
				e.printStackTrace();
				showError("Error loading file: " + pageFiles[f].getAbsolutePath());
			}
		}
		
		// Load documents.
		String documentsDefData = null;
		try {
			documentsDefData = IOUtils.readFileAsString(new File(documentsDefPath));
		} catch (Exception e) {
			e.printStackTrace();
			showError(documentsDefPath + " read error.");
		}
		Document data = Jsoup.parse(documentsDefData);
		Element body = data.body();
		Element documents = body.select("documents").first();
		Elements documentsList = documents.select("document");
		// Format each document.
		for (Element document : documentsList) {
			if (document.attr("enabled").equals("false")) {
				LOGGER.log(Level.INFO, "Document is not enabled: {0}", document.attr("title"));
				continue;
			}
			formatDocument(document);
		}
		
		// Display result about not included pages.
		System.out.println("\nPages not inclded in any document:");
		for (String pageTitle : pages.keySet()) {
			WikiPage page = pages.get(pageTitle);
			if (!page.isExcluded && !page.alreadyIncluded) {
				System.out.println("  " + pageTitle);
			}
		}
	}
	
	/**
	 * Format a single document.
	 */
	private void formatDocument(Element document) {
		Elements documentParts = document.children();
		LatexDocument latexDocument = new LatexDocument(document.attr("title")
				, document.attr("author"), document.attr("date")
				, document.attr("baseurl"), document.attr("space"));
		for (Element documentPart : documentParts) {
			String nodeName = documentPart.nodeName();
			if (nodeName.equals("toc")) {
				latexDocument.addDocumentPart(new TOC());
			} else if (nodeName.equals("part")) {
				Element part = documentPart;
				Part latexPart = new Part(part.attr("title"));
				latexDocument.addDocumentPart(latexPart);
				Elements chapters = part.select("chapter");
				for (Element chapter : chapters) {
					Chapter latexChapter = new Chapter(chapter.attr("title"));
					latexDocument.addChapter(latexPart, latexChapter);
				}
			} else if (nodeName.equals("chapter")) {
				Chapter c = new Chapter(documentPart.attr("title"));
				if (documentPart.attr("inline").equals("true"))
					c.inline = true;
				latexDocument.addDocumentPart(c);
			}
		}
		
		// Add content to chapters using page files.
		System.out.println("");
		Formatter formatter = new Formatter(destDir, pages, latexDocument);
		formatter.format(createMissingChapters);
		
		// Output file to latex directory using String Template.
		STGroup documentStg = new STGroupFile("templates/default-latex-template.stg");
        ST documentTemplate = documentStg.getInstanceOf("document");
        documentTemplate.add("d", latexDocument);
        String latexOutputFileName = null;
        try {
        	latexOutputFileName = WikiPage.getPageName(latexDocument.title) + ".tex";
        	File latexOutputFile = new File(latexDir, latexOutputFileName);
			IOUtils.saveStringToFile(documentTemplate.render(), latexOutputFile);
		} catch (IOException e) {
			e.printStackTrace();
			showError("Unable to save latex file " + latexOutputFileName);
		}
	}
	
	// Utilities.
	
	private String getParameterValue(String param) {
		return param.substring(param.indexOf('=')+1);
	}
	
	private void showError(String message) {
		System.err.println(message);
		System.exit(1);
	}
	
	private void showUsage() {
		System.out.println("usage: Stage2Latex [--command=download --url=<RESTful URL to query the documentation>]");
		System.out.println("                                       [--dest-dir=<destination directory for download content>]]");
		System.out.println("                   [--command=format [--dest-dir=<directory with downloaded content>]");
		System.out.println("                                     [--latex-dir=<directory to store generated latex files>]");
		System.out.println("                                     [--exclude=<comma separated page ids to exclude>]");
		System.out.println("                                     [--merge=<true/false, merge pages into 1 document, default true>]]");
	}

}
