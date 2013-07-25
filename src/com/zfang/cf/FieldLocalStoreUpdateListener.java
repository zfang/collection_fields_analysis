package com.zfang.cf;

import java.util.HashSet;
import java.util.Set;

import soot.Body;
import soot.SootMethod;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class FieldLocalStoreUpdateListener {
   private final Object obj;
   private final FieldLocalStore store;

   private CollectionVariableState state = CollectionVariableState.lastValue();

   private final Set<SootMethod> methods = new HashSet<SootMethod>();

   public FieldLocalStoreUpdateListener(Object o, FieldLocalStore s) {
      obj = o;
      store = s;
   }

   public void onStateChange(CollectionVariableState newState) {
      // CollectionFieldsAnalysis.print("onStateChange: newState => " + newState.name());
      state = CollectionVariableState.getNewValue(state, newState);
   }

   public void onAnalyzeExternal(SootMethod m) {
      if (!m.hasActiveBody()) {
         return;
      }

      if (methods.contains(m)) {
         return;
      }

      methods.add(m);

      Body body = m.getActiveBody();
      new ExternalCollectionFieldsAnalysis(new ExceptionalUnitGraph(body), this);
   }

   public void finalize() {
      // CollectionFieldsAnalysis.print("finalize: state => " + state.name());
      if (obj instanceof ObjectFieldPair) {
         ObjectFieldPair field = (ObjectFieldPair)obj;
         switch (state) {
            case ALIASED_IMMUTABLE:
            case ALIASED:
               store.addAliased(field, null, state);
               break;
            default:
               store.addField(field, state);
               break;
         }
         return;
      }
      if (obj instanceof InstanceKey) {
         InstanceKey local = (InstanceKey)obj;
         switch (state) {
            case ALIASED_IMMUTABLE:
            case ALIASED:
               store.addAliased(null, local, state);
               break;
            default:
               store.addLocal(local, state);
               break;
         }
         return;
      }
   }
}

