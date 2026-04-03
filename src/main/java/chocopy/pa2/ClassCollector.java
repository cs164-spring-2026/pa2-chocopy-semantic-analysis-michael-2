package chocopy.pa2;

import chocopy.common.analysis.AbstractNodeAnalyzer;
import chocopy.common.astnodes.ClassDef;
import chocopy.common.astnodes.Declaration;
import chocopy.common.astnodes.Errors;
import chocopy.common.astnodes.Program;

public final class ClassCollector extends AbstractNodeAnalyzer<Void> {
    private final ClassHierarchy hierarchy;
    private final Errors errors;

    public ClassCollector(ClassHierarchy hierarchy, Errors errors) {
        this.hierarchy = hierarchy;
        this.errors = errors;
    }

    @Override
    public Void analyze(Program program) {
        for (Declaration decl : program.declarations) {
            if (decl instanceof ClassDef) {
                decl.dispatch(this);
            }
        }
        return null;
    }

    @Override
    public Void analyze(ClassDef classDef) {
        String className = classDef.name.name;
        String superName = classDef.superClass.name;

        if (hierarchy.containsClass(className)) {
            errors.semError(classDef.name,
                            "Duplicate declaration of identifier in same scope: %s",
                            className);
            return null;
        }

        if (!"object".equals(superName) && !hierarchy.containsClass(superName)) {
            errors.semError(classDef.superClass,
                            "Super-class not defined: %s",
                            superName);
            return null;
        }

        hierarchy.addClass(className, superName);
        return null;
    }
}
