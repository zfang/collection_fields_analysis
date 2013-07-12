package com.zfang.cf;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.G;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.tagkit.StringTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class CollectionFieldsAnalysis extends ForwardFlowAnalysis<Unit, FlowSet> {

   public static final String TAG = "CollectionFieldsAnalysis";


	LocalMustAliasAnalysis localMustAliasAnalysis;
	LocalMustNotAliasAnalysis localNotMayAliasAnalysis;
   SootMethod m;
   Body body;
   ExceptionalUnitGraph g;

   FieldLocalStore fieldLocalStore = new FieldLocalStore();

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

   public CollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph) {
      super(exceptionalUnitGraph);

		localMustAliasAnalysis = new LocalMustAliasAnalysis(
				exceptionalUnitGraph, true);
		localNotMayAliasAnalysis = new LocalMustNotAliasAnalysis(
				exceptionalUnitGraph);

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

   public void analyzeExternal(ParameterRef param, FieldLocalStoreUpdateListener listener) {
      // TODO
   }

   public void analyzeExternal(InvokeExpr invoke, FieldLocalStoreUpdateListener listener) {
      SootMethod method = invoke.getMethod();
      if (method.hasActiveBody()) {
         Body body = method.getActiveBody();
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
         if (g.getSuccsOf(d).size() >= 1)
            ds = (Stmt) g.getSuccsOf(d).get(0);
         else
            ds = d;

         if (d instanceof DefinitionStmt) {
            Value leftop = ((DefinitionStmt) d).getLeftOp(),
                  rightop = ((DefinitionStmt) d).getRightOp();

            if (ALL_COLLECTION_NAMES.contains(leftop.getType().toString())) {
               //print(String.format("%s = %s; rightop class: %s",
               //         leftop.toString(), rightop.toString(), rightop.getClass().getName()));
               // Field references
               if (leftop instanceof FieldRef) {
                  InstanceKey leftopObject = 
                     (leftop instanceof InstanceFieldRef) ?
                     new InstanceKey((Local) ((InstanceFieldRef)leftop).getBase(), ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis)
                     : null;
                  SootField leftopField = ((FieldRef)leftop).getField();
                  ObjectFieldPair objectFieldPair = new ObjectFieldPair(leftopObject, leftopField);
                  // Check if rightop is NullConstant or NewExpr
                  if (rightop instanceof soot.jimple.NullConstant 
                        || rightop.getType() instanceof soot.NullType
                        || rightop instanceof soot.jimple.NewExpr) {
                     fieldLocalStore.addField(objectFieldPair, null);
                        }
                  // Check if rightop is CastExpr
                  else if (rightop instanceof CastExpr) {
                     InstanceKey rightKey = new InstanceKey((Local) ((CastExpr)rightop).getOp(), ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     fieldLocalStore.addField(objectFieldPair, rightKey);
                  }
                  // Check if rightop is Local
                  else if (rightop instanceof Local) {
                     InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     fieldLocalStore.addField(objectFieldPair, rightKey);
                  }
                  else if (rightop instanceof ParameterRef) {
                     // TODO
                     fieldLocalStore.addExternal(objectFieldPair);
                  }
                  else if (rightop instanceof InvokeExpr) {
                     // TODO
                     fieldLocalStore.addExternal(objectFieldPair);
                  }
                  else {
                     fieldLocalStore.addUnknown(objectFieldPair);
                  }
               }
               // Local variables
               else if (leftop instanceof Local) {
                  InstanceKey leftKey = new InstanceKey((Local) leftop, ds, m,
                        localMustAliasAnalysis, localNotMayAliasAnalysis);
                  // Check if rightop is NullConstant or NewExpr
                  if (rightop instanceof soot.jimple.NullConstant
                        || rightop.getType() instanceof soot.NullType
                        || rightop instanceof soot.jimple.NewExpr) {
                     fieldLocalStore.addLocal(leftKey, (InstanceKey)null);
                  }
                  // Check if rightop is CastExpr
                  else if (rightop instanceof CastExpr) {
                     InstanceKey rightKey = new InstanceKey((Local) ((CastExpr)rightop).getOp(), ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     fieldLocalStore.addLocal(leftKey, rightKey);
                  }
                  // Check if rightop is Local 
                  else if (rightop instanceof Local) {
                     InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     fieldLocalStore.addLocal(leftKey, rightKey);
                  }
                  // Check if rightop is FieldRef 
                  else if (rightop instanceof FieldRef) {
                     InstanceKey rightopObject = 
                        (rightop instanceof InstanceFieldRef) ?
                        new InstanceKey((Local) ((InstanceFieldRef)rightop).getBase(), ds, m,
                              localMustAliasAnalysis, localNotMayAliasAnalysis)
                        : null;
                     SootField rightopField = ((FieldRef)rightop).getField();
                     ObjectFieldPair objectFieldPair = new ObjectFieldPair(rightopObject, rightopField);
                     fieldLocalStore.addLocal(leftKey, objectFieldPair);
                  }
                  else if (rightop instanceof ParameterRef) {
                     // TODO
                     fieldLocalStore.addExternal(leftKey);
                  }
                  else if (rightop instanceof InvokeExpr) {
                     // TODO
                     fieldLocalStore.addExternal(leftKey);
                     analyzeExternal((InvokeExpr)rightop,
                           new FieldLocalStoreUpdateListener(null, leftKey, fieldLocalStore));
                  }
                  else {
                     fieldLocalStore.addUnknown(leftKey);
                  }
               }
            }
         }


         if (g.getSuccsOf(d).size() == 0) {
            String result = fieldLocalStore.toString();
            if ("" == result) {
               return;
            }

            print("At Method "+m);
            print(result);
            m.addTag(new StringTag(result));
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
