package org.testng.internal;

import org.testng.IClass;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.collections.Lists;

import java.util.List;

/**
 * Collections of helper methods to help deal with TestNG configuration methods
 */
class TestNgMethodUtils {

    private TestNgMethodUtils() {
        //Utility class. So hiding the constructor.
    }

    /**
     * A helper method that checks to see if a method is a configuration method or not.
     * @param method - A {@link ITestNGMethod} object which needs to be checked.
     * @return - <code>true</code> if the method is a configuration method and false if its a test method.
     */
    static boolean isConfigurationMethod(ITestNGMethod method) {
        return isConfigurationMethod(method, false);
    }

    /**
     *
     * A helper method that checks to see if a method is a configuration method or not.
     * @param method - A {@link ITestNGMethod} object which needs to be checked.
     * @param includeGroupConfigs - <code>true</code> if before/after group configuration annotations are also to
     *                            be taken into consideration.
     * @return - <code>true</code> if the method is a configuration method and false if its a test method.
     */
    private static boolean isConfigurationMethod(ITestNGMethod method, boolean includeGroupConfigs) {
        boolean flag =  method.isBeforeMethodConfiguration() || method.isAfterMethodConfiguration() ||
            method.isBeforeTestConfiguration()  || method.isAfterTestConfiguration() ||
            method.isBeforeClassConfiguration() || method.isAfterClassConfiguration() ||
            method.isBeforeSuiteConfiguration() || method.isAfterSuiteConfiguration();
        if (includeGroupConfigs) {
            flag = flag || method.isBeforeGroupsConfiguration() || method.isAfterGroupsConfiguration();
        }
        return flag;
    }

    /**
     * A helper method which checks if a given method is a configuration method and is part of list of TestNG methods
     * @param method - A {@link ITestNGMethod} object which needs to be checked.
     * @param methods - A List of {@link ITestNGMethod} in which the check needs to be done.
     * @return - <code>true</code> if the method is a configuration method and exists in the list of methods passed.
     */
    private static boolean containsConfigurationMethod(ITestNGMethod method, List<ITestNGMethod> methods) {
        return isConfigurationMethod(method, true) && methods.contains(method);
    }

    static ITestNGMethod[] filterBeforeTestMethods(ITestClass testClass, Invoker.Predicate<ITestNGMethod, IClass> predicate) {
        return filterMethods(testClass, testClass.getBeforeTestMethods(), predicate);
    }

    static ITestNGMethod[] filterAfterTestMethods(ITestClass testClass, Invoker.Predicate<ITestNGMethod, IClass> predicate) {
        return filterMethods(testClass, testClass.getAfterTestMethods(), predicate);
    }

    /**
     * @return Only the ITestNGMethods applicable for this testClass
     */
    static ITestNGMethod[] filterMethods(IClass testClass,
                                         ITestNGMethod[] methods,
                                         Invoker.Predicate<ITestNGMethod, IClass> predicate) {
        List<ITestNGMethod> vResult= Lists.newArrayList();

        for(ITestNGMethod tm : methods) {
            String msg;
            if (predicate.isTrue(tm, testClass) &&  (!TestNgMethodUtils.containsConfigurationMethod(tm, vResult)) ) {
                msg =  "Keeping method " + tm + " for class " + testClass;
                vResult.add(tm);
            } else {
                msg = "Filtering out method " + tm + " for class " + testClass;
            }
            Utils.log("Invoker " + Thread.currentThread().hashCode(), 10, msg);
        }
        return vResult.toArray(new ITestNGMethod[vResult.size()]);
    }

    /**
     * The array of methods contains @BeforeMethods if isBefore if true, @AfterMethods
     * otherwise.  This function removes all the methods that should not be run at this
     * point because they are either firstTimeOnly or lastTimeOnly and we haven't reached
     * the current invocationCount yet
     */
    static ITestNGMethod[] filterFirstTimeRunnableSetupConfigurationMethods(ITestNGMethod tm, ITestNGMethod[] methods) {
        List<ITestNGMethod> result = Lists.newArrayList();
        for (ITestNGMethod m : methods) {
            ConfigurationMethod cm = (ConfigurationMethod) m;
            if (isConfigMethodRunningFirstTime(cm, tm)) {
                result.add(m);
            }
        }
        return result.toArray(new ITestNGMethod[result.size()]);
    }

    static ITestNGMethod[] filterLastTimeRunnableTeardownConfigurationMethods(ITestNGMethod tm, ITestNGMethod[] methods) {
        List<ITestNGMethod> result = Lists.newArrayList();
        for (ITestNGMethod m : methods) {
            ConfigurationMethod cm = (ConfigurationMethod) m;
            if (isConfigMethodRunningLastTime(cm, tm)) {
                result.add(m);
            }
        }
        return result.toArray(new ITestNGMethod[result.size()]);
    }

    /**
     * @param tm - The {@link ITestNGMethod} object which is to be tested.
     * @return - <code>true</code> if the method depends on other methods and cannot be run independently.
     */
    static boolean cannotRunMethodIndependently(ITestNGMethod tm) {
        String[] methods = tm.getMethodsDependedUpon();
        return null != methods && methods.length > 0;
    }

    // Creates a token for tracking a unique invocation of a method on an instance.
    // Is used when configFailurePolicy=continue.
    static Object getMethodInvocationToken(ITestNGMethod method, Object instance) {
        return String.format("%s+%d+%d", instance.toString(), method.getCurrentInvocationCount(), method.getParameterInvocationCount());
    }

    private static boolean isConfigMethodRunningFirstTime(ConfigurationMethod cm, ITestNGMethod tm) {
        return !cm.isFirstTimeOnly() || (cm.isFirstTimeOnly() && tm.getCurrentInvocationCount() == 0);
    }

    private static boolean isConfigMethodRunningLastTime(ConfigurationMethod cm, ITestNGMethod tm) {
        return !cm.isLastTimeOnly() || (cm.isLastTimeOnly() && !tm.hasMoreInvocation());
    }
}
