package com.zfang.cf;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
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
         super.collectData(d, ds);

         if (d.containsInvokeExpr()) {
            updateParameterTypes(d, ds);
         }
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

      CollectionVariableState [] newStates = new CollectionVariableState[args.size()];

      Set<ObjectFieldPair> visitedFields = new HashSet<ObjectFieldPair>();
      Set<InstanceKey> visitedLocals = new HashSet<InstanceKey>();

      for (int i = 0; i < invoke.getArgCount(); ++i) {
         newStates[i] = CollectionVariableState.lastValue();

         Value arg = invoke.getArg(i);
         if (!ALL_COLLECTION_NAMES.contains(arg.getType().toString()))
            continue;

         if (arg instanceof FieldRef) {
            ObjectFieldPair field = getObjectFieldPair((FieldRef)arg, ds);

            if (visitedFields.contains(field)) {
               newStates[i] = CollectionVariableState.ALIASED;
               continue;
            }

SearchThroughFieldLocalStore:
            for (CollectionVariableState state : CollectionVariableState.values()) {
               for (FieldLocalMap fieldLocalMap : fieldLocalStore.getFieldStore(state)) {
                  if (fieldLocalMap.contains(field)) {
                     for (ObjectFieldPair visitedField : visitedFields) {
                        if (fieldLocalMap.contains(visitedField)) {
                           newStates[i] = CollectionVariableState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }
                     for (InstanceKey visitedLocal : visitedLocals) {
                        if (fieldLocalMap.contains(visitedLocal)) {
                           newStates[i] = CollectionVariableState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }

                     newStates[i] = state;

                     break SearchThroughFieldLocalStore;
                  }
               }
            }

            newStates[i] = CollectionVariableState.getNewValue(newStates[i],
                  fieldLocalStore.getState(field));

            visitedFields.add(field);
            continue;

         }
         else if (arg instanceof Local) {
            InstanceKey local = getInstanceKey((Local)arg, ds);

            if (visitedLocals.contains(local)) {
               newStates[i] = CollectionVariableState.ALIASED;
               continue;
            }

SearchThroughFieldLocalStore:
            for (CollectionVariableState state : CollectionVariableState.values()) {
               for (FieldLocalMap fieldLocalMap : fieldLocalStore.getFieldStore(state)) {
                  if (fieldLocalMap.contains(local)) {
                     for (ObjectFieldPair visitedField : visitedFields) {
                        if (fieldLocalMap.contains(visitedField)) {
                           newStates[i] = CollectionVariableState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }
                     for (InstanceKey visitedLocal : visitedLocals) {
                        if (fieldLocalMap.contains(visitedLocal)) {
                           newStates[i] = CollectionVariableState.ALIASED;
                           break SearchThroughFieldLocalStore;
                        }
                     }

                     break SearchThroughFieldLocalStore;
                  }
               }
            }

            newStates[i] = CollectionVariableState.getNewValue(newStates[i],
                  fieldLocalStore.getState(local));

            visitedLocals.add(local);
            continue;
         }
      }

      Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(d);
      while (it.hasNext()) {
         Edge e = it.next();
         SootMethod targetM = (SootMethod) e.getTgt();
         CollectionVariableState [] states = parameterStates.get(targetM);
         if (null == states) {
            parameterStates.put(targetM, newStates);
         }
         else {
            for (int i = 0; i < states.length && i < newStates.length; ++i) {
               states[i] = CollectionVariableState.getNewValue(states[i], newStates[i]);
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
         // print(fieldLocalStore.toStringDebug());
         m.addTag(new StringTag(result));

         fieldLocalStoreMap.put(m, fieldLocalStore);
      }

}
