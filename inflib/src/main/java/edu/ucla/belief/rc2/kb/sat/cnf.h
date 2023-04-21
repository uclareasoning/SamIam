#include <time.h>

#if(PROFILE)
#include "timer.h"
#endif

/* Defining the CNF structure. This is the base bone structure
 * needed to capture the DIMACS file. It can also be passed directly
 * from other programs (not necessarily the outcome of reading a
 * DIMACS file).
 */

struct cnf {

	/* size of cnf */
	CNF_VAR_INDEX vc;
	CNF_CLAUSE_INDEX cc;

	/* a two dimensional array holding clauses: each clause is an
	  array of indiced ended by 0 */
	CNF_LITERAL_INDEX **clauses;
};



/* Defining a variable structure */
struct var {
	/* index of variable; starts at 1 */
	CNF_VAR_INDEX index;

	/* +ve and -ve literals of var */
	struct literal *pliteral, *nliteral;

	/* number of clauses in which variable appear */
	CNF_CLAUSE_INDEX occurences;

	/* a list of pointers to clauses in which var appears */
	struct clause **clauses;

	/* used to mark var in various algorithms */
	VAR_MARK mark;
};


/* Defining a literal structure */
struct literal {
	/* index of literal: +ve literals 1 and up, -ve literals -1 and down */
	CNF_LITERAL_INDEX index;

	/* variable of literal */
	struct var *var;
	/* negation of literal */
	struct literal *negation;

	/* number of ORIGINAL clauses in which literal appears */
	CNF_CLAUSE_INDEX occurences;
	/* number of ORIGINAL, un-subsumed clauses in which literal appears */
	CNF_CLAUSE_INDEX active_occurences;
	/* a list of pointers to ORIGINAL clauses in which literal appears */
	struct clause **clauses;

	/* A list of pointers to watched clauses (both original and learned)
	  We only keep track of the clause literals, not the clause itself
	  This allows to implement learned clauses as simply a set of literals
	  Avoding the overhead of additional clause information (which is not
	  need for learned clauses) */

	/* This list is augmented as clauses are learned
	  We only keep track of the clause literals, not the clause itself */
	struct clause *watched_clauses;
	/* count of clauses being watched by this literal */
	CNF_CLAUSE_INDEX watched_clauses_count;

	/* the clause that implied this literal in the implication graph */
	struct clause *implying_clause;

	/* >0 means literal is resolved.
	if literal is resolved, then its negation is set */
	BOOLEAN resolved;
	/* used to record level at which literal is decided or implied (set) */
	CNF_VAR_INDEX slevel;
	/* used to record level at which literal is resolved */
	/* slevel(l) = rlevel(not l) */
	CNF_VAR_INDEX rlevel;

	LITERAL_MARK mark;

	/* the score used by zchaff to order variables */
	double relevance_count;

	//MODIFIED (ADC 7/22/04)
	//this will still operate identically, but now I'll be able to find out how many times a literal
	//appears in ALL clauses
	/* a counter used to store the number of learned clauses for every 255 decisions */
	double lit_count;
	double last_lit_count;

};


/* Defining a clause structure */
struct clause {

	/* index of clause; starts at 1 */
	CNF_CLAUSE_INDEX index;
	/* number of literals in clause */
	CNF_VAR_INDEX size;
	/* first two literals are watched */
	struct literal **literals;
	/* start here when looking for an unresolved literal */
	struct literal **start;

	/* Stores pointers to next clauses in watched clause list.
	The first pointer is used by literal[0], the second is used
	by literal[1]. */
	struct clause *next_clause[2];
	struct clause *prev_clause[2];

	/* The following are only used for original clauses */

	/* number of literals subsuming clause */
	CNF_VAR_INDEX subsumed_count;
	/* number of literals (set by decision) subsuming clause */
	CNF_VAR_INDEX decision_subsumed_count;
	/* used to mark clause in a variety of algorithms */
	CLAUSE_MARK mark;

	//ADDED: ADC (7/17/04)
	clause* next_cd_clause; //only for conflict-driven clauses - points to the next one.
};


/* Defining a CNF manager.
 * This structure keeps track of the data needed to condition/uncondition,
 * perform unit resolution, and cache the states of different subsets
 * of the cnf.
 */
struct cnf_manager {

	CNF_VAR_INDEX var_count;
	CNF_CLAUSE_INDEX clause_count;

	struct var *vars; /* array of all cnf vars (array of var structs) */
	struct clause *clauses; // array of all cnf clauses (array of clause structs) */

	/* another measure of cnf size */
	unsigned long literal_instance_count; /* number of literal instances in cnf */

	/* keeps of track of decision level for decide_literal */
	/* first decision level at 2 */
	CNF_VAR_INDEX current_decision_level;

	/* first_sliteral_at_level[d] holds a pointer to the position in the
	sliterals_stack where the literals set at level d start */
	struct literal ***first_sliteral_at_level;

	struct literal **sliterals_stack;
	struct literal **sliterals_stack_top;

	/* array for holding conflict directed clause */
	struct literal **cdc_stack;
	struct literal **cdc_stack_top;

	/* array for use as queue when searching the implication graph using bfs */
	struct literal **bfs_queue;

	/* stack for holding solution to sat problem */
	struct literal **solution_stack;
	struct literal **solution_stack_top;

	/* conflict report to be populated when a contradiction is discovered */
	struct conflict_report *conflict_report;

	/* counts the number of decisions made */
	unsigned long decisions_count;
	/* counts the number of conflict-driven assertions made */
	unsigned long assertions_count;
	/* counts the number of literals ever set to true */
	unsigned long implication_count;

	clause* cd_head; // the first conflict-driven clause
	clause* cd_tail; // the last conflict-driven clause

	// ADDED: (ADC 7/15/04)
#if(DELETE_CLAUSES)
	unsigned long deletions_count; // counts number of clauses we've deleted
	unsigned long deleted_literals_count; // counts number of literals we've deleted
#endif

#if(RESTART)
    unsigned int next_restart_backtrack;		//number of backtracks before we restart
    unsigned int restart_backtrack_incr;	//how much to increment above variable by
	clock_t start;
	BOOLEAN restart;	// a flag to indicate we're restarting
#endif
	// END ADDED

	/* depth of explored search tree */
	CNF_VAR_INDEX maximum_decision_level;

	/* number of conflict driven clauses */
	CNF_CLAUSE_INDEX cd_clause_count;
	/* number of literals in conflict driven clauses */
	unsigned long cd_literal_count;

#if(PROFILE)
	CYCLES set_cycles;
	CYCLES unset_cycles;
	CYCLES conflict_analysis_cycles;
	CYCLES find_ul_cycles;
	CYCLES var_order_cycles;
#endif

};


/* -literals is a vector of literals ending with NULL, and is
// interpreted as a clause (disjunction of literals) which was
// derived from a contradiction
//
// -this contradiction is due to having set aliteral to its
// opposite value. hence, the remedy is to assert aliteral
// instead and set literals are the justification for this
// conflict driven assertion
//
// -note that aliteral also appears in literals
//
// -aliteral can be NULL, in which case the CNF is inconsistent
// -literals is never null (but can be a null vector) */
struct conflict_report {
	struct clause *clause; /* conflict_driven clause */
	CNF_VAR_INDEX backtrack_level; /* highest decision level which is responsible for contradiction */
	CNF_VAR_INDEX assertion_level; /* second highest level */
	struct literal *aliteral; /* assertion literal */
	/* A literal in cdc at second highest decision level
	It has two uses: its level is the assertion level, and
	will be the second literal to be watched */
	struct literal *bliteral;
};


BOOLEAN sat(struct cnf_manager *);

struct cnf_manager *construct_cnf_manager(struct cnf *);
void free_cnf_manager(struct cnf_manager *);
void print_cnf_manager_properties(struct cnf_manager *, FILE *);

struct var *index2varp(CNF_VAR_INDEX, struct cnf_manager *);

struct cnf *get_cnf_from_file(char *);
struct cnf *get_cnf_from_file_dla(const char *);
void free_cnf(struct cnf *);
void print_cnf_properties(struct cnf *);

BOOLEAN assert_unit_clauses_and_pure_literals(struct cnf_manager *); //Modified (ADC 7/8/04)
void undo_assert_unit_clauses(struct cnf_manager *);
BOOLEAN decide_literal(struct literal *,struct cnf_manager *);
void undo_decide_literal(struct cnf_manager *);
BOOLEAN assert_cd_literal(struct cnf_manager *);

BOOLEAN at_assertion_level(struct cnf_manager *);

void save_solution(struct cnf_manager *);
void print_solution(struct cnf_manager *);

BOOLEAN verify_solution(struct cnf_manager *);
BOOLEAN isolated_clause(struct clause *);

char *literals2string(struct literal **, CNF_VAR_INDEX);
char *vars2string(struct var **, CNF_VAR_INDEX);
char *clauses2string(struct clause **, CNF_CLAUSE_INDEX);
char *clause2string(struct clause *);
void printf_clause(struct clause *);
void printf_literals(struct literal **);

struct literal *select_literal(struct var *);

BOOLEAN eliminated_var(struct var *);
BOOLEAN instantiated_var(struct var *);
BOOLEAN decided_var(struct var *);
BOOLEAN irrelevant_var(struct var *);
BOOLEAN relevant_var(struct var *);

BOOLEAN subsumed_clause(struct clause *);
BOOLEAN subsumed_clause_by_decision(struct clause *);
