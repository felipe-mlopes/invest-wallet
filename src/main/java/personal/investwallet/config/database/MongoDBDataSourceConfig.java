package personal.investwallet.config.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories
@EnableTransactionManagement
public class MongoDBDataSourceConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongo.uri}")
    private String uri;

    @Value("${spring.data.mongodb.database}")
    private String db;

    @Override
    protected String getDatabaseName() {
        return db;
    }

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
    }
}
