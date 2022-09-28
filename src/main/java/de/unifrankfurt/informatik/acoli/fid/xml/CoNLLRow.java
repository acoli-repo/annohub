package de.unifrankfurt.informatik.acoli.fid.xml;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class CoNLLRow implements Comparable<CoNLLRow> {
	private Integer INTERNAL_ID; // only use this for sorting rows internally
	private LinkedHashMap<String, String> columns;
	private LinkedHashMap<String, String> feats;
	
	
	public CoNLLRow(Integer id){
		columns = new LinkedHashMap<>();
		feats = new LinkedHashMap<>();
		INTERNAL_ID = id;
	}

	void injectInternalID(){
		LinkedHashMap<String, String> newMap = new LinkedHashMap<>();
		newMap.put("INTERNAL_ID", this.INTERNAL_ID.toString());
		newMap.putAll(this.columns);
		this.columns = newMap;
	}

	@Override
	public String toString(){
		ArrayList<String> repr = new ArrayList<>();
		repr.add(this.INTERNAL_ID.toString());
		columns.entrySet().forEach(
				a -> {if(a.getKey().equals("FEATS")){
					repr.add(swageFeats());
				} else{
					repr.add(a.getValue());
				}
				});
		if (!columns.containsKey("FEATS")){
			repr.add(swageFeats());
		}
	return String.join("\t", repr);
	}
	
	private String swageFeats (){
		ArrayList<String> featRepr = new ArrayList<>();
		for (String feat : feats.keySet()) {
			if (!feats.get(feat).equals("_")){
			featRepr.add(feat+"="+feats.get(feat));
			}
		}
		return String.join("|", featRepr);
	}

	@Override
	public int compareTo(CoNLLRow o) {
		return this.INTERNAL_ID - o.INTERNAL_ID;
	}

	/**
	 * @return the iNTERNAL_ID
	 */
	public Integer getINTERNAL_ID() {
		return INTERNAL_ID;
	}

	/**
	 * @param iNTERNAL_ID the iNTERNAL_ID to set
	 */
	public void setINTERNAL_ID(Integer iNTERNAL_ID) {
		INTERNAL_ID = iNTERNAL_ID;
	}

	/**
	 * @return the columns
	 */
	public LinkedHashMap<String, String> getColumns() {
		return columns;
	}

	/**
	 * @param columns the columns to set
	 */
	public void setColumns(LinkedHashMap<String, String> columns) {
		this.columns = columns;
	}

	/**
	 * @return the feats
	 */
	public LinkedHashMap<String, String> getFeats() {
		return feats;
	}

	/**
	 * @param feats the feats to set
	 */
	public void setFeats(LinkedHashMap<String, String> feats) {
		this.feats = feats;
	}
}
