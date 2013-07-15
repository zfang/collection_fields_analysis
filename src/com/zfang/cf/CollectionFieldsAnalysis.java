package com.zfang.cf;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.G;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public abstract class CollectionFieldsAnalysis extends ForwardFlowAnalysis<Unit, FlowSet> {

   public static final String TAG = "CollectionFieldsAnalysis";

	protected LocalMustAliasAnalysis localMustAliasAnalysis;
	protected LocalMustNotAliasAnalysis localNotMayAliasAnalysis;
   protected SootMethod m;
   protected Body body;
   protected ExceptionalUnitGraph g;

   protected FieldLocalStore fieldLocalStore = new FieldLocalStore();

   protected List<SootClass> ALL_COLLECTIONS = Scene.v().getActiveHierarchy()
      .getDirectImplementersOf(
            RefType.v("java.util.Collection").getSootClass());
   // A list that contains names of all subclasses of java.util.Collection.
   protected Set<String> ALL_COLLECTION_NAMES = new HashSet<String>();
   {
      for (SootClass cl : ALL_COLLECTIONS) {
         ALL_COLLECTION_NAMES.add(cl.toString());
      }
      ALL_COLLECTION_NAMES.add("java.util.Collection");
      ALL_COLLECTION_NAMES.add("java.util.List");
      ALL_COLLECTION_NAMES.add("java.util.SortedSet");
      ALL_COLLECTION_NAMES.add("java.util.Set");
   }

   protected CollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph) {
      super(exceptionalUnitGraph);

		localMustAliasAnalysis = new LocalMustAliasAnalysis(
				exceptionalUnitGraph, true);
		localNotMayAliasAnalysis = new LocalMustNotAliasAnalysis(
				exceptionalUnitGraph);

      m = exceptionalUnitGraph.getBody().getMethod();
      body = exceptionalUnitGraph.getBody();
      g = exceptionalUnitGraph;
   }

   public static boolean isNewOrNull(Value op) {
      return (op instanceof soot.jimple.NullConstant 
            || op.getType() instanceof soot.NullType
            || op instanceof soot.jimple.NewExpr);
   }

   protected void print(String TAG, Object obj) {
      String [] tokens = obj.toString().split("\n");
      for (String token : tokens) {
         G.v().out.println(
               new StringBuilder()
               .append("[")
               .append(TAG)
               .append("] ")
               .append(token)
               .toString()
               );
      }
   }

   abstract protected void analyzeExternal(Object o, ParameterRef param);

   abstract protected void analyzeExternal(Object o, Stmt d);

   abstract protected void collectData(Stmt d, Stmt ds);

   abstract protected void finalProcess(Stmt d);

   @Override
      protected void flowThrough(FlowSet in, Unit dd, FlowSet out) {
         in.copy(out);

         // Ignore constructors
         if (m.isConstructor())
            return;

         Stmt d = (Stmt) dd;
         Stmt ds;

         // We need instance keys stored before the successor of current
         // statement.
         if (g.getSuccsOf(d).size() >= 1)
            ds = (Stmt) g.getSuccsOf(d).get(0);
         else
            ds = d;

         collectData(d, ds);

         if (g.getSuccsOf(d).size() == 0) {
            fieldLocalStore.finalize();
            finalProcess(d);
         }
      }

   @Override
      protected void copy(FlowSet source, FlowSet dest) {
         source.copy(dest);

      }

   @Override
      protected FlowSet entryInitialFlow() {
         return new ValueArraySparseSet();
      }

   @Override
      protected void merge(FlowSet in1, FlowSet in2, FlowSet out) {
         in1.union(in2, out); // Use union temporarily
      }

   @Override
      protected FlowSet newInitialFlow() {
         return new ValueArraySparseSet();
      }

}
