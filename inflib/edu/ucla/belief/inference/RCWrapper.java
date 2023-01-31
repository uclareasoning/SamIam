package edu.ucla.belief.inference;

import il2.inf.*;
import il2.bridge.Converter;
import il2.inf.PartialDerivativeEngine;
import edu.ucla.belief.*;
//{superfluous} import il2.model.BayesianNetwork;

import java.util.*;
import il2.util.*;

public class RCWrapper extends PartialDerivativeWrapper
{
    private il2.inf.rc.RCEngine pde;
    private Converter c;
    private RCInfo myRCInfo;
    double infoCompilationTime;
    double localMemoryRequirements;

	public RCWrapper( RCInfo info ){
		this( info, info.createTables() );
	}

	/** @since 20040614 */
	private RCWrapper( RCInfo info, il2.model.Table[] tables )
	{
		myRCInfo = info;
		infoCompilationTime=info.getCompilationTime();
		localMemoryRequirements=info.getMemoryRequirements();
		c=info.converter();
		pde=new il2.inf.rc.RCEngine( info.dgraph(), tables, info.indicatorLocs() );
		pde.allocateCaches(info.cachedNodes());
	}

	/** @since 20040614 */
	public RCWrapper handledClone( QuantitativeDependencyHandler handler ){
		return new RCWrapper( this.myRCInfo, handler );
	}

	/** @since 20040614 */
	public RCWrapper( RCInfo info, QuantitativeDependencyHandler handler ){
		this( info, info.createTables( handler ) );
	}

    protected PartialDerivativeEngine pdengine(){
	return pde;
    }

    protected JointEngine engine(){
	return pde;
    }

    protected Converter converter(){
	return c;
    }

    public il2.inf.rc.RCEngine rcEngine(){
    	return pde;
    }

    /** @since 20040614 */
    public RCInfo getRCInfo(){
		return this.myRCInfo;
	}

    public double getCompilationTime(){
	return pde.getCompilationTime()+infoCompilationTime;
    }
    public double getMemoryRequirements(){
	return localMemoryRequirements;
    }
}
