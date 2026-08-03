# plusminus-inject

Auto-injection functionality for Spring beans.

Fields of a Spring bean that are still `null` after the bean has been constructed are resolved
from the `ApplicationContext` and written directly — no `@Autowired`, no setter, no constructor
parameter. Injection is performed by an `InjectBeanPostProcessor` (after initialization, and
before initialization for beans that declare a `@PostConstruct` method).

## Opt in with `@AutoInject`

Nothing is injected until at least one bean class is annotated with `@AutoInject`; the annotation
declares which package prefixes take part:

```java
@Configuration
@AutoInject("com.example.app")   // empty value = the package of the annotated class
public class AppConfig {
}
```

Only beans whose class name starts with one of the included prefixes are processed. The library's
own `InjectAutoconfig` contributes `@AutoInject("software.plusminus")`, so plusminus beans are
covered as soon as the artifact is on the classpath.

## Opt out with `@NoInject`

`@NoInject` excludes package prefixes, a whole class, or a single field:

```java
@NoInject({"com.example.app.legacy"})   // exclude these package prefixes
public class AppConfig {
}

@NoInject                               // exclude every field declared by this class
public class NotInjected {
    private MyDependency dependency;
}

public class MyClass {
    @NoInject
    private MyDependency notInjected;   // exclude this field only
}
```

Careful with the class form: the package list is built from *beans* carrying the annotation, and
an empty value means "this class's own package". So `@NoInject` without a value on a bean class
excludes that bean's entire package, not just the bean. `InjectAutoconfig` uses the explicit form
to keep `software.plusminus.test` and `software.plusminus.json` out of the included prefixes.

## What gets injected

```java
public class MyClass {

    private MyDependency1 myDependency1;      // injected by plusminus-inject
    @Autowired
    private MyDependency2 myDependency2;      // injected by Spring, left alone
    private MyDependency3 myDependency3;      // set by the constructor, left alone
    private List<MyHandler> handlers;         // injected: collections are resolved as usual
    private String name;                      // not injected: JDK type
    private BigDecimal number;                // not injected: JDK type
    private final MyDependency4 dep4;         // not injected: final

    public MyClass(MyDependency3 myDependency3) {
        this.myDependency3 = myDependency3;
    }
}
```

A field is skipped when it is `final`, annotated with `@Autowired`, `@Value` or `@NoInject`,
declared by a `@NoInject` class, of a JDK type that is not a `Collection`, or already non-`null`.
That last rule is what makes overloaded constructors work — a field the invoked constructor
populated keeps its value, a field it left `null` is injected:

```java
public class MyClass {

    private MyDependency myDependency;

    public MyClass() {                              // myDependency is injected
    }

    public MyClass(MyDependency myDependency) {     // myDependency is kept as passed
        this.myDependency = myDependency;
    }
}
```

`@ConfigurationProperties` beans are never processed. An unresolvable dependency fails the
context unless the field is annotated `@Nullable` (either `org.springframework.lang.Nullable`
or `javax.annotation.Nullable`), in which case it is left `null`.

## Getting started

```xml
<dependency>
    <groupId>software.plusminus</groupId>
    <artifactId>plusminus-inject</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

`InjectAutoconfig` is registered through `META-INF/spring.factories`, so the post-processor is
active as soon as the artifact is on the classpath. This is a standalone library: no other
plusminus module depends on it, and `plusminus-framework` does not bundle it.

## Building

Requires JDK 8. Build with the Maven wrapper:

```bash
./mvnw clean install
```

The build enforces Checkstyle, PMD, SpotBugs and JaCoCo coverage checks.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
