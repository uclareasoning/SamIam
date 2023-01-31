#include <jni.h>
#include "edu_ucla_belief_dtree_Hmetis.h"
#include <stdio.h>
#include <time.h>


extern "C" void HMETIS_PartRecursive( int nvtxs, int nhedges, int *vwgts, int *eptr, int *eind, 
                                      int *hewgts, int nparts, int ubfactor, int *options, 
                                      int *part, int *edgecut);

extern "C" void HMETIS_PartKway ( int nvtxs, int nhedges, int *vwgts, int *eptr, int *eind, 
                                  int *hewgts, int nparts, int ubfactor, int *options, 
                                  int *part, int *edgecut);

//prototype
int callHmetis(JNIEnv *env, jclass obj, 
               jint nvtxs, jint nhedges, 
               jintArray vwgts, jintArray eptr, 
               jintArray eind, jintArray hewgts, 
               jint nparts, jint ubfactor, 
               jintArray options, jintArray part, 
               jintArray edgecut, int algo, jdoubleArray timing);


JNIEXPORT jint JNICALL 
Java_edu_ucla_belief_dtree_Hmetis_HMETIS_1PartRecursive( 
                                                             JNIEnv *env, jclass obj, 
                                                             jint nvtxs, jint nhedges, 
                                                             jintArray vwgts, jintArray eptr, 
                                                             jintArray eind, jintArray hewgts, 
                                                             jint nparts, jint ubfactor, 
                                                             jintArray options, jintArray part, 
                                                             jintArray edgecut, jdoubleArray timing)
{
  return callHmetis( env, obj, nvtxs, nhedges, vwgts, eptr, eind, 
                     hewgts, nparts, ubfactor, options, part, edgecut, 1, timing);
}


JNIEXPORT jint JNICALL 
Java_edu_ucla_belief_dtree_Hmetis_HMETIS_1PartKway(
                                                        JNIEnv *env, jclass obj, 
                                                        jint nvtxs, jint nhedges, 
                                                        jintArray vwgts, jintArray eptr, 
                                                        jintArray eind, jintArray hewgts, 
                                                        jint nparts, jint ubfactor, 
                                                        jintArray options, jintArray part, 
                                                        jintArray edgecut, jdoubleArray timing)
{
  return callHmetis( env, obj, nvtxs, nhedges, vwgts, eptr, eind, 
                     hewgts, nparts, ubfactor, options, part, edgecut, 2, timing);
}



/* Returned status values:
 * 0 = sucess
 * Bad Int Values:
 * 1 = nvtxs
 * 2 = nhedges
 * 3 = nparts
 * 4 = ubfactor
 * Bad Array (length, null, ...)
 * 11 = vwgts
 * 12 = eptr
 * 13 = eind
 * 14 = hewgts
 * 15 = options
 * 16 = part
 * 17 = edgecut
 * 18 = timing (can be null or 4)
 * Misc Errors
 * 20 = Bad algo number
 * 21 = sizeof(int) != sizeof(jint)
 */

/* This function will check the parameters for some errors and then
 *  call the Hmetis library.
 *
 * It does not check all the parameters for every error (e.g. it doesn't
 *  look at the contents of the options array, etc.).  Those are left for
 *  the hMetis library to check.
 *
 * It will return a status integer as listed above.
 */

int callHmetis(JNIEnv *env, jclass obj, 
               jint nvtxs, jint nhedges, 
               jintArray vwgts, jintArray eptr, 
               jintArray eind, jintArray hewgts, 
               jint nparts, jint ubfactor, 
               jintArray options, jintArray part, 
               jintArray edgecut, int algo, jdoubleArray timing) {


  int status = 0;  //returned value
  clock_t start;
  clock_t end;

  if( sizeof(int) != sizeof(jint)) { return 21;} //typecast jint* to int*, so size errors would cause problems

  //test int values
  if( nvtxs <= 0) { status = 1;}
  if( nhedges <= 0) { status = 2;}
  if( nparts < 2) { status = 3;}  //test for exponent of 2?
  if( ubfactor < 1 || ubfactor > 49) { status = 4;}

  //if error, can return since haven't accepted any arrays yet
  if( status != 0) { return status;}



  //Get Arrays (if not null, which crashes the program for some reason)
  jint *c_vwgts=NULL, *c_eptr=NULL, *c_eind=NULL, *c_hewgts=NULL;
  jint *c_options=NULL, *c_part=NULL, *c_edgecut=NULL;
  jdouble *c_timing=NULL;
  if( vwgts != NULL)   { c_vwgts = env->GetIntArrayElements( vwgts, 0);}
  if( eptr != NULL)    { c_eptr = env->GetIntArrayElements( eptr, 0);}
  if( eind != NULL)    { c_eind = env->GetIntArrayElements( eind, 0);}
  if( hewgts != NULL)  { c_hewgts = env->GetIntArrayElements( hewgts, 0);}
  if( options != NULL) { c_options = env->GetIntArrayElements( options, 0);}
  if( part != NULL)    { c_part = env->GetIntArrayElements( part, 0);}
  if( edgecut != NULL) { c_edgecut = env->GetIntArrayElements( edgecut, 0);}
  if( timing != NULL)  { c_timing = env->GetDoubleArrayElements( timing, 0);}


  //Get sizes of arrays
  jsize len_vw=0, len_ep=0, len_ei=0, len_he=0;
  jsize len_op=0, len_pa=0, len_ed=0, len_ti;
  if( vwgts != NULL)   { len_vw = env->GetArrayLength( vwgts);}
  if( eptr != NULL)    { len_ep = env->GetArrayLength( eptr);}
  if( eind != NULL)    { len_ei = env->GetArrayLength( eind);}
  if( hewgts != NULL)  { len_he = env->GetArrayLength( hewgts);}
  if( options != NULL) { len_op = env->GetArrayLength( options);}
  if( part != NULL)    { len_pa = env->GetArrayLength( part);}
  if( edgecut != NULL) { len_ed = env->GetArrayLength( edgecut);}
  if( timing != NULL)  { len_ti = env->GetArrayLength( timing);}


  //Check arrays for errors (if an error is found, still must release the 
  //  array memory.
  if( len_vw != nvtxs && vwgts != NULL) { status = 11;} //can be NULL if unweighted
  if( len_ep != nhedges+1) { status = 12;}
  if( len_ep > 1) {
    if( len_ei != c_eptr[len_ep-1]) { status = 13;}
  }
  if( len_he != nhedges && hewgts != NULL) { status = 14;} //can be NULL if unweighted
  if( len_op != 9) { status = 15;}
  if( len_pa != nvtxs) { status = 16;}
  if( len_ed != 1) { status = 17;}
  if( len_ti != 1 && timing != NULL) { status = 18;} //can be NULL for no timing


  start = clock();

  if( status == 0 && algo == 1) {
    HMETIS_PartRecursive( nvtxs, nhedges, (int*)c_vwgts, (int*)c_eptr, (int*)c_eind, (int*)c_hewgts, 
                          nparts, ubfactor, (int*)c_options, (int*)c_part, (int*)c_edgecut);
  }
  else if( status == 0 && algo == 2) {
    HMETIS_PartKway( nvtxs, nhedges, (int*)c_vwgts, (int*)c_eptr, (int*)c_eind, (int*)c_hewgts, 
                     nparts, ubfactor, (int*)c_options, (int*)c_part, (int*)c_edgecut);
  }
  else if( algo < 1 || algo > 2) {
    status = 20;
  }

  end = clock();
  if( timing != NULL) { //cumulative time spent in hmetis library
    c_timing[0] += (double)(((double)(end - start)) / (double)CLOCKS_PER_SEC);
  }


  //Clean up the arrays
  if( vwgts != NULL)   {env->ReleaseIntArrayElements( vwgts, c_vwgts, 0);}
  if( eptr != NULL)    {env->ReleaseIntArrayElements( eptr, c_eptr, 0);}
  if( eind != NULL)    {env->ReleaseIntArrayElements( eind, c_eind, 0);}
  if( hewgts != NULL)  {env->ReleaseIntArrayElements( hewgts, c_hewgts, 0);}
  if( options != NULL) {env->ReleaseIntArrayElements( options, c_options, 0);}
  if( part != NULL)    {env->ReleaseIntArrayElements( part, c_part, 0);}
  if( edgecut != NULL) {env->ReleaseIntArrayElements( edgecut, c_edgecut, 0);}
  if( timing != NULL)  {env->ReleaseDoubleArrayElements( timing, c_timing, 0);}

  return status;
}
