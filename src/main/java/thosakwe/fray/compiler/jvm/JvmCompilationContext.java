package thosakwe.fray.compiler.jvm;

import java.util.HashMap;
import java.util.Map;

public class JvmCompilationContext {
    private Map<String, Integer> classIndices = new HashMap<>();

    public int getClassIndex(String className) {
        if (classIndices.containsKey(className))
            return classIndices.get(className);
        else {
            int index;
            classIndices.put(className, index = classIndices.size());
            return index;
        }
    }
}
