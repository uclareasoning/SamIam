package edu.ucla.belief.decision;

//{superfluous} import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.Table;
import edu.ucla.belief.TableIndex;
//{superfluous} import java.util.Set;

/** @author Keith Cascio
	@since 120804 */
public interface DecisionTree
{
	public double getParameter( final int[] indices );
	public DecisionLeaf getLeaf( final int[] indices );
	public DecisionNode getRoot();
	public TableIndex getIndex();
	public Table expand();
	public void normalize();
	public void addListener( DecisionListener listener );
	public boolean removeListener( DecisionListener listener );
	public void snapshot();
	public boolean restoreSnapshot();
	public void ensureSnapshot();
	public DecisionBackup getSnapshot();
	public void setSnapshot( DecisionBackup snaoshot );
}
