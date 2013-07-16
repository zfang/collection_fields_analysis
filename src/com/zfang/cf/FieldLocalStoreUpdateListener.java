package com.zfang.cf;

import java.util.HashSet;
import java.util.Set;

import soot.Body;
import soot.SootMethod;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class FieldLocalStoreUpdateListener {
   private Object obj;
   private FieldLocalStore store;

   private CollectionVaribleState state;

   private Set<SootMethod> methods = new HashSet<SootMethod>();

   public FieldLocalStoreUpdateListener(Object o, FieldLocalStore s) {
      obj = o;
      store = s;
      state = CollectionVaribleState.NONALIASED;
   }

   public void onExternal() {
      state = CollectionVaribleState.getNewValue(state, CollectionVaribleState.EXTERNAL);
   }

   public void onUnknown() {
      state = CollectionVaribleState.getNewValue(state, CollectionVaribleState.UNKNOWN);
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
      if (obj instanceof ObjectFieldPair) {
         ObjectFieldPair field = (ObjectFieldPair)obj;
         switch (state) {
            case EXTERNAL:
               store.addExternal(field);
               break;
            case UNKNOWN:
               store.addUnknown(field);
               break;
            case NONALIASED:
               store.addField(field, (InstanceKey)null);
               break;
            default:
               break;
         }
         return;
      }
      if (obj instanceof InstanceKey) {
         InstanceKey local = (InstanceKey)obj;
         switch (state) {
            case EXTERNAL:
               store.addExternal(local);
               break;
            case UNKNOWN:
               store.addUnknown(local);
               break;
            case NONALIASED:
               store.addLocal(local, (InstanceKey)null);
               break;
            default:
               break;
         }
         return;
      }
   }
}

