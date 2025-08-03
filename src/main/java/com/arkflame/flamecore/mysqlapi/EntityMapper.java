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
        String sqlKey = (key == null) ? mappedEntity.getPrimaryKey().getName() : key;
        String sql = "SELECT * FROM " + mappedEntity.getTableName() + " WHERE " + sqlKey + " = ?";

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
    
    // ... all other private/internal methods from the previous version remain here, unchanged ...
    // upsertEntity, saveMapFields, loadMapFields, ensureSchemaIsUpToDate, DDL methods, etc.
    // They correctly use the `mysqlAPI` field to get connections.
    
    private void upsertEntity(Connection conn, MappedEntity mappedEntity, Object entity) throws Exception {
        Field pkField = mappedEntity.getPrimaryKey();
        pkField.setAccessible(true);
        Object pkValue = pkField.get(entity);

        String checkSql = "SELECT COUNT(*) FROM " + mappedEntity.getTableName() + " WHERE " + pkField.getName() + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setObject(1, pkValue);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                boolean exists = rs.getInt(1) > 0;
                
                if (exists) {
                    String updateSql = "UPDATE " + mappedEntity.getTableName() + " SET " +
                            mappedEntity.getColumns().stream().map(f -> f.getName() + " = ?").collect(Collectors.joining(", ")) +
                            " WHERE " + pkField.getName() + " = ?";
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
                    String columns = mappedEntity.getAllFields().stream().map(Field::getName).collect(Collectors.joining(", "));
                    String placeholders = mappedEntity.getAllFields().stream().map(f -> "?").collect(Collectors.joining(", "));
                    String insertSql = "INSERT INTO " + mappedEntity.getTableName() + " (" + columns + ") VALUES (" + placeholders + ")";
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

            String mapTableName = mappedEntity.getTableName() + "_" + mapField.getName();
            String deleteSql = "DELETE FROM " + mapTableName + " WHERE owner_id = ?";
            try(PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setObject(1, pkValue);
                ps.executeUpdate();
            }

            String insertSql = "INSERT INTO " + mapTableName + " (owner_id, map_key, map_value) VALUES (?, ?, ?)";
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
            String mapTableName = mappedEntity.getTableName() + "_" + mapField.getName();
            String sql = "SELECT map_key, map_value FROM " + mapTableName + " WHERE owner_id = ?";
            
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
            String mapTableName = mappedEntity.getTableName() + "_" + mapField.getName();
            if (!tableExists(conn, mapTableName)) {
                createMapTable(conn, mapTableName, mappedEntity.getPrimaryKey(), mapField);
            }
        }
    }

    private void createTable(Connection conn, MappedEntity mappedEntity) throws SQLException {
        String columns = mappedEntity.getAllFields().stream()
                .map(f -> "`" + f.getName() + "` " + javaTypeToSqlType(f.getType()) + (f.isAnnotationPresent(PrimaryKey.class) ? " PRIMARY KEY" : ""))
                .collect(Collectors.joining(", "));
        String sql = "CREATE TABLE IF NOT EXISTS `" + mappedEntity.getTableName() + "` (" + columns + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void createMapTable(Connection conn, String tableName, Field ownerPk, Field mapField) throws SQLException {
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
        Field primaryKey = null;
        List<Field> columns = new ArrayList<>();
        List<Field> mapFields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Transient.class)) continue;
            
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                primaryKey = field;
            } else if (field.isAnnotationPresent(StoreAsTable.class)) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    mapFields.add(field);
                }
            } else {
                columns.add(field);
            }
        }
        if (primaryKey == null) {
            throw new IllegalStateException("Entity " + clazz.getSimpleName() + " must have a @PrimaryKey field.");
        }
        return new MappedEntity(tableName, primaryKey, columns, mapFields);
    }

    private static final class MappedEntity {
        private final String tableName;
        private final Field primaryKey;
        private final List<Field> columns;
        private final List<Field> mapFields;
        
        MappedEntity(String tableName, Field primaryKey, List<Field> columns, List<Field> mapFields) {
            this.tableName = tableName;
            this.primaryKey = primaryKey;
            this.columns = columns;
            this.mapFields = mapFields;
        }

        String getTableName() { return tableName; }
        Field getPrimaryKey() { return primaryKey; }
        List<Field> getColumns() { return columns; }
        List<Field> getMapFields() { return mapFields; }

        List<Field> getAllFields() {
            List<Field> all = new ArrayList<>(columns);
            all.add(primaryKey);
            return all;
        }
    }
}