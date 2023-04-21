#include <jvmpi.h>

//global jvmpi interface pointer
static JVMPI_Interface *jvmpi_interface;
static int loaded = 0;


extern "C" {

  // profiler agent entry point

  JNIEXPORT jint JNICALL
  JVM_OnLoad(JavaVM *jvm, char *options, void *reserved) {

    // get jvmpi interface pointer
    if ((jvm->GetEnv((void **)&jvmpi_interface, JVMPI_VERSION_1)) < 0) {
      return JNI_ERR;
    }
    loaded = 1;
    return JNI_OK;
  }


  JNIEXPORT jlong JNICALL
  Java_edu_ucla_util_JVMProfiler_getCurrentThreadCpuTime_1native(JNIEnv *, jclass) {

    // return 0 if agent not initialized
    return jvmpi_interface == 0 ? 0 : 
      jvmpi_interface->GetCurrentThreadCpuTime();
  }

  JNIEXPORT jboolean JNICALL
  Java_edu_ucla_util_JVMProfiler_profilerRunning_1native(JNIEnv *, jclass) {
    if( loaded == 0) { return JNI_FALSE;}
    else { return JNI_TRUE;}
  }

  int main(){
    return (int)0;
  }
}
