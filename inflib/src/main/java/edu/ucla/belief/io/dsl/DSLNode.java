package edu.ucla.belief.io.dsl;

//{superfluous} import java.awt.Point;
//{superfluous} import java.awt.Dimension;
import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.*;

/**
* A class for encapsulating a hugin node description.
*/
public interface DSLNode extends StandardNode
{
	public DSLSubmodel getDSLSubmodel();
	public void setDSLSubmodel( DSLSubmodel model );

	/** @since 030402 */
	//public List getNoisyOrWeights();

	/** moved to FiniteVariable 010905 */
	//public DSLNodeType getDSLNodeType();

	/** moved to FiniteVariable 010905 */
	//public void setDSLNodeType( DSLNodeType newVal );

	/** moved to StandardNode */
	//public DiagnosisType getDiagnosisType();

	/** moved to StandardNode */
	//public void setDiagnosisType( DiagnosisType newVal );

	/** @since 030402 */
	public Boolean getMandatory();

	/** @since 030402 */
	public void setMandatory( Boolean newVal );

	/** @since 030402 */
	public Boolean getRanked();

	/** @since 030402 */
	public void setRanked( Boolean newVal );

	/** @since 030402 */
	public List getTargetList();

	/** @since 030402 */
	public void setTargetList( List newVal );

	/**
		Moved to StandardNode 041304
		@since 030402
	*/
	//public Integer getDefaultStateIndex();

	/** @since 030402 */
	public void setDefaultStateIndex( Integer newVal );
}
