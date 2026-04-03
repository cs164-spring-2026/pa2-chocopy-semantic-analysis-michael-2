package chocopy.pa2;

import chocopy.common.analysis.*;
import chocopy.common.astnodes.*;

import static chocopy.common.analysis.types.Type.BOOL_TYPE;
import static chocopy.common.analysis.types.Type.INT_TYPE;
import static chocopy.common.analysis.types.Type.NONE_TYPE;
import static chocopy.common.analysis.types.Type.OBJECT_TYPE;
import static chocopy.common.analysis.types.Type.STR_TYPE;

/** Analyzer that performs ChocoPy type checks on all nodes. */
public class TypeChecker extends AbstractNodeAnalyzer<Type> {

    /** Current symbol table. */
    private SymbolTable<Type> sym;

    /** Class hierarchy. */
    private final ClassHierarchy hierarchy;

    /** Collector for errors. */
    private final Errors errors;

    /** Creates a type checker. */
    public TypeChecker(SymbolTable<Type> globalSymbols,
                       ClassHierarchy hierarchy0,
                       Errors errors0) {
        sym = globalSymbols;
        hierarchy = hierarchy0;
        errors = errors0;
    }

    /** Report semantic/type error. */
    private void err(Node node, String message, Object... args) {
        errors.semError(node, message, args);
    }

    @Override
    public Type analyze(Program program) {
        for (Declaration decl : program.declarations) {
            decl.dispatch(this);
        }
        for (Stmt stmt : program.statements) {
            stmt.dispatch(this);
        }
        return null;
    }

    @Override
    public Type analyze(ExprStmt s) {
        s.expr.dispatch(this);
        return null;
    }

    @Override
    public Type analyze(VarDef varDef) {
        Type valueType = varDef.value.dispatch(this);
        Type declaredType = ValueType.annotationToValueType(varDef.var.type);

        if (valueType instanceof ValueType && declaredType instanceof ValueType) {
            if (!hierarchy.isAssignmentCompatible((ValueType) valueType,
                                                  (ValueType) declaredType)) {
                err(varDef,
                    "Expected type `%s`; got type `%s`",
                    declaredType, valueType);
            }
        }
        return null;
    }

    @Override
    public Type analyze(AssignStmt stmt) {
        Type rhs = stmt.value.dispatch(this);

        for (Expr target : stmt.targets) {
            Type lhs = target.dispatch(this);

            if (!(lhs instanceof ValueType) || !(rhs instanceof ValueType)) {
                continue;
            }

            if (!hierarchy.isAssignmentCompatible((ValueType) rhs,
                                                  (ValueType) lhs)) {
                err(stmt,
                    "Expected type `%s`; got type `%s`",
                    lhs, rhs);
                break;
            }
        }
        return null;
    }

    @Override
    public Type analyze(ReturnStmt stmt) {
        if (stmt.value != null) {
            stmt.value.dispatch(this);
        }
        return null;
    }

    @Override
    public Type analyze(IntegerLiteral i) {
        return i.setInferredType(INT_TYPE);
    }

    @Override
    public Type analyze(BooleanLiteral b) {
        return b.setInferredType(BOOL_TYPE);
    }

    @Override
    public Type analyze(StringLiteral s) {
        return s.setInferredType(STR_TYPE);
    }

    @Override
    public Type analyze(NoneLiteral n) {
        return n.setInferredType(NONE_TYPE);
    }

    @Override
    public Type analyze(UnaryExpr e) {
        Type t = e.operand.dispatch(this);

        switch (e.operator) {
        case "-":
            if (INT_TYPE.equals(t)) {
                return e.setInferredType(INT_TYPE);
            }
            err(e, "Cannot apply operator `%s` on type `%s`",
                e.operator, t);
            return e.setInferredType(INT_TYPE);

        case "not":
            if (BOOL_TYPE.equals(t)) {
                return e.setInferredType(BOOL_TYPE);
            }
            err(e, "Cannot apply operator `%s` on type `%s`",
                e.operator, t);
            return e.setInferredType(BOOL_TYPE);

        default:
            return e.setInferredType(OBJECT_TYPE);
        }
    }

    @Override
    public Type analyze(BinaryExpr e) {
        Type t1 = e.left.dispatch(this);
        Type t2 = e.right.dispatch(this);

        switch (e.operator) {
        case "+":
        case "-":
        case "*":
        case "//":
        case "%":
            if (INT_TYPE.equals(t1) && INT_TYPE.equals(t2)) {
                return e.setInferredType(INT_TYPE);
            }
            err(e, "Cannot apply operator `%s` on types `%s` and `%s`",
                e.operator, t1, t2);
            if (INT_TYPE.equals(t1) || INT_TYPE.equals(t2)) {
                return e.setInferredType(INT_TYPE);
            }
            return e.setInferredType(OBJECT_TYPE);

        case "==":
        case "!=":
            if (t1 != null && t1.equals(t2) &&
                (INT_TYPE.equals(t1) || BOOL_TYPE.equals(t1) || STR_TYPE.equals(t1))) {
                return e.setInferredType(BOOL_TYPE);
            }
            err(e, "Cannot apply operator `%s` on types `%s` and `%s`",
                e.operator, t1, t2);
            return e.setInferredType(BOOL_TYPE);

        case "<":
        case "<=":
        case ">":
        case ">=":
            if (INT_TYPE.equals(t1) && INT_TYPE.equals(t2)) {
                return e.setInferredType(BOOL_TYPE);
            }
            err(e, "Cannot apply operator `%s` on types `%s` and `%s`",
                e.operator, t1, t2);
            return e.setInferredType(BOOL_TYPE);

        case "and":
        case "or":
            if (BOOL_TYPE.equals(t1) && BOOL_TYPE.equals(t2)) {
                return e.setInferredType(BOOL_TYPE);
            }
            err(e, "Cannot apply operator `%s` on types `%s` and `%s`",
                e.operator, t1, t2);
            return e.setInferredType(BOOL_TYPE);

        case "is":
            if (!INT_TYPE.equals(t1) && !BOOL_TYPE.equals(t1) && !STR_TYPE.equals(t1)
                && !INT_TYPE.equals(t2) && !BOOL_TYPE.equals(t2) && !STR_TYPE.equals(t2)) {
                return e.setInferredType(BOOL_TYPE);
            }
            err(e, "Cannot apply operator `%s` on types `%s` and `%s`",
                e.operator, t1, t2);
            return e.setInferredType(BOOL_TYPE);

        default:
            return e.setInferredType(OBJECT_TYPE);
        }
    }

    @Override
    public Type analyze(Identifier id) {
        String name = id.name;
        Type t = sym.get(name);

        if (t != null && t.isValueType()) {
            return id.setInferredType(t);
        }

        err(id, "Not a variable: %s", name);
        return id.setInferredType(OBJECT_TYPE);
    }
}