package chocopy.pa2;

import java.util.LinkedHashMap;
import java.util.Map;

import chocopy.common.analysis.types.FuncType;
import chocopy.common.analysis.types.ValueType;

public final class ClassInfo {
    public final String name;
    public String superName;

    public final Map<String, ValueType> attributes = new LinkedHashMap<>();
    public final Map<String, FuncType> methods = new LinkedHashMap<>();

    public ClassInfo(String name, String superName) {
        this.name = name;
        this.superName = superName;
    }
}