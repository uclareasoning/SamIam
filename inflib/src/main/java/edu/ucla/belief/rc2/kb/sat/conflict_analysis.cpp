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


/* resets the mark of all literals and negations on the implied_literal stack */
void clear_implied_literal_marks(struct cnf_manager *manager) {
	struct literal **stack = manager->sliterals_stack;
	while(stack < manager->sliterals_stack_top) {
		struct literal *sliteral = *stack++;
		sliteral->mark=0;
		sliteral->negation->mark=0;
	}
}

/* helper function for zchaff_conflict_clause
 * looks at all literals in the antecedent clause
 * if the literal was decided on the current level, it is marked
 * if not, the literal is added to the conflict clause
 * ADDED: ADC 7/8/04
 */

void mark_vars_at_level(struct literal **lits, struct cnf_manager *manager, CNF_VAR_INDEX& q_count, 
						CNF_VAR_INDEX clevel, CNF_VAR_INDEX& BL, CNF_VAR_INDEX& ccl_count, 
						CNF_VAR_INDEX& clevel_count, struct literal *this_lit)
{
	struct literal *literal;
	while((literal=*lits++) != NULL) 
	{
		if (literal == this_lit)
		{
			continue;
		}
		CNF_VAR_INDEX L = literal->rlevel;
		if(L==clevel)  //if it was decided on current level, mark the literal
		{
			if (literal->negation->mark == 0)
			{
				++clevel_count;
				++q_count;
				literal->negation->mark=1;
			}
		}
		else if (literal->negation->mark != 2)  //if we haven't already added it to the conflict clause, do so
		{
			ccl_count++;
			literal->negation->mark=2;
			if (BL == 0)
			{
				manager->cdc_stack[1]=literal;
				BL=L;
			}
			else if (L > BL)
			{
				*(manager->cdc_stack_top)++ = manager->cdc_stack[1];
				manager->cdc_stack[1] = literal;
				BL=L;
			}
			else
			{
				*(manager->cdc_stack_top)++ = literal;
			}
		}
	}
}

/* 
 * Generates a conflict clause using the zchaff method.
 * Backtracks over the assignment stack,
 * and stops when only a single variable from the conflict level
 * is present in the conflict-driven clause.
 *
 * The order in which variables on the fringe are expanded is dictated
 * by the assignment stack: if literal a implies literal b, then b will
 * always be expanded first.
 *
 * The method starts with the original conflict clause, and keeps
 * resolving literals in the conflict clause until only one literal
 * from the conflict level appears in the clause. It also does this
 * by resolving literals that are closer to the conflict first.
 *
 * Note that this method uses marks of resolved literals (not set literals).
 */

//Modified to fix UIP bug (ADC 7/8/04)

CNF_VAR_INDEX zchaff_conflict_clause(CNF_VAR_INDEX clevel, struct literal **cc_literals, struct cnf_manager *manager)
{
	/* these marks will be used to indicate which a literal has been put on bfs queue */
	clear_implied_literal_marks(manager);

	/* stack where conflict directed clause will be stored (initialization) */
	manager->cdc_stack_top = manager->cdc_stack;

	/* aliteral: the conflict-driven assertion is initialized to NULL */
	*(manager->cdc_stack_top)++ = NULL;
	/* bliteral: a literal with a second highest level will be placed here */
	*(manager->cdc_stack_top)++ = NULL;
	/* now pointing to third element in stack */
	
	/* points to current element in assignment stack */
	struct literal **assignment = manager->sliterals_stack_top;

	/* number of literals in conflict driven clause */
	CNF_VAR_INDEX ccl_count = 0;
	/* number of literals in current conflict clause which are at current conflict level */
	CNF_VAR_INDEX clevel_count = 0;
	/* number of nodes in fringe */
	CNF_VAR_INDEX q_count=0;

	CNF_VAR_INDEX BL = 0;

	mark_vars_at_level(cc_literals, manager, q_count, clevel, BL, ccl_count, clevel_count, NULL);

	struct literal *literal;
	while(q_count) { /* fringe not empty */
		literal = *(--assignment);
		if (literal->mark != 1)
		{
			continue;
		}
		//printf("\nL %d(%d) q_count %d, c_l_count %d",literal->index,literal->negation->rlevel,q_count,clevel_count);
		--q_count;
		if(clevel_count==1 || literal->implying_clause==NULL) {
			/* literal is first UIP */
			/* clevel_count=1 from now on */
			++ccl_count;
			/* set aliteral */
			manager->cdc_stack[0]=literal->negation;
			break;
		}
		else { /* non-root node at clevel, and more than one node at clevel */
			--clevel_count;
			struct literal **parents = literal->implying_clause->literals;
			mark_vars_at_level(parents, manager, q_count, clevel, BL, ccl_count, clevel_count, literal);
		}
	}
	/* cdc is in cdc_stack now, with aliteral first, and bliteral second */
	
	return ccl_count;
}


/*
 * Populates the conflict report after constructing a conflict
 * driven clause.
 */
void construct_conflict_report(CNF_VAR_INDEX clevel, struct literal **cc_literals, struct cnf_manager *manager)
{	
	/* The confict driven assertion will be stored first. 
	This will be a literal at the conflict level, clevel.
	The second literal will be at the assertion level, that is,
	at the highest level which is lower than the conflict level. */

	/* count contains the size of conflict-driven clause */
	/* CNF_VAR_INDEX count = rel_sat_conflict_clause(clevel,cc_literals,manager); */
	CNF_VAR_INDEX count = zchaff_conflict_clause(clevel,cc_literals,manager); 

	/* conflict driven clause */
	struct clause *clause = (struct clause *) malloc(sizeof(struct clause));
	//count could be zero in which case contradiction is fatal
	clause->size = count; 
	/* allocate vector for conflict-driven literals (last element is null) */
	struct literal **literals = 
		(struct literal **) calloc(1+count,sizeof(struct literal *));
	clause->literals = literals;

	struct conflict_report *report = manager->conflict_report;
	report->clause = clause;
	
	/* copy the cdc from cdc_stack to newly created space */
	while(count--) literals[count] = manager->cdc_stack[count];

	report->aliteral = literals[0]; /* first in literals (null if cdc is empty) */
	report->bliteral = literals[1]; /* null ==> aliteral is an implied unit clause */
	
	if (report->aliteral != NULL) report->backtrack_level = report->aliteral->rlevel;
	else report->backtrack_level = 0;
	if (report->bliteral != NULL) report->assertion_level = report->bliteral->rlevel;
	else report->assertion_level = 1;

	#if(CDB_DEBUG)
	if(report->aliteral==NULL) printf("\n\n **Conflict analysis: fatal conflict...");
	else {
		printf("\n\n **Conflict analysis (B%d, A%d):\n %d implied by ",report->backtrack_level,report->assertion_level,report->aliteral->index);
		while(*literals != NULL) {
			printf("%d(%d) ",(*literals)->negation->index,(*literals)->rlevel);
			++literals;
		}
	}
	#endif
}

//ADDED: (ADC 7/15/04)
/*
 *  deletes irrelevant clauses
 */
#if(DELETE_CLAUSES)
void clause_deletion(struct cnf_manager *manager)
{
	void remove_watched_clause(struct clause *clause, struct literal *literal);

	//TODO: check when out of memory?

	clause *cl = manager->cd_head, *last = NULL;
	//iterate through all new conflict clauses
	while(cl != NULL)
	{
		//only consider clauses of a certain minimum size
		if (cl->size < MIN_LITERALS_TO_DELETE)
		{
			last = cl;
			cl = cl->next_cd_clause;
			continue;
		}
		unsigned int val_0_lits = 0, val_1_lits = 0, unknown_lits = 0;
		for (unsigned int i=0; i<cl->size; i++)
		{
			literal* lit = *(cl->literals+i);

			if (lit->resolved > 0) val_0_lits++;
			else if (lit->slevel == 0) unknown_lits++;
			else val_1_lits++;

			//if there are too many unknown or true literals in the clause, then it isn't very useful.
			//also, if the clause is too large, automatically delete unless it is currently an implying clause
			if (((unknown_lits + val_1_lits) > IRRELEVANCE_FACTOR) ||
				((cl->size > MAX_LITERALS_TO_DELETE) && (unknown_lits+val_1_lits > 1)))
			{
				//delete the clause
				manager->deletions_count++;
				manager->deleted_literals_count += cl->size;
				manager->cd_clause_count--;
				manager->cd_literal_count -= cl->size;

				#if(VSIDS)
				for (unsigned int j=0; j<cl->size; j++)
				{
					//decrement relevance counts
					--((*(cl->literals+j))->lit_count);
				}
				#endif

				remove_watched_clause(cl, *(cl->literals));
				remove_watched_clause(cl, *(cl->literals+1));

				if (manager->cd_head == cl) manager->cd_head = cl->next_cd_clause;
				else if (manager->cd_tail == cl) manager->cd_tail = last;
				else last->next_cd_clause = cl->next_cd_clause;

				free(cl->literals);
				free(cl);

				cl = last;
				break;
			}
		}
		last = cl;
		if (cl != NULL) cl = cl->next_cd_clause;
		else cl = manager->cd_head;
	}
	//we don't want to iterate back over these clauses next time, so start a new list.
	//TODO: this could be more efficient.  Allocate a length 5000 array.
	manager->cd_head = NULL;
}
#endif

/* sort ints in place */
int int_cmp(const void *a, const void *b) { 
	
	int ai = *((int *)a);
	int bi = *((int *)b);

	if(ai < bi) return -1;
	else if(ai == bi) return 0;
	else return 1;
}
void sort_int(int *list,unsigned count)
{
	qsort((void *)list,(size_t)count,sizeof(int),int_cmp);
}

/*
 * Adds the confict-driven clause captured by the conflict report,
 * and locks the literals so they will not be freed.
 *
*/
void add_conflict_driven_clause(struct cnf_manager *manager)
{
	void declare_watched_literals(struct clause *);
	void remove_watched_clause(struct clause *,struct literal *);
	
	struct conflict_report *report = manager->conflict_report;
	struct clause *cdc = report->clause;

	/* Conflict-driven clause is not empty (size=0), and not unit (size=1)
	Would be empty if contradiction is fatal (not due to previous decisions) */
	if(cdc->size>1) { 
		/* The construction of cd clauses ensures that the first 
		   two literals are set at the last two levels and, hence,
		   the ones to be watched */
		declare_watched_literals(cdc);

		#if(VSIDS)
		/* increment relevance counts */
		struct literal **literals = cdc->literals;
		//MODIFIED (ADC 7/22/04)
		while(*literals!=NULL) ++((*literals++)->lit_count);
		#endif

		/* update manager statistics */
		manager->cd_clause_count += 1;
		manager->cd_literal_count += report->clause->size;

		/* set clause index */
		cdc->index = manager->clause_count+manager->cd_clause_count;

		//ADDED: ADC (7/17/04)
		if (manager->cd_head == NULL)
		{
			manager->cd_head = cdc;
			manager->cd_tail = cdc;
			cdc->next_cd_clause = NULL;
		}
		else
		{
			manager->cd_tail->next_cd_clause = cdc;
			manager->cd_tail = cdc;
			cdc->next_cd_clause = NULL;
		}
		//END ADDED

		#if(CDB_DEBUG)
		printf("\nAdded CD clause");
		#endif
	}
}

/* A conflict is reached due to a set of decisions that together 
 * lead to a contradiction with the cnf.
 * Decisions are made sequentially, so each decision has a level.
 *
 * The goal of conflict analysis is to find a set of literals from 
 *  previous levels which are sufficient
 *  to cause a conflict with the last decision
 *
 * The side effect is to set the conflict report, which contains
 * additional information about the conflict-driven clause, such
 * as:
 *  --the backtrack level: the level whose erasure will clear the conflict
 *  --the assertion level: the level which can be used to imply the
 *    conflict driven assertion, given the conflict driven clause
 */
void analyze_conflict(struct clause *conflicting_clause, CNF_VAR_INDEX conflicting_level, struct cnf_manager *manager) 
{
#if(PROFILE)
	CYCLES start_t = GetMachineCycleCount();
#endif
	
	void save_ig_as_vcg(CNF_VAR_INDEX,struct clause*, struct cnf_manager *);

	#if (IG_DEBUG)
    save_ig_as_vcg(conflicting_level,conflicting_clause,manager);
	#endif

	construct_conflict_report(conflicting_level,conflicting_clause->literals,manager);

#if(PROFILE)
	manager->conflict_analysis_cycles += (GetMachineCycleCount()-start_t);
#endif

}

BOOLEAN at_assertion_level(struct cnf_manager *manager)
{
	return manager->conflict_report->assertion_level==manager->current_decision_level;
}

/*
 * Debugging utilities
 *
 */
void save_ig_as_vcg(CNF_VAR_INDEX clevel, struct clause *conflict_clause, struct cnf_manager *cnf_manager)
{
	FILE *filep = fopen("z-igraph.vcg","w");

    fprintf(filep,"graph: { title: \"implication graph\"");
    fprintf(filep,"\n\nsmanhatten_edges: no");
//    fprintf(filep, "\nlayoutalgorithm: tree");
    fprintf(filep, "\nscaling: 0.50");
    fprintf(filep, "\ntreefactor: 0.9");
    fprintf(filep, "\ncolor: darkmagenta");
    fprintf(filep, "\nedge.color: lightyellow");
    fprintf(filep, "\nedge.thickness: 1");
    fprintf(filep, "\nedge.arrowsize: 5");
    fprintf(filep, "\nnode.borderwidth: 1");
    fprintf(filep, "\nnode.bordercolor: lightgreen");
    fprintf(filep, "\nnode.textcolor: blue");
    fprintf(filep, "\nnode.color: lightgray");
    fprintf(filep, "\n");

	struct literal **top = cnf_manager->sliterals_stack;
	while(top < cnf_manager->sliterals_stack_top) {

		struct literal *literal = *top++;
			
		CNF_VAR_INDEX lindex = literal->index;
		CNF_VAR_INDEX slevel = literal->slevel;
		
		if(literal->implying_clause==NULL) { //decision variable
			fprintf(filep,"\nnode: { title: \"%d\" label: \"%d(%d)\" shape: box }",lindex,lindex,slevel);
		}
		else { //implied variable
			fprintf(filep,"\nnode: { title: \"%d\" label: \"%d(%d)\" shape: ellipse }",lindex,lindex,slevel);
			struct literal **iliterals = literal->implying_clause->literals;
			while(*iliterals != NULL) {
				if(*iliterals != literal) {
					fprintf(filep,"\nedge: { sourcename: \"%d\" targetname: \"%d\" }",(*iliterals)->negation->index,lindex);
				}
				iliterals++;
			}
		}
	}

	fprintf(filep,"\nnode: { title: \"conflict\" label: \"conflict at %d\" shape: triangle }",clevel);
	struct literal **ls = conflict_clause->literals;
	while(*ls != NULL) fprintf(filep,"\nedge: { sourcename: \"%d\" targetname: \"conflict\" }",(*ls++)->negation->index);

	fprintf(filep, "\n\n}");
	fclose(filep);
}
