package edu.ucla.belief.ui.clipboard;

import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.belief.io.StandardNode;
import edu.ucla.belief.io.NodeLinearTask;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.tabledisplay.HuginGenieStyleTableFactory;

import java.util.List;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

/** @author keith cascio
	@since 20021016 */
public class NetworkClipBoardImpl implements NetworkClipBoard, ActionListener
{
	public NetworkClipBoardImpl()
	{
		myGraph = new HashDirectedGraph();
	}

	public synchronized void copy( DisplayableBeliefNetwork network, Set dVars )//DirectedGraph network, Set dVars )
	{
		//System.out.println( "NetworkClipBoardImpl.copy()" );

		if( dVars.isEmpty() ) return;

		clear();
		myVirtualCenter = null;
		INT_PASTE_ID_GUESS = (int)0;

		myNetworkCopiedFrom = network;
		Map mapVariablesOldToNew = cloneAll( dVars, (Copier)null, (DisplayableBeliefNetwork)null, (DisplayableBeliefNetwork)null );
		Collection newDVars = mapVariablesOldToNew.values();

		addAll( newDVars );

		Set tempIncoming = null;
		DisplayableFiniteVariable oldDVar = null;
		DisplayableFiniteVariable newDVar = null;
		DisplayableFiniteVariable oldParent = null;
		DisplayableFiniteVariable newParent = null;
		CPTShell tempCPTShell = null;

		for( Iterator itToCopy = dVars.iterator(); itToCopy.hasNext(); )
		{
			oldDVar = (DisplayableFiniteVariable)itToCopy.next();
			newDVar = (DisplayableFiniteVariable) mapVariablesOldToNew.get( oldDVar );
			tempIncoming = network.inComing( oldDVar );
			//System.out.println( "\tnetwork.inComing( "+oldDVar+" ) == " + tempIncoming );
			for( Iterator itParents = tempIncoming.iterator(); itParents.hasNext(); )
			{
				oldParent = (DisplayableFiniteVariable) itParents.next();
				if( dVars.contains( oldParent ) )
				{
					newParent = (DisplayableFiniteVariable) mapVariablesOldToNew.get( oldParent );
					//System.out.println( "\tcloning edge from " + newParent + " to " + newDVar );
					addEdge( newParent, newDVar );
				}
				else{
					for( Iterator itTypes = DSLNodeType.iterator(); itTypes.hasNext(); ){
						tempCPTShell = newDVar.getCPTShell( (DSLNodeType)itTypes.next() );
						if( tempCPTShell != null ) tempCPTShell.forget( oldParent );
					}
				}
			}
		}

		massageMapByReplacingCPTShellVariables( mapVariablesOldToNew );
	}

	public synchronized void cut( DisplayableBeliefNetwork network, NetworkDisplay display, Set dVars )//DirectedGraph network, NetworkDisplay display, Set dVars )
	{
		copy( network, dVars );
		if( !dVars.isEmpty() ) display.deleteNodes( dVars );
	}

	public boolean paste( DisplayableBeliefNetwork network, NetworkInternalFrame hnInternalFrame, Point center )
	{
		boolean withEdges   = true, withCPs = true, withRegexes = false;
		if( (myCbTelescope != null) && myCbTelescope.isSelected() ){
			withEdges       = myRadioWithEdges .isSelected();
			withCPs         = myRadioWithCPTs  .isSelected();
			withRegexes     = myCbUseRegex     .isSelected();
		}
		return paste( network, hnInternalFrame, center, withEdges, withCPs, withRegexes );
	}

	public synchronized boolean paste( DisplayableBeliefNetwork network, NetworkInternalFrame hnInternalFrame, Point actualCenter, boolean withEdges, boolean withCPs, boolean withRegexes )
	{
		this.myNetworkPastedTo = network;
		Map mapVariablesOldToNew = cloneAll( vertices(), network.getCopier(), myNetworkCopiedFrom, network );
		massageMapByReplacingCPTShellVariables( mapVariablesOldToNew );
		Map mapVariablesNewToOld = Util.invertMap( mapVariablesOldToNew );

		refresh( mapVariablesOldToNew );

		if( ! rebaptize( network, withRegexes ) ){ return false; }

		resetVirtualCenter( hnInternalFrame.getNetworkDisplay().actualToVirtual( new Point( actualCenter ) ) );

		DisplayableFiniteVariable newDVar = null;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			newDVar = (DisplayableFiniteVariable) it.next();
			newDVar.setNetworkInternalFrame( hnInternalFrame );
			network.addVariable( newDVar, !withCPs );
		}

		if( withEdges )
		{
			for( Iterator it = iterator(); it.hasNext(); )
			{
				newDVar = (DisplayableFiniteVariable) it.next();
				for( Iterator itChildren = outGoing( newDVar ).iterator(); itChildren.hasNext(); )
				{
					network.addEdge( newDVar, (Variable) itChildren.next(), !withCPs );
				}
			}
		}

		NetStructureEvent NSE = new NetStructureEvent( NetStructureEvent.NODES_ADDED, vertices() );
		hnInternalFrame.netStructureChanged( NSE );

		try{
			hnInternalFrame.getNetworkDisplay().select( (Collection) this );
		}catch( Throwable thrown ){
			System.err.println( "warning: NetworkClipBoardImpl.paste() caught " + thrown );
		}

		refresh( mapVariablesNewToOld );
		return true;
	}

	public boolean promptPaste( DisplayableBeliefNetwork network, NetworkInternalFrame hnInternalFrame, Point actualCenter, Component parent )
	{
		this.myNetworkPastedTo = network;
		boolean success = false;
		int result = JOptionPane.showConfirmDialog( parent, getPromptPastePanel(), "Paste Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		while( (result == JOptionPane.OK_OPTION) && (!( success = paste( network, hnInternalFrame, actualCenter, myRadioWithEdges.isSelected(), myRadioWithCPTs.isSelected(), myCbUseRegex.isSelected() ) )) )
		{
			result = JOptionPane.showConfirmDialog( parent, getPromptPastePanel(), "Paste Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		}
		return success;
	}

	public JComponent getPromptPastePanel()
	{
		if( myPromptPastePanel == null ) makePromptPastePanel();
		return myPromptPastePanel;
	}

	public static final String
	  STR_REGEX_MATCH_DEFAULT                =    ".*",
	  STR_APPEND                             =    "_2",
	  STR_REGEX_ID_REPLACEMENT_DEFAULT       =     "&" + STR_APPEND,
	  STR_REGEX_LABEL_REPLACEMENT_DEFAULT    =     "&";
	protected JComponent myPromptPastePanel;
	protected JCheckBox myRadioWithEdges, myRadioWithCPTs, myCbUseRegex, myCbTelescope;
	private JTextField myTfRegexIDMatch, myTfRegexIDReplace, myTfRegexLabelMatch, myTfRegexLabelReplace;
	private JButton myButtonPreview, myButtonReset, myButtonSuggest;
	private Runnable myOnclick;
	private JPanel myPanelRegexes;
	private JLabel myLabelIds, myLabelLabels, myLabelHelp, myLabelPrepositionIds, myLabelPrepositionLabels;

	public static final String
	  STR_TOOLTIP_ONOFF          =   "<html><nobr>Use <b>regular expressions</b> to control how "+UI.STR_SAMIAM_ACRONYM+" generates new ids/labels for the pasted nodes",
	  STR_TOOLTIP_IDS            =   "<html><nobr>Variable ids - must be <b>unique</b>",
	  STR_TOOLTIP_LABELS         =   "<html><nobr>Variable labels - can be <b>non-unique</b>",
	  STR_TOOLTIP_MATCH_REGEX    =   "<html><nobr>Type a <b>regular expression</b>, which can contain groups",
	  STR_TOOLTIP_REPLACEMENT    =   "<html><nobr>Type a <b>replacement expression</b>: "+UI.STR_SAMIAM_ACRONYM+" replaces \"<b>&amp;</b>\" with the entire match, \"<b>\\n</b>\" with the n<sup>th</sup> matched group",
	  STR_TOOLTIP_HELP           =   "Open the Sun Java regular expression web tutorial",
	  STR_TOOLTIP_PREVIEW        =   "Check the validity of the ids/labels that would result from the current expressions",
	  STR_TOOLTIP_RESET          =   "The default expressions match the entire original string and append \"" +STR_APPEND+ "\" to the id",
	  STR_TOOLTIP_TELESCOPE      =   "<html><nobr>to <font color='#990000'><b>deactivate</b></font>, invoke another special paste",
	  STR_TOOLTIP_SUGGEST        =   "Supply best-effort matching regular expressions and replacement expressions";

	/** @since 031505 */
	private void doPreview(){
		Replacements replacements = null;
		String error = null;
		try{
			replacements = previewReplacements();
		}catch( Throwable throwable ){
			error = throwable.getMessage();
			//System.err.println( throwable );
			throwable.printStackTrace();
		}
		Object message = null;
		int type = JOptionPane.PLAIN_MESSAGE;
		if( replacements == null ){
			if( error == null ) error = "unknown error";
			message = error;
			type = JOptionPane.ERROR_MESSAGE;
		}
		else message = replacements.getPreview();

		JOptionPane.showMessageDialog( myPanelRegexes, message, "Preview", type );
	}

	public static final String STR_PREFIX_PREVIEW = "<html><nobr><font size=\"4\">";

	/** @since 031505 */
	public static Dimension bestTableSize( JTable table ){
		TableColumnModel columnmodel = table.getColumnModel();
		int numcolumns = table.getColumnCount();
		TableColumn column;
		Dimension preferred;
		int maxHeight = 0;
		int totalWidth = 0;
		for( int i=0; i<numcolumns; i++ ){
			column = columnmodel.getColumn(i);
			preferred = HuginGenieStyleTableFactory.getPreferredSizeForColumn( column, table ).pref;
			maxHeight = Math.max( maxHeight, preferred.height );
			column.setPreferredWidth( preferred.width );
			totalWidth += preferred.width;
		}
		Dimension ret = new Dimension( totalWidth, maxHeight );
		//table.setPreferredSize( ret );
		return ret;
	}

	/** @since 031505 */
	public static class Replacements extends AbstractTableModel implements TableModel
	{
		public Replacements( ReplacementStruct[] data ){
			this.myData = data;
			init();
		}

		private void init(){
			myNumInvalid = myNumNonUnique = 0;
			for( int i=0; i<myData.length; i++ ){
				if( !myData[i].flagIDValid ) ++myNumInvalid;
				if( !myData[i].flagIDUnique ) ++myNumNonUnique;
			}
			myNumNonUnique -= myNumInvalid;
		}

		public int getNumInvalid(){
			return myNumInvalid;
		}

		public int getNumNonUnique(){
			return myNumNonUnique;
		}

		public boolean isValid(){
			return (myNumInvalid == 0) && (myNumNonUnique == 0);
		}

		public void rebaptizeAll(){
			for( int i=0; i<myData.length; i++ ){
				myData[i].rebaptize();
			}
		}

		public String getMessage(){
			String message = null;

			if( (myNumInvalid == 0) && (myNumNonUnique == 0) ) message = STR_PREFIX_PREVIEW + "All <b>" + Integer.toString(myData.length) + "</b> ids are "+STR_PREFIX_VALID+"valid</font>.";
			else{
				message = STR_PREFIX_PREVIEW + "Please edit the expressions because ";
				if( myNumInvalid > 0 ){
					message += "<b>" + Integer.toString(myNumInvalid) + "</b> ids are "+STR_PREFIX_INVALID+"invalid ";
					if( myNumNonUnique > 0 ) message += "and ";
					else  message += "</font>.";
				}
				if( myNumNonUnique > 0 ) message += "<b>" + Integer.toString(myNumNonUnique) + "</b> ids are "+STR_PREFIX_NOTUNIQUE+"not unique</font>.";
			}

			return message;
		}

		public JScrollPane makePain(){
			if( myPain == null ){
				JTable table = new JTable( (TableModel)this );
				Dimension preferred = bestTableSize( table );
				myPain = new JScrollPane( table );
				myPain.setPreferredSize( new Dimension( preferred.width + 16, preferred.height + 16 ) );
			}
			return myPain;
		}

		public JComponent getPreview(){
			if( myPreview == null ){
				myPreview = new JPanel( new GridBagLayout() );
				GridBagConstraints c = new GridBagConstraints();

				c.anchor = GridBagConstraints.WEST;
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.fill = GridBagConstraints.BOTH;
				c.weightx = c.weighty = 1;
				myPreview.add( makePain(), c );

				c.gridwidth = GridBagConstraints.REMAINDER;
				c.fill = GridBagConstraints.NONE;
				c.weightx = c.weighty = 0;
				myPreview.add( Box.createVerticalStrut(32), c );
				myPreview.add( new JLabel( getMessage() ), c );
			}

			return myPreview;
		}

		public int getColumnCount(){
			return 5;
		}

		public int getRowCount(){
			return myData.length;
		}

		public Object getValueAt( int row, int column ){
			return myData[row].getValueAt( column );
		}

		public String getColumnName( int column ){
			return ARRAY_COLUMN_NAMES[ column ];
		}

		private ReplacementStruct[] myData;
		private int myNumInvalid = (int)-1;
		private int myNumNonUnique = (int)-1;
		private JScrollPane myPain;
		private JComponent myPreview;
	}

	public static final String[] ARRAY_COLUMN_NAMES = new String[] { "original id", "new id", "id status", "original label", "new label" };
	public static final String
	  STR_PREFIX_VALID       =    "<html><nobr><font color=\"#009900\">",
	  STR_STATUS_VALID       =    STR_PREFIX_VALID + "valid",
	  STR_PREFIX_INVALID     =    "<html><nobr><font color=\"#CC0000\">",
	  STR_STATUS_INVALID     =    STR_PREFIX_INVALID + "<strike>valid",
	  STR_PREFIX_NOTUNIQUE   =    "<html><nobr><font color=\"#FF0000\">",
	  STR_STATUS_NOTUNIQUE   =    STR_PREFIX_NOTUNIQUE + "<strike>unique";

	/** @since 031505 */
	private Replacements previewReplacements() throws Exception
	{
		String regexIDMatch = myTfRegexIDMatch.getText();
		String regexLabelMatch = myTfRegexLabelMatch.getText();
		Pattern patternMatchID = Pattern.compile( regexIDMatch );
		Pattern patternMatchLabel = Pattern.compile( regexLabelMatch );
		String replacementID = myTfRegexIDReplace.getText();
		String replacementLabel = myTfRegexLabelReplace.getText();

		ReplacementStruct[] ret = new ReplacementStruct[ this.size() ];
		Set newIds = new HashSet( this.size() );

		StandardNode standard = null;
		String idOld, labelOld, idNew, labelNew;
		Matcher matcherID, matcherLabel;
		ReplacementStruct struct;
		int count = 0;
		for( Iterator it = this.iterator(); it.hasNext(); ){
			standard = (StandardNode) it.next();
			idOld = safe( standard.getID() );
			labelOld = safe( standard.getLabel() );
			idNew = replace( idOld, patternMatchID, replacementID, "id" );
			labelNew = replace( labelOld, patternMatchLabel, replacementLabel, "label" );
			struct = new ReplacementStruct( standard, idOld, idNew, labelOld, labelNew );
			ret[ count++ ] = struct;
			if( newIds.contains( idNew ) ) struct.setUnique( false );
			else newIds.add( idNew );
			//System.out.println( struct );
		}

		return new Replacements( ret );
	}

	/** @since 031505 */
	public static String safe( String original ){
		if( (original == null) || (original.length()<1) ) original = "blank";
		return original;
	}

	/** @since 031505 */
	public class ReplacementStruct
	{
		public ReplacementStruct( StandardNode standard, String idOld, String idNew, String labelOld, String labelNew ){
			this.standard = standard;
			this.idOld = idOld;
			this.idNew = idNew;
			this.labelOld = labelOld;
			this.labelNew = labelNew;
			this.init();
		}

		private void init(){
			this.flagIDValid = (idNew.length() > 0) && IdentifierField.isValidID( idNew );
			if( this.flagIDValid && (myNetworkPastedTo != null) ){
				this.flagIDUnique = (myNetworkPastedTo.forID( idNew ) == null);
			}
			initDisplay();
		}

		private void initDisplay(){
			String prefix = flagIDValid ? ( flagIDUnique ? STR_PREFIX_VALID : STR_PREFIX_NOTUNIQUE ) : STR_PREFIX_INVALID;
			this.idDisplay = prefix + idNew;
			this.statusDisplay = flagIDValid ? ( flagIDUnique ? STR_STATUS_VALID : STR_STATUS_NOTUNIQUE ) : STR_STATUS_INVALID;
		}

		public String toString(){
			return "id: " + idOld + " -> " + idNew + "(valid? "+flagIDValid+") (unique? "+flagIDUnique+"), label: " + labelOld + " -> " + labelNew;
		}

		public Object getValueAt( int column ){
			switch(column){
				case 0:
					return idOld;
				case 1:
					return idDisplay;
				case 2:
					return statusDisplay;
				case 3:
					return labelOld;
				case 4:
					return labelNew;
				default:
					return "error";
			}
		}

		public void setValid( boolean flag ){
			if( flagIDValid != flag ){
				flagIDValid = flag;
				initDisplay();
			}
		}

		public void setUnique( boolean flag ){
			if( flagIDUnique != flag ){
				flagIDUnique = flag;
				initDisplay();
			}
		}

		public void rebaptize(){
			standard.setID( idNew );
			standard.setLabel( labelNew );
		}

		public  StandardNode standard;
		public  String idOld, idNew, idDisplay, labelOld, labelNew, statusDisplay;
		private boolean flagIDValid = false, flagIDUnique = false;
	}

	/** @since 031505 */
	private static String replace( String original, Pattern patt, String replacement, String description ) throws Exception
	{
		Matcher matcher = patt.matcher( original );
		if( !matcher.find() ) throw new Exception( "Regex \"" + patt.pattern() + "\" does not match "+description+" \"" + original + "\"" );
		//StringBuffer buffer = new StringBuffer( original.length() );
		String replaced = replacement;
		try{
			replaced = Pattern.compile( "\\Q&" ).matcher( replaced ).replaceAll( matcher.group() );
			int groupCount = matcher.groupCount();
			for( int i=0; i<=groupCount; i++ ){
				replaced = Pattern.compile( "\\Q\\" + Integer.toString(i) ).matcher( replaced ).replaceAll( matcher.group(i) );
			}
		}catch( Throwable throwable ){
			throw new Exception( "Illegal "+description+" replacement expression \"" + replacement + "\": " + throwable.getMessage() );
		}
		return replaced;
	}

	/** @since 20050315 */
	private void setRegexesEnabled(   boolean flag ){
		myTfRegexIDMatch         .setEnabled( flag );
		myTfRegexIDReplace       .setEnabled( flag );
		myTfRegexLabelMatch      .setEnabled( flag );
		myTfRegexLabelReplace    .setEnabled( flag );
		myButtonPreview          .setEnabled( flag );
		myButtonReset            .setEnabled( flag );
		myButtonSuggest          .setEnabled( flag );
		myLabelIds               .setEnabled( flag );
		myLabelLabels            .setEnabled( flag );
		myLabelHelp              .setEnabled( flag );
		myLabelPrepositionIds    .setEnabled( flag );
		myLabelPrepositionLabels .setEnabled( flag );
		myPanelRegexes           .setEnabled( flag );
	}

	/** @since 20050315 */
	public void resetRegexes(){
		myTfRegexIDMatch.setText( STR_REGEX_MATCH_DEFAULT );
		myTfRegexIDReplace.setText( STR_REGEX_ID_REPLACEMENT_DEFAULT );
		myTfRegexLabelMatch.setText( STR_REGEX_MATCH_DEFAULT );
		myTfRegexLabelReplace.setText( STR_REGEX_LABEL_REPLACEMENT_DEFAULT );
	}

	/** @since 20050315 */
	public void suggestRegexes(){
		StandardNode standard = null;
		String bestMetaID = null, bestMetaLabel = null, newMetaID, newMetaLabel;
		boolean hopeID = true, hopeLabel = true;
		for( Iterator it = this.iterator(); it.hasNext(); ){
			standard = (StandardNode) it.next();
			if( hopeID ){
				newMetaID = meta( safe( standard.getID() ) );
				bestMetaID = bestMeta( bestMetaID, newMetaID );
				hopeID = ( bestMetaID != null );
				//System.out.println( "metaID: \"" + newMetaID + "\", best: \"" + bestMetaID + "\"" );
			}
			if( hopeLabel ){
				newMetaLabel = meta( safe( standard.getLabel() ) );
				bestMetaLabel = bestMeta( bestMetaLabel, newMetaLabel );
				hopeLabel = ( bestMetaLabel != null );
				//System.out.println( "metaLabel: \"" + newMetaLabel + "\", best: \"" + bestMetaLabel + "\"" );
			}
		}

		String regexID = null, replacementID = null, regexLabel = null, replacementLabel = null;
		if( hopeID ){
			regexID = metaToRegex( bestMetaID );
			replacementID = metaToReplacement( bestMetaID ) + STR_APPEND;
		}
		else{
			regexID = STR_REGEX_MATCH_DEFAULT;
			replacementID = STR_REGEX_ID_REPLACEMENT_DEFAULT;
		}

		if( hopeLabel ){
			regexLabel = metaToRegex( bestMetaLabel );
			replacementLabel = metaToReplacement( bestMetaLabel );
		}
		else{
			regexLabel = STR_REGEX_MATCH_DEFAULT;
			replacementLabel = STR_REGEX_LABEL_REPLACEMENT_DEFAULT;
		}

		myTfRegexIDMatch      .setText( regexID );
		myTfRegexIDReplace    .setText( replacementID );
		myTfRegexLabelMatch   .setText( regexLabel );
		myTfRegexLabelReplace .setText( replacementLabel );
	}

	public static final String
	  STR_REPLACEMENT_ALPHANUM    =    "\\\\w",//"[a-zA-Z_]";//[\\w&&[^\\d]];//"\\p{Alpha}";
	  STR_REPL_NONDIGIT           =    "[\\\\w&&[^\\\\d]]",
	  STR_REGEX_NONDIGIT          =    "[\\w&&[^\\d]]";

	/** @since 031505 */
	private static String metaToRegex( String meta ){
		String regex = meta;
		regex = quoteNonMeta( meta );
		regex = Pattern.compile( "a+0" ).matcher( regex ).replaceAll( "("+STR_REPL_NONDIGIT+"+)0" );
		regex = Pattern.compile( "0a+" ).matcher( regex ).replaceAll( "0("+STR_REPL_NONDIGIT+"+)" );
		regex = Pattern.compile(  "a+" ).matcher( regex ).replaceAll( "("+STR_REPLACEMENT_ALPHANUM+"+)" );
		regex = Pattern.compile( "x+0" ).matcher( regex ).replaceAll( "("+STR_REPL_NONDIGIT+"["+STR_REPL_NONDIGIT+"\\\\s]*"+STR_REPL_NONDIGIT+")0" );
		regex = Pattern.compile( "0x+" ).matcher( regex ).replaceAll( "0("+STR_REPL_NONDIGIT+"["+STR_REPL_NONDIGIT+"\\\\s]*"+STR_REPL_NONDIGIT+")" );
		regex = Pattern.compile(  "x+" ).matcher( regex ).replaceAll( "("+STR_REPLACEMENT_ALPHANUM+"["+STR_REPLACEMENT_ALPHANUM+"\\\\s]*"+STR_REPLACEMENT_ALPHANUM+")" );
		regex = Pattern.compile(  " +" ).matcher( regex ).replaceAll( "(\\\\s*)" );
		regex = Pattern.compile(  "0+" ).matcher( regex ).replaceAll( "(\\\\d+)" );
		return regex;
	}

	/** @since 031505 */
	private static String quoteNonMeta( String meta ){
		Matcher matcher = Pattern.compile( "[^ 0ax]+" ).matcher( meta );
		StringBuffer buffer = new StringBuffer( meta.length()*2 );
		while( matcher.find() ) matcher.appendReplacement( buffer, "\\\\Q" + matcher.group() + "\\\\E" );
		matcher.appendTail( buffer );
		return buffer.toString();
	}

	/** @since 031505 */
	private static String metaToReplacement( String meta ){
		Matcher matcher = Pattern.compile( "( +)|(0+)|(a+)|(x+)" ).matcher( meta );
		StringBuffer buffer = new StringBuffer( meta.length()*2 );
		int group = 1;
		while( matcher.find() ) matcher.appendReplacement( buffer, "\\\\" + Integer.toString( group++ ) );
		matcher.appendTail( buffer );
		return buffer.toString();
	}

	/** @since 031505 */
	private static String meta( String original ){
		String meta = original;
		meta = Pattern.compile( "\\d+" ).matcher( meta ).replaceAll( "0" );
		meta = Pattern.compile( STR_REGEX_NONDIGIT+"+" ).matcher( meta ).replaceAll( "a" );
		meta = Pattern.compile( "\\s+" ).matcher( meta ).replaceAll( " " );
		return meta.trim();
	}

	/** @since 031505 */
	private static String bestMeta( String meta1, String meta2 ){
		if( (meta1 == null) && (meta2 != null) ) return meta2;
		if( (meta2 == null) && (meta1 != null) ) return meta1;
		if( meta1.equals( meta2 ) ) return meta1;

		String nodigits1 = sacrificeExplicitDigits( meta1 );
		String nodigits2 = sacrificeExplicitDigits( meta2 );
		if( nodigits1.equals( nodigits2 ) ) return nodigits1;

		String sacrificed1 = sacrificeExplicitWords( meta1 );
		String sacrificed2 = sacrificeExplicitWords( meta2 );
		if( sacrificed1.equals( sacrificed2 ) ) return sacrificed1;

		sacrificed1 = sacrificeExplicitDigits( sacrificed1 );
		sacrificed2 = sacrificeExplicitDigits( sacrificed2 );
		if( sacrificed1.equals( sacrificed2 ) ) return sacrificed1;

		return lastDitchSacrifice( sacrificed1, sacrificed2 );
	}

	/** @since 031605 */
	private static String sacrificeExplicitDigits( String meta ){
		String sacrificed = meta;
		sacrificed = Pattern.compile( "0" ).matcher( sacrificed ).replaceAll( "a" );
		sacrificed = Pattern.compile( "aaa*" ).matcher( sacrificed ).replaceAll( "a" );
		return sacrificed;
	}

	/** @since 031505 */
	private static String sacrificeExplicitWords( String meta ){
		String reduced = meta;
		reduced = Pattern.compile( "a [a ]*a" ).matcher( reduced ).replaceAll( "x" );
		return reduced;
	}

	/** @since 031505 */
	private static String lastDitchSacrifice( String sacrificed1, String sacrificed2 ){
		int len1 = sacrificed1.length();
		int len2 = sacrificed2.length();
		StringBuffer buffer = new StringBuffer( Math.max( len1, len2 ) );
		int pos1 = 0, pos2 = 0;
		char char1, char2;
		while( (pos1 < len1) && (pos2 < len2) ){
			char1 = sacrificed1.charAt( pos1 );
			char2 = sacrificed2.charAt( pos2 );
			if( char1 == char2 ){
				buffer.append( char1 );
				++pos1;
				++pos2;
			}
			else if( char1 == ' ' ){
				buffer.append( ' ' );
				++pos1;
			}
			else if( char2 == ' ' ){
				buffer.append( ' ' );
				++pos2;
			}
			else if( (char1 == 'a') && (char2 == 'x') ){
				buffer.append( 'x' );
				++pos1;
				++pos2;
			}
			else if( (char2 == 'a') && (char1 == 'x') ){
				buffer.append( 'x' );
				++pos1;
				++pos2;
			}
			else return null;
		}
		if( (pos1 == len1) && (pos2 == len2) ) return buffer.toString();
		else return null;
	}

	protected void makePromptPastePanel()
	{
		JPanel panelDepth = new JPanel();
		BoxLayout bl = new BoxLayout( panelDepth, BoxLayout.Y_AXIS );
		panelDepth.setLayout( bl );
		myRadioWithEdges = new JCheckBox( "Paste edges" );
		myRadioWithEdges.addActionListener( this );
		myRadioWithCPTs = new JCheckBox( "Paste probabilities" );
		myRadioWithCPTs.setEnabled( false );
		panelDepth.add( myRadioWithEdges );
		panelDepth.add( myRadioWithCPTs );
		panelDepth.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Depth" ) );

		JPanel panelTelescope = new JPanel( new GridBagLayout() );
		GridBagConstraints cTele = new GridBagConstraints();
		cTele.anchor = GridBagConstraints.NORTHWEST;
		myCbTelescope = new JCheckBox( "Telescope:" );
		myCbTelescope.setToolTipText( STR_TOOLTIP_TELESCOPE );
		panelTelescope.add( myCbTelescope, cTele );
		panelTelescope.add( Box.createHorizontalStrut(8), cTele );
		panelTelescope.add( new JLabel( "<html><font color='#009900'><b>activate</b></font> these special paste options for normal paste<br>("+STR_TOOLTIP_TELESCOPE+")" ), cTele );
		cTele.gridwidth = GridBagConstraints.REMAINDER;
		cTele.weightx = 1;
		panelTelescope.add( Box.createGlue(), cTele );
		panelTelescope.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Persistence" ) );

		myPanelRegexes = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelRegexes.add( myCbUseRegex = new JCheckBox( "use regular expressions" ), c );
		myCbUseRegex.addActionListener( (ActionListener)this );
		myCbUseRegex.setToolTipText( STR_TOOLTIP_ONOFF );

		c.gridwidth = 1;
		myPanelRegexes.add( myLabelIds = new JLabel( "Identifiers:" ), c );
		myLabelIds.setToolTipText( STR_TOOLTIP_IDS );
		myPanelRegexes.add( Box.createHorizontalStrut(8), c );
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelRegexes.add( myTfRegexIDMatch = new JTextField(), c );
		myTfRegexIDMatch.setToolTipText( STR_TOOLTIP_MATCH_REGEX );
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		myPanelRegexes.add( myLabelPrepositionIds = new JLabel( "->" ), c );
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelRegexes.add( myTfRegexIDReplace = new JTextField(), c );
		myTfRegexIDReplace.setToolTipText( STR_TOOLTIP_REPLACEMENT );
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelRegexes.add( Box.createHorizontalStrut(8), c );

		c.gridwidth = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		myPanelRegexes.add( myLabelLabels = new JLabel( "Labels:" ), c );
		myLabelLabels.setToolTipText( STR_TOOLTIP_LABELS );
		myPanelRegexes.add( Box.createHorizontalStrut(8), c );
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelRegexes.add( myTfRegexLabelMatch = new JTextField(), c );
		myTfRegexLabelMatch.setToolTipText( STR_TOOLTIP_MATCH_REGEX );
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		myPanelRegexes.add( myLabelPrepositionLabels = new JLabel( "->" ), c );
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelRegexes.add( myTfRegexLabelReplace = new JTextField(), c );
		myTfRegexLabelReplace.setToolTipText( STR_TOOLTIP_REPLACEMENT );
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelRegexes.add( Box.createHorizontalStrut(8), c );

		JPanel panelRegexButtons = new JPanel();
		panelRegexButtons.add( myLabelHelp = new HyperLabel( "Java Regular Expression Syntax", getOnclick() ) );
		myLabelHelp.setToolTipText( STR_TOOLTIP_HELP );
		panelRegexButtons.add( myButtonPreview = makeButton( "preview" ) );
		myButtonPreview.setToolTipText( STR_TOOLTIP_PREVIEW );
		panelRegexButtons.add( myButtonSuggest = makeButton( "suggest" ) );
		myButtonSuggest.setToolTipText( STR_TOOLTIP_SUGGEST );
		panelRegexButtons.add( myButtonReset = makeButton( "reset default expressions" ) );
		myButtonReset.setToolTipText( STR_TOOLTIP_RESET );

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelRegexes.add( panelRegexButtons, c );

		myPanelRegexes.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "New Identifier/Label Generation" ) );
		resetRegexes();
		myCbUseRegex.setSelected( false );
		setRegexesEnabled( false );

		myPromptPastePanel = new JPanel( new GridBagLayout() );
		c = new GridBagConstraints();

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		myPromptPastePanel.add( panelDepth, c );
		myPromptPastePanel.add( myPanelRegexes, c );
		myPromptPastePanel.add( panelTelescope, c );
	}

	/** @since 031505 */
	public JButton makeButton( String text ){
		JButton btn = new JButton( text );
		btn.setMargin( new Insets( 0,8,0,8 ) );
		Font fntButton = btn.getFont();
		btn.setFont( fntButton.deriveFont( (float)10 ) );
		btn.addActionListener( (ActionListener)this );
		return btn;
	}

	public static final String STR_URL_JAVA_REGEX = "http://java.sun.com/docs/books/tutorial/extra/regex/index.html";

	/** @since 031505 */
	private Runnable getOnclick(){
		if( myOnclick == null ){
			myOnclick = new Runnable(){
				public void run(){
					BrowserControl.displayURL( STR_URL_JAVA_REGEX );
				}
			};
		}
		return myOnclick;
	}

	public void actionPerformed( ActionEvent evt )
	{
		Object src = evt.getSource();
		if( src == myRadioWithEdges ){
			if( myRadioWithEdges.isSelected() ) myRadioWithCPTs.setEnabled( true );
			else{
				myRadioWithCPTs.setEnabled( false );
				myRadioWithCPTs.setSelected( false );
			}
		}
		else if( src == myButtonReset ) resetRegexes();
		else if( src == myButtonSuggest ) suggestRegexes();
		else if( src == myCbUseRegex ){
			setRegexesEnabled( myCbUseRegex.isSelected() );
		}
		else if( src == myButtonPreview ) doPreview();
	}

	private static Map cloneAll( Collection dVars, Copier copier, DisplayableBeliefNetwork copiedFrom, DisplayableBeliefNetwork copiedTo )
	{
		Map mapVariablesOldToNew = new HashMap( dVars.size() );
		DisplayableFiniteVariable oldDVar;
		FiniteVariable newFVar, newDVar;
		for( Iterator it = dVars.iterator(); it.hasNext(); )
		{
			oldDVar = (DisplayableFiniteVariable) it.next();
			if( copier == null ){
				newFVar = (FiniteVariable) oldDVar.clone();
				newDVar = new DisplayableFiniteVariableImpl( newFVar );
			}
			else newDVar = copier.copyFiniteVariable( oldDVar, copiedFrom, copiedTo );
			mapVariablesOldToNew.put( oldDVar, newDVar );
		}
		return mapVariablesOldToNew;
	}

	/** @since 20021030 */
	private static void massageMapByReplacingCPTShellVariables( Map mapVariablesOldToNew )
	{
		DisplayableFiniteVariable newDVar = null;
		CPTShell shell = null;
		for( Iterator it = mapVariablesOldToNew.values().iterator(); it.hasNext(); )
		{
			newDVar = (DisplayableFiniteVariable) it.next();
			for( Iterator itTypes = DSLNodeType.iterator(); itTypes.hasNext(); ){
				shell = newDVar.getCPTShell( (DSLNodeType) itTypes.next() );
				if( shell != null ){ shell.replaceVariables( mapVariablesOldToNew, false ); }
			}
		}
	}

	/** @since 031505 */
	private boolean rebaptize( BeliefNetwork bn, boolean withRegexes ){
		if( withRegexes && (myCbUseRegex != null) && myCbUseRegex.isSelected() ){ return rebaptizeRegex(); }
		else return rebaptizeAccumulate( bn );
	}

	/** @since 031505 */
	private boolean rebaptizeRegex(){
		Replacements replacements = null;
		String error = null;
		try{
			replacements = previewReplacements();
		}catch( Throwable throwable ){
			error = throwable.getMessage();
			System.err.println( throwable );
		}
		Object message = null;
		int type = JOptionPane.PLAIN_MESSAGE;
		if( replacements == null ){
			if( error == null ) error = "unknown error";
			message = error;
			type = JOptionPane.ERROR_MESSAGE;
		}
		else if( !replacements.isValid() ) message = replacements.getPreview();

		if( message != null ){
			JOptionPane.showMessageDialog( myPanelRegexes, message, "Error Generating IDs/Labels from Regexes", type );
			return false;
		}
		else{
			replacements.rebaptizeAll();
			return true;
		}
	}

	public static final String STR_POSTFIX_PASTED_ID = "_paste";
	protected int INT_PASTE_ID_GUESS = (int)0;

	/** @since 102802 */
	protected boolean rebaptizeAccumulate( DisplayableFiniteVariable dVar, BeliefNetwork bn )
	{
		String postfixedID = dVar.getID();
		int foundind = postfixedID.indexOf( STR_POSTFIX_PASTED_ID );
		if( foundind == (int)-1 ) postfixedID += STR_POSTFIX_PASTED_ID;
		else postfixedID = postfixedID.substring( 0, foundind + STR_POSTFIX_PASTED_ID.length() );
		int count = INT_PASTE_ID_GUESS;
		String newID = null;
		do{
			newID = postfixedID + String.valueOf( count );
		}while( bn.forID( newID ) != null );
		dVar.setID( newID );
		return true;
	}

	/** @since 102802 */
	protected boolean rebaptizeAccumulate( BeliefNetwork bn )
	{
		//System.out.println( "NetworkClipBoardImpl.rebaptize()" );

		int numVariables = size();
		DisplayableFiniteVariable[]	dVars	= new DisplayableFiniteVariable[numVariables];
		String[]			pfIDs	= new String[numVariables];
		String[]			newIDs	= new String[numVariables];
		DisplayableFiniteVariable newDVar = null;
		Iterator it = iterator();
		String postfixedID = null;
		int foundind = (int)-1;
		//String newID = null;

		for( int i=0; i<numVariables && it.hasNext(); i++ )
		{
			newDVar = dVars[i] = (DisplayableFiniteVariable) it.next();
			postfixedID = newDVar.getID();
			foundind = postfixedID.indexOf( STR_POSTFIX_PASTED_ID );
			if( foundind != (int)-1 ) postfixedID = postfixedID.substring( 0, foundind );
			pfIDs[i] = postfixedID;
		}

		int hexPostfixCounter = (int)0;
		String modifiedID;

		for( int i=0; i<numVariables; i++ )
		{
			postfixedID = pfIDs[i];
			modifiedID = postfixedID;
			while( findInHead( modifiedID, pfIDs, i ) ) modifiedID = postfixedID + Integer.toHexString( hexPostfixCounter++ );
			pfIDs[i] = modifiedID;
		}

		String strNumericalIDGuess = String.valueOf( INT_PASTE_ID_GUESS );

		for( int i=0; i<numVariables; i++ )
		{
			modifiedID = pfIDs[i];
			if( bn.forID( modifiedID ) != null )
			{
				pfIDs[i] = pfIDs[i] + STR_POSTFIX_PASTED_ID;
				modifiedID = pfIDs[i] + strNumericalIDGuess;
			}
			newIDs[i] = modifiedID;
		}

		boolean allNewIDsUnique = false;

		do{
			allNewIDsUnique = true;
			for( int i=0; i<numVariables; i++ )
			{
				//System.out.print( "\ttesting \"" +newIDs[i]+ "\"" );
				if( bn.forID( newIDs[i] ) != null )
				{
					//System.out.println( " throwing out!" );
					allNewIDsUnique = false;
					++INT_PASTE_ID_GUESS;
					strNumericalIDGuess = String.valueOf( INT_PASTE_ID_GUESS );
					for( int j=0; j<numVariables; j++ ) newIDs[j] = pfIDs[j] + strNumericalIDGuess;
					break;
				}
				//else //System.out.println();
			}
		}while( !allNewIDsUnique );

		for( int i=0; i<numVariables; i++ ) dVars[i].setID( newIDs[i] );

		++INT_PASTE_ID_GUESS;
		return true;
	}

	/** @since 20021121 */
	protected boolean findInHead( String id, String[] array, int index )
	{
		for( int i=0; i<index; i++ )
		{
			if( array[i].equals( id ) ) return true;
		}

		return false;
	}

	protected void refresh( Map mapVariablesOldToNew )
	{
		DisplayableFiniteVariable oldDVar = null;
		DisplayableFiniteVariable newDVar = null;
		for( Iterator it = mapVariablesOldToNew.keySet().iterator(); it.hasNext(); )
		{
			oldDVar = (DisplayableFiniteVariable) it.next();
			newDVar = (DisplayableFiniteVariable) mapVariablesOldToNew.get( oldDVar );
			replaceVertex( oldDVar, newDVar );
		}
	}

	public synchronized void resetVirtualCenter( Point newVirtualCenter )
	{
		//System.out.println( "NetworkClipBoardImpl.resetCenter()" );

		Point oldVirtualCenter = new Point();
		getVirtualCenter( oldVirtualCenter );
		int deltaX = newVirtualCenter.x - oldVirtualCenter.x;
		int deltaY = newVirtualCenter.y - oldVirtualCenter.y;

		Point oldLocation = new Point();
		DisplayableFiniteVariable dVar = null;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable) it.next();
			dVar.getLocation( oldLocation );
			//System.out.print( "recentering " + dVar + " at " + oldLocation + " to " );
			oldLocation.x += deltaX;
			oldLocation.y += deltaY;
			//System.out.println( oldLocation );
			dVar.setLocation( oldLocation );
		}
	}

	public synchronized Point getVirtualCenter( Point p )
	{
		if( p == null ) p = new Point();
		if( myVirtualCenter == null ) myVirtualCenter = calculateVirtualCenter();
		p.setLocation( myVirtualCenter );
		return p;
	}

	protected Point calculateVirtualCenter()
	{
		DisplayableFiniteVariable dVar = null;
		Point p = new Point();
		Rectangle rect = null;

		Iterator it = iterator();
		if( it.hasNext() )
		{
			dVar = (DisplayableFiniteVariable) it.next();
			dVar.getLocation( p );
			rect = new Rectangle( p );
			while( it.hasNext() )
			{
				dVar = (DisplayableFiniteVariable) it.next();
				dVar.getLocation( p );
				rect.add( p );
			}
		}

		if( rect != null )
		{
			p.x = rect.x + rect.width/2;
			p.y = rect.y + rect.height/2;
		}

		return p;
	}

	//DirectedGraph
	public Object clone()
	{
		return null;
	}
	public List topologicalOrder()
	{
		return myGraph.topologicalOrder();
	}
	public void replaceVertex( Object oldVertex, Object newVertex )
	{
		myGraph.replaceVertex( oldVertex,  newVertex );
	}
	public void replaceVertices( Map verticesOldToNew, NodeLinearTask task )
	{
		myGraph.replaceVertices( verticesOldToNew, task );
	}
	public boolean maintainsAcyclicity( Object vertex1, Object vertex2 )
	{
		return myGraph.maintainsAcyclicity( vertex1, vertex2 );
	}
	public Set vertices()
	{
		return myGraph.vertices();
	}
	public Set inComing(Object vertex)
	{
		return myGraph.inComing( vertex);
	}
	public Set outGoing(Object vertex)
	{
		return myGraph.outGoing( vertex);
	}
	public int degree(Object vertex)
	{
		return myGraph.degree( vertex);
	}
	public int inDegree(Object vertex)
	{
		return myGraph.inDegree( vertex);
	}
	public int outDegree(Object vertex)
	{
		return myGraph.outDegree( vertex);
	}
	public boolean containsEdge(Object vertex1, Object vertex2)
	{
		return myGraph.containsEdge( vertex1,  vertex2);
	}
	public boolean isAcyclic()
	{
		return myGraph.isAcyclic();
	}
	public boolean isWeaklyConnected()
	{
		return myGraph.isWeaklyConnected();
	}
	public boolean isWeaklyConnected(Object vertex1, Object vertex2)
	{
		return myGraph.isWeaklyConnected( vertex1,  vertex2);
	}
	public boolean hasPath(Object vertex1, Object vertex2)
	{
		return myGraph.hasPath( vertex1,  vertex2);
	}
	public boolean isSinglyConnected()
	{
		return myGraph.isSinglyConnected();
	}
	public boolean addVertex(Object vertex)
	{
		return myGraph.addVertex( vertex);
	}
	public boolean removeVertex(Object vertex)
	{
		return myGraph.removeVertex( vertex);
	}
	public boolean addEdge(Object vertex1, Object vertex2)
	{
		return myGraph.addEdge( vertex1,  vertex2);
	}
	public boolean removeEdge(Object vertex1, Object vertex2)
	{
		return myGraph.removeEdge( vertex1,  vertex2);
	}

	//Collection
	public boolean add(Object o)
	{
		return myGraph.add( o);
	}
	public boolean addAll(Collection c)
	{
		return myGraph.addAll( c);
	}
	public void clear()
	{
		myGraph.clear();
	}
	public boolean contains(Object o)
	{
		return myGraph.contains( o);
	}
	public boolean containsAll(Collection c)
	{
		return myGraph.containsAll( c);
	}
	public boolean equals(Object o)
	{
		return myGraph.equals( o);
	}
	public int hashCode()
	{
		return myGraph.hashCode();
	}
	public boolean isEmpty()
	{
		return myGraph.isEmpty();
	}
	public Iterator iterator()
	{
		return myGraph.iterator();
	}
	public boolean remove(Object o)
	{
		return myGraph.remove( o);
	}
	public boolean removeAll(Collection c)
	{
		return myGraph.removeAll( c);
	}
	public boolean retainAll(Collection c)
	{
		return myGraph.retainAll( c);
	}
	public int size()
	{
		return myGraph.size();
	}
	public int numEdges()
	{
		return myGraph.numEdges();
	}
	public Object[] toArray()
	{
		return myGraph.toArray();
	}
	public Object[] toArray(Object[] a)
	{
		return myGraph.toArray( a);
	}


	private     DisplayableBeliefNetwork   myNetworkCopiedFrom, myNetworkPastedTo;
	protected   DirectedGraph              myGraph;
	protected   Point                      myVirtualCenter;
}
