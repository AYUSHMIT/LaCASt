package gov.nist.drmf.interpreter.common.cas;

import gov.nist.drmf.interpreter.common.exceptions.CASUnavailableException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is the base to load native jars dynamically and map local interfaces to the native implementations
 * in the proprietary jars via proxies (hence the interface {@link InvocationHandler}).
 *
 * This base just provides convenient methods to write a more specific class loader. A subclass of this should
 * especially take care of native lib specific error handling. An example can be found in the mathematica module:
 *      gov.nist.drmf.interpreter.mathematica.wrapper.JLinkWrapper
 *      (interpreter.mathematica/src/main/java/gov/nist/.../wrapper/JLinkWrapper.java)
 *
 * @author Andre Greiner-Petter
 */
public abstract class CASReflectionWrapper implements InvocationHandler {
    private static final Logger LOG = LogManager.getLogger(CASReflectionWrapper.class.getName());

    protected abstract Object getEntryPointInstance(Object... arguments) throws CASUnavailableException;

    /**
     * @return the class loader that includes the new jars
     */
    public static ClassLoader getClassLoader(Path... jarPaths) throws MalformedURLException {
        if ( jarPaths == null || jarPaths.length == 0 )
            throw new IllegalArgumentException("Try to load proprietary JAR but no JAR path was specified.");

        LOG.info("Try to load external jar(s)");
        URL[] urls = new URL[jarPaths.length];
        for ( int i = 0; i < jarPaths.length; i++ ) {
            urls[i] = jarPaths[i].toUri().toURL();
        }

        return URLClassLoader.newInstance(
                urls,
                ClassLoader.getSystemClassLoader() // we may want to use the system classloader here?
        );
    }

    /**
     * Creates a dictionary to easy access methods by their names in the given reference instance.
     * @param reference instance to fetch the methods from
     * @return a dictionary of declared methods of the given instances
     */
    public static Map<String, Method> registerMethods(Object reference) {
        Map<String, Method> methodRegister = new HashMap<>();
        for ( Method method : reference.getClass().getMethods() ) {
            methodRegister.put(getQualifiedMethodID(method), method);
        }
        return methodRegister;
    }

    public static String getQualifiedMethodID(Method method) {
        StringBuilder methodID = new StringBuilder(method.getName());
        methodID.append("(");
        Class<?>[] args = method.getParameterTypes();
        for ( int i = 0; i < args.length; i++ ) {
            methodID.append(args[i].getSimpleName());
            if ( i < args.length-1 ) methodID.append(", ");
        }
        methodID.append(")");
        return methodID.toString();
    }
}
