# plusminus-inject
Auto-injection functionality for Spring beans

##Why?
To get rich dependency injection. Examples:

1. Works exactly as @Autowired but without @Autowired
```java
public class MyClass {
    
    private MyDependency1 myDependency1; // injected by plusminus-inject
    @Autowired
    private MyDependency2 myDependency2; // injected by Spring
    private MyDependency3 myDependency3; //injected by Spring trough constructor

    public MyClass(MyDependency3 myDependency3) {
        this.myDependency3 = myDependency3;
    }
    
}
```

2. Inject if first constructor has been called and does not inject (not overwrites) if second constructor
```java
public class MyClass {
    
    private MyDependency myDependency;
    
    public MyClass() {
    }
    
    public MyClass(MyDependency myDependency) {
        this.myDependency = myDependency;
    }
}
```

3. JVM classes are ignored
```java
public class MyClass {
    
    private MyDependency myDependency; //injected
    private String string; //not injected
    private BigDecimal number; //not injected
}
```
