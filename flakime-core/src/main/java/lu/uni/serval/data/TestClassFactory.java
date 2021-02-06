package lu.uni.serval.data;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.File;
import java.util.Set;

public class TestClassFactory {
    public static TestClass create(String className, ClassPool classPool, File sourceDirectory, File classDirectory, Set<String> testAnnotations) {
        try {
            classPool.importPackage(className);
            CtClass ctClass = classPool.get(className);
            return new TestClass(testAnnotations, ctClass, sourceDirectory, classDirectory);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
