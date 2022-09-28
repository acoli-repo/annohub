package de.unifrankfurt.informatik.acoli.fid.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public abstract class XMLConverter {


    abstract public boolean convertToStream(File sourceFile, PrintStream outStream) throws FileNotFoundException;

    abstract public PrintStream convertToStream(File sourceFile) throws FileNotFoundException;

    abstract public boolean convertToFile(File sourceFile, File outFile) throws FileNotFoundException;

    abstract public File convertToFile(File sourceFile) throws FileNotFoundException;
}
