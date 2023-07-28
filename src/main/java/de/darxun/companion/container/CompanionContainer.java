package de.darxun.companion.container;

import de.darxun.companion.BeanCreationException;
import de.darxun.companion.BeanNotFoundException;
import de.darxun.companion.container.model.BeanDefinition;
import de.darxun.companion.container.model.BeanDependency;
import de.darxun.companion.container.model.BeanSupplier;
import de.darxun.companion.container.model.SingletonBeanSupplier;
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
import java.util.stream.Collectors;

// TODO add support for @Configuration-Classes
// TODO add support for ThreadScope
public class CompanionContainer {

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
    private boolean doInjectByInterface = true;

    /**
     * Flags wether injection by superclass should be allowed or not
     */
    private boolean doInjectBySuperclass = true;

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

        try {
            bean = getBean(BeanDefinitionHelper.getBeanId(clazz), clazz);
        } catch (BeanNotFoundException e) {
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
     */
    private <T extends Object> BeanDefinition getBeanDefinitionByClass(final Class<T> clazz) {
        for (BeanDefinition beanDefinition : beanDefinitionSet) {
            boolean isClazz = beanDefinition.getClazz().equals(clazz);
            boolean isInterfaceClazz = doInjectByInterface ? beanDefinition.getInterfaces().contains(clazz) : false;
            boolean isSuperClazz = doInjectBySuperclass ? beanDefinition.getSuperclasses().contains(clazz) : false;
            if (isClazz || isInterfaceClazz || isSuperClazz) {
                return beanDefinition;
            }
        }

        throw new BeanNotFoundException(String.format("No bean found for class %s", clazz));
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
        Set<BeanDefinition> beanDefinitions = new HashSet<>(beanClasses.size());

        for (Class<?> clazz : beanClasses) {
            Constructor injectableConstructor = ReflectionHelper.getInjectableConstructor(clazz);
            final String beanId = ReflectionHelper.getBeanId(clazz);

            if (beanId != null && beanId.trim().length() == 0) {
                throw new IllegalStateException(String.format("The id (%s) is not a valid bean id.", beanId));
            }

            BeanDefinition beanDefinition;
            if (beanId == null) {
                beanDefinition = new BeanDefinition(clazz);
            } else {
                beanDefinition = new BeanDefinition(clazz, beanId);
            }

            beanDefinition.setConstructor(injectableConstructor);

            String[] beanIdsForDependencies = ReflectionHelper.getBeanIdsForDependencies(injectableConstructor);
            Parameter[] parameters = injectableConstructor.getParameters();

            if (doInjectByInterface) {
                Set<Class<?>> interfaces = ReflectionHelper.getAllInterfaces(clazz);
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
        }

        return beanDefinitions;
    }

    /**
     * Creates instances for the given BeanDefinitions
     * @param beanDefinitions BeanDefinitions to create instances for
     */
    private void initializeBeans(Set<BeanDefinition> beanDefinitions) {
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
            Set<BeanDefinition> history = new HashSet<>();

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
    private BeanSupplier getOrCreateBean(final BeanDefinition beanDefinition, Set<BeanDefinition> history) {
        if (history.contains(beanDefinition)) {
            // if in the history of creating a bean for this beanDefinition we already came across this definition, then this is a circle injection
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

        Object instance = null;

        try {
            instance = beanDefinition.getClazz().cast(beanDefinition.getConstructor().newInstance(ctorParm));
        } catch (InstantiationException e) {
            throw new BeanCreationException(e);
        } catch (IllegalAccessException e) {
            throw new BeanCreationException(e);
        } catch (InvocationTargetException e) {
            throw new BeanCreationException(e);
        }

        if (instance == null) {
            throw new RuntimeException(String.format("Unexpected error creating bean %s", beanDefinition.getId()));
        }

        return new SingletonBeanSupplier(instance);
    }

    /**
     * Returns an Object-Array containing the instances for the dependencies of the given BeanDefinition.
     * May construct these dependecies via recursively calling getOrCreateBean
     *
     * @param beanDefinition BeanDefinition to create the dependencies for
     * @param history the bean-creation history containing all BeanDefinitions visited while creating a bean
     * @return Constructor-Parameter to instantiate the bean
     */
    private Object[] createConstructorParameters(BeanDefinition beanDefinition, Set<BeanDefinition> history) {
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
