/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Kevin Cornell (Rational Software Corporation)
 *     keith cascio 20060316 (UCLA Automated Reasoning Group)
 *******************************************************************************/

#ifndef ECLIPSE_OS_H
#define ECLIPSE_OS_H

//#include "callsmileUnicode.h"

#ifdef UNICODE
#define _Java_edu_ucla_belief_io_dsl_SMILEReader_librarySupports _Java_edu_ucla_belief_io_dsl_SMILEReader_librarySupportsW
#define _Java_edu_ucla_belief_io_dsl_SMILEReader_loadDSL _Java_edu_ucla_belief_io_dsl_SMILEReader_loadDSLW
#define _Java_edu_ucla_belief_io_dsl_SMILEReader_updateDSL _Java_edu_ucla_belief_io_dsl_SMILEReader_updateDSLW
#define AddUserProperties AddUserPropertiesW
#define class_Exception class_ExceptionW
#define class_SMILEReader class_SMILEReaderW
#define class_String class_StringW
#define Convert ConvertW
#define ConvertDiagnosisTypeToSMILEReaderExpectedIntConstant ConvertDiagnosisTypeToSMILEReaderExpectedIntConstantW
#define ConvertDSLNodeTypeToSMILEReaderExpectedIntConstant ConvertDSLNodeTypeToSMILEReaderExpectedIntConstantW
#define ConvertSMILEReaderIntConstantToDiagnosisType ConvertSMILEReaderIntConstantToDiagnosisTypeW
#define ConvertStringArray ConvertStringArrayW
#define DeleteEnumPropDefs DeleteEnumPropDefsW
#define DELIMITER DELIMITERW
#define FLAG_XDSL_ENABLED FLAG_XDSL_ENABLEDW
#define GetDoubleRow GetDoubleRowW
#define GetIntRow GetIntRowW
#define GetStringRow GetStringRowW
#define id_debugLoadNode id_debugLoadNodeW
#define id_loadChildOfRootSubmodels id_loadChildOfRootSubmodelsW
#define id_loadNode id_loadNodeW
#define id_loadSubmodel id_loadSubmodelW
#define id_putNetworkParameterInt id_putNetworkParameterIntW
#define id_putNetworkParameterStr id_putNetworkParameterStrW
#define init initW
#define initSupport initSupportW
#define Java_edu_ucla_belief_io_dsl_SMILEReader_librarySupports Java_edu_ucla_belief_io_dsl_SMILEReader_librarySupportsW
#define Java_edu_ucla_belief_io_dsl_SMILEReader_loadDSL Java_edu_ucla_belief_io_dsl_SMILEReader_loadDSLW
#define Java_edu_ucla_belief_io_dsl_SMILEReader_testJNI Java_edu_ucla_belief_io_dsl_SMILEReader_testJNIW
#define Java_edu_ucla_belief_io_dsl_SMILEReader_updateDSL Java_edu_ucla_belief_io_dsl_SMILEReader_updateDSLW
#define JNI_OnLoad JNI_OnLoadW
#define KEY_USERPROPERTIES KEY_USERPROPERTIESW
#define LoadNetworkParameters LoadNetworkParametersW
#define LoadNode LoadNodeW
#define LoadSubmodel LoadSubmodelW
#define LoadSubmodels LoadSubmodelsW
#define main mainW
#define MakeNodeIDList MakeNodeIDListW
#define MakeNodeNameList MakeNodeNameListW
#define MakeStrengthsArray MakeStrengthsArrayW
#define objThis objThisW
#define PutNetworkParameter PutNetworkParameterW
#define ReadDSL ReadDSLW
#define STR_EMPTY STR_EMPTYW
#define theEnvironment theEnvironmentW
#define theNet theNetW
#define theSubmodelHandler theSubmodelHandlerW
#define WriteDSL WriteDSLW
#endif

#endif /* ECLIPSE_OS_H */
