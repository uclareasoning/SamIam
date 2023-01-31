// Actually, a line must contain MAX_LINE_LEN-2 chars at most
#define MAX_LINE_LEN 100000
// Actually, a clause must contain MAX_CLAUSE_LEN-1 literals at most
#define MAX_CLAUSE_LEN 1025
// maximum size of a label string: used for printing clauses, var sets, etc/
#define STRING_LABEL_SIZE 1024

//ADDED globals: (ADC 7/15/04)
#define CLAUSE_DELETION_INTERVAL 5000 // number of backtracks before we clean up learned clauses
#define IRRELEVANCE_FACTOR 20 // how irrelevant a clause must be to delete
#define MIN_LITERALS_TO_DELETE 100 // minimum size of a clause to consider for deletion
#define MAX_LITERALS_TO_DELETE 5000 // delete all clauses larger than this

#define RESTART_TIME 50 //(in seconds)
#define INITIAL_RESTART_INCR 40000 //how many backtracks to increment by
#define RESTART_INCR_INCR 100 //multiply the restart_incr by this amount each time
//END added globals

#define BOOLEAN_NULL -1

#define MIN(A,B) ( (A)<(B) ? (A) : (B) )
#define MAX(A,B) ( (A)>(B) ? (A) : (B) )
#define KB 1024.0
#define MB (1024.0*1024.0)

