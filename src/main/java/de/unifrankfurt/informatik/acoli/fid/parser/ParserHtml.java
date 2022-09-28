package de.unifrankfurt.informatik.acoli.fid.parser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.unifrankfurt.informatik.acoli.fid.detector.HtmlEvaluator;
import edu.emory.mathcs.backport.java.util.Collections;
import de.unifrankfurt.informatik.acoli.fid.util.Utils;


/**
 * Experimental parser for finding links in HTML
 * @author frank
 *
 */
public class ParserHtml {
	
	public ParserHtml(){};
	
	public static void parseLinksInString (String html, String baseUrl) {
			
	Document doc;
	try {
	
		doc = Jsoup.parse(html, baseUrl);
		// need http protocol
		//doc = Jsoup.connect("http://google.com").get();
	
		// get page title
		String title = doc.title();
		Utils.debug("title : " + title);
	
		// get all links
		Elements links = doc.select("a[href]");
		for (Element link : links) {
	
			// get the value from href attribute
			Utils.debug("\nlink : " + link.attr("href"));
			Utils.debug("text : " + link.text());
	
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static void parseLinksFromUrl (String url) {
		
	Document doc;
	try {
	
		doc = Jsoup.connect(url).data("query", "Java").userAgent("Mozilla")
  	     .cookie("auth", "token")
  	     .timeout(3000)
  	     .post();
  	     // supports only http,https protocol
		 //doc = Jsoup.connect("http://google.com").get();
		 

		// get page title
		String title = doc.title();
		Utils.debug("title : " + title);
	
		// get all links
		Elements links = doc.select("a[href]");
		for (Element link : links) {
	
			// get the value from href attribute
			Utils.debug("\nlink : " + link.attr("href"));
			Utils.debug("text : " + link.text());
	
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList <String> parseLinksInFile (File file, String charset, String baseUri) {
		
		Document doc;
		ArrayList <String> results = new ArrayList <String> ();
		try {
		
			doc = Jsoup.parse(file, charset, baseUri);

			// get page title
			String title = doc.title();
			//Utils.debug("title : " + title);
		
			// get all links
			Elements links = doc.select("a[href]");
			for (Element link : links) {
		
				// get the value from href attribute
				// Utils.debug("\nlink : " + link.attr("href"));
				// Utils.debug("text : " + link.text());
				try {
					// check URL
					URL url = new URL(link.attr("href"));
					// check has extension (check extension type later)
					if (new File(url.getPath()).getName().contains(".")) {
						results.add(url.toString());
					}
				} catch (Exception e) {}
			}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return results;
		}
	
	
	
	public static String parseTextfromFile (File file, String charset, String baseUri) {
		
		Document doc;
		String text = "";
		try {
		
			doc = Jsoup.parse(file, charset, baseUri);

			// get page title
			String title = doc.title();
			//Utils.debug("title : " + title);
			text += title;
			text += doc.body().text();
			
		} catch (Exception e){}
			return text;
		}


	public static void main (String [] args) {
		
		File htmlDirectory = new File("/media/EXTRA/html/downloads");
		File [] files = htmlDirectory.listFiles();
		HashMap <Double, Integer> distribution = new HashMap<Double,Integer>();
		double score = 0;
		int hits = 0;
		for (File f : files) {
			String text = parseTextfromFile(f, "UTF-8", "http://dummy.org");
			score = HtmlEvaluator.findbuzzWords(text);
			if (score > 0) {
				Utils.debug(f.getAbsolutePath()+" :: "+score);
				hits++;
				if (!distribution.containsKey(score)) {
					distribution.put(score, 1);
				} else {
					distribution.put(score, distribution.get(score)+1);
				}
			}
		}
		
		Utils.debug("all :"+hits);
		
		ArrayList<Double> scores = new ArrayList<Double>();
		
		scores.addAll(distribution.keySet());
		Collections.sort(scores);
		for (Double sc : scores) {
			Utils.debugNor(sc+" : ");
			Utils.debug(distribution.get(sc));
		}
		
		/*
		String html = "<html><head><title>First parse</title></head>"
				  + "<body><p><a href='http://www.sil.org'>SIL International</a></p></body></html>";
		String baseUrl = "http://where-it-came-from.html"; // in order to resolve relative links
		File htmlFile = new File("/media/EXTRA/VifaRun/sil27717.html");
		
		//HtmlParser.parseString(html, baseUrl);
		//HtmlParser.parseUrl("https://www.sil.org/resources/archives/27717");
		for (String link : HtmlParser.parseLinksInFile(htmlFile, "UTF-8", "https://www.sil.org/resources/archives/27717")) {
			Utils.debug(link);
		}
		*/
	}
		 

}
