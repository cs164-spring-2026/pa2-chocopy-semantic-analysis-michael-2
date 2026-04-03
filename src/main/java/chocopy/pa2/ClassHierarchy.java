package chocopy.pa2;

import java.util.LinkedHashMap;
import java.util.Map;

import chocopy.common.analysis.types.ClassValueType;
import chocopy.common.analysis.types.ListValueType;
import chocopy.common.analysis.types.Type;
import chocopy.common.analysis.types.ValueType;

public final class ClassHierarchy {
    private final Map<String, ClassInfo> classes = new LinkedHashMap<>();

    public ClassHierarchy() {
        addBuiltins();
    }

    private void addBuiltins() {
        classes.put("object", new ClassInfo("object", null));
        classes.put("int", new ClassInfo("int", "object"));
        classes.put("bool", new ClassInfo("bool", "object"));
        classes.put("str", new ClassInfo("str", "object"));
    }

    public boolean containsClass(String name) {
        return classes.containsKey(name);
    }

    public ClassInfo getClassInfo(String name) {
        return classes.get(name);
    }

    public void addClass(String name, String superName) {
        classes.put(name, new ClassInfo(name, superName));
    }

    public boolean isClassType(Type t) {
        return t instanceof ClassValueType;
    }

    public boolean isSubtype(ValueType sub, ValueType sup) {
        if (sub == null || sup == null) {
            return false;
        }

        if (sub.equals(sup)) {
            return true;
        }

        if (sub.equals(Type.NONE_TYPE)) {
            return sup.equals(Type.OBJECT_TYPE)
                || (sup instanceof ClassValueType
                    && !sup.equals(Type.INT_TYPE)
                    && !sup.equals(Type.BOOL_TYPE)
                    && !sup.equals(Type.STR_TYPE));
        }

        if (sub.equals(Type.EMPTY_TYPE)) {
            return sup.equals(Type.OBJECT_TYPE) || sup instanceof ListValueType;
        }

        if (sub instanceof ListValueType) {
            if (sup.equals(Type.OBJECT_TYPE)) {
                return true;
            }
            return false;
        }

        if (sub instanceof ClassValueType && sup instanceof ClassValueType) {
            String cur = sub.className();
            String target = sup.className();

            while (cur != null) {
                if (cur.equals(target)) {
                    return true;
                }
                ClassInfo info = classes.get(cur);
                cur = (info == null) ? null : info.superName;
            }
        }

        return false;
    }

    public boolean isAssignmentCompatible(ValueType from, ValueType to) {
        if (from == null || to == null) {
            return false;
        }

        if (isSubtype(from, to)) {
            return true;
        }

        if (from.equals(Type.NONE_TYPE)) {
            return !to.equals(Type.INT_TYPE)
                && !to.equals(Type.BOOL_TYPE)
                && !to.equals(Type.STR_TYPE);
        }

        if (from.equals(Type.EMPTY_TYPE)) {
            return to instanceof ListValueType;
        }

        if (from instanceof ListValueType && to instanceof ListValueType) {
            ValueType fromElt = from.elementType();
            ValueType toElt = to.elementType();

            if (fromElt != null && fromElt.equals(Type.NONE_TYPE)) {
                return isAssignmentCompatible(Type.NONE_TYPE, toElt);
            }
        }

        return false;
    }

    public ValueType join(ValueType a, ValueType b) {
        if (a == null || b == null) {
            return Type.OBJECT_TYPE;
        }

        if (isAssignmentCompatible(a, b)) {
            return b;
        }

        if (isAssignmentCompatible(b, a)) {
            return a;
        }

        if (a instanceof ClassValueType && b instanceof ClassValueType) {
            String cur = a.className();
            while (cur != null) {
                ClassValueType candidate = new ClassValueType(cur);
                if (isAssignmentCompatible(b, candidate)) {
                    return candidate;
                }
                ClassInfo info = classes.get(cur);
                cur = (info == null) ? null : info.superName;
            }
        }

        return Type.OBJECT_TYPE;
    }
}