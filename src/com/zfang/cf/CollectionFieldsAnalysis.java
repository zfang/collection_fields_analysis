package com.zfang.cf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.G;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.InstanceKey;
import soot.jimple.toolkits.pointer.LocalMustAliasAnalysis;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public abstract class CollectionFieldsAnalysis extends ForwardFlowAnalysis<Unit, FlowSet> {

   public static final String TAG = "CollectionFieldsAnalysis";

	protected final LocalMustAliasAnalysis localMustAliasAnalysis;
	protected final LocalMustNotAliasAnalysis localNotMayAliasAnalysis;
   protected final SootMethod m;
   protected final Body body;
   protected final ExceptionalUnitGraph g;

   protected final FieldLocalStore fieldLocalStore = new FieldLocalStore();

   public static final Map<SootMethod, CollectionVaribleState[]> parameterStates =
      new HashMap<SootMethod, CollectionVaribleState[]>();

   public static final Map<SootField, CollectionVaribleState> fieldMap = new HashMap<SootField, CollectionVaribleState>();

   private static final List<SootClass> ALL_COLLECTIONS = Scene.v().getActiveHierarchy()
      .getDirectImplementersOf(
            RefType.v("java.util.Collection").getSootClass());
   // A list that contains names of all subclasses of java.util.Collection.
   public static final Set<String> ALL_COLLECTION_NAMES = new HashSet<String>();
   {
      for (SootClass cl : ALL_COLLECTIONS) {
         ALL_COLLECTION_NAMES.add(cl.toString());
      }
      ALL_COLLECTION_NAMES.add("java.util.Collection");
      ALL_COLLECTION_NAMES.add("java.util.List");
      ALL_COLLECTION_NAMES.add("java.util.SortedSet");
      ALL_COLLECTION_NAMES.add("java.util.Set");
      // Add class Object in case of casting and clone
      ALL_COLLECTION_NAMES.add("java.lang.Object"); 
   }

   protected CollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph) {
      super(exceptionalUnitGraph);

		localMustAliasAnalysis = new LocalMustAliasAnalysis(
				exceptionalUnitGraph, true);
		localNotMayAliasAnalysis = new LocalMustNotAliasAnalysis(
				exceptionalUnitGraph);

      m = exceptionalUnitGraph.getBody().getMethod();
      body = exceptionalUnitGraph.getBody();
      g = exceptionalUnitGraph;
   }

   public static boolean isNewOrNull(Value op) {
      return (op instanceof soot.jimple.NullConstant 
            || op.getType() instanceof soot.NullType
            || op instanceof soot.jimple.NewExpr);
   }

   public ObjectFieldPair getObjectFieldPair(FieldRef fieldRef, Stmt ds) {
      InstanceKey object = 
         (fieldRef instanceof InstanceFieldRef) ?
         new InstanceKey((Local) ((InstanceFieldRef)fieldRef).getBase(), ds, m,
               localMustAliasAnalysis, localNotMayAliasAnalysis) : null;
      SootField _field = ((FieldRef)fieldRef).getField();
      return new ObjectFieldPair(object, _field);
   }

   public InstanceKey getInstanceKey(Local local, Stmt ds) {
      return new InstanceKey((Local) local, ds, m,
            localMustAliasAnalysis, localNotMayAliasAnalysis);
   }

   protected void print(String TAG, Object obj) {
      String [] tokens = obj.toString().split("\n");
      for (String token : tokens) {
         G.v().out.println(
               new StringBuilder()
               .append("[")
               .append(TAG)
               .append("] ")
               .append(token)
               .toString()
               );
      }
   }

   private void print(Object obj) {
      print(TAG, obj);
   }

   protected void analyzeExternal(Object o, ParameterRef param) {
      ObjectFieldPair field = null;
      InstanceKey local = null;

      if (o instanceof ObjectFieldPair) {
         field = (ObjectFieldPair)o;
         fieldLocalStore.removeField(field);
      }
      else if (o instanceof InstanceKey) {
         local = (InstanceKey)o;
      }

      CollectionVaribleState [] states = parameterStates.get(m);

      // If we don't know about the method
      // We assume it's external
      if (null == states) {
         fieldLocalStore.addToStore(field, local, CollectionVaribleState.EXTERNAL);
         return;
      }

      if (param.getIndex() >= states.length)
         return;

      CollectionVaribleState state = states[param.getIndex()];

      switch(state) {
         case ALIASED: 
         case EXTERNAL: 
         case UNKNOWN:
         case NONALIASED:
            fieldLocalStore.addToStore(field, local, state);
            return;
         default:
            return;
      }
   }

   protected void analyzeExternal(Stmt d, FieldLocalStoreUpdateListener listener) {
      // Take care of clone case, which returns a shallow copy
      // and we say the shallow copies are non-aliased
      InvokeExpr invoke = d.getInvokeExpr(); 
      if (invoke.getMethod().getName().equals("clone")) {
         if (invoke instanceof InstanceInvokeExpr) {
            if (ALL_COLLECTION_NAMES.contains(
                     ((InstanceInvokeExpr)invoke).getBase().getType().toString())) {
               listener.finalize();
            }
         }
      }

      Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(d);
      while (it.hasNext()) {
         Edge e = it.next();
         SootMethod targetM = (SootMethod) e.getTgt();
         listener.onAnalyzeExternal(targetM);
      }
      listener.finalize();
   }


   abstract protected void collectData(Stmt d, Stmt ds);

   abstract protected void finalProcess(Stmt d);

   @Override
      protected void flowThrough(FlowSet in, Unit dd, FlowSet out) {
         in.copy(out);

         // Ignore constructors
         if (m.isConstructor())
            return;

         Stmt d = (Stmt) dd;
         Stmt ds;

         // We need instance keys stored before the successor of current
         // statement.
         if (g.getSuccsOf(d).size() >= 1)
            ds = (Stmt) g.getSuccsOf(d).get(0);
         else
            ds = d;

         collectData(d, ds);

         if (g.getSuccsOf(d).size() == 0) {
            fieldLocalStore.finalize();
            finalProcess(d);
         }
      }

   @Override
      protected void copy(FlowSet source, FlowSet dest) {
         source.copy(dest);

      }

   @Override
      protected FlowSet entryInitialFlow() {
         return new ValueArraySparseSet();
      }

   @Override
      protected void merge(FlowSet in1, FlowSet in2, FlowSet out) {
         in1.union(in2, out); // Use union temporarily
      }

   @Override
      protected FlowSet newInitialFlow() {
         return new ValueArraySparseSet();
      }

}
