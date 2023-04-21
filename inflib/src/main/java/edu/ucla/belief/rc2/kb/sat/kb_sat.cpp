#include <stdio.h>
#include <time.h>
#include <stdlib.h>

#include "edu_ucla_belief_rc2_kb_sat_KB_0005fSAT.h"

#include "types.h"
#include "flags.h"
#include "cnf.h"
#include "globals.h"
#include "memory.h"


int KB_UNSATISFIABLE = -1;
int initialized = 0;
struct cnf_manager *cnf_manager = NULL;



/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    load_library
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_load_1library
  (JNIEnv *, jclass, jint unsat) {

	KB_UNSATISFIABLE = unsat;
}

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    sat_createKB
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_sat_1createKB
  (JNIEnv *env, jobject obj, jstring str) {

	if(sizeof(int) != sizeof(jint)) { return -1;}
	if(initialized!=0) { return -2;}

	initialized = 1;

	const char *fileNm = (env)->GetStringUTFChars(str, 0);
	struct cnf *cnf = get_cnf_from_file_dla(fileNm);
	(env)->ReleaseStringUTFChars(str, fileNm);

	if(cnf==NULL) {return -3;}
	print_cnf_properties(cnf);

	cnf_manager = construct_cnf_manager(cnf);
	free_cnf(cnf);


	//do initial unit resolution (pre-rc computation)
	if( !assert_unit_clauses_and_pure_literals(cnf_manager)) {
		return KB_UNSATISFIABLE;
	}
	return cnf_manager->current_decision_level;
}

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    sat_releaseKB
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_sat_1releaseKB
  (JNIEnv *, jobject) {

	if(cnf_manager != NULL) {
		undo_assert_unit_clauses(cnf_manager);

		print_cnf_manager_properties(cnf_manager, stdout);
		free_cnf_manager(cnf_manager);
		cnf_manager = NULL;
	}
	if(initialized == 1) {
		initialized = 0;
	}
}

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    numClauses
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_numClauses
  (JNIEnv *, jobject) {
	if(cnf_manager != NULL) {
		return cnf_manager->clause_count + cnf_manager->cd_clause_count;
	}
	else {
		return -1;
	}
}

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    numLiterals
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_numLiterals
  (JNIEnv *, jobject) {
	return -1;//TODO calculate this
}



/*
Dump KB to stdout
{
	int n = cnf_manager->var_count;
	struct var **vars = (struct var**) calloc(n, sizeof(struct var *));
	for(int i=0; i<n; i++) vars[i] = index2varp(i+1,cnf_manager);

	for(int i=0; i<n; i++) {
		if(vars[i]->pliteral->resolved) {
			printf("%d (%d),", -(i+1), vars[i]->pliteral->rlevel);
		}
		if(vars[i]->nliteral->resolved) {
			printf("%d (%d),", (i+1), vars[i]->nliteral->rlevel);
		}
	}
	printf("\n");
	delete(vars);
}*/



/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    my_decide
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_my_1decide
  (JNIEnv *, jobject, jint bvar) {


	jint pos_bvar = bvar;
	jint assert_pos = 1;

	//if asserting the negation of the variable
	if(bvar<0) {pos_bvar = -bvar; assert_pos = 0;}

	struct var *var = ((cnf_manager->vars)+pos_bvar-1);


	if(assert_pos==1) {
		if(var->nliteral->resolved) { return 2;} //already knew this
		else if(var->pliteral->resolved) { return 0;} //already knew this isnt' possible
		else {
			if(decide_literal(var->pliteral,cnf_manager)) {
				return 1; //success
			}
			else { //failed to set it, call undo_decide until reach assertion_level and then call assert_cd_literal (repeat as necessary)
				jint cnt = 0;
				while(1) {
					undo_decide_literal(cnf_manager); cnt++;

					if(at_assertion_level(cnf_manager) && assert_cd_literal(cnf_manager)) {
						return -cnt;
					}
				}
			}
		}
	}
	else {
		if(var->pliteral->resolved) { return 2;} //already knew this
		else if(var->nliteral->resolved) { return 0;} //already knew this isnt' possible
		else {
			if(decide_literal(var->nliteral,cnf_manager)) {
				return 1; //success
			}
			else { //failed to set it, call undo_decide until reach assertion_level and then call assert_cd_literal (repeat as necessary)
				jint cnt = 0;
				while(1) {
					undo_decide_literal(cnf_manager); cnt++;

					if(at_assertion_level(cnf_manager) && assert_cd_literal(cnf_manager)) {
						return -cnt;
					}
				}
			}
		}
	}
}

/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    undo_decide
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_undo_1decide
  (JNIEnv *, jobject) {
	undo_decide_literal(cnf_manager);
}



/*
 * Class:     edu_ucla_belief_rc2_kb_sat_KB_0005fSAT
 * Method:    varStatus
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_rc2_kb_sat_KB_1SAT_varStatus
  (JNIEnv *, jobject, jint bvar) {

	struct var *var = ((cnf_manager->vars)+bvar-1);

	if(var->nliteral->resolved) { return 1;} //already knew this
	else if(var->pliteral->resolved) { return -1;} //already knew this isnt' possible
	else { return 0;}
}
