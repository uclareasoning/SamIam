#include <stdlib.h>
#include <stdio.h>

#include "flags.h"
#include "types.h"
#include "cnf.h"
#include "globals.h"


//ADDED (ADC 7/22/04)
#if(RESTART)
BOOLEAN restart(struct cnf_manager *manager)
{
	if (manager->assertions_count < manager->next_restart_backtrack) return 0;

	clock_t curr_t = clock();
	manager->next_restart_backtrack += manager->restart_backtrack_incr;
#if(EXPONENTIAL_GROWTH)
	manager->restart_backtrack_incr *= 2;
#else
	manager->restart_backtrack_incr += RESTART_INCR_INCR;
#endif
	if ((double)(curr_t-manager->start)/CLOCKS_PER_SEC < RESTART_TIME) return 0;
	
	//restart
	var* v = manager->vars;
	for (unsigned int i = 0; i < manager->var_count; v++)
	{
		v->pliteral->relevance_count = v->pliteral->lit_count;
		v->pliteral->last_lit_count = 0;
		v->nliteral->relevance_count = v->nliteral->lit_count;
		v->nliteral->last_lit_count = 0;
		i++;
	}
	manager->restart = 1;
	return 1;
}
#endif

/* 
 * n: start looking at position 0 in vars for next var
 * vars: array of pointers to vars
 */

BOOLEAN sat_aux(struct var **vars, struct cnf_manager *manager)
{
	struct var *select_zchaff_var(struct var **,struct cnf_manager *);
	void clause_deletion(struct cnf_manager *manager);

	//ADDED: (ADC 7/15/04)
#if(DELETE_CLAUSES)
	if (manager->assertions_count % CLAUSE_DELETION_INTERVAL == 0) clause_deletion(manager);
#endif

#if(RESTART)
	if(restart(manager)) return 1;
#endif
	//END ADDED

	struct var *var = select_zchaff_var(vars,manager);

	if(var==NULL) { 
		/* all variables are instantiated or irrelvant; */
		save_solution(manager);
		return 1; 
	}

	struct literal *literal = select_literal(var);

	//try positive literal
	if(decide_literal(literal,manager) && sat_aux(vars,manager)) {
		undo_decide_literal(manager);
		return 1;
	}
	undo_decide_literal(manager);

	/* first branch failed: we dont know if immediately or deeper in the tree */
	if(at_assertion_level(manager)) {
		return assert_cd_literal(manager) && sat_aux(vars,manager);
	}
	/* backtrack to an earlier level */
	else return 0;
}


BOOLEAN sat(struct cnf_manager *manager)
{
	CNF_VAR_INDEX n=manager->var_count;

	//vars holds a list of pointers to all vars
	struct var **vars = (struct var **) calloc(n,sizeof(struct var *));

	/* start with natural order: 1, 2, 3,... */
	for(CNF_VAR_INDEX i=0; i<n; i++) vars[i] = index2varp(i+1,manager);

	//Modified to handle pure literals(ADC 7/8/04)
	BOOLEAN status = assert_unit_clauses_and_pure_literals(manager) && sat_aux(vars,manager);

	//ADDED (ADC 7/22/04)
#if(RESTART)
	while(manager->restart)
	{
		manager->restart = 0;
		printf("restart...\n");
		status = sat_aux(vars,manager);
	}
#endif
	//END ADDED
	
	//retract assertions (unit clauses) if any
	undo_assert_unit_clauses(manager);

	free(vars);

	if(status) {
		if (verify_solution(manager)) {
//			print_solution(manager);
			printf("\nVerified solution");
		}
		else {
			printf("\n\n****Incorrect solution!!!\n");
			exit(1);
		}
	}

	return status;
}
