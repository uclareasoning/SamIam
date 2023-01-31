package il2.inf.SDP;

import edu.ucla.belief.BeliefNetwork;
import il2.model.BayesianNetwork;
import il2.model.Domain;
import il2.model.Table;
import il2.util.IntList;
import il2.util.IntMap;
import il2.util.IntSet;
import il2.inf.structure.EliminationOrders;
import il2.inf.BasicInference;
import il2.inf.structure.Bucketer;
import il2.inf.edgedeletion.EDEdgeDeleter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import il2.bridge.IO;


    
public class SDP { // CACHING AND ELIMINATING INTERMEDIATE VARIABLES //does not deal well with deterministic parameters (if decision var is equivalence constraint it breaks)
    BayesianNetwork bn;
    IntMap evidence;
    PrintWriter pw;
    IntSet ivars, hvars, evars;
    int dvar, inst_visited;
    Table[] cpts, cpts_d, cpts_dbar;
    Domain domain;
    double lg_threshold, current, prior, probability_evidence, totalp, totalpbar;
    //current = logO(d|e), prior = logO(d), Pr(e)
    double d_probability, dbar_probability; //Pr(d), Pr(dbar)
    double d_e_probability, dbar_e_probability; //Pr(d,e) Pr(dbar,e)
    SubNetwork[] subnetwork_array;
    double[] max_array, min_array, d_prob_array, dbar_prob_array; //holds max weight and min weight remaining subnets
    boolean cache;
    boolean flipped; // if initial decision not over threshold
    /* for testing purposes, certain parameters are hard-coded */
    
    
    public String[] query_variables;// HEALTH_ST = "H";
    public String decision_variable;//"health";
    //public int TARGET_STATE = 0;

    public SDP(BayesianNetwork bn, Map<String,String> temp_map, PrintWriter pw, double threshold, String decision_variable, String[] query_variables) {
        this.cache = true;
        this.query_variables = query_variables;
        this.decision_variable = decision_variable;
        this.bn = bn;
        this.cpts = bn.cpts();
        this.domain = bn.domain();
        //this.evidence = evidence;
        this.pw = pw == null ? new java.io.PrintWriter(System.out,true) : pw;
        

        this.evidence = new IntMap();
        //System.out.println("evidence map is " + temp_map);
        for (String key : temp_map.keySet()) {
            String value = temp_map.get(key);
            int temp_var = this.domain.index(key);
            this.evidence.put(temp_var,this.domain.instanceIndex(temp_var, value));
        }
        this.evars = new IntSet(evidence.keys());
        this.hvars = this.findHealthVariables();
        this.dvar = this.findTargetVariable();
        IntSet all_ivars = this.findInterVariables();
        this.ivars = new IntSet();
        for(int i=0; i<all_ivars.size(); i++) {
            if (!this.evars.contains(all_ivars.get(i)))
                this.ivars.add(all_ivars.get(i));
        }

        //System.out.println("set up complete");
        //calculating prior of d
        calculatePriorLogOdds();
        
        //can remove any table with intermediary variable if that's the only
        //table the intermediary variable appears in (remove leaf nodes)
        int[] a = {this.dvar};
        IntSet query_vars = new IntSet(a);
        this.cpts = this.bn.simplify(this.hvars.union(query_vars),evidence); //AC
        //these are the only vars we are interested in, D,H,E
        double log_odds = threshold/(1-threshold);
        this.lg_threshold = this.lg(log_odds);
        //System.out.println("Log threshold is: " + this.lg_threshold);
        updatePosterior();
        //System.out.println(this.current);
        if (this.current >= lg_threshold) {
            this.flipped = false;
        } else {
            //System.out.println("flipped!");

            this.flipped = true;
        }
    }

    public void calculatePriorLogOdds() {
        int index = this.domain.index(this.decision_variable);
        IntMap prior_evidence = new IntMap();
        prior_evidence.put(index,0);
        Table[] cpts_d = Table.shrink(this.cpts,prior_evidence);
        HashSet<Table> cpts_d_hashset =  new HashSet<Table> (Arrays.asList(cpts_d));
        this.d_probability = BasicInference.sumProduct(cpts_d_hashset);
        prior_evidence.put(index,1);
        Table[] cpts_dbar = Table.shrink(this.cpts,prior_evidence);
        HashSet<Table> cpts_dbar_hashset =  new HashSet<Table> (Arrays.asList(cpts_dbar));
        this.dbar_probability = BasicInference.sumProduct(cpts_dbar_hashset);
        //double prob_prior = this.d_probability/this.dbar_probability;
        //System.out.println("Prior probability is " + prob_prior);
        this.prior = this.lg( this.d_probability/this.dbar_probability);
        //System.out.println("Prior log-odds is " + this.prior);
        //

    }
    
    public void updatePosterior() {
        IntMap evidence = this.evidence; //shrink with d as evidence
        evidence.put(this.domain.index(this.decision_variable),0);
        this.cpts_d = Table.shrink(this.cpts,evidence);
        HashSet<Table> cpts_d_hashset = new HashSet<Table> (Arrays.asList(this.cpts_d));
        this.d_e_probability = BasicInference.sumProduct(cpts_d_hashset);
        evidence.put(this.domain.index(this.decision_variable),1);
        this.cpts_dbar = Table.shrink(cpts,evidence);
        HashSet<Table> cpts_dbar_hashset =  new HashSet<Table> (Arrays.asList(cpts_dbar));
        this.dbar_e_probability = BasicInference.sumProduct(cpts_dbar_hashset);
        this.probability_evidence = this.dbar_e_probability + this.d_e_probability;
        //System.out.println("Probability of evidence is " + this.probability_evidence);
        //prevent deterministic
        if (this.d_e_probability == 0) 
            //this.current = Double.MIN_VALUE;
            this.current = Double.NEGATIVE_INFINITY;
        else if (this.dbar_e_probability == 0)
            //this.current = Double.MAX_VALUE;
            this.current = Double.POSITIVE_INFINITY;
        else
            this.current =  this.lg(this.d_e_probability/this.dbar_e_probability);
        //System.out.println("Posterior probability (updated) is " + this.d_e_probability/(this.d_e_probability+this.dbar_e_probability));
        //System.out.println("Posterior log odds is " + this.current);
    }
    
    public IntSet findHealthVariables() { 
        Domain d = bn.domain();
        IntSet vars = new IntSet();
        //for (int var = 0; var < d.size(); var++) {
        for (int i = 0; i < this.query_variables.length; i++) {
            String var_name = query_variables[i];
            vars.add(d.index(var_name));
        }
        return vars;
    }

    public IntSet findInterVariables() {
        //this includes evidence variables...
        Domain d = bn.domain();
        IntSet vars = new IntSet();
        for (int var = 0; var < d.size(); var++) {
            //String name = d.name(var);
            if ( !this.hvars.contains(var) && !(var == this.dvar))
                vars.add(var);
        }
        return vars;
    }
    
        
    public int findTargetVariable() {
        Domain d = bn.domain();
        int ret_val = d.index(this.decision_variable);
        return ret_val;
    }


	public ArrayList ve2(ArrayList fset, IntList order) {
        //System.out.println("order is " + order);
        for (int i = 0; i < order.size(); i++) {
			int var = order.get(i);
			HashSet<Table> gset = new HashSet<Table>();
			IntSet vars = new IntSet();
            for (Iterator<Table> it = fset.iterator(); it.hasNext(); ) {
				Table t = it.next();
				if ( t.vars().contains(var) ) {
					it.remove();
					gset.add(t);
					vars = vars.union(t.vars());
				}
			}
            if ( gset.isEmpty() ) continue;
			vars.remove(var);
			Table f = new Table(bn.domain(),vars);
			f.multiplyAndProjectInto(gset.toArray(new Table[]{}));
			fset.add(f);
		}

        return fset;
	}
    

    class InstantiationGenerator {
        //have a feeling there might be a bug.
        public int maxNum, currentNum; //number of total instantiations and current
        public IntSet hvars;
        public int[] domain_sizes, cached_domain;
        public InstantiationGenerator(IntSet hvars, int[] domain_sizes) {
            this.hvars = hvars;
            this.domain_sizes = domain_sizes;
            this.cached_domain = new int[domain_sizes.length];
            this.maxNum = 1;
            for (int i = 0; i < domain_sizes.length; i++)
                this.maxNum *= domain_sizes[i];
        }

        public IntMap next() {
           
            //create the return intmap
            IntMap ret_intmap = new IntMap();
            for (int i = 0; i < this.hvars.size(); i++)
                ret_intmap.put(hvars.get(i), this.cached_domain[i]);
            int pointer = this.hvars.size() - 1;
            this.currentNum++;
            while (true) {
                if (pointer == -1)
                    break;
                if (this.cached_domain[pointer] + 1 == this.domain_sizes[pointer]) {
                    this.cached_domain[pointer] = 0;
                    pointer--;
                } else {
                    this.cached_domain[pointer]++;
                    break;
                }
                
            }
            return ret_intmap;

        }
    }
   
    public double lg(double prob) {
        return Math.log(prob)/Math.log(2);
    }

    class SubNetwork implements Comparable<SubNetwork>{
        public Table[] tables_d, tables_dbar, new_cpts;
        public IntSet ivars, hvars;
        double max_weight, min_weight;
        double prob_e_d, prob_e_dbar; //Pr(e|d) and Pr(e|dbar)
        //if there is pruning, these subnetworks Pr(evidence) must still be considered
        PartialInstantiation[] cached_instantiations;
        Domain domain;
        public SubNetwork(Table[] tables_d, Table[] tables_dbar,
                          IntSet ivars, IntSet hvars, Domain domain) {
            this.tables_d = tables_d;
            this.tables_dbar = tables_dbar;
            IntSet subnetwork_vars = SDP.findVariables(tables_d);
            //System.out.println("subnetwork vars are " + subnetwork_vars);
            this.ivars = subnetwork_vars.intersection(ivars);
            this.hvars = subnetwork_vars.intersection(hvars);
            this.cached_instantiations = null;
            this.domain = domain;
        }
      
        public int compareTo(SubNetwork tempS2) {
            double total_weight1 = this.max_weight - this.min_weight;
            double total_weight2 = tempS2.max_weight - tempS2.min_weight;
            return Double.compare(total_weight1,total_weight2);
            /* //AC
            if (total_weight1 > total_weight2)
                return -1;
            if (total_weight1 == total_weight2)
                return 0;
            else
                return 1;
            */
        }

         public PartialInstantiation performInference(IntMap evidence_x) {
            //intermediary variables have been removed (calculateUpperLower)
            Table[] cpts_d_modified = Table.shrink(this.tables_d, evidence_x);
            Table[] cpts_dbar_modified = Table.shrink(this.tables_dbar, evidence_x);
            HashSet<Table> temphset1 = new HashSet<Table> (Arrays.asList(cpts_d_modified));
            HashSet<Table> temphset2 = new HashSet<Table> (Arrays.asList(cpts_dbar_modified));
            double temp_prob_e_d = BasicInference.sumProduct(temphset1);
            double temp_prob_e_dbar = BasicInference.sumProduct(temphset2);
            /*
            double resultx = temp_prob_e_d/temp_prob_e_dbar;
            resultx = Math.log(resultx)/Math.log(2); // weight of the instantiation 
            */
            double resultx;
            if (temp_prob_e_d == 0) 
                //resultx = Double.MIN_VALUE;
                resultx = Double.NEGATIVE_INFINITY;
            else if (temp_prob_e_dbar == 0)
                //resultx = Double.MAX_VALUE;
                resultx = Double.POSITIVE_INFINITY;
            else
                resultx =  Math.log(temp_prob_e_d/temp_prob_e_dbar)/Math.log(2);
            PartialInstantiation newCI = new PartialInstantiation(evidence_x, resultx,
                                                      temp_prob_e_d, temp_prob_e_dbar);
            return newCI;
        }
         
        public void generateCachedInstantiation() {
            //compute all the instantiations for a subnetwork of variables.
            //intermediary variables have been removed (calculateUpperLower)
            int[] domain_sizes = new int[this.hvars.size()];
            for (int i = 0; i < this.hvars.size(); i++)
                domain_sizes[i] = this.domain.size(this.hvars.get(i));
            InstantiationGenerator ig = new InstantiationGenerator(this.hvars,domain_sizes);
            this.cached_instantiations = new PartialInstantiation[ig.maxNum];
            //takes a lot of memory!
            for (int k = 0; k <ig.maxNum; k++){
                IntMap evidence_x = ig.next();
                this.cached_instantiations[k] = this.performInference(evidence_x);
            }

        }
    }

    public void  partitionSubnetwork() {
        //initializes subnetwork array
        //System.out.println("Partitioning Subnetworks...");
        BayesianNetwork new_bn  = new BayesianNetwork(this.cpts_d);
        IntSet[] components_array = this.getConnectedComponents(new_bn);
        boolean[] used_tables = new boolean[this.cpts_d.length];
        Arrays.fill(used_tables,false);
        for(int i = 0; i < components_array.length; i++){ 
            IntSet temp_iset = components_array[i];
            for (int j = 0; j < temp_iset.size(); j++) {
                used_tables[temp_iset.get(j)] = true;
            }
        }
        //some tables may not have been used
        IntSet evidence_subnetwork = new IntSet(); //might not be necessary..right now evidence subnetworks aren't being added??
        for(int i = 0; i < used_tables.length; i++) {
            if (used_tables[i] == false)
                evidence_subnetwork.add(i);
        }
        if(evidence_subnetwork.size() > 0) {
            //System.out.println("Evidence subnetwork being added, should happen only once!");
            IntSet[] new_components_array = new IntSet[components_array.length+1];
            for (int j=0; j<components_array.length;j++)
                new_components_array[j] = components_array[j];
            new_components_array[components_array.length] = evidence_subnetwork;
            components_array = new_components_array;
        }
        //System.out.println("Table partitions are: "+ Arrays.toString(components_array));
        this.subnetwork_array = new SubNetwork[components_array.length];
        for(int i = 0; i < components_array.length; i++) {
            //System.out.println("Subnetwork " + i);
            IntSet table_list = components_array[i];
            Table[] subnetwork_tables_d = new Table[table_list.size()];
            Table[] subnetwork_tables_dbar = new Table[table_list.size()];
            for(int j=0; j< table_list.size(); j++){
                int k = table_list.get(j);
                subnetwork_tables_d[j] = cpts_d[k];
                subnetwork_tables_dbar[j] = cpts_dbar[k];
            }
            //if (subnetwork_tables_d.length == 1) //this is evidence subnetwork (may not be..)
            //  System.out.println("fake subnetwork table is " + subnetwork_tables_d[0]);
            //these empty subnetworks carry evidence, not preventing from
            //reaching correct solution (cross tested with brute force)
            SubNetwork new_subnetwork =  new SubNetwork(subnetwork_tables_d,
                                                   subnetwork_tables_dbar,
                                                          this.ivars,
                                                           this.hvars,
                                                           this.domain);
            this.subnetwork_array[i] = new_subnetwork;
        }
    }

    public void calculateUpperLower() {
        //eliminating ivars from the cpts and calculating subnetwork max/min weights
        for (int i = 0; i < this.subnetwork_array.length; i++) {
            SubNetwork tempS = this.subnetwork_array[i];
            //eliminating ivars and then update the cpts of the subnetwork. 
            java.util.Collection subdomains = Arrays.asList(tempS.tables_d);
            EliminationOrders.Record rec =
                EliminationOrders.constrainedMinFill(subdomains,tempS.hvars);
            IntList orderS = rec.order;
            IntList new_orderS = new IntList(tempS.ivars.size());
            for (int j = 0; j < tempS.ivars.size(); j++)
                new_orderS.add(orderS.get(j));
            ArrayList<Table> fsetS = new ArrayList<Table>(Arrays.asList(tempS.tables_d));
            ArrayList<Table> fsetSb = new ArrayList<Table>(Arrays.asList(tempS.tables_dbar));
            ArrayList<Table> gsetS = this.ve2(fsetS,new_orderS);
            ArrayList<Table> gsetSb = this.ve2(fsetSb,new_orderS);

            Table[] new_tables_d = new Table[gsetS.size()];
            Table[] new_tables_dbar = new Table[gsetSb.size()];
            //creating new factors
            tempS.new_cpts = new Table[gsetS.size()];

            for (int j = 0; j < gsetS.size(); j++) {
                Table t = (Table) gsetS.get(j);
                //System.out.println("t is:\n" +t);
                new_tables_d[j] = t;
                Table t2 = (Table) gsetSb.get(j);
                //System.out.println("t2 is:\n" +t2);
                new_tables_dbar[j] = t2;
                t2 = t2.invert();
                Table x = t.multiply(t2);
                tempS.new_cpts[j] = x;
                //System.out.println("x is:\n" +x);
            }

            tempS.tables_d = new_tables_d;
            tempS.tables_dbar = new_tables_dbar;
            HashSet<Table> finalc = new HashSet<Table> (Arrays.asList(tempS.new_cpts));
            double max_odds = BasicInference.MAP(finalc, tempS.hvars);
            double min_odds = BasicInference.MinAP(finalc, tempS.hvars);
            tempS.max_weight = this.lg(max_odds);
            tempS.min_weight = this.lg(min_odds);

            
        }
        
        Arrays.sort(this.subnetwork_array);
        int num_relevant_subnetwork = 0;
        for (int i = 0; i < this.subnetwork_array.length; i++) {
            SubNetwork tempS = this.subnetwork_array[i];
            if (tempS.hvars.size() > 0) {
                //System.out.println("Subnetwork " + i + "\n ============================");
                //System.out.println("hvars are " + tempS.hvars);
                //System.out.println("ivars are " + tempS.ivars);
                //System.out.println("max weight is " + Math.pow(2,tempS.max_weight));
                //System.out.println("min weight is " + Math.pow(2,tempS.min_weight));
                //System.out.println("max weight (log) is " + tempS.max_weight);
                //System.out.println("min weight (log) is " + tempS.min_weight + "\n\n");
                num_relevant_subnetwork++;
            }
        }
        //if (num_relevant_subnetwork <= 1) //don't cache if only 1 subnetwork
        //    this.cache = false;
        
    }

    public void calculateSubnetworkProbE() { //calculating prob of evidence subnetwork given d
        for (int i = 0; i < this.subnetwork_array.length; i++) {
            SubNetwork tempS = this.subnetwork_array[i];
            Table[] cpts_d = tempS.tables_d;
            Table[] cpts_dbar = tempS.tables_dbar;
            HashSet<Table> temphset1 = new HashSet<Table> (Arrays.asList(cpts_d));
            HashSet<Table> temphset2 = new HashSet<Table> (Arrays.asList(cpts_dbar));
            tempS.prob_e_d = BasicInference.sumProduct(temphset1);
            tempS.prob_e_dbar = BasicInference.sumProduct(temphset2);
        }
    }
    
    public void computeMaxMinArray() {
        //last element should really be 0, 0 weight to add remaining!!
        this.max_array = new double[this.subnetwork_array.length+1];
        this.min_array = new double[this.subnetwork_array.length+1];
        double maxsum = 0;
        double minsum = 0;
        this.max_array[this.max_array.length-1] = maxsum;
        this.min_array[this.min_array.length-1] = minsum;
        for (int i = this.max_array.length - 2; i >= 0; i--) {
            maxsum += this.subnetwork_array[i].max_weight;
            minsum += this.subnetwork_array[i].min_weight;
            this.max_array[i] = maxsum;
            this.min_array[i] = minsum;

        }

    }

    public void computeProbArray() {
        //for partial instantiations in order to calculate prob need to normalize with this.
        this.d_prob_array = new double[this.subnetwork_array.length+1];
        this.dbar_prob_array = new double[this.subnetwork_array.length+1];
        double d_prob = 1.0;
        double dbar_prob = 1.0;
        this.d_prob_array[this.subnetwork_array.length] = d_prob;
        this.dbar_prob_array[this.subnetwork_array.length] = dbar_prob;
        for (int i = this.d_prob_array.length -2; i>=0; i--) {
            //System.out.println("prob d is " + this.subnetwork_array[i].prob_e_d);
            d_prob *= this.subnetwork_array[i].prob_e_d;
            //System.out.println("prob dbar is " + this.subnetwork_array[i].prob_e_dbar);
            dbar_prob *= this.subnetwork_array[i].prob_e_dbar;
            this.d_prob_array[i] = d_prob;
            this.dbar_prob_array[i] = dbar_prob;
        }
        //System.out.println("d prob array is " + Arrays.toString(this.d_prob_array));
        //System.out.println("dbar prob array is " + Arrays.toString(this.dbar_prob_array));

    }

    public void addProbPartial(PartialInstantiation p) {
        this.totalp += p.prob_e_d * this.d_prob_array[p.subnetwork_to_visit];
        this.totalpbar += p.prob_e_dbar * this.dbar_prob_array[p.subnetwork_to_visit];
    }

    public void searchTree2(){
        //System.out.println("Not caching values!");
        IntMap  no_evidence_set = new IntMap();
        PartialInstantiation starting =
            this.newPI(0, no_evidence_set,0.0,1.0,1.0);
        this.totalp = 0.0; //initial SDP
        this.totalpbar = 0.0;

        //check to see if there is even a solution
        if (starting.weight + this.max_array[0] < this.lg_threshold)
            return;
        if (starting.weight + this.min_array[0] >= this.lg_threshold){
            //this.final_instantiations.add(starting);
            this.addProbPartial(starting);
            return;
        }
        InstantiationGenerator[] inst_gen_array = new InstantiationGenerator[this.subnetwork_array.length];
        for (int i = 0; i < this.subnetwork_array.length; i++) {
            SubNetwork temp_sub = this.subnetwork_array[i];
            int[] domain_sizes = new int[temp_sub.hvars.size()];
            for (int k = 0; k < temp_sub.hvars.size(); k++)
                domain_sizes[k] = temp_sub.domain.size(temp_sub.hvars.get(k));
            InstantiationGenerator ig = new InstantiationGenerator(temp_sub.hvars,domain_sizes);
            inst_gen_array[i] = ig;
        }

        PartialInstantiation[] backtrack_array = new PartialInstantiation[this.subnetwork_array.length+1];
        backtrack_array[0] = starting; //holds position of tree...0 
        int backtrack_pointer = 0; //if this ever becomes -1 should stop
        while (backtrack_pointer >= 0) {
            PartialInstantiation temp_inst =  backtrack_array[backtrack_pointer];
            InstantiationGenerator ig = inst_gen_array[backtrack_pointer];
            if (ig.currentNum >= ig.maxNum) {
                ig.currentNum = 0; //reset
                backtrack_pointer--;
                continue;
            }
            SubNetwork tempS = this.subnetwork_array[backtrack_pointer];
            IntMap evidence_x = ig.next();
            PartialInstantiation pi = tempS.performInference(evidence_x);
            IntMap combined = pi.evidence.combine(temp_inst.evidence); 
            PartialInstantiation new_pi = this.newPI(backtrack_pointer+1, combined,
                                                     temp_inst.weight+pi.weight, temp_inst.prob_e_d*pi.prob_e_d,
                                                     temp_inst.prob_e_dbar*pi.prob_e_dbar);
            if (new_pi.weight + this.max_array[new_pi.subnetwork_to_visit] < this.lg_threshold){
                this.inst_visited++;
                continue;
            } else if (new_pi.weight + this.min_array[new_pi.subnetwork_to_visit] >= this.lg_threshold) {
                this.inst_visited++;
                 this.addProbPartial(new_pi);
            
            } else {
                backtrack_pointer++;
                backtrack_array[backtrack_pointer] = new_pi;
            }
        }
        
    }
    
    public void searchTree(){ 
        IntMap  no_evidence_set = new IntMap();
        PartialInstantiation starting = this.newPI(0, no_evidence_set,0.0,1.0,1.0);
        this.totalp = 0.0; //initial SDP
        this.totalpbar = 0.0;

        //check to see if there is even a solution
        if (starting.weight + this.max_array[0] < this.lg_threshold)
            return;
        if (starting.weight + this.min_array[0] >= this.lg_threshold){
            this.addProbPartial(starting);
            return;
        }
        int[] inst_counter_array = new int[this.subnetwork_array.length]; //to 0 automatically
        for (int i = 0; i < this.subnetwork_array.length; i++){
            SubNetwork temp_sub = this.subnetwork_array[i];
            temp_sub.generateCachedInstantiation();
        }

        PartialInstantiation[] backtrack_array = new PartialInstantiation[this.subnetwork_array.length+1];
        backtrack_array[0] = starting; //holds position of tree...0 
        int backtrack_pointer = 0; //if this ever becomes -1 should stop
        while (backtrack_pointer >= 0) {
            PartialInstantiation temp_inst =  backtrack_array[backtrack_pointer];
            int inst_counter = inst_counter_array[backtrack_pointer];
            SubNetwork tempS = this.subnetwork_array[backtrack_pointer];
            if (inst_counter >= tempS.cached_instantiations.length) {
                inst_counter_array[backtrack_pointer] = 0; //reset
                backtrack_pointer--;
                continue;
            }

            PartialInstantiation pi = tempS.cached_instantiations[inst_counter];
            inst_counter_array[backtrack_pointer]++;
            IntMap combined = pi.evidence.combine(temp_inst.evidence); 
            PartialInstantiation new_pi = this.newPI(backtrack_pointer+1, combined,
                                                     temp_inst.weight+pi.weight, temp_inst.prob_e_d*pi.prob_e_d,
                                                     temp_inst.prob_e_dbar*pi.prob_e_dbar);

            if (new_pi.weight + this.max_array[new_pi.subnetwork_to_visit] < this.lg_threshold){
                this.inst_visited++;
                continue;
            } else if (new_pi.weight + this.min_array[new_pi.subnetwork_to_visit] >= this.lg_threshold) {
                this.inst_visited++;
                this.addProbPartial(new_pi);
            
            } else {
                backtrack_pointer++;
                backtrack_array[backtrack_pointer] = new_pi;
            }
        }
    }

    public  IntSet[] getConnectedComponents(BayesianNetwork bn) {
        //return the various sets of connected tables
        Table[] cpts = bn.cpts();
        edu.ucla.structure.UnionFind cs =
			new edu.ucla.structure.UnionFind(cpts.length);
        
        IntSet all_vars = this.findVariables(cpts);
        IntSet[] var_tables = new IntSet[all_vars.size()]; //each var, which table contains it
        for (int i = 0; i < var_tables.length; i++)
            var_tables[i] = new IntSet();
        for (int i = 0; i < cpts.length; i++) {
            IntSet table_vars = cpts[i].vars();
            for (int j = 0; j < table_vars.size(); j++) {
                int indice = all_vars.indexOf(table_vars.get(j));
                IntSet var_table = var_tables[indice];
                var_table.add(i);

            }
        }
        for (int var = 0; var < all_vars.size(); var++) {
            for (int j = 0; j < var_tables[var].size()-1; j++)
                cs.union(var_tables[var].get(j),var_tables[var].get(j+1));
        }

		IntSet cci = new IntSet(); // an index of connected components
		for (int var = 0; var < cpts.length; var++) cci.add(cs.find(var));
		IntSet[] cc = new IntSet[cci.size()]; // list of connected components
		for (int i = 0; i < cc.length; i++) cc[i] = new IntSet();

		// each IntSet of cc represents a connected component where
		// each IntSet is a set of the corresponding variables
		for (int var = 0; var < cpts.length; var++)
			cc[cci.indexOf(cs.find(var))].add(var);
        return cc;
	}
    
    class PartialInstantiation {
        int subnetwork_to_visit;
        IntMap evidence;
        double weight;
        double prob_e_d;
        double prob_e_dbar;
        public PartialInstantiation(IntMap evidence,
                                    double weight, double prob_e_d, double prob_e_dbar) {
            this.evidence = evidence;
            this.weight = weight;
            this.prob_e_d = prob_e_d;
            this.prob_e_dbar = prob_e_dbar;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer(3*this.evidence.size());
            buf.append("Instantiation is " + this.evidence);
            buf.append("\nWeight is " + this.weight);
            buf.append("\n Pr(e|d) is " + this.prob_e_d);
            buf.append("\n Pr(e|bar(d)) is " + this.prob_e_dbar);
            buf.append("\n");
            return buf.toString();
        }
    }

    public PartialInstantiation newPI(int subnetwork_to_visit, IntMap evidence,
                                      double weight, double prob_e_d, double prob_e_dbar ) {
        PartialInstantiation pi = new PartialInstantiation( evidence,
                                                           weight, prob_e_d, prob_e_dbar);
        pi.subnetwork_to_visit = subnetwork_to_visit;
        return pi;
    }
                
    public static IntSet findVariables(Table[] cpts) {
        //takes CPTs and finds the variables in these CPTs
        IntSet ret_intset = new IntSet();
        for (int i = 0; i < cpts.length; i++) 
            ret_intset= ret_intset.union(cpts[i].vars());
        return ret_intset;
    }

    public double computeSDP() {
        this.partitionSubnetwork();
        this.calculateUpperLower();
        this.computeMaxMinArray();
        this.calculateSubnetworkProbE();
        this.computeProbArray();
        if (this.cache)
            this.searchTree();
        else
            this.searchTree2();
        //System.out.println("number of instantiations visited at the end is "
        //  + this.inst_visited);

        //System.out.println("number of h variables is " +  this.hvars.size());
        double finaltotal =  (this.totalp + this.totalpbar)/this.probability_evidence;
       
        if (this.flipped)
            finaltotal = 1-finaltotal;
        //System.out.println("SDP IS " + finaltotal);
        return finaltotal;
        
    }
    public static void main(String[] args) {
        //debug!!!
        //IMPORTANT, SDP right now is always assuming initial decision is above T
        /*
        boolean DEBUG = false;
        PrintWriter pwriter = new PrintWriter(System.out, true);
        System.out.println("Computing the SDP...");
        BayesianNetwork main;
        try {
            main = IO.readNetwork(args[0]);
        } catch (Exception e) {
            System.out.println("IO Exception");
             main = null;
        }
        try{
            if( args[3].equals("-debug") )
                DEBUG = true;
        } catch (Exception e) {
            DEBUG = false;
        }
        
        Domain domain = main.domain();

        //Getting evidence from inst file.
        String evidence_path = args[0] + ".inst";
        System.out.println("evidence path is " + evidence_path);
        Map<String,String> temp_map = new HashMap<String,String>();
        IntMap evidence = new IntMap();
        edu.ucla.belief.io.InstantiationXmlizer xml = 
			new edu.ucla.belief.io.InstantiationXmlizer();
        il2.bridge.Converter conv = new il2.bridge.Converter();
        //try {
        try {
            temp_map =  xml.getMap(new java.io.File(evidence_path));
        } catch (Exception e) {
            System.out.println("no evidence or  IO err getting evidence");
        }
        System.out.println(temp_map);
        for (String key : temp_map.keySet()) {
            String value = temp_map.get(key);
            int temp_var = domain.index(key);
            evidence.put(temp_var,domain.instanceIndex(temp_var, value));
        }
        System.out.println("evidence is " + evidence);
        
        long start_time = System.nanoTime();
        double threshold_string = Double.parseDouble(args[1]);
        SDP sdp = new SDP(main,evidence,pwriter, threshold_string, args[2]);
        //THRESHOLD SETTER
        int index = domain.index(sdp.TARGET_ST);

        sdp.computeSDP();
        
        if (DEBUG) {
            pwriter.println("DEBUG INFO:\n==================================");
            pwriter.println("query variables are: " + sdp.hvars);
            pwriter.println("decision variable is: " + sdp.dvar);
            pwriter.println("evidence variables are: " + sdp.evars);
            pwriter.println("intermediary variables are: " + sdp.ivars);
            for(int i=0; i<sdp.ivars.size(); i++)
                pwriter.println(domain.name(sdp.ivars.get(i)));
            pwriter.println("Domain information");
            pwriter.println("==========================");
            for (int i = 0; i < domain.size(); i++)
                pwriter.println(i + ":" +domain.name(i));
            pwriter.println("current weight is at " + sdp.current);
            pwriter.println("threshold is " + sdp.lg_threshold);
            pwriter.println("prior log odds is " + sdp.prior);
        }


        long total_time = System.nanoTime()- start_time;
        double seconds = (double) total_time / 1000000000.0;
        pwriter.println("Total time is " + seconds + " seconds.");*/
    }

}
