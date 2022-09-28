package de.unifrankfurt.informatik.acoli.fid.conll;

import java.util.ArrayList;

public class ConllCSVColumn {
	
	private int column;
	private ArrayList <String> tokens;
	
	public ConllCSVColumn (int column, ArrayList<String> tokens) {
		this.column = column;
		this.tokens = tokens;
	}

	public int getColumn() {
		return column;
	}
	
	public ArrayList<String> getTokens() {
		return tokens;
	}
	
}
