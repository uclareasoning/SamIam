package il2.inf.map;
import il2.util.*;
import il2.model.*;
import il2.inf.structure.*;
import java.util.*;
/**
 *
 * @author  jdpark
 */
public class MapJoinTree {
    private int root;
    private Domain domain;
    private IntMap evidenceIndicators;
    private Cluster[] clusters;
    private IntSet mapvars;
    private double[] scratch;
    private double[] smallScratch;

    private IntSet topTierVars;

    /** Creates a new instance of MapJoinTree */
    public MapJoinTree(EliminationOrders.JT jt, int initialRoot,Collection cpts, IntSet mapvars) {
        //sanityCheckJoinTree(jt);
        root=jt.tree.addNew();
        jt.tree.addEdge(root,initialRoot);
        jt.clusters.put(new Integer(root), new IntSet());
        //sanityCheckJoinTree(jt);
        this.mapvars=mapvars;
        domain=((Table)cpts.iterator().next()).domain();
        IntMap depth=computeDepth(jt,root);
        IntMap firstMention=computeFirstMention(jt,root);
        Map cptAssignments=augmentWithCpts(jt,depth,firstMention,cpts);
        //sanityCheckJoinTree(jt);
        evidenceIndicators=augmentWithIndicators(jt,firstMention,mapvars);
        //sanityCheckJoinTree(jt);
        clusters=new Cluster[jt.tree.vertices().largest()+1];
        new Cluster(root,jt);
        setCPTMessages(cptAssignments);
        setEvidenceIndicators();
        setRemainingLeafMessages(jt);
        scratch=new double[largestClusterSize()];
        smallScratch=new double[scratch.length/2+1];
        computeTopTierVars();

    }

    private void sanityCheckJoinTree(EliminationOrders.JT jt){
        if(!jt.tree.isTree()){
            throw new IllegalStateException();
        }
        Map varLocs=computeVarLocs(jt.clusters);
        for(Iterator iter=varLocs.keySet().iterator();iter.hasNext();){
            Object v=iter.next();
            IntSet locs=(IntSet)varLocs.get(v);
            if(!jt.tree.isConnected(locs)){
                throw new IllegalStateException();
            }
        }
    }



    private Map computeVarLocs(Map clusters){
        Map result=new HashMap(100);
        for(Iterator iter=clusters.keySet().iterator();iter.hasNext();){
            Integer node=(Integer)iter.next();
            IntSet vars=(IntSet)clusters.get(node);

            for(int i=0;i<vars.size();i++){
                Integer var=new Integer(vars.get(i));
                IntSet varLocs=(IntSet)result.get(var);
                if(varLocs==null){
                    varLocs=new IntSet();
                    result.put(var, varLocs);
                }
                varLocs.add(node.intValue());
            }
        }
        return result;
    }

    public Domain domain(){
        return domain;
    }
    private void computeTopTierVars(){
        topTierVars=new IntSet();
        int neighbor=clusters[root].neighbors[0];
        computeTopTierVars(neighbor,root);
    }

    private void computeTopTierVars(int node,int parent){
        IntSet mv=clusters[node].ind.vars().intersection(mapvars);
        topTierVars=topTierVars.union(mv);
        if(mv.size()==clusters[node].ind.vars().size()){
            int[] neighbors=clusters[node].neighbors;
            for(int i=0;i<neighbors.length;i++){
                if(neighbors[i]!=parent){
                    computeTopTierVars(neighbors[i],node);
                }
            }
        }
    }


    public double getValue(){
        computeMessage(clusters[root].neighbors[0], root);
        return clusters[root].inMessages[0][0];
    }

    public double[] getIntoMessage(int var){
        int vert=evidenceIndicators.get(var);
        computeMessage(clusters[vert].neighbors[0], vert);
        return (double[])clusters[vert].inMessages[0].clone();
    }

    public void setLikelihood(int var,double[] lik){
        int vert=evidenceIndicators.get(var);
        invalidate(vert);
        if(clusters[vert].neighbors.length!=1){
            throw new IllegalStateException();
        }
        int n=clusters[vert].neighbors[0];
        int vind=clusters[vert].myInd[0];
        double[] message=clusters[n].inMessages[vind];
        if(lik.length!=message.length){
            throw new IllegalStateException();
        }
        System.arraycopy(lik,0, message,0, message.length);
        clusters[n].inMessageIsValid[vind]=true;
    }

    public double[] getLikelihood(int var){
        int vert=evidenceIndicators.get(var);
        int n=clusters[vert].neighbors[0];
        int vind=clusters[vert].myInd[0];
        return (double[])clusters[n].inMessages[vind].clone();
    }

    private int largestClusterSize(){
        int result=1;
        for(int i=0;i<clusters.length;i++){
            if(clusters[i]!=null && clusters[i].ind.sizeInt()>result){
                result=clusters[i].ind.sizeInt();
            }
        }
        return result;
    }
    private void setRemainingLeafMessages(EliminationOrders.JT jt){
        IntSet leaves=jt.tree.treeLeaves();
        for(int i=0;i<leaves.size();i++){
            int leaf=leaves.get(i);
            if(clusters[leaf].neighbors.length!=1){
                throw new IllegalStateException();
            }
            int neighbor=clusters[leaf].neighbors[0];
            int nind=clusters[leaf].myInd[0];
            if(!clusters[neighbor].inMessageIsValid[nind]){

                double[] message=clusters[neighbor].inMessages[nind];
                java.util.Arrays.fill(message,1);
                clusters[neighbor].inMessageIsValid[nind]=true;
            }
        }
    }
    private void setEvidenceIndicators(){
        for(int i=0;i<mapvars.size();i++){
            int v=mapvars.get(i);
            int node=evidenceIndicators.get(v);
            if(clusters[node].neighbors.length>1){
                throw new IllegalStateException();
            }
            int neighbor=clusters[node].neighbors[0];
            double[] message=clusters[neighbor].inMessages[clusters[node].myInd[0]];
            java.util.Arrays.fill(message,1);

            clusters[neighbor].inMessageIsValid[clusters[node].myInd[0]]=true;
        }
    }

    private void setCPTMessages(Map cptAssignments){
        for(Iterator iter=cptAssignments.keySet().iterator();iter.hasNext();){
            Table t=(Table)iter.next();
            int node=((Integer)cptAssignments.get(t)).intValue();
            if(clusters[node].neighbors.length>1){
                throw new IllegalStateException();
            }
            int neighbor=clusters[node].neighbors[0];

            double[] message=clusters[neighbor].inMessages[clusters[node].myInd[0]];
            if(message.length==t.sizeInt()){
                System.arraycopy(t.values(),0,message,0, message.length);
            }else{
                IntSet eliminated=clusters[node].ind.vars().diff(clusters[neighbor].ind.vars());
                Table smaller=t.forget(eliminated);
                if(message.length!=smaller.sizeInt()){
                    throw new IllegalStateException();
                }
                System.arraycopy(smaller.values(),0,message, 0,message.length);
            }
            clusters[neighbor].inMessageIsValid[clusters[node].myInd[0]]=true;
        }
    }



    private IntMap augmentWithIndicators(EliminationOrders.JT jt,IntMap firstMention,IntSet mapvars){
        IntMap result=new IntMap(mapvars.size());
        for(int i=0;i<mapvars.size();i++){
            int v=mapvars.get(i);
            int neighbor=firstMention.get(v);
            int vert=jt.tree.addNew();
            jt.tree.addEdge(vert,neighbor);
            jt.clusters.put(new Integer(vert), IntSet.singleton(v));
            result.put(v,vert);
        }
        return result;
    }
    private Map augmentWithCpts(EliminationOrders.JT jt,IntMap depth,IntMap firstMention, Collection cpts){
        Map result=new HashMap(100);
        int deepest=root;
        int deepestDepth=depth.get(root);
        for(int i=0;i<depth.size();i++){
            if(depth.values().get(i)>deepestDepth){
                deepestDepth=depth.values().get(i);
                deepest=depth.keys().get(i);
            }
        }
        for(Iterator iter=cpts.iterator();iter.hasNext();){
            Table t=(Table)iter.next();
            int neighbor=findBestLocation(jt,t.vars(),depth,firstMention,deepest);
            IntSet neighborVars=(IntSet)jt.clusters.get(new Integer(neighbor));
            int vert=jt.tree.addNew();
            jt.tree.addEdge(vert,neighbor);
            jt.clusters.put(new Integer(vert),new IntSet(t.vars()));
            result.put(t, new Integer(vert));
        }
        return result;
    }


    private int findBestLocation(EliminationOrders.JT jt,IntSet vars,IntMap depth,IntMap firstMention,int deepest){
        if(vars.size()==0){
            return deepest;
        }

        int bestDepth=-1;
        int bestNode=-1;
        for(int i=0;i<vars.size();i++){
            int n=firstMention.get(vars.get(i));
            int d=depth.get(n);
            if(d>=bestDepth){
                bestDepth=d;
                bestNode=n;
            }
        }

        boolean improved;
        do{
            improved=false;
            IntSet neighbors=jt.tree.neighbors(bestNode);
            for(int i=0;i<neighbors.size();i++){
                int n=neighbors.get(i);
                int d=depth.get(n,-1);
                if(d<bestDepth){
                    continue;
                }
                IntSet nvars=(IntSet)jt.clusters.get(new Integer(n));
                if(nvars.containsAll(vars)){
                    bestNode=n;
                    bestDepth=d;
                    improved=true;
                    break;
                }
            }
        }while(improved);
        return bestNode;
    }


    private IntMap computeFirstMention(EliminationOrders.JT jt,int root){
        IntMap result=new IntMap(100);
        IntSet cluster=(IntSet)jt.clusters.get(new Integer(root));
        for(int i=0;i<cluster.size();i++){
            result.put(cluster.get(i),root);
        }
        IntSet neighbors=jt.tree.neighbors(root);
        for(int i=0;i<neighbors.size();i++){
            computeFirstMention(jt,neighbors.get(i),root,result);
        }
        return result;
    }
    private void computeFirstMention(EliminationOrders.JT jt,int node, int parent, IntMap result){
        IntSet cluster=(IntSet)jt.clusters.get(new Integer(node));
        IntSet firsts=cluster.diff(result.keys());
        for(int i=0;i<firsts.size();i++){
            result.put(firsts.get(i),node);
        }
        IntSet neighbors=jt.tree.neighbors(node);
        for(int i=0;i<neighbors.size();i++){
            int n=neighbors.get(i);
            if(n!=parent){
                computeFirstMention(jt,n,node,result);
            }
        }
    }
    private IntMap computeDepth(EliminationOrders.JT jt,int root){
        IntMap result=new IntMap(100);
        IntSet neighbors=jt.tree.neighbors(root);
        result.put(root, 0);
        for(int i=0;i<neighbors.size();i++){
            computeDepth(jt,neighbors.get(i),root,result);
        }
        return result;
    }

    private void computeDepth(EliminationOrders.JT jt,int node,int parent,IntMap result){
        result.put(node, 1+result.get(parent));
        IntSet neighbors=jt.tree.neighbors(node);
        for(int i=0;i<neighbors.size();i++){
            int n=neighbors.get(i);
            if(n!=parent){
                computeDepth(jt,n,node,result);
            }
        }
    }


    private class Cluster{
        private static final int COPY=0;
        private static final int BOTH=1;
        private static final int MAXONLY=2;
        private static final int SUMONLY=3;
        Index ind;
        int me;
        int[] neighbors;
        int[] myInd;
        int[][][] sumOutMaps;
        int[][][] maximizeMaps;
        int[][][] fullMaps;
        double[][] inMessages;
        int[] messageTypes;
        boolean[] inMessageIsValid;

        private Cluster(int node,EliminationOrders.JT jt){
            me=node;
            ind=new Index(domain,(IntSet)jt.clusters.get(new Integer(node)));

            neighbors=jt.tree.neighbors(node).toArray();
            myInd=new int[neighbors.length];
            sumOutMaps=new int[neighbors.length][][];
            maximizeMaps=new int[neighbors.length][][];
            fullMaps=new int[neighbors.length][][];
            inMessages=new double[neighbors.length][];
            messageTypes=new int[neighbors.length];
            inMessageIsValid=new boolean[neighbors.length];
            clusters[node]=this;
            for(int i=0;i<neighbors.length;i++){
                if(clusters[neighbors[i]]==null){
                    new Cluster(neighbors[i],jt);
                }
            }
            for(int i=0;i<neighbors.length;i++){
                Index separator=ind.separatorIndex(clusters[neighbors[i]].ind);
                IntSet eliminated=ind.vars().diff(separator.vars());
                IntSet maxEliminated=eliminated.intersection(mapvars);
                IntSet sumEliminated=eliminated.diff(maxEliminated);

                if(eliminated.size()==0){
                    messageTypes[i]=COPY;
                }else if(maxEliminated.size()==0){
                    messageTypes[i]=SUMONLY;
                }else if(sumEliminated.size()==0){
                    messageTypes[i]=MAXONLY;
                }else{
                    messageTypes[i]=BOTH;
                    Index sumElimInd=ind.forgetIndex(sumEliminated);
                    sumOutMaps[i]=ind.baselineOffsetIndex(sumElimInd);
                    maximizeMaps[i]=sumElimInd.baselineOffsetIndex(separator);
                }
                fullMaps[i]=ind.baselineOffsetIndex(separator);
                inMessages[i]=new double[separator.sizeInt()];
                myInd[i]=java.util.Arrays.binarySearch(clusters[neighbors[i]].neighbors,node);
            }

        }

        private void invalidateOthers(int from){
            for(int i=0;i<neighbors.length;i++){
                int n=neighbors[i];
                if(n!=from){
                    int mind=myInd[i];
                    clusters[n].inMessageIsValid[mind]=false;
                    clusters[n].invalidateOthers(me);
                }
            }
        }

        private void computeMessageTo(int to){
            if(me==to){
                throw new IllegalArgumentException();
            }
            for(int i=0;i<neighbors.length;i++){
                if(to!=neighbors[i] && !inMessageIsValid[i]){
                    clusters[neighbors[i]].computeMessageTo(me);
                }
            }
            java.util.Arrays.fill(scratch,0,ind.sizeInt(),1.0);
            int toInd=-1;
            for(int i=0;i<neighbors.length;i++){
                int n=neighbors[i];
                if(n==to){
                    toInd=i;
                }else{
                    Table.multiplyInto(inMessages[i], scratch, fullMaps[i]);
                }
            }


            int mind=myInd[toInd];
            double[] message=clusters[to].inMessages[mind];
            if(messageTypes[toInd]==COPY){
                System.arraycopy(scratch, 0, message, 0,message.length);
            }else if(messageTypes[toInd]==BOTH){
                Table.projectInto(scratch,smallScratch, sumOutMaps[toInd]);
                Table.maximizeInto(smallScratch, message, maximizeMaps[toInd]);
            }else if(messageTypes[toInd]==MAXONLY){
                Table.maximizeInto(scratch, message, fullMaps[toInd]);
            }else{
                Table.projectInto(scratch, message, fullMaps[toInd]);
            }
            clusters[to].inMessageIsValid[mind]=true;
        }

    }

    private void invalidate(int from,int to){
        clusters[to].invalidateOthers(from);
    }
    private void invalidate(int node){
        int[] neighbors=clusters[node].neighbors;
        for(int i=0;i<neighbors.length;i++){
            invalidate(node,neighbors[i]);
        }
    }
    private void computeMessage(int from,int to){
        int inInd=java.util.Arrays.binarySearch(clusters[to].neighbors,from);
        if(!clusters[to].inMessageIsValid[inInd]){
            clusters[from].computeMessageTo(to);
        }
    }

}
