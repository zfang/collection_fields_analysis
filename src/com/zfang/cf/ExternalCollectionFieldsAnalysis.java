package com.zfang.cf;

import soot.Local;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ExternalCollectionFieldsAnalysis extends CollectionFieldsAnalysis {

   public static final String TAG = "ExternalCollectionFieldsAnalysis";

   private final FieldLocalStoreUpdateListener listener;

   public ExternalCollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph, 
         FieldLocalStoreUpdateListener listener) {
      super(exceptionalUnitGraph);

      this.listener = listener;

      doAnalysis();
   }

   public static void print(Object obj) {
      print(TAG, obj);
   }

   @Override
      protected FieldLocalStoreUpdateListener getListener(Object o) {
         return listener;
      }


   @Override
      protected void finalProcess(Stmt d) {
         if (!(d instanceof ReturnStmt)) 
            return;

         Local op = (Local)((ReturnStmt)d).getOp();

         InstanceKey opKey = new InstanceKey(op, d, m,
               localMustAliasAnalysis, localNotMayAliasAnalysis);

         // print(opKey);

         listener.onStateChange(fieldLocalStore.getState(opKey));
      }
}
