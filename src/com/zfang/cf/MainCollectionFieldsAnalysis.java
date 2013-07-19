package com.zfang.cf;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.tagkit.StringTag;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class MainCollectionFieldsAnalysis extends CollectionFieldsAnalysis {

   public static final String TAG = "MainCollectionFieldsAnalysis";


   public MainCollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph) {
      super(exceptionalUnitGraph);

      doAnalysis();
   }

   public static void print(Object obj) {
      print(TAG, obj);
   }

   @Override
      protected void collectData(Stmt d, Stmt ds) {
         if (d instanceof DefinitionStmt) {
            Value leftop = ((DefinitionStmt) d).getLeftOp(),
                  rightop = ((DefinitionStmt) d).getRightOp();

            if (isAssignedToCloneMethod(leftop, rightop, ds)) {
               return;
            }

            if (!ALL_COLLECTION_NAMES.contains(leftop.getType().toString())) {
               return;
            }
            // print(String.format("%s = %s; rightop class: %s",
            //          leftop.toString(), rightop.toString(), rightop.getClass().getName()));
            // Field references
            if (leftop instanceof FieldRef) {
               ObjectFieldPair objectFieldPair = getObjectFieldPair((FieldRef)leftop, ds);
               // Check if rightop is NullConstant or NewExpr
               if (isNewOrNull(rightop)) {
                  fieldLocalStore.addField(objectFieldPair, null);
               }
               // Check if rightop is CastExpr
               else if (rightop instanceof CastExpr) {
                  fieldLocalStore.addField(objectFieldPair, getInstanceKey((Local)rightop, ds));
               }
               // Check if rightop is Local
               else if (rightop instanceof Local) {
                  fieldLocalStore.addField(objectFieldPair, getInstanceKey((Local)rightop, ds));
               }
               else if (rightop instanceof ParameterRef) {
                  analyzeExternal(objectFieldPair, (ParameterRef)rightop);
               }
               else if (rightop instanceof InvokeExpr) {
                  analyzeExternal(d, new FieldLocalStoreUpdateListener(objectFieldPair, fieldLocalStore));
               }
               else {
                  fieldLocalStore.addUnknown(objectFieldPair);
               }
            }
            // Local variables
            else if (leftop instanceof Local) {
               InstanceKey leftKey = getInstanceKey((Local)leftop, ds);
               // Check if rightop is NullConstant or NewExpr
               if (isNewOrNull(rightop)) {
                  fieldLocalStore.addLocal(leftKey, (InstanceKey)null);
               }
               // Check if rightop is CastExpr
               else if (rightop instanceof CastExpr) {
                  fieldLocalStore.addLocal(leftKey, getInstanceKey((Local)((CastExpr)rightop).getOp(), ds));
               }
               // Check if rightop is Local 
               else if (rightop instanceof Local) {
                  fieldLocalStore.addLocal(leftKey, getInstanceKey((Local)rightop, ds));
               }
               // Check if rightop is FieldRef 
               else if (rightop instanceof FieldRef) {
                  fieldLocalStore.addLocal(leftKey, getObjectFieldPair((FieldRef)rightop, ds));
               }
               else if (rightop instanceof ParameterRef) {
                  analyzeExternal(leftKey, (ParameterRef)rightop);
               }
               else if (rightop instanceof InvokeExpr) {
                  analyzeExternal(d, new FieldLocalStoreUpdateListener(leftKey, fieldLocalStore));
               }
               else {
                  fieldLocalStore.addUnknown(leftKey);
               }
            }
         }

         if (d.containsInvokeExpr()) {
            updateParameterTypes(d, ds);
         }

         // print(fieldLocalStore.toStringDebug());
      }

   private void updateParameterTypes(Stmt d, Stmt ds) {
      InvokeExpr invoke = d.getInvokeExpr();
      if (null == invoke)
         return;

      List<Value> args = invoke.getArgs();

      boolean hasCollectionParameter = false;
      for (Value arg : args) {
         if (ALL_COLLECTION_NAMES.contains(arg.getType().toString())) {
            hasCollectionParameter = true;
            break;
         }
      }
      if (!hasCollectionParameter)
         return;

      CollectionVaribleState [] newStates = new CollectionVaribleState[args.size()];

      Set<ObjectFieldPair> visitedFields = new HashSet<ObjectFieldPair>();
      Set<InstanceKey> visitedLocals = new HashSet<InstanceKey>();

      for (int i = 0; i < invoke.getArgCount(); ++i) {
         newStates[i] = CollectionVaribleState.NONALIASED;

         Value arg = invoke.getArg(i);
         if (!ALL_COLLECTION_NAMES.contains(arg.getType().toString()))
            continue;

         if (arg instanceof FieldRef) {
            ObjectFieldPair field = getObjectFieldPair((FieldRef)arg, ds);

            if (visitedFields.contains(field)) {
               newStates[i] = CollectionVaribleState.ALIASED;
               continue;
            }

SearchThroughFieldLocalStore:
            for (CollectionVaribleState state : CollectionVaribleState.allStates) {
               for (FieldLocalMap fieldLocalMap : fieldLocalStore.getFieldStore(state)) {
                  if (fieldLocalMap.containsField(field)) {
                     for (ObjectFieldPair visitedField : visitedFields) {
                        if (fieldLocalMap.containsField(visitedField)) {
                           newStates[i] = CollectionVaribleState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }
                     for (InstanceKey visitedLocal : visitedLocals) {
                        if (fieldLocalMap.containsLocal(visitedLocal)) {
                           newStates[i] = CollectionVaribleState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }

                     newStates[i] = state;

                     break SearchThroughFieldLocalStore;
                  }
               }
            }

            if (fieldLocalStore.isExternal(field))
               newStates[i] = CollectionVaribleState.getNewValue(newStates[i],
                     CollectionVaribleState.EXTERNAL);

            else if (fieldLocalStore.isUnknown(field))
               newStates[i] = CollectionVaribleState.getNewValue(newStates[i],
                     CollectionVaribleState.UNKNOWN);

            visitedFields.add(field);
            continue;

         }
         else if (arg instanceof Local) {
            InstanceKey local = getInstanceKey((Local)arg, ds);

            if (visitedLocals.contains(local)) {
               newStates[i] = CollectionVaribleState.ALIASED;
               continue;
            }

SearchThroughFieldLocalStore:
            for (CollectionVaribleState state : CollectionVaribleState.allStates) {
               for (FieldLocalMap fieldLocalMap : fieldLocalStore.getFieldStore(state)) {
                  if (fieldLocalMap.containsLocal(local)) {
                     for (ObjectFieldPair visitedField : visitedFields) {
                        if (fieldLocalMap.containsField(visitedField)) {
                           newStates[i] = CollectionVaribleState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }
                     for (InstanceKey visitedLocal : visitedLocals) {
                        if (fieldLocalMap.containsLocal(visitedLocal)) {
                           newStates[i] = CollectionVaribleState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }

                     break SearchThroughFieldLocalStore;
                  }
               }
            }

            if (fieldLocalStore.isExternal(local))
               newStates[i] = CollectionVaribleState.getNewValue(newStates[i],
                     CollectionVaribleState.EXTERNAL);

            if (fieldLocalStore.isUnknown(local))
               newStates[i] = CollectionVaribleState.getNewValue(newStates[i],
                     CollectionVaribleState.UNKNOWN);

            visitedLocals.add(local);
            continue;
         }
      }

      Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(d);
      while (it.hasNext()) {
         Edge e = it.next();
         SootMethod targetM = (SootMethod) e.getTgt();
         CollectionVaribleState [] states = parameterStates.get(targetM);
         if (null == states) {
            parameterStates.put(targetM, newStates);
         }
         else {
            for (int i = 0; i < states.length && i < newStates.length; ++i) {
               states[i] = CollectionVaribleState.getNewValue(states[i], newStates[i]);
            }
         }
      }
   }

   @Override
      protected void finalProcess(Stmt d) {
         String result = fieldLocalStore.toString();
         if ("" == result) {
            return;
         }

         print("At Method "+m);
         print(result);
         m.addTag(new StringTag(result));
      }

}
