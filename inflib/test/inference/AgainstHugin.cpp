#include "hugin"

#include <iostream>
#include <fstream>
#include <math.h>
#include <time.h>

#ifdef __linux__
  //Linux definitions
  #include <unistd.h>
  #define C_FILESEP '/'
#elif defined __sun__
  //Solaris definitions
  #include <unistd.h>
  #define C_FILESEP '/'
#elif defined _WIN32
  //Windows definitions
  #include <getopt.h>
  #define C_FILESEP '\\'
#elif defined __APPLE__
  //MacOS X definitions
  #include <unistd.h>
  #define C_FILESEP '/'
#endif

using namespace HAPI;
using namespace std;

#define PRECISION 22 //41

/** author keith cascio
    since  20060706 */
class AgainstHugin
{
	public:
	AgainstHugin(){
		mySizeEvidence = -1;

		myTimeLoadIO = myTimeTriangulate = myTimeCompile = myTimePropagate = myTimeGetNormalizationConstant = (time_t)-1;

		myFlagQuiet = myFlagTimes = false;
	}

	string AgainstHugin::basename( const string& fileName ){
		string::size_type posSep = fileName.rfind( C_FILESEP );
		if( posSep >= 0 ) return fileName.substr( posSep+1 );
		else return fileName;
	}

	bool AgainstHugin::readHuginNet( const string& fileName )
	{
		DefaultParseListener pl;
		string netFileName = fileName;// + ".net";
		if( myFlagQuiet ) cout << this->basename( netFileName ) << " ";
		else              cout << "opening " <<   netFileName   << "..." << endl;

		int len = fileName.length();

		time_t t0 = time(NULL);

		if( (len > 5) && (fileName.substr(len-5) == ".oobn") ){
			ClassCollection classcollection;
			classcollection.parseClasses( fileName, &pl );
			myDomain = classcollection.getClassByName( fileName.substr(0,len-5) )->createDomain();
		}
		else if( (len > 4) && (fileName.substr(len-4) == ".net") ){
			myDomain = new Domain( netFileName, &pl );
		}
		else{
			cerr << "unrecognized file extension" << endl;
			return false;
		}

		myTimeLoadIO = time(NULL) - t0;

		string logFileName = fileName + ".log";
		if( !myFlagQuiet ) cout << "opening log file " << logFileName << "..." << endl;
		myLogFile = fopen( logFileName.c_str(), "w" );
		myDomain->setLogFile( myLogFile );

		if( !myFlagQuiet ) cout << "triangulating..." << endl;
		time_t t1 = time(NULL);
		myDomain->triangulate( H_TM_FILL_IN_WEIGHT );
		myTimeTriangulate = time(NULL) - t1;

		if( !myFlagQuiet ) cout << "compiling..." << endl;
		time_t t2 = time(NULL);
		myDomain->compile();
		myTimeCompile = time(NULL) - t2;

		return true;
	}

	bool AgainstHugin::setEvidence( const char* fileName ){
		int caseNew = myDomain->newCase();
		myDomain->retractFindings();
		mySizeEvidence = 0;
		if( !fileName || (strlen(fileName) < 5) ) return true;

		if( !myFlagQuiet ) cout << "reading evidence file " << fileName << "..." << endl;
		//myFileEvidence = fopen( fileName.c_str(), "r" );

		string token0 = "<inst id=\"";
		string token1 = "\" value=\"";
		string token2 = "\"/>";

		int len0 = token0.length();
		int len1 = token1.length();

		string line, id, value;
		ifstream infile ( fileName );
		while( getline( infile, line ) )
		{
			int ind0 = line.find( token0 ) + len0;
			int ind1 = line.find( token1 );
			int ind2 = ind1 + len1;
			int ind3 = line.find( token2 );

			if( (ind0 < 0) || (ind1 < 0) || (ind2 < 0) || (ind3 < 0) ) continue;

			++mySizeEvidence;

			id    = line.substr( ind0, ind1 - ind0 );
			value = line.substr( ind2, ind3 - ind2 );

			DiscreteChanceNode* node = (DiscreteChanceNode*) myDomain->getNodeByName( id );

			if( node ){
				int index = this->index( node, value );
				if( index < 0 ){
					cerr << "evidence value \"" << value << "\" not found for node \"" << id << "\"" << endl;
					return false;
				}
				else{
					node->setCaseState( caseNew, index );
					node->selectState( index );
				}
			}
			else{
				cerr << "evidence node not found for id \"" << id << "\"" << endl;
				return false;
			}
		}

		return true;
	}

	int AgainstHugin::index( DiscreteChanceNode* node, const string& label ){
		int size = node->getNumberOfStates();

		/*cout << "index( \"";
		cout << *new string( node->getName() );
		cout << "\" |" << size << "|, \"";
		cout << label << "\" )" << endl;*/

		string* statelabel = NULL;
		for( int i=0; i<size; i++ ){
			statelabel = new string( node->getStateLabel(i) );
			/*cout << "    statelabel[" << i << "] == \"" << *statelabel << "\" " << statelabel << "" << endl;*/
			if( *statelabel == label ) return i;
		}
		cerr << "evidence value \"" << label << "\" not found for node \"" << *new string( node->getName() ) << "\", valid values are:" << endl;
		for( int i=0; i<size; i++ ){
			cerr << "    " << node->getStateLabel(i) << endl;
		}
		return -1;
	}

	void AgainstHugin::propagate( Equilibrium eq=H_EQUILIBRIUM_SUM ){
		if( !myFlagQuiet ) cout << "propagating " << eq << "..." << endl;

		time_t t0 = time(NULL);
		myDomain->propagate( eq, H_MODE_NORMAL );
		myTimePropagate = time(NULL) - t0;
	}

	double AgainstHugin::pre(){
		return this->query( H_EQUILIBRIUM_SUM );
	}

	double AgainstHugin::mpe(){
		return this->query( H_EQUILIBRIUM_MAX );
	}

	double AgainstHugin::query( Equilibrium eq=H_EQUILIBRIUM_SUM ){
		this->propagate( eq );

		time_t t0 = time(NULL);
		double ret = myDomain->getNormalizationConstant();
		double log = myDomain->getLogNormalizationConstant();
		myTimeGetNormalizationConstant = time(NULL) - t0;

		cout.precision( PRECISION );

		char* caption = NULL;
		if(      eq == H_EQUILIBRIUM_SUM ) caption = "e,   ";
		else if( eq == H_EQUILIBRIUM_MAX ) caption = "mpe, ";
		else return -1;

		cout << "hugin p(" << caption << "evidence size " << mySizeEvidence << ") == " << ret << " (" << log << ")" << endl;
		if( (!myFlagQuiet) || myFlagTimes ){
			char* unit = "s";
			int width  = 8;
			cout.width(width);
			cout << "load io:     ";
			cout.width(width);
			cout << myTimeLoadIO                   << unit << endl;
			cout << "triangulate: ";
			cout.width(width);
			cout << myTimeTriangulate              << unit << endl;
			cout << "compile:     ";
			cout.width(width);
			cout << myTimeCompile                  << unit << endl;
			cout << "propagate:   ";
			cout.width(width);
			cout << myTimePropagate                << unit << endl;
			cout << "get value:   ";
			cout.width(width);
			cout << myTimeGetNormalizationConstant << unit << endl;
			cout << "total:       ";
			cout.width(width);
			cout << myTimeLoadIO + myTimeTriangulate + myTimeCompile + myTimePropagate + myTimeGetNormalizationConstant << unit << endl;
		}
		return ret;
	}

	AgainstHugin::~AgainstHugin(){
		if( myLogFile      ) fclose( myLogFile );
		if( myFileEvidence ) fclose( myFileEvidence );
	}

	bool    myFlagQuiet, myFlagTimes;

	private:
	Domain* myDomain;
	FILE*   myLogFile;
	FILE*   myFileEvidence;
	int     mySizeEvidence;

	time_t  myTimeLoadIO, myTimeTriangulate, myTimeCompile, myTimePropagate, myTimeGetNormalizationConstant;
};

int main( int argc, char *argv[] )
{
	string QUERY_PRE  = "pre";
	string QUERY_MPE  = "mpe";
	string QUERY_BOTH = "both";

	bool quiet = false;
	bool times = false;
	char netFileName[0x100];
	char evidenceFileName[0x100];
	char query[0x100];
	netFileName[0]      = '\0';
	evidenceFileName[0] = '\0';
	strcpy( query, QUERY_PRE.c_str() );
	char* optstring     = "n:e:q:st";//-n <network file> -e <inst file> -q <pre|mpe|both> -s -t
	int c;
	while( (c = getopt( argc, argv, optstring )) != -1 ){
		switch(c){
			case 'n':
				strcpy( netFileName,      optarg );
				break;
			case 'e':
				strcpy( evidenceFileName, optarg );
				break;
			case 'q':
				strcpy( query,            optarg );
				break;
			case 's':
				quiet = true;
				break;
			case 't':
				times = true;
				break;
			default:
				cout << "getopt returned character code '" << c << "'" << endl;
		}
	}

	if( !netFileName[0] ){
		cerr << "Usage: " << argv[0] << " -n <network file> [-e <inst file>] [-q <pre|mpe|both>] [-s] [-t]" << endl;
		cerr << "    -s    quiet, i.e. not verbose"       << endl;
		cerr << "    -t    show times even in quiet mode" << endl;
		return -1;
	}

	AgainstHugin* againsthugin;
	bool          successRead      = false;
	try{
		againsthugin = new AgainstHugin();
		againsthugin->myFlagQuiet = quiet;
		againsthugin->myFlagTimes = times;
		successRead = againsthugin->readHuginNet( string(netFileName) );
	}catch( ExceptionMemory& exceptionmemory ){
		cerr << "not enough memory to compile: " << exceptionmemory.what() << endl;
		successRead = false;
	}catch( ExceptionHugin& exceptionhugin ){
		cerr << "possible insufficient hugin lite license: " << exceptionhugin.what() << endl;
		successRead = false;
	}

	if( successRead ){
		bool successEvidence = againsthugin->setEvidence( evidenceFileName );

		if( successEvidence ){
			string squery (query);
			if( (squery == QUERY_PRE) || (squery == QUERY_BOTH) ){
				againsthugin->pre();
			}
			if( (squery == QUERY_MPE) || (squery == QUERY_BOTH) ){
				againsthugin->mpe();
			}
			return 0;
		}
		else return 1;
	}
	else return 1;
}
