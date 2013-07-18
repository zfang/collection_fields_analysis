package com.zfang.cf;

import soot.Local;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
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
      protected void collectData(Stmt d, Stmt ds) {
         if (d instanceof DefinitionStmt) {
            Value leftop = ((DefinitionStmt) d).getLeftOp(),
                  rightop = ((DefinitionStmt) d).getRightOp();

            if (!ALL_COLLECTION_NAMES.contains(leftop.getType().toString())) {
               return;
            }
            // print(String.format("%s = %s; rightop class: %s",
            //          leftop.toString(), rightop.toString(), rightop.getClass().getName()));
            // Local variables
            if (leftop instanceof Local) {
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
                  analyzeExternal(d, listener);
               }
               else {
                  fieldLocalStore.addUnknown(leftKey);
               }
            }

         }
      }

   @Override
      protected void finalProcess(Stmt d) {
         if (!(d instanceof ReturnStmt)) 
            return;

         Value op = ((ReturnStmt)d).getOp();

         if (!(op instanceof Local))
            return;

         InstanceKey opKey = new InstanceKey((Local)op, d, m,
               localMustAliasAnalysis, localNotMayAliasAnalysis);

         if (fieldLocalStore.isAliased(opKey) || fieldLocalStore.isExternal(opKey))
            listener.onExternal();
         else if (fieldLocalStore.isUnknown(opKey))
            listener.onUnknown();
      }
}
