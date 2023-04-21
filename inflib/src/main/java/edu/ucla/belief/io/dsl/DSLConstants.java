package edu.ucla.belief.io.dsl;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

/** @author keith cascio
	@since  20020425 */
public class DSLConstants
{
	//special Hugin property keys
	public static final Object keyNOISYOR           = "noisyor";
  //public static final Object keyNOISYORWEIGHTS    = "noisyorweights";
  //public static final Object keyISDSL             = "isDSL";

	//property values
	public static final String
		valueTRUE                                   =                "TRUE",
		valueFALSE                                  =                "FALSE",
		valueCPT                                    =                "CPT",
		valueNOISY_OR                               =                "NOISY_OR",
		valueTRUTHTABLE                             =                "TRUTHTABLE";

	//DSL property keys
	public static final String
		STR_KEY_PREFIX                              =           "DSLx",
		KEY_SUBMODEL                                = STR_KEY_PREFIX+"SUBMODEL",
		KEY_OBSERVATION_COST_NODENAMESLIST          = STR_KEY_PREFIX+"OBSERVATION_COSTxNODENAMESLIST",
		KEY_OBSERVATION_COST_NODEPARENTSLIST        = STR_KEY_PREFIX+"OBSERVATION_COSTxNODEPARENTSLIST",
		KEY_OBSERVATION_COST_NODECOSTSLIST          = STR_KEY_PREFIX+"OBSERVATION_COSTxNODECOSTSLIST",
		KEY_HEADER_COMMENT                          = STR_KEY_PREFIX+"HEADERxCOMMENT",
		KEY_CREATION_CREATOR                        = STR_KEY_PREFIX+"CREATIONxCREATOR",
		KEY_CREATION_CREATED                        = STR_KEY_PREFIX+"CREATIONxCREATED",
		KEY_CREATION_MODIFIED                       = STR_KEY_PREFIX+"CREATIONxMODIFIED",
		KEY_NUMSAMPLES                              = STR_KEY_PREFIX+"NUMSAMPLES",
		KEY_SCREEN_COLOR                            = STR_KEY_PREFIX+"SCREENxCOLOR",
		KEY_SCREEN_SELCOLOR                         = STR_KEY_PREFIX+"SCREENxSELCOLOR",
		KEY_SCREEN_FONT                             = STR_KEY_PREFIX+"SCREENxFONT",
		KEY_SCREEN_FONTCOLOR                        = STR_KEY_PREFIX+"SCREENxFONTCOLOR",
		KEY_SCREEN_BORDERTHICKNESS                  = STR_KEY_PREFIX+"SCREENxBORDERTHICKNESS",
		KEY_SCREEN_BORDERCOLOR                      = STR_KEY_PREFIX+"SCREENxBORDERCOLOR",
		KEY_POSITION_WIDTH                          = STR_KEY_PREFIX+"POSITIONxWIDTH",
		KEY_POSITION_HEIGHT                         = STR_KEY_PREFIX+"POSITIONxHEIGHT",
		KEY_WINDOWPOSITION_CENTER_X                 = STR_KEY_PREFIX+"WINDOWPOSITIONxCENTER_X",
		KEY_WINDOWPOSITION_CENTER_Y                 = STR_KEY_PREFIX+"WINDOWPOSITIONxCENTER_Y",
		KEY_WINDOWPOSITION_WIDTH                    = STR_KEY_PREFIX+"WINDOWPOSITIONxWIDTH",
		KEY_WINDOWPOSITION_HEIGHT                   = STR_KEY_PREFIX+"WINDOWPOSITIONxHEIGHT",
		KEY_BKCOLOR                                 = STR_KEY_PREFIX+"BKCOLOR",
		KEY_SHOWAS                                  = STR_KEY_PREFIX+"SHOWAS",
		KEY_EXTRADEFINITION_DIAGNOSIS_TYPE          = STR_KEY_PREFIX+"EXTRA_DEFINITIONxDIAGNOSIS_TYPE",
		KEY_EXTRADEFINITION_RANKED                  = STR_KEY_PREFIX+"EXTRA_DEFINITIONxRANKED",
		KEY_EXTRADEFINITION_MANDATORY               = STR_KEY_PREFIX+"EXTRA_DEFINITIONxMANDATORY",
		KEY_EXTRADEFINITION_SETASDEFAULT            = STR_KEY_PREFIX+"EXTRA_DEFINITIONxSETASDEFAULT",
		KEY_EXTRADEFINITION_FAULT_STATES            = STR_KEY_PREFIX+"EXTRA_DEFINITIONxFAULT_STATES",
		KEY_EXTRADEFINITION_FAULT_NAMES             = STR_KEY_PREFIX+"EXTRA_DEFINITIONxFAULT_NAMES",
		KEY_EXTRADEFINITION_FAULT_LABELS            = STR_KEY_PREFIX+"EXTRA_DEFINITIONxFAULT_LABELS",
		KEY_EXTRADEFINITION_DEFAULT_STATE           = STR_KEY_PREFIX+"EXTRA_DEFINITIONxDEFAULT_STATE",
		KEY_EXTRADEFINITION_STATECOMMENTS           = STR_KEY_PREFIX+"EXTRA_DEFINITIONxSTATECOMMENTS",
		KEY_EXTRADEFINITION_STATEREPAIRINFO         = STR_KEY_PREFIX+"EXTRA_DEFINITIONxSTATEREPAIRINFO",
		KEY_EXTRADEFINITION_QUESTION                = STR_KEY_PREFIX+"EXTRA_DEFINITIONxQUESTION",
		KEY_EXTRADEFINITION_DOCUMENTATIONS          = STR_KEY_PREFIX+"EXTRA_DEFINITIONxDOCUMENTATIONS",
		KEY_SCREENCOMMENT_COORDINATES               = STR_KEY_PREFIX+"SCREENCOMMENTxCOORDINATES",
		KEY_SCREENCOMMENT_COMMENT                   = STR_KEY_PREFIX+"SCREENCOMMENTxCOMMENT",
		KEY_USER_PROPERTIES                         = STR_KEY_PREFIX+"USER_PROPERTIES",
		KEY_DOCUMENTATION                           = STR_KEY_PREFIX+"DOCUMENTATION",
		KEY_TARGET                                  = STR_KEY_PREFIX+"DOCUMENTATION",
		KEY_DEFINITION_RESULTINGSTATES              = STR_KEY_PREFIX+"DEFINITIONxRESULTINGSTATES",
		KEY_TYPE                                    = STR_KEY_PREFIX+"TYPE";

	/** <p>
		Expand noisy-Or weights into CPT. Works only for binary variables.
		<p>
		Preconditions:
		The node and each one of its parents are binary variables.
		<p>
		@author hei chan */
	public static List expandNoisyOr(List noisyOrWeights)
	{
		int size = noisyOrWeights.size();
		int cptSize = 1;
		for (int i = 0; i < size / 2; i++)
		cptSize *= 2;
		double[] cpt = null;
		//**************************************************
		//NOTE:
		//If the assumption that the node is a binary variable
		//is violated, and the node has many parents, this
		//line will very likely fail because the JVM cannot
		//allocate enough memory.	new() will throw an
		//OutOfMemoryError.
		try
		{
			cpt = new double[cptSize];
		}
		catch( OutOfMemoryError err )
		{
			return null;
		}

		//**************************************************
		Arrays.fill(cpt, -1);

		double leak = ((Double)noisyOrWeights.get(size - 1)).doubleValue();
		double[] params = new double[size / 2 - 1];
		for (int i = 0; i < size / 2 - 1; i++)
			params[i] = ((Double)noisyOrWeights.get(size - 2 * i - 3)).doubleValue();

		cpt[cptSize - 1] = leak;
		for (int i = cptSize - 1; i >= 0; i -= 2)
		{
			cpt[i - 1] = 1.0 - cpt[i];
			int offset = 2;
			for (int j = 0; j < size / 2 - 1; j++)
			{
				if (i < offset) break;
				if (cpt[i - offset] >= 0) break;
				cpt[i - offset] = cpt[i] * params[j] / leak;
				offset *= 2;
			}
		}

		ArrayList cptList = new ArrayList(cptSize);
		for (int i = 0; i < cptSize; i++)
			cptList.add(new Double(cpt[i]));
		return cptList;
	}
}
