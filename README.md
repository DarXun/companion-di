# companion-di

companion-di is a very simple dependency injection framework (if you could even call it a framework).
Its goal is to smoothen the development of small tools projects without the overhead of some established dependecy injection frameworks.

## Upcoming features

1. The @Bean-Annotation only works on class-level so far but will be supported on methods via @Configuration-Classes in the future.

Other than that no features are planned.

# Usage

Call *CompanionContainer.setup()* to initialize your *@Beans* and use *getBean* on the container.

1. Define your beans via *@Bean*-Annotation and give them an id with javax.inject's *@Named* or via *value on the @Bean*-Annotation.
2. Inject your beans via constructor-injection with javax.inject's *@Inject*-Annotation on the relevant constructor.
3. The default scope for any bean is the singleton scope but with *@ThreadScope* you can limit the beans lifecycle to a thread.

## License

[BSD 3-Clause](https://choosealicense.com/licenses/bsd-3-clause/)
