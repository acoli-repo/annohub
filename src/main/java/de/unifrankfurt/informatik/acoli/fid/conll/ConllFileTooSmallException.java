package de.unifrankfurt.informatik.acoli.fid.conll;

public class ConllFileTooSmallException extends Exception {

	public ConllFileTooSmallException(int rowCount) {
		super ("Conll file to small - has only "+rowCount+ " rows.");
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3403434199243200598L;

}
