package be.hogent.tarsos.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import be.hogent.tarsos.util.Execute;

public class ExecuteTests {

    @Test
    public void testExecutableInPath() {
        assertTrue("Executable should be in path", Execute
                .executableAvailable("dir"));
        assertTrue("Executable should not be in path", !Execute
                .executableAvailable("qfmjfpoenl"));
    }
}
