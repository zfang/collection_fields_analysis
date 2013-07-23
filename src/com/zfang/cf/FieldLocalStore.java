package com.zfang.cf;

import static com.zfang.cf.CollectionFieldsAnalysis.isFromJavaOrSunPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalStore implements Cloneable {

   private final List<ObjectFieldPair> nonAliasedFields = new ArrayList<ObjectFieldPair>(), 
          externalFields = new ArrayList<ObjectFieldPair>(),
          unknownFields = new ArrayList<ObjectFieldPair>();

   /* 0: fields from externalFields
    * 1: fields from nonAliasedFields
    * 2: fields from unknownFields
    */
   @SuppressWarnings("unchecked")
      private final List<FieldLocalMap> [] mayAliasedFieldStore = (List<FieldLocalMap>[]) new List[3];

   private final List<InstanceKey> externalLocals = new ArrayList<InstanceKey>(),
           nonAliasedLocals = new ArrayList<InstanceKey>(),
           unknownLocals = new ArrayList<InstanceKey>();

   private final List<FieldLocalMap> aliasedFieldStore = new ArrayList<FieldLocalMap>();

   private final Set<FieldLocalMap> finalAliasedFieldStore = 
      Collections.synchronizedSet(new LinkedHashSet<FieldLocalMap>());

   public FieldLocalStore() {
      for (int i = 0, size = mayAliasedFieldStore.length; i < size; ++i) {
         mayAliasedFieldStore[i] = new ArrayList<FieldLocalMap>();
      }
   }

   public List<FieldLocalMap> getFieldStore(CollectionVariableState state) {
      return state == CollectionVariableState.ALIASED ? aliasedFieldStore :
         mayAliasedFieldStore[state.ordinal()-1];
   }

   public List<ObjectFieldPair> getFields(CollectionVariableState state) {
      switch (state) {
         case EXTERNAL:
            return externalFields;
         case UNKNOWN:
            return unknownFields;
         case NONALIASED:
            return nonAliasedFields;
         case ALIASED: 
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
         case NONALIASED:
            return nonAliasedLocals;
         case ALIASED: 
         default:
            return null;
      }
   }

   public void remove(ObjectFieldPair objectFieldPair) {
      if (!nonAliasedFields.remove(objectFieldPair) 
            && !externalFields.remove(objectFieldPair)
            && !unknownFields.remove(objectFieldPair)) {
         for (CollectionVariableState state : CollectionVariableState.allStates) {
            for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
               if (fieldLocalMap.getFields().remove(objectFieldPair)) {
                  return;
               }
            }
         }
      }
   }

   public void remove(InstanceKey localKey) {
      if (!externalLocals.remove(localKey) 
            && !unknownLocals.remove(localKey)) {
         for (CollectionVariableState state : CollectionVariableState.allStates) {
            for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
               if (fieldLocalMap.getLocals().remove(localKey)) {
                  return;
               }
            }
         }
      }
   }

   public void addToStore(final ObjectFieldPair objectFieldPair, final InstanceKey local, CollectionVariableState state) {
      List<FieldLocalMap> store = getFieldStore(state);
      if ((null == objectFieldPair && null == local) || null == store) {
         return;
      }
      store.add(
            new FieldLocalMap(){{
               addToLocals(local);
               addToFields(objectFieldPair);
      }});
   }

   public void addToStore(final InstanceKey local1,  final InstanceKey local2,  CollectionVariableState state) {
      List<FieldLocalMap> store = getFieldStore(state);
      if ((null == local1 && null == local2) || null == store) {
         return;
      }
      store.add(
            new FieldLocalMap(){{
               addToLocals(local1);
               addToLocals(local2);
      }});
   }

   public void addField(ObjectFieldPair objectFieldPair, InstanceKey rightKey) {
      remove(objectFieldPair);

      if (externalLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, CollectionVariableState.EXTERNAL);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, CollectionVariableState.UNKNOWN);
         return;
      }

      if (null == rightKey) {
         nonAliasedFields.add(objectFieldPair);
         return;
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            List<InstanceKey> localSet = fieldLocalMap.getLocals();
            if (localSet.contains(rightKey)) {
               List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
               fieldSet.add(objectFieldPair);
               return;
            }
         }
      }

      unknownFields.add(objectFieldPair);
   }

   public void addLocal(InstanceKey leftKey, InstanceKey rightKey) {
      // CollectionFieldsAnalysis.print("addField: leftKey=> " + leftKey + ", rightKey=> " + rightKey);
      if (null == rightKey) {
         addToStore(leftKey, rightKey, CollectionVariableState.NONALIASED);
         return;
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            List<InstanceKey> localSet = fieldLocalMap.getLocals();
            if (localSet.contains(rightKey)) {
               localSet.add(leftKey);
               return;
            }
         }
      }

      if (externalLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, CollectionVariableState.EXTERNAL);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, CollectionVariableState.UNKNOWN);
         return;
      }

   }

   public void addLocal(InstanceKey leftKey, ObjectFieldPair objectFieldPair) {

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
            if (fieldSet.contains(objectFieldPair)) {
               List<InstanceKey> localSet = fieldLocalMap.getLocals();
               localSet.add(leftKey);
               return;
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         if (state == CollectionVariableState.ALIASED)
            continue;
         if (getFields(state).remove(objectFieldPair)) {
            addToStore(objectFieldPair, leftKey, state);
            return;
         }
      }
      
      addToStore(objectFieldPair, leftKey, CollectionVariableState.UNKNOWN);
      return;
   }

   public void addNonAliased(ObjectFieldPair field) {
      nonAliasedFields.add(field);
   }
      
   public void addAliased(ObjectFieldPair field, InstanceKey local) {
      if (null == field && null == local) {
         return;
      }

      CollectionVariableState state = CollectionVariableState.ALIASED;

      for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
         List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
         List<InstanceKey> localSet = fieldLocalMap.getLocals();
         if (null != local && localSet.contains(local)) {
            fieldLocalMap.addToFields(field);
            return;
         }
         if (null != field && fieldSet.contains(field)) {
            fieldLocalMap.addToLocals(local);
            return;
         }
      }

      addToStore(field, local, state);
   }

   public void addExternal(ObjectFieldPair field) {
      remove(field);

      externalFields.add(field);
   }

   public void addExternal(InstanceKey local) {
      remove(local);

      externalLocals.add(local);
   }

   public void addUnknown(ObjectFieldPair field) {
      remove(field);

      unknownFields.add(field);
   }
      
   public void addUnknown(InstanceKey local) {
      remove(local);

      unknownLocals.add(local);
   }

   public CollectionVariableState getState(ObjectFieldPair field) {
      for (CollectionVariableState state : CollectionVariableState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            if (state == CollectionVariableState.ALIASED) {
               if (fieldLocalMap.containsField(field)) {
                  return CollectionVariableState.ALIASED;
               }
            }
            else {
               if (fieldLocalMap.containsField(field)) {
                  return fieldLocalMap.getFields().size() <= 1 ? state: CollectionVariableState.ALIASED;
               }
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         if (state == CollectionVariableState.ALIASED) {
            continue;
         }
         if (getFields(state).contains(field)) {
            return state;
         }
      }

      return CollectionVariableState.NOINFO;
   }

   public CollectionVariableState getState(InstanceKey local) {
      // CollectionFieldsAnalysis.print("getState: local => " + local);
      // CollectionFieldsAnalysis.print(toStringDebug());

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            if (state == CollectionVariableState.ALIASED) {
               if (fieldLocalMap.containsLocal(local)) {
                  return CollectionVariableState.ALIASED;
               }
            }
            else {
               if (fieldLocalMap.containsLocal(local)) {
                  return fieldLocalMap.getFields().size() <= 1 ? state: CollectionVariableState.ALIASED;
               }
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         if (state == CollectionVariableState.ALIASED) {
            continue;
         }
         if (getLocals(state).contains(local)) {
            return state;
         }
      }

      // HACK: last resort for branches
      return getState(local.getLocal());
   }

   public CollectionVariableState getState(Local local) {
      for (CollectionVariableState state : CollectionVariableState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            if (state == CollectionVariableState.ALIASED) {
               if (fieldLocalMap.containsLocal(local)) {
                  return CollectionVariableState.ALIASED;
               }
            }
            else {
               if (fieldLocalMap.containsLocal(local)) {
                  return fieldLocalMap.getFields().size() <= 1 ? state: CollectionVariableState.ALIASED;
               }
            }
         }
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         if (state == CollectionVariableState.ALIASED) {
            continue;
         }
         for (InstanceKey l : getLocals(state)) {
            if (l.getLocal().equals(local))
               return state;
         }
      }

      return CollectionVariableState.NOINFO;
   }

   public void setFinalAliasedFieldStore(Set<FieldLocalMap> store) {
      finalAliasedFieldStore.addAll(store);
   }

   public void finalize() {
      for (CollectionVariableState state : CollectionVariableState.allStates) {
         populateFinalAliasedFieldStore(state);
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
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
                     finalAliasedFieldStore.add(fieldLocalMap);
                  }
               }
               return;
            }
         case EXTERNAL:
         case UNKNOWN:
         case NONALIASED:
            fields = getFields(state);
            locals = getLocals(state);
            break;
         default:
            return;
      }

      for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
         List<ObjectFieldPair> fieldSet = fieldLocalMap.getFields();
         if (fieldSet.size() > 1) {
            finalAliasedFieldStore.add(fieldLocalMap);
         }
         else if (fieldSet.size() == 1) {
            if (fields != null)
               fields.addAll(fieldSet);
            if (locals != null)
               locals.addAll(fieldLocalMap.getLocals());
         }
      }
   }

   public void updateFieldMap(CollectionVariableState state) {
      List<List<ObjectFieldPair>> fieldList = new ArrayList<List<ObjectFieldPair>>();
      switch (state) {
         case ALIASED: 
               for (FieldLocalMap fieldLocalMap : finalAliasedFieldStore) {
                  fieldList.add(fieldLocalMap.getFields());
               }
               break;
         case EXTERNAL:
         case UNKNOWN:
         case NONALIASED:
            fieldList.add(getFields(state));
            break;
         default:
            return;
      }

      // cleanup
      for (List<ObjectFieldPair> fields : fieldList) {
         Iterator<ObjectFieldPair> iter = fields.iterator();
         while (iter.hasNext()) {
            if (isFromJavaOrSunPackage(iter.next().getField())) {
               iter.remove();
               break;
            }
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

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         builder
            .append(state.name())
            .append(" store: ")
            .append(getFieldStore(state))
            .append("\n");
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         if (state == CollectionVariableState.ALIASED)
            continue;

         builder
            .append(state.name())
            .append(" fields: ")
            .append(getFields(state))
            .append("\n");
      }

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         if (state == CollectionVariableState.ALIASED)
            continue;

         builder
            .append(state.name())
            .append(" locals: ")
            .append(getLocals(state))
            .append("\n");
      }

      return builder.toString();
   }

   public String toString() {
      if (nonAliasedFields.isEmpty() 
            && finalAliasedFieldStore.isEmpty() 
            && externalFields.isEmpty()
            && unknownFields.isEmpty()) {
         return "";
      }

      int finalAliasedFieldsCount = 0;
      for (FieldLocalMap fieldLocalMap : finalAliasedFieldStore) {
         finalAliasedFieldsCount += fieldLocalMap.getFields().size();
      }

      StringBuilder builder = new StringBuilder()
         .append(CollectionVariableState.ALIASED.name())
         .append(" fields: ")
         .append(finalAliasedFieldStore)
         .append("\n")
         .append(CollectionVariableState.ALIASED.name())
         .append(" fields size: ")
         .append(finalAliasedFieldsCount)
         .append("\n");

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         if (state == CollectionVariableState.ALIASED)
            continue;

         List<ObjectFieldPair> fields = getFields(state);
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

      for (CollectionVariableState state : CollectionVariableState.allStates) {
         List<FieldLocalMap> newStore = storeClone.getFieldStore(state);
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            newStore.add(fieldLocalMap.clone());
         }
      }

      for (ObjectFieldPair field : nonAliasedFields) {
         storeClone.addNonAliased(field);
      }

      for (ObjectFieldPair field : externalFields) {
         storeClone.addExternal(field);
      }

      for (ObjectFieldPair field : unknownFields) {
         storeClone.addUnknown(field);
      }

      for (InstanceKey local : externalLocals) {
         storeClone.addExternal(local);
      }

      for (InstanceKey local : unknownLocals) {
         storeClone.addUnknown(local);
      }

      storeClone.setFinalAliasedFieldStore(finalAliasedFieldStore);

      return storeClone;
   }
}

