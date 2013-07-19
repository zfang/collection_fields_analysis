package com.zfang.cf;

import static com.zfang.cf.CollectionFieldsAnalysis.isFromJavaOrSunPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalStore implements Cloneable {

   private final Set<ObjectFieldPair> nonAliasedFields = new LinkedHashSet<ObjectFieldPair>(), 
          externalFields = new LinkedHashSet<ObjectFieldPair>(),
          unknownFields = new LinkedHashSet<ObjectFieldPair>();

   /* 0: fields from externalFields
    * 1: fields from unknownFields
    * 2: fields from nonAliasedFields
    */
   @SuppressWarnings("unchecked")
      private final List<FieldLocalMap> [] mayAliasedFieldStore = (List<FieldLocalMap>[]) new List[3];

   private final Set<InstanceKey> externalLocals = new LinkedHashSet<InstanceKey>(),
      unknownLocals = new LinkedHashSet<InstanceKey>();

   private final List<FieldLocalMap> aliasedFieldStore = new ArrayList<FieldLocalMap>();

   private final Set<FieldLocalMap> finalAliasedFieldStore = 
      Collections.synchronizedSet(new LinkedHashSet<FieldLocalMap>());

   public FieldLocalStore() {
      for (int i = 0, size = mayAliasedFieldStore.length; i < size; ++i) {
         mayAliasedFieldStore[i] = new ArrayList<FieldLocalMap>();
      }
   }

   public List<FieldLocalMap> getFieldStore(CollectionVaribleState state) {
      return state == CollectionVaribleState.ALIASED ? aliasedFieldStore :
         mayAliasedFieldStore[state.ordinal()-1];
   }

   public void removeField(ObjectFieldPair objectFieldPair) {
      if (!nonAliasedFields.remove(objectFieldPair) 
            && !externalFields.remove(objectFieldPair)
            && !unknownFields.remove(objectFieldPair)) {
         for (CollectionVaribleState state : CollectionVaribleState.allStates) {
            for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               if (fieldSet.remove(objectFieldPair)) {
                  return;
               }
            }
         }
      }
   }

   public void addToStore(final ObjectFieldPair objectFieldPair, final InstanceKey local, CollectionVaribleState type) {
      List<FieldLocalMap> store = getFieldStore(type);
      if ((null == objectFieldPair && null == local) || null == store) {
         return;
      }
      store.add(
            new FieldLocalMap(){{
               addToLocalSet(local);
               addToFieldSet(objectFieldPair);
      }});
   }

   public void addToStore(final InstanceKey local1,  final InstanceKey local2,  CollectionVaribleState type) {
      List<FieldLocalMap> store = getFieldStore(type);
      if ((null == local1 && null == local2) || null == store) {
         return;
      }
      store.add(
            new FieldLocalMap(){{
               addToLocalSet(local1);
               addToLocalSet(local2);
      }});
   }

   public void addField(ObjectFieldPair objectFieldPair, InstanceKey rightKey) {
      removeField(objectFieldPair);

      if (externalLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, CollectionVaribleState.EXTERNAL);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, CollectionVaribleState.UNKNOWN);
         return;
      }

      if (null == rightKey) {
         nonAliasedFields.add(objectFieldPair);
         return;
      }

      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
            if (localSet.contains(rightKey)) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               fieldSet.add(objectFieldPair);
               return;
            }
         }
      }

      unknownFields.add(objectFieldPair);
   }

   public void addLocal(InstanceKey leftKey, InstanceKey rightKey) {
      //print("addField: leftKey: " + leftKey);
      if (null == rightKey) {
         addToStore(leftKey, rightKey, CollectionVaribleState.NONALIASED);
         return;
      }

      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
            if (localSet.contains(rightKey)) {
               localSet.add(leftKey);
               return;
            }
         }
      }

      if (externalLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, CollectionVaribleState.EXTERNAL);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, CollectionVaribleState.UNKNOWN);
         return;
      }

   }

   public void addLocal(InstanceKey leftKey, ObjectFieldPair objectFieldPair) {

      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
         for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
            Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
            if (fieldSet.contains(objectFieldPair)) {
               Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
               localSet.add(leftKey);
               return;
            }
         }
      }

      if (nonAliasedFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, CollectionVaribleState.NONALIASED);
         return;
      }

      if (externalFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, CollectionVaribleState.EXTERNAL);
         return;
      }
      
      if (unknownFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, CollectionVaribleState.UNKNOWN);
         return;
      }
      
      addToStore(objectFieldPair, leftKey, CollectionVaribleState.UNKNOWN);
      return;
   }

   public void addNonAliased(ObjectFieldPair field) {
      nonAliasedFields.add(field);
   }
      
   public void addAliased(ObjectFieldPair field, InstanceKey local) {
      if (null == field && null == local) {
         return;
      }

      CollectionVaribleState state = CollectionVaribleState.ALIASED;

      for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
         if (null != local && localSet.contains(local)) {
            fieldLocalMap.addToFieldSet(field);
            return;
         }
         if (null != field && fieldSet.contains(field)) {
            fieldLocalMap.addToLocalSet(local);
            return;
         }
      }

      addToStore(field, local, state);
   }

   public void addExternal(ObjectFieldPair field) {
      externalFields.add(field);
   }

   public void addExternal(InstanceKey local) {
      externalLocals.add(local);
   }

   public void addUnknown(ObjectFieldPair field) {
      unknownFields.add(field);
   }
      
   public void addUnknown(InstanceKey local) {
      unknownLocals.add(local);
   }

   public boolean isNonAliased(ObjectFieldPair field) {
      return nonAliasedFields.contains(field);
   }
      
   public boolean isAliased(ObjectFieldPair field) {
      for (FieldLocalMap fieldLocalMap : aliasedFieldStore) {
         if (fieldLocalMap.getFieldSet().contains(field)) {
            return true;
         }
      }
      return false;
   }

   public boolean isAliased(InstanceKey local) {
      for (FieldLocalMap fieldLocalMap : aliasedFieldStore) {
         if (fieldLocalMap.getLocalSet().contains(local)) {
            return true;
         }
      }
      return false;
   }
      
   public boolean isExternal(ObjectFieldPair field) {
      return externalFields.contains(field);
   }

   public boolean isExternal(InstanceKey local) {
      return externalLocals.contains(local);
   }

   public boolean isUnknown(ObjectFieldPair field) {
      return unknownFields.contains(field);
   }
      
   public boolean isUnknown(InstanceKey local) {
      return unknownLocals.contains(local);
   }

   public void setFinalAliasedFieldStore(Set<FieldLocalMap> store) {
      finalAliasedFieldStore.addAll(store);
   }

   public void finalize() {
      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
         populateFinalAliasedFieldStore(state);
      }

      Iterator<FieldLocalMap> iter = finalAliasedFieldStore.iterator();
      while (iter.hasNext()) {
         FieldLocalMap fieldLocalMap = iter.next();
         for (ObjectFieldPair field : fieldLocalMap.getFieldSet()) {
            if (isFromJavaOrSunPackage(field.getField())) {
               iter.remove();
               break;
            }
         }
      }

      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
         updateFieldMap(state);
      }
   }

   public void populateFinalAliasedFieldStore(CollectionVaribleState state) {
      Set<ObjectFieldPair> fields = null;
      Set<InstanceKey> locals = null;

      switch(state) {
         case ALIASED: 
            {
               for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
                  if (!fieldLocalMap.getFieldSet().isEmpty()) {
                     finalAliasedFieldStore.add(fieldLocalMap);
                  }
               }
               return;
            }
         case EXTERNAL:
            fields = externalFields;
            locals = externalLocals;
            break;
         case UNKNOWN:
            fields = unknownFields;
            locals = unknownLocals;
            break;
         case NONALIASED:
            fields = nonAliasedFields;
            break;
         default:
            return;
      }

      for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() > 1) {
            finalAliasedFieldStore.add(fieldLocalMap);
         }
         else if (fieldSet.size() == 1) {
            if (fields != null)
               fields.addAll(fieldSet);
            if (locals != null)
               locals.addAll(fieldLocalMap.getLocalSet());
         }
      }
   }

   public void updateFieldMap(CollectionVaribleState state) {
      List<Set<ObjectFieldPair>> fieldList = new ArrayList<Set<ObjectFieldPair>>();
      switch(state) {
         case ALIASED: 
               for (FieldLocalMap fieldLocalMap : finalAliasedFieldStore) {
                  fieldList.add(fieldLocalMap.getFieldSet());
               }
               break;
         case EXTERNAL:
            fieldList.add(externalFields);
            break;
         case UNKNOWN:
            fieldList.add(unknownFields);
            break;
         case NONALIASED:
            fieldList.add(nonAliasedFields);
            break;
         default:
            return;
      }

      for (Set<ObjectFieldPair> fields : fieldList) {
         for (ObjectFieldPair field : fields) {
            CollectionVaribleState currentState = CollectionFieldsAnalysis.fieldMap.get(field.getField());
            CollectionFieldsAnalysis.fieldMap.put(field.getField(), 
                  currentState == null ? state : CollectionVaribleState.getNewValue(currentState, state));
         }
      }
   }
      
   public String toStringDebug() {
      StringBuilder builder =  new StringBuilder();

      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
         builder
            .append(state.name())
            .append(": ")
            .append(getFieldStore(state))
            .append("\n");
      }

      return builder
         .append("nonAliasedFields: ")
         .append(nonAliasedFields.toString())
         .append("\n")
         .append("externalFields: ")
         .append(externalFields.toString())
         .append("\n")
         .append("externalLocals: ")
         .append(externalLocals.toString())
         .append("\n")
         .append(unknownFields.toString())
         .append("\n")
         .append("unknownLocals: ")
         .append(unknownLocals.toString())
         .append("\n")
         .toString();
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
         finalAliasedFieldsCount += fieldLocalMap.getFieldSet().size();
      }

      return new StringBuilder()
         .append("nonAliasedFields: ")
         .append(nonAliasedFields.toString())
         .append("\n")
         .append("nonAliasedFields size: ")
         .append(nonAliasedFields.size())
         .append("\n")
         .append("finalAliasedFieldStore: ")
         .append(finalAliasedFieldStore.toString())
         .append("\n")
         .append("finalAliasedFieldStore size: ")
         .append(finalAliasedFieldsCount)
         .append("\n")
         .append("externalFields: ")
         .append(externalFields.toString())
         .append("\n")
         .append("externalFields size: ")
         .append(externalFields.size())
         .append("\n")
         .append("unknownFields: ")
         .append(unknownFields.toString())
         .append("\n")
         .append("unknownFields size: ")
         .append(unknownFields.size())
         .append("\n")
         .toString();
   }

   public FieldLocalStore clone() {
      FieldLocalStore storeClone = new FieldLocalStore();

      for (CollectionVaribleState state : CollectionVaribleState.allStates) {
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

