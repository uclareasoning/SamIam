package edu.ucla.belief.ui.preference;

/** @author keith cascio
	@since 20060522 */
public abstract class TargetedBundle
{
	public TargetedBundle( String key, String[] keys, SamiamPreferences prefs ){
		this.myKey       = key;
		this.myKeys      = keys;
		this.validate( prefs );
	}

	         public void begin( Object me, boolean force ){}
	abstract public void setPreference( int index, Object value );
	         public void finish() {}
	         //public void update() {}
	         //public void preview(){}
	         //public void set()    {}

	public TargetedBundle validate( SamiamPreferences prefs ){
		if( this.mySamiamPreferences != prefs ){
			this.mySamiamPreferences  = prefs;
			if( this.myPrefs == null ) this.myPrefs = new Preference[ myKeys.length ];

			for( int i=0; i<myPrefs.length; i++ ){
				this.myPrefs[i] = prefs.getMappedPreference( myKeys[i] );
			}
		}
		return TargetedBundle.this;
	}

	/** @since 20060523 */
	public boolean isRecentlyCommittedValue(){
		for( int i = 0; i<myPrefs.length; i++ ){
			if( myPrefs[i].isRecentlyCommittedValue() ) return true;
		}
		return false;
	}

	public void setPreference( Object me, int index ){
		this.begin(  me, true );
		this.setPreference( index, myPrefs[index].getValue() );
		this.finish();
	}

	public void updatePreferences( Object me ){
		this.begin(  me, false );
		//this.update();
		for( int i = 0; i<myPrefs.length; i++ ){
			if( myPrefs[i].isRecentlyCommittedValue() ) this.setPreference( i, myPrefs[i].getValue() );
		}
		this.finish();
	}

	public void previewPreferences( Object me ){
		this.begin(  me, false );
		//this.preview();
		for( int i = 0; i<myPrefs.length; i++ ){
			if( myPrefs[i].isComponentEdited() ) this.setPreference( i, myPrefs[i].getCurrentEditedValue() );
		}
		this.finish();
	}

	public void setPreferences( Object me ){
		this.begin(  me, true );
		//this.set();
		for( int i = 0; i<myPrefs.length; i++ ){
			this.setPreference( i, myPrefs[i].getValue() );
		}
		this.finish();
	}

	private String            myKey;
	private String[]          myKeys;
	private Preference[]      myPrefs;
	private SamiamPreferences mySamiamPreferences;
}
