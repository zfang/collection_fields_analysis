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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

   static boolean isJavaOrSunLibMethod(SootMethod method) {
      return method.getDeclaringClass().toString().contains("java.") 
         || method.getDeclaringClass().toString().contains("sun.");
   }

   @Override
      protected void internalTransform(String phaseName, Map options) {
         graph = Scene.v().getCallGraph();
         Chain<SootClass> classes = Scene.v().getApplicationClasses();
         ArrayList<SootMethod> methods = new ArrayList<SootMethod>();
         Set<SootMethod> sortedMethods = new LinkedHashSet<SootMethod>();

         for (SootClass c : classes) {
            methods.addAll(c.getMethods());
         }

         for (SootMethod method : methods) {
            if (method.hasActiveBody()
                  && !sortedMethods.contains(method)
                  && !(isJavaOrSunLibMethod(method))) {
               findEdges(method, sortedMethods);
            }
         }

         //G.v().out.println("[CollectionFieldsAnalysis] Sorted: " + sortedMethods);

         for (SootMethod m : sortedMethods) {
            if (m.hasActiveBody()) {
               Body body = m.getActiveBody();
               new MainCollectionFieldsAnalysis(new ExceptionalUnitGraph(body));
            }
         }
      }

   // Method to arrange the order of methods depending on the caller & callee
   // relations
   // if a calls b, a is stored earlier than b.
   protected void findEdges(SootMethod m, Set<SootMethod> sorted) {
      sorted.add(m);

      Iterator<Edge> it = graph.edgesOutOf(m);
      while (it.hasNext()) {
         Edge e = it.next();
         SootMethod targetM = (SootMethod) e.getTgt();
         if (isJavaOrSunLibMethod(targetM)) {
            continue;
         }

         if (m.equals(targetM)) {
            continue;
         }

         sorted.remove(targetM);

         findEdges(targetM, sorted);
      }
   }
}
