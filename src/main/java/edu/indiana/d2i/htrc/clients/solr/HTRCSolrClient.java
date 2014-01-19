package edu.indiana.d2i.htrc.clients.solr;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class HTRCSolrClient {
	private final String QUERY_PREFIX = "/select/?q=";
	private final String FILTER_PREFIX = "/TermVector/";
	private final String VOLUME_TAG = "id";
	private final String VOLUME_OCR = "ocr";
	
	private String solrEPR = "http://coffeetree.cs.indiana.edu:9994/solr";
	private XMLInputFactory factory = XMLInputFactory.newInstance();
	
	private String encodeQueryStr(String query) throws UnsupportedEncodingException {
		// extract parts need to be encoded
		String[] splits = query.split("&");
		
		StringBuilder res = new StringBuilder();
		String split = null;
		for (int i = 0; i < splits.length-1; i++) {
			split  = splits[i];
//			if (split.equals("qt=sharding")) {
			if (split.contains("=")) {
				res.append(split + "&");
			} else {
				res.append(URLEncoder.encode(split, "UTF-8") + "&");
			}
		}
		split = splits[splits.length-1];
		if (split.equals("qt=sharding")) {
			res.append(split);
		} else {
			res.append(URLEncoder.encode(split, "UTF-8"));
		}
		
		return res.toString();
	}
	
	private int getNumberOfResult(String query) throws IOException, XMLStreamException {
		int resultNum = 0;
		String encodedQueryStr = encodeQueryStr(query);
		
		String queryurl = null;
		if (!encodedQueryStr.contains("rows="))
			queryurl = solrEPR + QUERY_PREFIX + encodedQueryStr + "&rows=0";	
		else 
			queryurl = solrEPR + QUERY_PREFIX + encodedQueryStr;
		
		URL url = new URL(queryurl);
		XMLStreamReader parser = factory.createXMLStreamReader(url.openStream());
		while (parser.hasNext()) {
			int event = parser.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String attributeValue = parser.getAttributeValue(null,
						"numFound");
				if (attributeValue != null) resultNum = Integer.valueOf(attributeValue); 
			}
		}
		
		return resultNum;
	}
	
	private List<String> parseVolumeID(InputStream in) throws XMLStreamException {
		List<String> volumes = new ArrayList<String>();
		
		XMLStreamReader parser = factory.createXMLStreamReader(in);
		while (parser.hasNext()) {
			int event = parser.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String attributeValue = parser.getAttributeValue(null,
						"name");
				if (attributeValue != null) {
					if (attributeValue.equals(VOLUME_TAG)) {
						volumes.add(parser.getElementText());
					}
				}
			}
		}
		return volumes;
	}
	
	
	private void accumulateWords(Map<String, Integer> wordCount, XMLStreamReader parser) throws XMLStreamException {
		int tagCount = 1;
		
		while (parser.hasNext()) {			
			int event = parser.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				tagCount++;
				String attributeValue = parser.getAttributeValue(null, "name");				
				if (attributeValue != null) {
					if (!attributeValue.contains("_")) {
						tagCount = (parser.nextTag() == XMLStreamConstants.START_ELEMENT) ? tagCount+1: tagCount-1;
						String str = parser.getElementText();
						int count = Integer.valueOf(str);
						tagCount = (parser.nextTag() == XMLStreamConstants.START_ELEMENT) ? tagCount+1: tagCount-2;
						if (wordCount.containsKey(attributeValue)) {
							wordCount.put(attributeValue, wordCount.get(attributeValue) + count);
						} else {
							wordCount.put(attributeValue, count);
						}
					} else {
						tagCount = (parser.nextTag() == XMLStreamConstants.START_ELEMENT) ? tagCount+1: tagCount-1;
						parser.getElementText();
						tagCount = (parser.nextTag() == XMLStreamConstants.START_ELEMENT) ? tagCount+1: tagCount-2;
					}
				}
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				tagCount--;
				if (tagCount == 0) break;
			}
		}
	}
	
	private Map<String, Integer> parseWordFrequency(InputStream in) throws XMLStreamException {
		Map<String, Integer> wordCount = new HashMap<String, Integer>();
		
		XMLStreamReader parser = factory.createXMLStreamReader(in);
		while (parser.hasNext()) {			
			int event = parser.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String attributeValue = parser.getAttributeValue(null,
						"name");
				if (attributeValue != null) {
					if (attributeValue.equals(VOLUME_OCR)) {
						accumulateWords(wordCount, parser);
					}
				}
			}
		}
		return wordCount;
	}
	
	private void parseWordFrequency(Map<String, Integer> wordCount, InputStream in) throws XMLStreamException {		
		XMLStreamReader parser = factory.createXMLStreamReader(in);
		while (parser.hasNext()) {			
			int event = parser.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String attributeValue = parser.getAttributeValue(null,
						"name");
				if (attributeValue != null) {
					if (attributeValue.equals(VOLUME_OCR)) {
						accumulateWords(wordCount, parser);
					}
				}
			}
		}
	}
	
    private static class ValueComparator implements Comparator<String> {
    	private final Map<String, Integer> data;
    	private final boolean natural;
    	public ValueComparator(Map<String, Integer> data, boolean natural) {
    		this.data = data;
    		this.natural = natural;
    	}
		public int compare(String o1, String o2) {
			int result = (natural) ? data.get(o1).compareTo(data.get(o2)): 
				data.get(o2).compareTo(data.get(o1));
			result = (result == 0) ? o1.compareTo(o2): result;			
			return result;
		}    	
    }
	
	public HTRCSolrClient(String solrEPR) {
		if (solrEPR.lastIndexOf("/") == solrEPR.length() - 1)
			this.solrEPR = solrEPR.substring(0, solrEPR.length() - 1);
		else this.solrEPR = solrEPR;
	}
	
	/**  
	 * For example,
	 * author:Raymond AND publishDate:1884
	 */
	public List<String> getVolumeIDs(String queryurl) throws IOException, XMLStreamException {
		int resultNum = getNumberOfResult(queryurl);
		String encodedQueryStr = encodeQueryStr(queryurl);
		String path = null;
		if (!encodedQueryStr.contains("rows")) 
			path = solrEPR + QUERY_PREFIX + encodedQueryStr + "&fl=id&rows=" + resultNum;
		else 
			path = solrEPR + QUERY_PREFIX + encodedQueryStr + "&fl=id";
//		String path = solrEPR + QUERY_PREFIX + URLEncoder.encode(queryurl, "UTF-8") + "&fl=id&rows=" + resultNum;
		URL url = new URL(path);
		return parseVolumeID(url.openStream());
	}
	
	/**  
	 * For example,
	 * author:Raymond AND publishDate:1884
	 */
	public Map<String, Integer> getWordCount(String queryurl) throws IOException, XMLStreamException {
		int resultNum = getNumberOfResult(queryurl);
		String path = solrEPR + QUERY_PREFIX + URLEncoder.encode(queryurl, "UTF-8") + "&fl=id&tv=on&tv.tf=true&qt=tvrh&start=0&rows=" + resultNum;
//		String path = solrEPR + QUERY_PREFIX + URLEncoder.encode(queryurl, "UTF-8") + "&fl=id&tv=on&tv.tf=true&qt=tvrh&start=0&rows=" + 2;
//		path = "http://coffeetree.cs.indiana.edu:9994/solr/TermVector/miua.2916929,0001,001/?prefix=wi&ngram=false&offset=false";
		
		URL url = new URL(path);
		return parseWordFrequency(url.openStream());
	}
	
	/**  
	 * For example,
	 * author:Raymond AND publishDate:1884
	 */
	public Map<String, Integer> getWordCountInCountOrder(String queryurl, boolean naturalOrder) throws IOException, XMLStreamException {
		int resultNum = getNumberOfResult(queryurl);
		String path = solrEPR + QUERY_PREFIX + URLEncoder.encode(queryurl, "UTF-8") + "&fl=id&tv=on&tv.tf=true&qt=tvrh&start=0&rows=" + resultNum;
//		String path = solrEPR + QUERY_PREFIX + URLEncoder.encode(queryurl, "UTF-8") + "&fl=id&tv=on&tv.tf=true&qt=tvrh&start=0&rows=" + 2;
		URL url = new URL(path);
		
		Map<String, Integer> wordcount = parseWordFrequency(url.openStream());
		SortedMap<String, Integer> sortedData = new TreeMap<String, Integer>(new ValueComparator(wordcount, naturalOrder));
		sortedData.putAll(wordcount);
		return sortedData; 
	}
	
	/**  
	 * For example,
	 * author:Raymond AND publishDate:1884
	 */
	public Map<String, Integer> getWordCountInCountOrderFromSolrFilter(List<String> volumeID, boolean naturalOrder) throws IOException, XMLStreamException {
		Map<String, Integer> wordcount = new HashMap<String, Integer>();
		for (String id : volumeID) {
			String path = solrEPR + FILTER_PREFIX + URLEncoder.encode(id, "UTF-8") + "/?prefix=*&ngram=false&offset=false";
			URL url = new URL(path);
			parseWordFrequency(wordcount, url.openStream());
		}
		SortedMap<String, Integer> sortedData = new TreeMap<String, Integer>(new ValueComparator(wordcount, naturalOrder));
		sortedData.putAll(wordcount);
		return sortedData; 
	}
}
