package com.zfang.cf;

import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalStoreUpdateListener {
   private ObjectFieldPair field;
   private InstanceKey local;
   private FieldLocalStore store;

   public FieldLocalStoreUpdateListener(ObjectFieldPair f, 
         InstanceKey l, FieldLocalStore s) {
      field = f;
      local = l;
      store = s;
   }

   public void onNonAlias() {
      if (field != null) {
         // TODO
         return;
      }

      if (local != null) {
         // TODO
         return;
      }
   }

   public void onExternal() {
      if (field != null) {
         // TODO
         return;
      }

      if (local != null) {
         // TODO
         return;
      }
   }

   public void onUnknown() {
      if (field != null) {
         // TODO
         return;
      }

      if (local != null) {
         // TODO
         return;
      }
   }
}

