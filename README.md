# companion-di

companion-di is a very simple dependency injection framework (if you could even call it a framework).
Its goal is to smoothen the development of small tools projects.

## Plans

1. Currently only singleton beans are supported. I'm planning on adding support for a thread scope to simplify multithreading in tools projects.
2. The @Bean-Annotation only works on class-level so far but will be supported on methods via @Configuration-Classes in the future.

# Usage

Call *CompanionContainer.setup()* to initialize your *@Beans* and use *getBean* on the container.

Aside from *Bean*-Annotation just use javax.inject's *@Inject* and *@Named*. 

## License

[BSD 3-Clause](https://choosealicense.com/licenses/bsd-3-clause/)
