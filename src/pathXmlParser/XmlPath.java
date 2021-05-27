package pathXmlParser;

import java.lang.annotation.*;

@Target(value= ElementType.FIELD)
@Retention(value= RetentionPolicy.RUNTIME)
public @interface XmlPath {
    String path();
}

