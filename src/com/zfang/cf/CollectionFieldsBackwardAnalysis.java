package com.zfang.cf;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.G;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

public class CollectionFieldsBackwardAnalysis extends BackwardFlowAnalysis<Unit, FlowSet> {

   public static final String TAG = "CollectionFieldsBackwardAnalysis";

   Set<ObjectFieldPair> 
      nonAliasedFields = new HashSet<ObjectFieldPair>(), 
      otherFields = new HashSet<ObjectFieldPair>(),
      unknownFields = new HashSet<ObjectFieldPair>();

   /* 0: fields from nonAliasedFields
    * 1: fields from otherFields
    * 2: fields from unknownFields
    */
   List<FieldLocalMap> [] mayAliasedFieldStore = (List<FieldLocalMap>[]) new List[3];
   Set<InstanceKey> 
      otherLocals = new HashSet<InstanceKey>(),
      unknownLocals = new HashSet<InstanceKey>();

	LocalMustAliasAnalysis localMustAliasAnalysis;
	LocalMustNotAliasAnalysis localNotMayAliasAnalysis;
   SootMethod m;
   Body body;
   ExceptionalUnitGraph g;

   List<SootClass> ALL_COLLECTIONS = Scene.v().getActiveHierarchy()
      .getDirectImplementersOf(
            RefType.v("java.util.Collection").getSootClass());
   // A list that contains names of all subclasses of java.util.Collection.
   Set<String> ALL_COLLECTION_NAMES = new HashSet<String>();
   {
      for (SootClass cl : ALL_COLLECTIONS) {
         ALL_COLLECTION_NAMES.add(cl.toString());
      }
      ALL_COLLECTION_NAMES.add("java.util.Collection");
      ALL_COLLECTION_NAMES.add("java.util.List");
      ALL_COLLECTION_NAMES.add("java.util.SortedSet");
      ALL_COLLECTION_NAMES.add("java.util.Set");
   }

   public CollectionFieldsBackwardAnalysis(ExceptionalUnitGraph exceptionalUnitGraph) {
      super(exceptionalUnitGraph);

		localMustAliasAnalysis = new LocalMustAliasAnalysis(
				exceptionalUnitGraph, true);
		localNotMayAliasAnalysis = new LocalMustNotAliasAnalysis(
				exceptionalUnitGraph);

      for (int i = 0, size = mayAliasedFieldStore.length; i < size; ++i) {
         mayAliasedFieldStore[i] = new LinkedList<FieldLocalMap>();
      }

      m = exceptionalUnitGraph.getBody().getMethod();
      body = exceptionalUnitGraph.getBody();
      g = exceptionalUnitGraph;
      doAnalysis();
   }

   public void print(Object obj) {
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

   @Override
      protected void flowThrough(FlowSet in, Unit dd, FlowSet out) {
         in.copy(out);

         // Ignore constructors
         if (m.isConstructor()) {
            return;
         }

         Stmt d = (Stmt) dd;
         Stmt ds;

         // We need instance keys stored before the successor of current
         // statement.
         if (g.getPredsOf(d).size() >= 1)
            ds = (Stmt) g.getPredsOf(d).get(0);
         else
            ds = d;

         if (d instanceof ReturnStmt) {
            Value op = ((ReturnStmt) d).getOp();

            if (ALL_COLLECTION_NAMES.contains(op.getType().toString())) {
               // TODO
            }
         }

         if (d instanceof DefinitionStmt) {
            Value leftop = ((DefinitionStmt) d).getLeftOp(),
                  rightop = ((DefinitionStmt) d).getRightOp();

            if (ALL_COLLECTION_NAMES.contains(leftop.getType().toString())) {
               if (leftop instanceof Local) {
                  // TODO
               }
            }
         }


         if (g.getPredsOf(d).size() == 0) {
            // TODO
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
