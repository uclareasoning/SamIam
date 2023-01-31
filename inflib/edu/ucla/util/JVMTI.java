package edu.ucla.util;

/**
	Access point for the Sun JVM Tool Interface (JVMTI).
	Since profiling requires special vm initialization,
	you must use the command line vm option "-Xruncalljvmti".
	That option loads the native library with filename:
	<br />
	calljvmti.dll on Windows
	<br />
	libcalljvmti.jnilib on Mac
	<br />
	libcalljvmti.so on Linux and Solaris

	@author keith cascio
	@since 20060313
*/
public class JVMTI
{
	private static boolean flagLoaded = false;
	public static boolean isLoaded() { return flagLoaded; }

	static{
		try{
			System.loadLibrary( "calljvmti" );
			JVMTI.flagLoaded = true;
		}
		catch( Throwable throwable ){
			System.err.println( "warning: JVMTI static init caught + " + throwable );
			JVMTI.flagLoaded = false;
		}
	}

	/** WARNING: Call this method from test code only.
		This method exists in order to provide the same information as getCurrentThreadCpuTime(),
		but without any safety checks or exception handling.
		@see edu.ucla.util.JVMTI#getCurrentThreadCpuTime()
	*/
	public static long getCurrentThreadCpuTimeUnsafe(){
		return getCurrentThreadCpuTimeUnsafe_native();
	}

	/** Call this method from a user application to report running times to a user. */
	public static long getCurrentThreadCpuTime(){
		try{
			if( flagLoaded ) return getCurrentThreadCpuTime_native();
		}catch( Throwable throwable ){
			System.err.println( "warning: JVMTI.getCurrentThreadCpuTime() caught + " + throwable );
		}

		return 0;
	}

	public static boolean isProfilerRunning(){
		try{
			if( flagLoaded ) return isProfilerRunning_native();
		}catch( Throwable throwable ){
			System.err.println( "warning: JVMTI.isProfilerRunning() caught + " + throwable );
		}

		return false;
	}

	private static native long    getCurrentThreadCpuTimeUnsafe_native();
	private static native long    getCurrentThreadCpuTime_native();
	private static native boolean isProfilerRunning_native();

	/** Test/debug */
	public static void main( String[] args ){
		java.io.PrintStream stream = System.out;
		boolean loaded = JVMTI.flagLoaded;
		stream.println( "JVMTI.flagLoaded ? " + loaded );
		if( !loaded ) System.exit(1);

		boolean running = JVMTI.isProfilerRunning();
		stream.println( "JVMTI.isProfilerRunning() ? " + running );
		if( !running ) System.exit(1);

		double wastetime = 1.0;
		double factor = 1.0001;
		double limit = Double.MAX_VALUE/((double)256);//7.0222388080559207349424774895197e+305

		stream.println( "testing safe version, i.e. getCurrentThreadCpuTime() ..." );
		long cpu_start = JVMTI.getCurrentThreadCpuTime();

		wastetime = 1.0;
		while( wastetime < limit ) wastetime *= factor;

		long cpu_end = JVMTI.getCurrentThreadCpuTime();

		stream.println( "cpu_start:           " + cpu_start );
		stream.println( "cpu_end:             " + cpu_end );
		stream.println( "elapsed cpu profile: " + (cpu_end - cpu_start) );

		stream.println( "testing unsafe version, i.e. getCurrentThreadCpuTimeUnsafe() ..." );
		cpu_start = JVMTI.getCurrentThreadCpuTimeUnsafe();

		wastetime = 1.0;
		while( wastetime < limit ) wastetime *= factor;

		cpu_end = JVMTI.getCurrentThreadCpuTimeUnsafe();

		stream.println( "cpu_start:           " + cpu_start );
		stream.println( "cpu_end:             " + cpu_end );
		stream.println( "elapsed cpu profile: " + (cpu_end - cpu_start) );
	}
}
