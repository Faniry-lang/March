package march.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import march.enums.ToolType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MarchTool {
    String name() default "";

    ToolType type() default ToolType.FUNCTION;

    String description() default "";

}