package de.unifrankfurt.informatik.acoli.fid.types;

import org.apache.jena.riot.system.ErrorHandler;

public class RiotValidationLogger implements ErrorHandler {
	
	long warn = 0;
	long errors = 0;
	long fatal = 0;
	long uwcip = 0;

	@Override
	public void warning(String message, long line, long col) {
		System.out.println("WARN : "+message);
		warn++;
		if (message.contains("UNWISE_CHARACTER in PATH")) {uwcip++;}
	}

	@Override
	public void error(String message, long line, long col) {
		System.out.println("ERROR : "+message);
		errors++;
		
	}

	@Override
	public void fatal(String message, long line, long col) {
		System.out.println("FATAL : "+message);
		fatal++;
	}
	
	
	public boolean validationHasFailed() {
		return (errors > 0 || fatal > 0 || uwcip > 0);
	}

}
