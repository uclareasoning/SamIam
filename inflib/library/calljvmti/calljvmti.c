/** author keith cascio
    since 20060313 */

#include <jvmti.h>

//#include <iostream>
//using namespace std ;

#ifdef UNICODE
#define JNI_OnLoad JNI_OnLoadW
#define Agent_OnLoad Agent_OnLoadW
#define Java_edu_ucla_util_JVMTI_getCurrentThreadCpuTimeUnsafe_1native Java_edu_ucla_util_JVMTI_getCurrentThreadCpuTimeUnsafe_1nativeW
#define Java_edu_ucla_util_JVMTI_getCurrentThreadCpuTime_1native Java_edu_ucla_util_JVMTI_getCurrentThreadCpuTime_1nativeW
#define Java_edu_ucla_util_JVMTI_isProfilerRunning_1native Java_edu_ucla_util_JVMTI_isProfilerRunning_1nativeW
#endif

//global jvmti interface pointer
static jvmtiEnv* jvmti_interface;
static jboolean flagLoaded = JNI_FALSE;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
JNI_OnLoad( JavaVM *vm, void *reserved ){
	return JNI_VERSION_1_4;
}

/*
from jdk1.5.0/docs/guide/jvmti/jvmti.html#writingAgents
The return value from Agent_OnLoad is used to indicate an error.
Any value other than zero indicates an error and causes termination of the VM.
*/
JNIEXPORT jint JNICALL
Agent_OnLoad( JavaVM *vm, char *options, void *reserved ){
	jvmtiCapabilities capa;
	jvmtiError err;

	if( ((*vm)->GetEnv( vm, (void **)&jvmti_interface, JVMTI_VERSION_1_0 )) < 0 ){
		return JNI_ERR;
	}

	err = (*jvmti_interface)->GetCapabilities( jvmti_interface, &capa );
	if( err != JVMTI_ERROR_NONE ){
		return JNI_ERR;
	}
	capa.can_get_current_thread_cpu_time = 1;
	err = (*jvmti_interface)->AddCapabilities( jvmti_interface, &capa );
	if( err != JVMTI_ERROR_NONE ){
		return JNI_ERR;
	}

	flagLoaded = JNI_TRUE;
	return JNI_OK;
}

/*
 * Class:     edu_ucla_util_JVMTI
 * Method:    isProfilerRunning_native
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_edu_ucla_util_JVMTI_isProfilerRunning_1native( JNIEnv * aa, jclass ab ){
	return flagLoaded;
}

/*
 * Class:     edu_ucla_util_JVMTI
 * Method:    getCurrentThreadCpuTime_native
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_edu_ucla_util_JVMTI_getCurrentThreadCpuTime_1native( JNIEnv * aa, jclass ab ){
	jlong ret = 0;
	if( jvmti_interface ){
		jvmtiError error = (*jvmti_interface)->GetCurrentThreadCpuTime( jvmti_interface, &ret );
	}
	return ret;
}

/*
 * Class:     edu_ucla_util_JVMTI
 * Method:    getCurrentThreadCpuTimeUnsafe_native
 * Signature: ()J
 *
 * from jdk1.5.0/docs/guide/jvmti/jvmti.html#JVMTI_ERROR_MUST_POSSESS_CAPABILITY
 * JVMTI_ERROR_MUST_POSSESS_CAPABILITY (99)
 * The capability being used is false in this environment.
 */
JNIEXPORT jlong JNICALL Java_edu_ucla_util_JVMTI_getCurrentThreadCpuTimeUnsafe_1native( JNIEnv * aa, jclass ab ){
	//cout << "getCurrentThreadCpuTimeUnsafe_1native()" << endl;
	//cout << "    jvmti_interface " << (int)jvmti_interface << endl;
	jlong ret = 0;
	//if( jvmti_interface )
	//jvmtiError error =
	(*jvmti_interface)->GetCurrentThreadCpuTime( jvmti_interface, &ret );
	//cout << "    error " << error << endl;
	return ret;
}

#ifdef __cplusplus
}
#endif
