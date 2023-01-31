package edu.ucla.belief.inference;

import edu.ucla.belief.*;
import java.util.*;

public class RCEngine extends PartialDerivativeWrapperEngine
{
	public RCEngine( RCSettings settings, Dynamator dyn )
	{
		this( settings.getInfo(), dyn );
		settings.addChangeListener( this );
	}

	private RCEngine(RCInfo info,Dynamator dyn)
	{
	    super( new RCWrapper(info),info.network(),dyn);
	    myRCWrapper = (RCWrapper)pdw;
	}

	/** @since 061404 */
	private RCEngine( RCWrapper rappa, BeliefNetwork bn, Dynamator dyn )
	{
	    super( rappa, bn, dyn );
	    myRCWrapper = rappa;
	}

	public il2.inf.rc.RC rcCore() {
		return myRCWrapper.rcEngine().rcCore();
	}

	/** @since 061404 */
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler )
	{
		RCEngine ret = new RCEngine( myRCWrapper.handledClone(handler), this.getBeliefNetwork(), this.getDynamator() );
		ret.setQuantitativeDependencyHandler( handler );
		return ret;
	}

	/** @since 20091226 */
	public String compilationStatus( edu.ucla.belief.io.PropertySuperintendent bn ){
		return "recursive conditioning " + edu.ucla.belief.inference.RCEngineGenerator.getSettings( bn ).describeUserMemoryProportion();
	}

	private RCWrapper myRCWrapper;
}
