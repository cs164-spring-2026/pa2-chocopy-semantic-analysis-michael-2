package chocopy.pa2;

import java.util.LinkedHashMap;
import chocopy.common.analysis.types.*;
import java.util.Map;


public final class ClassInfo {
    public final String name;
    public String supername;

    public final Map<String, ValueType> attributes = new LinkedHashMap<>();
    public final Map<String, FuncType> methods = new LinkedHashMap<>();

    public ClassInfo(String name, String supername) {
        this.name = name;
        this.supername = supername;
    }
}


