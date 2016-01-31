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
	private List<Part> parts = new ArrayList<Part>();
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
	
	public void addPart(Part part) {
		parts.add(part);
	}
	
	public void addChapter(Part part, Chapter chapter) {
		part.addChapter(chapter);
		chapters.put(chapter.title, chapter);
		if (chapter.number == -1)
			chapter.number = chapters.size();
	}
	
	private Part appendixPart = null;
	public Chapter getChapter(String title, boolean create) {
		Chapter c = chapters.get(title);
		if (c == null && create) {
			if (appendixPart == null) {
				appendixPart = new Part("Appendix");
				addPart(appendixPart);
			}
			c = new Chapter(title);
			addChapter(appendixPart, c);
		}
		return c;
	}
	
	public Part[] getParts() {
		Part[] a = new Part[parts.size()];
		parts.toArray(a);
		return a;
	}

}
