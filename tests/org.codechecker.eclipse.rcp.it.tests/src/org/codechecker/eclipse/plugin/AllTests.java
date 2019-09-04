package org.codechecker.eclipse.plugin;

import org.codechecker.eclipse.plugin.utils.GuiUtils;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test Suite for running the gui tests. Add your class to the Suite class list.
 */
@RunWith(Suite.class)
@SuiteClasses({ PluginTest.class,
    IndicatorTest.class,
    ConfigurationTest.class})
public class AllTests {
    
    /**
     * Never called.
     */
    private AllTests() {}
    
    /**
     * Import cpp project into workspace, and setup SWTBot.
     *
     */
    @BeforeClass
    public static void setup() {
        //clearWs();
        SWTWorkbenchBot bot = new SWTWorkbenchBot();
        GuiUtils.closeWelcomeIfPresent(bot);
        GuiUtils.changePerspectiveTo(GuiUtils.C_CPP_PESPECTIVE, bot);
    }
}