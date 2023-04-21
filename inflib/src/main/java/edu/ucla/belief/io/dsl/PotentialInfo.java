package edu.ucla.belief.io.dsl;

import edu.ucla.belief.Table;
import edu.ucla.belief.Potential;
import edu.ucla.belief.CPTShell;
import edu.ucla.belief.TableShell;
import edu.ucla.belief.NoisyOrShell;
import edu.ucla.belief.NoisyOrShellPearl;
import edu.ucla.belief.io.hugin.HuginPotential;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
	@author Keith Cascio
	@since 051403
*/
public class PotentialInfo
{
	public PotentialInfo( DSLNode newNode, String[] parentIDs, double[] probabilities, int[] strengths, int intDSLNodeType )
	{
		this.newNode = newNode;
		this.parentIDs = parentIDs;
		this.probabilities = probabilities;
		this.strengths = strengths;
		this.intDSLNodeType = intDSLNodeType;
	}

	public PotentialInfo( HuginPotential newPotential )
	{
		this.newPotential = newPotential;
	}

	public CPTShell makeShell( Map mapIDsToFVars ) throws Exception
	{
		if( myShell == null )
		{
			if( newPotential == null && intDSLNodeType == SMILEReader.DSL_NOISY_MAX )
			{
				DSLNode currentParent = null;
				List variables = new ArrayList( parentIDs.length + 1 );
				for( int i=0; i<parentIDs.length; i++ )
				{
					currentParent = (DSLNode) mapIDsToFVars.get( parentIDs[i] );
					if( currentParent == null ) System.err.println( "Error" );
					else variables.add( currentParent );
				}
				//variables.add( newNode );

				//myShell = new NoisyOrShell( variables, probabilities );
				myShell = new NoisyOrShellPearl( variables, newNode, probabilities, strengths );
			}
			else
			{
				Potential newTable = newPotential.makePotential( mapIDsToFVars );
				myShell = new TableShell( (Table) newTable );
			}
		}

		return myShell;
	}

	public DSLNode newNode;
	public String[] parentIDs;
	public double[] probabilities;
	public int[] strengths;
	public int intDSLNodeType;
	public HuginPotential newPotential;

	public CPTShell myShell;
}
