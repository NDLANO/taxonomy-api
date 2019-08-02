package no.ndla.taxonomy.hibernate;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.StringType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

import java.net.URI;

public class UriType extends AbstractSingleColumnStandardBasicType<URI> implements DiscriminatorType<URI> {
    public static final UriType INSTANCE = new UriType();

    public UriType() {
        super(VarcharTypeDescriptor.INSTANCE, UriTypeDescriptor.INSTANCE);
    }

    public String getName() {
        return "uri";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    @Override
    public String toString(URI value) {
        return UriTypeDescriptor.INSTANCE.toString(value);
    }

    public String objectToSQLString(URI value, Dialect dialect) throws Exception {
        return StringType.INSTANCE.objectToSQLString(toString(value), dialect);
    }

    public URI stringToObject(String xml) {
        return UriTypeDescriptor.INSTANCE.fromString(xml);
    }
}
