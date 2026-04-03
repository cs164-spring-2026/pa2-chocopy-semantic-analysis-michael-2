package chocopy.pa2;

import java.util.ArrayList;
import java.util.List;

import chocopy.common.analysis.AbstractNodeAnalyzer;
import chocopy.common.analysis.SymbolTable;
import chocopy.common.analysis.types.ClassValueType;
import chocopy.common.analysis.types.FuncType;
import chocopy.common.analysis.types.Type;
import chocopy.common.analysis.types.ValueType;
import chocopy.common.astnodes.ClassDef;
import chocopy.common.astnodes.Declaration;
import chocopy.common.astnodes.Errors;
import chocopy.common.astnodes.FuncDef;
import chocopy.common.astnodes.Identifier;
import chocopy.common.astnodes.Program;
import chocopy.common.astnodes.TypedVar;
import chocopy.common.astnodes.VarDef;

/**
 * Analyzes declarations to create the top-level symbol table.
 */
public class DeclarationAnalyzer extends AbstractNodeAnalyzer<Type> {

    /** Current symbol table. */
    private SymbolTable<Type> sym = new SymbolTable<>();

    /** Global symbol table. */
    private final SymbolTable<Type> globals = sym;

    /** Class hierarchy / class table. */
    private final ClassHierarchy hierarchy;

    /** Receiver for semantic error messages. */
    private final Errors errors;

    /** New declaration analyzer. */
    public DeclarationAnalyzer(ClassHierarchy hierarchy0, Errors errors0) {
        this.hierarchy = hierarchy0;
        this.errors = errors0;
        initBuiltins();
    }

    /** Install predefined functions. */
    private void initBuiltins() {
        List<ValueType> oneObj = new ArrayList<>();
        oneObj.add(Type.OBJECT_TYPE);

        globals.put("print", new FuncType(oneObj, Type.NONE_TYPE));
        globals.put("len", new FuncType(oneObj, Type.INT_TYPE));
        globals.put("input", new FuncType(new ArrayList<>(), Type.STR_TYPE));
    }

    public SymbolTable<Type> getGlobals() {
        return globals;
    }

    /** Report semantic error. */
    private void err(Identifier id, String message, Object... args) {
        errors.semError(id, message, args);
    }

    @Override
    public Type analyze(Program program) {
        for (Declaration decl : program.declarations) {
            Identifier id = decl.getIdentifier();
            String name = id.name;

            Type type = decl.dispatch(this);
            if (type == null) {
                continue;
            }

            if (globals.declares(name)) {
                err(id, "Duplicate declaration of identifier in same scope: %s",
                    name);
                continue;
            }

            globals.put(name, type);
        }
        return null;
    }

    @Override
    public Type analyze(VarDef varDef) {
        ValueType t = ValueType.annotationToValueType(varDef.var.type);

        if (t instanceof ClassValueType) {
            String className = t.className();
            if (!hierarchy.containsClass(className)
                && !className.equals("<None>")
                && !className.equals("<Empty>")) {
                errors.semError(varDef.var.type,
                                "Invalid type annotation; there is no class named: %s",
                                className);
            }
        }

        return t;
    }

    @Override
    public Type analyze(FuncDef funcDef) {
        String name = funcDef.name.name;

        if (hierarchy.containsClass(name)) {
            err(funcDef.name, "Cannot shadow class name: %s", name);
        }

        List<ValueType> params = new ArrayList<>();
        for (TypedVar tv : funcDef.params) {
            params.add(ValueType.annotationToValueType(tv.type));
        }

        ValueType ret =
            ValueType.annotationToValueType(funcDef.returnType);

        return new FuncType(params, ret);
    }

    @Override
    public Type analyze(ClassDef classDef) {
        String name = classDef.name.name;

        // Top-level symbol table can map class names to their class type.
        return new ClassValueType(name);
    }
}