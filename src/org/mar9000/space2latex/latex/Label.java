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

public class Label implements LatexElement {
	
	public String pageTitle = null;
	public String anchor = null;
	public boolean referenced = false;
	public boolean defined = false;
	
	public Label(String pageTitle, String anchor) {
		this.pageTitle = pageTitle;
		this.anchor = anchor;
	}
	
	public String getLabelString() {
		return getLabelString(pageTitle, anchor);
	}
	
	public String getHtmlTitle() {
		return pageTitle.replace(' ' , '+').replace("&", "\\&");
	}
	
	public static String getLabelString(String pageTitle, String anchor) {
		String labelString = pageTitle;
		if (anchor != null && anchor.length() > 0)
			labelString += "-" + anchor;
		return labelString;
	}

}
