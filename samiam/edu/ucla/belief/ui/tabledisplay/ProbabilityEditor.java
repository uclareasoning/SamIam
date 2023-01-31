package edu.ucla.belief.ui.tabledisplay;

import javax.swing.JComponent;

/**
	Interface represents an Object that can handle
	many of the functions formerly performed
	by DisplayableFiniteVariableImpl

	@author Keith Cascio
	@since 061303
*/
public interface ProbabilityEditor
{
	public void setEditable( boolean editable );
	public boolean isEditable();
	public String commitProbabilityChanges();
	public String commitExcludeDataChanges();
	public boolean discardChanges();
	public JComponent makeProbabilityEditComponent();
	public String resize();
	public String normalize();
	public String complement();
	public String selectAll();
	public boolean isProbabilityEditInProgress();
	public void stopProbabilityEditing();
	public boolean isProbabilityEdited();
	public boolean isExcludeDataEdited();
}
