package com.zfang.cf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

   public static final Map<SootField, CollectionVaribleState> fieldMap = new LinkedHashMap<SootField, CollectionVaribleState>();

   @SuppressWarnings("unchecked")
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

   public static void print(String TAG, Object obj) {
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

   public static void print(Object obj) {
      print("Base" + TAG, obj);
   }

   public static Map<CollectionVaribleState, Set<SootField>> getReverseFieldMap() {
      Map<CollectionVaribleState, Set<SootField>> reverseFieldMap = new LinkedHashMap<CollectionVaribleState, Set<SootField>>();
      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
         reverseFieldMap.put(state, new LinkedHashSet<SootField>());
      }

      for (Map.Entry<SootField, CollectionVaribleState> entry : fieldMap.entrySet()) {
         reverseFieldMap.get(entry.getValue()).add(entry.getKey());
      }

      return reverseFieldMap;
   }

   public static void printReverseFieldMap() {
      Map<CollectionVaribleState, Set<SootField>> reverseFieldMap = getReverseFieldMap();
      StringBuilder stringBuilder = new StringBuilder();
      for (Map.Entry<CollectionVaribleState, Set<SootField>> entry : reverseFieldMap.entrySet()) {
         stringBuilder
            .append(entry.getKey().name())
            .append(": ")
            .append(entry.getValue())
            .append("\n")
            .append(entry.getKey().name())
            .append(" size: ")
            .append(entry.getValue().size())
            .append("\n")
            ;
      }
      print(stringBuilder.toString());
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

      if (null == states) {
         fieldLocalStore.addToStore(field, local, CollectionVaribleState.UNKNOWN);
         return;
      }

      if (param.getIndex() >= states.length)
         return;

      CollectionVaribleState state = states[param.getIndex()];

      switch(state) {
         case ALIASED: 
         case UNKNOWN:
         case NONALIASED:
            fieldLocalStore.addToStore(field, local, state);
            return;
         default:
            return;
      }
   }

   protected void analyzeExternal(Stmt d, FieldLocalStoreUpdateListener listener) {
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
