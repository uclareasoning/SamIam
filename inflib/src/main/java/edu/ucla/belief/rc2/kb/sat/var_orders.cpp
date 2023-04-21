#include <stdio.h>
#include <time.h>
#include <stdlib.h>

#if(PROFILE)
#include "timer.h"
#endif
#include "flags.h"
#include "globals.h"
#include "types.h"
#include "cnf.h"


/*
 ***********************************************************************************
 *
 * Var stuff
 *
 ************************************************************************************
 */

CNF_CLAUSE_INDEX var_active_occurences(struct var *var) 
{
	return var->pliteral->active_occurences + var->nliteral->active_occurences;
}
/* 
 * returns 1 if the variable is either set to some value or
 * appears in no unsubsumed clause, 
 * returns 0 otherwise.
 */

BOOLEAN eliminated_var(struct var *var)
{
	return (var_active_occurences(var)==0 || var->pliteral->resolved || var->nliteral->resolved);
}


BOOLEAN irrelevant_var(struct var *var)
{
	return (var->pliteral->active_occurences==0 && var->nliteral->active_occurences==0);
}

BOOLEAN relevant_var(struct var *var)
{
	return (var->pliteral->active_occurences || var->nliteral->active_occurences);
}


/* Tests whether value of var is known */
BOOLEAN instantiated_var(struct var *var)
{
	return (var->pliteral->resolved || var->nliteral->resolved);
}

/* Var is set due to a decision, not an implication */
BOOLEAN decided_var(struct var *var)
{
	return ((var->pliteral->resolved && var->pliteral->implying_clause==NULL) || 
		    (var->nliteral->resolved && var->nliteral->implying_clause==NULL));
}

BOOLEAN subsumed_clause(struct clause *clause)
{
	struct literal **literals = clause->literals;
	struct literal *literal;
	while((literal = *literals++) != NULL) {
		if(literal->negation->resolved) {
			return 1;
		}
	}
	return 0;
	//return (clause->subsumed_count || clause->decision_subsumed_count);
}

BOOLEAN subsumed_clause_by_decision(struct clause *clause)
{
	return clause->decision_subsumed_count;
}


double var_score_vsids(struct var *var)
{
	return MAX(var->pliteral->relevance_count,var->nliteral->relevance_count);
}

//Modified (ADC 7/8/04)
//I'm not sure why, but ordering all ties by var index seems to improve 
//performance (this is how zchaff does it)


/* Old sorting stuff
int vsids_comp(struct var *v1, struct var *v2)
{
	double s1 = var_score_vsids(v1);
	double s2 = var_score_vsids(v2);
	if (s1<s2) return 1;
	else if (s2<s1) return -1;
	else if (v2->index < v1->index) return 1;
	else return -1;
}

void swap(struct var **vars,CNF_VAR_INDEX i, CNF_VAR_INDEX j)
{
	if(i==j) return;
	//printf("\nswaping %d and %d",i,j);
	struct var *temp = vars[i];
	vars[i]=vars[j];
	vars[j]=temp;
}

void my_qsort(struct var **vars, int left, int right)
{
	int i, last;
	if(left >= right) return;
	swap(vars,left,(left+right)/2);
	last=left;
	for(i=left+1; i <= right; i++)
		if(vsids_comp(vars[i],vars[left])<0) swap(vars,++last,i);
	swap(vars,left,last);
	my_qsort(vars,left,last-1);
	my_qsort(vars,last+1,right);
}
*/

/* sort vars in place */
int vsids_comp(const void *a, const void *b)
{
	struct var *v1 = *((struct var **)a);
	struct var *v2 = *((struct var **)b);

	double s1 = var_score_vsids(v1);
	double s2 = var_score_vsids(v2);

	if (s1<s2) return 1;
	else if (s2<s1) return -1;
	else if (v2->index < v1->index) return 1;
	else return -1;
}

void sort_vars(struct var **vars,unsigned count)
{
	qsort((void *)vars,(size_t)count,sizeof(int),vsids_comp);
}


void update_vsids(struct cnf_manager *manager) 
{
	//update relevance counts
	for(CNF_VAR_INDEX i=0; i<manager->var_count; i++) {
		struct var *var = manager->vars+i;

		//MODIFIED (ADC 7/22/04)
		var->pliteral->relevance_count = 
			(var->pliteral->relevance_count)/2.0 + var->pliteral->lit_count - var->pliteral->last_lit_count;
		var->pliteral->last_lit_count = var->pliteral->lit_count;
		var->nliteral->relevance_count = 
			(var->nliteral->relevance_count)/2.0 + var->nliteral->lit_count - var->nliteral->last_lit_count;
		var->nliteral->last_lit_count = var->nliteral->lit_count;
	}
}


struct var *select_zchaff_var(struct var **vars, struct cnf_manager *manager)
{

#if(PROFILE)
	CYCLES start_t = GetMachineCycleCount();
#endif

	struct var *var = NULL;

	if ((manager->decisions_count % 255)==0) {
		update_vsids(manager);
		sort_vars(vars,manager->var_count);
	}

	for(CNF_VAR_INDEX i=0; i < manager->var_count; i++) {
		//if(!eliminated_var(*vars)) {
		if(!(*vars)->pliteral->resolved && !(*vars)->nliteral->resolved) {
			var = *vars;
			break;
		}
		else vars++;
	}

#if(PROFILE)
	manager->var_order_cycles += (GetMachineCycleCount()-start_t);
#endif

	return var;
}


struct literal *select_literal(struct var *var)
{
	if(var->pliteral->relevance_count >= var->nliteral->relevance_count) return var->pliteral;
	else return var->nliteral;	
}

