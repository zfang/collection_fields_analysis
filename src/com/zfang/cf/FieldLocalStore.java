package com.zfang.cf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalStore implements Cloneable {
   
   private Set<ObjectFieldPair> nonAliasedFields = new LinkedHashSet<ObjectFieldPair>(), 
          externalFields = new LinkedHashSet<ObjectFieldPair>(),
          unknownFields = new LinkedHashSet<ObjectFieldPair>();

   /* 0: fields from externalFields
    * 1: fields from unknownFields
    * 2: fields from nonAliasedFields
    */
   private List<FieldLocalMap> [] mayAliasedFieldStore = (List<FieldLocalMap>[]) new List[3];

   private Set<InstanceKey> externalLocals = new LinkedHashSet<InstanceKey>(),
          unknownLocals = new LinkedHashSet<InstanceKey>();

   private List<FieldLocalMap> aliasedFieldStore = new ArrayList<FieldLocalMap>();

   private Set<FieldLocalMap> finalAliasedFieldStore = new LinkedHashSet<FieldLocalMap>();

   public FieldLocalStore() {
      for (int i = 0, size = mayAliasedFieldStore.length; i < size; ++i) {
         mayAliasedFieldStore[i] = new ArrayList<FieldLocalMap>();
      }
   }

   public List<FieldLocalMap> getFieldStore(CollectionVaribleState type) {
      switch (type) {
         case ALIASED: 
            return aliasedFieldStore;
         case EXTERNAL: 
         case UNKNOWN:
         case NONALIASED:
            return mayAliasedFieldStore[type.ordinal()-1];
         default:
            return null;
      }
   }

   public void removeField(ObjectFieldPair objectFieldPair) {
      if (!nonAliasedFields.remove(objectFieldPair) 
            && !externalFields.remove(objectFieldPair) 
            && !unknownFields.remove(objectFieldPair)) {
         for (CollectionVaribleState state : EnumSet.allOf(CollectionVaribleState.class)) {
            for (FieldLocalMap fieldLocalMap : getFieldStore(state)) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               if (fieldSet.remove(objectFieldPair)) {
                  return;
               }
            }
         }
      }
   }

   public void addToStore(ObjectFieldPair objectFieldPair, InstanceKey local, CollectionVaribleState type) {
      List<FieldLocalMap> store = getFieldStore(type);
      if ((null == objectFieldPair && null == local) || null == store) {
         return;
      }
      FieldLocalMap fieldLocalMap = new FieldLocalMap();
      fieldLocalMap.addToLocalSet(local);
      fieldLocalMap.addToFieldSet(objectFieldPair);
      store.add(fieldLocalMap);
   }

   public void addToStore(InstanceKey local1,  InstanceKey local2,  CollectionVaribleState type) {
      List<FieldLocalMap> store = getFieldStore(type);
      if ((null == local1 && null == local2) || null == store) {
         return;
      }
      FieldLocalMap fieldLocalMap = new FieldLocalMap();
      fieldLocalMap.addToLocalSet(local1);
      fieldLocalMap.addToLocalSet(local2);
      store.add(fieldLocalMap);
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

      for (CollectionVaribleState state : EnumSet.allOf(CollectionVaribleState.class)) {
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

      for (CollectionVaribleState state : EnumSet.allOf(CollectionVaribleState.class)) {
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

      for (CollectionVaribleState state : EnumSet.allOf(CollectionVaribleState.class)) {
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
      
      // If we cannot find the field, we say it's external
      addExternal(leftKey);
   }

   public void addNonAliased(ObjectFieldPair field) {
      nonAliasedFields.add(field);
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
      finalAliasedFieldStore = store;
   }

   public void finalize() {
      for (FieldLocalMap fieldLocalMap : getFieldStore(CollectionVaribleState.EXTERNAL)) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() > 1) {
            finalAliasedFieldStore.add(fieldLocalMap);
         }
         else {
            externalFields.addAll(fieldSet);
            externalLocals.addAll(fieldLocalMap.getLocalSet());
         }
      }

      for (FieldLocalMap fieldLocalMap : getFieldStore(CollectionVaribleState.UNKNOWN)) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() > 1) {
            finalAliasedFieldStore.add(fieldLocalMap);
         }
         else {
            unknownFields.addAll(fieldSet);
            unknownLocals.addAll(fieldLocalMap.getLocalSet());
         }
      }

      for (FieldLocalMap fieldLocalMap : getFieldStore(CollectionVaribleState.NONALIASED)) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() == 1) {
            nonAliasedFields.addAll(fieldSet);
         }
         else if (fieldSet.size() > 1) {
            aliasedFieldStore.add(fieldLocalMap);
         }
      }

      for (FieldLocalMap fieldLocalMap : getFieldStore(CollectionVaribleState.ALIASED)) {
         if (!fieldLocalMap.getFieldSet().isEmpty()) {
            finalAliasedFieldStore.add(fieldLocalMap);
         }
      }
   }
      
   public String toStringDebug() {
      return new StringBuilder()
         .append("nonAliasedFields: ")
         .append(nonAliasedFields.toString())
         .append("\n")
         .append("aliasedFieldStore: ")
         .append(aliasedFieldStore.toString())
         .append("\n")
         .append("mayAliasedFieldStore[0]: ")
         .append(mayAliasedFieldStore[0].toString())
         .append("\n")
         .append("mayAliasedFieldStore[1]: ")
         .append(mayAliasedFieldStore[1].toString())
         .append("\n")
         .append("mayAliasedFieldStore[2]: ")
         .append(mayAliasedFieldStore[2].toString())
         .append("\n")
         .append("externalFields: ")
         .append(externalFields.toString())
         .append("\n")
         .append("unknownFields: ")
         .append(unknownFields.toString())
         .append("\n")
         .append("externalLocals: ")
         .append(externalLocals.toString())
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
         .append("finalAliasedFieldsCount size: ")
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

      for (CollectionVaribleState state : EnumSet.allOf(CollectionVaribleState.class)) {
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

      Set<FieldLocalMap> newFinalAliasedFieldsCount
         = new LinkedHashSet<FieldLocalMap>();
      newFinalAliasedFieldsCount.addAll(finalAliasedFieldStore);
      storeClone.setFinalAliasedFieldStore(newFinalAliasedFieldsCount);

      return storeClone;
   }
}

