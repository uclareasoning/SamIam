package edu.ucla.belief.inference.map;
import edu.ucla.belief.*;
import java.util.*;
import edu.ucla.util.*;
abstract class TableEliminationEngine extends BucketEliminator {
    protected final Collection variables(Object obj) {
        return ((Table) obj).variables();
    }
}
