/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class edu_ucla_belief_rc2_kb_sat_KB_0005fSAT */

#ifndef _Included_edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
#define _Included_edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
#ifdef __cplusplus
extern "C" {
#endif
/* Inaccessible static: loaded */
/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    my_decide
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_my_1decide
  (JNIEnv *, jobject, jint);

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    undo_decide
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_undo_1decide
  (JNIEnv *, jobject);

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    varStatus
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_varStatus
  (JNIEnv *, jobject, jint);

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    sat_createKB
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_sat_1createKB
  (JNIEnv *, jobject, jstring);

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    sat_releaseKB
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_sat_1releaseKB
  (JNIEnv *, jobject);

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    numClauses
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_numClauses
  (JNIEnv *, jobject);

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    numLiterals
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_numLiterals
  (JNIEnv *, jobject);

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    load_library
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_load_1library
  (JNIEnv *, jclass, jint);

#ifdef __cplusplus
}
#endif
#endif