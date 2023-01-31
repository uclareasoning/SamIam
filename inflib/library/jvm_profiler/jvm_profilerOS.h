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
 *     keith cascio 20060315 (UCLA Automated Reasoning Group)
 *******************************************************************************/

#ifndef ECLIPSE_OS_H
#define ECLIPSE_OS_H

#ifdef UNICODE
#define JVM_OnLoad JVM_OnLoadW
#define Java_edu_ucla_util_JVMProfiler_getCurrentThreadCpuTime_1native Java_edu_ucla_util_JVMProfiler_getCurrentThreadCpuTime_1nativeW
#define Java_edu_ucla_util_JVMProfiler_profilerRunning_1native Java_edu_ucla_util_JVMProfiler_profilerRunning_1nativeW
#define main mainW
#endif

#endif /* ECLIPSE_OS_H */
