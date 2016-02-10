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
package org.mar9000.space2latex.latex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LatexDocument {
	
	public String title = null;
	public String author = null;
	public String date = null;
	public String baseUrl = null;
	public String space = null;
	public VerbatimDefs verbatimDefs = null;
	private List<DocumentPart> parts = new ArrayList<DocumentPart>();
	private Map<String, Chapter> chapters = new HashMap<String, Chapter>();
	
	public LatexDocument(String title, String author, String date, String url, String space) {
		this.title = title;
		this.author = author;
		this.verbatimDefs = new VerbatimDefs();
		if (date == null)
			this.date = "\today";
		else {
			this.date = date;
		}
		this.baseUrl = url;
		this.space = space;
	}
	
	public void addDocumentPart(DocumentPart documentPart) {
		parts.add(documentPart);
		if (documentPart instanceof Chapter) {
			Chapter c = (Chapter)documentPart;
			chapters.put(c.title, c);
		}
	}
	
	private int chapterCounter = 1;
	public void addChapter(Part part, Chapter chapter) {
		part.addChapter(chapter);
		chapters.put(chapter.title, chapter);
		// Chapter number not yet assigned and not inline.
		if (chapter.number == -1 && !chapter.inline)
			chapter.number = chapterCounter++;
	}
	
	private Part appendixPart = null;
	public Chapter getChapter(String title, boolean create) {
		Chapter c = chapters.get(title);
		if (c == null && create) {
			if (appendixPart == null) {
				appendixPart = new Part("Appendix");
				addDocumentPart(appendixPart);
			}
			c = new Chapter(title);
			addChapter(appendixPart, c);
		}
		return c;
	}
	
	public DocumentPart[] getParts() {
		DocumentPart[] a = new DocumentPart[parts.size()];
		parts.toArray(a);
		return a;
	}

}
