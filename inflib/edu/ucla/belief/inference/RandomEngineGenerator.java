package edu.ucla.belief.inference;

import edu.ucla.belief.*;
import edu.ucla.belief.io.PropertySuperintendent;

import java.util.*;
import java.io.Serializable;

/** Generates instances of RandomInferenceEngine,
	an inference engine that does no computation,
	instead returns random answers,
	for the purpose of testing.

	@author Keith Cascio
    @since 20060201 */
public class RandomEngineGenerator extends Dynamator implements Serializable
{
	static final long serialVersionUID = 6100481570899378575L;

	public String getDisplayName(){
		return "random";
	}

	public Object getKey(){
		return getKeyStatic();
	}

	public static Object getKeyStatic(){
		return "randomenginegenerator6100481570899378575L";
	}

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ){
		return new RandomInferenceEngine( bn, dyn );
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){ return null; }
	public void       killState( PropertySuperintendent bn ){}

	public Dynamator getCanonicalDynamator(){
		return this;
	}

	public Collection getClassDependencies(){
		return Arrays.asList( new Class[] {
			RandomEngineGenerator.class,
			RandomInferenceEngine.class,
			java.math.BigInteger.class } );
	}
}
