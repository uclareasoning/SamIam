/** author keith cascio
    since 20020405 */

#if HAVE_CONFIG_H
#include <config.h>
#endif

//#define DEBUG_USERPROPERTIES
//#define DEBUG_LOAD
//#define DEBUG_IO
//#define DEBUG_CONVERSION
//#define DEBUG_UPDATE
//#define DEBUG_SUBMODELS
//#define DEBUGDONTKEEPSYNCHRONIZED

#ifdef __linux__
	//Linux definitions
	#define EDU_UCLA_ENABLEXDSL
#elif defined __sun__
	//Solaris definitions
	#define EDU_UCLA_ENABLEXDSL
#elif defined _WIN32
	//Windows definitions
	#define EDU_UCLA_ENABLEXDSL
#elif defined __APPLE__
	//MacOS X definitions
	//#undef EDU_UCLA_ENABLEXDSL
	#define EDU_UCLA_ENABLEXDSL
#endif

#define EXCEPTION_OCCURRED	-1
#define SUCCESS			 0
#define FAILURE			 1

#include <stdio.h>
#include <jni.h>
#include "extradefinition.h"
//#include "network.h"
#include "smile.h"
//#ifdef EDU_UCLA_ENABLEXDSL
//#include "enablexdsl.h"
//#endif
#include "edu_ucla_belief_io_dsl_SMILEReader.h"
//#include <xstring>
//#include <string>
#include <iostream>

using namespace std ;

jclass		class_String			= NULL;
jclass		class_Exception			= NULL;
jclass		class_SMILEReader		= NULL;
jmethodID	id_debugLoadNode		= NULL;
jmethodID	id_loadNode			= NULL;
//jmethodID	id_loadPotential		= NULL;
jmethodID	id_loadChildOfRootSubmodels	= NULL;
jmethodID	id_loadSubmodel			= NULL;
jmethodID	id_putNetworkParameterStr	= NULL;
jmethodID	id_putNetworkParameterInt	= NULL;
jobject		objThis				= NULL;
JNIEnv*		theEnvironment			= NULL;
jstring		STR_EMPTY			= NULL;
DSL_network*	theNet				= NULL;
DSL_submodelHandler* theSubmodelHandler		= NULL;
char*		KEY_USERPROPERTIES		= "user_properties";
char		DELIMITER			= ',';
jboolean	FLAG_XDSL_ENABLED		= JNI_FALSE;

struct TwoStringArrays
{
	jobjectArray keys;
	jobjectArray values;
};

int			init();
int			initSupport();
int			ReadDSL( const char*, const int );
int			WriteDSL( const char*, const int );
DSL_stringArray*	MakeNodeNameList( DSL_intArray* );
DSL_stringArray*	MakeNodeIDList( DSL_intArray* );
jdoubleArray		Convert( DSL_doubleArray* );
jintArray		Convert( jint*, jint );
jintArray		Convert( DSL_intArray* );
jobjectArray		Convert( DSL_stringArray* );
DSL_stringArray*	ConvertStringArray( jobjectArray );
DSL_doubleArray*	Convert( jdoubleArray );
DSL_intArray*		Convert( jintArray );
DSL_doubleArray*	GetDoubleRow( jobjectArray, int );
DSL_intArray*		GetIntRow( jobjectArray, int );
jobjectArray		GetStringRow( jobjectArray, int );
jint			ConvertDSLNodeTypeToSMILEReaderExpectedIntConstant( int );
jint			ConvertDiagnosisTypeToSMILEReaderExpectedIntConstant( DSL_extraDefinition::troubleType );
DSL_extraDefinition::troubleType ConvertSMILEReaderIntConstantToDiagnosisType( jint );
jintArray		MakeStrengthsArray( DSL_noisyMAX* noisyornodedef, DSL_intArray* parents );
int			LoadNetworkParameters( DSL_network* );
TwoStringArrays*	Convert( DSL_userProperties* );
void			DeleteEnumPropDefs( DSL_userProperties* );
void			AddUserProperties( DSL_userProperties&, jobjectArray, jobjectArray, jboolean overwrite );

int main()
{
	return (int)0;
}

int LoadNode( const char* cname, const char* cid, DSL_idArray* states, jint* jint_args,
	      DSL_intArray* parents, DSL_doubleArray* probabilities, jintArray strengths, TwoStringArrays* props )
{
	//setup String arguments(s)
	jstring jname = theEnvironment->NewStringUTF( cname );
	jstring jid = theEnvironment->NewStringUTF( cid );

	//setup int[] arguments(s)
	jint size_jint_args = edu_ucla_belief_io_dsl_SMILEReader_ARGUMENT_LIST_SIZE_SANS_TARGETS_LIST + jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_NUM_TARGETS];
	jintArray jargs = Convert( jint_args, size_jint_args );

	//setup double[] arguments(s)
	jdoubleArray jprobabilities = Convert( probabilities );
	//jdoubleArray jweights = Convert( weights );

	//setup String[] arguments(s)
	jobjectArray jstates = Convert( states );
	//DSL_stringArray* parentnames = MakeNodeNameList( parents );
	//jobjectArray jparents = Convert( parentnames );
	//delete parentnames;
	DSL_stringArray* parentIDs = MakeNodeIDList( parents );
	jobjectArray jparents = Convert( parentIDs );
	delete parentIDs;

	theEnvironment->CallVoidMethod( objThis, id_loadNode, jname, jid, jstates, jargs, jparents, jprobabilities, strengths, props->keys, props->values );
	jboolean occurred = theEnvironment->ExceptionCheck();
	if( occurred ){ return EXCEPTION_OCCURRED; }

	//theEnvironment->CallVoidMethod( objThis, id_loadPotential, jid, jparents, jprobabilities );
	//occurred = theEnvironment->ExceptionCheck();
	//if( occurred ){ return EXCEPTION_OCCURRED; }

	/*		This is NOT the right thing to do:	*/
	//cleanup
	//theEnvironment->ReleaseStringUTFChars( jname, cname );

	return SUCCESS;
}

int LoadSubmodels( DSL_intArray* handles, DSL_stringArray* names, DSL_intArray* xcoords, DSL_intArray* ycoords )
{
    //setup int[] arguments(s)
    jintArray jhandles = Convert( handles );
    jintArray jxcoordinates = Convert( xcoords );
    jintArray jycoordinates = Convert( ycoords );

    //setup String[] arguments(s)
    jobjectArray jnames = Convert( names );

    theEnvironment->CallVoidMethod( objThis, id_loadChildOfRootSubmodels, jhandles, jnames, jxcoordinates, jycoordinates );
    jboolean occurred = theEnvironment->ExceptionCheck();
    if( occurred ){ return EXCEPTION_OCCURRED; }

    return SUCCESS;
}

int LoadSubmodel( jint jhandle, const char* cname, jint jxcoord, jint jycoord, DSL_intArray* children )
{
    //setup String arguments(s)
    jstring jname = theEnvironment->NewStringUTF( cname );

    //setup int[] arguments(s)
    jintArray jchildren = Convert( children );

    theEnvironment->CallVoidMethod( objThis, id_loadSubmodel, jhandle, jname, jxcoord, jycoord, jchildren );
    jboolean occurred = theEnvironment->ExceptionCheck();
    if( occurred ){ return EXCEPTION_OCCURRED; }

    return SUCCESS;
}

/**
	Keith Cascio
	050202

	A preorder recursive tree-traversal.
*/
int LoadSubmodel( const int handle )
{
	DSL_submodel* submodel = theSubmodelHandler->GetSubmodel( handle );
	if( submodel )
	{
		const char* tempSubmodelName = submodel->header.GetName();
#ifdef DEBUG_SUBMODELS
		cout << "C++ loading submodel: { " << handle << ", " << tempSubmodelName << " }" << endl;
		if( theSubmodelHandler->IsMainSubmodel( handle ) ) cout << "\tIsMainSubmodel() == true" << endl;
#endif
		jint tempSubmodelXcoord = submodel->info.position.center_X;
		jint tempSubmodelYcoord = submodel->info.position.center_Y;
		//DSL_intArray* children = new DSL_intArray();
		DSL_intArray children;//on the stack
		//theSubmodelHandler->GetChildSubmodels(handle, children, dsl_normalArc);
		theSubmodelHandler->GetIncludedSubmodels( handle, children );
		int success = LoadSubmodel( handle, tempSubmodelName, tempSubmodelXcoord, tempSubmodelYcoord, &children );
		if( success == EXCEPTION_OCCURRED ) return EXCEPTION_OCCURRED;
		else if( success == SUCCESS )
		{
			jint numchildren = children.NumItems();
			for( int i = (int)0; i < numchildren; i++ )
			{
				success = LoadSubmodel( children[i] );
				if( success == EXCEPTION_OCCURRED ) return EXCEPTION_OCCURRED;
			}
			return SUCCESS;
		}
	}
	else cerr << "C++ warning: bad submodel handle." << endl;



	return FAILURE;
}

/**
   Obtain references to class SMILEReader methods
   and other Java objects
   from the JNI environment.
*/
int init()
{
    class_String		= theEnvironment->FindClass( "java/lang/String" );
    class_Exception		= theEnvironment->FindClass( "java/lang/Exception" );
    class_SMILEReader		= theEnvironment->GetObjectClass( objThis );

    id_loadNode			= theEnvironment->GetMethodID( class_SMILEReader, "loadNode", "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;[I[Ljava/lang/String;[D[I[Ljava/lang/String;[Ljava/lang/String;)V" );
    //id_loadPotential		= theEnvironment->GetMethodID( class_SMILEReader, "loadPotential", "(Ljava/lang/String;[Ljava/lang/String;[D)V" );
    id_loadChildOfRootSubmodels	= theEnvironment->GetMethodID( class_SMILEReader, "loadChildOfRootSubmodels", "([I[Ljava/lang/String;[I[I)V" );
    id_loadSubmodel		= theEnvironment->GetMethodID( class_SMILEReader, "loadSubmodel", "(ILjava/lang/String;II[I)V" );
    id_debugLoadNode		= theEnvironment->GetMethodID( class_SMILEReader, "debugLoadNode", "(Ljava/lang/String;)V" );
    id_putNetworkParameterStr	= theEnvironment->GetMethodID( class_SMILEReader, "putNetworkParameter", "(Ljava/lang/Object;Ljava/lang/Object;)V" );
    id_putNetworkParameterInt	= theEnvironment->GetMethodID( class_SMILEReader, "putNetworkParameter", "(Ljava/lang/Object;I)V" );

    STR_EMPTY			= theEnvironment->NewStringUTF("");

    initSupport();

    if( id_loadNode == NULL ||
	//id_loadPotential == NULL ||
	id_loadChildOfRootSubmodels == NULL ||
	id_loadSubmodel == NULL ||
	id_debugLoadNode == NULL ||
	id_putNetworkParameterStr == NULL ||
	id_putNetworkParameterInt == NULL ||
	class_String == NULL || class_SMILEReader == NULL ||
	STR_EMPTY == NULL )
    {
	return FAILURE;
    }
    else return SUCCESS;
}

/** @since 020304 */
int initSupport()
{
#ifdef EDU_UCLA_ENABLEXDSL
	if( !FLAG_XDSL_ENABLED ){
		FLAG_XDSL_ENABLED = JNI_TRUE;
	  //EnableXdslFormat();//073003
	}
#endif
	return SUCCESS;
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
JNI_OnLoad( JavaVM *vm, void *reserved ){
	return JNI_VERSION_1_4;
}

/*
 * Class:     edu_ucla_belief_io_dsl_SMILEReader
 * Method:    testJNI
 * Signature: ()Z
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_io_dsl_SMILEReader_testJNI(JNIEnv *, jobject)
{
	int size = theNet->GetNumberOfNodes();
	cout << "theNet->GetNumberOfNodes() == " << size << endl;
	return SUCCESS;
}

/*
 * Class:     edu_ucla_belief_io_dsl_SMILEReader
 * Method:    librarySupports
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_edu_ucla_belief_io_dsl_SMILEReader_librarySupports( JNIEnv* env, jclass obj, jint jgenieType )
{
	int success = initSupport();

	jboolean ret = JNI_FALSE;
	if( jgenieType == DSL_XDSL_FORMAT ) ret = FLAG_XDSL_ENABLED;
	else ret = JNI_TRUE;

	//cout << "SMILEReader_librarySupports( " << jgenieType << " ) = " << ret << endl;

	return ret;
}

/*
 * Class:     edu_ucla_belief_io_dsl_SMILEReader
 * Method:    loadDSL
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT jint JNICALL Java_edu_ucla_belief_io_dsl_SMILEReader_loadDSL( JNIEnv* env, jobject obj, jstring jfname, jint jgenieType )
{
    //setup
    theEnvironment = env;
    objThis = obj;

    int success = init();
    if( success != SUCCESS )
    {
	env->ThrowNew( class_Exception,
		       "C++ failed to find expected interface to class SMILEReader." );
	return EXCEPTION_OCCURRED;
    }

    success = ReadDSL( env->GetStringUTFChars( jfname, NULL ), (int)jgenieType );
    if( success != SUCCESS ) return EXCEPTION_OCCURRED;

    success = LoadNetworkParameters( theNet );
    if( success == EXCEPTION_OCCURRED ) return EXCEPTION_OCCURRED;

    theSubmodelHandler = &(theNet->GetSubmodelHandler());
    success = LoadSubmodel( DSL_MAIN_SUBMODEL );//will traverse tree recursively from root
    if( success == EXCEPTION_OCCURRED ) return EXCEPTION_OCCURRED;

    int size = theNet->GetNumberOfNodes();
    DSL_intArray* nodes = new DSL_intArray( size );
    theNet->GetAllNodes( *nodes );

    //lots of locals
    DSL_node* ptrTempNode = NULL;
    DSL_idArray* ptrStates = NULL;
    const char* strTempName = NULL;
    const char* strTempId = NULL;
    DSL_nodeInfo* info = NULL;
    DSL_header* header = NULL;
    DSL_screenInfo* screeninfo = NULL;
    DSL_rectangle* rect = NULL;
    DSL_nodeDefinition* nodedef = NULL;
    //DSL_noisyOR* noisyornodedef = NULL;
    DSL_noisyMAX* noisyornodedef = NULL;
    DSL_extraDefinition* extra = NULL;
    DSL_userProperties* userproperties = NULL;
    jint xpos, ypos;
    jint width, height;
    jint DSLNodeType, DiagnosisType;
    jint mandatory, ranked;
    jint defaultindex;
    jint submodelhandle;
    jint numtargets;
    //jint* targetlist = NULL;
    DSL_intArray* targetlist = NULL;

    jint* jint_args = NULL;
    //jint args_size = (jint)0;
    /*
      jint_args is of the form:

      { xpos, ypos, width, height, DSLNodeType, DiagnosisType, mandatory, ranked, defaultindex, numtargets, ... targetlist ... }

      Therefore it is of the size:

      edu_ucla_belief_io_dsl_SMILEReader_ARGUMENT_LIST_SIZE_SANS_TARGETS_LIST + numtargets
    */
    DSL_intArray* parents = NULL;
    DSL_doubleArray* probabilities = NULL;
    DSL_Dmatrix* weightsmatrix = NULL;
    DSL_doubleArray* weightsarray = NULL;
    jintArray strengths = NULL;
#ifdef DEBUG_LOAD
    DSL_stringArray* listParents = NULL;
#endif

    for( int i=0; i < size ; i++ )
    {
	ptrTempNode	= theNet->GetNode( (*nodes)[i] );
	info		= &(ptrTempNode->Info());
	header		= &(info->Header());
	screeninfo	= &(info->Screen());
	rect		= &(screeninfo->position);
	nodedef		= ptrTempNode->Definition();
	extra		= ptrTempNode->ExtraDefinition();
	userproperties	= &(info->UserProperties());
	strTempName	= header->GetName();
	strTempId	= header->GetId();
	xpos		= rect->center_X;
	ypos		= rect->center_Y;
	width		= rect->width;
	height		= rect->height;
	DSLNodeType	= ConvertDSLNodeTypeToSMILEReaderExpectedIntConstant( nodedef->GetType() );
	if( DSLNodeType == edu_ucla_belief_io_dsl_SMILEReader_UNDEFINED_NODE_TYPE )
	{
		return EXCEPTION_OCCURRED;
	}
	DiagnosisType	= ConvertDiagnosisTypeToSMILEReaderExpectedIntConstant( extra->GetType() );
	mandatory	= extra->IsMandatory();
	ranked		= extra->IsRanked();
	targetlist	= &(extra->GetFaultStates());
	numtargets	= 0;
	for( int j=0; j<targetlist->NumItems(); j++ ){
	    if( (*targetlist)[j] == (int)1 ) numtargets++;
	}
	defaultindex	= ( extra->IsSetToDefault() ) ? extra->GetDefaultOutcome() : (jint)-1;
	submodelhandle	= ptrTempNode->GetSubmodel();
	ptrStates	= nodedef->GetOutcomesNames();

	jint_args = new jint[ edu_ucla_belief_io_dsl_SMILEReader_ARGUMENT_LIST_SIZE_SANS_TARGETS_LIST + numtargets ];
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_X_POSITION]		= xpos;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_Y_POSITION]		= ypos;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_WIDTH]			= width;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_HEIGHT]			= height;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_DSLNodeType]		= DSLNodeType;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_DiagnosisType]		= DiagnosisType;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_MANDATORY]			= mandatory;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_RANKED]			= ranked;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_DEFAULT_STATE_INDEX]	= defaultindex;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_SUBMODEL_HANDLE]		= submodelhandle;
	jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_NUM_TARGETS]		= numtargets;
	//for( int offset=0; offset<numtargets; offset++ ) jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_BEGIN_TARGETS_LIST + offset] = targetlist[offset];
	int counter = (int)0;
	for( int j=0; j<targetlist->NumItems(); j++ ){
	    if( (*targetlist)[j] == (int)1 ) jint_args[edu_ucla_belief_io_dsl_SMILEReader_INDEX_BEGIN_TARGETS_LIST + counter++] = j;
	}

	parents = &(theNet->GetParents( (*nodes)[i] ));
	/*	Keith Cascio 041502			*/
	/*	This is NOT the right thing to do:	*/
	//parents->UseAsList();

	if( nodedef->GetType() == DSL_NOISY_MAX )
	{
		//noisyornodedef = dynamic_cast<DSL_noisyOR*>(nodedef);
		//Keith Cascio
		//041502
		//Visual C++ doesn't like the dynamic_cast.
		noisyornodedef = static_cast<DSL_noisyMAX*>(nodedef);
		noisyornodedef->SetDontKeepSynchronized();
		//weights = &(((DSL_noisyOR*)(nodedef))->GetNoisyORWeights());
		//weightsmatrix = &( noisyornodedef->GetNoisyORWeights() );
		//weightsarray = &( weightsmatrix->GetItems() );
		//weightsarray = new DSL_doubleArray( noisyornodedef->GetSize() );
		//weightsarray = new DSL_doubleArray();
		//success = noisyornodedef->GetLegacyNoisyOrProbabilities( *weightsarray );
		weightsmatrix = &( noisyornodedef->GetCiWeights() );
		weightsarray = &( weightsmatrix->GetItems() );
		weightsarray->UseAsList();
		probabilities = weightsarray;
		strengths = MakeStrengthsArray( noisyornodedef, parents );
	}
	else
	{
		nodedef->GetDefinition( &probabilities );
		probabilities->UseAsList();
		noisyornodedef = NULL;
		weightsmatrix = NULL;
		weightsarray = NULL;
		strengths = NULL;
	}


#ifdef DEBUG_LOAD
	listParents = MakeNodeNameList( parents );
	cout << "C++ encountered node: " << strTempName << endl;
	cout << "\tnum parents: " << parents->NumItems() << endl;
	cout << "\tparents { ";
	for( int i=0; i<parents->NumItems();i++) cout << (*listParents)[i] << " ";
	cout << "}" << endl;
	cout << "\tsize probabilities: " << nodedef->GetSize() << "==" << probabilities->NumItems() << endl;
	cout << "\tprobabilities [ ";
	for( int i=0; i<probabilities->NumItems();i++) cout << (*probabilities)[i] << " ";
	cout << "]" << endl;
	if( strengths )
	{
		jint* tempArray = theEnvironment->GetIntArrayElements(strengths,NULL);
		int lenStrengths = theEnvironment->GetArrayLength(strengths);
		cout << "\tstrengths [ ";
		for( int i=0; i<lenStrengths;i++ ) cout << tempArray[i] << " ";
		cout << "]" << endl;
		theEnvironment->ReleaseIntArrayElements(strengths,tempArray,0);
	}
#endif
	success = LoadNode( strTempName, strTempId, ptrStates, jint_args, parents, probabilities, strengths, Convert(userproperties) );
	//040902 - don't want to do this!
	//delete strTempName;
	if( success == EXCEPTION_OCCURRED ) return EXCEPTION_OCCURRED;
	delete jint_args;

#ifdef DEBUGDONTKEEPSYNCHRONIZED
	if( noisyornodedef != NULL ) cout << "C++ " << strTempName << ".IsExpanded() == " << noisyornodedef->IsExpanded() << endl;
#endif
    }

    //clean up
    delete nodes;
    //delete theNet;

#ifdef DEBUG_SUBMODELS
    cout << "C++ loadDSL() returning DSL_MAIN_SUBMODEL = " << DSL_MAIN_SUBMODEL << endl;//debug
#endif

    return DSL_MAIN_SUBMODEL;
}

/*
 * Class:     edu_ucla_belief_io_dsl_SMILEReader
 * Method:    updateDSL
 * Signature: (Ljava/lang/String;ILjava/lang/String;I[Ljava/lang/String;[[D[Ljava/lang/String;[Ljava/lang/String;[[Ljava/lang/String;[[Ljava/lang/String;[[I)Z
 */
JNIEXPORT jboolean JNICALL Java_edu_ucla_belief_io_dsl_SMILEReader_updateDSL(JNIEnv* env,
									       jobject obj,
									       jstring joriginalfname,
									       jint jgenieTypeOriginal,
									       jstring jofname,
									       jint jgenieType,
									       jobjectArray jids,
									       jobjectArray jprobs,
									       jobjectArray arrayUserKeys,
									       jobjectArray arrayUserValues,
									       jobjectArray arraysVariableKeys,
									       jobjectArray arraysVariableValues,
									       jobjectArray arraysIntArgs )
{
    //do a simple check
    jint numnodes = env->GetArrayLength( jids );
    if( env->GetArrayLength( jprobs ) != numnodes )
    {
	env->ThrowNew( class_Exception,
		       "C++ array of node IDs and array of probabilities are of unequal length." );
	return JNI_FALSE;
    }

    //setup
    theEnvironment = env;
    objThis = obj;

    int success = init();
    if( success != SUCCESS )
    {
	env->ThrowNew( class_Exception,
		       "C++ failed to find expected interface to class SMILEReader." );
	return JNI_FALSE;
    }

    const char* coriginalfname = env->GetStringUTFChars( joriginalfname, NULL );
    const char* cofname = env->GetStringUTFChars( jofname, NULL );
    success = ReadDSL( coriginalfname, (int)jgenieTypeOriginal );
    if( success != SUCCESS ) return JNI_FALSE;

#ifdef DEBUG_USERPROPERTIES
	cout << "&(theNet->UserProperties()) = " << &(theNet->UserProperties()) << endl;
#endif

	DSL_userProperties& userproperties = theNet->UserProperties();
	DeleteEnumPropDefs( &userproperties );

#ifdef DEBUG_USERPROPERTIES
	cout << "theNet->UserProperties().GetNumberOfProperties(): " << userproperties.GetNumberOfProperties() << endl;
#endif

	AddUserProperties( userproperties, arrayUserKeys, arrayUserValues, JNI_FALSE );

#ifdef DEBUG_USERPROPERTIES
	cout << "theNet->UserProperties().GetNumberOfProperties(): " << userproperties.GetNumberOfProperties() << endl;
#endif

    DSL_stringArray* cids = ConvertStringArray( jids );
#ifdef DEBUG_UPDATE
    cout << "C++ cids[0] = '" << (*cids)[0] << "'" << endl;
    DSL_doubleArray* firstrow = GetDoubleRow( jprobs, 0 );
    cout << "C++ cprobs[0][0] = " << (*firstrow)[0] << endl;
#endif

    //locals
    int node_handle = (int)0;
    DSL_node* ptrTempNode = NULL;
    DSL_doubleArray* ptrTempProbs = NULL;
    jobjectArray ptrTempKeys = NULL;
    jobjectArray ptrTempValues = NULL;
    DSL_intArray* ptrTempIntArgs = NULL;
    DSL_nodeDefinition* nodedef = NULL;
    DSL_nodeInfo* info = NULL;
    DSL_userProperties* properties = NULL;
    DSL_extraDefinition* extra = NULL;
    DSL_extraDefinition::troubleType diagnosisType = DSL_extraDefinition::auxiliary;
    //DSL_noisyOR* noisyornodedef = NULL;
    DSL_noisyMAX* noisyornodedef = NULL;
    char* error_message = NULL;
    const int max_length_error_msg = (int)255;

    for( jint i=0; i<numnodes; i++ )
    {
	node_handle = theNet->FindNode( (*cids)[i] );
#ifdef DEBUG_UPDATE
	cout << "C++ attempting update of node '" << (*cids)[i] << "' node_handle = " << node_handle << endl;
#endif
	if( node_handle == DSL_OUT_OF_RANGE )
	{
#ifdef DEBUG_UPDATE
	    cout << "C++ node '" << (*cids)[i] << "' not in network." << endl;
#endif
	    error_message = new char[max_length_error_msg];
	    sprintf( error_message, "C++ error: Attempt to update node '%s' that does not exist in network '%s'",(*cids)[i],coriginalfname );
	    env->ThrowNew( class_Exception, error_message );
	    delete error_message;
	    return JNI_FALSE;
	}


	ptrTempNode	= theNet->GetNode( node_handle );
	ptrTempProbs	= GetDoubleRow( jprobs, i );
	ptrTempKeys	= GetStringRow( arraysVariableKeys, i );
	ptrTempValues	= GetStringRow( arraysVariableValues, i );
	ptrTempIntArgs	= GetIntRow( arraysIntArgs, i );
	nodedef		= ptrTempNode->Definition();
	info		= &(ptrTempNode->Info());
	properties	= &(info->UserProperties());
	extra		= ptrTempNode->ExtraDefinition();
	diagnosisType	= ConvertSMILEReaderIntConstantToDiagnosisType( (*ptrTempIntArgs)[edu_ucla_belief_io_dsl_SMILEReader_INDEX_UPDATE_DiagnosisType] );

	if( nodedef->GetType() == DSL_NOISY_MAX )
	{
	    //success = dynamic_cast<DSL_noisyOR*>(nodedef)->SetProbabilities( *ptrTempProbs );
	    //dynamic_cast<DSL_noisyOR*>(nodedef)->NoisyORToCpt();
		//Keith Cascio
		//041502
		//Visual C++ doesn't like the dynamic_cast.
		//noisyornodedef = static_cast<DSL_noisyOR*>(nodedef);
		noisyornodedef = static_cast<DSL_noisyMAX*>(nodedef);
	    //success = noisyornodedef->SetLegacyNoisyOrProbabilities( *ptrTempProbs );
		success = noisyornodedef->SetDefinition( *ptrTempProbs );
	    //noisyornodedef->NoisyORToCpt();
	}
	else success = nodedef->SetDefinition( *ptrTempProbs );
#ifdef DEBUG_UPDATE
	cout << "C++ setting definition for node '" << (*cids)[i] << "' to: { ";
	for( int i=0; i<ptrTempProbs->NumItems(); i++ ) cout << (*ptrTempProbs)[i] << ", ";
	cout << "}" << endl;
	cout << "C++ SetDefinition() returned " << success << endl;
#endif

	AddUserProperties( *properties, ptrTempKeys, ptrTempValues, JNI_TRUE );
	extra->SetType( diagnosisType );

	delete ptrTempProbs;
	delete ptrTempIntArgs;
    }

#ifdef DEBUG_USERPROPERTIES
	cout << "&(theNet->UserProperties()) = " << &(theNet->UserProperties()) << endl;
#endif

    success = WriteDSL( cofname, (int)jgenieType );
    if( success == SUCCESS ) return JNI_TRUE;
    else return JNI_FALSE;
}

#ifdef __cplusplus
}
#endif

void AddUserProperties( DSL_userProperties& props, jobjectArray arrayUserKeys, jobjectArray arrayUserValues, jboolean overwrite )
{
#ifdef DEBUG_USERPROPERTIES
	cout << "AddUserProperties( " << overwrite << " )" << endl;
#endif
	if( !arrayUserKeys || !arrayUserValues ) return;

	jint length = theEnvironment->GetArrayLength( arrayUserKeys );
	if( length < 1 || length != theEnvironment->GetArrayLength( arrayUserValues ) ) return;

	jstring tempjstrKey = NULL;
	char* tempstrKey = NULL;
	jstring tempjstrValue = NULL;
	char* tempstrValue = NULL;
	int tempIndex = -1;
	int result = -99;
	for( int i=0; i < length; i++ )
	{
		tempjstrKey = static_cast<jstring>(theEnvironment->GetObjectArrayElement( arrayUserKeys, i ));
		tempstrKey = const_cast<char*>(theEnvironment->GetStringUTFChars( tempjstrKey, NULL ));
		tempjstrValue = static_cast<jstring>(theEnvironment->GetObjectArrayElement( arrayUserValues, i ));
		tempstrValue = const_cast<char*>(theEnvironment->GetStringUTFChars( tempjstrValue, NULL ));

		if( overwrite )
		{
			tempIndex = props.FindProperty( tempstrKey );
			if( tempIndex >= 0 ) props.DeleteProperty( tempIndex );
		}

		result = props.AddProperty( tempstrKey, tempstrValue );
#ifdef DEBUG_USERPROPERTIES
		cout << "props.AddProperty( \"" << tempstrKey << "\", \"" << tempstrValue << "\" ) == " << result << endl;
#endif
		//props.InsertProperty( 0, tempstrKey, tempstrValue );
		//cout << "props.InsertProperty( 0, \"" << tempstrKey << "\", \"" << tempstrValue << "\" )" << endl;

		//from SMILE errors.h
		//#define DSL_OKAY                    0
		//#define DSL_GENERAL_ERROR          -1
		//#define DSL_OUT_OF_RANGE           -2
		//#define DSL_NO_ITEM                -3
		//#define DSL_INVALID_VALUE          -4
		//#define DSL_NO_USEFUL_SAMPLES      -5
	}

	//props.CheckConsistency();
	//props.CleanUp(1);
}

void DeleteEnumPropDefs( DSL_userProperties* props )
{
#ifdef DEBUG_USERPROPERTIES
	cout << "DeleteEnumPropDefs()" << endl;
	cout << "props->GetNumberOfProperties(): " << props->GetNumberOfProperties() << endl;
#endif

	int indexKey = props->FindProperty( KEY_USERPROPERTIES );
	if( indexKey < 0 ) return;

	char* strPropNamesOrig = props->GetPropertyValue( indexKey );
	if( strPropNamesOrig )
	{
		char* strPropNames = new char[ strlen(strPropNamesOrig)+1 ];
		strcpy( strPropNames, strPropNamesOrig );
#ifdef DEBUG_USERPROPERTIES
		cout << "strPropNames = \"" << strPropNames << "\"" << endl;
#endif
		char** arrayPropNames = new char*[99];
		int size = 1;
		int i = 0;
		char* pointer = strPropNames;
		arrayPropNames[i++] = pointer;
		while(true)
		{
			while( *pointer != DELIMITER && *pointer != (char)0 ) ++pointer;
			if( *pointer == (char)0 ) break;
			else
			{
				while( *pointer == DELIMITER ) *(pointer++) = (char)0;
				if( *pointer == (char)0 ) break;
				else
				{
					arrayPropNames[i++] = pointer;
					++size;
				}
			}
		}
		int tempIndexKey = (int)-1;
		for( i=0; i<size; i++ )
		{
#ifdef DEBUG_USERPROPERTIES
			cout << i << ": \"" << arrayPropNames[i] << "\"" << endl;
#endif
			tempIndexKey = props->FindProperty( arrayPropNames[i] );
			if( tempIndexKey >= 0 ) props->DeleteProperty( tempIndexKey );
			//delete arrayPropNames[i];
		}
		delete arrayPropNames;
		delete strPropNames;
	}
	props->DeleteProperty( indexKey );
	//props->DeleteAllProperties();
	props->CleanUp(1);
	props->CheckConsistency();
#ifdef DEBUG_USERPROPERTIES
	cout << "props->GetNumberOfProperties(): " << props->GetNumberOfProperties() << endl;
#endif
}

TwoStringArrays* Convert( DSL_userProperties* props )
{
	TwoStringArrays* ret = new TwoStringArrays();
	int numitems = props->GetNumberOfProperties();

	ret->keys = theEnvironment->NewObjectArray( numitems, class_String, STR_EMPTY );
	ret->values = theEnvironment->NewObjectArray( numitems, class_String, STR_EMPTY );

	for( int i=0; i<numitems; i++ ){
		theEnvironment->SetObjectArrayElement( ret->keys, i,
			theEnvironment->NewStringUTF(  props->GetPropertyName(i) ) );
		theEnvironment->SetObjectArrayElement( ret->values, i,
			theEnvironment->NewStringUTF(  props->GetPropertyValue(i) ) );
	}

	return ret;
}

DSL_doubleArray* GetDoubleRow( jobjectArray twod, int rowindex )
{
    //a simple check
    if( rowindex < 0 || theEnvironment->GetArrayLength( twod ) < rowindex ) return NULL;

    jdoubleArray row = static_cast<jdoubleArray>( theEnvironment->GetObjectArrayElement( twod, rowindex ) );
    return Convert( row );
}

DSL_intArray* GetIntRow( jobjectArray twod, int rowindex )
{
    //a simple check
    if( rowindex < 0 || theEnvironment->GetArrayLength( twod ) < rowindex ) return NULL;

    jintArray row = static_cast<jintArray>( theEnvironment->GetObjectArrayElement( twod, rowindex ) );
    return Convert( row );
}

jobjectArray GetStringRow( jobjectArray twod, int rowindex )
{
    //a simple check
    if( rowindex < 0 || theEnvironment->GetArrayLength( twod ) < rowindex ) return NULL;

    return static_cast<jobjectArray>( theEnvironment->GetObjectArrayElement( twod, rowindex ) );
}

DSL_doubleArray* Convert( jdoubleArray array )
{
    jint length = theEnvironment->GetArrayLength( array );
    DSL_doubleArray* ret = new DSL_doubleArray( length );

    jdouble* jdouble_array = theEnvironment->GetDoubleArrayElements( array, NULL );
    for( int i=0; i<length; i++ )
    {
	ret->Insert( i, jdouble_array[i] );
    }
    theEnvironment->ReleaseDoubleArrayElements( array, jdouble_array, JNI_ABORT );//JNI_ABORT means: free the buffer 'jdouble_array' without any attempt to update the elements of 'array'
    ret->UseAsList();
    return ret;
}

DSL_intArray* Convert( jintArray array )
{
    jint length = theEnvironment->GetArrayLength( array );
    DSL_intArray* ret = new DSL_intArray( length );

    jint* jint_array = theEnvironment->GetIntArrayElements( array, NULL );
    for( int i=0; i<length; i++ )
    {
	ret->Insert( i, jint_array[i] );
    }
    theEnvironment->ReleaseIntArrayElements( array, jint_array, JNI_ABORT );//JNI_ABORT means: free the buffer 'jint_array' without any attempt to update the elements of 'array'
    ret->UseAsList();
    return ret;
}

DSL_stringArray* ConvertStringArray( jobjectArray array )
{
    jint length = theEnvironment->GetArrayLength( array );
    DSL_stringArray* ret = new DSL_stringArray( length );

    jstring tempjstr = NULL;
    char* tempstr = NULL;
    for( int i=0; i < length; i++ )
    {
	tempjstr = static_cast<jstring>(theEnvironment->GetObjectArrayElement( array, i ));
	tempstr = const_cast<char*>(theEnvironment->GetStringUTFChars( tempjstr, NULL ));
	ret->SetString( i, tempstr );
    }
    ret->UseAsList();
    return ret;
}

int WriteDSL( const char* cfname, const int fileType )
{
    if( !theNet ) return FAILURE;

    //char* fname = new char[ strlen( cfname ) ];
    //strcpy( fname,  cfname );

    //int result = theNet->WriteFile( fname, DSL_DSL_FORMAT );
    //int result = theNet->WriteFile( const_cast<char*>(cfname), DSL_DSL_FORMAT );
    int result = theNet->WriteFile( const_cast<char*>(cfname), fileType );

    if( result == DSL_OKAY )
    {
#ifdef DEBUG_IO
	cout << "C++ success writing file " << cfname << endl;
#endif
	return SUCCESS;
    }
    else
    {
#ifdef DEBUG_IO
	cout << "C++ WriteFile() result: " << result << endl;
#endif
	theEnvironment->ThrowNew( class_Exception, "C++ error: Failed to write file." );
	return EXCEPTION_OCCURRED;
    }

    //return FAILURE;
}

int ReadDSL( const char* cfname, const int id_format )
{
    //char* fname = new char[ strlen( cfname ) ];
    //strcpy( fname,  cfname );

    int format_id_effective = id_format;
    if( format_id_effective < (int)0 ) format_id_effective = (int)0;

    theNet = new DSL_network();
#ifdef DEBUG_IO
    cout << "C++ (int)theNet: " << (int)theNet << endl;
#endif
    //int result = theNet->ReadFile( const_cast<char*>(cfname), DSL_DSL_FORMAT );
    //int result = theNet->ReadFile( const_cast<char*>(cfname),(int)0,static_cast<std::FILE*>(NULL) );
    int result = theNet->ReadFile( const_cast<char*>(cfname), format_id_effective );

    /*debug*/
    /*
    int size = theNet->GetNumberOfNodes();
    DSL_intArray* nodes = new DSL_intArray( size );
    theNet->GetAllNodes( *nodes );
    DSL_node* ptrTempNode = NULL;
    DSL_header* header = NULL;
    DSL_nodeInfo* info = NULL;
    char* strTempName = NULL;

    cout << "C++ read network " << cfname << ", nodes: " << endl;
    for( int i=0; i < size ; i++ )
    {
	ptrTempNode	= theNet->GetNode( (*nodes)[i] );
	info		= &(ptrTempNode->Info());
	header		= &(info->Header());
	strTempName	= header->GetName();
	cout << "	node: " << strTempName << endl;
    }
    */
    /*debug*/

    if( result == DSL_SYNTAX_ERROR )
    {
#ifdef DEBUG_IO
	cout << "C++ DSL_SYNTAX_ERROR" << endl;
#endif
	char*       prefix               = "DSL_SYNTAX_ERROR; ";
	const char* syntax_error_message = theNet->ErrorHandler().GetLastErrorMessage();
	char*       exception_message    = new char[ strlen(prefix) + strlen(syntax_error_message) + 1 ];
	strcpy( exception_message, prefix );
	strcat( exception_message, syntax_error_message );
	theEnvironment->ThrowNew( class_Exception, exception_message );
	return EXCEPTION_OCCURRED;
    }
    else if( result == DSL_OKAY )
    {
#ifdef DEBUG_IO
	cout << "C++ success reading file " << cfname << endl;
#endif
	return SUCCESS;
    }
#ifdef DEBUG_IO
    else if( result == DSL_WRONG_FILE ) cout << "C++ file not found." << endl;
    else cout << "C++ ReadFile() result: " << result << endl;
#endif
    return FAILURE;
}

jobjectArray Convert( DSL_stringArray* array )
{
	if( !array ) return theEnvironment->NewObjectArray( 0, class_String, STR_EMPTY );

    int numitems = array->NumItems();
    jobjectArray ret = theEnvironment->NewObjectArray( numitems,
						       class_String,
						       STR_EMPTY );
    for( int i=0; i<numitems; i++ ){
	theEnvironment->SetObjectArrayElement( ret, i,
					       theEnvironment->NewStringUTF( (*array)[i] ) );
    }
    return ret;
}

jdoubleArray Convert( DSL_doubleArray* array )
{
    if( array )
    {
	jint size_array = array->NumItems();
	jdouble* jdouble_array = new jdouble[size_array];
	for( int i=0; i<size_array; i++ ) jdouble_array[i] = (*array)[i];
	jdoubleArray ret = theEnvironment->NewDoubleArray( size_array );
	theEnvironment->SetDoubleArrayRegion( ret, (jint)0, size_array, jdouble_array );
	delete jdouble_array;

#ifdef DEBUG_CONVERSION
	jdouble* tempArray = theEnvironment->GetDoubleArrayElements(ret,NULL);
	cout << "\tConvert( DSL_doubleArray ) [ ";
	for( int i=0; i<size_array;i++ ) cout << tempArray[i] << " ";
	cout << "]" << endl;
	theEnvironment->ReleaseDoubleArrayElements(ret,tempArray,0);
#endif

	return ret;
    }
    else return theEnvironment->NewDoubleArray( 0 );
}

jintArray Convert( jint* array, jint size )
{
    jintArray ret = theEnvironment->NewIntArray( size );
    if( ret == NULL ) cerr << "We're in trouble." << endl;
    theEnvironment->SetIntArrayRegion( ret, (jint)0, size, array );
    return ret;
}

jintArray Convert( DSL_intArray* array )
{
	int size = array->NumItems();
	jint* jarray = new jint[size];
	for( jint i=0; i<size; i++ ) jarray[i] = (*array)[i];
	jintArray ret = theEnvironment->NewIntArray( size );
	theEnvironment->SetIntArrayRegion( ret, (jint)0, size, jarray );
	delete jarray;
	return ret;
}

DSL_stringArray* MakeNodeNameList( DSL_intArray* parents )
{
	int numitems = parents->NumItems();

	//int size = parents->GetSize();
	//cout << "C++ MakeNodeNameList() parents numitems: " << numitems << ", size: " << size << endl;
	//if( numitems != size ) return NULL;
	//char** templist = new char*[numitems];
	//int numparents = (int)0;

	DSL_stringArray* ret = new DSL_stringArray( numitems );

	DSL_node* ptrTempNode = NULL;
	for( int i=0; i<numitems; i++ )
	{
		ptrTempNode = theNet->GetNode( (*parents)[i] );
		if( ptrTempNode ){
			ret->SetString( i, ptrTempNode->Info().Header().GetName() );
		}
		else{
			cerr << "C++ error: MakeNodeNameList() called on bad DSL_intArray, either contains invalid node handles or not initialized to the right length." << endl;
			break;
		}
	}

	ret->UseAsList();
	return ret;
}

DSL_stringArray* MakeNodeIDList( DSL_intArray* parents )
{
	int numitems = parents->NumItems();
	DSL_stringArray* ret = new DSL_stringArray( numitems );

	DSL_node* ptrTempNode = NULL;
	for( int i=0; i<numitems; i++ )
	{
		ptrTempNode = theNet->GetNode( (*parents)[i] );
		if( ptrTempNode ){
			ret->SetString( i, ptrTempNode->Info().Header().GetId() );
		}
		else{
			cerr << "C++ error: MakeNodeIDList() called on bad DSL_intArray, either contains invalid node handles or not initialized to the right length." << endl;
			break;
		}
	}

	ret->UseAsList();
	return ret;
}

jint ConvertDSLNodeTypeToSMILEReaderExpectedIntConstant( int DSLvalue )
{
	char* strerror = NULL;
    if( DSLvalue == DSL_CPT ) return edu_ucla_belief_io_dsl_SMILEReader_DSL_CPT;
    else if( DSLvalue == DSL_NOISY_MAX ) return edu_ucla_belief_io_dsl_SMILEReader_DSL_NOISY_MAX;
    else if( DSLvalue == DSL_TRUTHTABLE ) return edu_ucla_belief_io_dsl_SMILEReader_DSL_TRUTHTABLE;
    else if( DSLvalue == DSL_LIST ) strerror = "LIST";
    //else if( DSLvalue == DSL_NOISY_AND ) strerror = "NOISY_AND";
    else if( DSLvalue == DSL_TABLE ) strerror = "TABLE";
    else if( DSLvalue == DSL_MAU ) strerror = "MAU";

    char* nodetype_error_message = new char[125];
    sprintf( nodetype_error_message, "C++ error: Network contains unsupported DSL node type: %s.", strerror );
    theEnvironment->ThrowNew( class_Exception, nodetype_error_message );
    return edu_ucla_belief_io_dsl_SMILEReader_UNDEFINED_NODE_TYPE;
}

jint ConvertDiagnosisTypeToSMILEReaderExpectedIntConstant( DSL_extraDefinition::troubleType DSLvalue )
{
    if( DSLvalue == DSL_extraDefinition::target ) return edu_ucla_belief_io_dsl_SMILEReader_TARGET;
    else if( DSLvalue == DSL_extraDefinition::observation ) return edu_ucla_belief_io_dsl_SMILEReader_OBSERVATION;
    else if( DSLvalue == DSL_extraDefinition::auxiliary ) return edu_ucla_belief_io_dsl_SMILEReader_AUXILIARY;
    //else return edu_ucla_belief_io_dsl_SMILEReader_UNDEFINED_NODE_TYPE;
    else
    {
	    cerr << "C++ warning: encountered unknown DSL_extraDefinition::troubleType " << DSLvalue << endl;//debug
	    cerr << "C++ defaulting to type 'auxiliary'." << endl;//debug
	    return edu_ucla_belief_io_dsl_SMILEReader_AUXILIARY;
    }
}

DSL_extraDefinition::troubleType ConvertSMILEReaderIntConstantToDiagnosisType( jint SMILEReaderValue )
{
	if( SMILEReaderValue == edu_ucla_belief_io_dsl_SMILEReader_TARGET ) return DSL_extraDefinition::target;
	else if( SMILEReaderValue == edu_ucla_belief_io_dsl_SMILEReader_OBSERVATION ) return DSL_extraDefinition::observation;
	else if( SMILEReaderValue == edu_ucla_belief_io_dsl_SMILEReader_AUXILIARY ) return DSL_extraDefinition::auxiliary;
	else
	{
		cerr << "C++ warning: encountered unknown diagnosis type " << SMILEReaderValue << endl;//debug
		cerr << "C++ defaulting to type 'auxiliary'." << endl;//debug
		return DSL_extraDefinition::auxiliary;
	}
}

jintArray MakeStrengthsArray( DSL_noisyMAX* noisyornodedef, DSL_intArray* parents )
{
	int numitems = parents->NumItems();
	int length = 0;

	DSL_node* ptrTempNode = NULL;
	for( int i=0; i<numitems; i++ )
	{
		ptrTempNode = theNet->GetNode( (*parents)[i] );
		if( ptrTempNode ){
			length += ptrTempNode->Definition()->GetSize();
		}
		else{
			cerr << "C++ error: MakeStrengthsArray() called on bad DSL_intArray, either contains invalid node handles or not initialized to the right length." << endl;
			break;
		}
	}

	jint* jarray = new jint[length];
	int index = 0;

	DSL_intArray arrayStrengthsOneParent;
	int sizeStrengthsOneParent = 0;
	for( int i=0; i<numitems; i++ )
	{
		arrayStrengthsOneParent = noisyornodedef->GetParentOutcomeStrengths( i );
		if( &arrayStrengthsOneParent ){
			sizeStrengthsOneParent = arrayStrengthsOneParent.GetSize();
			for( int j=0; j<sizeStrengthsOneParent; j++ )
				jarray[index++] = arrayStrengthsOneParent[j];
		}
		else{
			cerr << "C++ error: MakeStrengthsArray() GetParentOutcomeStrengths() failed." << endl;
			break;
		}
	}

	jintArray ret = Convert( jarray, length );
	delete jarray;
	return ret;
}

 int PutNetworkParameter( const char* ckey, char* cvalue )
 {
     jstring jkey = theEnvironment->NewStringUTF( ckey );
     jstring jvalue = theEnvironment->NewStringUTF( cvalue );

     theEnvironment->CallVoidMethod( objThis, id_putNetworkParameterStr, jkey, jvalue );
     if( theEnvironment->ExceptionCheck() ){ return EXCEPTION_OCCURRED; }

     /*		This is NOT the right thing to do:	*/
     //cleanup
     //theEnvironment->ReleaseStringUTFChars( jkey, ckey );
     //theEnvironment->ReleaseStringUTFChars( jvalue, cvalue );

     else return SUCCESS;
 }

// int PutNetworkParameter( const char* ckey, int value )
// {
//     jstring jkey = theEnvironment->NewStringUTF( ckey );

//     theEnvironment->CallVoidMethod( objThis, id_putNetworkParameterInt, jkey, value );

//     if( theEnvironment->ExceptionCheck() ){ return EXCEPTION_OCCURRED; }
//     else return SUCCESS;
// }

//const char* KEY_HEADER_ID =  			"DSL.HEADER.ID";
//const char* KEY_HEADER_NAME =			"DSL.HEADER.NAME";
//const char* KEY_HEADER_COMMENT =		"DSL.HEADER.COMMENT";
//const char* KEY_CREATION_CREATOR =		"DSL.CREATION.CREATOR";
//const char* KEY_CREATION_CREATED =		"DSL.CREATION.CREATED";
//const char* KEY_CREATION_MODIFIED =		"DSL.CREATION.MODIFIED";
//const char* KEY_SCREEN_COLOR =			"DSL.SCREEN.COLOR";
//const char* KEY_SCREEN_SELCOLOR =	       	"DSL.SCREEN.SELCOLOR";
//const char* KEY_SCREEN_FONT =			"DSL.SCREEN.FONT";
//const char* KEY_SCREEN_FONTCOLOR =		"DSL.SCREEN.FONTCOLOR";
//const char* KEY_SCREEN_BORDERTHICKNESS =	"DSL.SCREEN.BORDERTHICKNESS";
//const char* KEY_SCREEN_BORDERCOLOR =		"DSL.SCREEN.BORDERCOLOR";
//const char* keyISDSL =			"isDSL";

int LoadNetworkParameters( DSL_network* theNet )
{
	int status = (int)0;

	DSL_userProperties userproperties = theNet->UserProperties();
	int numitems = userproperties.GetNumberOfProperties();

	for( int i=0; i<numitems; i++ )
	{
		status = PutNetworkParameter( userproperties.GetPropertyName(i),userproperties.GetPropertyValue(i) );
		if( status == EXCEPTION_OCCURRED ) return status;
	}

/*
  status = PutNetworkParameter( keyISDSL, NULL );
  if( status == EXCEPTION_OCCURRED ) return status;


  char* tempVal = NULL;

  //header*****************************************************************
  DSL_header netHeader = theNet->Header();

  tempVal = netHeader.GetId();
  if( tempVal ){
  status = PutNetworkParameter( KEY_HEADER_ID, tempVal );
  if( status == EXCEPTION_OCCURRED ) return status;
  }

  tempVal = netHeader.GetName();
  if( tempVal ){
  status = PutNetworkParameter( KEY_HEADER_NAME, tempVal );
  if( status == EXCEPTION_OCCURRED ) return status;
  }

  tempVal = netHeader.GetComment();
  if( tempVal ){
  status = PutNetworkParameter( KEY_HEADER_COMMENT, tempVal );
  if( status == EXCEPTION_OCCURRED ) return status;
  }
  //header*****************************************************************

  //creation*****************************************************************
  DSL_creation netCreation = theNet->Creation();

  tempVal = netCreation.GetCreator();
  if( tempVal ){
  status = PutNetworkParameter( KEY_CREATION_CREATOR, tempVal );
  if( status == EXCEPTION_OCCURRED ) return status;
  }

  tempVal = netCreation.GetCreated();
  if( tempVal ){
  status = PutNetworkParameter( KEY_CREATION_CREATED, tempVal );
  if( status == EXCEPTION_OCCURRED ) return status;
  }

  tempVal = netCreation.GetModified();
  if( tempVal ){
  status = PutNetworkParameter( KEY_CREATION_MODIFIED, tempVal );
  if( status == EXCEPTION_OCCURRED ) return status;
  }
  //creation*****************************************************************

  DSL_submodelHandler netSH = theNet->GetSubmodelHandler();

  //screen info**************************************************************
  DSL_screenInfo netScreenInfo = netSH.GetSubmodelDefaultValues();

  status = PutNetworkParameter( KEY_SCREEN_COLOR, netScreenInfo.color );
  if( status == EXCEPTION_OCCURRED ) return status;

  status = PutNetworkParameter( KEY_SCREEN_SELCOLOR, netScreenInfo.selColor );
  if( status == EXCEPTION_OCCURRED ) return status;

  status = PutNetworkParameter( KEY_SCREEN_FONT, netScreenInfo.font );
  if( status == EXCEPTION_OCCURRED ) return status;

  status = PutNetworkParameter( KEY_SCREEN_FONTCOLOR, netScreenInfo.fontColor );
  if( status == EXCEPTION_OCCURRED ) return status;

  status = PutNetworkParameter( KEY_SCREEN_BORDERTHICKNESS, netScreenInfo.borderThickness );
  if( status == EXCEPTION_OCCURRED ) return status;

  status = PutNetworkParameter( KEY_SCREEN_BORDERCOLOR, netScreenInfo.borderColor );
  if( status == EXCEPTION_OCCURRED ) return status;
  //screen info**************************************************************

*/

    return SUCCESS;
}
