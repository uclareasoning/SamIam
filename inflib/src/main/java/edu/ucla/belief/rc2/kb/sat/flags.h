#define RSAT_VERSION 1.01

// set to 0 in case of compiling under C
// set to 1 in case of compiling under C++
#define CPP 1

// print progress information
#define PROGRESS 1
// time various functions (works only for windows)
#define PROFILE 0

//ADDED (ADC 7/15/04) 
#define DELETE_CLAUSES 0 // delete irrelevant clauses
#define RESTART 0 // random restarts
#define EXPONENTIAL_GROWTH 0 // the length between restarts will grow exponentially
//END ADDED

#define INITIALIZE_VSIDS 1
#define VSIDS 1    //Added (ADC 7/8/04)
// debugging of unit resolution
#define COND_DEBUG 0
// debugging of conflict-directed backtracking
#define CDB_DEBUG 0
// write implication graph as vcg file
#define IG_DEBUG 0
// prints cnf as being read
#define PRINT_CNF 0
// debug memory allocation
#define MEMORY_DEBUG 0
