package edu.ucla.belief.ui.actionsandmodes;

/** @author keith cascio
	@since  20020617 */
public class SamiamUserMode
{
	//statics
	protected static int theNextMask = (int)1;
	public static final String
	  STR_NAME_USER  = "user",
	  STR_32_ZEROS   = "00000000000000000000000000000000";

	public static        final SamiamUserMode
	  OPENFILE           = new SamiamUserMode(         "Open File", "Open File description."                        ),
	  EDIT               = new SamiamUserMode(              "Edit", "Edit description."                             ),
	  QUERY              = new SamiamUserMode(             "Query", "Query description."                            ),
	  NEEDSCOMPILE       = new SamiamUserMode(     "Needs Compile", "Network has been modified but not compiled."   ),
	  SMILEFILE          = new SamiamUserMode(   "Open SMILE File", "'Open SMILE File' description."                ),
	  COMPILING          = new SamiamUserMode(         "Compiling", "Compiling description."                        ),
	  READONLY           = new SamiamUserMode(         "Read Only", "Read Only description."                        ),
	  ANIMATE            = new SamiamUserMode(           "Animate", "Animate description."                          ),
	  HIDE               = new SamiamUserMode( "Hide Hidden Nodes", "Hide nodes marked as hidden."                  ),
	  HIDEHIDDENEDGES    = new SamiamUserMode( "Hide Hidden Edges", "Hide edges incident on nodes marked as hidden." ),
	  EVIDENCEFROZEN     = new SamiamUserMode(   "Freeze Evidence", "Evidence Frozen description."                  ),
	  MODELOCK           = new SamiamUserMode(         "Mode Lock", "Mode Lock description."                        ),
	  LOCATIONSFROZEN    = new SamiamUserMode(  "Freeze Locations", "Locations Frozen description."                 ),
	  SHOWEDGES          = new SamiamUserMode(             "Edges", "Show network edges."                           ),
	  SHOWRECOVERABLES   = new SamiamUserMode( "Recoverable Edges", "Show recoverable network edges."               );

	public SamiamUserMode(){
		this .setModeEnabled( SHOWEDGES,        true );
		this .setModeEnabled( SHOWRECOVERABLES, true );
	}

	/** Copy constructor. @since 20020711 */
	public SamiamUserMode( SamiamUserMode toCopy )
	{
		this.myName = toCopy.myName;
		this.myDescription = toCopy.myDescription;
		this.myMask = toCopy.myMask;
	}

	/** @since 20051006 */
	public String getName(){
		return this.myName;
	}

	/** @since 20080221 */
	public String getDescription(){
		return this.myDescription;
	}

	public boolean contains( SamiamUserMode submode )
	{
		return (myMask & submode.myMask) == submode.myMask;
	}

	public void setModeEnabled( SamiamUserMode aMode, boolean enabled )
	{
		if( enabled ) myMask |= aMode.myMask;
		else myMask &= ~aMode.myMask;
	}

	public String toString()
	{
		return myName + ": " + toBinaryString( myMask );
	}

	public static String toBinaryString( int mask )
	{
		String ret = Integer.toBinaryString( mask );
		ret = STR_32_ZEROS.substring( ret.length() ) + ret;
		return ret;
	}

	/** @since 081704 */
	public boolean equals( Object o ){
		return (o instanceof SamiamUserMode) && (((SamiamUserMode)o).myMask == this.myMask);
	}

	/** @since 081704 */
	public boolean agreesModulo( SamiamUserMode mode, SamiamUserMode mask ){
		if( mode == null || mask == null ) return false;
		return (mode.myMask & mask.myMask) == (this.myMask & mask.myMask);
	}

	protected String
	  myName        = STR_NAME_USER,
	  myDescription = STR_NAME_USER;
	protected int     myMask = 0;

	private SamiamUserMode( String name, String description )
	{
		if( theNextMask == (int)0 ) throw new RuntimeException( "SamiamUserMode() error: no more bits." );

		myName = name;
		myDescription = description;
		myMask = theNextMask;
		//System.out.println( "new SamiamUserMode( "+name+" ) == " + toBinaryString( myMask ) );//debug

		theNextMask = theNextMask << 1;
	}
}
