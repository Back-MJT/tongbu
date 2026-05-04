-- Legacy PostgreSQL compatibility functions for historical migration only.
-- The active runtime path for this project is now local MySQL, so this file
-- should not be applied during default environment setup.

-- find_in_set: Find position of a value in a comma-separated string.
-- MySQL returns 1-based position or 0 if not found.
-- Used in: SysDeptMapper.xml, SysUserMapper.xml (department hierarchy queries)
CREATE OR REPLACE FUNCTION find_in_set(needle text, haystack text)
RETURNS integer AS $$
    SELECT COALESCE(array_position(string_to_array(haystack, ','), needle), 0)
$$ LANGUAGE SQL IMMUTABLE;

-- date_format: Format a timestamp using MySQL-style format strings.
-- Only %Y%m%d is used by RuoYi mappers currently.
-- Used in: SysUserMapper.xml, SysRoleMapper.xml, SysOperLogMapper.xml (date range filters)
CREATE OR REPLACE FUNCTION date_format(dt timestamp, fmt text)
RETURNS text AS $$
    SELECT to_char(dt, 
        CASE fmt
            WHEN '%Y%m%d' THEN 'YYYYMMDD'
            WHEN '%Y-%m-%d' THEN 'YYYY-MM-DD'
            WHEN '%Y-%m-%d %H:%i:%s' THEN 'YYYY-MM-DD HH24:MI:SS'
            WHEN '%H:%i:%s' THEN 'HH24:MI:SS'
            ELSE fmt
        END
    )
$$ LANGUAGE SQL IMMUTABLE;
