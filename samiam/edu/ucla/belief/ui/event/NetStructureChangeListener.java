package edu.ucla.belief.ui.event;

/** An interface to listen for network structure changes. */

public interface NetStructureChangeListener {
    /** NetStructureEvent never null.*/
    public void netStructureChanged( NetStructureEvent ev);
}
