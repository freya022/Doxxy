package com.freya02.docs;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.intellij.lang.annotations.Language;

import java.util.List;

public class JavaParserBugTest {
    public static void main(String[] args) {
        @Language("Java")
        final String code = """
                interface Activity {
                    class Timestamps {}
                    enum ActivityType {}
                
                    @Nonnull
                    ActivityType getType();
                    
                    @Nullable
                    Timestamps getTimestamps();
                }
                
                interface RichPresence extends Activity {}
                
                class ActivityImpl implements Activity {
                    @Nonnull
                    @Override
                    public ActivityType getType() { return type; }
                
                    @Nullable
                    public RichPresence.Timestamps getTimestamps() { return timestamps; }
                }
                
                class RichPresenceImpl extends ActivityImpl implements RichPresence { }
                """;

        final JavaSymbolSolver solver = new JavaSymbolSolver(new ReflectionTypeSolver(false));

        StaticJavaParser.getParserConfiguration().setSymbolResolver(solver);
        final CompilationUnit compilationUnit = StaticJavaParser.parse(code);

        final List<String> returnTypes = compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .map(ClassOrInterfaceDeclaration::resolve)
                .flatMap(typeDeclaration -> typeDeclaration.getDeclaredMethods().stream())
                .map(ResolvedMethodDeclaration::getReturnType)
                .map(Object::toString)
                .toList();

        System.out.println("returnTypes = " + returnTypes);
    }
}
