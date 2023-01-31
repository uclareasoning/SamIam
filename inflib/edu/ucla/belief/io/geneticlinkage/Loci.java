package edu.ucla.belief.io.geneticlinkage;

import edu.ucla.util.*;

import java.io.*;
import java.util.*;

public class Loci
{
    private static boolean DEBUG_Loci = false;

    protected Loci() {};


    Header hdr;
    Locus loci[]; //input order
    Footer ftr;
    int diseaseLocus = 0;  //Programs other than MLink=5 will change this.


	public Header header() { return hdr;}
	public Locus loci( int i) { return loci[i];}
	public Footer footer() { return ftr;}

	/** Return 0 based Chromosome Order of disease locus.*/
	public int diseaseLocusChromoOrd() { return diseaseLocus;}
	/** Return 0 based Input Order of disease locus.*/
	public int diseaseLocusInputOrd() { return hdr.chromosomeOrder[diseaseLocus]-1;}


    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append( "" + hdr);
        ret.append( ObjArrays.convertToString( loci) + "\n");
        ret.append( "" + ftr);
        return ret.toString();
    }

    /** Returns the 0 based Chromosome Order of the recom locus where the cpt of s will change
     *   (e.g. if between A&B, will return B).  This function returns the left most changing locus
     *   the next one in Chromosome Order may also be changing.*/
    public int getChromoOrd_ChangingS1() {
	    int ret = ftr.progData.getInputOrd_ChangingS1();
	    if(ret>0) ret = hdr.inputToChromOrder[ret-1];
	    else ret = -1;
	    return ret;
    }
    /** Returns the 0 based Chromosome Order of the recom locus where the cpt of s will change
     *   (e.g. if between A&B, will return B).  This function returns the 2nd (right) locus if more
     *   than one are changing.*/
    public int getChromoOrd_ChangingS2() {
	    int ret = ftr.progData.getInputOrd_ChangingS2();
	    if(ret>0) ret = hdr.inputToChromOrder[ret-1];
	    else ret = -1;
	    return ret;
    }



    static public Loci readLoci( String file)
    throws IOException {
        return readLoci( new BufferedReader( new FileReader( file)));
    }

    static public Loci readLoci( File file)
    throws IOException {
        return readLoci( file.getCanonicalPath());
    }

    static public Loci readLoci( BufferedReader in)
    throws IOException {
        Loci ret = new Loci();
        ret.hdr = Header.readHeader( in);
        ret.loci = new Locus[ret.hdr.numLoci];
        int numComplexAlreadySeen = 0;
        int firstComplexLociInputOrd = -1; //0..numLoci-1
        for( int i=0; i<ret.loci.length; i++) {
            ret.loci[i] = Locus.readLocus( in, ret.hdr, numComplexAlreadySeen);
            if(ret.loci[i].type==4) {
				numComplexAlreadySeen++;
				if(firstComplexLociInputOrd<0) {firstComplexLociInputOrd=i;}
				else {((LocusDataComplexAffection2)ret.loci[i].data).firstComplexLociInputOrd = firstComplexLociInputOrd;}
			}
        }
        ret.ftr = Footer.readFooter( in, ret.hdr.numLoci, ret.hdr.program);
        return ret;
    }


	static private String readLine(BufferedReader in)
	throws IOException {
		String ln = in.readLine();
		if(ln!=null) {
			ln=ln.trim();
			if(ln.length()==0) { ln=null;}
		}
		return ln;
	}


    static public class Header {
        public int numLoci;
        public int riskLoci;//not presently used
        public int sexLinked;
        public int program;
        public int numComplexAffection;
        public int chromosomeOrder[]; //values 1..length
        public int inputToChromOrder[]; //values 0..length-1

        private Header(){}

        static public Header readHeader( BufferedReader in)
        throws IOException {
            Header ret = new Header();

            String ln = readLine(in);
            if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
            StringBuffer line = new StringBuffer(ln);

            //Line 1
            ret.numLoci = UtilGeneReader.readIntExpected( line);
            ret.riskLoci = UtilGeneReader.readIntExpected( line);
            ret.sexLinked = UtilGeneReader.readIntExpected( line);
            ret.program = UtilGeneReader.readIntExpected( line);
            ret.numComplexAffection = UtilGeneReader.readInt( line);
            if( ret.numComplexAffection == Integer.MIN_VALUE) { ret.numComplexAffection = 0;} //possibly not there

            //Line 2  (skip it)
            ln = readLine(in);
            if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
            line = null;

            //Line 3
            ln = readLine(in);
            if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
            line = new StringBuffer(ln);

            ret.chromosomeOrder = readChromosomeOrder( line);//1..numLoci
            ret.inputToChromOrder = convertChromOrder( ret.chromosomeOrder);//0..numLoci-1


            if( !ret.isValid()) { throw new IllegalArgumentException("Invalid Locus File: read but not valid: " + ret.toString());}
            return ret;
        }


        private boolean isValid() {
            if( numLoci <= 1) { return false;}
            if( riskLoci < 0 || riskLoci >= numLoci) { return false;}
            if( sexLinked < 0 || sexLinked > 1) { return false;}
            if( program < 4 || program > 13) { return false;}
            if( numComplexAffection != 0 && numComplexAffection != 2) { return false;}
            if( numLoci != chromosomeOrder.length) { return false;}

            return true;
        }

        public String toString() {
            StringBuffer ret = new StringBuffer();
            ret.append("Locus Header {\n");
            ret.append(" numLoci = " + numLoci + "\n");
            ret.append(" riskLoci = " + riskLoci + "\n");
            ret.append(" sexLinked = " + sexLinked + "\n");
            ret.append(" program = " + program + "\n");
            ret.append(" numComplexAffection = " + numComplexAffection + "\n");
            ret.append(" order = " + IntArrays.convertToString(chromosomeOrder) + "\n");
            ret.append("} Locus Header\n");
            return ret.toString();
        }

        /** Read in a line of integers and return them in an array.*/
        static private int[] readChromosomeOrder( StringBuffer line)
        throws IOException {

            int ret[] = new int[line.length()]; //over estimate!

            int n = UtilGeneReader.readInt( line);
            int nextIndx = 0;
            while ( n > Integer.MIN_VALUE) {
                ret[nextIndx] = n;
                nextIndx++;
                n = UtilGeneReader.readInt( line);
            }
            int ret_old[] = ret;
            ret = new int[nextIndx]; //make correct size
            System.arraycopy( ret_old, 0, ret, 0, ret.length);
            return ret;
        }

        /** ChromOrder is expected to contain the numbers 1..length and the resulting
         *  array will contain the numbers 0..length-1.
         */
        static private int[] convertChromOrder( int chromOrder[]) {
            int ret[] = new int[chromOrder.length];
            Arrays.fill( ret, -1);

            for( int i=0; i<ret.length; i++) {
                ret[chromOrder[i]-1] = i;
            }

            //verify all were set correctly
            for( int i=0; i<ret.length; i++) {
                if( ret[i] < 0 || ret[i] > ret.length-1) { throw new IllegalArgumentException("invalid chromosome ordering: " +
                                                           IntArrays.convertToString( chromOrder));}
            }
            return ret;
        }


    } //end Header




    static public class Locus {
        int type;
        int numAlleles;
        LocusData data;

        private Locus(){}

        static public Locus readLocus( BufferedReader in, Header hdr, int numComplexAlreadySeen)
        throws IOException {
            Locus ret = new Locus();

            String ln = readLine(in);
            if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
            StringBuffer line = new StringBuffer(ln);

            ret.type = UtilGeneReader.readIntExpected( line);
            ret.numAlleles = UtilGeneReader.readIntExpected( line);

            if( ret.type == 0 && ret.numAlleles == 0) { throw new IllegalArgumentException("Invalid Locus File: read in 0,0");}
            if( ret.type == 1) {
                ret.data = LocusDataAffectionStatus.read(in, ret.numAlleles, hdr.sexLinked);
            }
            else if( ret.type == 3) {
                ret.data = LocusDataNumberedAlleles.read(in, ret.numAlleles, hdr.sexLinked);
            }
            else if( ret.type == 4) {
				if(numComplexAlreadySeen==0) {
	                ret.data = LocusDataComplexAffection1.read(in, ret.numAlleles, hdr.sexLinked);
				}
				else if(numComplexAlreadySeen==1) {
	                ret.data = LocusDataComplexAffection2.read(in, ret.numAlleles, hdr.sexLinked);
				}
				else {
					throw new IllegalStateException("Found a third complex locus");
				}
            }
            else { throw new IllegalArgumentException("Illegal type: " + ret.type);}
            return ret;
        }

        public String toString() {
            StringBuffer ret = new StringBuffer();
            ret.append("Locus {\n");
            ret.append(" type = " + type + "\n");
            ret.append(" numAlleles = " + numAlleles + "\n");
            ret.append( data.toString() + "\n");
            ret.append("} Locus\n");
            return ret.toString();
        }
    } //end Locus


    abstract static public class LocusData {
        double geneFreq[];
        protected LocusData(){}
        public String toString() {
            return "geneFreq" + DblArrays.convertToString( geneFreq);
        }

        protected void read( BufferedReader in, int numAlleles)
        throws IOException {
            geneFreq = new double[numAlleles];

            String ln = readLine(in);
            if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
            StringBuffer line = new StringBuffer(ln);

            int indx = 0;
            double n = UtilGeneReader.readDbl( line);
            while( n != Double.NEGATIVE_INFINITY) {
                if( indx >= geneFreq.length) { throw new IllegalArgumentException("Invalid Locus File: too many gene frequencies");}
                geneFreq[indx] = n;
                indx++;
                n = UtilGeneReader.readDbl( line);
            }
            if( indx != geneFreq.length) { throw new IllegalArgumentException("Invalid Locus File: too few gene frequencies");}
        }
    } //end LocusData


    static public class LocusDataAffectionStatus extends LocusData {
        int numPenetrances; //aka Liability classes
        double penetrances[/*numPenetrances*/][/*3(autosomal) or 5(sexlinked)*/];

        protected LocusDataAffectionStatus(){}

        public String toString() {
            StringBuffer ret = new StringBuffer();

            ret.append("{ LocusDataAffectionStatus\n");
            ret.append(" " + super.toString() + "\n");
            ret.append(" numPenetrances = " + numPenetrances + "\n");

            for( int j=0; j<penetrances.length; j++) {
                ret.append(" [");
                for( int i=0; i<penetrances[j].length; i++) {
                    if( i>0) { ret.append(",");}
                    ret.append("" + penetrances[j][i]);
                }
                ret.append("]\n");
            }
            ret.append("} LocusDataAffectionStatus");
            return ret.toString();
        }

        static LocusData read( BufferedReader in, int numAlleles, int sexLinked)
        throws IOException {
            if( numAlleles != 2) { throw new IllegalArgumentException("numAlleles = " + numAlleles);}

            LocusDataAffectionStatus ret = new LocusDataAffectionStatus();
            ret.read( in, numAlleles);

            //Line 2
            String ln = readLine(in);
            if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
            StringBuffer line = new StringBuffer(ln);

            ret.numPenetrances = UtilGeneReader.readIntExpected( line);

            ret.penetrances = new double[ret.numPenetrances][];
            for( int i=0; i<ret.numPenetrances; i++) {
                //Line 3+
                ln = readLine(in);
                if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
                line = new StringBuffer(ln);


                if( sexLinked == 0) {
                    ret.penetrances[i] = new double[3];
                    ret.penetrances[i][0] = UtilGeneReader.readDblExpected( line);
                    ret.penetrances[i][1] = UtilGeneReader.readDblExpected( line);
                    ret.penetrances[i][2] = UtilGeneReader.readDblExpected( line);
                }
                else if( sexLinked == 1) {
                    ret.penetrances[i] = new double[5];
                    ret.penetrances[i][0] = UtilGeneReader.readDblExpected( line);
                    ret.penetrances[i][1] = UtilGeneReader.readDblExpected( line);
                    ret.penetrances[i][2] = UtilGeneReader.readDblExpected( line);

                    ln = readLine(in);
                    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
                    line = new StringBuffer(ln);

                    ret.penetrances[i][3] = UtilGeneReader.readDblExpected( line);
                    ret.penetrances[i][4] = UtilGeneReader.readDblExpected( line);
                }
                else {
                    throw new IllegalArgumentException("Invalid Locus File: sexLinked: " + sexLinked);
                }
            }
            return ret;
        }
    } //end LocusDataAffectionStatus

    static public class LocusDataNumberedAlleles extends LocusData {
        protected LocusDataNumberedAlleles(){}

        public String toString() {
            return " " + super.toString();
        }

        static LocusData read( BufferedReader in, int numAlleles, int ignored)
        throws IOException {
            LocusDataNumberedAlleles ret = new LocusDataNumberedAlleles();
            ret.read( in, numAlleles);
            return ret;
        }
    } //end LocusDataNumberedAlleles

	static public class LocusDataComplexAffection1 extends LocusData {
		protected LocusDataComplexAffection1(){}

		public String toString() {
			return " " + super.toString();
		}

		static LocusData read(BufferedReader in, int numAlleles, int ignored)
		throws IOException {
            if( numAlleles != 2) { throw new IllegalArgumentException("numAlleles = " + numAlleles);}

			LocusDataComplexAffection1 ret = new LocusDataComplexAffection1();
			ret.read(in, numAlleles);
			return ret;
		}
	}//end LocusDataComplexAffection1


    static public class LocusDataComplexAffection2 extends LocusData {
        int numPenetrances; //aka Liability classes
        int firstComplexLociInputOrd = -1;

        double penetrances[/*numPenetrances*/][][];
        //format for last two indecies
        //autosomal
        //[0,hh1]->[hh2,hd2,dd2]
        //[1,hd1]->[hh2,hd2,dd2]
        //[2,dd1]->[hh2,hd2,dd2]
        //sexlinked
        //[][][]
        //[][][]
        //[][][]
        //[][]
        //[][]


        protected LocusDataComplexAffection2(){}

        public String toString() {
            StringBuffer ret = new StringBuffer();

            ret.append("{ LocusDataComplexAffection2\n");
            ret.append(" " + super.toString());
            ret.append( " numPenetrances = " + numPenetrances);

            ret.append( " [");
            for( int j=0; j<penetrances.length; j++) {
                if( j>0) { ret.append(" ");}
                ret.append("[");
                for( int i=0; i<penetrances[j].length; i++) {
                    if( i>0) { ret.append("  ");}
                    ret.append("[");
                    for( int k=0; k<penetrances[j][i].length; k++) {
                        if( i>0) { ret.append(",");}
                        ret.append("" + penetrances[j][i][k]);
                    }
                    ret.append("]");
                    if( i<penetrances[j].length-1 ) {ret.append("\n");}
                }
                ret.append("]");
                if( j<penetrances.length-1 ) {ret.append("\n");}
            }
            ret.append("]\n");
            ret.append("} LocusDataComplexAffection2");
            return ret.toString();
        }

        static LocusData read( BufferedReader in, int numAlleles, int sexLinked)
        throws IOException {
            if( numAlleles != 2) { throw new IllegalArgumentException("numAlleles = " + numAlleles);}

            LocusDataComplexAffection2 ret = new LocusDataComplexAffection2();
            ret.read( in, numAlleles);

            //Line 2
            String ln = readLine(in);
            if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
            StringBuffer line = new StringBuffer(ln);

            ret.numPenetrances = UtilGeneReader.readIntExpected( line);

            ret.penetrances = new double[ret.numPenetrances][][];
            for( int i=0; i<ret.numPenetrances; i++) {
                //Line 3+
//                ln = in.readLine();
//                if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
//                line = new StringBuffer(ln);

                if( sexLinked == 0) {
                    ret.penetrances[i] = new double[3][];
                    for( int j=0; j<3; j++) {
                        //Line 3+
                        ln = readLine(in);
                        if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
                        line = new StringBuffer(ln);

                        ret.penetrances[i][j] = new double[3];
                        ret.penetrances[i][j][0] = UtilGeneReader.readDblExpected( line);
                        ret.penetrances[i][j][1] = UtilGeneReader.readDblExpected( line);
                        ret.penetrances[i][j][2] = UtilGeneReader.readDblExpected( line);
                    }
                }
                else if( sexLinked == 1) {
					ret.penetrances[i] = new double[5][];
					for(int j=0; j<3; j++) {
                        ln = readLine(in);
                        if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
                        line = new StringBuffer(ln);

                        ret.penetrances[i][j] = new double[3];
                        ret.penetrances[i][j][0] = UtilGeneReader.readDblExpected( line);
                        ret.penetrances[i][j][1] = UtilGeneReader.readDblExpected( line);
                        ret.penetrances[i][j][2] = UtilGeneReader.readDblExpected( line);
					}
					for(int j=3; j<5; j++) {
                        ln = readLine(in);
                        if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
                        line = new StringBuffer(ln);

                        ret.penetrances[i][j] = new double[2];
                        ret.penetrances[i][j][0] = UtilGeneReader.readDblExpected( line);
                        ret.penetrances[i][j][1] = UtilGeneReader.readDblExpected( line);
					}
                }
                else {
                    throw new IllegalArgumentException("Invalid Locus File: sexLinked: " + sexLinked);
                }
            }
            return ret;
        }
    } //end LocusDataComplexAffection2


    
    
    static public class Footer {
	    public int sexDifference;
	    public int interference;
	    public double recombinationValues_male[];
	    public double recombinationValues_female[];
	    public double sexDiffConstant = 1.0; //only used for SexDifference=1
	    public ProgData progData;
	    
	    static Footer readFooter( BufferedReader in, int numLoci, int program)
	    throws IOException {
		    Footer ret = new Footer();
		    
		    
		    //Line1
		    String ln = readLine(in);
		    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
		    StringBuffer line = new StringBuffer(ln);
		    
		    ret.sexDifference = UtilGeneReader.readIntExpected(line);
		    ret.interference = UtilGeneReader.readIntExpected(line);
		    
		    if(ret.interference != 0) {throw new UnsupportedOperationException("File is using interference: " + ret.interference);}
		    
		    //Line 2
		    ln = readLine(in);
		    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
		    line = new StringBuffer(ln);
		    
		    ret.recombinationValues_male = new double[numLoci-1];
		    for( int i=0; i<ret.recombinationValues_male.length; i++) {
			    ret.recombinationValues_male[i] = UtilGeneReader.readDblExpected( line);
		    }
		    
		    if(ret.sexDifference==0) { //should be same
			    ret.recombinationValues_female = ret.recombinationValues_male;

			    //possibly has sex difference value, even if not using it
			    in.mark(250);
			    ln = readLine(in);
			    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
			    line = new StringBuffer(ln);
			    boolean foundInt = true;

			    try{
				    UtilGeneReader.readIntExpected( line);
			    }
			    catch(NumberFormatException e) { //found a double instead of an integer
				    foundInt = false;
			    }
			    
			    if(foundInt) {in.reset();} //found an int, which should be part of prog data
		    
		    }
		    else if(ret.sexDifference==1) { //constant difference (conversion from J. Ott's book)
			    ln = readLine(in);
			    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
			    line = new StringBuffer(ln);

			    ret.sexDiffConstant = UtilGeneReader.readDblExpected(line);
			    
			    ret.recombinationValues_female = new double[numLoci-1];
			    for(int i=0; i<ret.recombinationValues_female.length; i++) {
				    ret.recombinationValues_female[i] = 0.5 * (1- Math.pow((1.0-2.0*ret.recombinationValues_male[i]),ret.sexDiffConstant));
			    }
		    }
		    else {
			//TODO sexDifference==2
			throw new UnsupportedOperationException("File is using sexDifference: " + ret.sexDifference);
		    }
		    
		    if( program == 4) {
			    ret.progData = Linkmap.readProgData(in, ret.recombinationValues_male.length);
		    } else if ( program == 5) {
			    ret.progData = Mlink.readProgData(in);
		    } else if(program == 8) {
			    ret.progData = GH.readProgData(in);
		    } else if(program == 9) {
			    ret.progData = BinaryILink.readProgData(in);
		    } else {
			    throw new IllegalArgumentException("Invalid Locus File: Unsupported program = " + program);
		    }
		    return ret;
	    }

	    public String toString() {
		    StringBuffer ret = new StringBuffer();
		    ret.append("{ Locus Footer\n");
		    ret.append(" Recombinant Values Male: " + DblArrays.convertToString( recombinationValues_male) + "\n");
		    if(recombinationValues_male!=recombinationValues_female) ret.append(" Recombinant Values Female: " + DblArrays.convertToString( recombinationValues_female) + "\n");
		    ret.append("" + progData);
		    ret.append("\n} Locus Footer\n");
		    return ret.toString();
	    }





	    abstract static public class ProgData {
		    protected ProgData(){}
		    /**Input order of the 1st locus containing a changing recombination value (1 based).*/
		    abstract public int getInputOrd_ChangingS1();
		    /**Input order of the 2nd locus containing a changing recombination value (1 based).*/
		    abstract public int getInputOrd_ChangingS2();
	    }

	    static public class Mlink extends ProgData {
		    public int recomVaried;
		    public double inc;
		    public double finishedValue;
		    
		    protected Mlink(){}
		    static ProgData readProgData( BufferedReader in)
		    throws IOException {
			    Mlink ret = new Mlink();
			    
			    String ln = readLine(in);
			    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
			    StringBuffer line = new StringBuffer(ln);
			    
			    //Line 1
			    ret.recomVaried = UtilGeneReader.readIntExpected( line);
			    ret.inc = UtilGeneReader.readDblExpected( line);
			    ret.finishedValue = UtilGeneReader.readDblExpected( line);
			    
			    if(ret.recomVaried != 1) { throw new IllegalArgumentException("Invalid Locus File: recombination varied for MLINK was set to " + ret.recomVaried);}
			    return ret;
		    }
		    
		    public String toString() {
			    return "Mlink( recomVaried="+recomVaried+
				    ",inc="+inc+
				    ",finishedValue="+finishedValue+")";
		    }
		    public int getInputOrd_ChangingS1() {return recomVaried+1;} //the changing recom is between recomVaried and recomVaried+1
		    public int getInputOrd_ChangingS2() {return -1;}
		    
	    }//end Mlink

	    static public class Linkmap extends ProgData {
		    public int locusVaried;
		    public double finishedValue;
		    public int numEvals;
		    private final int lengthOfRecomArr;
		    
		    protected Linkmap(int ra){ lengthOfRecomArr=ra;}
		    static ProgData readProgData( BufferedReader in, int lengthOfRecomArr)
		    throws IOException {
			    Linkmap ret = new Linkmap(lengthOfRecomArr);
			    
			    String ln = readLine(in);
			    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
			    StringBuffer line = new StringBuffer(ln);
			    
			    //Line 1
			    ret.locusVaried = UtilGeneReader.readIntExpected( line);
			    ret.finishedValue = UtilGeneReader.readDblExpected( line);
			    ret.numEvals = UtilGeneReader.readIntExpected( line);
			    return ret;
		    }
		    
		    public String toString() {
			    return "LinkMap( locusVaried="+locusVaried+
				    ",finishedValue="+finishedValue+
				    ",numEvals="+numEvals+")";
		    }
		    public int getInputOrd_ChangingS1() {
			    if(locusVaried==1) return locusVaried+1; //the changing recom is between locusVaried and locusVaried+1 (same as prog==5)
			    else return locusVaried;  //the changing recom is between locusVaried-1 and locusVaried
		    } 
		    public int getInputOrd_ChangingS2() { 
			    if(locusVaried==1) return -1; //there is only one changing recom
			    else if((locusVaried-1)<=lengthOfRecomArr) return locusVaried+1; //the changing recom is between locusVaried and locusVarid+1 (if not end)
			    else return -1;
		    }
	    }//end Linkmap

	    static public class GH extends ProgData {
		    protected GH(){}
		    static ProgData readProgData(BufferedReader in)
		    throws IOException {
			    throw new UnsupportedOperationException();//TODO
		    }
		    
		    public String toString() { return "GH()";}
		    public int getInputOrd_ChangingS1() {return -1;}
		    public int getInputOrd_ChangingS2() {return -1;}
	    }//end GH
	    
	    static public class BinaryILink extends ProgData {
		    public int locusVaried;
		    
		    protected BinaryILink(){}
		    static ProgData readProgData(BufferedReader in)
		    throws IOException {
			    BinaryILink ret = new BinaryILink();
			    
			    String ln = readLine(in);
			    if( ln == null) { throw new IllegalArgumentException("Invalid Locus File");}
			    StringBuffer line = new StringBuffer(ln);
			    
			    //Line 1
			    ret.locusVaried = UtilGeneReader.readIntExpected( line);
			    return ret;
		    }
		    
		    public String toString() {return "BinaryILink( locusVaried="+locusVaried+")";}
		    public int getInputOrd_ChangingS1() {return -1;}
		    public int getInputOrd_ChangingS2() {return -1;}
	    }//end BinaryILink
    }//end footer
}
