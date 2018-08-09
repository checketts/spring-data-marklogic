package io.github.malteseduck.springframework.data.marklogic.core.mapping;

import com.marklogic.client.io.Format;
import io.github.malteseduck.springframework.data.marklogic.core.Util;
import io.github.malteseduck.springframework.data.marklogic.core.convert.ServerTransformer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;

import java.util.Comparator;

public class BasicMarkLogicPersistentEntity<T> extends BasicPersistentEntity<T, MarkLogicPersistentProperty> implements
        MarkLogicPersistentEntity<T>, ApplicationContextAware {

    private TypePersistenceStrategy typePersistenceStrategy;
    private Format documentFormat;
    private String baseUri;
    private String typeName;
    private Class<? extends ServerTransformer> transformer;

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information) {
        this(information, null);
    }

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information, Comparator<MarkLogicPersistentProperty> comparator) {
        super(information, comparator);

        Document document = this.findAnnotation(Document.class);
        TypePersistenceStrategy defaultTypeStrategy = TypePersistenceStrategy.COLLECTION;
        String defaultTypeName = information.getType().getSimpleName();
        Format defaultFormat = Format.JSON;
        String defaultUri = normalize(defaultTypeName);
        Class<? extends ServerTransformer> defaultTransformer = null;

        if (document != null) {
            defaultUri = normalize(Util.coalesce(document.type(), defaultUri));
            this.baseUri = normalize(Util.coalesce(document.uri(), document.value(), defaultUri));
            this.documentFormat = document.format().toFormat();
            this.typePersistenceStrategy = document.typeStrategy();
            // TODO: if configuration says use full name instead of simple name, let that be the default
            this.typeName = Util.coalesce(document.type(), defaultTypeName);
            this.transformer = document.transformer();
        } else {
            this.baseUri = defaultUri;
            this.typePersistenceStrategy = defaultTypeStrategy;
            this.documentFormat = defaultFormat;
            this.typeName = defaultTypeName;
            this.transformer = defaultTransformer;
        }
    }

    @Override
    public String getBaseUri() {
        return baseUri;
    }

    @Override
    public String getCollection() {
        return null;
    }

    public TypePersistenceStrategy getTypePersistenceStrategy() {
        return this.typePersistenceStrategy;
    }

    @Override
    public Format getDocumentFormat() {
        return this.documentFormat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public Class<? extends ServerTransformer> getTransformer() {
        return transformer;
    }

    private String normalize(String uri) {
        String result = uri;
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        if (!result.endsWith("/")) {
            result = result + "/";
        }
        return result;
    }
}
