package dk.dbc.jega;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.hazelcast.map.MapStore;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestMapStore implements MapStore<Long, UUID>, MapLoaderLifecycleSupport {
    DataSource dataSource;

    @Override
    public void store(Long id, UUID data) {
        String sql = "insert into test_table(id, data) values(?, ?) on conflict(id) do update set data=excluded.data";
        try {
            update(sql, s -> {
                s.setLong(1, id);
                s.setObject(2, data.toString());
                s.executeUpdate();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void storeAll(Map<Long, UUID> map) {
        String sql = "insert into test_table(id, data) values(?, ?) on conflict(id) do update set data=excluded.data";
        try {
            update(sql, s -> {
                for (Map.Entry<Long, UUID> row : map.entrySet()) {
                    s.setLong(1, row.getKey());
                    s.setString(2, row.getValue().toString());
                    s.addBatch();
                }
                s.executeBatch();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "delete from test_table where id=?";
        try {
            update(sql, PreparedStatement::executeUpdate, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll(Collection<Long> collection) {
        String sql = "delete from test_table where id in (" + stringList(collection) + ")";
        try {
            update(sql, PreparedStatement::executeUpdate);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UUID load(Long id) {
        String sql = "select * from test_table where id=?";
        return find(sql, rs -> fromString(rs.getString(1)), id);
    }

    private UUID fromString(String string) {
        return string == null ? null : UUID.fromString(string);
    }

    @Override
    public Map<Long, UUID> loadAll(Collection<Long> collection) {
        String sql = "select * from test_table where id in (" + stringList(collection) + ")";
        RowMapper<AbstractMap.SimpleImmutableEntry<Long, UUID>> mapper = rs -> new AbstractMap.SimpleImmutableEntry<>(rs.getLong("id"), fromString(rs.getString("data")));
        List<AbstractMap.SimpleImmutableEntry<Long, UUID>> list = getList(mapper, sql);
        return list.stream().collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));
    }

    private String stringList(Collection<?> collection) {
        return collection.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(", "));
    }

    private void update(String query, SpicyFunction<PreparedStatement> block, Object... vars) throws SQLException {
        try(Connection connection = dataSource.getConnection(); PreparedStatement s = connection.prepareCall(query)) {
            setParameters(s, vars);
            block.apply(s);
        }
    }

    private void setParameters(PreparedStatement s, Object... vars) throws SQLException {
        for (int i = 0; i < vars.length; i++) s.setObject(i + 1, vars[i]);
    }

    @Override
    public Iterable<Long> loadAllKeys() {
        String sql = "select id from test_table";
        return getList(rs -> rs.getLong("id"), sql);
    }

    @Override
    public void init(HazelcastInstance hazelcastInstance, Properties properties, String s) {
        try {
            InitialContext context = new InitialContext();
            dataSource = (DataSource) context.lookup("jdbc/test");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
    }

    private <R> List<R> getList(RowMapper<R> mapper, String query, Object... vars) {
        try(Connection connection = dataSource.getConnection(); PreparedStatement s = connection.prepareCall(query)) {
            setParameters(s, vars);
            try (ResultSet rs = s.executeQuery()) {
                List<R> result = new ArrayList<>();
                while(rs.next()) result.add(mapper.map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <R> R find(String query, RowMapper<R> mapper, Object... vars) {
        try(Connection connection = dataSource.getConnection(); PreparedStatement s = connection.prepareCall(query)) {
            setParameters(s, vars);
            try (ResultSet rs = s.executeQuery()) {
                if(rs.next()) return mapper.map(rs);
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    interface RowMapper<R> {
        R map(ResultSet rs) throws SQLException;
    }

    interface SpicyFunction<P> {
        void apply(P p) throws SQLException;
    }
}
