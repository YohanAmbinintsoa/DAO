package TableMapping;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TableInfo {
    public String jdbc() default "postgresql";
    public String user() default "";
    public String pass() default "";
    public String database();
    public String name() default "";
    public int idtype() default 0;
    public int dimId() default 4;
    public String sequence() default "";
}
