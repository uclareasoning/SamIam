#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>

#if(PROFILE)
#include "timer.h"
#endif
#include "globals.h"
#include "types.h"
#include "flags.h"
#include "cnf.h"


/****************************************************************************
 * Reads a cnf from a file fname. Constructs and returns a cnf structure.
 ****************************************************************************/

struct cnf *get_cnf_from_file(char *fname) {

	int read_line(char *,unsigned long *, FILE *);


	FILE *ifp;
	if ( (ifp = fopen(fname,"r")) == NULL )
	{
		printf("\nCannot open file: %s\n",fname);
		exit(1);
	}

	#if (PRINT_CNF)
	printf("Reading %s...\n",fname);
	#endif

	struct cnf *cnf = (struct cnf *) malloc(sizeof(struct cnf));

	CNF_LITERAL_INDEX literals[MAX_CLAUSE_LEN]; /* temporary space for storing clause literals */
	CNF_CLAUSE_INDEX clause_index = 0; /* this will end up containing count of non-skipped clauses */
	char line[MAX_LINE_LEN]; /* temporary space for reading a line */
	line[MAX_LINE_LEN-1] = '\n'; /* anything other than '\0' */
	unsigned long i = 0; /* counter for line being read */
	char c; /* peeked at character */
	unsigned duplicate_literals_count=0;

	while ((c=getc(ifp)) != EOF) {

		if (isspace(c)) continue;
		else ungetc(c,ifp);

		if (c=='p') {
			/* cnf size declaration */
			read_line(line,&i,ifp);
			#if (PRINT_CNF)
			printf("%s",line);
			#endif
			if (sscanf(line,"p cnf %d %d",&(cnf->vc),&(cnf->cc))==2) {
				cnf->clauses = (CNF_LITERAL_INDEX **) calloc(cnf->cc,sizeof(CNF_LITERAL_INDEX *));
			}
			else {
				printf("Unknown Line %d: %s",i,line);
				exit(1);
			}
		}
		else if (c=='c' || c== '%' || c=='0') {
			/* comment line */
			read_line(line,&i,ifp);
			#if (PRINT_CNF)
			printf("%s",line);
			#endif
		}
		else if ((c=='-') || (isdigit(c) > 0)) {
			/* clause */
			int j;
			/* read clause into literals array */
			for(j=0; fscanf(ifp,"%d",&(literals[j])), (literals[j]!=0 && j<MAX_CLAUSE_LEN-1); j++);
			/* check if clause too long */
			if (j==MAX_CLAUSE_LEN-1 && literals[j]!=0) {
				printf("Clause at Line %d exceeds maximum size of %d!\n",i+1,MAX_CLAUSE_LEN-1);
				exit(1);
			}
			/* j contains size of clause, excluding 0 at end */
			/* allocating space for clause */
			cnf->clauses[clause_index] = (CNF_LITERAL_INDEX *) calloc(j+1,sizeof(CNF_LITERAL_INDEX));
			/* filling clause literals into new allocated array */
			BOOLEAN skip_clause = 0;
			int k=0;
			while(k<=j) {
				/* checks */
				BOOLEAN duplicate_literal = 0;
				for(int x=0; x<k; x++) {
					if(literals[x]==literals[k]) {
						/* duplicate literals in same clause */
						#if(PROGRESS)
						printf("\nDuplicate literal %d...",literals[k]);
						#endif
						duplicate_literal = 1;
						++duplicate_literals_count;
						break;
					}
					else if(abs(literals[x])==abs(literals[k])) {
						/* literal and its negation in same clause */
						skip_clause=1;
						/* keep reading clause; no interruption */
					}
				}
				#if (PRINT_CNF)
				printf("%d ",literals[k]);
				#endif
				if(duplicate_literal==0) {
					cnf->clauses[clause_index][k] = literals[k];
					k++;
				}
				else {
					literals[k]=literals[--j]; /* replace duplicate literal by last literal in clause */
					literals[j]=0; /* reduce clause size by 1 */
					/* not incrementing k as it yet has to be filled */
				}
			}
			#if (PRINT_CNF)
			printf("\n");
			#endif
			if(skip_clause==0) clause_index++; /* moving to next place in cnf array */
			else free(cnf->clauses[clause_index]); /* stay where you are and discard clause just read */
			read_line(line,&i,ifp);
		}
		else {
			read_line(line,&i,ifp);
			printf("Unknown Line %d: %s",i,line);
			exit(1);
		}
	}

		#if (PRINT_CNF)
		printf("...%d vars, %d clauses.\n",cnf->var_count,cnf->clause_count);
		#endif

	fclose(ifp);

	if(duplicate_literals_count) printf("\nSkipped %d duplicate literals.\n",duplicate_literals_count);
	if(cnf->cc != clause_index) {
		/* some clauses were skipped */
		printf("\nSkipped %d trivial clauses.\n",cnf->cc-clause_index);
		cnf->cc = clause_index;
		cnf->clauses =  (CNF_LITERAL_INDEX **) realloc(cnf->clauses,clause_index*sizeof(CNF_LITERAL_INDEX *));
	}

	return cnf;
}


struct cnf *get_cnf_from_file_dla(const char *fname) {

	int read_line(char *,unsigned long *, FILE *);


	FILE *ifp;
	if ( (ifp = fopen(fname,"r")) == NULL )
	{
		printf("\nCannot open file: %s\n",fname);
		//exit(1);
		return NULL;
	}

	#if (PRINT_CNF)
	printf("Reading %s...\n",fname);
	#endif

	struct cnf *cnf = (struct cnf *) malloc(sizeof(struct cnf));

	CNF_LITERAL_INDEX literals[MAX_CLAUSE_LEN]; /* temporary space for storing clause literals */
	CNF_CLAUSE_INDEX clause_index = 0; /* this will end up containing count of non-skipped clauses */
	char line[MAX_LINE_LEN]; /* temporary space for reading a line */
	line[MAX_LINE_LEN-1] = '\n'; /* anything other than '\0' */
	unsigned long i = 0; /* counter for line being read */
	char c; /* peeked at character */
	unsigned duplicate_literals_count=0;

	while ((c=getc(ifp)) != EOF) {

		if (isspace(c)) continue;
		else ungetc(c,ifp);

		if (c=='p') {
			/* cnf size declaration */
			read_line(line,&i,ifp);
			#if (PRINT_CNF)
			printf("%s",line);
			#endif
			if (sscanf(line,"p cnf %d %d",&(cnf->vc),&(cnf->cc))==2) {
				cnf->clauses = (CNF_LITERAL_INDEX **) calloc(cnf->cc,sizeof(CNF_LITERAL_INDEX *));
			}
			else {
				printf("Unknown Line %d: %s",i,line);
				//exit(1);
				return NULL;
			}
		}
		else if (c=='c' || c== '%' || c=='0') {
			/* comment line */
			read_line(line,&i,ifp);
			#if (PRINT_CNF)
			printf("%s",line);
			#endif
		}
		else if ((c=='-') || (isdigit(c) > 0)) {
			/* clause */
			int j;
			/* read clause into literals array */
			for(j=0; fscanf(ifp,"%d",&(literals[j])), (literals[j]!=0 && j<MAX_CLAUSE_LEN-1); j++);
			/* check if clause too long */
			if (j==MAX_CLAUSE_LEN-1 && literals[j]!=0) {
				printf("Clause at Line %d exceeds maximum size of %d!\n",i+1,MAX_CLAUSE_LEN-1);
				//exit(1);
				return NULL;
			}
			/* j contains size of clause, excluding 0 at end */
			/* allocating space for clause */
			cnf->clauses[clause_index] = (CNF_LITERAL_INDEX *) calloc(j+1,sizeof(CNF_LITERAL_INDEX));
			/* filling clause literals into new allocated array */
			BOOLEAN skip_clause = 0;
			int k=0;
			while(k<=j) {
				/* checks */
				BOOLEAN duplicate_literal = 0;
				for(int x=0; x<k; x++) {
					if(literals[x]==literals[k]) {
						/* duplicate literals in same clause */
						#if(PROGRESS)
						printf("\nDuplicate literal %d...",literals[k]);
						#endif
						duplicate_literal = 1;
						++duplicate_literals_count;
						break;
					}
					else if(abs(literals[x])==abs(literals[k])) {
						/* literal and its negation in same clause */
						skip_clause=1;
						/* keep reading clause; no interruption */
					}
				}
				#if (PRINT_CNF)
				printf("%d ",literals[k]);
				#endif
				if(duplicate_literal==0) {
					cnf->clauses[clause_index][k] = literals[k];
					k++;
				}
				else {
					literals[k]=literals[--j]; /* replace duplicate literal by last literal in clause */
					literals[j]=0; /* reduce clause size by 1 */
					/* not incrementing k as it yet has to be filled */
				}
			}
			#if (PRINT_CNF)
			printf("\n");
			#endif
			if(skip_clause==0) clause_index++; /* moving to next place in cnf array */
			else free(cnf->clauses[clause_index]); /* stay where you are and discard clause just read */
			read_line(line,&i,ifp);
		}
		else {
			read_line(line,&i,ifp);
			printf("Unknown Line %d: %s",i,line);
			//exit(1);
			return NULL;
		}
	}

		#if (PRINT_CNF)
		printf("...%d vars, %d clauses.\n",cnf->var_count,cnf->clause_count);
		#endif

	fclose(ifp);

	if(duplicate_literals_count) printf("\nSkipped %d duplicate literals.\n",duplicate_literals_count);
	if(cnf->cc != clause_index) {
		/* some clauses were skipped */
		printf("\nSkipped %d trivial clauses.\n",cnf->cc-clause_index);
		cnf->cc = clause_index;
		cnf->clauses =  (CNF_LITERAL_INDEX **) realloc(cnf->clauses,clause_index*sizeof(CNF_LITERAL_INDEX *));
	}

	return cnf;
}



/* Reads a line into the array line. Signals an error in case the
lines fills the array completely. */
int read_line(char *line,unsigned long *i, FILE *ifp)
{
	++*i;
	fgets(line,MAX_LINE_LEN,ifp);
	if (line[MAX_LINE_LEN-1]=='\0') {
		printf("Line %d exceeds max length of %d!\n",*i,MAX_LINE_LEN-2);
		exit(1);
	}
	return 0;

}

/* saves a cnf to file */
void save_cnf_to_file(char *fname,struct cnf *cnf)
{
	FILE *filep = fopen(fname,"w");

	fprintf(filep,"p cnf %d %d\n",cnf->vc,cnf->cc);

	int **clauses = cnf->clauses;
	for(unsigned i=0; i<cnf->cc; i++) {
		int *literal = *clauses++;
		while(*literal!=0) fprintf(filep,"%d ",*literal++);
		fprintf(filep,"0\n");
	}

	fclose(filep);
}


void free_cnf(struct cnf *cnf)
{
	for(unsigned i=0; i<cnf->cc; i++) free(cnf->clauses[i]);
	free(cnf->clauses);
	free(cnf);
}

void print_cnf_properties(struct cnf *cnf)
{
	printf("\nCNF: Vars=%d, Clauses=%d\n",cnf->vc,cnf->cc);
}
