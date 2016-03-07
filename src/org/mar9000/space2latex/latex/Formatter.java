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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.mar9000.space2latex.WikiImage;
import org.mar9000.space2latex.WikiPage;
import org.mar9000.space2latex.WikiPages;
import org.mar9000.space2latex.log.S2LLogUtils;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.css.RGBColor;

import com.steadystate.css.parser.CSSOMParser;

public class Formatter {

	private static Logger LOGGER = S2LLogUtils.getLogger(Formatter.class.getName());
	
	private final static String SPACE_PREFIX = "MPSD";
	private File downloadDir = null;
	private LatexDocument latexDocument = null;
	private WikiPages pages = null;
	private Map<String, Label> labels = new HashMap<String, Label>();

	public Formatter(File downloadDir, WikiPages pages, LatexDocument latexDocument) {
		this.downloadDir = downloadDir;
		this.latexDocument = latexDocument;
		this.pages = pages;
	}

	private Stack<WikiPage> pagesStack = new Stack<WikiPage>();
	private boolean createMissingChapters = true;
	public void format(boolean createMissingChapters) {
		this.createMissingChapters = createMissingChapters;
		for (WikiPage page : pages.values()) {
			if (page.isExcluded) {
				System.out.println("Page excluded as requested: " + page.title);
				continue;
			}
			pagesStack.push(page);
			Chapter chapter = latexDocument.getChapter(pagesStack.peek().title, createMissingChapters);
			if (chapter == null)
				continue;
			System.out.println("Format page: " + page.title);
			if (page.alreadyIncluded) {
				System.err.println("Page already included: " + page.title);
			}
			page.alreadyIncluded = true;
			formatNodes(page.pageContent.childNodes(), chapter.elements);
			pagesStack.pop();
		}
	}

	private boolean insideTable = false;
	private boolean insideList = false;
	public void formatNodes(List<Node> nodes, List<LatexElement> result) {
		for (Node node : nodes) {
			if (node instanceof TextNode) {
				String nodeText = ((TextNode)node).text();
				result.add(new TextElement(nodeText));
			} else if (node instanceof Element) {
				Element element = (Element)node;
				if (element.nodeName().equals("p")) {
					Paragraph p = new Paragraph();
					result.add(p);
					formatNodes(element.childNodes(), p.elements);
				} else if (element.nodeName().equals("h1")) {
					LatexElements le = null;
					if (insideTable) {
						Hx hx = new Hx();
						hx.fontSize = "Huge";
						le = hx;
					} else {
						System.err.println("Warning H1 elements typeset sections like H2 elements.");
						Section section = new Section(Section.TYPE_SECTION);
						le = section;
					}
					result.add(le);
					formatNodes(element.childNodes(), le.elements);
				} else if (element.nodeName().equals("h2")) {
					LatexElements le = null;
					if (insideTable) {
						Hx hx = new Hx();
						hx.fontSize = "huge";
						le = hx;
					} else {
						// TODO: workaround because there is an image inside an H2 element.
						if (element.childNodes().get(0).nodeName().equals("ac:image")) {
							System.err.println("Using workaround for image inside H2.");
							// Do not create H2, create element inside current result.
							formatNodes(element.childNodes(), result);
							continue;
						}
						Section section = new Section(Section.TYPE_SECTION);
						le = section;
					}
					result.add(le);
					formatNodes(element.childNodes(), le.elements);
				} else if (element.nodeName().equals("h3")) {
					LatexElements le = null;
					if (insideTable) {
						Hx hx = new Hx();
						hx.fontSize = "LARGE";
						le = hx;
					} else {
						Section section = new Section(Section.TYPE_SUBSECTION);
						le = section;
					}
					result.add(le);
					formatNodes(element.childNodes(), le.elements);
				} else if (element.nodeName().equals("h4")) {
					LatexElements le = null;
					if (insideTable) {
						Hx hx = new Hx();
						hx.fontSize = "Large";
						le = hx;
					} else {
						Section section = new Section(Section.TYPE_SUBSUBSECTION);
						le = section;
					}
					result.add(le);
					formatNodes(element.childNodes(), le.elements);
				} else if (element.nodeName().equals("h5")) {
					LatexElements le = null;
					if (insideTable) {
						Hx hx = new Hx();
						hx.fontSize = "large";
						le = hx;
					} else {
						Section section = new Section(Section.TYPE_PARAGRAPH);
						le = section;
					}
					result.add(le);
					formatNodes(element.childNodes(), le.elements);
				} else if (element.nodeName().equals("h6")) {
					Bold b = new Bold();
					result.add(b);
					formatNodes(element.childNodes(), b.elements);
				} else if (element.nodeName().equals("em")) {
					Emph e = new Emph();
					result.add(e);
					formatNodes(element.childNodes(), e.elements);
				} else if (element.nodeName().equals("code")) {
					TextTT tt = new TextTT();
					result.add(tt);
					formatNodes(element.childNodes(), tt.elements);
				} else if (element.nodeName().equals("s")) {
					St st = new St();
					result.add(st);
					formatNodes(element.childNodes(), st.elements);
				} else if (element.nodeName().equals("strong")) {
					Bold b = new Bold();
					result.add(b);
					formatNodes(element.childNodes(), b.elements);
				} else if (element.nodeName().equals("a")) {
					String href = node.attr("href");
					Href h = new Href();
					h.setUrl(href);
					result.add(h);
					formatNodes(element.childNodes(), h.elements);
				} else if (element.nodeName().equals("ol")) {
					if (insideTable) {
						if (insideList)
							throw new IllegalArgumentException("It seems there is a list inside a list inside a table. Not supported.");
						insideList = true;
						formatNodes(element.childNodes(), result);
						insideList = false;
						continue;
					}
					Ol o = new Ol();
					result.add(o);
					formatNodes(element.childNodes(), o.elements);
				} else if (element.nodeName().equals("ul")) {
					if (insideTable) {
						if (insideList)
							throw new IllegalArgumentException("It seems there is a list inside a list inside a table. Not supported.");
						insideList = true;
						formatNodes(element.childNodes(), result);
						insideList = false;
						continue;
					}
					Ul l = new Ul();
					result.add(l);
					formatNodes(element.childNodes(), l.elements);
				} else if (node.nodeName().equals("li")) {
					Li i = new Li();
					if (insideTable)
						i.useTabItem = true;
					result.add(i);
					formatNodes(node.childNodes(), i.elements);
				} else if (element.nodeName().equals("div")) {
					// Divs are typeset as in-line content, as the div did not exist.
					// TODO: once latex elements are subdivided in horizontal and vertical, this can
					// probably be improved by closing the current vertical content and opening a new vertical one.
					formatNodes(element.childNodes(), result);
				} else if (node.nodeName().equals("br")) {
					result.add(new Newline());
				} else if ((node.nodeName().equals("ac:macro") || element.nodeName().equals("ac:structured-macro"))
						&& node.attr("ac:name").equals("toc")) {
					System.out.println("Page TOC omitted: " + pagesStack.peek().title);
					continue;
				} else if ((element.nodeName().equals("ac:structured-macro") || node.nodeName().equals("ac:macro"))
						&& element.attr("ac:name").equals("anchor")) {
					String anchor = element.text();
					String pageTitle = pagesStack.peek().title;
					String labelString = Label.getLabelString(pageTitle, anchor);
					Label label = labels.get(labelString);
					if (label != null) {
						// Found.
						if (label.defined) {
							// Label created by anchor definition.
							System.err.println("Label already defined: " + labelString);
						} else {
							// Label created by a link definition, define and add at this document point.
							label.defined = true;
							result.add(label);
						}
					} else {
						label = new Label(pageTitle, anchor);
						label.defined = true;
						labels.put(label.getLabelString(), label);
						result.add(label);
					}
				} else if (node.nodeName().equals("table")) {
					if (insideTable == true)
						throw new IllegalArgumentException("It seems there is table inside a table. Not supported.");
					//insideTable = true;   // TODO test with list inside table
					Table table = new Table();
					table.preamble = "|";
					boolean firstRow = true;
					result.add(table);
					Elements trElements = element.select("tr");
					for (Element trElement : trElements) {
						Tr tr = new Tr();
						table.rows.add(tr);
						// Try TH elements.
						String dataTag = "th";
						if (trElement.select(dataTag).size() == 0)
							dataTag = "td";
						//
						Elements tdElements = trElement.select(dataTag);
						for (Element tdElement : tdElements) {
							Td td = new Td();
							tr.columns.add(td);
							if (firstRow)
								table.preamble = table.preamble + "X[-1]|";
							formatNodes(tdElement.childNodes(), td.elements);
						}
						firstRow = false;
					}
					insideTable = false;
				} else if (element.nodeName().equals("ac:link")) {
					// This can result in an internal link when a label exists at rendering time.
					// Otherwise it will be typeset as an external link.
					// Do not include navigation link.
					if (element.text().equals("Previous") || element.text().equals("Next"))
						continue;
					// Get anchor, page and space.
					String anchor = element.attr("ac:anchor");
					String pageTitle = null;
					String space = null;
					Element riPage = element.select("ri|page").first();
					Element linkBody = element.select("ac|link-body").first();
					if (riPage == null) {
						// Only an anchor, the page is the current one.
						pageTitle = pagesStack.peek().title;
					} else {
						pageTitle = riPage.attr("ri:content-title");
						space = riPage.attr("ri:space-key");
						if (space.startsWith(SPACE_PREFIX)) {
							// There are link that wrongly point to space other then MPSD33, usually this is an error.
							System.err.println("Page " + pagesStack.peek().title
									+ " contains link to specific space " + space + ". It will be ignored.");
							space = "";
						}
					}
					// If there is a space we should emit an Href.
					LatexElements latexElement = null;
					if (space != null && space.length() > 0) {
						Href h = new Href();
						h.setUrl(latexDocument.baseUrl + "/" + space + "/" + pageTitle.replace(' ', '+'));
						result.add(h);
						latexElement = h;
					} else {
						// No space, add a Link that will be typeset either as hyperref or as href.
						String labelString = null;
						if (anchor.length() == 0) {   // Link to chapter.
							// Chapter.
							Chapter chapter = latexDocument.getChapter(pageTitle, createMissingChapters);
							if (chapter != null) {
								if (chapter.inline)
									labelString = "inline-chapter-" + chapter.title;
								else
									labelString = "chapter." + chapter.number;
							} else {
								labelString = Label.getLabelString(pageTitle, null);
								// There are links that seem to be errors, for instance link to "search scope" page, see the Constraints page.
								if (pages.get(pageTitle) == null)
									System.err.println("A link will be created to a page that does not exists into the download dir.: "
											+ pageTitle);
							}
						} else {
							labelString = Label.getLabelString(pageTitle, anchor);
							if (pages.get(pageTitle) == null)
								System.err.println("A link/anchor will be created to a page that does not exists into the download dir.: "
										+ pageTitle + "#" + anchor);
						}
						Label l = labels.get(labelString);
						if (l == null) {  // Create a Label with defined = false.
							l = new Label(anchor.length() == 0 ? labelString : pageTitle, anchor);
							labels.put(l.getLabelString(), l);
							// Chapter label are always defined.
							if (labelString.startsWith("chapter.") || labelString.startsWith("inline-chapter-"))
								l.defined = true;
						}
						l.referenced = true;
						Link link = new Link(l);
						result.add(link);
						latexElement = link;
					}
					// Link content.
					if (linkBody != null) {
						formatNodes(linkBody.childNodes(), latexElement.elements);
					} else {
						latexElement.elements.add(new TextElement(pageTitle));
					}
				} else if (node.nodeName().equals("hr")) {
					result.add(new Hrule());
				} else if ((node.nodeName().equals("ac:macro") || node.nodeName().equals("ac:structured-macro"))
						&& node.attr("ac:name").equals("code")) {
					Element body = ((Element) node).select("ac|plain-text-body").first();
					TextNode textNode = (TextNode)body.childNodes().get(0);
					VerbatimDef v = new VerbatimDef();
					v.setContent(textNode.getWholeText());
					latexDocument.verbatimDefs.addVerbatimDef(v);
					VerbatimUse verbatimUse = new VerbatimUse();
					verbatimUse.key = v.key;
					result.add(verbatimUse);
				} else if (node.nodeName().equals("ac:structured-macro") && node.attr("ac:name").equals("info")) {
					Info info = new Info();
					result.add(info);
					formatNodes(node.childNodes(), info.elements);
				} else if (node.nodeName().equals("ac:macro") && node.attr("ac:name").equals("info")) {
					Info info = new Info();
					result.add(info);
					formatNodes(element.childNodes(), info.elements);
				} else if (node.nodeName().equals("ac:rich-text-body")) {
					// Include content.
					formatNodes(element.childNodes(), result);
				} else if ((node.nodeName().equals("ac:structured-macro") || node.nodeName().equals("ac:macro"))
						&& node.attr("ac:name").equals("include")) {
					String includedTitle = null;
					if (node.nodeName().equals("ac:structured-macro")) {
						Element riPage = element.select("ri|page").first();
						if (riPage == null) {
							System.err.println("ac:structured-macro 'include' does not have ri:page." + element.outerHtml());
							continue;
						}
						includedTitle = riPage.attr("ri:content-title");
					} else {   // ac:macro.
						Element defaultParameter = element.select("ac|default-parameter").first();
						if (defaultParameter == null) {
							System.err.println("ac:macro 'include' does not have ac:default-parameter." + element.outerHtml());
							continue;
						}
						includedTitle = defaultParameter.text();
					}
					if (includedTitle == null) {
						System.err.println("ri:page to include does not have content-title." + element.outerHtml());
						continue;
					}
					WikiPage pageToInclude = pages.get(includedTitle);
					if (pageToInclude == null) {
						System.err.println("Page to include not found: " + includedTitle);
						continue;
					}
					if (pageToInclude.isExcluded) {
						System.out.println("Page excluded as requested: " + includedTitle);
						continue;
					}
					if (pageToInclude.alreadyIncluded) {
						System.err.println("About to include a page already included: " + pageToInclude.title);
					}
					pageToInclude.alreadyIncluded = true;
					pagesStack.push(pageToInclude);
					System.out.println("Include page: " + includedTitle);
					formatNodes(pagesStack.peek().pageContent.childNodes(), result);
					pagesStack.pop();
				} else if (node.nodeName().equals("ac:structured-macro") && node.attr("ac:name").equals("section")) {
					// Format only first column.
					Element richtext = ((Element) node).select("ac|rich-text-body").first()
							.select("ac|structured-macro").first().select("ac|rich-text-body").first();
					formatNodes(richtext.childNodes(), result);
				} else if (node.nodeName().equals("ac:image")) {
					// TODO: do not use ac:image string to retrieve images, some image is included with more than one width.
					String acImage = element.outerHtml();
					WikiImage image = pagesStack.peek().images.get(acImage);
					if (image == null) {
						// Some images are included with different width, try to search only ri:attachment.
						String riAttachment = element.select("ri|attachment").first().outerHtml();
						for (String imageKey : pagesStack.peek().images.keySet()) {
							if (imageKey.indexOf(riAttachment) != -1) {
								image = pagesStack.peek().images.get(imageKey);
								break;
							}
						}
						// Still not found?
						if (image == null) {
							//throw new IllegalArgumentException("ac:image not found in page images: " + element.outerHtml());
							LOGGER.log(Level.SEVERE, "ac:image not found in page images: {0}", element.outerHtml());
							continue;
						}
					}
					result.add(new Image(downloadDir.getAbsolutePath() + "/" + image.filename));
				} else if (node.nodeName().equals("span")) {
					List<LatexElement> container = result;
					String style = element.attr("style");
					if (style != null) {
						InputSource source = new InputSource(new StringReader(style));
						CSSOMParser parser = new CSSOMParser();
						try {
							CSSStyleDeclaration sd = parser.parseStyleDeclaration(source);
							for (int p = 0; p < sd.getLength(); p++) {
								String propertyName = sd.item(p);
								CSSValue value = sd.getPropertyCSSValue(propertyName);
								short valueType = value.getCssValueType();
								//boolean isPrimitive = valueType == CSSValue.CSS_PRIMITIVE_VALUE;
								if (propertyName.equals("color")) {
									CSSPrimitiveValue primitive = (CSSPrimitiveValue)value;
									short primitiveType = primitive.getPrimitiveType();
									if (primitiveType == CSSPrimitiveValue.CSS_RGBCOLOR) {
										RGBColor rgbColor = primitive.getRGBColorValue();
										Color c = new Color();
										result.add(c);
										c.r = rgbColor.getRed().toString();
										c.g = rgbColor.getGreen().toString();
										c.b = rgbColor.getBlue().toString();
										container = c.elements;
									} else {
										System.err.println("CSSPrimitiveValue not supported: " + value.toString());
									}
								} else if (propertyName.equals("text-decoration")) {
									CSSPrimitiveValue primitive = (CSSPrimitiveValue)value;
									short primitiveType = primitive.getPrimitiveType();
									if (primitiveType == CSSPrimitiveValue.CSS_IDENT) {
										String decoration = primitive.getStringValue();
										if (decoration.equals("line-through")) {
											St st = new St();
											result.add(st);
											container = st.elements;
										} else if (decoration.equals("underline")) {
											TextUnderline underline = new TextUnderline();
											result.add(underline);
											container = underline.elements;
										} else {
											System.err.println("Text decoration not supported: " + decoration);
										}
									} else {
										System.err.println("CSSPrimitiveValue not supported: " + value.toString());
									}
								} else {
									System.err.println("CSS property not supported: " + propertyName);
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							System.err.println("Error parsing CSS style: " + style);
						}
					}
					// Format content in line.
					formatNodes(element.childNodes(), container);
				} else if (node.nodeName().equals("blockquote")) {
					Blockquote b = new Blockquote();
					result.add(b);
					formatNodes(element.childNodes(), b.elements);
				} else if ((node.nodeName().equals("ac:macro") || node.nodeName().equals("ac:structured-macro"))
						&& node.attr("ac:name").equals("noformat")) {
					Element plainText = element.select("ac|plain-text-body").first();
					TextNode textNode = (TextNode)plainText.childNodes().get(0);
					VerbatimDef v = new VerbatimDef();
					v.setContent(textNode.getWholeText());
					latexDocument.verbatimDefs.addVerbatimDef(v);
					VerbatimUse verbatimUse = new VerbatimUse();
					verbatimUse.key = v.key;
					result.add(verbatimUse);
				} else if ((node.nodeName().equals("ac:macro") || node.nodeName().equals("ac:structured-macro"))
						&& node.attr("ac:name").equals("tip")) {
					Tip tip = new Tip();
					result.add(tip);
					Element richText = element.select("ac|rich-text-body").first();
					formatNodes(richText.childNodes(), tip.elements);
				} else if ((element.nodeName().equals("ac:structured-macro") || element.nodeName().equals("ac:macro"))
						&& node.attr("ac:name").equals("warning")) {
					Warning warn = new Warning();
					result.add(warn);
					Element richText = element.select("ac|rich-text-body").first();
					List<Node> warnNodes = null;
					if (richText != null) {
						warnNodes = richText.childNodes();
					} else {
						warnNodes = element.childNodes();
					}
					formatNodes(warnNodes, warn.elements);
				} else if (element.nodeName().equals("ac:parameter") && node.attr("ac:name").equals("title")) {
					Hx h5 = new Hx();
					h5.fontSize = "large";
					result.add(h5);
					formatNodes(element.childNodes(), h5.elements);
					result.add(new Newline());
				} else if (element.nodeName().equals("sub")) {
					Sub sub = new Sub();
					result.add(sub);
					formatNodes(element.childNodes(), sub.elements);
				} else if (element.nodeName().equals("sup")) {
					Sup sup = new Sup();
					result.add(sup);
					formatNodes(element.childNodes(), sup.elements);
				} else if (element.nodeName().equals("pre")) {
					StringBuffer buffer = new StringBuffer();
					formatPre(element.childNodes(), buffer);
					VerbatimDef v = new VerbatimDef();
					v.setContent(buffer.toString());
					latexDocument.verbatimDefs.addVerbatimDef(v);
					VerbatimUse verbatimUse = new VerbatimUse();
					verbatimUse.key = v.key;
					result.add(verbatimUse);
				} else if (element.nodeName().equals("ac:emoticon")) {
					String name = element.attr("ac:name");
					if (name.equals("light-on")) {
						result.add(new Emoticon(Emoticon.LIGHT_ON));
					} else if (name.equals("warning")) {
						result.add(new Emoticon(Emoticon.WARNING));
					} else if (name.equals("information")) {
						result.add(new Emoticon(Emoticon.INFORMATION));
					} else if (name.equals("tick")) {
						result.add(new Emoticon(Emoticon.TIP));
					} else if (name.equals("smile")) {
						result.add(new Emoticon(Emoticon.SMILE));
					} else {
						System.err.println("Emoticon not supported: " + name);
					}
				} else if ((element.nodeName().equals("ac:macro") || element.nodeName().equals("ac:structured-macro"))
						&& node.attr("ac:name").equals("note")) {
					Info info = new Info();
					result.add(info);
					formatNodes(node.childNodes(), info.elements);
				} else if (element.nodeName().equals("ac:macro") && node.attr("ac:name").equals("toc-zone")) {
					Element richTextBody = element.select("ac|rich-text-body").first();
					if (richTextBody == null) {
						System.err.println("Macro 'toc-zone' without 'rich-text-body' element.");
						continue;
					}
					// Format in line.
					formatNodes(richTextBody.childNodes(), result);
				} else if (node.nodeName().equals("ac:macro") && node.attr("ac:name").equals("unmigrated-wiki-markup")) {
					System.err.println("Old wiki markup, content will be ignored: " + pagesStack.peek().title);
				} else if (element.nodeName().equals("ac:default-parameter")) {
					// I think this should be ignored, do nothing.
					continue;
				} else {
					throw new IllegalArgumentException("Element " + element.nodeName() + " not supported.");
					//System.err.println("Element " + element.nodeName() + " not supported.");
				}
			} else {
				throw new IllegalArgumentException("Node " + node.nodeName() + " not supported.");
			}
		}
	}
	
	private void formatPre(List<Node> childNodes, StringBuffer buffer) {
		for (Node node : childNodes) {
			if (node instanceof TextNode) {
				buffer.append(((TextNode)node).getWholeText());
			} else if (node instanceof Element) {
				Element element = (Element)node;
				if (element.nodeName().equals("br"))
					buffer.append("\n");
				else if (element.nodeName().equals("span")) {
					formatPre(element.childNodes(), buffer);
				} else if (element.nodeName().equals("code")) {
					formatPre(element.childNodes(), buffer);
				} else {
					throw new IllegalArgumentException("Element " + element.nodeName() + " not supported in 'pre' elements.");
				}
			} else {
				throw new IllegalArgumentException("Node " + node.nodeName() + " not supported in 'pre' elements.");
			}
		}
	}

}
