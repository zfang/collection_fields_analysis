/* This class is used in with grimp pack (gb)
 * to analyze if object fields that are collections 
 * are assigned
 */

package com.hang.ld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.tagkit.StringTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class CollectionFieldsAnalysis extends ForwardFlowAnalysis<Unit, Connections> {

   public static final String TAG = "CollectionFieldsAnalysis";

   Connections emptyConnection = new Connections();

   static final String fieldKey = "field";
   static final String localKey = "local";
   Set<ObjectFieldPair> 
      nonAliasedFields = new HashSet<ObjectFieldPair>(), 
      unknownFields = new HashSet<ObjectFieldPair>();
   List<Map<String, Set>> 
      aliasedFieldStore = new LinkedList<Map<String, Set>>(),
      mayAliasedFieldStore = new LinkedList<Map<String, Set>>();
   Set<InstanceKey> unknownLocals = new HashSet<InstanceKey>();

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

   private void removeField(ObjectFieldPair objectFieldPair) {
      if (!nonAliasedFields.remove(objectFieldPair) && !unknownFields.remove(objectFieldPair)) {
         for (Map<String, Set> fieldLocalMap : aliasedFieldStore) {
            Set<ObjectFieldPair> fieldSet = (Set<ObjectFieldPair>)fieldLocalMap.get(fieldKey);
            if (fieldSet.remove(objectFieldPair)) {
               return;
            }
         }
      }
   }

   private void addField(ObjectFieldPair objectFieldPair, InstanceKey rightKey) {
      removeField(objectFieldPair);

      if (null == rightKey) {
         nonAliasedFields.add(objectFieldPair);
         return;
      }
      for (Map<String, Set> fieldLocalMap : aliasedFieldStore) {
         Set<InstanceKey> localSet = (Set<InstanceKey>)fieldLocalMap.get(localKey);
         if (localSet.contains(rightKey)) {
            Set<ObjectFieldPair> fieldSet = (Set<ObjectFieldPair>)fieldLocalMap.get(fieldKey);
            fieldSet.add(objectFieldPair);
            return;
         }
      }
      unknownFields.add(objectFieldPair);
   }

   private void addLocal(InstanceKey leftKey, InstanceKey rightKey) {
      if (null == rightKey) {
         Set<InstanceKey> localKeys = new HashSet<InstanceKey>();
         localKeys.add(leftKey);
         Map<String, Set> fieldLocalMap = new HashMap<String, Set>();
         fieldLocalMap.put(localKey, localKeys);
         fieldLocalMap.put(fieldKey, new HashSet<ObjectFieldPair>());
         aliasedFieldStore.add(fieldLocalMap);
         return;
      }
      for (Map<String, Set> fieldLocalMap : aliasedFieldStore) {
         Set<InstanceKey> localSet = (Set<InstanceKey>)fieldLocalMap.get(localKey);
         if (localSet.contains(rightKey)) {
            localSet.add(leftKey);
            return;
         }
      }
      unknownLocals.add(leftKey);
   }

   private void addLocal(InstanceKey leftKey, ObjectFieldPair objectFieldPair) {

      for (Map<String, Set> fieldLocalMap : aliasedFieldStore) {
         Set<ObjectFieldPair> fieldSet = (Set<ObjectFieldPair>)fieldLocalMap.get(fieldKey);
         if (fieldSet.contains(objectFieldPair)) {
            Set<InstanceKey> localSet = (Set<InstanceKey>)fieldLocalMap.get(localKey);
            localSet.add(leftKey);
            return;
         }
      }

      for (Map<String, Set> fieldLocalMap : mayAliasedFieldStore) {
         Set<ObjectFieldPair> fieldSet = (Set<ObjectFieldPair>)fieldLocalMap.get(fieldKey);
         if (fieldSet.contains(objectFieldPair)) {
            Set<InstanceKey> localSet = (Set<InstanceKey>)fieldLocalMap.get(localKey);
            localSet.add(leftKey);
            return;
         }
      }

      Set<InstanceKey> localKeys = new HashSet<InstanceKey>();
      localKeys.add(leftKey);
      Set<ObjectFieldPair> fieldPairs = new HashSet<ObjectFieldPair>();
      fieldPairs.add(objectFieldPair);
      Map<String, Set> fieldLocalMap = new HashMap<String, Set>();
      fieldLocalMap.put(localKey, localKeys);
      fieldLocalMap.put(fieldKey, fieldPairs);

      if (nonAliasedFields.remove(objectFieldPair)) {
         aliasedFieldStore.add(fieldLocalMap);
         return;
      }

      if (unknownFields.remove(objectFieldPair)) {
         mayAliasedFieldStore.add(fieldLocalMap);
         return;
      }
   }

   @Override
      protected void flowThrough(Connections in, Unit dd, Connections out) {
         // Ignore constructors
         if (m.isConstructor()) {
            return;
         }

         Stmt d = (Stmt) dd;
         Value leftop, rightop;
         in.copy(out);
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
                  // Check if rightop is Local and in newOrNullLocalCollections
                  else if (rightop instanceof Local) {
                     InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     addField(objectFieldPair, rightKey);
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
                  if (rightop instanceof soot.jimple.NullConstant || rightop instanceof soot.jimple.NewExpr) {
                     addLocal(leftKey, (InstanceKey)null);
                  }
                  // Check if rightop is CastExpr
                  else if (rightop instanceof soot.jimple.CastExpr) {
                     InstanceKey rightKey = new InstanceKey((Local) ((CastExpr)rightop).getOp(), ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     addLocal(leftKey, rightKey);
                  }
                  // Check if rightop is Local and in newOrNullLocalCollections
                  else if (rightop instanceof Local) {
                     InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                           localMustAliasAnalysis, localNotMayAliasAnalysis);
                     addLocal(leftKey, rightKey);
                  }
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
                  else {
                     unknownLocals.add(leftKey);
                  }
               }
            }
         }

         if (g.getSuccsOf(d).size() == 0) {
            Iterator<Map<String, Set>> iter = aliasedFieldStore.iterator();
            while (iter.hasNext()) {
               Set<ObjectFieldPair> fieldSet = (Set<ObjectFieldPair>)iter.next().get(fieldKey);
               if (fieldSet.isEmpty()) {
                  iter.remove();
                  continue;
               }
               else if (fieldSet.size() == 1) {
                  nonAliasedFields.addAll(fieldSet);
                  iter.remove();
               }
            }

            for (Map<String, Set> fieldLocalMap : mayAliasedFieldStore) {
               Set<ObjectFieldPair> fieldSet = (Set<ObjectFieldPair>)fieldLocalMap.get(fieldKey);
               if (fieldSet.size() == 1) {
                  unknownFields.addAll(fieldSet);
               }
               else if (fieldSet.size() > 1) {
                  aliasedFieldStore.add(fieldLocalMap);
               }
            }


            if (!nonAliasedFields.isEmpty() || !aliasedFieldStore.isEmpty() || !unknownFields.isEmpty()) {
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
                  .append("aliasedFieldStore size: ")
                  .append(aliasedFieldStore.size())
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
      protected void copy(Connections source, Connections dest) {
         source.copy(dest);

      }

   @Override
      protected Connections entryInitialFlow() {
         return emptyConnection.clone();
      }

   @Override
      protected void merge(Connections in1, Connections in2, Connections out) {
         Connections inc1 = (Connections) in1, inc2 = (Connections) in2, outc = (Connections) out;
         inc1.union(inc2, outc); // Use union temporarily
      }

   @Override
      protected Connections newInitialFlow() {
         return emptyConnection.clone();
      }

}
