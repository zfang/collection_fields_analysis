package com.zfang.cf;

import static com.zfang.cf.CollectionFieldsAnalysis.isFromJavaOrSunPackage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;

public class MySceneTransformer extends SceneTransformer {
	protected CallGraph graph;

   @Override
      protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
         graph = Scene.v().getCallGraph();
         Chain<SootClass> classes = Scene.v().getApplicationClasses();
         ArrayList<SootMethod> methods = new ArrayList<SootMethod>();
         Set<SootMethod> sortedMethods = new LinkedHashSet<SootMethod>();
         Set<SootMethod> visitedMethods = new HashSet<SootMethod>();

         for (SootClass c : classes) {
            methods.addAll(c.getMethods());
         }

         for (SootMethod method : methods) {
            if (method.hasActiveBody()
                  && !sortedMethods.contains(method)
                  && !(isFromJavaOrSunPackage(method))) {
               findEdges(method, sortedMethods, visitedMethods);
            }
         }

         // CollectionFieldsAnalysis.print("Number of methods: " + sortedMethods.size());

         for (SootMethod m : sortedMethods) {
            if (m.hasActiveBody()) {
               Body body = m.getActiveBody();
               new MainCollectionFieldsAnalysis(new ExceptionalUnitGraph(body));
            }
         }
         
         CollectionFieldsAnalysis.printReverseFieldMap();
      }

   // Method to arrange the order of methods depending on the caller & callee
   // relations
   // if a calls b, a is stored earlier than b.
   protected void findEdges(SootMethod m, Set<SootMethod> sorted, Set<SootMethod> visited) {
		visited.add(m);

      sorted.add(m);

      Iterator<Edge> it = graph.edgesOutOf(m);
      while (it.hasNext()) {
         Edge e = it.next();
         SootMethod targetM = (SootMethod) e.getTgt();
         if (isFromJavaOrSunPackage(targetM)) {
            continue;
         }

         if (m.equals(targetM) || visited.contains(targetM)) {
            continue;
         }

         sorted.remove(targetM);

         findEdges(targetM, sorted, visited);
      }
   }
}
