package de.unifrankfurt.informatik.acoli.fid.xml;

import com.google.gson.Gson;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class Utils {
    private final static Logger LOGGER =
            Logger.getLogger(Utils.class.getName());
    public static void printDocument(Document doc, OutputStream out) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc),
                    new StreamResult(new OutputStreamWriter(out, "UTF-8")));
        }
        catch (Exception e){
            System.err.println("Couldn't print");
        }
    }

    /**
     * reads the whole JSON template file and makes it iterable.
     */
    public static Template[] readJSONTemplates(String path) {
        LOGGER.fine("Loading templates from "+path);
        Template[] templates = {};
        Gson gson = new Gson();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            templates = gson.fromJson(in, Template[].class);
        }
        catch (FileNotFoundException e){
            // TODO: make some sort of default config, then change to warn.
            LOGGER.severe("Couldn't find templates file. Stacktrace: "+e.getMessage());
        }
        return templates;
    }

    /**
     * Retrieves a stack that represents an xPath and returns a string representation of this xPath.
     * @param stack stack of each element representing a visited node
     * @return an xpath to the top node
     */
    static String stack2Path(Stack<String> stack) {
        return String.join("/", stack);
    }


    static public File gunzip(File zippedFile){
        File out = null;
        String filePath = zippedFile.getAbsolutePath();
        InputStream inputStream = null;
        try {
            if (filePath.contains(".tar.gz")) {
                inputStream = new GZIPInputStream(new FileInputStream(zippedFile));
                out = new File(filePath.replace("tar.gz", ""));
            }
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buffer = new byte[1024];
            int len;
            while((len = inputStream.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }
    static public File unzip(File zippedFile){
        File out = null;
        ZipInputStream inputStream = null;
        try {
            inputStream = new ZipInputStream(new FileInputStream(zippedFile));
            out = new File(zippedFile.getAbsolutePath().replace(".zip",""));


            FileOutputStream fos = new FileOutputStream(out);
            byte[] buffer = new byte[1024];
            int len;
            while((len = inputStream.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

//    static public File untar(File taredFile){
//        File out = null;
//        TarArchiveInputStream inputStream = null;
//        try {
//            inputStream
//        }
//    }
    /**
     * gets a raw input string and tries to figure out whether it's an xml file or not.
     * more precisely, tries to generate a dom from a File, if it succeedes
     * we assume it's xml, if it fails we assume it isn't.
     * @param in File
     * @return
     * @throws ParserConfigurationException
     */
    public static boolean isXML(File in) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            try {
                dBuilder.parse(in);
                return true;
            }
            catch (IOException | SAXException e) {
                return false;
            }
        } catch (ParserConfigurationException e) {
            // most basic Builder, should never happen.
            System.err.println("Can't check if it's an XML");
        }
        return false;
    }

    public static boolean isURL(String urlCandidate) {
        try {
            new URL(urlCandidate);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static PrintStream convertFileToPrintStream(File file) throws FileNotFoundException {
        return new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
    }
}
