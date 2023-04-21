package il2.model;
import il2.bridge.*;
public class RandomNetworks{
    public static BayesianNetwork generate(int nodes,int width){
	return new Converter().convert(edu.ucla.belief.RandomNetworks.randomNetwork(nodes,width));
    }
}
