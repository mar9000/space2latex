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

public class Emoticon implements LatexElement {
	
	public int type = -1;
	public static final int LIGHT_ON = 0;
	public static final int WARNING = 1;
	public static final int INFORMATION = 2;
	public static final int TIP = 3;
	public static final int SMILE = 4;
	
	public Emoticon(int type) {
		this.type = type;
	}
	
	public String getTypeString() {
		if (isLight())
			return "light";
		if (isWarning())
			return "warning";
		if (isInformation())
			return "information";
		if (isTip())
			return "tip";
		if (isSmile())
			return "smile";
		return "unknown";
	}
	
	public boolean isLight() {
		return type == LIGHT_ON;
	}
	
	public boolean isWarning() {
		return type == WARNING;
	}
	
	public boolean isInformation() {
		return type == INFORMATION;
	}
	
	public boolean isTip() {
		return type == TIP;
	}
	
	public boolean isSmile() {
		return type == SMILE;
	}
	
}
