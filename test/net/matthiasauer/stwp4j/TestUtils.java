package net.matthiasauer.stwp4j;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestUtils {
    public interface TestUtilsExecutable {
        void execute();
    }
    
    public static void expectInterruptedExceptionToContain(TestUtilsExecutable executable, String fragment) {
        try {
            executable.execute();
            fail("expected IllegalArgumentException not thrown !");
        } catch(IllegalArgumentException e) {
            assertTrue(
                    "exception didn't contain '" + fragment + "' instead was '" + e.getMessage() + "'",
                    e.getMessage().contains(fragment));
        }
    }
}
