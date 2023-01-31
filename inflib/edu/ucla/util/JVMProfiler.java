package edu.ucla.util;


/**
* This code comes from Java Tip 92 on Javaworld.com  (but was modified quite a bit)
*  see http://www.javaworld.com/javaworld/javatips/jw-javatip92.html
*
* <p>It is an interface into the JVM Profiler and allows the cpu time for the
*  current thread to be obtained (and possibly other things to be added later).
*
* <p>The program must use the command line java -Xrunjvm_profiler application
*/
public class JVMProfiler {


    private static boolean loaded = false;
    public static boolean loaded() { return loaded;}


	public final static long nanosPerMilli = 1000000;


    /** This will throw an unsatisfied link exception if loaded is not true.*/
    public static native long getCurrentThreadCpuTime_native();

    /** Will return true if the command line to start the profiler was set, otherwise will return false.
     * <p>Will throw an unsatisfied link exception if loaded is not true.
     */
    public static native boolean profilerRunning_native();


    static {
        try{
            System.loadLibrary("jvm_profiler");
            loaded = true;
        }
        catch( Throwable e) {
            loaded = false;
        }
    }

    /** Will return the CPU Time of the thread or else will return 0 if the profiler
     *  cannot be interfaced with.  The cpu time is in nanoseconds.
     */
    public static long getCurrentThreadCpuTime() {
        if( loaded) {
            try {
                return getCurrentThreadCpuTime_native();
            }
            catch( Throwable e) {
                return 0;
            }
        }
        else {
            return 0;
        }
    }


    /** Will return the CPU Time of the thread or else will return 0 if the profiler
     *  cannot be interfaced with.  The cpu time is in milliseconds (integer division used).
     */
    public static long getCurrentThreadCpuTimeMS() {
        if( loaded) {
            try {
                return getCurrentThreadCpuTime_native() / nanosPerMilli; //(integer division being used)
            }
            catch( Throwable e) {
                return 0;
            }
        }
        else {
            return 0;
        }
    }


    /** Will return true if the command line to start the profiler was set, otherwise will return false.
     */
    public static boolean profilerRunning() {
        if( loaded) {
            try{
                return profilerRunning_native();
            }
            catch( Throwable e) {
                return false;
            }
        }
        else {
            return false;
        }
    }

	/** Test/debug
		@since 20060315 */
	public static void main( String[] args ){
		java.io.PrintStream stream = System.out;
		boolean fLoaded = JVMProfiler.loaded;
		stream.println( "JVMProfiler.loaded ? " + fLoaded );
		if( !fLoaded ) System.exit(1);

		boolean running = JVMProfiler.profilerRunning();
		stream.println( "JVMProfiler.profilerRunning() ? " + running );
		if( !running ) System.exit(1);

		double wastetime = 1.0;
		double factor = 1.0001;
		double limit = Double.MAX_VALUE/((double)256);//7.0222388080559207349424774895197e+305

		long cpu_start = JVMProfiler.getCurrentThreadCpuTime();

		wastetime = 1.0;
		while( wastetime < limit ) wastetime *= factor;

		long cpu_end = JVMProfiler.getCurrentThreadCpuTime();

		stream.println( "cpu_start:           " + cpu_start );
		stream.println( "cpu_end:             " + cpu_end );
		stream.println( "elapsed cpu profile: " + (cpu_end - cpu_start) );
	}
}
