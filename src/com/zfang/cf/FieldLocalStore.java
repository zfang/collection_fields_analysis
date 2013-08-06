package com.zfang.cf;

import static com.zfang.cf.CollectionFieldsAnalysis.isCollectionsImmutableContainer;
import static com.zfang.cf.CollectionFieldsAnalysis.isFromJavaOrSunPackage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.SootField;
import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalStore implements Cloneable {

   private final List<ObjectFieldPair> immutableFields = new ArrayList<ObjectFieldPair>(),
           externalFields = new ArrayList<ObjectFieldPair>(),
           unknownFields = new ArrayList<ObjectFieldPair>(),
           nullFields = new ArrayList<ObjectFieldPair>(),
           nonaliasedFields = new ArrayList<ObjectFieldPair>(); 

   private final List<FieldLocalMap> [] mayAliasedFieldStore = 
      (List<FieldLocalMap>[]) new List[CollectionVariableState.values().length];

   private final List<InstanceKey> externalLocals = new ArrayList<InstanceKey>(),
           unknownLocals = new ArrayList<InstanceKey>();

   private final Set<FieldLocalMap> aliasedFieldStore = new LinkedHashSet<FieldLocalMap>();

   public FieldLocalStore() {
      for (int i = 0, size = mayAliasedFieldStore.length; i < size; ++i) {
         mayAliasedFieldStore[i] = new ArrayList<FieldLocalMap>();
      }
   }

   public List<FieldLocalMap> getFieldStore(CollectionVariableState state) {
      return null == state ? null : mayAliasedFieldStore[state.ordinal()];
   }

   public List<ObjectFieldPair> getFields(CollectionVariableState state) {
      switch (state) {
         case IMMUTABLE:
            return immutableFields;
         case EXTERNAL:
            return externalFields;
         case UNKNOWN:
            return unknownFields;
         case NULL:
            return nullFields;
         case NONALIASED:
            return nonaliasedFields;
         default:
            return null;
      }
   }

   public List<InstanceKey> getLocals(CollectionVariableState state) {
      switch (state) {
         case EXTERNAL:
            return externalLocals;
         case UNKNOWN:
            return unknownLocals;
         default:
            return null;
      }
   }

   public boolean remove(ObjectFieldPair objectFieldPair) {
      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<ObjectFieldPair> fields = getFields(state);
         if (null == fields)
            continue;
         if (fields.remove(objectFieldPair)) {
            return true;
         }
      }
      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         Iterator<FieldLocalMap> iter = store.iterator();
         while (iter.hasNext()) {
            FieldLocalMap fieldLocalMap = iter.next();
            if (fieldLocalMap.remove(objectFieldPair)) {
               if (fieldLocalMap.isEmpty())
                  iter.remove();
               return true;
            }
         }
      }

      return false;
   }

   public boolean remove(InstanceKey localKey) {
      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<InstanceKey> locals = getLocals(state);
         if (null == locals)
            continue;
         if (locals.remove(localKey)) {
            return true;
         }
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         Iterator<FieldLocalMap> iter = store.iterator();
         while (iter.hasNext()) {
            FieldLocalMap fieldLocalMap = iter.next();
            if (fieldLocalMap.remove(localKey)) {
               if (fieldLocalMap.isEmpty())
                  iter.remove();
               return true;
            }
         }
      }
      
      return false;
   }

   public void addToStore(final ObjectFieldPair objectFieldPair, final InstanceKey local, CollectionVariableState state) {
      List<FieldLocalMap> store = getFieldStore(state);
      if ((null == objectFieldPair && null == local) || null == store) {
         return;
      }
      store.add(
            new FieldLocalMap(){{
               add(local);
               add(objectFieldPair);
      }});
   }

   public void addToStore(final InstanceKey local1,  final InstanceKey local2,  CollectionVariableState state) {
      List<FieldLocalMap> store = getFieldStore(state);
      if ((null == local1 && null == local2) || null == store) {
         return;
      }
      store.add(
            new FieldLocalMap(){{
               add(local1);
               add(local2);
      }});
   }

   public void addField(ObjectFieldPair objectFieldPair, InstanceKey rightKey) {
      // CollectionFieldsAnalysis.print("addField: objectFieldPair=> " + objectFieldPair + ", rightKey=> " + rightKey);
      if (null == objectFieldPair || null == rightKey) {
         return;
      }

      remove(objectFieldPair);

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         for (FieldLocalMap fieldLocalMap : store) {
            List<InstanceKey> localSet = fieldLocalMap.getLocals();
            if (localSet.contains(rightKey)) {
               List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
               fieldSet.add(objectFieldPair);
               return;
            }
         }
      }

      CollectionVariableState state = getState(rightKey);

      remove(rightKey);

      addToStore(objectFieldPair, rightKey, 
            state == CollectionVariableState.NOINFO ?
            CollectionVariableState.UNKNOWN : state);
   }

   public void addLocal(InstanceKey leftKey, InstanceKey rightKey) {
      // CollectionFieldsAnalysis.print("addField: leftKey=> " + leftKey + ", rightKey=> " + rightKey);
      if (null == leftKey || null == rightKey) {
         return;
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         for (FieldLocalMap fieldLocalMap : store) {
            List<InstanceKey> localSet = fieldLocalMap.getLocals();
            if (localSet.contains(rightKey)) {
               localSet.add(leftKey);
               return;
            }
         }
      }

      CollectionVariableState state = getState(rightKey);

      remove(rightKey);

      addToStore(leftKey, rightKey, state);
   }

   public void addLocal(InstanceKey leftKey, ObjectFieldPair objectFieldPair) {
      if (null == leftKey || null == objectFieldPair) {
         return;
      }

      if (isCollectionsImmutableContainer(objectFieldPair.getField())) {
         addLocal(leftKey, CollectionVariableState.IMMUTABLE);
         return;
      }


      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         for (FieldLocalMap fieldLocalMap : store) {
            List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
            if (fieldSet.contains(objectFieldPair)) {
               List<InstanceKey> localSet = fieldLocalMap.getLocals();
               localSet.add(leftKey);
               return;
            }
         }
      }
      
      CollectionVariableState state = getState(objectFieldPair);

      remove(objectFieldPair);
      
      addToStore(objectFieldPair, leftKey, state);
   }

   public void addAliased(ObjectFieldPair field, InstanceKey local, CollectionVariableState state) {
      if (null == field && null == local) {
         return;
      }

      List<FieldLocalMap> store = getFieldStore(state);
      if (null == store)
         return;
      for (FieldLocalMap fieldLocalMap : store) {
         List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
         List<InstanceKey> localSet = fieldLocalMap.getLocals();
         if (null != local && localSet.contains(local)) {
            fieldLocalMap.add(field);
            return;
         }
         if (null != field && fieldSet.contains(field)) {
            fieldLocalMap.add(local);
            return;
         }
      }

      addToStore(field, local, state);
   }

   public void addField(ObjectFieldPair field, CollectionVariableState state) {
      remove(field);
      addToStore(field, null, state);
   }
      
   public void addLocal(InstanceKey local, CollectionVariableState state) {
      remove(local);
      addToStore(local, null, state);
   }

   public CollectionVariableState getState(ObjectFieldPair field) {
      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         for (FieldLocalMap fieldLocalMap : store) {
            if (!fieldLocalMap.contains(field)) {
               continue;
            }
            if (state == CollectionVariableState.ALIASED
                  || state == CollectionVariableState.IMMUTABLE
                  || state == CollectionVariableState.NULL) {
               return state;
            }
            else {
               return fieldLocalMap.getFields().size() <= 1 ? state: CollectionVariableState.ALIASED;
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<ObjectFieldPair> fields = getFields(state);
         if (null == fields)
            continue;
         if (fields.contains(field)) {
            return state;
         }
      }

      // HACK: last resort for branches
      return getState(field.getField());
   }

   public CollectionVariableState getState(SootField field) {
      CollectionVariableState finalState = CollectionVariableState.lastValue();

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         for (FieldLocalMap fieldLocalMap : store) {
            if (!fieldLocalMap.contains(field)) {
               continue;
            }
            if (state == CollectionVariableState.ALIASED
                  || state == CollectionVariableState.IMMUTABLE
                  || state == CollectionVariableState.NULL) {
               finalState = CollectionVariableState.getNewValue(finalState,
                     state);
            }
            else {
               finalState = CollectionVariableState.getNewValue(finalState,
                     fieldLocalMap.getFields().size() <= 1 ? state: CollectionVariableState.ALIASED);
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<ObjectFieldPair> fields = getFields(state);
         if (null == fields)
            continue;
         for (ObjectFieldPair f : fields) {
            if (f.getField().equals(field)) 
               finalState = CollectionVariableState.getNewValue(finalState, state);
         }
      }

      return finalState;
   }

   public CollectionVariableState getState(InstanceKey local) {
      // CollectionFieldsAnalysis.print("getState: local => " + local);
      // CollectionFieldsAnalysis.print(toStringDebug());

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         for (FieldLocalMap fieldLocalMap : store) {
            if (!fieldLocalMap.contains(local)) {
               continue;
            }
            if (state == CollectionVariableState.ALIASED
                  || state == CollectionVariableState.IMMUTABLE
                  || state == CollectionVariableState.NULL) {
               return state;
            }
            else {
               return fieldLocalMap.getFields().size() <= 1 ? state: CollectionVariableState.ALIASED;
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<InstanceKey> locals = getLocals(state);
         if (null == locals)
            continue;
         if (locals.contains(local)) {
            return state;
         }
      }

      // HACK: last resort for branches
      return getState(local.getLocal());
   }

   public CollectionVariableState getState(Local local) {
      CollectionVariableState finalState = CollectionVariableState.lastValue();

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         for (FieldLocalMap fieldLocalMap : store) {
            if (!fieldLocalMap.contains(local)) {
               continue;
            }
            if (state == CollectionVariableState.ALIASED
                  || state == CollectionVariableState.IMMUTABLE
                  || state == CollectionVariableState.NULL) {
               finalState = CollectionVariableState.getNewValue(finalState,
                     state);
            }
            else {
               finalState = CollectionVariableState.getNewValue(finalState,
                     fieldLocalMap.getFields().size() <= 1 ? state: CollectionVariableState.ALIASED);
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<InstanceKey> locals = getLocals(state);
         if (null == locals)
            continue;
         for (InstanceKey l : locals) {
            if (l.getLocal().equals(local))
               finalState = CollectionVariableState.getNewValue(finalState, state);
         }
      }

      return finalState;
   }

   public void finalize() {
      for (CollectionVariableState state : CollectionVariableState.values()) {
         populateFinalAliasedFieldStore(state);
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         cleanup(state);
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         updateFieldMap(state);
      }
   }

   public void populateFinalAliasedFieldStore(CollectionVariableState state) {
      List<ObjectFieldPair> fields = null;
      List<InstanceKey> locals = null;

      switch (state) {
         case ALIASED: 
            {
               for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
                  if (!fieldLocalMap.getFields().isEmpty()) {
                     aliasedFieldStore.add(fieldLocalMap);
                  }
               }
               return;
            }
         default:
            fields = getFields(state);
            locals = getLocals(state);
            break;
      }

      List<FieldLocalMap> store = getFieldStore(state);
      if (null == store)
         return;
      for (FieldLocalMap fieldLocalMap : store) {
         List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
         if (fieldSet.size() == 1 
               // We don't consider immutable and null fields as aliased
               || state == CollectionVariableState.IMMUTABLE
               || state == CollectionVariableState.NULL) {
            if (fields != null)
               fields.addAll(fieldSet);
            if (locals != null)
               locals.addAll(fieldLocalMap.getLocals());
         }
         else if (fieldSet.size() > 1) {
            aliasedFieldStore.add(fieldLocalMap);
         }
      }
   }

   public void cleanup(CollectionVariableState state) {
      List<List<ObjectFieldPair>> fieldList = new ArrayList<List<ObjectFieldPair>>();
      switch (state) {
         case ALIASED: 
               for (FieldLocalMap fieldLocalMap : aliasedFieldStore) {
                  fieldList.add(fieldLocalMap.getFields());
               }
               break;
         default:
               {
                  List<ObjectFieldPair> fields = getFields(state);
                  if (null != fields)
                     fieldList.add(fields);
                  break;
               }
      }

      for (List<ObjectFieldPair> fields : fieldList) {
         Iterator<ObjectFieldPair> iter = fields.iterator();
         while (iter.hasNext()) {
            if (isFromJavaOrSunPackage(iter.next().getField())) {
               iter.remove();
               break;
            }
         }
      }

      Iterator<FieldLocalMap> iter = null;
      switch (state) {
         case ALIASED: 
            iter = aliasedFieldStore.iterator();
            break;
         default:
            break;
      }

      if (iter != null) {
         while (iter.hasNext()) {
            if (iter.next().getFields().isEmpty()) {
               iter.remove();
            }
         }
      }
   }
   public void updateFieldMap(CollectionVariableState state) {
      List<List<ObjectFieldPair>> fieldList = new ArrayList<List<ObjectFieldPair>>();
      switch (state) {
         case ALIASED: 
               for (FieldLocalMap fieldLocalMap : aliasedFieldStore) {
                  fieldList.add(fieldLocalMap.getFields());
               }
               break;
         default:
               {
                  List<ObjectFieldPair> fields = getFields(state);
                  if (null != fields)
                     fieldList.add(fields);
                  break;
               }
      }

      for (List<ObjectFieldPair> fields : fieldList) {
         for (ObjectFieldPair field : fields) {
            CollectionVariableState currentState = CollectionFieldsAnalysis.fieldMap.get(field.getField());
            CollectionFieldsAnalysis.fieldMap.put(field.getField(), 
                  currentState == null ? state : CollectionVariableState.getNewValue(currentState, state));
         }
      }
   }

   public String toStringDebug() {
      StringBuilder builder =  new StringBuilder();

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);

         if (null == store)
            continue;

         if (store.isEmpty())
            continue;

         builder
            .append(state.name())
            .append(" store: ")
            .append(store)
            .append("\n");
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<ObjectFieldPair> fields = getFields(state);
         if (null == fields)
            continue;

         if (fields.isEmpty())
            continue;

         builder
            .append(state.name())
            .append(" fields: ")
            .append(fields)
            .append("\n");
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<InstanceKey> locals = getLocals(state);
         if (null == locals)
            continue;

         if (locals.isEmpty())
            continue;

         builder
            .append(state.name())
            .append(" locals: ")
            .append(locals)
            .append("\n");
      }

      return builder.toString();
   }

   public String toString() {
      boolean empty = true;
      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<ObjectFieldPair> fields = getFields(state);
         if (null == fields)
            continue;

         if (!fields.isEmpty()) {
            empty = false;
            break;
         }
      }

      empty = empty && aliasedFieldStore.isEmpty();

      if (empty)
         return "";

      StringBuilder builder = new StringBuilder();

      int finalAliasedFieldsCount = 0;
      for (FieldLocalMap fieldLocalMap : aliasedFieldStore) {
         finalAliasedFieldsCount += fieldLocalMap.getFields().size();
      }

      if (finalAliasedFieldsCount > 0) {
         builder
            .append(CollectionVariableState.ALIASED.name())
            .append(" fields: ")
            .append(aliasedFieldStore)
            .append("\n")
            .append(CollectionVariableState.ALIASED.name())
            .append(" fields size: ")
            .append(finalAliasedFieldsCount)
            .append("\n");
      }

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<ObjectFieldPair> fields = getFields(state);
         if (null == fields)
            continue;

         if (fields.isEmpty())
            continue;

         builder
            .append(state.name())
            .append(" fields: ")
            .append(fields)
            .append("\n")
            .append(state.name())
            .append(" fields size: ")
            .append(fields.size())
            .append("\n");
      }

      return builder.toString();
   }

   public FieldLocalStore clone() {
      FieldLocalStore storeClone = new FieldLocalStore();

      for (CollectionVariableState state : CollectionVariableState.values()) {
         List<FieldLocalMap> store = getFieldStore(state);
         if (null == store)
            continue;
         List<FieldLocalMap> newStore = storeClone.getFieldStore(state);
         for (FieldLocalMap fieldLocalMap : store) {
            newStore.add(fieldLocalMap.clone());
         }

         List<ObjectFieldPair> fields = getFields(state);
         if (null == fields)
            continue;
         for (ObjectFieldPair field : fields) {
            storeClone.addField(field, state);
         }

         List<InstanceKey> locals = getLocals(state);
         if (null == locals)
            continue;
         for (InstanceKey local : locals) {
            storeClone.addLocal(local, state);
         }
      }

      return storeClone;
   }
}

