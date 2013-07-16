package com.zfang.cf;

import java.util.LinkedHashSet;
import java.util.Set;

import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalMap implements Cloneable {

   private Set<InstanceKey> localSet = new LinkedHashSet<InstanceKey>();
   private Set<ObjectFieldPair> fieldSet = new LinkedHashSet<ObjectFieldPair>();

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

   public FieldLocalMap clone() {
      FieldLocalMap clone = new FieldLocalMap();
      for (InstanceKey local : localSet)
         clone.addToLocalSet(local);
      for (ObjectFieldPair field : fieldSet)
         clone.addToFieldSet(field);
      return clone;
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

   public boolean containsField(ObjectFieldPair field) {
      return fieldSet.contains(field);
   }

   public boolean containsLocal(InstanceKey local) {
      return localSet.contains(local);
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
