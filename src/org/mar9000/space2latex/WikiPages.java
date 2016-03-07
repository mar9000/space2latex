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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implements few methods of a non-case sensitive String Map.
 * This because page titles in Confluence are not case sensitive.
 */
public class WikiPages {
	
	private Map<String, WikiPage> pages = new HashMap<String, WikiPage>();
	
	public void put(String key, WikiPage page) {
		pages.put(key.toLowerCase(), page);
	}
	
	public Set<String> keySet() {
		return pages.keySet();
	}
	
	public WikiPage get(String key) {
		return pages.get(key.toLowerCase());
	}
	
	public Collection<WikiPage> values() {
		return pages.values();
	}

}
