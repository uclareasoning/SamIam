#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <string.h>
#include <math.h>

#include "types.h"
#include "flags.h"
#include "globals.h"
#include "cnf.h"


main(int argc, char *argv[])
{
	printf("Rsat version %0.2f\n",RSAT_VERSION);

	clock_t start_t, end_t;

	char *cnf_fname = argv[1];

	//read cnf
	struct cnf *cnf = get_cnf_from_file(cnf_fname);
	print_cnf_properties(cnf);

	//construct cnf manager
	struct cnf_manager *cnf_manager = construct_cnf_manager(cnf);
	free_cnf(cnf);

	start_t = clock();

	//ADDED (ADC 7/22/04)
#if(RESTART)
	cnf_manager->start = start_t;
#endif
	//END ADDED

	BOOLEAN result = sat(cnf_manager);
	end_t = clock();

	if (result) printf("\n\nSATISFIABLE.\n");
	else printf("\n\nNOT SATISFIABLE.\n");

	print_cnf_manager_properties(cnf_manager,stdout);
	free_cnf_manager(cnf_manager);

	printf("Time: %0.3fs\n",(double)(end_t-start_t)/CLOCKS_PER_SEC);
	return 0;

}

