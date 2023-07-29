package de.darxun.companion.container;

import de.darxun.companion.BeanComputationException;
import de.darxun.companion.BeanCreationException;
import de.darxun.companion.BeanNotFoundException;
import de.darxun.companion.NoUniqueBeanFoundException;
import de.darxun.companion.api.ThreadScope;
import de.darxun.companion.container.model.*;
import de.darxun.companion.container.model.beansupplier.BeanSupplier;
import de.darxun.companion.container.model.beansupplier.SingletonBeanSupplier;
import de.darxun.companion.container.model.beansupplier.ThreadScopeBeanSupplier;
import de.darxun.companion.container.util.BeanDefinitionHelper;
import de.darxun.companion.container.util.ReflectionHelper;
import de.darxun.companion.api.Bean;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level;

// TODO add support for @Configuration-Classes
// TODO if there's only one ctor consider this as injectable
// TODO maybe add support for lazy-init
public class CompanionContainer {

    private static final System.Logger LOGGER = System.getLogger(CompanionContainer.class.getName());

    /**
     * Containing all BeanDefinitions
     */
    private Set<BeanDefinition> beanDefinitionSet;

    /**
     * Containing the real bean (or rather a supplier) for a BeanDefinition
     */
    private Map<BeanDefinition, BeanSupplier> beanContainerMap;

    /**
     * Flags wether injection by interface should be allowed (required for ThreadScope-Beans) or not
     */
    private final boolean doInjectByInterface = true;

    /**
     * Flags wether injection by superclass should be allowed or not
     */
    private final boolean doInjectBySuperclass = true;

    /**
     * Private constructor as the container is instantiated via setup-method
     */
    private CompanionContainer() {
        beanDefinitionSet = new HashSet<>();
        beanContainerMap = new HashMap<>();
    }

    /**
     * Initializes the container
     * @return the container
     */
    public static CompanionContainer setup() {
        CompanionContainer container = new CompanionContainer();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "Injection by interface is {0}", container.doInjectByInterface ? "enabled" : "disabled");
            LOGGER.log(Level.INFO, "Injection by superclass is {0}", container.doInjectBySuperclass ? "enabled" : "disabled");
        }

        container.init();
        return container;
    }

    /**
     * Returns the requested bean by the specified id if present.
     * May throw a BeanNotFoundException if no matching bean could be found.
     * @param beanId the beanId
     * @return the bean as Object
     */
    public Object getBean(final String beanId) {
        BeanDefinition beanDefinitionById = getBeanDefinitionById(beanId);

        return beanContainerMap.get(beanDefinitionById).get();
    }

    /**
     * Returns the requested bean (as the correct type) by the specified id and class if present.
     * May throw a BeanNotFoundException if no matching bean could be found.
     * @param beanId the beanId
     * @param clazz the class of the bean
     * @return the bean
     * @param <T> type of the bean
     */
    public <T extends Object> T getBean(final String beanId, final Class<T> clazz) {
        BeanDefinition beanDefinitionById = getBeanDefinitionById(beanId);

        return (T) beanContainerMap.get(beanDefinitionById).get();
    }

    /**
     * Returns the requested bean (as the correct type) by the specified class if present.
     * May throw a BeanNotFoundException if no matching bean could be found.
     * @param clazz the class of the bean
     * @return the bean
     * @param <T> type of the bean
     */
    public <T extends Object> T getBean(final Class<T> clazz) {
        T bean;

        final String bdfBeanId = BeanDefinitionHelper.getBeanId(clazz);

        try {
            bean = getBean(bdfBeanId, clazz);
        } catch (BeanNotFoundException e) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Bean could not be found by default bean id ({0})", bdfBeanId);
            }

            // maybe there was no bean definition for the standard bean id
            bean = getBean(getBeanDefinitionByClass(clazz).getId(), clazz);
        }

        return bean;
    }

    /**
     * Returns the BeanDefinition for the specified beanId if present.
     * May throw a BeanNotFoundException if no matching BeanDefinition could be found.
     * @param beanId the beanId
     * @return the BeanDefinition
     */
    private BeanDefinition getBeanDefinitionById(final String beanId) {
        if (beanId == null || beanId.trim().length() == 0) {
            throw new IllegalArgumentException(String.format("The id (%s) is not a valid bean id.", beanId));
        }

        for (BeanDefinition beanDefinition : beanDefinitionSet) {
            if (beanDefinition.getId().equals(beanId)) {
                return beanDefinition;
            }
        }

        throw new BeanNotFoundException(String.format("No bean found for bean id %s", beanId));
    }

    /**
     * Returns the BeanDefinition for the specified class if present.
     * May throw a BeanNotFoundException if no matching BeanDefinition could be found.
     * @param clazz the class
     * @return the BeanDefinition
     * @param <T> type of the bean
     */
    private <T extends Object> BeanDefinition getBeanDefinitionByClass(final Class<T> clazz) {
        List<BeanDefinition> matches = new ArrayList<>();

        for (BeanDefinition beanDefinition : beanDefinitionSet) {
            boolean isClazz = beanDefinition.getClazz().equals(clazz);
            boolean isInterfaceClazz = doInjectByInterface ? beanDefinition.getInterfaces().contains(clazz) : false;
            boolean isSuperClazz = doInjectBySuperclass ? beanDefinition.getSuperclasses().contains(clazz) : false;

            if (isClazz || isInterfaceClazz || isSuperClazz) {
                logMatchingInfo(beanDefinition, clazz, isClazz, isInterfaceClazz, isSuperClazz);
                matches.add(beanDefinition);
            }
        }

        if (matches.size() == 1) {
            return matches.get(0);
        } else if (matches.size() > 1) {
            throw new NoUniqueBeanFoundException(String.format("No unique bean found for class %s", clazz));
        }

        throw new BeanNotFoundException(String.format("No bean found for class %s", clazz));
    }

    /**
     * Logs some additional information on how the bean definition is a match for the given class
     * @param beanDefinition the BeanDefinition that matches
     * @param clazz the class that matches
     * @param isClazz true, if they match by class
     * @param isInterfaceClazz true, if they match by interface
     * @param isSuperClazz true, if they match by superclass
     * @param <T> type of the bean
     */
    private <T extends Object> void logMatchingInfo(BeanDefinition beanDefinition, Class<T> clazz, boolean isClazz, boolean isInterfaceClazz, boolean isSuperClazz) {
        if (LOGGER.isLoggable(Level.INFO)) {
            List<String> matchInfo = new ArrayList<>(3);
            if (isClazz) {
                matchInfo.add("by class");
            }
            if (isInterfaceClazz) {
                matchInfo.add("by interface");
            }
            if (isSuperClazz) {
                matchInfo.add("by superclass");
            }

            LOGGER.log(Level.DEBUG, String.format("BeanDefinition (%s) is a match for class (%s) %s", beanDefinition.toString(), clazz.getName(), matchInfo.stream().collect(Collectors.joining(","))));
        }
    }

    /**
     * Initializes the container by
     * 1. Scanning for .class-Files in the classpath
     * 2. Finding all @Beans
     * 3. Computing BeanDefinitions
     * 4. Instantiating beans from BeanDefinitions
     */
    private void init() {
        Set<Class<?>> classes = scanForClasses();

        Set<Class<?>> beanClasses = findBeanClasses(classes);
        this.beanDefinitionSet = computeBeanDefinitons(beanClasses);
        initializeBeans(beanDefinitionSet);
    }

    /**
     * Returns all @Bean-Classes
     * @param classes Classes to analyze
     * @return @Bean-Classes
     */
    private Set<Class<?>> findBeanClasses(Set<Class<?>> classes) {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Finding beans");
        }

        return classes.stream().filter(cls -> ReflectionHelper.hasClassAnnotation(cls, Bean.class)).collect(Collectors.toSet());
    }

    /**
     * Scans the classpath for .class-Files
     * @return Class-Objects in the classpath
     */
    private Set<Class<?>> scanForClasses() {
        Set<Class<?>> classes = new HashSet<>();
        scanForClasses("", classes);

        return classes;
    }

    /**
     * Scans a given directory for .class-Files and adds found Class-Objects to the given set
     * @param dirName directory to scan
     * @param classes Set of classes to append to
     */
    private void scanForClasses(String dirName, Set<Class<?>> classes) {
        final boolean isDirNamePresent = dirName != null && dirName.length() != 0;

        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        InputStream resourceAsStream = systemClassLoader.getResourceAsStream(dirName);
        if (resourceAsStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));

            List<String> content = reader.lines().collect(Collectors.toList());
            Set<String> classNames = content.stream().filter(line -> line.trim().length() > 0).filter(line -> line.endsWith(".class")).collect(Collectors.toSet());
            Set<String> nonClasses = content.stream().filter(line -> !line.endsWith(".class")).collect(Collectors.toSet());

            classNames.forEach(clsName -> {
                try {
                    String pkgClsName = (isDirNamePresent ? dirName + "/" : "") + clsName;
                    Class<?> clazz = Class.forName(pkgClsName.replaceAll("/", ".").substring(0, pkgClsName.length() - 6));
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

            nonClasses.stream().map(entry -> {
                try {
                    String resName = (isDirNamePresent ? dirName + "/" : "") + entry;
                    return Paths.get(systemClassLoader.getResource(resName).toURI()).toFile();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }).filter(File::isDirectory).forEach(dir -> scanForClasses((isDirNamePresent ? dirName + "/" : "") + dir.getName(), classes));
        }
    }

    /**
     * Computes BeanDefinition-Instances from the given Class-Objects
     * @param beanClasses Bean-Class-Objects to compute BeanDefinition-Instances for
     * @return BeanDefinitions
     */
    private Set<BeanDefinition> computeBeanDefinitons(Set<Class<?>> beanClasses) {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Computing BeanDefinitions");
        }

        Set<BeanDefinition> beanDefinitions = new HashSet<>(beanClasses.size());

        for (Class<?> clazz : beanClasses) {
            try {
                Constructor injectableConstructor = ReflectionHelper.getInjectableConstructor(clazz);
                final String beanId = ReflectionHelper.getBeanId(clazz);

                if (beanId != null && beanId.trim().length() == 0) {
                    throw new IllegalStateException(String.format("The id (%s) is not a valid bean id.", beanId));
                }

                boolean isThreadScopeBean = false;
                Set<Class<?>> interfaces;
                if (doInjectByInterface) {
                    interfaces = ReflectionHelper.getAllInterfaces(clazz);

                    isThreadScopeBean = isThreadScopeBean(clazz, interfaces);
                }

                BeanDefinition beanDefinition;
                if (beanId == null) {
                    beanDefinition = new BeanDefinition(clazz, isThreadScopeBean ? BeanScope.Thread : BeanScope.Singleton);
                } else {
                    beanDefinition = new BeanDefinition(clazz, beanId, isThreadScopeBean ? BeanScope.Thread : BeanScope.Singleton);
                }

                beanDefinition.setConstructor(injectableConstructor);

                String[] beanIdsForDependencies = ReflectionHelper.getBeanIdsForDependencies(injectableConstructor);
                Parameter[] parameters = injectableConstructor.getParameters();

                if (doInjectByInterface) {
                    beanDefinition.addInterfaces(interfaces);
                }

                if (doInjectBySuperclass) {
                    Set<Class<?>> superclasses = ReflectionHelper.getAllSuperclasses(clazz);
                    superclasses.forEach(beanDefinition::addSuperclass);
                }

                for (int i = 0; i < parameters.length; i++) {
                    beanDefinition.addDependency(new BeanDefinition(parameters[i].getType(), beanIdsForDependencies[i]));
                }

                beanDefinitions.add(beanDefinition);
            } catch (RuntimeException e) {
                throw new BeanComputationException(String.format("BeanDefinition for class (%s) could not be computed", clazz.getName()), e);
            }
        }

        return beanDefinitions;
    }

    /**
     * Returns wether the bean is a valid thread-scope bean.
     * To be so, the bean must be annotated with @ThreadScope and must implement atleast one interface.
     * @param clazz the class to analyze
     * @param interfaces the classes interfaces
     * @return true, if this bean-class qualifies as a thread-scope-bean
     */
    private boolean isThreadScopeBean(Class<?> clazz, Set<Class<?>> interfaces) {
        boolean hasThreadScopeAnnotation = clazz.isAnnotationPresent(ThreadScope.class);

        if (hasThreadScopeAnnotation && interfaces.size() == 0) {
            throw new IllegalStateException(String.format("The class (%s) must implement atleast one interface in order to register for a ThreadScope-Bean", clazz.getName()));
        }

        return hasThreadScopeAnnotation;
    }

    /**
     * Creates instances for the given BeanDefinitions
     * @param beanDefinitions BeanDefinitions to create instances for
     */
    private void initializeBeans(Set<BeanDefinition> beanDefinitions) {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Initializing beans");
        }

        ArrayList<BeanDefinition> sortedBeanDefinitions = new ArrayList<>(beanDefinitions);

        // to lessen the chance of deep recursion, we sort the BeanDefinitions by the number of their dependencies (ascending)
        // Beans without dependencies can be created directly, no recursion-steps needed
        Collections.sort(sortedBeanDefinitions, (o1, o2) -> {
            return o1.getDependencies().size() - o2.getDependencies().size();
        });

        for (int i = 0; i < sortedBeanDefinitions.size(); i++) {
            BeanDefinition beanDefinition = sortedBeanDefinitions.get(i);
            // this is the first getOrCreateBean-call, there may be more to come recursively
            // to pretend a circle-injection we create a temporary history
            List<BeanDefinition> history = new ArrayList<>();

            BeanSupplier beanSupplier = getOrCreateBean(beanDefinition, history);

            if (!beanContainerMap.containsKey(beanDefinition)) {
                beanContainerMap.put(beanDefinition, beanSupplier);
            }
        }
    }

    /**
     * Returns the BeanSupplier for the specified BeanDefinition
     * @param beanDefinition the BeanDefinition
     * @return the BeanSupplier
     */
    private BeanSupplier getOrCreateBean(final BeanDefinition beanDefinition, List<BeanDefinition> history) {
        if (history.contains(beanDefinition)) {
            history.add(beanDefinition);
            // if in the history of creating a bean for this beanDefinition we already came across this definition, then this is a circle injection
            LOGGER.log(Level.ERROR, "Circle detected in history: {0}", history.stream().map(BeanDefinition::toString).collect(Collectors.joining(" -> ")));
            throw new IllegalStateException(String.format("Circle detected while retrieving bean %s", beanDefinition));
        }

        // maybe a supplier was already created?
        if (beanContainerMap.containsKey(beanDefinition)) {
            return beanContainerMap.get(beanDefinition);
        }

        history.add(beanDefinition);

        Object[] ctorParm;
        try {
            ctorParm = createConstructorParameters(beanDefinition, history);
        } catch (IllegalStateException e) {
            throw new BeanCreationException(String.format("Error retrieving constructor parameters to create bean %s", beanDefinition), e);
        }

        BeanSupplier beanSupplier;

        Supplier<Object> instantiator = createBeanInstantiator(beanDefinition, ctorParm);

        BeanScope beanScope = beanDefinition.getScope();
        switch (beanScope) {
            case Singleton:
                Object instance = instantiator.get();
                if (instance == null) {
                    throw new RuntimeException(String.format("Unexpected error creating bean %s", beanDefinition.getId()));
                }

                beanSupplier = new SingletonBeanSupplier(instance);
                break;

            case Thread:
                beanSupplier = new ThreadScopeBeanSupplier(beanDefinition, instantiator);
                break;

            default:
                throw new BeanCreationException(String.format("Bean (%s) cannot be created with Scope %s", beanDefinition.getId(), beanScope));
        }

        return beanSupplier;
    }

    /**
     * Creates an instantiator to use by/for a BeanSupplier
     * @param beanDefinition the BeanDefinition to create a bean for
     * @param ctorParm the constructor-parameters to instantiate the bean
     * @return a supplier that returns an instance for the bean
     */
    private static Supplier<Object> createBeanInstantiator(BeanDefinition beanDefinition, Object[] ctorParm) {
        return () -> {
            try {
                return beanDefinition.getClazz().cast(beanDefinition.getConstructor().newInstance(ctorParm));
            } catch (InstantiationException e) {
                throw new BeanCreationException(e);
            } catch (IllegalAccessException e) {
                throw new BeanCreationException(e);
            } catch (InvocationTargetException e) {
                throw new BeanCreationException(e);
            }
        };
    }

    /**
     * Returns an Object-Array containing the instances for the dependencies of the given BeanDefinition.
     * May construct these dependecies via recursively calling getOrCreateBean
     *
     * @param beanDefinition BeanDefinition to create the dependencies for
     * @param history the bean-creation history containing all BeanDefinitions visited while creating a bean
     * @return Constructor-Parameter to instantiate the bean
     */
    private Object[] createConstructorParameters(BeanDefinition beanDefinition, List<BeanDefinition> history) {
        final List<BeanDependency> dependencies = beanDefinition.getDependencies();
        Object[] ctorParm = new Object[dependencies.size()];

        int i = 0;
        Iterator<BeanDependency> iterator = dependencies.iterator();
        while (iterator.hasNext()) {
            BeanDependency dependency = iterator.next();
            BeanDefinition dependencyBeanDefinition = getBeanDefinitionFromDependency(dependency);

            BeanSupplier dependencySupplier = getOrCreateBean(dependencyBeanDefinition, history);
            ctorParm[i] = dependencySupplier.get();
            i++;
        }

        return ctorParm;
    }

    /**
     * Returns a (complete) BeanDefinition for the given dependency
     * @param dependency the dependency
     * @return the BeanDefinition the for dependency
     */
    private BeanDefinition getBeanDefinitionFromDependency(BeanDependency dependency) {
        Set<BeanDefinition> hits = new HashSet<>();

        if (dependency.getId() != null) { // if an id is specified we search for it
            for (BeanDefinition beanDefinition : this.beanDefinitionSet) {
                if (beanDefinition.getId().equals(dependency.getId())) {
                    hits.add(beanDefinition);
                }
            }
        } else { // otherwise we search by type
            for (BeanDefinition beanDefinition : this.beanDefinitionSet) {
                boolean isClazz = beanDefinition.getClazz().equals(dependency.getClazz());
                boolean isInterfaceClazz = doInjectByInterface ? beanDefinition.getInterfaces().contains(dependency.getClazz()) : false;
                boolean isSuperClazz = doInjectBySuperclass ? beanDefinition.getSuperclasses().contains(dependency.getClazz()) : false;
                if (isClazz || isInterfaceClazz || isSuperClazz) {
                    hits.add(beanDefinition);
                }
            }
        }

        if (hits.size() == 0) {
            throw new IllegalStateException(String.format("No BeanDefinition found for dependency %s.", dependency));
        } else if (hits.size() > 1) {
            throw new IllegalStateException(String.format("Expected one BeanDefinition but found %d: %s", hits.size(), hits.stream().map(BeanDefinition::toString).collect(Collectors.joining(", "))));
        }

        return hits.iterator().next();
    }



}
