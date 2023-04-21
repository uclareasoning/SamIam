package il2.inf;
import il2.model.Table;

public interface PartialDerivativeEngine extends JointEngine{
    public Table tablePartial(int table);
    public Table varPartial(int var);
}
