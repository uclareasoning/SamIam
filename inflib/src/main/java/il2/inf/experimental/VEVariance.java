package il2.inf.experimental;

import edu.ucla.belief.BeliefNetwork;
import il2.model.BayesianNetwork;
import il2.model.Domain;
import il2.model.Table;
import il2.util.IntList;
import il2.util.IntMap;
import il2.util.IntSet;
import il2.inf.structure.EliminationOrders;

import java.io.PrintWriter;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Arrays;

public class VEVariance {
    BayesianNetwork bn;
    IntMap evidence;
    // Random myr;
    PrintWriter pw;


    /* for testing purposes, certain parameters are hard-coded */
    public String HEALTH_ST = "health";
    public String TARGET_ST = "A";
    public int TARGET_STATE = 0;

    public VEVariance(BayesianNetwork bn, IntMap evidence) {
        this(bn,evidence,null);
    }

    public VEVariance(BayesianNetwork bn, IntMap evidence, PrintWriter pw) {
        this.bn = bn;
        this.evidence = evidence;
        this.pw = pw == null ? new java.io.PrintWriter(System.out,true) : pw;
    }

    /**
     * this method is intended for use with SamIam
     *
     * expecting evidence as Map of FiniteVariable to Object
     */
    public static void manual_variance(BeliefNetwork bn, Map evidence,
                                       PrintWriter pw) {
        il2.bridge.Converter c = new il2.bridge.Converter();
        BayesianNetwork bnx = c.convert(bn);
        IntMap evidencex = c.convert(evidence);
        manual_variance(bnx,evidencex,pw);
    }

    /**
     * this method is intended for use with SamIam
     */
    public static void manual_variance(BayesianNetwork bn, IntMap evidence,
                                       PrintWriter pw) {
        VEVariance vevar = new VEVariance(bn,evidence,pw);
        IntSet hvars = vevar.findHealthVariables();
        int dvar = vevar.findTargetVariable();
        if ( ! vevar.variablesOk(hvars,dvar) ) return;
        IntList order = vevar.getConstrainedOrder(hvars,dvar);
        vevar.printInformation(hvars,dvar,order);
        VarianceInfo info = vevar.ve_var(bn,evidence,hvars,dvar);
        vevar.printVarianceInformation(info);
    }

    public static void manual_variance(BeliefNetwork bn, Map evidence) {
        manual_variance(bn,evidence,null);
    }
    public static void manual_variance(BayesianNetwork bn, IntMap evidence) {
        manual_variance(bn,evidence,null);
    }

    public void printInformation(IntSet hvars, int dvar, IntList order) {
        Domain d = bn.domain();
        pw.println("================================");
        pw.println("Target Variable:\n  " + d.name(dvar));
        pw.println("Health Variables:");
        for (int i = 0; i < hvars.size(); i++) {
            int var = hvars.get(i);
            pw.println("  " + d.name(var));
        }
        /*
        pw.print("Constrained Order:");
        for (int i = 0; i < order.size(); i++)
            pw.print(" " + d.name(order.get(i)));
        pw.println();
        */
        java.util.Collection subdomains = Arrays.asList(bn.cpts());
        double width = EliminationOrders.computeSize(subdomains,order);
        pw.printf("Constrained Width: %.2f\n",width);
    }

    public void printVarianceInformation(VarianceInfo info) {
        pw.println("================================");
        pw.printf("    Pr(e) = %.6e\n",info.pre);
        pw.printf(" ex[Q(H)] = %.6e\n",info.mu);
        pw.printf("var[Q(D)] = %.6e\n",info.mu*(1-info.mu));
        pw.printf("var[Q(H)] = %.6e\n",info.variance);
        pw.printf("std[Q(H)] = %.6e\n",Math.sqrt(info.variance));
        pw.println("================================");

        String le = "\u2264", ge = "\u2265";
        // String le = "<=", ge = ">=";
        for (int i = 0; i <= 10; i++) {
            double T = i/10.0;
            if ( T-0.1 < info.mu && info.mu <= T ) 
                pw.printf("          mu = %.6f\n", info.mu);
            String ineq = T <= info.mu ? le : ge;
            double a = info.mu - T;
            double conf = info.variance/(info.variance + a*a);
            pw.printf("Pr( %s %s %3.1f ) %s %.6f",TARGET_ST,ineq,T,le,conf);
            pw.printf(" [%.6f]\n",1-conf);
        }
        pw.println("================================");
    }

    public boolean variablesOk(IntSet hvars, int dvar) {
        if ( hvars.isEmpty() ) {
            String msg1 = "warning: no health variables; expect 0.0 variance";
            String msg2 = "warning: expecting health variables with prefix '" +
                HEALTH_ST + "'";
            pw.println(msg1);
            pw.println(msg2);
        }
        try { bn.domain().name(dvar); }
        catch ( ArrayIndexOutOfBoundsException e )  {
            String msg1 = "error: unknown/bad target variable";
            String msg2 = "error: expecting variable name '" + TARGET_ST + "'";
            pw.println(msg1);
            pw.println(msg2);
            return false;
        }
        return true;
    }

	//////////////////////////////////////////////////
	// misc. stuff
	//////////////////////////////////////////////////

	double timertime;
	public void startTimer() {timertime = System.nanoTime();}
	public double peekTimer() { return (System.nanoTime()-timertime)*1e-6; }
	public void stopTimer(String prefix) {
		pw.printf(prefix + " (%.2fms)",peekTimer());
	}
	public void stopTimerLn(String prefix) {
		stopTimer(prefix);
		pw.println();
	}

	//////////////////////////////////////////////////
	// stuff
	//////////////////////////////////////////////////

    public void setHealthString(String st) { HEALTH_ST = st; }
    public void setTargetString(String st) { TARGET_ST = st; }
    public void setTargetState(int st) { TARGET_STATE = st; }

    public IntSet findHealthVariables() {
        Domain d = bn.domain();
        IntSet vars = new IntSet();

        for (int var = 0; var < d.size(); var++) {
            String name = d.name(var);
            if ( name.startsWith(HEALTH_ST) )
                vars.add(var);
        }
        return vars;
    }

    public int findTargetVariable() {
        Domain d = bn.domain();

        for (int var = 0; var < d.size(); var++) {
            String name = d.name(var);
            if ( name.equals(TARGET_ST) )
                return var;
        }
        return -1;
    }

    public IntList getConstrainedOrder(IntSet hvars, int dvar) {
        IntSet vars = new IntSet(hvars);
        vars.add(dvar);
		java.util.Collection subdomains = Arrays.asList(bn.cpts());
        EliminationOrders.Record rec =
            EliminationOrders.constrainedMinFill(subdomains,vars);
        return rec.order;
    }

    public IntList[] partitionOrder(IntList order, IntSet hvars, int dvar) {
        IntList[] orders = new IntList[3];
        int size = order.size() - hvars.size() - 1;

        orders[0] = new IntList(size);
        for (int i = 0; i < size; i++)
            orders[0].add(order.get(i));
        orders[1] = new IntList(hvars.size()+1);
        orders[2] = new IntList(hvars.size());
        for (int i = size; i < order.size(); i++) {
            int var = order.get(i);
            orders[1].add(var);
            if ( var != dvar )
                orders[2].add(var);
        }

        return orders;
    }

    public HashSet<Table> varianceTables(HashSet<Table> fset, int dvar) {
        Table lambda = Table.evidenceTable(bn.domain(),dvar,TARGET_STATE);

        HashSet<Table> gset = new HashSet<Table>();
        HashSet<Table> dset = new HashSet<Table>();
        IntSet vars = new IntSet();
        for ( Table t : fset ) {
            IntSet tvars = t.vars();
            if ( tvars.contains(dvar) ) {
                dset.add(t);
                vars = vars.union(tvars);
            } else
                gset.add(t);
        }
        vars.remove(dvar);
        Table g = new Table(bn.domain(),vars);
        g.multiplyAndProjectInto(dset.toArray(new Table[]{}));
        Table d = new Table(bn.domain(),vars);
        dset.add(lambda);
        d.multiplyAndProjectInto(dset.toArray(new Table[]{}));
        g = g.invert();
        Table t = d.multiply(d).multiply(g);
        gset.add(t);
        return gset;
    }

	//////////////////////////////////////////////////
	// variable elimination
	//////////////////////////////////////////////////

	public HashSet<Table> ve(HashSet<Table> fset, IntList order) {
		startTimer();
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

    public Table ve_d(BayesianNetwork bn, IntMap evidence, int dvar) {
		Table[] cpts = bn.cpts();
        cpts = Table.shrink(cpts,evidence);
		HashSet<Table> fset = new HashSet<Table>( Arrays.asList(cpts) );

        IntList order = getConstrainedOrder(new IntSet(), dvar);
        order.removeLast();
        HashSet<Table> gset = ve(fset,order);
        Table mar = Table.multiplyAll(gset);
        return mar;
    }

    public VarianceInfo ve_var(BayesianNetwork bn, IntMap evidence,
                               IntSet hvars, int dvar) {
		Table[] cpts = bn.cpts();
        cpts = Table.shrink(cpts,evidence);
		HashSet<Table> fset = new HashSet<Table>( Arrays.asList(cpts) );
        IntList order = getConstrainedOrder(hvars, dvar);
        IntList[] orders = partitionOrder(order, hvars, dvar);

        HashSet<Table> gset = ve(fset,orders[0]);
        HashSet<Table> gsetp = new HashSet<Table>(gset);
        gsetp = ve(gsetp,orders[2]);
        Table mar = Table.multiplyAll(gsetp);
        gsetp = varianceTables(gset,dvar);
        gsetp = ve(gsetp,orders[1]);
        Table var = Table.multiplyAll(gsetp);

        double pre = mar.sum();
        double mu = mar.values()[TARGET_STATE]/pre;
        double sumh = var.values()[0];
        double variance = sumh/pre - mu*mu;

        return new VarianceInfo(pre,mu,sumh,variance);
    }
}

class BN {
    public BayesianNetwork bn;
    public IntMap e;
    public BN(BayesianNetwork bn, IntMap e) {
        this.bn = bn;
        this.e = e;
    }
}

class VarianceInfo {
    public double pre, mu, sumh, variance;
    public VarianceInfo(double pre, double mu, double sumh, double variance) {
        this.pre = pre;
        this.mu = mu;
        this.sumh = sumh;
        this.variance = variance;
    }
}
