package de.unifrankfurt.informatik.acoli.fid.spider;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import edu.emory.mathcs.backport.java.util.Collections;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;

public class XMLAttributeEvaluator {
	
	/**
	 * Compute most useful set of XML attributes to be used for annotation extraction
	 * @param availableAttributes Set of all Xpath for XML attributes
	 * @param allowedAttributes Set of 'good' XML attributes
	 * @param foundTagsOrClassesForAttribute
	 * @param processDuplicates
	 * @return
	 */
	public static HashSet<String> computeCompleteXMLAttributSet(HashSet<String> availableAttributes, HashSet<String> allowedAttributes, HashMap<String, HashSet<String>> foundTagsOrClassesForAttribute, boolean processDuplicates) {
		
		HashSet <String> result = new HashSet<String>();
		

		if (foundTagsOrClassesForAttribute != null) {
			
			// I. Filter attributes with very few or simplistic hits
			int tagOrClassCount=0;
			String tagOrClass="";
			for (String attr : foundTagsOrClassesForAttribute.keySet()) {
				
				//Utils.debug("check : "+attr);
				
				int diffHits = 0;
				//diffHits = foundTagsOrClassesForAttribute.get(attr).size();
				
				int maxLength=0;
				int maxCountInSample=0;
				//Utils.debug("attritubte :"+attr);
				for (String yy : foundTagsOrClassesForAttribute.get(attr)) {
					tagOrClass=yy.split("@")[0];
					tagOrClassCount = Integer.parseInt(yy.split("@")[1]);
					
					// do not count tags that have length 1 and are not alphabetic
					if (!(tagOrClass.length() == 1 && !StringUtils.isAlpha(tagOrClass))) diffHits++;
					
					//Utils.debug(tagOrClass);
					if (!tagOrClass.startsWith("http") && !tagOrClass.startsWith("file:") && !tagOrClass.startsWith("ftp:")) {
						Utils.debug(attr+":"+tagOrClass);
						if (tagOrClass.length() > maxLength) {
							//Utils.debug("length of "+tagOrClass+" is "+tagOrClass.length()+" is max !");
							maxLength = tagOrClass.length();
						}
					}
					if(tagOrClassCount > maxCountInSample) {maxCountInSample = tagOrClassCount;}
				}
				
				Utils.debug(attr+" different hits :"+diffHits);
				Utils.debug(attr+" max tag length :"+maxLength);
				
				
				
				if (maxLength == 0 && diffHits <= 2 ||	// not more than two different URLs
					maxLength == 1 						// all found tags have length 1
					// maxCountInSample == 1			// not a indicator because count is limited to sample only
					) {					
					
					Utils.debug("The trivial XML attribute : "+attr+ " was discarded");
					Utils.debug("because only "+diffHits+" different types of values could be matched");
					if (maxLength > 0)
						Utils.debug("and the maximal tag length was "+maxLength);
					Utils.debug("and the maximal number of found occurrences was "+maxCountInSample);
	
					allowedAttributes.remove(attr);
				}
			}
		}
		
		
		// check duplicates
		HashMap<String, HashSet<String>> duplicates = getDuplicateAttributes(allowedAttributes);
		
		// II. Select elements that cover duplicate attributes
		// (attributes in different elements with the same name)
		if (!duplicates.isEmpty() && processDuplicates) {
			
			// TODO In case of a tie in computeCoverageForDuplicateAttributes 
			// (e.g. /a/b/c/@name and /x/y/z/@name are the only allowed attributes for /a/b/c and /x/y/z)
			// => keep both attributes and resolve tie later in getAllXMLElementAttributes() with 
			// detailed comparison of the hit quality for both attributes 
			HashSet<String> selectedElements = computeCoverageForDuplicateAttributes(duplicates);
			
			// remove duplicate XML attributes that do not belong to the selectedElements (coverage)
			filterAllowedAttributes(allowedAttributes, selectedElements);
		}
		
		
		// III. Expand allowedAttributes to all available attributes in allowedAttributes elements
		result = getAllXMLElementAttributes(availableAttributes, allowedAttributes, foundTagsOrClassesForAttribute);
		
		// check final result
		;
		for (String y : result) {
			Utils.debug("attribute in result :"+y);
		}
		
		return result;
	}
	
	
	private static void filterAllowedAttributes( HashSet<String> allowedAttributes, HashSet<String> allowedElements) {

		Iterator <String> it = allowedAttributes.iterator();
		while (it.hasNext()) {
			String y = it.next();
			
			// Remove allowedAttribute if its parent element is not allowed
			if (!allowedElements.contains(getXMLParentElement(y))) {
				//Utils.debug("filter "+y);
				it.remove();
			}
		}
	}
	


	/**
	 * Given a set of XML attributes. Compute a new set that additionally contains all sister XML attributes for the given attribute set
	 * @param availableAttributeXpath All available XML attribute XPATH
	 * @param allowedAttributes A input set of XML attribute XPATH
	 * @param foundTagsOrClassesForAttribute 
	 * @return The expanded set of XML attribute XPATH
	 */
	static public HashSet<String> getAllXMLElementAttributes (HashSet<String> availableAttributeXpath, HashSet<String> allowedAttributes, HashMap<String, HashSet<String>> foundTagsOrClassesForAttribute)  {

		HashSet<String> result = new HashSet<String>();
		
		// Only allow attributes from one Element (filter)
		//      -> select the element with the most successful attributes
		// in the example the element xyz/chunk will contain one successful attribute : type
		// and xyz/w will contain two will contain two successful attributes : tree, pos
		// therefore all attributes from xyz/w should be selected !
		// Example	
		/*<s id="s2.1">
		 <chunk type="NP" id="c2.1-1">
		  <w tree="NN" lem="part" pos="NNP" id="w2.1.1">PART</w>
		  <w tree="NP" lem="III" pos="NNP" id="w2.1.2">III</w>
		 </chunk>
		</s>*/
		
		// Compute the parent element(s) with the largest number of allowed attributes
		HashMap<String, Integer> parentElement2AttributeCount = new HashMap<String,Integer>();
		for (String attr : allowedAttributes) {
			String attrParent = getXMLParentElement(attr);
			if (!parentElement2AttributeCount.containsKey(attrParent)) {
				parentElement2AttributeCount.put(attrParent, 1);
			} else {
				parentElement2AttributeCount.put(attrParent, parentElement2AttributeCount.get(attrParent)+1);
			}
		}
		
		/*for (String yy : parentElement2AttributeCount.keySet()){
			Utils.debug("pe2atc "+yy +" : "+parentElement2AttributeCount.get(yy));
		}*/
		
		
		// Sort elements in descending order by allowed attribute count
	    List<Entry<String,Integer>> sortedParentElement2AttributeCount = new ArrayList<Entry<String,Integer>>(parentElement2AttributeCount.entrySet());

		Collections.sort(sortedParentElement2AttributeCount, 
	            new Comparator<Entry<String,Integer>>() {
	                @Override
	                public int compare(Entry<String,Integer> e1, Entry<String,Integer> e2) {
	                    return e2.getValue().compareTo(e1.getValue());
	                }
	            }
	    );
		
		// Determine largest element(s)
		HashSet<String> maxElements = new HashSet<String>();
		int maxAllowedAttrCount = 0;
		for (Entry <String,Integer> yy : sortedParentElement2AttributeCount) {
			//Utils.debug(yy.getKey()+" attribute count "+yy.getValue());
			if ((Integer) yy.getValue() >= maxAllowedAttrCount) {
				maxAllowedAttrCount = (Integer) yy.getValue();
				maxElements.add((String) yy.getKey());
			}
		}
		
		// In case of several elements the same maximal allowed attribute count
		// => select that element that has the most different hit types and
		//    if there's another tie compare the total sum of hits
		int maxDiffHits=0;
		int maxHitsCount=0;
		String maxParentElement="";
		if (maxElements.size() == 1) {
			maxParentElement = maxElements.iterator().next();
		} else {
			for (String yy : maxElements) {	
				Utils.debug("max element :"+yy);
				int diffHits = 0;
				int hitsCount = 0;

				// sum different hits for attributes from element
				for (String zz : foundTagsOrClassesForAttribute.keySet()) {
					if (getXMLParentElement(zz).equals(yy)) {
						// NEIN diffHits += foundTagsOrClassesForAttribute.get(zz).size();
						for (String qq : foundTagsOrClassesForAttribute.get(zz)) {
							// do not count hits with length 1 that are not alphabetic
							if (!(qq.split("@")[0].length() == 1 && !StringUtils.isAlpha(qq.split("@")[0]))) {
								diffHits++;
								hitsCount += Integer.parseInt(qq.split("@")[1]);
							}
						}
					}
				}
				
				if (diffHits > maxDiffHits) {
					maxDiffHits = diffHits;
					maxHitsCount = hitsCount;
					maxParentElement = yy;
					Utils.debug("Selected "+yy+" as maxElement because of diffHits "+diffHits);
				}
				if (diffHits == maxDiffHits && hitsCount > maxHitsCount) {
					maxDiffHits = diffHits;
					maxHitsCount = hitsCount;
					maxParentElement = yy;
					Utils.debug("Selected "+yy+" as maxElement because of hitsCount "+hitsCount);
				}
			}
			Utils.debug("Max element   : "+maxParentElement);
			Utils.debug("has diffHits  : "+maxDiffHits);
			Utils.debug("has hitsCount : "+maxHitsCount);
		}
		
		
		/* old version selects maxParentElement only by maxAttrCount. In case of a tie the maxParentElement
		 * is selected randomly
		int maxAttrCount = 0;
		String maxParentElement="";
		for (String e : parentElement2AttributeCount.keySet()) {
			if (parentElement2AttributeCount.get(e) > maxAttrCount) {
				maxAttrCount = parentElement2AttributeCount.get(e);
				maxParentElement = e;
			}
		}*/
		
		// Expand computed best element (include all attributes)
		result.addAll(getXMLElementAttributes(availableAttributeXpath, maxParentElement));

		// old version
		/*for (String attr : allowedAttributes) {
			result.addAll(getXMLElementAttributes(availableAttributeXpath, getXMLParentElement(attr)));
		}*/
		
		return result;
	}
	
	
	/**
	 * Return for a given XML element Xpath all available XML attributes 
	 * @param xmlElementXpath Xpath for XML element
	 * @return The set of XPATH for all available attributes in the XML element
	 */
	static public HashSet<String> getXMLElementAttributes (HashSet<String> availableAttributeXpath, String xmlElementXpath) {
		
		HashSet<String> result = new HashSet<String>();
		
		String tmp = "";
		for (String x : availableAttributeXpath) {
			tmp=getXMLParentElement(x);
			if (tmp == null) tmp="/";
			if (tmp.equals(xmlElementXpath)) result.add(x);
		}
		
		/*for (String y : result){
			Utils.debug(y);
		}*/
		
		return result;
	}
	
	
	/**
	 * Compute all parent XML elements from a set of XPATH for XML attributes
	 * @param attributeXpath
	 * @return Set of XML elements
	 */
	static public HashSet<String> getXMLParentElements(HashSet<String> attributeXpath) {
		
		HashSet<String> results = new HashSet<String>();
		for (String x : attributeXpath) {
			File f = new File (x);
			results.add(f.getParent());
		}
		return results;
	}
	
	
	/**
	 * Compute parent XML element for given XML attribute XPATH
	 * @param attributeXpath
	 * @return XML parent element
	 */
	static public String getXMLParentElement(String attributeXpath) {
		
		File f = new File (attributeXpath);
		return f.getParent();
	}
	
	
	/**
	 * Get XML attribute name from XPATH
	 * @param attributeXpath
	 * @return XML attribute name
	 * TODO Simplified version only returns last element in path
	 */
	static public String getXMLAttributeNameFromXpath(String attributeXpath) {
		
		File f = new File (attributeXpath);
		return f.getName();
	}
	
	
	
	/**
	 * Compute a map which contains XML attribute names that occur several times
	 * @param xmlAttributeXpath Set of XML attribute XPATH
	 * @return Map from XML attribute names to XPATH which use a attribute name
	 */
	public static HashMap<String,HashSet<String>> getDuplicateAttributes(HashSet<String> xmlAttributeXpath) {
		
		HashMap <String,HashSet<String>> attributes2Xpath = new HashMap <String,HashSet<String>>();
		HashMap <String,HashSet<String>> result = new HashMap <String,HashSet<String>>();
		
		// Get attribute names
		String key="";
		for (String x : xmlAttributeXpath) {
			
			key = getXMLAttributeNameFromXpath(x);
			
			if (!attributes2Xpath.containsKey(key)) {
				HashSet<String> y =  new HashSet<String>();
				y.add(x);
				attributes2Xpath.put(key,y);
			} else {
				HashSet<String> y = attributes2Xpath.get(key);
				y.add(x);
				attributes2Xpath.put(key,y);
			}				
		}
		
		// Remove attributes that only occur once (not duplicates)
		for (String x : attributes2Xpath.keySet()) {
			if (attributes2Xpath.get(x).size() > 1) {
				result.put(x, attributes2Xpath.get(x));
				//Utils.debug("duplicate :"+x+" "+attributes2Xpath.get(x));
			}
		}
		
		
		return result;
	}
	
	
	/**
	 * The set of XML elements that provide the best coverage for the given duplicate XML attributes.
	 * Implementations chooses the smallest set of elements for coverage 
	 * @param duplicateMap
	 * @return Set of elements that provide best coverage for duplicate attributes
	 */
	public static HashSet<String> computeCoverageForDuplicateAttributes(HashMap<String,HashSet<String>> duplicateMap) {
		
		HashSet<String> selectedElements = new HashSet<String>();
		HashSet<String> remainingAttributes = new HashSet<String>(duplicateMap.keySet());
		HashMap<String, Integer> element2Size = new HashMap<String ,Integer>();
		HashMap<String, HashSet<String>> element2Attributes = new HashMap<String ,HashSet<String>>();

		
		String el="";
		// Compute maps
		for (String key : duplicateMap.keySet()) {
			for (String attr : duplicateMap.get(key)) {
				
				el= getXMLParentElement(attr);
				
				if (!element2Size.containsKey(el)) {
					element2Size.put(el, 1);
					
					HashSet<String> attributes = new HashSet<String>();
					attributes.add(key);
					element2Attributes.put(el, attributes);
				} else {
					element2Size.put(el, element2Size.get(el)+1);
					
					HashSet<String> attributes = element2Attributes.get(el);
					attributes.add(key);
					element2Attributes.put(el, attributes);
				}
			}
		}
				
		// Sort duplicates by element and element size
	    List<Entry<String,Integer>> sortedElement2Size = new ArrayList<Entry<String,Integer>>(element2Size.entrySet());

		Collections.sort(sortedElement2Size, 
	            new Comparator<Entry<String,Integer>>() {
	                @Override
	                public int compare(Entry<String,Integer> e1, Entry<String,Integer> e2) {
	                    return e2.getValue().compareTo(e1.getValue());
	                }
	            }
	    );
		

		
		// Compute coverage for duplicate attributes
		// for each duplicate attribute
		for (String duplicateAttr : duplicateMap.keySet()) {
			
			//Utils.debug("duplicate attr :"+duplicateAttr);
			
			if (!remainingAttributes.contains(duplicateAttr)) continue;
			
			//Utils.debug("searching");
			
			// Search the 'largest' element that contains the duplicate attribute
			Iterator <Entry<String,Integer>> it = sortedElement2Size.iterator();
			while (it.hasNext()) {
				Entry<String,Integer> e = it.next();
				
				//Utils.debug(e.getKey()+" : "+e.getValue());
				
				/*for (String yy : element2Attributes.get(e.getKey())) {
					Utils.debug("test "+yy+ " : "+duplicateAttr);
				}*/
				
				// DuplicateAttr is contained in element !
				if (element2Attributes.get(e.getKey()).contains(duplicateAttr)) {
					
					//Utils.debug("found");
					
					// 1. select element for result
					selectedElements.add(e.getKey());
					
					// 2. remove all attributes covered by this element from duplicate attr list
					for (String elAttr : element2Attributes.get(e.getKey())) {
						remainingAttributes.remove(elAttr);
						//Utils.debug("remove "+elAttr);
					}
					//Utils.debug("remaining "+remainingAttributes.size());

					
					// 3. remove the element from the element list
					it.remove();
					
					// continue with next duplicate attribute
					break;
				}
			}
		}
		
		
		
		return selectedElements;
	}
	
	

}
