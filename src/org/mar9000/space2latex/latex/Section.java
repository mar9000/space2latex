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

public class Section extends LatexElements implements TitledElement {
	
	public static final String TYPE_SECTION = "section";
	public static final String TYPE_SUBSECTION = "subsection";
	public static final String TYPE_SUBSUBSECTION = "subsubsection";
	public static final String TYPE_PARAGRAPH = "paragraph";
	
	public Section(String type) {
		this.type = type;
	}
	
	private String title = null;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	private String type = null;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
}
