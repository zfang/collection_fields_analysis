/* This class is used in with grimp pack (gb)
 * to analyze if object fields that are collections 
 * are assigned
 */

package com.hang.ld;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.Body;
import soot.EquivTo;
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
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.tagkit.StringTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class CollectionFieldsAnalysis extends ForwardFlowAnalysis<Unit, FlowSet> {

   class ValueArraySparseSet extends ArraySparseSet {
      public boolean contains(Object obj) {
         for (int i = 0; i < numElements; i++)
            if (elements[i] instanceof EquivTo
                  && ((EquivTo) elements[i]).equivTo(obj))
               return true;
            else if (elements[i].equals(obj))
               return true;
         return false;
      }
   }

   public static final String TAG = "CollectionFieldsAnalysis";

	CallGraph graph;

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

	static LocalMustAliasAnalysis localMustAliasAnalysis;
	static LocalMustNotAliasAnalysis localNotMayAliasAnalysis;
   static SootMethod m;
   static Body body;
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

   public CollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph, CallGraph graph) {
      super(exceptionalUnitGraph);

      this.graph = graph;

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

   private void addToStore(ObjectFieldPair objectFieldPair, InstanceKey local,  List<FieldLocalMap> store) {
      if ((null == objectFieldPair && null == local) || null == store) {
         return;
      }
      FieldLocalMap fieldLocalMap = new FieldLocalMap();
      fieldLocalMap.addToLocalSet(local);
      fieldLocalMap.addToFieldSet(objectFieldPair);
      store.add(fieldLocalMap);
   }

   private void addToStore(InstanceKey local1,  InstanceKey local2,  List<FieldLocalMap> store) {
      if ((null == local1 && null == local2) || null == store) {
         return;
      }
      FieldLocalMap fieldLocalMap = new FieldLocalMap();
      fieldLocalMap.addToLocalSet(local1);
      fieldLocalMap.addToLocalSet(local2);
      store.add(fieldLocalMap);
   }

   private void removeField(ObjectFieldPair objectFieldPair) {
      if (!nonAliasedFields.remove(objectFieldPair) 
            && !otherFields.remove(objectFieldPair) 
            && !unknownFields.remove(objectFieldPair)) {
         for (List<FieldLocalMap> store : mayAliasedFieldStore) {
            for (FieldLocalMap fieldLocalMap : store) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               if (fieldSet.remove(objectFieldPair)) {
                  return;
               }
            }
         }
      }
   }

   private void addField(ObjectFieldPair objectFieldPair, InstanceKey rightKey) {
      //print("addField: rightKey: " + rightKey);
      removeField(objectFieldPair);

      if (otherLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, mayAliasedFieldStore[1]);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, mayAliasedFieldStore[2]);
         return;
      }

      if (null == rightKey) {
         nonAliasedFields.add(objectFieldPair);
         return;
      }

      for (List<FieldLocalMap> store : mayAliasedFieldStore) {
         for (FieldLocalMap fieldLocalMap : store) {
            Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
            if (localSet.contains(rightKey)) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               fieldSet.add(objectFieldPair);
               return;
            }
         }
      }

      unknownFields.add(objectFieldPair);
   }

   private void addLocal(InstanceKey leftKey, InstanceKey rightKey) {
      //print("addField: leftKey: " + leftKey);
      if (null == rightKey) {
         addToStore(leftKey, rightKey, mayAliasedFieldStore[0]);
         return;
      }

      for (List<FieldLocalMap> store : mayAliasedFieldStore) {
         for (FieldLocalMap fieldLocalMap : store) {
            Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
            if (localSet.contains(rightKey)) {
               localSet.add(leftKey);
               return;
            }
         }
      }

      if (otherLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, mayAliasedFieldStore[1]);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, mayAliasedFieldStore[2]);
         return;
      }

   }

   private void addLocal(InstanceKey leftKey, ObjectFieldPair objectFieldPair) {

      for (List<FieldLocalMap> store : mayAliasedFieldStore) {
         for (FieldLocalMap fieldLocalMap : store) {
            Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
            if (fieldSet.contains(objectFieldPair)) {
               Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
               localSet.add(leftKey);
               return;
            }
         }
      }

      if (nonAliasedFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, mayAliasedFieldStore[0]);
         return;
      }

      if (otherFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, mayAliasedFieldStore[1]);
         return;
      }

      if (unknownFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, mayAliasedFieldStore[2]);
         return;
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
         Value leftop, rightop;
         Stmt ds;

         // We need instance keys stored before the successor of current
         // statement.
         if (g.getSuccsOf(d).size() >= 1)
            ds = (Stmt) g.getSuccsOf(d).get(0);
         else
            ds = d;

         if (d instanceof DefinitionStmt) {
            leftop = ((DefinitionStmt) d).getLeftOp();
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
                     addField(objectFieldPair, null);
                        }
                  // Check if rightop is CastExpr
                  else if (rightop instanceof soot.jimple.CastExpr) {
                     InstanceKey rightKey = new InstanceKey((Local) ((CastExpr)rightop).getOp(), ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     addField(objectFieldPair, rightKey);
                  }
                  // Check if rightop is Local
                  else if (rightop instanceof Local) {
                     InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     addField(objectFieldPair, rightKey);
                  }
                  else if (rightop instanceof soot.jimple.ParameterRef
                        || rightop instanceof soot.jimple.InvokeExpr) {
                     otherFields.add(objectFieldPair);
                  }
                  else {
                     unknownFields.add(objectFieldPair);
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
                     addLocal(leftKey, (InstanceKey)null);
                  }
                  // Check if rightop is CastExpr
                  else if (rightop instanceof soot.jimple.CastExpr) {
                     InstanceKey rightKey = new InstanceKey((Local) ((CastExpr)rightop).getOp(), ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     addLocal(leftKey, rightKey);
                  }
                  // Check if rightop is Local 
                  else if (rightop instanceof Local) {
                     InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     addLocal(leftKey, rightKey);
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
                     addLocal(leftKey, objectFieldPair);
                  }
                  else if (rightop instanceof soot.jimple.ParameterRef
                        || rightop instanceof soot.jimple.InvokeExpr) {
                     otherLocals.add(leftKey);
                  }
                  else {
                     unknownLocals.add(leftKey);
                  }
               }
               //String result = new StringBuilder()
               //   .append("nonAliasedFields: ")
               //   .append(nonAliasedFields.toString())
               //   .append("\n")
               //   .append("nonAliasedFields size: ")
               //   .append(nonAliasedFields.size())
               //   .append("\n")
               //   .append("mayAliasedFieldStore[0]: ")
               //   .append(mayAliasedFieldStore[0].toString())
               //   .append("\n")
               //   .append("mayAliasedFieldStore[0] size: ")
               //   .append(mayAliasedFieldStore[0].size())
               //   .append("\n")
               //   .append("mayAliasedFieldStore[1]: ")
               //   .append(mayAliasedFieldStore[1].toString())
               //   .append("\n")
               //   .append("mayAliasedFieldStore[1] size: ")
               //   .append(mayAliasedFieldStore[1].size())
               //   .append("\n")
               //   .append("mayAliasedFieldStore[2]: ")
               //   .append(mayAliasedFieldStore[2].toString())
               //   .append("\n")
               //   .append("mayAliasedFieldStore[2] size: ")
               //   .append(mayAliasedFieldStore[2].size())
               //   .append("\n")
               //   .append("otherFields: ")
               //   .append(otherFields.toString())
               //   .append("\n")
               //   .append("otherFields size: ")
               //   .append(otherFields.size())
               //   .append("\n")
               //   .append("unknownFields: ")
               //   .append(unknownFields.toString())
               //   .append("\n")
               //   .append("unknownFields size: ")
               //   .append(unknownFields.size())
               //   .append("\n")
               //   .toString();
               //print(result);
            }
         }
         //if (d.containsInvokeExpr()) {
         //   InvokeExpr invoke = d.getInvokeExpr();
         //   if (invoke instanceof InstanceInvokeExpr) {
         //      Value v = ((InstanceInvokeExpr) invoke).getBase();
         //      if (ALL_COLLECTION_NAMES.contains(v.getType().toString())) {
         //         if (v instanceof Local) {
         //            InstanceKey vKey = new InstanceKey((Local) v, ds, m,
         //                  localMustAliasAnalysis, localNotMayAliasAnalysis);
         //            print("vKey: " + vKey);
         //         }
         //      }
         //   }
         //}


         if (g.getSuccsOf(d).size() == 0) {
            Set<FieldLocalMap> aliasedFieldStore = new HashSet<FieldLocalMap>();

            for (FieldLocalMap fieldLocalMap : mayAliasedFieldStore[0]) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               if (fieldSet.size() == 1) {
                  nonAliasedFields.addAll(fieldSet);
               }
               else if (fieldSet.size() > 1) {
                  aliasedFieldStore.add(fieldLocalMap);
               }
            }

            for (FieldLocalMap fieldLocalMap : mayAliasedFieldStore[1]) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               if (fieldSet.size() == 1) {
                  otherFields.addAll(fieldSet);
               }
               else if (fieldSet.size() > 1) {
                  aliasedFieldStore.add(fieldLocalMap);
               }
            }

            for (FieldLocalMap fieldLocalMap : mayAliasedFieldStore[2]) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               if (fieldSet.size() == 1) {
                  unknownFields.addAll(fieldSet);
               }
               else if (fieldSet.size() > 1) {
                  aliasedFieldStore.add(fieldLocalMap);
               }
            }

            if (!nonAliasedFields.isEmpty() || !aliasedFieldStore.isEmpty() || !otherFields.isEmpty()) {
               int aliasedFieldsCount = 0;
               for (FieldLocalMap fieldLocalMap : aliasedFieldStore) {
                  aliasedFieldsCount += fieldLocalMap.getFieldSet().size();
               }

               print("At Method "+m);
               String result = new StringBuilder()
                  .append("nonAliasedFields: ")
                  .append(nonAliasedFields.toString())
                  .append("\n")
                  .append("nonAliasedFields size: ")
                  .append(nonAliasedFields.size())
                  .append("\n")
                  .append("aliasedFieldStore: ")
                  .append(aliasedFieldStore.toString())
                  .append("\n")
                  .append("aliasedFields size: ")
                  .append(aliasedFieldsCount)
                  .append("\n")
                  .append("otherFields: ")
                  .append(otherFields.toString())
                  .append("\n")
                  .append("otherFields size: ")
                  .append(otherFields.size())
                  .append("\n")
                  .append("unknownFields: ")
                  .append(unknownFields.toString())
                  .append("\n")
                  .append("unknownFields size: ")
                  .append(unknownFields.size())
                  .append("\n")
                  .toString();
               print(result);
               m.addTag(new StringTag(result));
            }
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
