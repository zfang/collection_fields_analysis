package com.hang.ld;

import java.util.HashSet;
import java.util.Set;

import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalMap {

   Set<InstanceKey> localSet = new HashSet<InstanceKey>();
   Set<ObjectFieldPair> fieldSet = new HashSet<ObjectFieldPair>();

	public int hashCode() {
      int hashCode = 0;   
      for (InstanceKey local : localSet) {
         hashCode ^= local.hashCode();
      }
      for (ObjectFieldPair field : fieldSet) {
         hashCode ^= field.hashCode();
      }
      return hashCode;
	}


   public boolean equals(Object obj) {
      if (null == obj) {
         return false;
      }
      if (this == obj) {
         return true;
      }
      if (! (obj instanceof FieldLocalMap)) {
         return false;
      }
      return ((FieldLocalMap)obj).hashCode() == hashCode();
   }

   public Set<InstanceKey> getLocalSet() {
      return localSet;
   }

   public Set<ObjectFieldPair> getFieldSet() {
      return fieldSet;
   }

   public void addToLocalSet(InstanceKey local) {
      if (null != local)
         localSet.add(local);
   }
   
   public void addToFieldSet(ObjectFieldPair field) {
      if (null != field)
         fieldSet.add(field);
   }

   public String toString() {
      return new StringBuilder()
         .append("{")
         .append("field=")
         .append(fieldSet)
         .append(", ")
         .append("local=")
         .append(localSet)
         .append("}")
         .toString();
   }

}
