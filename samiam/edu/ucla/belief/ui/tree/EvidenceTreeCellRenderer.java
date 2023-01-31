package edu.ucla.belief.ui.tree;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.ui.preference.*;

import edu.ucla.belief.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

import java.text.NumberFormat;
import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.tree.*;

/**
	TreeCellRenderer for class EvidenceTree.
*/
public class EvidenceTreeCellRenderer extends DefaultTreeCellRenderer
{
	protected SamiamPreferences myTreePrefs;

	private NetworkInternalFrame hnInternalFrame;
	private ComputationCache myComputationCache;
	protected List myListTypesToShow = new ArrayList( 3 );
	protected boolean myFlagShowTypeProbabilities = false;
	private boolean myShowAbsentTypes = true;

	public EvidenceTreeCellRenderer(	SamiamPreferences treePrefs,
						NetworkInternalFrame hnInternalFrame )
	{
		this.myTreePrefs = treePrefs;
		this.hnInternalFrame = hnInternalFrame;
		this.myComputationCache = hnInternalFrame.getComputationCache();

		updateDefaults();

		normalColor   = (Color) myTreePrefs.getMappedPreference( SamiamPreferences .treeNormalClr ).getValue();
		observedColor = (Color) myTreePrefs.getMappedPreference( SamiamPreferences     .manualClr ).getValue();
		warnColor     = (Color) myTreePrefs.getMappedPreference( SamiamPreferences       .warnClr ).getValue();
		//System.out.println( "EvidenceTreeCellRenderer() warnColor == " + warnColor );
		//if( warnColor.equals( Color.pink ) ) //System.out.println( "\t(pink)" );

		setOpenIcon( null );
		setClosedIcon( null );
		setLeafIcon( null );

		if( leafIcon != null ) myWidthBufferLeaf += leafIcon.getIconWidth();
	}

	protected int myWidthBufferLeaf = 0;

	protected Font               myFont, myBoldFont, myBigBoldFont;
	protected FontRenderContext  myFontRenderContext;

	/** @since 030702 */
	public void setShowProbabilities( DiagnosisType type, boolean show )
	{
		if( show )
		{
			if( !myListTypesToShow.contains( type ) ) myListTypesToShow.add( type );
		}
		else myListTypesToShow.remove( type );
		myFlagShowTypeProbabilities = !myListTypesToShow.isEmpty();
	}

	/** @since 031102 */
	public void setShowAbsentType( boolean show )
	{
		myShowAbsentTypes = show;
	}

	public static String KEY_BACKGROUND_COLOR = "Tree.background";
	//public static String KEY_FONT = "Tree.font";

	/*
	// @since 081302
	public void updateUI()
	{
		super.updateUI();
		updateDefaults();
	}*/

	/** @since 0812002 */
	protected void updateDefaults()
	{
		UIDefaults defs = UIManager.getDefaults();

		if( hnInternalFrame != null )
		{
			//myFont = defs.getFont( KEY_FONT );
			JLabel dummy = new JLabel();
			myFont = dummy.getFont();
			myBoldFont = myFont.deriveFont( Font.BOLD );
			myBigBoldFont = myBoldFont.deriveFont( (float)15 );
			Graphics2D graphics = (Graphics2D) hnInternalFrame.getParentFrame().getGraphics();
			if( graphics == null ) System.err.println( "graphics == null" );
			else myFontRenderContext = graphics.getFontRenderContext();

			backgroundColor = defs.getColor( KEY_BACKGROUND_COLOR );
			emptyPanel.setBackground( backgroundColor );
			setBackgroundNonSelectionColor( backgroundColor );
			setBackgroundSelectionColor( new Color( 0x33, 0x99, 0xff ) );
		}
	}

	private Color normalColor, backgroundColor, observedColor, warnColor,
	              typeColor      = new Color(   0, 192, 0 ),
	              submodelColor  = new Color( 192,   0, 0 );
	//protected static final Dimension DIM_COMPONENT_SIZE = new Dimension( 120, 50 );

	public static final Dimension DIM_ZERO = new Dimension((int)0,(int)0);
	public static JComponent emptyPanel = new JPanel();
	static
	{
		emptyPanel.setPreferredSize( DIM_ZERO );
		emptyPanel.setSize( DIM_ZERO );
	}

	/** @since 20020819 */
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		JLabel                 ret          = (JLabel) super.getTreeCellRendererComponent( tree, value, selected, expanded, leaf, row, hasFocus );
		EvidenceTree           evidenceTree = (EvidenceTree) tree;
		DefaultMutableTreeNode node         = (DefaultMutableTreeNode) value;
		Object                 obj          = node.getUserObject();

		String     newText                  = null;
		Color      newForeground            = normalColor;
		Font       newFont                  = null;
		boolean    flagDebugShowProbability = false;

		if( obj instanceof DisplayableFiniteVariable )
		{
			DisplayableFiniteVariable dvar = (DisplayableFiniteVariable)obj;
			Object evidence = evidenceTree.getEvidence( dvar );
			newText = dvar.toString();
			if( evidence == EvidenceTree.WARNING ) newForeground = warnColor;
			else if( evidence != null )
			{
				newForeground = observedColor;
				if( !expanded ) newText += " = " + evidence.toString();
			}
		}
		else if( obj instanceof EnumValue )
		{
			if( leaf && !myShowAbsentTypes ) return emptyPanel;
			//newText = obj.toString()+" NODES";
			newText = obj.toString();
			newFont = myBigBoldFont;
			newForeground = typeColor;
		}
		else if( obj == evidenceTree.myUserobjectSubmodels )
		{
			if( leaf && !myShowAbsentTypes ) return emptyPanel;
			newText = obj.toString();
			newFont = myBoldFont;
			newForeground = typeColor;
		}
		else if( obj instanceof DSLSubmodel )
		{
			newText = obj.toString();
			newForeground = submodelColor;
		}
		else
		{
			Object instance = obj;
			String text = instance.toString();

			DisplayableFiniteVariable parent = null;
			DefaultMutableTreeNode pnode = (DefaultMutableTreeNode)(node.getParent());
			if( pnode != null )
			{
				parent = (DisplayableFiniteVariable)(pnode.getUserObject());
				if( myFlagShowTypeProbabilities && myListTypesToShow.contains( parent.getDiagnosisType() ) )
				{
					myUtilVariableInstance.setData( parent, instance );
					List targetList = null;
					Integer one = new Integer( (int)1 );
					int evidenceIndex = (int)-1;
					Object indexValue = null;

					targetList = parent.getTargetList();
					if( targetList == null )
					{
						//System.err.println( "Java EvidenceTreeCellRenderer warning: missing fault state list for target node " + parent );
						flagDebugShowProbability = true;
						text = getConditionalString( myUtilVariableInstance );//hnInternalFrame.getConditionalString( parent, instance, message_status );
					}
					else
					{
						evidenceIndex = parent.index( instance );
						indexValue = targetList.get( evidenceIndex );
						if( indexValue == null ) System.err.println( "Java EvidenceTreeCellRenderer warning: missing fault state value at index " + evidenceIndex + " for target node " + parent );
						else if( indexValue.equals( one ) )
						{
							flagDebugShowProbability = true;
							text = getConditionalString( myUtilVariableInstance );//hnInternalFrame.getConditionalString( parent, instance, message_status );
						}
					}
				}
			}
			newText = text;
			if( parent != null )
			{
				Object evidence = evidenceTree.getEvidence(parent);
				if( evidence == EvidenceTree.WARNING  && evidenceTree.getWarningEvidenceValue(parent) == instance ) newForeground = warnColor;
				else if( evidence == instance ) newForeground = observedColor;
			}
		}

		if( newFont == null ) newFont = myFont;
		ret.setFont( newFont );
		if( newText != null ) ret.setText( newText );
		if( newForeground != null ) ret.setForeground( newForeground );

	  /*if( myFontRenderContext != null ){
			myRectangle2D = (Rectangle2D.Float) newFont.getStringBounds( getText(), myFontRenderContext );
			myDimension.height = (int) myRectangle2D.getHeight();
			myDimension.width = (int) myRectangle2D.getWidth() + INT_WIDTH_BUFFER;
			if( leaf ) myDimension.width += myWidthBufferLeaf;
			ret.setPreferredSize( myDimension );
			ret.setMinimumSize( myDimension );
			//ret.setMaximumSize( myDimension );
			//ret.setBorder( BorderFactory.createLineBorder( Color.blue, (int)1 ) );
			//System.out.println( myDimension + "\n" );
			//if( flagDebugShowProbability ) Util.STREAM_DEBUG.println( getText() + " -> " + ret.getMinimumSize().width );
		}*/

		ret.invalidate();
		return ret;
	}

	/** @since 012004 */
	private String getConditionalString( VariableInstance inst ){
		return myComputationCache.getConditionalString( inst, STR_MESSAGE_STATUS );
	}

	private VariableInstance myUtilVariableInstance = new VariableInstance( null, null );
	protected Dimension myDimension = new Dimension();
	protected Rectangle2D.Float myRectangle2D = null;
	public static int INT_WIDTH_BUFFER = (int)28;
	public static final String STR_MESSAGE_STATUS = " evidence tree calculating conditional for ";
}
