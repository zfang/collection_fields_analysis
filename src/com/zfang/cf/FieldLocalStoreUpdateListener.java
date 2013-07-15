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

   private boolean isExternal;
   private boolean isUnknown;

   private Set<SootMethod> methods = new HashSet<SootMethod>();

   public FieldLocalStoreUpdateListener(Object o, FieldLocalStore s) {
      obj = o;
      store = s;
      isExternal = false;
      isUnknown = false;
   }

   public void onExternal() {
      isExternal = true;
   }

   public void onUnknown() {
      isUnknown = true;
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
         if (isExternal) {
            store.addExternal(field);
         }
         else if (isUnknown) {
            store.addUnknown(field);
         }
         else {
            // non-aliased
            store.addField(field, (InstanceKey)null);
         }
         return;
      }
      if (obj instanceof InstanceKey) {
         InstanceKey local = (InstanceKey)obj;
         if (isExternal) {
            store.addExternal(local);
         }
         else if (isUnknown) {
            store.addUnknown(local);
         }
         else {
            // non-aliased
            store.addLocal(local, (InstanceKey)null);
         }
         return;
      }
   }
}

