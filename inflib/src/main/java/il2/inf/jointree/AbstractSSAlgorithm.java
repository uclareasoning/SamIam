package il2.inf.jointree;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
import il2.inf.PartialDerivativeEngine;

public abstract class AbstractSSAlgorithm extends JoinTreeAlgorithm implements PartialDerivativeEngine{

    Table[][] messages;
    Table[][] incomingMessages;

    public AbstractSSAlgorithm(EliminationOrders.JT jointree,Table[] tables){
	super(jointree,tables);
	allocateTables();
	findIncomingMessages();
    }

    protected void findIncomingMessages(){
	incomingMessages=new Table[clusters.length][];
	for(int i=0;i<clusters.length;i++){
	    incomingMessages[i]=new Table[tree.degree(new Integer(i))];
	}
	int[] currentLength=new int[clusters.length];
	for(int i=0;i<messageOrder.length;i++){
	    Pair p=messageOrder[i];
	    incomingMessages[p.s2][currentLength[p.s2]]=messages[0][i];
	    currentLength[p.s2]++;
	    incomingMessages[p.s1][currentLength[p.s1]]=messages[1][i];
	    currentLength[p.s1]++;
	}
    }
    protected void allocateTables(){
	messages=new Table[2][messageOrder.length];
	for(int i=0;i<messageOrder.length;i++){
	    IntSet vars=(IntSet)separators.get(new UPair(messageOrder[i]));
	    messages[0][i]=new Table(domain,vars);
	    messages[1][i]=new Table(domain,vars);
	}
    }

    protected double computePrE(){
	if(messages[0].length>0){
	    double[] v1=messages[0][0].values();
	    double[] v2=messages[1][0].values();
	    double total=0;
	    for(int i=0;i<v1.length;i++){
		total+=v1[i]*v2[i];
	    }
	    return total;
	}else{
	    return computeTableJoint(0).sum();
	}
    }

    public abstract Table tablePartial(int table);
    public abstract Table varPartial(int var);

    public double getTableSizes(){
	double total=0;
	for(int i=0;i<messages.length;i++){
	    for(int j=0;j<messages[i].length;j++){
		total+=messages[i][j].sizeDouble();
	    }
	}
	return total*8;
    }

}
