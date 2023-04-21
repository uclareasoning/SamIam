#include <stdlib.h>
#include <stdio.h>
#include <limits.h>

#if(PROFILE)
#include "timer.h"
#endif
#include "types.h"
#include "flags.h"
#include "cnf.h"
#include "globals.h"

#define IND(literal,clause) (literal)==(clause)->literals[1]

/*
 * Add a set of literals (clause) to the list of watched clauses for literal.
 * It is required that literal be in either the 0 or 1 position of clause->literals.
 *
*/
void add_watched_clause(struct clause *clause, struct literal *literal)
{
	++(literal->watched_clauses_count);

	/* i=0 when literal=literals[0] and i=1 when literal=literals[1] */
	int i = IND(literal,clause); //(literal==clause->literals[1]);

	/* current head of watched clauses literal */
	struct clause *head = literal->watched_clauses;

	/* add clause to head of list */
	clause->next_clause[i] = head;
	clause->prev_clause[i] = NULL;

	literal->watched_clauses = clause;
	if(head!=NULL) head->prev_clause[IND(literal,head)] = clause; //literal==head->literals[1]
}

/* 
 * Remove clause from the list of clauses being watched by literal.
 * It is required that literal be in either the 0 or 1 position of clause->literals.
 */
void remove_watched_clause(struct clause *clause, struct literal *literal) 
{
	--(literal->watched_clauses_count);

	/* i=0 when literal=literals[0] and i=1 when literal=literals[1] */
	int i = IND(literal,clause); //(literal==clause->literals[1]);

	struct clause *p = clause->prev_clause[i];
	struct clause *n = clause->next_clause[i];

	if(p==NULL) literal->watched_clauses = n;
	else p->next_clause[IND(literal,p)] = n; //literal==p->literals[1]

	if(n!=NULL) n->prev_clause[IND(literal,n)] = p; //literal==n->literals[1]

	/*
	clause->next_clause[i]=NULL;
	clause->prev_clause[i]=NULL;
	*/
}



/* Switch the watched literal from now_watched to then_watched */
void move_watched_literal(struct literal **now_watched, struct literal **then_watched, struct clause *clause)
{
	remove_watched_clause(clause,*now_watched);
	/* Switching must be done after calling remove and before calling add */
	struct literal *l = *now_watched;
	*now_watched = *then_watched;
	*then_watched = l;
	add_watched_clause(clause,*now_watched);
}


struct literal **find_unresolved_literal(struct clause *clause)
{
	//should start looking from start +1
	/* search from start to end */
	struct literal **literals = clause->start;
	while(*(++literals)!=NULL) {
		if((*literals)->resolved==0) {
			return clause->start = literals;
		}
	}
	/* search from literals[2] to start-1 */
	literals = clause->literals+1;
	while(literals++!=clause->start) {
		if((*literals)->resolved==0) {
			return clause->start = literals;
		}
	}

	return NULL;
}


BOOLEAN set_literal(struct literal *cond_l, struct cnf_manager *cnf_manager)
{
	
#if(PROFILE)
	CYCLES start_t = GetMachineCycleCount();
#endif

	void analyze_conflict(struct clause *, CNF_VAR_INDEX, struct cnf_manager *);

	/* last: keeps track of literals that needs processing:
	literals that need to be processed are at [top....last-1]
	they are processed starting at top and then moving towards
	last-1 */
	struct literal **last = cnf_manager->sliterals_stack_top;

	/* literal to be processed */
	*(last++) = cond_l;
	/* all literals derived at this level */
	CNF_VAR_INDEX slevel = cond_l->slevel;

	while (last > (cnf_manager->sliterals_stack_top)) { /* more literals to process */

		/* the literal to process next */
		struct literal *literal = *(cnf_manager->sliterals_stack_top)++;
		struct literal *nliteral = literal->negation;
		/* resolve negation */
		nliteral->resolved = 1;
		nliteral->rlevel=slevel;
		/* literal has been implied... */
		++(cnf_manager->implication_count);

		#if(COND_DEBUG)
		printf(" %d(%d)",literal->index,literal->slevel);
		#endif
			
		/* Increment subsumed counts, and possibly decrement active_occurrences
		 * This affects ORIGINAL clauses only */
		struct clause *clause;
/*
		struct clause **clauses = literal->clauses;
		if(literal->implying_clause==NULL) {
			// decision literal 
			while((clause = *clauses++) != NULL) {
				if(++(clause->decision_subsumed_count)==1 && clause->subsumed_count==0) {
					struct literal **literals = clause->literals;
					while(*literals != NULL) --((*literals++)->active_occurences);
				}
			}
		}
		else {
			// implied literal 
			while((clause = *clauses++) != NULL) {
				if(++(clause->subsumed_count)==1 && clause->decision_subsumed_count==0) {
					#if(SEP_CHECK==2)
					decrement_separator_counts(clause);
					#endif
					struct literal **literals = clause->literals;
					while(*literals != NULL) --((*literals++)->active_occurences);
				}
			}
		}
*/

		/* Check watched clauses of nliteral
		This affects all clauses (original and learned) */
		struct clause *next = nliteral->watched_clauses;
		/* We are actually keeping locations of found, this_literal (nliteral) and other_literal */
		struct literal **literals, **found, **this_literal, **other_literal;

		while((clause=next)!=NULL) {

			literals = clause->literals;
			int i = (nliteral==literals[1]);
			/* should be done before clause is possibly deleted from watched list of nliteral */
			next = clause->next_clause[i];
			/* first two literals are watched */
			this_literal = literals+i;
			other_literal = literals+1-i;

#if(PROFILE)
			CYCLES start_t2 = GetMachineCycleCount();
#endif
			/* search for an unresolved literal in literals[2...] */
			if(clause->size>2) found=find_unresolved_literal(clause);
			else found=NULL;
#if(PROFILE)
			cnf_manager->find_ul_cycles += (GetMachineCycleCount()-start_t2);
#endif

			/* Case 1: found unresolved literal different from other */
			if(found!=NULL) move_watched_literal(this_literal,found,clause);

			/* literals[2...] are resolved.
			
			/* Case 2: contradiction */
			else if((*other_literal)->resolved) {
				
				#if(COND_DEBUG)
				printf("\nConflicting clause...");
				printf_clause(clause);
				#endif

				/* reset literals on stack that are yet to be processed */
				struct literal **stack=cnf_manager->sliterals_stack_top;
				while(stack < last) {
					struct literal *to_do = *stack++;
					to_do->slevel=0;
					to_do->implying_clause=NULL;
				}
#if(PROFILE)
				cnf_manager->set_cycles += (GetMachineCycleCount()-start_t);
#endif
				/* analyze conflict */
				analyze_conflict(clause,slevel,cnf_manager);
				return 0;
			}
			else { /* Cases 3 and 4: other literal is implied */
				struct literal *unit_literal = *other_literal;
				if(unit_literal->slevel==0) { 
					/* other literal is free, and not on to_do */
					unit_literal->slevel=slevel;
					unit_literal->implying_clause=clause;
					*(last++) = unit_literal;
				}
			}
		}
	}

#if(PROFILE)
	cnf_manager->set_cycles += (GetMachineCycleCount()-start_t);
#endif

	return 1;
}


/* 
 * Undo all set literals at level 
 */
void erase_level(CNF_VAR_INDEX level, struct cnf_manager *cnf_manager)
{
#if(PROFILE)
	CYCLES start_t = GetMachineCycleCount();
#endif

	while (cnf_manager->sliterals_stack_top > cnf_manager->first_sliteral_at_level[level]) {
		
		struct literal *literal = *(--(cnf_manager->sliterals_stack_top));
		struct literal *nliteral = literal->negation;

		#if (COND_DEBUG)
		printf("%d ",literal->index);
		#endif
	
/*
		struct clause **clauses=literal->clauses;
		struct clause *clause;	
		// Decrement subsumed counts, and possible increment active_occurrences
		// This affects orginal clauses only 
		if(literal->implying_clause==NULL) {
			// decision literal 
			while((clause = *clauses++) != NULL) {
				if(--(clause->decision_subsumed_count)==0 && clause->subsumed_count==0) {
					struct literal **literals = clause->literals;
					while(*literals != NULL) ++((*literals++)->active_occurences);
				}
			}
		}
		else { // implied literal 
			while((clause = *clauses++) != NULL) {
				if(--(clause->subsumed_count)==0 && clause->decision_subsumed_count==0) {
					#if(SEP_CHECK==2)
					increment_separator_counts(clause);
					#endif
					struct literal **literals = clause->literals;
					while(*literals != NULL) ++((*literals++)->active_occurences);
				}
			}
		}
*/
		literal->slevel=0;
		nliteral->rlevel=0;
		literal->implying_clause=NULL;
		nliteral->resolved=0;

	}

#if(PROFILE)
	cnf_manager->unset_cycles += (GetMachineCycleCount()-start_t);
#endif

}


BOOLEAN decide_literal(struct literal *literal, struct cnf_manager *manager) 
{
	if(manager->current_decision_level > manager->maximum_decision_level) {
		manager->maximum_decision_level = manager->current_decision_level;
	}
	CNF_VAR_INDEX level = ++(manager->current_decision_level);
	++(manager->decisions_count);

	literal->slevel = level;
	literal->implying_clause = NULL;

	/* save the top of sliterals stack, which marks the place where the
	literals set at this level will start */
	manager->first_sliteral_at_level[level] = manager->sliterals_stack_top;

	#if (COND_DEBUG) 
	printf("\n\nDecision %d at L%d ==> deriving:",literal->index,manager->current_decision_level); 
	#endif

	return set_literal(literal,manager);
}

/* 
 * Undo all implications at current decision level.
 * Some of these have been derived immediately after decisions,
 * while others have been added by assert_literal
 */

void undo_decide_literal(struct cnf_manager *manager) 
{
	#if (COND_DEBUG) 
	printf("\nUndoing decision at L%d ==> retracting: ",manager->current_decision_level); 
	#endif
	
	CNF_VAR_INDEX level = (manager->current_decision_level)--;
	erase_level(level,manager);
}

/* 
 * This is called after a contradiction has been found, a conflict
 * report constructed, and backtracking took place to the assertion
 * level (contradiction removed). This report identifies a literal 
 * (conflict driven assertion) which is implied by earlier decisions
 * and the conflict driven clause. 
 *
 * This function will assert this cd_literal as an implication, and
 * will possibly learn the conflict driven clause
 *
 * If this assertion also leads to a contradiction, a new report will 
 * be constructed.
 */
BOOLEAN assert_cd_literal(struct cnf_manager *manager) 
{
	void add_conflict_driven_clause(struct cnf_manager *);

	//Add conflict directed clause
	//This must be done first, as it sets the lock parameter for iliterals
	add_conflict_driven_clause(manager);

	struct conflict_report *report = manager->conflict_report;
	struct literal *literal = report->aliteral;
	struct clause *clause = report->clause;
	
	literal->slevel = report->assertion_level;
	literal->implying_clause = clause;

	++(manager->assertions_count);
	
	#if (COND_DEBUG) 
	printf("\n\nAssertion %d ==> deriving:",literal->index); 
	#endif

	return set_literal(literal,manager);
}

/*
 * Assert all unit clauses at decision level 1.
 * Also asserts all pure literals.
 * Returns 1 if that succeeds, and 0 otherwise.
 */
BOOLEAN assert_unit_clauses_and_pure_literals(struct cnf_manager *manager)
{
	for(CNF_CLAUSE_INDEX i=0; i<manager->clause_count; i++) {
		struct clause *clause = manager->clauses+i;
		if(clause->size==1) {
			struct literal *literal = clause->literals[0];

			#if (COND_DEBUG) 
			printf("\n\nAsserting unit clause %d at L1 ==> deriving:",literal->index); 
			#endif

			literal->slevel=1;
			literal->implying_clause=NULL;

			if(!set_literal(literal,manager)) return 0;
		}
	}

	//Assert all pure literals (ADC 7/8/04)
	for(CNF_VAR_INDEX j=0; j<manager->var_count; j++) 
    {
		struct var *var = manager->vars+j;
		if (var->pliteral->occurences == 0)
		{
			var->nliteral->slevel=1;
			var->nliteral->implying_clause=NULL;
			if(!set_literal(var->nliteral,manager)) return 0;
		}
		else if(var->nliteral->occurences == 0)
		{
			var->pliteral->slevel=1;
			var->pliteral->implying_clause=NULL;
			if(!set_literal(var->pliteral,manager)) return 0;
		}
    }

	return 1;
}


void undo_assert_unit_clauses(struct cnf_manager *manager)
{
	erase_level(1,manager);
}

/**************************************************************************
*
* Solution related utilities
*
**************************************************************************/

/* copies the set literals from the sliteral stack to the
 * solution stack
 */
void save_solution(struct cnf_manager *manager)
{
	struct literal **stack = manager->sliterals_stack;
	while(stack < manager->sliterals_stack_top) *(manager->solution_stack_top)++ = *stack++;
}


/* prints the current solution */
void print_solution(struct cnf_manager *manager)
{
	printf("\nSolution (%d): ",manager->solution_stack_top-manager->solution_stack);
	struct literal **stack = manager->solution_stack;
	while(stack < manager->solution_stack_top) printf("%d ",(*stack++)->index);

}


/*******************************************************************************/
/* debugging utilities
/*******************************************************************************/


/* Checks whether clause is independent of every other clause: 
 * no shared free variables with any other un-subsumed clause */
BOOLEAN isolated_clause(struct clause *clause) {
	if(subsumed_clause(clause)) return 1;
	else {
		struct literal **literals = clause->literals;
		struct literal *literal;
		while((literal = *literals++) != NULL) {
			if(literal->resolved==0 && (literal->active_occurences+literal->negation->active_occurences)!=1) {
				return 0;
			}
		}
		return 1;
	}
}


BOOLEAN verify_solution(struct cnf_manager *manager)
{
	CNF_VAR_INDEX count=0;

	//set all literals in solution
	struct literal **stack = manager->solution_stack;

	while(stack < manager->solution_stack_top) {

		struct literal *literal = *stack++;

		if(literal->resolved==1) {
			printf("\nerror: Inconsistency 1: %d",literal->index);
			return 0;
		}

		if(literal->negation->resolved==0) {
			if(!decide_literal(literal,manager)) {
				printf("\nerror: Inconsistency 2: %d",literal->index);
				return 0;
			}
			else ++count;
		}
	}

	//check all clauses are either subsumed or isolated
	for(CNF_CLAUSE_INDEX i=0; i<manager->clause_count; i++) {
		struct clause clause = manager->clauses[i];
		if(!subsumed_clause(&clause)) { //clause not subsumed!
			//if(!isolated_clause(&clause)) {
				printf_clause(&clause);
				printf("\nerror: clause %d not subsumed and not isolated",clause.index);
				return 0;
			//}
		}
	}
	//done
	while(count--) undo_decide_literal(manager);
	return 1;
}
