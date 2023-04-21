#include <stdlib.h>
#include <stdio.h>
#include <limits.h>
#include <math.h>
#include <time.h>
#include <string.h>

#include "types.h"
#include "flags.h"
#include "cnf.h"
#include "globals.h"


/****************************************************************************
 * mapping indices of vars, literals, clauses to pointers
 * of corresponding structures. 
 ***************************************************************************/

struct var *index2varp(CNF_VAR_INDEX i, struct cnf_manager *manager) 
{
	return ((manager->vars)+i-1);
}


struct literal *index2literalp(CNF_LITERAL_INDEX i, struct cnf_manager *manager) 
{
	return ( (i>0) ? ((manager->vars)+i-1)->pliteral : ((manager->vars)-i-1)->nliteral );
}


struct clause *index2clausep(CNF_CLAUSE_INDEX i, struct cnf_manager *manager) 
{
	return ((manager->clauses)+i-1);
}


/****************************************************************************
 * Constructing a cnf manager for a given cnf
 ****************************************************************************/

struct cnf_manager *construct_cnf_manager(struct cnf *cnf)
{
	void construct_cnf_vars_and_literals(struct cnf_manager *);
	void construct_cnf_clauses(struct cnf_manager *, struct cnf *);
	void initialize_cnf_manager(struct cnf_manager *);

	struct cnf_manager *manager = (struct cnf_manager *) malloc(sizeof(struct cnf_manager));

	/* decisions start at 2 since:
	level 1: is for literals implied by cnf (without justification)
	level 0: is reserved to indicate variable is not implied */
	manager->current_decision_level = 1;

	manager->maximum_decision_level=0;
	manager->decisions_count=0;
	manager->assertions_count=0;
	manager->implication_count=0;
	manager->cd_clause_count = 0;
	manager->cd_literal_count = 0;
	manager->var_count = cnf->vc;
	manager->clause_count = cnf->cc;

	#if(PROFILE)
	manager->set_cycles = 0;
	manager->unset_cycles = 0;
	manager->conflict_analysis_cycles = 0;
	manager->find_ul_cycles = 0;
	manager->var_order_cycles = 0;
	#endif

	// ADDED (ADC 7/15/04)
	manager->cd_head = NULL;
	manager->cd_tail = NULL;
#if(DELETE_CLAUSES)
	manager->deletions_count = 0; 
	manager->deleted_literals_count = 0;
#endif
#if(RESTART)
	manager->restart_backtrack_incr = INITIAL_RESTART_INCR;
	manager->next_restart_backtrack = 0;
	manager->restart = 0;
#endif
	// END ADDED

	manager->conflict_report = (struct conflict_report *) malloc(sizeof(struct conflict_report));

	construct_cnf_vars_and_literals(manager);
	construct_cnf_clauses(manager,cnf);
	initialize_cnf_manager(manager);

	return manager;
}

void free_cnf_manager(struct cnf_manager *cnf_manager)
{
	/* free vars and literals */
	for(CNF_VAR_INDEX i=0; i<cnf_manager->var_count; i++) {
		struct var *var = i+cnf_manager->vars;
		struct literal *pl = var->pliteral;
		struct literal *nl = var->nliteral;
		free(pl->clauses);
		free(pl);
		free(nl->clauses);
		free(nl);
		free(var->clauses);
	}

	free(cnf_manager->vars); /* this takes care of freeing individual variable structures */

	/* free original clauses */
	for(CNF_CLAUSE_INDEX j=0; j<cnf_manager->clause_count; j++) {
		struct clause *clause = j+cnf_manager->clauses;
		free(clause->literals);
	}
	free(cnf_manager->clauses); /* this takes care of freeing individual clause structures */


	/* free learned clauses */
	struct clause *cdc = cnf_manager->cd_head;
	while(cdc!=NULL) {
		struct clause *next = cdc->next_cd_clause;
		free(cdc->literals);
		free(cdc);
		cdc=next;
	}
	
	/* free manager */
	free(cnf_manager->conflict_report);
	free(cnf_manager->bfs_queue);
	free(cnf_manager->cdc_stack);
	free(cnf_manager->sliterals_stack);
	free(cnf_manager->first_sliteral_at_level);
	free(cnf_manager->solution_stack);

	free(cnf_manager);
}

/* 
 * Constructs variable structures, initializes them, and keeps
 * track of them in the cnf manager.
 *
 * Constructs literal structures, initializes them partially, and
 * keeps track of them in the variable structures. 
 *
 * The initialization of literals is completed when clauses are added.
 *
*/

void construct_cnf_vars_and_literals(struct cnf_manager *manager)
{

	CNF_VAR_INDEX count = manager->var_count;

	//allocate variables and literals, and relevant
	manager->vars = (struct var *) calloc(count,sizeof(struct var));

	for(CNF_VAR_INDEX i=0; i<count; i++) {
		
		struct var *var = (manager->vars)+i;
		//set variable index & mark
		var->index = i+1;
		var->mark=0;

		//allocate literals
		struct literal *pliteral = (struct literal *) malloc(sizeof(struct literal));
		struct literal *nliteral = (struct literal *) malloc(sizeof(struct literal));
	
		var->pliteral = pliteral;
		var->nliteral = nliteral;

		pliteral->var = (manager->vars)+i;
		pliteral->index = var->index;
		pliteral->negation = nliteral;
		pliteral->occurences = 0;
		pliteral->active_occurences = 0;
		pliteral->relevance_count = 0;

		//MODIFIED (ADC 7/22/04)
		//pliteral->temp_relevance_count = 0;
		pliteral->last_lit_count = 0;
		pliteral->lit_count = 0;
		//END

		pliteral->resolved = 0;
	
		nliteral->var = (manager->vars)+i;
		nliteral->index = -1 * var->index;
		nliteral->negation = pliteral;
		nliteral->occurences = 0;	
		nliteral->active_occurences = 0;
		nliteral->relevance_count = 0;

		//MODIFIED (ADC 7/22/04)
		//nliteral->temp_relevance_count = 0;
		nliteral->last_lit_count = 0;
		nliteral->lit_count = 0;
		//END

		nliteral->resolved = 0;

	}
}


/* Constructs clause structures and keeps track of them in the cnf manager. */
void construct_cnf_clauses(struct cnf_manager *manager, struct cnf *cnf)
{
	
	void initialize_clause(CNF_CLAUSE_INDEX,CNF_VAR_INDEX, CNF_LITERAL_INDEX *, clause *, struct cnf_manager *);

	CNF_CLAUSE_INDEX count = manager->clause_count;
	manager->clauses = (struct clause *) calloc(count,sizeof(struct clause));

	for (CNF_CLAUSE_INDEX i=0; i<count; i++) {
		CNF_LITERAL_INDEX *ls = cnf->clauses[i];
		CNF_VAR_INDEX lcount;
		for(lcount=0; ls[lcount] != 0; lcount++);
		initialize_clause(i+1,lcount,ls, manager->clauses+i,manager);
	}
}


/* Initializing the clause structure, and then doing more initialization
 * of the literal structures. 
 */
void initialize_clause(CNF_CLAUSE_INDEX index, CNF_VAR_INDEX count, CNF_LITERAL_INDEX *ls, struct clause *clause, struct cnf_manager *manager)
{
	clause->index = index;
	clause->mark=0;
	clause->size = count;
	clause->subsumed_count = 0;
	clause->decision_subsumed_count = 0;

	for(unsigned i=0; i< count; i++) {
		struct var *var = index2varp(abs(ls[i]),manager);
		++(var->occurences);
	}

	clause->literals = (struct literal **) calloc(1+count,sizeof(struct literal *));
	for(unsigned i=0; i < count; i++) {
		struct literal *literal = index2literalp(ls[i],manager);
		(clause->literals)[i] = literal;
		++(literal->occurences);
	}
}

/*
 * Assumes that the first two literals in the clause are the ones
 * being watched.
 */
void declare_watched_literals(struct clause *clause)
{
	void add_watched_clause(struct clause *, struct literal *);

	struct literal **literals = clause->literals;

	add_watched_clause(clause,literals[0]);
	add_watched_clause(clause,literals[1]);

	/* start looking at start+1 */
	clause->start=literals+1;
	/*
	if(literals[2]==NULL) clause->start=NULL;
	else clause->start=literals+2;
	*/
}

/*
 * Initialize watched literals for original set of clauses.
 */
void initialize_watched_literals(struct cnf_manager *manager)
{
	//initialize watched_clauses counts to 0, vectors to NULL
	for(CNF_VAR_INDEX i=0; i<manager->var_count; i++) {	

		struct var *var = (manager->vars)+i;

		struct literal *pliteral = var->pliteral;
		pliteral->watched_clauses_count=0;
		pliteral->watched_clauses=NULL;

		struct literal *nliteral = var->nliteral;
		nliteral->watched_clauses_count=0;
		nliteral->watched_clauses=NULL;
	}

	//the two watched literals per clause are the first two in the list of literals
	for(CNF_CLAUSE_INDEX j=0; j<manager->clause_count; j++) {
		if(manager->clauses[j].size >1) declare_watched_literals(manager->clauses+j);
	}
}


/* Finishing the initialization of literals, clauses and manager. */
void initialize_cnf_manager(struct cnf_manager *manager)
{
	// allocate space for various fields of the var & literal structures
	for(CNF_VAR_INDEX i = 0; i < manager->var_count; i++) {
		
		struct var *var = (manager->vars)+i;
		var->clauses = (struct clause **) 
			calloc(1+var->occurences,sizeof(struct clause *));
		var->occurences = 0;

		struct literal *pliteral = (manager->vars[i]).pliteral;
		pliteral->clauses = (struct clause **) 
			calloc(1+pliteral->occurences,sizeof(struct clause *));
		pliteral->occurences = 0;
		pliteral->active_occurences = 0;

		//MODIFIED (ADC 7/22/04)
		//pliteral->temp_relevance_count = 0;
		pliteral->last_lit_count = 0;
		pliteral->lit_count = 0;
		//END

		pliteral->relevance_count = 0;
		pliteral->slevel = 0;
		pliteral->rlevel = 0;
		pliteral->implying_clause = NULL;
		pliteral->mark = 0;

		struct literal *nliteral = (manager->vars[i]).nliteral;
		nliteral->clauses = (struct clause **) calloc(1+nliteral->occurences,sizeof(struct clause *));
		nliteral->occurences = 0;
		nliteral->active_occurences = 0;

		//MODIFIED (ADC 7/22/04)
		//nliteral->temp_relevance_count = 0;
		nliteral->last_lit_count = 0;
		nliteral->lit_count = 0;
		//END

		nliteral->relevance_count = 0;
		nliteral->slevel = 0;
		nliteral->rlevel = 0;
		nliteral->implying_clause = NULL;
		nliteral->mark = 0;
	}

	// fill-in fields for literals and vars
	unsigned long literal_count = 0;

	//iterate over clauses
	for(CNF_CLAUSE_INDEX j = 0; j < manager->clause_count; j++) { 

		struct clause *clause = (manager->clauses)+j;

		//iterate over vars & literals of clause
		for(CNF_CLAUSE_SIZE k=0; k < clause->size; k++) { 

			struct var *var = (clause->literals[k])->var;
			(var->clauses)[var->occurences] = clause;
			++(var->occurences);

			struct literal *literal = (clause->literals)[k];
			(literal->clauses)[literal->occurences] = clause;
			++(literal->occurences);
			++(literal->active_occurences);
			#if(INITIALIZE_VSIDS)
			//MODIFIED (ADC 7/22/04)
			++(literal->lit_count);
			#endif
			++literal_count;
		}
	}
	// literal_count contains number of literals in all clauses
	manager->literal_instance_count = literal_count;

	//initialize watched literals of original clauses
	initialize_watched_literals(manager);

	//initialize stackes
	CNF_CLAUSE_INDEX stack_size = manager->var_count;

	/* a literal and its negation may be put on stack, hence, 2*stack_size */
	manager->sliterals_stack  = (struct literal **) calloc(2*stack_size,sizeof(struct literal *));
	manager->sliterals_stack_top = manager->sliterals_stack;

	/* allocate the decide/imply count stack */
	/* The two additional cells are for 0, 1 */
	manager->first_sliteral_at_level=(struct literal ***) calloc(2+stack_size,sizeof(struct literal **));
	manager->first_sliteral_at_level[1]=manager->sliterals_stack;

	//array holding conflict directed clause
	manager->cdc_stack  = (struct literal **) calloc(stack_size,sizeof(struct literal *));
	manager->cdc_stack_top = manager->cdc_stack;

	//queue for use in bfs of implication graph
	manager->bfs_queue = (struct literal **) calloc(stack_size,sizeof(struct literal *));

	//stack for holding sat solution
	manager->solution_stack  = (struct literal **) calloc(stack_size,sizeof(struct literal *));
	manager->solution_stack_top = manager->solution_stack;

}


void watched_clauses_stats(double *ave_s, CNF_CLAUSE_INDEX *max_s, CNF_CLAUSE_INDEX *max_occ, struct var **max_var,struct cnf_manager *manager)
{
	*ave_s=0;
	*max_s=0;
	*max_occ=0;
	*max_var;

	for(CNF_VAR_INDEX i=0; i<manager->var_count; i++) {
		struct var *var = manager->vars + i;

		CNF_CLAUSE_INDEX p_s = var->pliteral->watched_clauses_count;
		CNF_CLAUSE_INDEX n_s = var->nliteral->watched_clauses_count;
		
		if (p_s>*max_s) *max_s=p_s;
		if (n_s>*max_s) *max_s=n_s;
		*ave_s += (p_s+n_s);
		CNF_CLAUSE_INDEX occ=var->occurences; //var->pliteral->active_occurences+var->nliteral->active_occurences;
		if(occ > *max_occ) {
			*max_occ=occ;
			*max_var=var;
		}
	}
	*ave_s = *ave_s/(2*manager->var_count);
}


void print_cnf_manager_properties(struct cnf_manager *cnf_manager, FILE *filep)
{
	CNF_CLAUSE_INDEX max_s;
	double ave_s;
	CNF_CLAUSE_INDEX max_occ;
	struct var *max_var=NULL;

	watched_clauses_stats(&ave_s,&max_s,&max_occ,&max_var,cnf_manager);

#if(PROFILE)
	CYCLES ur_time = cnf_manager->set_cycles+cnf_manager->unset_cycles+cnf_manager->conflict_analysis_cycles;
#endif

	fprintf(filep,
		"\nCNF manager statistics (%d vars, %d clauses):\n"
		"  Decisions count: %d\n"
		"  Assertions count: %d\n"
		"  Implication count: %d\n"
		"  Maximum decision level: %d\n"
		"  Added CD-clauses: %d\n"
		"  Added CD-literals: %d\n"
#if(REMOVE_SUBSUMED_LEARNED_CLAUSES)
		"  Surviving CD-clauses: %d\n"
#endif
#if(DELETE_CLAUSES)
		"  Deleted CD-clauses: %d\n"   //ADDED: ADC (7/17/04)
		"  Deleted CD-literals: %d\n"  //ADDED: ADC (7/17/04)
#endif
		"  Average CD-clause size: %d\n"
		"  WL size: max=%d  ave=%.0f\n"
		"  Max occ var: v%d (%d occ)\n"
#if(PROFILE)
		"\n  UR TIME=%.2fs: set=%.1f%% (find ul=%.1f%%), unset=%.1f%%, conflicts=%.1f%%\n"
		"  Var order TIME=%.2fs\n\n"
#endif
		,cnf_manager->var_count,
		cnf_manager->clause_count,
		cnf_manager->decisions_count,
		cnf_manager->assertions_count,
		cnf_manager->implication_count,
		cnf_manager->maximum_decision_level,
		cnf_manager->cd_clause_count,
		cnf_manager->cd_literal_count,
#if(DELETE_CLAUSES)
		cnf_manager->deletions_count,		 //ADDED: ADC (7/17/04)
		cnf_manager->deleted_literals_count, //ADDED: ADC (7/17/04)
#endif
		(cnf_manager->cd_clause_count? cnf_manager->cd_literal_count/cnf_manager->cd_clause_count: 0),
		max_s,ave_s,
		max_var->index,
		max_occ
#if(PROFILE)
		,
		(double)ur_time/CYCLES_PER_SEC,
		(double)(100*cnf_manager->set_cycles)/(ur_time ? ur_time : 1),
		(double)(100*cnf_manager->find_ul_cycles)/(cnf_manager->set_cycles ? cnf_manager->set_cycles : 1),
		(double)(100*cnf_manager->unset_cycles)/(ur_time ? ur_time : 1),
		(double)(100*cnf_manager->conflict_analysis_cycles)/(ur_time ? ur_time : 1),
		(double)cnf_manager->var_order_cycles/CYCLES_PER_SEC
#endif
		); 
}


// returns a string which depicts the clause literals (indices)
char *clause2string(struct clause *clause)
{
	char *literals2string(struct literal **, CNF_VAR_INDEX);

	/* char temp[20]; */
	char *s = literals2string(clause->literals,clause->size);
	/*
	sprintf(temp," w: %d, ",clause->literals[0]->index);
	strcat(s,temp);
	sprintf(temp,"%d",clause->literals[1]->index);
	strcat(s,temp);
	*/
	return s;

}

void printf_clause(struct clause *clause)
{
	char *s = clause2string(clause);
	printf("\n%s",s);
	free(s);
}

// returns a string which depicts the var indices
char *vars2string(struct var **vars, CNF_VAR_INDEX n)
{
	char *string = (char *) calloc(STRING_LABEL_SIZE,sizeof(char));
	char temp[20];

	string[0]='[';
	string[1]='\0';

	unsigned int length = 1;
	for(CNF_VAR_INDEX i=0; i<n; i++) {
		if (length >= STRING_LABEL_SIZE-25) {
			strcat(string,"...");
			break;
		}
		if (i==n-1) sprintf(temp,"%d",vars[i]->index);
		else sprintf(temp,"%d,",vars[i]->index);
		length += (unsigned) strlen(temp);
		strcat(string,temp);
	}

	strcat(string,"]");

	return string;
}

// returns a string which depicts the literal indices
char *literals2string(struct literal **literals, CNF_VAR_INDEX n)
{
	char *string = (char *) calloc(STRING_LABEL_SIZE,sizeof(char));
	char temp[20];

	string[0]='(';
	string[1]='\0';
	unsigned int length = 1;
	for(CNF_VAR_INDEX i=0; i<n; i++) {
		if (length >= STRING_LABEL_SIZE-25) {
			strcat(string,"...");
			break;
		}
		if (i==n-1) {
			if (literals[i]->resolved) sprintf(temp,"%dr[%d] ",literals[i]->index,literals[i]->rlevel);
			else if (literals[i]->negation->resolved) sprintf(temp,"%ds[%d] ",literals[i]->index,literals[i]->slevel);
			else sprintf(temp,"%d",literals[i]->index);
		}
		else {
			if (literals[i]->resolved) sprintf(temp,"%dr[%d], ",literals[i]->index,literals[i]->rlevel);
			else if (literals[i]->negation->resolved) sprintf(temp,"%ds[%d], ",literals[i]->index,literals[i]->slevel);
			else sprintf(temp,"%d,",literals[i]->index);
		}
		length += (unsigned) strlen(temp);
		strcat(string,temp);
	}
	strcat(string,")");

	return string;
}

void printf_literals(struct literal **literals)
{
	CNF_VAR_INDEX n=0;
	while(literals[n++] != NULL);

	char *s = literals2string(literals,n-1);
	printf("\n%s",s);
	free(s);
}

// returns a string which depicts the clause indices
char *clauses2string(struct clause **clauses, CNF_CLAUSE_INDEX n)
{
	char *string = (char *) calloc(STRING_LABEL_SIZE,sizeof(char));
	char temp[20];

	string[0]='{';
	string[1]='\0';
	unsigned int length = 1;
	for(CNF_VAR_INDEX i=0; i<n; i++) {
		if (length >= STRING_LABEL_SIZE-25) {
			strcat(string,"...");
			break;
		}
		if (i==n-1) {
			if (subsumed_clause(clauses[i])) sprintf(temp,"%ds",clauses[i]->index);
			else sprintf(temp,"%d",clauses[i]->index);
		}
		else {
			if (subsumed_clause(clauses[i])) sprintf(temp,"%ds,",clauses[i]->index);
			else sprintf(temp,"%d,",clauses[i]->index);
		}
		length += (unsigned) strlen(temp);
		strcat(string,temp);
	}
	strcat(string,"}");

	return string;
}

