package edu.ucla.belief;

import        java.io.PrintStream;

/** Static definitions removed from BeliefNetworks
	in order to break circular dependencies.

	@author keith cascio
	@since  20081110 */
public class Definitions{
	private  Definitions(){}

	public static       boolean
	  DEBUG                                      = false;
	public static final String
	  STR_SAMIAM_ACRONYM                         = "SamIam",
	  STR_VERBOSE_TRACE_MESSAGE                  = "VERBOSE *****Caught Exception***** VERBOSE:";
	public static final PrintStream
	  STREAM_TEST                                = System.out,
	  STREAM_DEBUG                               = System.out,
	  STREAM_VERBOSE                             = System.out;
}
