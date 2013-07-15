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

   private Set<FieldLocalMap> aliasedFieldStore = new HashSet<FieldLocalMap>();

   public FieldLocalStore() {
      for (int i = 0, size = mayAliasedFieldStore.length; i < size; ++i) {
         mayAliasedFieldStore[i] = new LinkedList<FieldLocalMap>();
      }
   }

   private void removeField(ObjectFieldPair objectFieldPair) {
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

   public void finalize() {
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
         if (fieldSet.size() > 1) {
            aliasedFieldStore.add(fieldLocalMap);
         }
         else {
            externalFields.addAll(fieldSet);
            externalLocals.addAll(fieldLocalMap.getLocalSet());
         }
      }

      for (FieldLocalMap fieldLocalMap : mayAliasedFieldStore[2]) {
         Set<ObjectFieldPair> fieldSet = fieldLocalMap.getFieldSet();
         if (fieldSet.size() > 1) {
            aliasedFieldStore.add(fieldLocalMap);
         }
         else {
            unknownFields.addAll(fieldSet);
            unknownLocals.addAll(fieldLocalMap.getLocalSet());
         }
      }
   }
      
   public String toStringDebug() {
      return new StringBuilder()
         .append("nonAliasedFields: ")
         .append(nonAliasedFields.toString())
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
            && aliasedFieldStore.isEmpty() 
            && externalFields.isEmpty()
            && unknownFields.isEmpty()) {
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

