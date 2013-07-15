package com.zfang.cf;

import java.util.Iterator;

import soot.Local;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ExternalCollectionFieldsAnalysis extends CollectionFieldsAnalysis {

   public static final String TAG = "ExternalCollectionFieldsAnalysis";

   private FieldLocalStoreUpdateListener listener;

   public ExternalCollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph, 
         FieldLocalStoreUpdateListener listener) {
      super(exceptionalUnitGraph);

      this.listener = listener;

      doAnalysis();
   }

   @Override
      protected void analyzeExternal(Object o, ParameterRef param) {
         // TODO
      }

   @Override
      protected void analyzeExternal(Object o, Stmt d) {
      Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(d);
		while (it.hasNext()) {
			Edge e = it.next();
			SootMethod targetM = (SootMethod) e.getTgt();
         listener.onAnalyzeExternal(targetM);
      }
   }

   public void print(Object obj) {
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
            // print(TAG, String.format("%s = %s; rightop class: %s",
            //          leftop.toString(), rightop.toString(), rightop.getClass().getName()));
            // Local variables
            if (leftop instanceof Local) {
               InstanceKey leftKey = new InstanceKey((Local) leftop, ds, m,
                     localMustAliasAnalysis, localNotMayAliasAnalysis);
               // Check if rightop is NullConstant or NewExpr
               if (isNewOrNull(rightop)) {
                  fieldLocalStore.addLocal(leftKey, (InstanceKey)null);
               }
               // Check if rightop is CastExpr
               else if (rightop instanceof CastExpr) {
                  InstanceKey rightKey = new InstanceKey((Local) ((CastExpr)rightop).getOp(), ds, m,
                        localMustAliasAnalysis, localNotMayAliasAnalysis);
                  fieldLocalStore.addLocal(leftKey, rightKey);
               }
               // Check if rightop is Local 
               else if (rightop instanceof Local) {
                  InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                        localMustAliasAnalysis, localNotMayAliasAnalysis);
                  fieldLocalStore.addLocal(leftKey, rightKey);
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
                  fieldLocalStore.addLocal(leftKey, objectFieldPair);
               }
               else if (rightop instanceof ParameterRef) {
                  // TODO
                  fieldLocalStore.addExternal(leftKey);
               }
               else if (rightop instanceof InvokeExpr) {
                  //fieldLocalStore.addExternal(leftKey);
                  analyzeExternal(leftKey, d);
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

         if (fieldLocalStore.isAliased(opKey) || fieldLocalStore.isExternal(opKey)) {
            listener.onExternal();
         }
         else if (fieldLocalStore.isUnknown(opKey)) {
            listener.onUnknown();
         }
      }
}
