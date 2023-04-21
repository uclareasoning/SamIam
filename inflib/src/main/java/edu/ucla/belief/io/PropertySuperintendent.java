package edu.ucla.belief.io;

/** @author keith cascio
	@since  20021210 */
public interface PropertySuperintendent
{
	public java.util.Map getProperties();

	public static final String
	  KEY_HUGIN_ID                     = "ID",
	  KEY_HUGIN_NODE_SIZE              = "node_size",
	  KEY_HUGIN_LABEL                  = "label",
	  KEY_HUGIN_STATES                 = "states",
	  KEY_HUGIN_POSITION               = "position",
	  KEY_HUGIN_potential              = "potential",
	  KEY_HUGIN_potential_data         = "data",
	  KEY_HUGIN_SUBTYPE                = "subtype",
	  KEY_SOFT_EVIDENCE_CHILDREN       = "SOFT_EVIDENCE_CHILDREN",
	  KEY_APPROXIMATED_PARENTS         = "APPROXIMATED_PARENTS",
	  KEY_IDS_RECOVERABLE_PARENTS      = "ids_recoverable_parents",
	  KEY_RECOVERABLES                 = "recoverables",
	  KEY_ISMAPVARIABLE                = "ismapvariable",
      KEY_ISSDPVARIABLE                = "isdecisionvariable",
	  KEY_EXCLUDEPOLICY                = "excludepolicy",
	  KEY_EXCLUDEARRAY                 = "EXCLUDE_FROM_SENSITIVITY",
	  KEY_USERPROPERTIES               = "user_properties",//edu.ucla.belief.io.dsl.SMILEReader.KEY_USERPROPERTIES_UNFOLDED,
	  KEY_HUGIN_NAME                   = "name",
	  KEY_HUGINITY                     = "huginity",//huginness, huginacy, huginance, hugindom, huginism, huginity, huginship, correctness, validity, faithfulness, legitimacy
	  KEY_SEENDEFAULTEVIDENCE          = "seendefaultevidence",

	  VALUE_TRUE                       = "true",
	  VALUE_PERMISSIVE                 = "permissive",//relaxed, lenient, loose, forgiving
	  VALUE_STRICT                     = "strict";//enforcing, faithful, picky, pedantic, tight
}
