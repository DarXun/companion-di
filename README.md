# companion-di

companion-di is a very simple dependency injection framework (if you could even call it a framework).
Its goal is to smoothen the development of small tools projects.

Currently only singleton beans are supported. I'm planning on adding support for a thread scope to simplify multithreading in tools projects.

# Usage

Call *CompanionContainer.setup()* to initialize your *@Beans* and use *getBean* on the container.

## License

[BSD 3-Clause](https://choosealicense.com/licenses/bsd-3-clause/)
