package il2.inf.structure;
import java.util.*;
import il2.util.*;
public interface Orderer{
    int removeBest();
    void add(IntSet element);
    void addAll(Collection elements);
}
