/*
* Pointer.java
*
* Created on February 29, 2000, 1:24 PM
*/
package edu.ucla.util;
/**
* A simple class to ease call by reference type implementations.
* @author unknown
* @version
*/
public class Reference extends Object {
    public Object object;
    /** Creates new Reference */
    public Reference() {
    }
    public Reference(Object ref) {
        object = ref;
    }
}
