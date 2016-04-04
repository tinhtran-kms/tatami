package fr.ippon.tatami.repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static fr.ippon.tatami.config.ColumnFamilyKeys.SHARES_CF;

/**
 * Cassandra implementation of the Shares repository.
 * Lists the shares for a given status.
 * <p/>
 * Structure :
 * - Key = status Id
 * - Name = time
 * - Value = username who shared this status
 *
 * @author Julien Dubois
 */
@Repository
public class SharesRepository {

    @Inject
    private Session session;


    @CacheEvict(value = "shared-cache", key = "#statusId")
    public void newShareByUsername(String statusId, String sharedByUsername) {
        Statement statement = QueryBuilder.insertInto(SHARES_CF)
                .value("status", UUID.fromString(statusId))
                .value("username",sharedByUsername);
        session.execute(statement);
    }


   @Cacheable("shared-cache")
    public Collection<String> findUsernamesWhoSharedAStatus(String statusId) {
       Statement statement = QueryBuilder.select()
               .column("username")
               .from(SHARES_CF)
               .where(eq("status", UUID.fromString(statusId)))
               .limit(100);
       ResultSet results = session.execute(statement);
       return results
               .all()
               .stream()
               .map(e -> e.getString("username"))
               .collect(Collectors.toList());
    }


    public boolean hasBeenShared(String statusId) {
        Statement statement = QueryBuilder.select()
                .column("username")
                .from(SHARES_CF)
                .where(eq("status", UUID.fromString(statusId)))
                .limit(1);
        ResultSet results = session.execute(statement);
        return !results.isExhausted();
    }
}