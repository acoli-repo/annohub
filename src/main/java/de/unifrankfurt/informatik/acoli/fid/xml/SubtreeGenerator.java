package de.unifrankfurt.informatik.acoli.fid.xml;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.unifrankfurt.informatik.acoli.fid.xml.Utils.stack2Path;


/**
 * SubtreeGenerator should be used to read in any .xml file that represents a corpus. It splits up a xml file of
 * arbitrary size into subtrees with a previously specified name. (e.g.: each sentence subtree).
 * Evaluates the xml lazily, thus saving significantly on memory. Also hold's a few service functions for
 * optional use.
 * @author lglaser
 *
 */
public class SubtreeGenerator implements Iterable<Document>, Iterator<Document>{

	public class Span implements Comparable<Span> {
		private Integer begin;
		private Integer end;

		Span() {
			this.begin = null;
			this.end = null;
		}

		public boolean isComplete() {
			return this.begin != null && this.end != null;
		}

		@Override
		public int compareTo(Span o) {
			return this.begin - o.begin;
		}

		@Override
		public String toString(){
			return "Span["+this.begin+", "+this.end+"]";
		}
	}

	private final static Logger LOGGER = Logger.getLogger(SubtreeGenerator.class.getName());
	private String sentenceName;
	private File file;
	private XMLEventReader xmlReader;
	private int indexOfNextXMLEvent; // this will always represent the index of the item next will return
	private int currentSpanIndex;
	private ArrayList<Span> subtreeSpans;
	private Stack<String> XPATHSTACK;
	private boolean SPLIT_ON_XPATH = false;


	public SubtreeGenerator(String sentenceName, String filePath) throws FileNotFoundException{
		LOGGER.info("Instantiating subtreeGenerator with String: "+filePath);
		this.sentenceName = sentenceName;
		File inFile;
		try {
			URL fileURL = new URL(filePath);
			inFile = FileUtils.toFile(fileURL);
		} catch (MalformedURLException e1) {
			LOGGER.info("MalformedURL, trying as local filePath..");
			inFile = new File(filePath);
		}
		this.file = inFile;
		try {
			this.initialize(this.file);
		} catch (XMLStreamException e) {
			LOGGER.severe("Unable to initialize the reader. Stacktrace:");
			e.printStackTrace();
		}
	}

	public SubtreeGenerator(String sentenceName, File file) throws FileNotFoundException {
		LOGGER.info("Instantiating subtreeGenerator with File: "+file.getAbsolutePath());
		this.sentenceName = sentenceName;
		this.file = file;
		try{
			this.initialize();
		} catch (XMLStreamException e) {
			LOGGER.severe("Unable to initialize the reader. Stacktrace: ");
			e.printStackTrace();
		}
	}

	/**
	 * Initializes the XMLReader directly with a file.
	 * @param file
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	private void initialize(File file) throws XMLStreamException, FileNotFoundException{
		Reader xml = new FileReader(file);
		XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		staxFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		XMLEventReader staxReader = staxFactory.createXMLEventReader( xml );
		this.setXmlReader(staxReader);
		this.indexOfNextXMLEvent = 0;
		this.currentSpanIndex = 0;
		this.XPATHSTACK = new Stack<>();
		if (this.sentenceName.contains("/")) {
			this.SPLIT_ON_XPATH = true;
		}
	}
	/**
	 * Sets up the XMLEventReader and sets it as an object variable.
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	private void initialize() throws XMLStreamException, FileNotFoundException {
		this.initialize(this.file);
	}

	public ArrayList<Span> collectSubtreeIndices(XMLEventReader staxReader) {
		ArrayList<Span> spanIndices = new ArrayList<>();
		int i = 0;
		Span span = new Span();
		while (staxReader.hasNext()) {
			try {
				XMLEvent next = staxReader.nextEvent();
				if (next.isStartElement()) {
					this.XPATHSTACK.push(next.asStartElement().getName().toString());
					if ((this.SPLIT_ON_XPATH && stack2Path(this.XPATHSTACK).equals(this.sentenceName))
							|| (!this.SPLIT_ON_XPATH && this.sentenceName.equals(next.asStartElement().getName().toString()))) {
						if (span.begin != null) {
							LOGGER.warning("Span " + span + " already has a begin.");
						}
						span.begin = i;
					}
				}
				if (next.isEndElement()) {
					if ((this.SPLIT_ON_XPATH && stack2Path(this.XPATHSTACK).equals(this.sentenceName))
							|| (!this.SPLIT_ON_XPATH && this.sentenceName.equals(next.asEndElement().getName().toString()))) {
						if (span.end != null) {
							LOGGER.warning("Span " + span + " already has an end.");
						}
						span.end = i;
						if (span.isComplete()) {
							LOGGER.finer("Found span [" + span.begin + ", " + span.end + "]");
							spanIndices.add(span);
						} else {
							LOGGER.warning("Overwriting span " + span);
						}

						span = new Span();

					}
					this.XPATHSTACK.pop();
				}
				i++;
			} catch (XMLStreamException e) {
				LOGGER.severe("Failed to read XMLEvent at index "+i+", stopping collection. Aborting. Stacktrace: "+e.getMessage());
				break;
			}
		}
		LOGGER.info("Collected "+spanIndices.size()+" indices from file.");
		return spanIndices;
	}

	/**
	 * Initializes again, renamed to have things more readable and provide public access
	 * without BufferedReader argument
	 */
	public void reset(){
		try {
			this.initialize(); // maybe don't just call init but do it properly?
		} catch (XMLStreamException e) {
			System.err.println("Couldn't reset the SubtreeGenerator");
			e.printStackTrace();
		}catch (FileNotFoundException e) {
			System.err.println("File went missing.");
			e.printStackTrace();
		}
	}

	//========================================================================
	// TREE NAVIGATION AND SAMPLING
	//========================================================================

	public Integer getDocumentLength() {
		// if we computed it already, we just get it.
		if (this.subtreeSpans != null) {
			return this.subtreeSpans.size();
		}
		// otherwise we compute the documentLength and save it for later use.
		else {
			this.subtreeSpans = this.collectSubtreeIndices(this.getXmlReader());
		}
		this.reset(); // reset reader to beginning
		return this.subtreeSpans.size();
	}

	/**
	 * TODO: document
	 * @param k
	 * @return
	 */
	public ArrayList<Document> getSamples(Integer k) {
		this.reset();
		int doclen = this.getDocumentLength();
		if (k > doclen) {
			LOGGER.info("sample size ("+k+") exceedes number of sentences, reducing to corpus size ("+this.getDocumentLength()+").");
			k = doclen;
		}
		ArrayList<Document> samples = new ArrayList<>();
		try {
			ArrayList<Integer> sampleIndices = createSampleIndices(k);
			Collections.sort(sampleIndices);
			for (Integer sampleIndex : sampleIndices) {
				Span sampleSpan = this.getSubtreeSpans().get(sampleIndex);
				LOGGER.fine("Sampling of subtree #"+sampleIndex+": "+sampleSpan);
				this.skipToBeginOf(sampleSpan);
				Document sampled = this.collectSubtree(sampleSpan);
				samples.add(sampled);
			}
		} catch (IllegalArgumentException e){
			this.reset();
			LOGGER.warning(e.toString());
			return samples;
		}
		this.reset();
		return samples;
	}

	/**
	 * creates k random indices originating from the number of sentences in one document.
	 * @param k
	 * @return
	 */
	ArrayList<Integer> createSampleIndices(Integer k){
		int max = this.getSubtreeSpans().size();
		ArrayList<Integer> sampleIndices = ThreadLocalRandom.current()
				.ints(0, max)
				.distinct().limit(k).boxed()
				.collect(Collectors.toCollection(ArrayList::new));
		return sampleIndices;
	}


	private void skipToBeginOf(Span target) {
		XMLEventReader xmlReader = this.getXmlReader();
		int indexBeforeSkipping = this.indexOfNextXMLEvent;
		LOGGER.fine("Skipping to begin of span from "+indexBeforeSkipping+", target span: "+target);
		// i < target.begin so we don't touch the element that will want to write to the DOM
		for (int i = indexBeforeSkipping; i < target.begin; i++){
			xmlReader.next();
			this.indexOfNextXMLEvent++;
			//System.out.println("Skipped to "+next+" at index "+this.indexOfNextXMLEvent);
		}
		this.setXmlReader(xmlReader); // save back
		LOGGER.fine("Skipped to begin of span, next Event will have index: "+this.indexOfNextXMLEvent +", target span: "+target);
	}



	//========================================================================
	// SUBTREE PARSING
	//========================================================================


	private Document collectSubtree(Span span){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document subtree = null;

		XMLEventReader current = this.getXmlReader(); // retrieve the Reader in it's current state
		try {
			db = dbf.newDocumentBuilder();
			subtree = db.newDocument();

			// Using an XMLWriter that writes directly to a DOM Object
			XMLEventWriter writer;
			writer = XMLOutputFactory.newInstance().createXMLEventWriter(new DOMResult(subtree));

			if ((span.begin) != this.indexOfNextXMLEvent) {
				LOGGER.warning("Beginning to collect from span "+span+" while next element reader will return is "+this.indexOfNextXMLEvent);
			}

			for (int i = span.begin; i <= span.end; i++){
				XMLEvent next = current.nextEvent();
				//System.out.println("Trying to write at index "+i+": "+next);
				writer.add(next);
				this.indexOfNextXMLEvent++; // update reader index


			}
			this.setXmlReader(current);

		}catch (XMLStreamException | ParserConfigurationException e ) {
			System.err.println("Unable to collect the subtree with index " + this.currentSpanIndex);
			e.printStackTrace();
		}
		return subtree;
	}
	@Override
	public boolean hasNext() {
		return this.currentSpanIndex < this.getDocumentLength();
	}

	private void push2Stack(XMLEvent startElement) {
		this.XPATHSTACK.push(startElement.asStartElement().getName().toString());
	}

	private void popFromStack(){
		this.XPATHSTACK.pop();
	}
	public Document next() {
		Span currentSpan = this.subtreeSpans.get(this.currentSpanIndex);
		this.skipToBeginOf(currentSpan);
		Document doc = this.collectSubtree(currentSpan);
		this.currentSpanIndex++;
		return doc;
	}

	//========================================================================
	// ITERATOR, SETTERS AND GETTERS
	//========================================================================

	@Override
	public Iterator<Document> iterator() {
		// TODO Auto-generated method stub
		return this;
	}
	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return this.file.getPath();
	}

	/**
	 * @return the xmlReader
	 */
	XMLEventReader getXmlReader() {
		return xmlReader;
	}

	/**
	 * @param xmlReader the xmlReader to set
	 */
	void setXmlReader(XMLEventReader xmlReader) {
		this.xmlReader = xmlReader;
	}


	public ArrayList<Span> getSubtreeSpans() {
		return subtreeSpans;
	}
}
