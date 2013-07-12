package com.zfang.cf;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalStore {
   
   private Set<ObjectFieldPair> nonAliasedFields = new HashSet<ObjectFieldPair>(), 
          externalFields = new HashSet<ObjectFieldPair>(),
          unknownFields = new HashSet<ObjectFieldPair>();

   /* 0: fields from nonAliasedFields
    * 1: fields from externalFields
    * 2: fields from unknownFields
    */
   private List<FieldLocalMap> [] mayAliasedFieldStore = (List<FieldLocalMap>[]) new List[3];

   private Set<InstanceKey> externalLocals = new HashSet<InstanceKey>(),
          unknownLocals = new HashSet<InstanceKey>();

   public FieldLocalStore() {
      for (int i = 0, size = mayAliasedFieldStore.length; i < size; ++i) {
         mayAliasedFieldStore[i] = new LinkedList<FieldLocalMap>();
      }
   }

   public void removeField(ObjectFieldPair objectFieldPair) {
      if (!nonAliasedFields.remove(objectFieldPair) 
            && !externalFields.remove(objectFieldPair) 
            && !unknownFields.remove(objectFieldPair)) {
         for (List<FieldLocalMap> store : mayAliasedFieldStore) {
            for (FieldLocalMap fieldLocalMap : store) {
               Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
               if (fieldSet.remove(objectFieldPair)) {
                  return;
               }
            }
         }
      }
   }

   public void addToStore(ObjectFieldPair objectFieldPair, InstanceKey local,  int i) {
      if ((null == objectFieldPair && null == local) || (i < 0 || i >= mayAliasedFieldStore.length)) {
         return;
      }
      FieldLocalMap fieldLocalMap = new FieldLocalMap();
      fieldLocalMap.addToLocalSet(local);
      fieldLocalMap.addToFieldSet(objectFieldPair);
      mayAliasedFieldStore[i].add(fieldLocalMap);
   }

   public void addToStore(InstanceKey local1,  InstanceKey local2,  int i) {
      if ((null == local1 && null == local2) || (i < 0 || i >= mayAliasedFieldStore.length)) {
         return;
      }
      FieldLocalMap fieldLocalMap = new FieldLocalMap();
      fieldLocalMap.addToLocalSet(local1);
      fieldLocalMap.addToLocalSet(local2);
      mayAliasedFieldStore[i].add(fieldLocalMap);
   }

   public void addField(ObjectFieldPair objectFieldPair, InstanceKey rightKey) {
      removeField(objectFieldPair);

      if (externalLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, 1);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(objectFieldPair, rightKey, 2);
         return;
      }

      if (null == rightKey) {
         nonAliasedFields.add(objectFieldPair);
         return;
      }

      for (List<FieldLocalMap> store : mayAliasedFieldStore) {
         for (FieldLocalMap fieldLocalMap : store) {
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
         addToStore(leftKey, rightKey, 0);
         return;
      }

      for (List<FieldLocalMap> store : mayAliasedFieldStore) {
         for (FieldLocalMap fieldLocalMap : store) {
            Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
            if (localSet.contains(rightKey)) {
               localSet.add(leftKey);
               return;
            }
         }
      }

      if (externalLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, 1);
         return;
      }

      if (unknownLocals.remove(rightKey)) {
         addToStore(leftKey, rightKey, 2);
         return;
      }

   }

   public void addLocal(InstanceKey leftKey, ObjectFieldPair objectFieldPair) {

      for (List<FieldLocalMap> store : mayAliasedFieldStore) {
         for (FieldLocalMap fieldLocalMap : store) {
            Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
            if (fieldSet.contains(objectFieldPair)) {
               Set<InstanceKey> localSet = fieldLocalMap.getLocalSet();
               localSet.add(leftKey);
               return;
            }
         }
      }

      if (nonAliasedFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, 0);
         return;
      }

      if (externalFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, 1);
         return;
      }

      if (unknownFields.remove(objectFieldPair)) {
         addToStore(objectFieldPair, leftKey, 2);
         return;
      }
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
      
   public String toString() {
      Set<FieldLocalMap> aliasedFieldStore = new HashSet<FieldLocalMap>();

      for (FieldLocalMap fieldLocalMap : mayAliasedFieldStore[0]) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() == 1) {
            nonAliasedFields.addAll(fieldSet);
         }
         else if (fieldSet.size() > 1) {
            aliasedFieldStore.add(fieldLocalMap);
         }
      }

      for (FieldLocalMap fieldLocalMap : mayAliasedFieldStore[1]) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() == 1) {
            externalFields.addAll(fieldSet);
         }
         else if (fieldSet.size() > 1) {
            aliasedFieldStore.add(fieldLocalMap);
         }
      }

      for (FieldLocalMap fieldLocalMap : mayAliasedFieldStore[2]) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() == 1) {
            unknownFields.addAll(fieldSet);
         }
         else if (fieldSet.size() > 1) {
            aliasedFieldStore.add(fieldLocalMap);
         }
      }

      if (nonAliasedFields.isEmpty() && aliasedFieldStore.isEmpty() && externalFields.isEmpty()) {
         return "";
      }

      int aliasedFieldsCount = 0;
      for (FieldLocalMap fieldLocalMap : aliasedFieldStore) {
         aliasedFieldsCount += fieldLocalMap.getFieldSet().size();
      }

      return new StringBuilder()
         .append("nonAliasedFields: ")
         .append(nonAliasedFields.toString())
         .append("\n")
         .append("nonAliasedFields size: ")
         .append(nonAliasedFields.size())
         .append("\n")
         .append("aliasedFieldStore: ")
         .append(aliasedFieldStore.toString())
         .append("\n")
         .append("aliasedFields size: ")
         .append(aliasedFieldsCount)
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

}

