/* Soot - a J*va Optimization Framework
 * Copyright (C) 2008 Eric Bodden
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package com.zfang.cf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

public class MyMain {

	public static void main(String[] args) {
		G.v().out.println("Adding My Scene Transformer");
		PackManager.v().getPack("wjtp").add(
				new Transform("wjtp.myTransform", new MySceneTransformer()));
		soot.Main.main(args);
	}
}

class MySceneTransformer extends SceneTransformer {
	protected CallGraph graph;

	protected void internalTransform(String phaseName, Map options) {
		graph = Scene.v().getCallGraph();
		Chain<SootClass> classes = Scene.v().getApplicationClasses();
		ArrayList<SootMethod> methods = new ArrayList<SootMethod>();
		ArrayList<SootMethod> sortedMethods = new ArrayList<SootMethod>();
		ArrayList<SootMethod> visitedMethods = new ArrayList<SootMethod>();
		ArrayList<ArrayList<SootMethod>> circles = new ArrayList<ArrayList<SootMethod>>();

		for (SootClass c : classes) {
			methods.addAll(c.getMethods());
		}

		for (SootMethod method : methods) {
			if (method.hasActiveBody()
					&& !sortedMethods.contains(method)
					&& !(method.getDeclaringClass().toString()
							.contains("java.") || method.getDeclaringClass()
							.toString().contains("sun."))) {
				findEdges(method, sortedMethods, visitedMethods, circles);
			}
		}

		for (SootMethod m : sortedMethods) {
			if (m.hasActiveBody()) {
				Body body = m.getActiveBody();
            new MainCollectionFieldsAnalysis(new ExceptionalUnitGraph(body));
			}
		}
	}

	// Method to arrange the order of methods depending on the caller & callee
	// relations
	// if a calls b, b is stored earlier than a.
	protected void findEdges(SootMethod m, ArrayList<SootMethod> sorted,
			ArrayList<SootMethod> visited,
			ArrayList<ArrayList<SootMethod>> circles) {
		visited.add(m);

		Iterator<Edge> it = graph.edgesOutOf(m);
		while (it.hasNext()) {
			Edge e = it.next();
			SootMethod targetM = (SootMethod) e.getTgt();
			 if (targetM.getDeclaringClass().toString().contains("java.") ||
			 targetM.getDeclaringClass().toString().contains("sun.")) {
			 continue;
			 }

			if (!sorted.contains(targetM)) {
				// solve self-loop
				if (targetM.equals(m)) {
					ArrayList<SootMethod> circle = new ArrayList<SootMethod>();
					circle.add(m);
					circles.add(circle);
				}
				// solve circles
				else if (visited.contains(targetM)) {
					ArrayList<SootMethod> circle = new ArrayList<SootMethod>();
					Iterator<SootMethod> it1 = visited.iterator();
					whilebreak: while (it1.hasNext()) {
						SootMethod mintheCircle = it1.next();
						if (mintheCircle.equals(targetM)) {
							circle.add(mintheCircle);
							while (it1.hasNext()) {
								mintheCircle = it1.next();
								circle.add(mintheCircle);
								if (mintheCircle.equals(m)) {
									break whilebreak;
								}
							}

						}
					}
					if (!circles.contains(circle))
						circles.add(circle);
				}
				// no self-loop or circles found
				else {
					findEdges(targetM, sorted, visited, circles);
				}
			}
		}
		sorted.add(m);
	}
}
