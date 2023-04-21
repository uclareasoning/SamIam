package il2.model;

import java.util.*;
import edu.ucla.util.*;
import il2.util.*;

public class MPETable extends Table
{
    protected MPE[] mpeInstantiations;

    public static MPETable createCompatibleMPE( Index index ){
        return new MPETable(index.domain(), index.vars());
    }

    public MPETable(Domain d, IntSet vars)
    {
        super(d, vars);
        mpeInstantiations = new MPE[this.sizeInt()];
        for (int i = 0; i < mpeInstantiations.length; i++)
            mpeInstantiations[i] = new MPE();
    }

    public MPETable(Index index, double[] vals)
    {
        super(index, vals);
        mpeInstantiations = new MPE[this.sizeInt()];
        for (int i = 0; i < mpeInstantiations.length; i++)
            mpeInstantiations[i] = new MPE();
    }

    public MPETable(Index index, double[] vals, MPE[] mpeInsts)
    {
        super(index, vals);
        mpeInstantiations = mpeInsts;
    }

    public MPE[] mpeInstantiations()
    {
        return mpeInstantiations;
    }

    public static MPETable constantMPETable( Domain d, double val )
    {
        MPETable t = new MPETable(d, new IntSet(new int[]{}));
        t.values[0] = val;
        return t;
    }

    public static MPETable varMPETable( Domain d, int var ) {
        MPETable t = new MPETable(d, new IntSet(new int[]{var}));
        return t;
    }

    public static MPETable evidenceMPETable( Domain d, int var, int evVal )
    {
        MPETable t = new MPETable(d, new IntSet(new int[]{var}));
        t.values[evVal] = 1;
        return t;
    }

/*
    public static MPETable parameterMPETable(Domain d, Table cpt)
    {
        MPETable t = new MPETable(d, cpt.vars());
        t.values = (double[])cpt.values().clone();
        for (int i = 0; i < t.mpeInstantiations.length; i++)
        {
            t.mpeInstantiations[i] = new MPE();
        }
        return t;
    }
*/

    public void multiplyAndProjectInto(Table[] tables)
    {
        java.util.Arrays.fill(values,0);
        double[][] vals=new double[tables.length][];
        MPE[][] mpeInsts = new MPE[tables.length][];
        for(int i=0;i<vals.length;i++){
            vals[i]=tables[i].values;
            mpeInsts[i]=((MPETable)tables[i]).mpeInstantiations;
        }
        List wholeThing=new ArrayList(tables.length+1);
        wholeThing.addAll(Arrays.asList(tables));
        wholeThing.add(this);
        Index big=Index.createBigIndex(wholeThing);
        int[][] fc=new int[vals.length][];
        for(int i=0;i<tables.length;i++){
            fc[i]=tables[i].flipChange(big);
        }
        int[] destFc=flipChange(big);
        int destInd=0;
        int[] current=new int[big.vars().size()];
        int[] inds=new int[vals.length];
        int bound=big.sizeInt()-1;
        for(int i=0;i<bound;i++){
           double v=1;
           MPE mpe = new MPE();
            for(int j=0;j<vals.length;j++){
                v*=vals[j][inds[j]];
                mpe = MPE.merge(mpe, mpeInsts[j][inds[j]]);
            }
            if (values[destInd] < v)
            {
                values[destInd] = v;
                mpeInstantiations[destInd] = mpe;
            }
            else if (values[destInd] == v)
            {
                mpeInstantiations[destInd].addAll(mpe);
            }
            // values[destInd]+=v;
            int f=big.next(current);
            for(int j=0;j<inds.length;j++){
                inds[j]+=fc[j][f];
            }
            destInd+=destFc[f];
        }
        double v=1;
        MPE mpe = new MPE();
        for(int j=0;j<vals.length;j++){
            v*=vals[j][inds[j]];
            mpe = MPE.merge(mpe, mpeInsts[j][inds[j]]);
        }
        if (values[destInd] < v)
        {
            values[destInd] = v;
            mpeInstantiations[destInd] = mpe;
        }
        else if (values[destInd] == v)
        {
            mpeInstantiations[destInd].addAll(mpe);
        }
        // values[destInd]+=v;
    }
}
