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

   @Override
      protected void analyzeExternal(Object o, ParameterRef param) {
         // TODO
      }

   @Override
      protected void analyzeExternal(Object o, Stmt d) {
         Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(d);
         FieldLocalStoreUpdateListener listener = new FieldLocalStoreUpdateListener(o, fieldLocalStore);
         while (it.hasNext()) {
            Edge e = it.next();
            SootMethod targetM = (SootMethod) e.getTgt();
            listener.onAnalyzeExternal(targetM);
         }
         listener.finalize();
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
            // Field references
            if (leftop instanceof FieldRef) {
               InstanceKey leftopObject = 
                  (leftop instanceof InstanceFieldRef) ?
                  new InstanceKey((Local) ((InstanceFieldRef)leftop).getBase(), ds, m,
                        localMustAliasAnalysis, localNotMayAliasAnalysis) : null;
               SootField leftopField = ((FieldRef)leftop).getField();
               ObjectFieldPair objectFieldPair = new ObjectFieldPair(leftopObject, leftopField);
               // Check if rightop is NullConstant or NewExpr
               if (isNewOrNull(rightop)) {
                  fieldLocalStore.addField(objectFieldPair, null);
               }
               // Check if rightop is CastExpr
               else if (rightop instanceof CastExpr) {
                  InstanceKey rightKey = new InstanceKey((Local) ((CastExpr)rightop).getOp(), ds, m,
                        localMustAliasAnalysis, localNotMayAliasAnalysis);
                  fieldLocalStore.addField(objectFieldPair, rightKey);
               }
               // Check if rightop is Local
               else if (rightop instanceof Local) {
                  InstanceKey rightKey = new InstanceKey((Local) rightop, ds, m,
                        localMustAliasAnalysis, localNotMayAliasAnalysis);
                  fieldLocalStore.addField(objectFieldPair, rightKey);
               }
               else if (rightop instanceof ParameterRef) {
                  // TODO
                  fieldLocalStore.addExternal(objectFieldPair);
               }
               else if (rightop instanceof InvokeExpr) {
                  //fieldLocalStore.addExternal(objectFieldPair);
                  analyzeExternal(objectFieldPair, d);
               }
               else {
                  fieldLocalStore.addUnknown(objectFieldPair);
               }
            }
            // Local variables
            else if (leftop instanceof Local) {
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
         String result = fieldLocalStore.toString();
         if ("" == result) {
            return;
         }

         print("At Method "+m);
         print(result);
         m.addTag(new StringTag(result));
      }

}
