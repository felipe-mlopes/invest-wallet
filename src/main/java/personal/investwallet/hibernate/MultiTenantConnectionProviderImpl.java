package personal.investwallet.hibernate;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

@Component
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {
    private final DataSource datasource;

    public MultiTenantConnectionProviderImpl(DataSource datasource) {
        this.datasource = datasource;
    };

    @Override
    public Connection getAnyConnection() throws SQLException {
        return datasource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }
   
    @Override
    public Connection getConnection(String tentantIdentifier) throws SQLException {
        final Connection connection = getAnyConnection();

        try {
            connection.createStatement().execute("set search_path to " + tentantIdentifier);
        } catch (SQLException e) {
            throw new HibernateException("Não foi possível alterar o schema " + tentantIdentifier);
        }

        return connection;
    }
    
    @Override
    public void releaseConnection(String tentantIdentifier, Connection connection) throws SQLException {
        try (connection) {
            connection.createStatement().execute("set search_path to " + tentantIdentifier);
        } catch (SQLException e) {
            throw new HibernateException("Não foi possível se conectar ao schema " + tentantIdentifier);
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }

    @Override
    public boolean isUnwrappableAs(Class aClass) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> aClass) {
        return null;
    }

}
