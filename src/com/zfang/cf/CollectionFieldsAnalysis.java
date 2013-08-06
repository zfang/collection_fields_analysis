package com.zfang.cf;

import java.util.Collections;
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
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
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

   class MyLocalMustAliasAnalysis extends LocalMustAliasAnalysis {
      public MyLocalMustAliasAnalysis(ExceptionalUnitGraph g, boolean tryTrackFieldAssignments) {
         super(g, tryTrackFieldAssignments);
      }
      public Set<Value> getLocalsAndFieldRefs() {
         return localsAndFieldRefs;
      }
   }

   class MyLocalMustNotAliasAnalysis extends LocalMustNotAliasAnalysis {
      public MyLocalMustNotAliasAnalysis(ExceptionalUnitGraph g) {
         super(g);
      }
      public Set<Local> getLocals() {
         return locals;
      }
   }

   public static final String TAG = "CollectionFieldsAnalysis";

   protected final MyLocalMustAliasAnalysis localMustAliasAnalysis;
   protected final MyLocalMustNotAliasAnalysis localNotMayAliasAnalysis;
   protected final SootMethod m;
   protected final Body body;
   protected final ExceptionalUnitGraph g;

   protected final FieldLocalStore fieldLocalStore = new FieldLocalStore();

   public static final Map<SootMethod, CollectionVariableState[]> parameterStates =
      new HashMap<SootMethod, CollectionVariableState[]>();

   public static final Map<SootField, CollectionVariableState> fieldMap = new LinkedHashMap<SootField, CollectionVariableState>();

   public static final Map<SootMethod, FieldLocalStore> fieldLocalStoreMap = new LinkedHashMap<SootMethod, FieldLocalStore>();

   @SuppressWarnings("unchecked")
      private static final List<SootClass> ALL_COLLECTIONS = Scene.v().getActiveHierarchy()
      .getDirectImplementersOf(
            RefType.v("java.util.Collection").getSootClass());
   // A list that contains names of all subclasses of java.util.Collection.
   public static final Set<String> ALL_COLLECTION_NAMES = new HashSet<String>()
   {{
       for (SootClass cl : ALL_COLLECTIONS) {
          add(cl.toString());
       }
       add("java.util.Collection");
       add("java.util.List");
       add("java.util.SortedSet");
       add("java.util.Set");
    }};

   public static final Set<String> ALL_COLLECTION_DIRECT_IMPLEMENTER_NAMES = new HashSet<String>()
   {{
       for (SootClass cl : ALL_COLLECTIONS) {
          add(cl.toString());
       }
    }};

   protected CollectionFieldsAnalysis(ExceptionalUnitGraph exceptionalUnitGraph) {
      super(exceptionalUnitGraph);

      localMustAliasAnalysis = new MyLocalMustAliasAnalysis(
            exceptionalUnitGraph, true);
      localNotMayAliasAnalysis = new MyLocalMustNotAliasAnalysis(
            exceptionalUnitGraph);

      m = exceptionalUnitGraph.getBody().getMethod();
      body = exceptionalUnitGraph.getBody();
      g = exceptionalUnitGraph;
   }

   static boolean isFromJavaOrSunPackage(SootMethod method) {
      return isFromJavaOrSunPackage(method.getDeclaringClass());
   }

   static boolean isFromJavaOrSunPackage(SootField field) {
      return isFromJavaOrSunPackage(field.getDeclaringClass());
   }

   static boolean isFromJavaOrSunPackage(SootClass declaringClass) {
      return declaringClass.isJavaLibraryClass()
         || declaringClass.isLibraryClass();
   }

   private static Set<String> CollectionsStaticImmutableFieldNames = new HashSet<String>() {{
      add("EMPTY_LIST");
      add("EMPTY_MAP");
      add("EMPTY_SET");
   }};

   static boolean isCollectionsImmutableContainer(SootField field) {
      return field.getDeclaringClass().getName().equals("java.util.Collections")
         && field.isStatic()
         && CollectionsStaticImmutableFieldNames.contains(field.getName());
   }

   public static boolean isNew(Value op) {
      return op instanceof soot.jimple.NewExpr;
   }

   public static boolean isNull(Value op) {
      return (op instanceof soot.jimple.NullConstant 
            || op.getType() instanceof soot.NullType);
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

   public static Map<CollectionVariableState, Set<SootField>> getReverseFieldMap() {
      Map<CollectionVariableState, Set<SootField>> reverseFieldMap = new LinkedHashMap<CollectionVariableState, Set<SootField>>();
      for (CollectionVariableState state : CollectionVariableState.values()) {
         if (state == CollectionVariableState.NOINFO)
            continue;

         reverseFieldMap.put(state, new LinkedHashSet<SootField>());
      }

      for (Map.Entry<SootField, CollectionVariableState> entry : fieldMap.entrySet()) {
         reverseFieldMap.get(entry.getValue()).add(entry.getKey());
      }

      return reverseFieldMap;
   }

   public static void printReverseFieldMap() {
      Map<CollectionVariableState, Set<SootField>> reverseFieldMap = getReverseFieldMap();
      StringBuilder stringBuilder = new StringBuilder();
      for (Map.Entry<CollectionVariableState, Set<SootField>> entry : reverseFieldMap.entrySet()) {
         if (entry.getValue().isEmpty())
            continue;

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
         fieldLocalStore.remove(field);
      }
      else if (o instanceof InstanceKey) {
         local = (InstanceKey)o;
      }

      CollectionVariableState [] states = parameterStates.get(m);

      if (null == states) {
         fieldLocalStore.addToStore(field, local, CollectionVariableState.NOINFO);
         return;
      }

      if (param.getIndex() >= states.length)
         return;

      CollectionVariableState state = states[param.getIndex()];

      fieldLocalStore.addToStore(field, local, state);
   }

   protected void analyzeExternal(Stmt d, FieldLocalStoreUpdateListener listener) {
      SootMethod method = d.getInvokeExpr().getMethod();

      if (method.isStatic() && method.getDeclaringClass().getName().equals("java.util.Collections")) {
         listener.onStateChange(CollectionVariableState.NONALIASED);
         listener.finalize();
         return;
      }

      if (isFromJavaOrSunPackage(method)) {
         listener.onStateChange(CollectionVariableState.EXTERNAL);
         listener.finalize();
         return;
      }

      Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(d);
      while (it.hasNext()) {
         Edge e = it.next();
         SootMethod targetM = (SootMethod) e.getTgt();
         listener.onAnalyzeExternal(targetM);
      }
      listener.finalize();
   }

   protected boolean isAssignedToCloneMethod(Value leftop, Value rightop, Stmt ds) {
      if (rightop instanceof InvokeExpr) {
         SootMethod method = ((InvokeExpr)rightop).getMethod();
         if (ALL_COLLECTION_DIRECT_IMPLEMENTER_NAMES.contains(method.getDeclaringClass().getName())
               && method.getName().equals("clone")) {
            if (leftop instanceof FieldRef) {
               fieldLocalStore.addField(getObjectFieldPair((FieldRef)leftop, ds), 
                     CollectionVariableState.NONALIASED);
               return true;
            }
            else if (leftop instanceof Local) {
               fieldLocalStore.addLocal(getInstanceKey((Local)leftop, ds), CollectionVariableState.NONALIASED);
               return true;
            }
               }
      }

      return false;
   }

   protected FieldLocalStoreUpdateListener getListener(Object o) {
      return new FieldLocalStoreUpdateListener(o, fieldLocalStore);
   }

   protected void processFieldRef(Value leftop, Value rightop, Stmt d, Stmt ds) {
      ObjectFieldPair objectFieldPair = getObjectFieldPair((FieldRef)leftop, ds);
      // Check if rightop is NewExpr
      if (isNew(rightop)) {
         fieldLocalStore.addField(objectFieldPair, CollectionVariableState.NONALIASED);
      }
      // Check if rightop is Null
      else if (isNull(rightop)) {
         fieldLocalStore.addField(objectFieldPair, CollectionVariableState.NULL);
      }
      // Check if rightop is CastExpr
      else if (rightop instanceof CastExpr) {
         fieldLocalStore.addField(objectFieldPair, getInstanceKey((Local)rightop, ds));
      }
      // Check if rightop is Local
      else if (rightop instanceof Local) {
         fieldLocalStore.addField(objectFieldPair, getInstanceKey((Local)rightop, ds));
      }
      else if (rightop instanceof ParameterRef) {
         analyzeExternal(objectFieldPair, (ParameterRef)rightop);
      }
      else if (rightop instanceof InvokeExpr) {
         analyzeExternal(d, getListener(objectFieldPair));
      }
      else {
         fieldLocalStore.addField(objectFieldPair, CollectionVariableState.UNKNOWN);
      }
   }

   protected void processLocal(Value leftop, Value rightop, Stmt d, Stmt ds) {
      InstanceKey leftKey = getInstanceKey((Local)leftop, ds);
      // Check if rightop is NewExpr
      if (isNew(rightop)) {
         fieldLocalStore.addLocal(leftKey, CollectionVariableState.NONALIASED);
      }
      // Check if rightop is Null
      else if (isNull(rightop)) {
         fieldLocalStore.addLocal(leftKey, CollectionVariableState.NULL);
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
         analyzeExternal(d, getListener(leftKey));
      }
      else {
         fieldLocalStore.addLocal(leftKey, CollectionVariableState.UNKNOWN);
      }
   }

   protected void collectData(Stmt d, Stmt ds) {
      // print(d);
      // print(fieldLocalStore.toStringDebug());
      // for (Value value : localMustAliasAnalysis.getLocalsAndFieldRefs()) {
      //    if (value instanceof Local)
      //       print(value + ": " + localMustAliasAnalysis.instanceKeyString((Local)value, d));
      // }
      // print(localNotMayAliasAnalysis.getLocals());
      if (d instanceof DefinitionStmt) {
         Value leftop = ((DefinitionStmt) d).getLeftOp(),
               rightop = ((DefinitionStmt) d).getRightOp();

         if (isAssignedToCloneMethod(leftop, rightop, ds)) {
            return;
         }

         if (!ALL_COLLECTION_NAMES.contains(leftop.getType().toString())) {
            return;
         }
         // print(String.format("%s = %s; rightop class: %s",
         //          leftop.toString(), rightop.toString(), rightop.getClass().getName()));
         // Field references
         if (leftop instanceof FieldRef) {
            processFieldRef(leftop, rightop, d, ds);
         }
         // Local variables
         else if (leftop instanceof Local) {
            processLocal(leftop, rightop, d, ds);
         }
      }

   }

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
