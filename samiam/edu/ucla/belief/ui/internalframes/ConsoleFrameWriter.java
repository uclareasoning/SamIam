package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** @author keith cascio
	@since  20030715 */
public class ConsoleFrameWriter extends PrintWriter
{
	public static int INT_STRINGWRITER_SIZE = (int)128;
	public static boolean FLAG_AUTO_SCROLL = true;
	public static final String STR_FILENAME_ICON = "Console16.gif";

	public ConsoleFrameWriter()
	{
		super( new StringWriter( INT_STRINGWRITER_SIZE ), true );
		myStringWriter = (StringWriter) out;
		BufferedWriter bw = new SlaveBufferedWriter( myStringWriter );
		out = bw;
	}

	private class SlaveBufferedWriter extends BufferedWriter
	{
		private SlaveBufferedWriter( Writer out )
		{
			super( out );
		}

		public void flush() throws IOException
		{
			super.flush();
			ConsoleFrameWriter.this.flushToConsole();
		}
	}

	public void clear()
	{
		if( myJTextArea != null ) myJTextArea.setText( "" );
		clearStringWriter();
	}

	private void clearStringWriter()
	{
		/*
		try{
			myStringWriter.close();
		}catch( IOException e ){
			System.err.println( "Warning: ConsoleFrameWriter.flushToConsole() caught exception." );
		}
		myStringWriter = new StringWriter( INT_STRINGWRITER_SIZE );
		BufferedWriter bw = new SlaveBufferedWriter( myStringWriter );
		out = bw;
		*/
		myStringWriter.getBuffer().setLength(0);
	}

	public void flush()
	{
		super.flush();
		flushToConsole();
	}

	public void flushToConsole()
	{
		if( myJTextArea != null && myJTextArea.isVisible() )
		{
			String contents = myStringWriter.toString();
			//System.out.println( "ConsoleFrameWriter.flushToConsole(): " + contents );
			myJTextArea.append( contents );
			clearStringWriter();
			if( contents.length() > 0 && FLAG_AUTO_SCROLL ) scrollToEnd();
		}
	}

	public JInternalFrame getFrame()
	{
		if( myJInternalFrame == null )
		{
			myJTextArea = new JTextArea( 10, 70 );
			myJTextArea.setEditable( false );
			try{
				if( myJTextArea.getFont().getFamily().toLowerCase().indexOf( "monospaced" ) < 0 ){
					myJTextArea.setFont( new Font( "Monospaced", Font.PLAIN, myJTextArea.getFont().getSize() ) );
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: ConsoleFrameWriter.getFrame() caught " + thrown );
			}

			myJScrollPane = new JScrollPane( myJTextArea );
			myJInternalFrame = new JInternalFrame( "Console", true, true, true, true );

			JPanel pnlMain = new JPanel( new BorderLayout() );
			pnlMain.add( myJToolBar = makeJToolBar(), BorderLayout.NORTH );
			pnlMain.add( myJScrollPane, BorderLayout.CENTER );

			myJInternalFrame.getContentPane().add( pnlMain );
			myJInternalFrame.setDefaultCloseOperation( JInternalFrame.HIDE_ON_CLOSE );

			Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
			if( iconFrame != null ) myJInternalFrame.setFrameIcon( iconFrame );

			//Dimension dim = myJTextArea.getPreferredSize();
			myJInternalFrame.pack();
		}

		flush();
		scrollToEnd();
		return myJInternalFrame;
	}

	/** @since 062304 */
	public JToolBar getJToolBar(){
		return myJToolBar;
	}

	private JToolBar makeJToolBar()
	{
		JToolBar toolbar = new JToolBar();
		toolbar.add( MainToolBar.initButton( action_SAVE ) );
		//toolbar.add( MainToolBar.initButton( action_CUT ) );
		toolbar.add( MainToolBar.initButton( action_COPY ) );
		//toolbar.add( MainToolBar.initButton( action_SELECTALL ) );
		toolbar.add( MainToolBar.initButton( action_CLEAR ) );
		toolbar.add( MainToolBar.initButton( action_SCROLLTOEND ) );

		if( Util.DEBUG )
		{
			toolbar.add( MainToolBar.initButton( action_SHORTLINE ) );
			toolbar.add( MainToolBar.initButton( action_LONGLINE ) );
		}

		toolbar.setFloatable( false );
		toolbar.putClientProperty( "JToolBar.isRollover", Boolean.TRUE );
		return toolbar;
	}

	public void scrollToEnd()
	{
		if( myJScrollPane != null )
		{
			JViewport port = myJScrollPane.getViewport();
			int heightText = myJTextArea.getHeight();
			RECT_UTIL.setBounds( 0,heightText,0,heightText );
			port.scrollRectToVisible( RECT_UTIL );
		}
	}

	/** @since 20051010 */
	public void scrollToBeginning(){
		if( myJScrollPane == null ) return;
		JViewport port = myJScrollPane.getViewport();
		POINT_UTIL.setLocation(0,0);
		port.setViewPosition( POINT_UTIL );
		myJScrollPane.validate();
	}

	public void save( File saveFile ) throws IOException
	{
		FileWriter fWriter = new FileWriter( saveFile );
		flush();
		if( myJTextArea != null ) myJTextArea.write( fWriter );
		else fWriter.write( myStringWriter.toString() );
	}

	public void copy()
	{
		if( myJTextArea != null ) myJTextArea.copy();
	}

	public void cut()
	{
		if( myJTextArea != null ) myJTextArea.cut();
	}

	public void delete()
	{
		if( myJTextArea != null ) myJTextArea.replaceSelection( "" );
	}

	public void selectAll()
	{
		if( myJTextArea != null ) myJTextArea.selectAll();
	}

	private static final Rectangle RECT_UTIL = new Rectangle(0,0,0,0);
	private static final Point POINT_UTIL = new Point(0,0);

	/**
		test/debug
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		final ConsoleFrameWriter CFW = new ConsoleFrameWriter();

		final JFrame frame = new JFrame( "DEBUG ConsoleFrameWriter" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		final JDesktopPane pain = new JDesktopPane();
		//pain.setBorder( BorderFactory.createLineBorder( Color.red, 2 ) );

		JPanel panel = new JPanel( new BorderLayout() );
		//panel.setBorder( BorderFactory.createLineBorder( Color.green, 2 ) );

		panel.add( pain, BorderLayout.CENTER );

		JPanel pnlButtons = new JPanel();
		panel.add( pnlButtons, BorderLayout.SOUTH );

		JButton btnTemp;

		btnTemp = new JButton( CFW.action_SHORTLINE );
		btnTemp.setText( "" );
		pnlButtons.add( btnTemp );

		btnTemp = new JButton( CFW.action_LONGLINE );
		btnTemp.setText( "" );
		pnlButtons.add( btnTemp );

		final JButton btnFlush = new JButton( "flush" );
		btnFlush.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				CFW.flush();
			}
		} );
		pnlButtons.add( btnFlush );

		btnTemp = new JButton( CFW.action_CLEAR );
		btnTemp.setText( "" );
		pnlButtons.add( btnTemp );

		btnTemp = new JButton( CFW.action_SCROLLTOEND );
		btnTemp.setText( "" );
		pnlButtons.add( btnTemp );

		btnTemp = new JButton( CFW.action_COPY );
		btnTemp.setText( "" );
		pnlButtons.add( btnTemp );

		final JButton btnShowConsole = new JButton( "show console" );
		btnShowConsole.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				JInternalFrame frame = CFW.getFrame();
				if( !pain.isAncestorOf( frame ) ) pain.add( frame );

				//frame.setBounds( 10,10,350,200 );
				frame.setLocation( 10, 10 );
				frame.setVisible( true );
			}
		} );
		pnlButtons.add( btnShowConsole );

		btnTemp = new JButton( CFW.action_SAVE );
		btnTemp.setText( "" );
		pnlButtons.add( btnTemp );

		frame.getContentPane().add( panel );
		//frame.pack();
		frame.setBounds( 0,0,600,400 );
		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	public final Action action_SAVE = new SamiamAction( "Save", "Save contents of console", 's', MainToolBar.getIcon( "Save16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			JFileChooser chooser = new JFileChooser();
			if( chooser.showSaveDialog( myJInternalFrame ) == JFileChooser.APPROVE_OPTION )
			{
				File fileSelected = chooser.getSelectedFile();
				if( fileSelected != null )
				{
					try{
						save( fileSelected );
					}catch( IOException ex ){
						JOptionPane.showMessageDialog( myJInternalFrame, "Error: Failed to save the contents\nof the console.", "Console Error", JOptionPane.ERROR_MESSAGE );
					}
				}
			}
		}
	};

	//"Copy   ctrl-c"
	public final Action action_COPY = new SamiamAction( "Copy", "Copy selected text to system clipboard", 'c', MainToolBar.getIcon( "Copy16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			copy();
		}
	};

	public final Action action_CUT = new SamiamAction( "Cut", "Cut selected text to system clipboard", 'x', MainToolBar.getIcon( "Cut16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			cut();
		}
	};

	public final Action action_CLEAR = new SamiamAction( "Delete", "Delete selected text", 'd', MainToolBar.getIcon( "Delete16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			delete();
		}
	};

	public final Action action_SELECTALL = new SamiamAction( "Select all", "Select all", 'a', MainToolBar.getIcon( "SelectAll16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			selectAll();
		}
	};

	public final Action action_SCROLLTOEND = new SamiamAction( "To end", "Scroll to end", 'e', MainToolBar.getIcon( "EndOfDocument16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			scrollToEnd();
		}
	};

	public final Action action_SHORTLINE = new SamiamAction( "short line", "print a short line", 's', MainToolBar.getIcon( "History16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			println( "short line " + INT_DEBUG_COUNTER++ );
		}
	};

	public final Action action_LONGLINE = new SamiamAction( "long line", "print a long line", 'l', MainToolBar.getIcon( "HistoryAlt16.gif" ) )
	{
		public void actionPerformed( ActionEvent e )
		{
			println( "long line long line long line long line long line long line long line long line " + INT_DEBUG_COUNTER++ );
		}
	};

	private static int INT_DEBUG_COUNTER = (int)0;

	private JInternalFrame myJInternalFrame;
	private JScrollPane myJScrollPane;
	private JTextArea myJTextArea;
	private StringWriter myStringWriter;
	private JToolBar myJToolBar;
}
