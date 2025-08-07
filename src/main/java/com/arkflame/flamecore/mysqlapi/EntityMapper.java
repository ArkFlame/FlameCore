package com.arkflame.flamecore.mysqlapi;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.arkflame.flamecore.mysqlapi.annotations.PrimaryKey;
import com.arkflame.flamecore.mysqlapi.annotations.StoreAsTable;
import com.arkflame.flamecore.mysqlapi.annotations.Transient;

/**
 * The internal ORM engine. Handles mapping Java objects to MySQL tables.
 * This class is not intended for direct use. It is managed by MySQLAPI.
 */
class EntityMapper {
    private final MySQLAPI mysqlAPI;
    private final Map<Class<?>, MappedEntity> entityCache = new ConcurrentHashMap<>();

    EntityMapper(MySQLAPI mysqlAPI) {
        this.mysqlAPI = mysqlAPI;
    }

    void save(Object entity) {
        try (Connection conn = mysqlAPI.getConnection()) {
            MappedEntity mappedEntity = getMappedEntity(entity.getClass());
            ensureSchemaIsUpToDate(conn, mappedEntity);
            upsertEntity(conn, mappedEntity, entity);
            saveMapFields(conn, mappedEntity, entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    <T> T loadById(Class<T> clazz, Object primaryKeyValue) {
        List<T> results = loadInternal(clazz, null, primaryKeyValue);
        return results.isEmpty() ? null : results.get(0);
    }

    <T> List<T> loadAllBy(Class<T> clazz, String key, Object value) {
        return loadInternal(clazz, key, value);
    }

    private <T> List<T> loadInternal(Class<T> clazz, String key, Object value) {
        List<T> results = new ArrayList<>();
        MappedEntity mappedEntity = getMappedEntity(clazz);
        // If key is null, use the primary key's name, which is pre-validated.
        // If key is not null, it's external input and MUST be validated to prevent SQL injection.
        String sqlKey = (key == null) ? mappedEntity.getPrimaryKey().getName() : key;
        validateIdentifier(sqlKey);

        // This query is now safe as the table name and column name (sqlKey) are validated and quoted.
        String sql = "SELECT * FROM `" + mappedEntity.getTableName() + "` WHERE `" + sqlKey + "` = ?";

        try (Connection conn = mysqlAPI.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    T instance = clazz.getDeclaredConstructor().newInstance();
                    for (Field field : mappedEntity.getAllFields()) {
                        field.setAccessible(true);
                        field.set(instance, rs.getObject(field.getName()));
                    }
                    Object pkValue = mappedEntity.getPrimaryKey().get(instance);
                    loadMapFields(conn, mappedEntity, instance, pkValue);
                    results.add(instance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
    
    private void upsertEntity(Connection conn, MappedEntity mappedEntity, Object entity) throws Exception {
        Field pkField = mappedEntity.getPrimaryKey();
        pkField.setAccessible(true);
        Object pkValue = pkField.get(entity);

        // This query is safe as table and column names are pre-validated and quoted.
        String checkSql = "SELECT COUNT(*) FROM `" + mappedEntity.getTableName() + "` WHERE `" + pkField.getName() + "` = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setObject(1, pkValue);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                boolean exists = rs.getInt(1) > 0;
                
                if (exists) {
                    // This query is safe as table and column names are pre-validated and quoted.
                    String updateSql = "UPDATE `" + mappedEntity.getTableName() + "` SET " +
                            mappedEntity.getColumns().stream().map(f -> "`" + f.getName() + "` = ?").collect(Collectors.joining(", ")) +
                            " WHERE `" + pkField.getName() + "` = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        int i = 1;
                        for (Field field : mappedEntity.getColumns()) {
                            field.setAccessible(true);
                            updatePs.setObject(i++, field.get(entity));
                        }
                        updatePs.setObject(i, pkValue);
                        updatePs.executeUpdate();
                    }
                } else {
                    // This query is safe as table and column names are pre-validated and quoted.
                    String columns = mappedEntity.getAllFields().stream().map(f -> "`" + f.getName() + "`").collect(Collectors.joining(", "));
                    String placeholders = mappedEntity.getAllFields().stream().map(f -> "?").collect(Collectors.joining(", "));
                    String insertSql = "INSERT INTO `" + mappedEntity.getTableName() + "` (" + columns + ") VALUES (" + placeholders + ")";
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        int i = 1;
                        for (Field field : mappedEntity.getAllFields()) {
                            field.setAccessible(true);
                            insertPs.setObject(i++, field.get(entity));
                        }
                        insertPs.executeUpdate();
                    }
                }
            }
        }
    }
    
    private void saveMapFields(Connection conn, MappedEntity mappedEntity, Object entity) throws Exception {
        Object pkValue = mappedEntity.getPrimaryKey().get(entity);
        for(Field mapField : mappedEntity.getMapFields()) {
            mapField.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) mapField.get(entity);
            if (map == null) continue;

            // Use the pre-validated table name from the mapped entity.
            String mapTableName = mappedEntity.getMapTableName(mapField);
            String deleteSql = "DELETE FROM `" + mapTableName + "` WHERE `owner_id` = ?";
            try(PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setObject(1, pkValue);
                ps.executeUpdate();
            }

            String insertSql = "INSERT INTO `" + mapTableName + "` (`owner_id`, `map_key`, `map_value`) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    ps.setObject(1, pkValue);
                    ps.setObject(2, entry.getKey());
                    ps.setObject(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private void loadMapFields(Connection conn, MappedEntity mappedEntity, Object instance, Object pkValue) throws Exception {
        for (Field mapField : mappedEntity.getMapFields()) {
            mapField.setAccessible(true);
            Map<Object, Object> map = (Map<Object, Object>) mapField.getType().getDeclaredConstructor().newInstance();
            // Use the pre-validated table name from the mapped entity.
            String mapTableName = mappedEntity.getMapTableName(mapField);
            String sql = "SELECT `map_key`, `map_value` FROM `" + mapTableName + "` WHERE `owner_id` = ?";
            
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, pkValue);
                try (ResultSet rs = ps.executeQuery()) {
                    while(rs.next()) {
                        map.put(rs.getObject("map_key"), rs.getObject("map_value"));
                    }
                }
            }
            mapField.set(instance, map);
        }
    }

    private void ensureSchemaIsUpToDate(Connection conn, MappedEntity mappedEntity) throws SQLException {
        if (!tableExists(conn, mappedEntity.getTableName())) {
            createTable(conn, mappedEntity);
        } else {
            alterTable(conn, mappedEntity);
        }
        for (Field mapField : mappedEntity.getMapFields()) {
            // Use the pre-validated map table name.
            String mapTableName = mappedEntity.getMapTableName(mapField);
            if (!tableExists(conn, mapTableName)) {
                createMapTable(conn, mapTableName, mappedEntity.getPrimaryKey(), mapField);
            }
        }
    }

    private void createTable(Connection conn, MappedEntity mappedEntity) throws SQLException {
        // This is safe because all identifiers are pre-validated and quoted.
        String columns = mappedEntity.getAllFields().stream()
                .map(f -> "`" + f.getName() + "` " + javaTypeToSqlType(f.getType()) + (f.isAnnotationPresent(PrimaryKey.class) ? " PRIMARY KEY" : ""))
                .collect(Collectors.joining(", "));
        String sql = "CREATE TABLE IF NOT EXISTS `" + mappedEntity.getTableName() + "` (" + columns + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createMapTable(Connection conn, String tableName, Field ownerPk, Field mapField) throws SQLException {
        // The tableName parameter is pre-validated by the caller.
        ParameterizedType mapType = (ParameterizedType) mapField.getGenericType();
        Class<?> keyType = (Class<?>) mapType.getActualTypeArguments()[0];
        Class<?> valueType = (Class<?>) mapType.getActualTypeArguments()[1];
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                     "`owner_id` " + javaTypeToSqlType(ownerPk.getType()) + ", " +
                     "`map_key` " + javaTypeToSqlType(keyType) + ", " +
                     "`map_value` " + javaTypeToSqlType(valueType) + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void alterTable(Connection conn, MappedEntity mappedEntity) throws SQLException {
        // This is safe because all identifiers are pre-validated and quoted.
        DatabaseMetaData meta = conn.getMetaData();
        List<String> existingColumns = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(null, null, mappedEntity.getTableName(), null)) {
            while(rs.next()) {
                existingColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }
        for(Field field : mappedEntity.getAllFields()) {
            if(!existingColumns.contains(field.getName().toLowerCase())) {
                String sql = "ALTER TABLE `" + mappedEntity.getTableName() + "` ADD COLUMN `" + field.getName() + "` " + javaTypeToSqlType(field.getType());
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
            }
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        // The tableName is pre-validated by callers.
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private String javaTypeToSqlType(Class<?> type) {
        if (type == int.class || type == Integer.class) return "INT";
        if (type == long.class || type == Long.class) return "BIGINT";
        if (type == String.class) return "VARCHAR(255)";
        if (type == double.class || type == Double.class) return "DOUBLE";
        if (type == boolean.class || type == Boolean.class) return "BOOLEAN";
        if (type == java.util.UUID.class) return "VARCHAR(36)";
        return "TEXT";
    }

    private MappedEntity getMappedEntity(Class<?> clazz) {
        return entityCache.computeIfAbsent(clazz, this::analyzeClass);
    }
    
    private MappedEntity analyzeClass(Class<?> clazz) {
        String tableName = clazz.getSimpleName().toLowerCase() + "s";
        validateIdentifier(tableName);

        Field primaryKey = null;
        List<Field> columns = new ArrayList<>();
        List<Field> mapFields = new ArrayList<>();
        Map<Field, String> mapTableNames = new ConcurrentHashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Transient.class)) continue;
            
            validateIdentifier(field.getName());
            
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                if (primaryKey != null) {
                    throw new IllegalStateException("Entity " + clazz.getSimpleName() + " must not have more than one @PrimaryKey field.");
                }
                primaryKey = field;
            } else if (field.isAnnotationPresent(StoreAsTable.class)) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    mapFields.add(field);
                    String mapTableName = tableName + "_" + field.getName();
                    validateIdentifier(mapTableName);
                    mapTableNames.put(field, mapTableName);
                }
            } else {
                columns.add(field);
            }
        }
        if (primaryKey == null) {
            throw new IllegalStateException("Entity " + clazz.getSimpleName() + " must have a @PrimaryKey field.");
        }
        return new MappedEntity(tableName, primaryKey, columns, mapFields, mapTableNames);
    }
    
    /**
     * Validates an SQL identifier (e.g., table or column name) to prevent SQL injection.
     * Allows only alphanumeric characters and underscores, and must start with a letter or underscore.
     * @param identifier The identifier to validate.
     * @throws IllegalArgumentException if the identifier is invalid.
     */
    private void validateIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty() || !identifier.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid or potentially unsafe SQL identifier: " + identifier + ". Only alphanumeric characters and underscores are allowed, and it must not start with a number.");
        }
    }

    private static final class MappedEntity {
        private final String tableName;
        private final Field primaryKey;
        private final List<Field> columns;
        private final List<Field> mapFields;
        private final Map<Field, String> mapTableNames;
        
        MappedEntity(String tableName, Field primaryKey, List<Field> columns, List<Field> mapFields, Map<Field, String> mapTableNames) {
            this.tableName = tableName;
            this.primaryKey = primaryKey;
            this.columns = columns;
            this.mapFields = mapFields;
            this.mapTableNames = mapTableNames;
        }

        String getTableName() { return tableName; }
        Field getPrimaryKey() { return primaryKey; }
        List<Field> getColumns() { return columns; }
        List<Field> getMapFields() { return mapFields; }
        String getMapTableName(Field mapField) { return mapTableNames.get(mapField); }

        List<Field> getAllFields() {
            List<Field> all = new ArrayList<>(columns);
            all.add(primaryKey);
            return all;
        }
    }
}