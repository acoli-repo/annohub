package de.unifrankfurt.informatik.acoli.fid.types;

/*
 * Resource filters in their order of precedence
 * RDF > CONLL > ARCHIVE > SPARQL > METASHARE
 * <p>
 * because filters do not have to be disjunct. For example some URLs will be found 
 * with the RDF as well as the ARCHIVE filter.
 */
public enum ResourceFilter {

	    RDF,CONLL,ARCHIVE,XML,SPARQL,HTML,PDF,METASHARE,JSON,NOEXT,UNKNOWN

}
