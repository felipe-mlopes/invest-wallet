package personal.investwallet.config.database;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class MongoConfig {

    @ConditionalOnMissingBean(LocalValidatorFactoryBean.class)
    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @ConditionalOnMissingBean(ValidatingMongoEventListener.class)
    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean localValidatorFactoryBean) {
        return new ValidatingMongoEventListener(localValidatorFactoryBean);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory, MongoMappingContext context) {
        return new MongoTemplate(mongoDbFactory);
    }
}
