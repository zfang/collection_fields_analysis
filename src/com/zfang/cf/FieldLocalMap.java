package com.zfang.cf;

import java.util.ArrayList;
import java.util.List;

import soot.Local;
import soot.jimple.toolkits.pointer.InstanceKey;

public class FieldLocalMap implements Cloneable {

   private List<InstanceKey> localSet = new ArrayList<InstanceKey>();
   private List<ObjectFieldPair> fieldSet = new ArrayList<ObjectFieldPair>();

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
         clone.addToLocals(local);
      for (ObjectFieldPair field : fieldSet)
         clone.addToFields(field);
      return clone;
   }

   public List<InstanceKey> getLocals() {
      return localSet;
   }

   public List<ObjectFieldPair> getFields() {
      return fieldSet;
   }

   public void addToLocals(InstanceKey local) {
      if (null != local)
         localSet.add(local);
   }
   
   public void addToFields(ObjectFieldPair field) {
      if (null != field)
         fieldSet.add(field);
   }

   public boolean containsField(ObjectFieldPair field) {
      return fieldSet.contains(field);
   }

   public boolean containsLocal(InstanceKey local) {
      return localSet.contains(local);
   }

   public boolean containsLocal(Local l) {
      for (InstanceKey local : localSet) {
         if (local.getLocal().equals(l))
            return true;
      }
      return false;
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
