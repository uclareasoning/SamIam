package edu.ucla.belief.ui.clipboard;

import edu.ucla.belief.EvidenceController;
import edu.ucla.belief.BeliefNetwork;
import java.util.Collection;
import java.util.Map;
import javax.swing.JComponent;
import java.io.*;

/** @since 20030516 */
public interface InstantiationClipBoard extends Map
{
	public static final String STR_FILENAME_ICON = "PasteAlt16.gif";

	public   void                  copy( Map instantiation );
	public   void                   cut( Map instantiation, EvidenceController controller );
	public   void                 paste( BeliefNetwork network );
	public   JComponent            view();
	public   boolean               load()                      throws UnsupportedOperationException, IOException;
	public   boolean               save()                      throws UnsupportedOperationException;
	public   boolean               load( File fileInput  )     throws UnsupportedOperationException, IllegalArgumentException, IOException;
	public   boolean               save( File fileOutput )     throws UnsupportedOperationException;

	public   int       importFromSystem()                      throws UnsupportedOperationException;
	public   boolean     exportToSystem()                      throws UnsupportedOperationException;
}
