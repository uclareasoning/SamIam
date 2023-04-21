package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.tree.EvidenceTree;

import java.text.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**	Removed from EvidenceDialog.java

	@author david allen
	@author keith cascio
	@since  20020718 */
public class EvidenceLabel /*extends JLabel*/ implements PreferenceListener
{
	public static final boolean  FLAG_SHOW_EVIDENCE_ICON = true;

	protected   JPanel                       panel;
	protected   JLabel                       lblIcon, lblLeft, lblRight;
	protected   EvidenceIcon                 myEvidenceIcon;
	protected   int[]                        evidenceIndex;
	protected   DisplayableFiniteVariable    dspVar;
	public      final String                 strLabel;
	protected   int                          myMinimumFractionDigits = 1, myCardinality = 1, myIndex = 0;
	protected   FormatManager                myFormatter;
	private     double[]                     lastDrawnPrs = new double[]{ 0.0 };
	private     double                       lastDrawnPr  = 0.0;
	private     StringBuffer                 buffer                  = new StringBuffer( 0x40 );

	public static final String      STR_NAN              =        "NaN",
	                                STR_ERROR            =      "error",
	                                STR_CERTAIN          =    "certain",
	                                STR_IMPOSSIBLE       = "impossible";
	public static final float       FLOAT_BASE_FONT_SIZE =    (float)10;

	/** @since 20080226 */
	public    static   final   Color
	  COLOR_ERROR = Color.red;
	private   static           Color
	  COLOR_PREF;

	/** @since 20030703 */
	public static final PercentManager  PERCENT_MANAGER  = new PercentManager();
	public static final    OddsManager     ODDS_MANAGER  = new    OddsManager();
	public static final LogOddsManager LOG_ODDS_MANAGER  = new LogOddsManager();
	public static final FormatManager[]   ARRAY_MANAGERS = new  FormatManager[]{ PERCENT_MANAGER, ODDS_MANAGER, LOG_ODDS_MANAGER };

	/** @since 20030703 */
	public static FormatManager forString( String toParse )
	{
		for( int i=0; i<ARRAY_MANAGERS.length; i++ )
		{
			if( ARRAY_MANAGERS[i].getDisplayName().equals( toParse ) ) return ARRAY_MANAGERS[i];
		}
		return null;
	}

	/** @since 20080226 */
	public EvidenceLabel addMouseListener( MouseListener mouselistener ){
		labelL().addMouseListener( mouselistener );
		return this;
	}

	/** @since 20080226 */
	public int rotate(){
		if( myCardinality == 1 ){ return -99; }
		if(     myIndex < 0 ){ myIndex = 0; }
		else{
			++  myIndex;
			if( myIndex >= myCardinality ){
				myIndex = (myCardinality > 1) ? -1 : 0;
			}
		}
		lastDrawnPr = (myIndex < 0) ? lastDrawnPrs[0] - lastDrawnPrs[1] : lastDrawnPrs[ myIndex ];
		redrawEvidence();
		return myIndex;
	}

	/** @author keith cascio
		@since 20030703 */
	public interface FormatManager
	{
		public StringBuffer     format(           StringBuffer buff, double    pr, boolean observed );
		public StringBuffer     format(           StringBuffer buff, Table[] tbls, int[]   index, EvidenceIcon icon, EvidenceLabel label, boolean observed );
		public StringBuffer     format(           StringBuffer buff, double  odds, double normal, EvidenceIcon icon, EvidenceLabel label, double pr );
		public StringBuffer     format(           StringBuffer buff, double[] prs, int cardinality, double value, EvidenceIcon icon, boolean observed );
		public StringBuffer     formatSampleMode( StringBuffer buff,                              EvidenceIcon icon );
		public DecimalFormat getFormat();
		public String        getDisplayName();

		public static final FieldPosition FP = new FieldPosition( NumberFormat.INTEGER_FIELD );
	}

	/** @since 20080106 */
	public abstract static class AbstractManager implements FormatManager{
		public StringBuffer format( StringBuffer buff, Table[] tbls, int[] index, EvidenceIcon icon, EvidenceLabel label, boolean observed )
		{
			double[]        prs  = null;
			int     cardinality  = 0;
			if( tbls != null ){
				for( ; (cardinality < tbls.length) && (tbls[cardinality] != null); cardinality++ ){}
				prs = ((label == null) || (label.lastDrawnPrs == null) || (label.lastDrawnPrs.length < cardinality)) ? new double[cardinality] : label.lastDrawnPrs;
				Arrays.fill( prs, 0.99 );
				for( int i=0; i<cardinality; i++ ){ prs[i] = tbls[i].value( index ); }
			}else{
				prs = ((label == null) || (label.lastDrawnPrs == null)) ? new double[]{ 0.99 } : label.lastDrawnPrs;
			}

			int     selected  = label == null ? 0 : (cardinality > 1 ? label.myIndex : 1);
			double     value  = 0.99;
			if(     selected  < 0 ){
				value = (cardinality > 1) ? Math.abs( prs[1] - prs[0] ) : 0.0;
			}
			else if( selected < cardinality ){
				value = prs[ selected ];
			}
			else{
				value = prs[ selected = 0 ];
			}
			StringBuffer ret  = tbls  == null ? buff.append( STR_ERROR ) : format( buff, value, observed );
			if(         icon != null ){ icon.setValues( prs, cardinality ); }
			if(        label != null ){
				label.lastDrawnPrs  = prs;
				label.myCardinality = cardinality;
				label.myIndex       = selected;
				label.lastDrawnPr   = value;
			}

			return ret;
		}

		public StringBuffer format( StringBuffer buff, double odds, double normal, EvidenceIcon icon, EvidenceLabel label, double pr )
		{
			throw new UnsupportedOperationException();
		}

		public StringBuffer format( StringBuffer buff, double[] prs, int cardinality, double value, EvidenceIcon icon, boolean observed )
		{
			if( icon != null ){      icon.setValues( prs, cardinality ); }
			return                     format( buff, value, observed );
		}

		public StringBuffer reset( StringBuffer buff ){
			buff.setLength( 0 );
			return buff;
		}
	}

	public static class PercentManager extends AbstractManager implements FormatManager
	{
		public StringBuffer format( StringBuffer buff, double pr, boolean observed )
		{
			reset( buff );
			if( observed ){
				if(                     pr == 0.0 ){ return buff.append(    "0%" ); }//"\u2007\u20070%" ); }
				else  if(               pr == 1.0 ){ return buff.append(  "100%" ); }
			}
			if( Double.isNaN( pr )      ){ return buff.append( STR_NAN ); }
			else{                          return getFormat().format( pr, buff, FP ); }
		}

		public StringBuffer formatSampleMode( StringBuffer buff, EvidenceIcon icon )
		{
			return format( buff, VALUES_SAMPLEMODE, 1, 0, icon, false );
		}

		public DecimalFormat getFormat()
		{
			if(    myFormat == null ){ myFormat = new DecimalFormat( "###%"); }
			return myFormat;
		}

		public String getDisplayName()
		{
			return "percentage";
		}

		protected DecimalFormat myFormat;

		public static final double[] VALUES_SAMPLEMODE = new double[]{ 0.5 };
	}

	public static class OddsManager extends AbstractManager implements FormatManager
	{
		public StringBuffer format( StringBuffer buff, double odds, double normal, EvidenceIcon icon, EvidenceLabel label, double pr )
		{
			reset( buff );
			if( label != null ){ label.lastDrawnPrs = new double[]{ pr }; }
			String strOdds;
			if( Double.isNaN( odds ) ){ strOdds = STR_NAN; }
			else if( odds == Double.POSITIVE_INFINITY )
			{
				strOdds = STR_CERTAIN;
				if( icon != null ){ icon.setValue( (double)1 ); }
			}
			else
			{
				if( icon != null ){ icon.setValue( odds/normal ); }
				return getFormat().format( odds, buff, FP );
			}
			return buff.append( strOdds );
		}

		public StringBuffer format( StringBuffer buff, double pr, boolean observed )
		{
			reset( buff );
			String strOdds;
			if(   Double.isNaN( pr ) ){ strOdds = STR_NAN;     }
			else if( pr == (double)1 ){ strOdds = STR_CERTAIN; }
			else{ return getFormat().format( pr / ((double)1 - pr), buff, FP ); }
			return buff.append( strOdds );
		}

		public StringBuffer formatSampleMode( StringBuffer buff, EvidenceIcon icon )
		{
			if( icon != null ){ icon.setValue( (double)0.5 ); }
			return format( buff, (double)1, false );
		}

		public DecimalFormat getFormat()
		{
			if( myFormat == null ){ myFormat = new DecimalFormat( "###"); }
			return myFormat;
		}

		public String getDisplayName()
		{
			return "odds";
		}

		protected DecimalFormat myFormat;
	}

	public static class LogOddsManager extends AbstractManager implements FormatManager
	{
		public StringBuffer format( StringBuffer buff, double odds, double normal, EvidenceIcon icon, EvidenceLabel label, double pr )
		{
			reset( buff );
			if( label != null ){ label.lastDrawnPrs = new double[]{ pr }; }
			String strOdds;
			if( Double.isNaN( odds ) ){ strOdds = STR_NAN; }
			else if( odds == Double.POSITIVE_INFINITY )
			{
				strOdds = STR_CERTAIN;
				if( icon != null ){ icon.setValue( (double)1 ); }
			}
			else if( odds == (double)0 )
			{
				strOdds = STR_IMPOSSIBLE;
				if( icon != null ){ icon.setValue( (double)0 ); }
			}
			else
			{
				if( icon != null ){ icon.setValue( odds/normal ); }
				return getFormat().format( Math.log( odds ), buff, FP );
			}
			return buff.append( strOdds );
		}

		public StringBuffer format( StringBuffer buff, double pr, boolean observed )
		{
			reset( buff );
			String strLogOdds;
			if(   Double.isNaN( pr ) ){ strLogOdds = STR_NAN; }
			else if( pr == (double)1 ){ strLogOdds = STR_CERTAIN; }
			else if( pr == (double)0 ){ strLogOdds = STR_IMPOSSIBLE; }
			else{ return getFormat().format( Math.log( pr / ((double)1 - pr) ), buff, FP ); }
			return buff.append( strLogOdds );
		}

		public StringBuffer formatSampleMode( StringBuffer buff, EvidenceIcon icon )
		{
			if( icon != null ){ icon.setValue( (double)0.5 ); }
			return format( buff, (double)0, false );
		}

		public DecimalFormat getFormat()
		{
			if( myFormat == null ){ myFormat = new DecimalFormat( "###"); }
			return myFormat;
		}

		public String getDisplayName()
		{
			return "log odds";
		}

		protected DecimalFormat myFormat;
	}

	/** @since 20030307 */
	public void recalculateActual( CoordinateTransformer xformer )
	{
		if( FLAG_SHOW_EVIDENCE_ICON ){ myEvidenceIcon.recalculateActual( xformer ); }
		recalculateActual( labelL(), xformer ).recalculateActual( labelR(), xformer );
	}

	/** @since 20080106 */
	private EvidenceLabel recalculateActual( JComponent comp, CoordinateTransformer xformer ){
		comp.setFont( comp.getFont().deriveFont( xformer == null ? FLOAT_BASE_FONT_SIZE : xformer.virtualToActual( FLOAT_BASE_FONT_SIZE ) ) );
		return this;
	}

	/** @since 20030307 */
	public void setDoZoom( boolean flag )
	{
		if( FLAG_SHOW_EVIDENCE_ICON ){ myEvidenceIcon.setDoZoom( flag ); }
		if( !flag ){ recalculateActual( labelL(), null ).recalculateActual( labelR(), null ); }
	}

	/** @since 20020826 */
	public Object getValue()
	{
		return dspVar.instance( evidenceIndex[0] );
	}

	public EvidenceIcon getEvidenceIcon() { return myEvidenceIcon; }

	/** @since 20020826 */
	protected void set( String text, Icon icon, int alignment )
	{
		if( FLAG_SHOW_EVIDENCE_ICON ){ labelI().setIcon( icon ); }
		labelR().setText( text );
		labelR().setHorizontalAlignment( alignment );
	}

	/** @since 20080106 */
	public JLabel labelI(){
		if( this.lblIcon == null ){ this.lblIcon = new JLabel(); }
		return this.lblIcon;
	}

	/** @since 20080106 */
	public JLabel labelL(){
		if( this.lblLeft == null ){
			this.lblLeft = new JLabel( "", SwingConstants.RIGHT );
			this.lblLeft.setBorder( BorderFactory.createEmptyBorder(0,4,0,0) );
		}
		return this.lblLeft;
	}

	/** @since 20080106 */
	public JLabel labelR(){
		if( this.lblRight == null ){
			this.lblRight = new JLabel();
			this.lblRight.setBorder( BorderFactory.createEmptyBorder(0,4,0,2) );
		}
		return this.lblRight;
	}

	/** @since 20080106 */
	public JPanel asComponent(){
		if( panel == null ){ this.add( panel = new JPanel( new GridBagLayout() ), new GridBagConstraints() ); }
		return this.panel;
	}

	/** @since 20080106 */
	public EvidenceLabel add( Container panel, GridBagConstraints c ){
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		c.gridwidth = 1;
		panel.add( this.labelI(), c );
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		panel.add( this.labelL(), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		panel.add( this.labelR(), c );
	  //panel.add( Box.createVerticalStrut(2), c );
		return this;
	}

	public EvidenceLabel( SamiamPreferences monitorPrefs, DisplayableFiniteVariable dspV, int evidIndx, Table conditional, int observedIndex )
	{
		evidenceIndex = new int[]{ evidIndx };
		dspVar        = dspV;
		if( FLAG_SHOW_EVIDENCE_ICON ) myEvidenceIcon = new EvidenceIcon( monitorPrefs );
		//myMinimumFractionDigits = minimumFactionDigits;

		EvidenceLabel.validatePreferenceBundle( monitorPrefs );
		//this.myMonitorPrefs = monitorPrefs;

		if( !dspVar.isSampleMode() )
		{
			strLabel = dspV.instance(evidIndx).toString();
		}
		else if( evidIndx == 1)
		{
			if( FLAG_SHOW_EVIDENCE_ICON ) myEvidenceIcon.setManuallySetEvid( false );
			strLabel = "Automatic";
		}
		else
		{
			if( FLAG_SHOW_EVIDENCE_ICON ) myEvidenceIcon.setManuallySetEvid( true );
			strLabel = "Manual";
		}
		set( dspVar.isSampleMode() ? strLabel : ("- " + strLabel), myEvidenceIcon, SwingConstants.LEFT);
		if( conditional == null )
		{
			setPreferences();
			if( dspVar.isSampleMode() ) drawEvidence();
		}
		else setPreferences( conditional, observedIndex );
		init();
	  //setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );
	}

	/** @since 20030710 */
	public EvidenceLabel( SamiamPreferences monitorPrefs, DisplayableFiniteVariable dspV, int evidIndx )
	{
		this( monitorPrefs, dspV, evidIndx, null, -1 );
	}

	private static Font FONT;
	/** @since 20080106 */
	public  static Font font(){
		if( FONT == null ){
			try{
				FONT  = new Font( "Monospaced", Font.PLAIN, (int) FLOAT_BASE_FONT_SIZE );//"Courier New"
			}catch( Throwable thrown ){
				System.err.println( "warning: EvidenceLabel.font() caught " + thrown );
			}
			if( FONT == null ){ FONT = new JLabel().getFont(); }
		}
		return FONT;
	}

	/** @since 20020827 */
	protected void init(){
	  //setFont( font() );
		setFormatter();

		if( !dspVar.isSampleMode() )
		{
			labelI().addMouseListener( new MouseAdapter(){
				public void mouseClicked( MouseEvent e ){
					NetworkInternalFrame nif = dspVar.getNetworkInternalFrame();
					Object newValue = dspVar.instance( evidenceIndex[0] );//getValue();
					nif.evidenceRequest( dspVar, newValue, labelI() );
				}
			});
		}
	}

	/** @since 20020827 */
	protected void setFormatter()
	{
		if( myFormatter == null ) myFormatter = PERCENT_MANAGER;
		DecimalFormat format = myFormatter.getFormat();
		format.setMinimumFractionDigits( myMinimumFractionDigits );
		format.setMaximumFractionDigits( myMinimumFractionDigits );
		format.setMinimumIntegerDigits( (int)1 );
	}

	/** @since 20030710 */
	public FormatManager getFormatManager()
	{
		return myFormatter;
	}

	public void drawEvidence()
	{
		if( dspVar.isSampleMode() ) {
			setText( myFormatter.formatSampleMode( buffer, myEvidenceIcon ) );
		}
		else{
			System.err.println( "Warning: non-sample mode calls to EvidenceLabel.drawEvidence() deprecated." );
			InferenceEngine ie  = dspVar.getInferenceEngine();
			Table           tbl = (ie == null) ? null : ie.conditional( dspVar );
			drawEvidence( new Table[]{ tbl }, dspVar.getObservedIndex() );
		}
	}

	public void redrawEvidence()
	{
		setText( dspVar.isSampleMode() ? myFormatter.formatSampleMode( buffer,               myEvidenceIcon        ) :
		                                 myFormatter.format(           buffer, lastDrawnPrs, myCardinality, lastDrawnPr, myEvidenceIcon, observed() ) );
		chromatize();
	}

	/** @since 20080226 */
	private boolean observed(){
		return ( (dspVar != null) && (dspVar.getBeliefNetwork() != null) && (dspVar.getBeliefNetwork().getEvidenceController().getValue( dspVar ) != null) );
	}

	/** @since 20080226 */
	private Color chromatize(){
		Color effective = COLOR_PREF;
		try{
			if(      myIndex         < 0    ){ effective = COLOR_ERROR; }
			else if( (myEvidenceIcon != null) && (myCardinality > 1) ){
				Color[] colors = myEvidenceIcon.getAutoColors();
				effective = colors[ myIndex % colors.length ];
			}
			this.labelL().setForeground( effective );
		}catch( Throwable thrown ){
			System.err.println( "warning: EvidenceLabel.chromatize() caught " + thrown );
		}
		return effective;
	}

	public static final char CHAR_MONOSPACE = '\u2007';//'\u205f';//

	/** @since 20080106 */
	protected EvidenceLabel setText( StringBuffer buff ){
	  //if( buff.charAt( 0 ) == '0' ){ buff.setCharAt( 0, CHAR_MONOSPACE ); }
		labelL().setText( buff.toString() );
		try{
			if(      myIndex         < 0    ){
				buff.append( " - error" );
			}
			else if( (myCardinality > 1) ){
				buff.append( " - " );
				String description = null;
				if( dspVar != null ){
					InferenceEngine ie = dspVar.getInferenceEngine();
					if( ie != null ){
						String[] descriptions = ie.describeConditionals();
						if( (descriptions != null) && (descriptions.length > myIndex) ){
							buff.append( description = descriptions[ myIndex ] );
						}
					}
				}
				if( description == null ){ buff.append( myIndex ); }
			}
			else{
				buff.append( " - " ).append( myFormatter.getDisplayName() );
			}
			labelL().setToolTipText( buff.toString() );
		}catch( Throwable thrown ){
			System.err.println( "warning: EvidenceLabel.setText() caught " + thrown );
		}
		return this;
	}

	/** @since 20030318 */
	public void drawEvidence( Table[] tbls, int observedIndex )
	{
		if( FLAG_SHOW_EVIDENCE_ICON ) myEvidenceIcon.setManuallySetEvid( evidenceIndex[0] == observedIndex );
		setText( myFormatter.format( buffer, tbls, evidenceIndex, myEvidenceIcon, this, observedIndex >= 0 ) );
		chromatize();
	}

	/** @since 20030710 */
	public void drawOdds( double odds, double normal, int observedIndex, double pr )
	{
		if( dspVar.isSampleMode() ) {
			setText( myFormatter.formatSampleMode( buffer, myEvidenceIcon ) );
		}
		else
		{
			if( FLAG_SHOW_EVIDENCE_ICON ) myEvidenceIcon.setManuallySetEvid( evidenceIndex[0] == observedIndex );
			setText( myFormatter.format( buffer, odds, normal, myEvidenceIcon, this, pr ) );
		}
	}

	/** @since 20030710 */
	public void warnEvidence( int observedIndex )
	{
		//System.out.print( "EvidenceLabel.warnEvidence("+observedIndex+")" );
		if( FLAG_SHOW_EVIDENCE_ICON && ( evidenceIndex[0] == observedIndex || myEvidenceIcon.isObserved() ) )
		{
			//System.out.print( "...true" );
			myEvidenceIcon.setWarn( true );
			//repaint();
			labelI().paintImmediately( 0,0,myEvidenceIcon.getIconWidth(),myEvidenceIcon.getIconHeight() );
		}
		//System.out.println();
	}

	/** Allow options to change.*/
	public void changePackageOptions()
	{
		updatePreferences();
	}
	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values
	*/
	public void updatePreferences()
	{
		if( FLAG_SHOW_EVIDENCE_ICON ) myEvidenceIcon.updatePreferences();

		BUNDLE_OF_PREFERENCES.updatePreferences( EvidenceLabel.this );
	}
	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values
	*/
	public void previewPreferences()
	{
		//System.out.println( "EvidenceLabel.previewPreferences()" );
		if( FLAG_SHOW_EVIDENCE_ICON ) myEvidenceIcon.previewPreferences();

		BUNDLE_OF_PREFERENCES.previewPreferences( EvidenceLabel.this );
	}
	/**
		Call this method to force a PreferenceListener to
		reset itself
	*/
	public void setPreferences()
	{
		BUNDLE_OF_PREFERENCES.setPreferences( EvidenceLabel.this );
	}

	public void setPreferences( Table conditional, int observedIndex )
	{
		setPreferences();

		if( dspVar.isSampleMode() ){ drawEvidence(); }
		else{ drawEvidence( new Table[]{ conditional }, observedIndex ); }
	}

	private static       TargetedBundle BUNDLE_OF_PREFERENCES;
	public  static final String         STR_KEY_PREFERENCE_BUNDLE = EvidenceLabel.class.getName();

	public static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ) return BUNDLE_OF_PREFERENCES.validate( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.evidDlgTextClr, SamiamPreferences.evidDlgMinimumFractionDigits, SamiamPreferences.evidDlgView };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.evidencelabel = (EvidenceLabel) me;
			}

			public void setPreference( int index, Object value ){
				boolean redraw = false;
				switch( index ){
					case 0:
						evidencelabel.labelL().setForeground( COLOR_PREF = (Color) value );
						evidencelabel.labelR().setForeground( (Color) value );
						break;
					case 1:
						evidencelabel.myMinimumFractionDigits = ((Integer) value).intValue();
						redraw = true;
						break;
					case 2:
						FormatManager manager = (FormatManager) value;
						if( evidencelabel.myFormatter != manager ){
							evidencelabel.myFormatter = manager;
							redraw = true;
						}
						break;
					default:
						throw new IllegalArgumentException();
				}
				if( redraw ){
					evidencelabel.setFormatter();
					evidencelabel.redrawEvidence();
				}
			}

			private EvidenceLabel evidencelabel;
		};

		return BUNDLE_OF_PREFERENCES;
	}
}
