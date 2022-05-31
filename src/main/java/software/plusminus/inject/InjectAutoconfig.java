package software.plusminus.inject;

import org.springframework.context.annotation.ComponentScan;

@AutoInject("software.plusminus")
@NoInject({ "software.plusminus.test", "software.plusminus.json" })
@ComponentScan
public class InjectAutoconfig {
}
