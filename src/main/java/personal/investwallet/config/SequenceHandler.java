package personal.investwallet.config;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Following class is using to get unique ID using sequence name
 */

@Service
public class SequenceHandler {
    private final EntityManager entityManager;

    public SequenceHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * @param sequenceName String
     * @return ID as String , It can be parse any type of number (Long ,Integer)
     */

    @Transactional
    public String generateId(String sequenceName) {

        return entityManager.createNativeQuery("select nextval('" + sequenceName + "')").getSingleResult().toString();
    }
}
